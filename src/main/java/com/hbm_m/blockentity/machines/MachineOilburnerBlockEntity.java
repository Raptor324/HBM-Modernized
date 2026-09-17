package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FT_Polluting;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.interfaces.IHeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * Порт {@code TileEntityHeaterOilburner} (1.7.10 Original) — жидкотопливный
 * теплогенератор: бак Heating Oil (16000 mB), 3 слота (контейнер внутрь /
 * контейнер наружу / fluid identifier для смены типа бака), скорость горения
 * {@code setting} 1..10 (отвёртка, зацикливается 10 → 1), вкл/выкл только через
 * кнопку GUI (оригинальный {@code receiveControl("toggle")}, красного камня нет).
 *
 * <p>Также обслуживает {@code oilburner_hp} ("High Pressure"): в оригинале
 * вариант существовал только как осиротевшие арт-ассеты, здесь тот же класс
 * с удвоённым баком и удвоенным тепловыделением на mB (выбирается по блоку).
 *
 * <p>Дым — порт {@code TileEntityMachinePolluting}: 3 бака SMOKE / SMOKE_LEADED /
 * SMOKE_POISON с буфером 100 (оригинал: super(3, 100)); раз в 5 тиков горения —
 * {@code pollute(FluidReleaseType.BURN, toBurn * 5)}, burnMap из
 * {@link FT_Polluting} (SOOT → обычный дым, HEAVYMETAL → свинцовый, POISON →
 * ядовитый), переполнение сбрасывается (мировой сетки загрязнения в порте нет),
 * с шансом 1/3 — шипение.
 */
public class MachineOilburnerBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardReceiverMK2, IFluidStandardSenderMK2, IHeatSource, com.hbm_m.interfaces.IFluidCopiable {

    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    public static final int SLOT_FLUID_ID = 2;
    public static final int INVENTORY_SIZE = 3;

    /** Буфер дымовых баков — оригинал TileEntityMachinePolluting(3, 100). */
    public static final int SMOKE_BUFFER = 100;

    public static final int MAX_SETTING = 10;
    private static final int MAX_HEAT = 100_000;
    private static final int TANK_CAPACITY_NORMAL = 16_000;
    private static final int TANK_CAPACITY_HP = 32_000;

    private final FluidTank oilTank;
    private final int heatMultiplier;

    protected final FluidTank smoke = new FluidTank(ModFluids.SMOKE.getSource(), SMOKE_BUFFER);
    protected final FluidTank smokeLeaded = new FluidTank(ModFluids.SMOKE_LEADED.getSource(), SMOKE_BUFFER);
    protected final FluidTank smokePoison = new FluidTank(ModFluids.SMOKE_POISON.getSource(), SMOKE_BUFFER);

    /** Скорость горения 1..10, оригинальное {@code setting}. */
    private int setting = 1;
    private int heatEnergy = 0;
    /** Оригинальный {@code isOn} — только кнопка GUI, никакого редстоуна. */
    private boolean isOn = false;

    public MachineOilburnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OILBURNER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);

        boolean isHp = state.is(ModBlocks.OILBURNER_HP.get());
        this.oilTank = new FluidTank(ModFluids.HEATINGOIL.getSource(), isHp ? TANK_CAPACITY_HP : TANK_CAPACITY_NORMAL);
        this.heatMultiplier = isHp ? 2 : 1;
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return oilTank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    // ─────────────────────────── Жидкости ───────────────────────────

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { oilTank };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { smoke, smokeLeaded, smokePoison };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { oilTank, smoke, smokeLeaded, smokePoison };
    }

    public FluidTank getOilTank() {
        return oilTank;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ─────────────────────────── Тик сервера ───────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, MachineOilburnerBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    /**
     * Порты подключения оригинала ({@code getConPos()}): 4 позиции на ±2 по X/Z.
     */
    private BlockPos[] getConPos(Level level, BlockPos pos) {
        return new BlockPos[] {
                pos.offset(2, 0, 0),
                pos.offset(-2, 0, 0),
                pos.offset(0, 0, 2),
                pos.offset(0, 0, -2)
        };
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        // Автозалив/автоопустошение флюид-контейнеров в слотах 0/1 + смена типа бака по слоту 2.
        ItemStack[] slots = getSlotsArray();
        boolean slotsChanged = false;
        if (oilTank.loadTank(SLOT_IN, SLOT_OUT, slots)) slotsChanged = true;
        if (oilTank.setType(SLOT_FLUID_ID, slots)) slotsChanged = true;
        if (slotsChanged) applySlotsArray(slots);

        // Подписка на сеть масла + отправка дыма (оригинал: trySubscribe + sendSmoke по каждой позиции).
        for (BlockPos conPos : getConPos(level, pos)) {
            Direction dir = getDirectionTo(pos, conPos);
            trySubscribe(oilTank.getTankType(), level, conPos, dir);
            sendSmoke(level, conPos, dir);
        }

        // Точное портирование shouldCool-логики оригинала.
        boolean shouldCool = true;

        if (this.isOn && this.heatEnergy < MAX_HEAT) {
            FT_Flammable trait = com.hbm_m.inventory.fluid.FluidType.getTrait(oilTank.getTankType(), FT_Flammable.class);
            if (trait != null) {
                int burnRate = setting;
                int toBurn = Math.min(burnRate, oilTank.getFill());

                oilTank.setFill(oilTank.getFill() - toBurn);

                int heat = (int) (trait.getHeatEnergy() / 1000L) * this.heatMultiplier;
                this.heatEnergy += heat * toBurn;

                if (level.getGameTime() % 5 == 0 && toBurn > 0) {
                    this.pollute(oilTank.getTankType(), toBurn * 5, level, pos);
                }

                shouldCool = false;
            }
        }

        if (this.heatEnergy >= MAX_HEAT)
            shouldCool = false;

        if (shouldCool)
            this.heatEnergy = Math.max(this.heatEnergy - Math.max(this.heatEnergy / 1000, 1), 0);

        setChanged();
        sendUpdateToClient();
    }

    /**
     * Порт {@code TileEntityMachinePolluting.pollute(FluidType, FluidReleaseType.BURN, amount)}:
     * каждая запись burnMap из {@link FT_Polluting} добавляет ceil(amount * 100) mB
     * в соответствующий дымовой бак (SOOT → smoke, HEAVYMETAL → smoke_leaded,
     * остальное → smoke_poison); переполнение клампится и сбрасывается, шанс 1/3 — шипение.
     */
    private void pollute(net.minecraft.world.level.material.Fluid type, float amount, ServerLevel level, BlockPos pos) {
        FT_Polluting trait = com.hbm_m.inventory.fluid.FluidType.getTrait(type, FT_Polluting.class);
        if (trait == null) return;

        for (var entry : trait.burnMap.entrySet()) {
            FluidTank tank = entry.getKey() == PollutionType.SOOT ? smoke
                    : entry.getKey() == PollutionType.HEAVYMETAL ? smokeLeaded
                    : smokePoison;

            int fluidAmount = (int) Math.ceil(entry.getValue() * amount * 100);
            int newFill = tank.getFill() + fluidAmount;
            if (newFill > tank.getMaxFill()) {
                // Мировой сетки загрязнения в порте нет — излишек просто отбрасывается.
                newFill = tank.getMaxFill();
                if (level.random.nextInt(3) == 0) {
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.1F, 1.5F);
                }
            }
            tank.setFill(newFill);
        }
    }

    /** Порт {@code TileEntityMachinePolluting.sendSmoke}: 3 дымовых бака в позицию трубы. */
    private void sendSmoke(Level level, BlockPos pipePos, Direction dir) {
        if (smoke.getFill() > 0) tryProvide(smoke, level, pipePos, dir);
        if (smokeLeaded.getFill() > 0) tryProvide(smokeLeaded, level, pipePos, dir);
        if (smokePoison.getFill() > 0) tryProvide(smokePoison, level, pipePos, dir);
    }

    private static Direction getDirectionTo(BlockPos from, BlockPos to) {
        return Direction.getNearest(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    private ItemStack[] getSlotsArray() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) slots[i] = inventory.getStackInSlot(i);
        return slots;
    }

    private void applySlotsArray(ItemStack[] slots) {
        for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, slots[i]);
    }

    // ─────────────────────────── Управление ───────────────────────────

    /** Оригинальный {@code toggleSetting}: зацикливание 10 → 1. */
    public void toggleSetting() {
        setting++;
        if (setting > MAX_SETTING) setting = 1;
        setChanged();
    }

    public int getSetting() {
        return setting;
    }

    /** Порт {@code receiveControl("toggle")}: isOn = !isOn (кнопка GUI). */
    public void toggleOn() {
        this.isOn = !this.isOn;
        setChanged();
        sendUpdateToClient();
    }

    /** Оригинальный {@code isOn} — горелка включена кнопкой (не обязательно текущий факел). */
    public boolean isOn() {
        return isOn;
    }

    // ─────────────────────────── Тепло (IHeatSource) ───────────────────────────

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public int getMaxHeatStored() {
        return MAX_HEAT;
    }

    @Override
    public void useUpHeat(int amount) {
        heatEnergy = Math.max(0, heatEnergy - amount);
        setChanged();
    }

    // ─────────────────────────── Инвентарь / GUI ───────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Оригинальные слоты не имеют валидации — любые предметы кладутся вручную,
        // конвейеры/вёдра разбираются тиковой логикой loadTank/setType.
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.oilburner");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineOilburnerMenu.create(id, inventory, this);
    }

    // ─────────────────────────── NBT ───────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        oilTank.writeToNBT(tag, "tank");
        tag.putBoolean("isOn", isOn);
        tag.putInt("heatEnergy", heatEnergy);
        tag.putByte("setting", (byte) setting);
        smoke.writeToNBT(tag, "smoke0");
        smokeLeaded.writeToNBT(tag, "smoke1");
        smokePoison.writeToNBT(tag, "smoke2");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("tank")) oilTank.readFromNBT(tag, "tank");
        isOn = tag.getBoolean("isOn");
        heatEnergy = tag.getInt("heatEnergy");
        setting = tag.contains("setting") ? tag.getByte("setting") : 1;
        if (tag.contains("smoke0")) smoke.readFromNBT(tag, "smoke0");
        if (tag.contains("smoke1")) smokeLeaded.readFromNBT(tag, "smoke1");
        if (tag.contains("smoke2")) smokePoison.readFromNBT(tag, "smoke2");
    }

    /** Оригинальный {@code getRenderBoundingBox}: 3×3×2 вокруг контроллера. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
    }

    // ─────────────────── Устройство настройки ───────────────────
    // Оригинал: тип топлива + burnRate + isOn (дымовые баки не копируются).

    @Override
    public java.util.List<String> getFluidsToCopy() {
        Fluid type = oilTank.getTankType();
        if (type != Fluids.EMPTY && type != ModFluids.NONE.getSource()) {
            return java.util.List.of(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(type).toString());
        }
        return java.util.List.of();
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = com.hbm_m.interfaces.IFluidCopiable.super.getSettings(level, pos);
        nbt.putInt("burnRate", setting);
        nbt.putBoolean("isOn", isOn);
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, net.minecraft.world.entity.player.Player player, BlockPos pos) {
        com.hbm_m.interfaces.IFluidCopiable.super.pasteSettings(nbt, index, level, player, pos);
        if (nbt.contains("isOn")) isOn = nbt.getBoolean("isOn");
        if (nbt.contains("burnRate")) setting = Math.min(nbt.getInt("burnRate"), MAX_SETTING);
        setChanged();
        sendUpdateToClient();
    }
}
