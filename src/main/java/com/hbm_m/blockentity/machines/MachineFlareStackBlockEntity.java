package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IControlReceiver;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.network.MachineControlC2SPacket;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
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
 * Газовая факельная установка — полный порт {@code TileEntityMachineGasFlare} (1.7.10).
 *
 * <p>Клапан ({@code isOn}) и поджиг ({@code doesBurn}) управляются кнопками GUI
 * (оригинальные "valve"/"dial"): при открытом клапане и выключенном поджиге горючий
 * газ просто стравливается (если он газообразный), при включённом поджиге горючие
 * флюиды сжигаются с выработкой энергии (штраф 5 для газов, 10 для жидкостей),
 * а всё негорючее/негазообразное не отводится вовсе.
 *
 * <p>Эффекты 1:1: башня-партикл при стравливании (тип "tower"), пламя "gasfire" +
 * дым + поджог сущностей в AABB над факелом при горении, звуки шипения (раз в 7 тиков)
 * и flamethrowerShoot (раз в 3 тика).
 *
 * <p>Установленное расхождение с оригиналом: {@code FluidTrait.onRelease} (SPILL/BURN)
 * требует мировой сетки загрязнения, которой в порте нет (известный пробел, как у
 * масло-горелки) — вызовы опущены.
 */
public class MachineFlareStackBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2, IControlReceiver, com.hbm_m.interfaces.IFluidCopiable {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_IN = 1;
    public static final int SLOT_FLUID_OUT = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_UPGRADE_1 = 4;
    public static final int SLOT_UPGRADE_2 = 5;
    public static final int INVENTORY_SIZE = 6;

    private static final long MAX_POWER = 100_000L;
    private static final long MAX_EXTRACT = 1_000L;

    private static final int TANK_CAPACITY_MB = 64_000;
    private static final int MAX_VENT_MB = 50;
    private static final int MAX_BURN_MB = 10;
    private static final int BURN_PENALTY_LIQUID = 10;
    private static final int BURN_PENALTY_GASEOUS = 5;

    /** Оригинал getValidUpgrades: SPEED до 3, EFFECT до 3. */
    private static final java.util.Map<ItemMachineUpgrade.UpgradeType, Integer> VALID_UPGRADES =
            java.util.Map.of(ItemMachineUpgrade.UpgradeType.SPEED, 3, ItemMachineUpgrade.UpgradeType.EFFECT, 3);

    public final FluidTank tank;
    /** Оригинальный клапан {@code isOn} — кнопка "valve". */
    public boolean isOn = false;
    /** Оригинальный поджиг {@code doesBurn} — кнопка "dial". */
    public boolean doesBurn = false;

    private final UpgradeManager upgradeManager = new UpgradeManager();
    private int fluidUsed = 0;
    private long lastOutput = 0;

    public MachineFlareStackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLARE_STACK_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, 0L, MAX_EXTRACT);
        tank = new FluidTank(com.hbm_m.inventory.fluid.ModFluids.GAS.getSource(), TANK_CAPACITY_MB);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFlareStackBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        fluidUsed = 0;
        lastOutput = 0;

        // Оригинал: 4 кардинальных подключения на ±2 по X/Z, каждый тик (подписка на флюид).
        for (BlockPos conPos : getConPos(pos)) {
            trySubscribe(tank.getTankType(), level, conPos, getDirectionTo(pos, conPos));
        }

        // Автозалив/автоопустошение канистр (слоты 1/2) + смена типа по fluid-ID (слот 3).
        ItemStack[] slots = getSlotsArray();
        boolean slotsChanged = false;
        if (tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, slots)) slotsChanged = true;
        if (tank.setType(SLOT_FLUID_ID, slots)) slotsChanged = true;
        if (slotsChanged) applySlotsArray(slots);

        int maxVent = MAX_VENT_MB;
        int maxBurn = MAX_BURN_MB;

        if (isOn && tank.getFill() > 0) {

            upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);
            int burn = upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.SPEED);
            int yield = upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.EFFECT);

            maxVent += maxVent * burn;
            maxBurn += maxBurn * burn;

            boolean flammable = FluidType.hasTrait(tank.getTankType(), FT_Flammable.class);
            boolean gaseous = FluidType.hasTrait(tank.getTankType(), FT_Gaseous.class)
                    || FluidType.hasTrait(tank.getTankType(), FT_Gaseous_ART.class);

            if (!doesBurn || !flammable) {
                if (gaseous) {
                    int eject = Math.min(maxVent, tank.getFill());
                    this.fluidUsed = eject;
                    tank.setFill(tank.getFill() - eject);

                    if (level.getGameTime() % 7 == 0) {
                        level.playSound(null, pos.getX(), pos.getY() + 11, pos.getZ(),
                                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.5F, 0.5F);
                    }

                    // Оригинал каждый тик: башня-партикл цвета флюида (тип "tower", lift 1, base 0.25, max 3).
                    CompoundTag data = new CompoundTag();
                    data.putString("type", "tower");
                    data.putFloat("lift", 1F);
                    data.putFloat("base", 0.25F);
                    data.putFloat("max", 3F);
                    data.putInt("life", 150 + level.random.nextInt(20));
                    data.putInt("color", com.hbm_m.inventory.fluid.FluidType.forFluid(tank.getTankType()).getColor());
                    IParticleCreator.sendPacket(level, pos.getX() + 0.5, pos.getY() + 11, pos.getZ() + 0.5, 100, data);
                }
            } else {
                int eject = Math.min(maxBurn, tank.getFill());
                this.fluidUsed = eject;
                tank.setFill(tank.getFill() - eject);

                FT_Flammable trait = FluidType.getTrait(tank.getTankType(), FT_Flammable.class);
                if (trait != null) {
                    int penalty = gaseous ? BURN_PENALTY_GASEOUS : BURN_PENALTY_LIQUID;

                    long powerProd = trait.getHeatEnergy() * eject / 1_000L; // на 1000 за mB
                    powerProd /= penalty;
                    powerProd += powerProd * yield / 3;

                    this.lastOutput = powerProd;
                    setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + powerProd));

                    // Оригинал каждый тик: пламя "gasfire" на вершине башни.
                    CompoundTag data = new CompoundTag();
                    data.putString("type", "gasfire");
                    data.putDouble("mX", level.random.nextGaussian() * 0.15);
                    data.putDouble("mY", 0.2);
                    data.putDouble("mZ", level.random.nextGaussian() * 0.15);
                    IParticleCreator.sendPacket(level, pos.getX() + 0.5, pos.getY() + 11.75, pos.getZ() + 0.5, 100, data);

                    // Поджог сущностей над факелом (оригинальный AABB).
                    for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(
                            pos.getX() - 1, pos.getY() + 12, pos.getZ() - 2,
                            pos.getX() + 2, pos.getY() + 17, pos.getZ() + 2))) {
                        e.setRemainingFireTicks(100); // setFire(5)
                        e.hurt(level.damageSources().onFire(), 5F);
                    }

                    if (level.getGameTime() % 3 == 0) {
                        level.playSound(null, pos.getX(), pos.getY() + 11, pos.getZ(),
                                ModSounds.WEAPON_FLAMETHROWER_SHOOT.get(), SoundSource.BLOCKS, 1.5F, 0.75F);
                    }

                    // Оригинал каждые 5 тиков: дым "vanillaExt" в двух чередующихся точках.
                    if (level.getGameTime() % 2 == 0) {
                        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                                pos.getX() + 1.5, pos.getY() + 10.75, pos.getZ() + 1.5, 1, 0, 0, 0, 0);
                    } else {
                        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                                pos.getX() + 1.125, pos.getY() + 11.75, pos.getZ() - 0.5, 1, 0, 0, 0, 0);
                    }
                }
            }
        }

        // Заряд батареи в слоте 0 (оригинал chargeItemsFromTE(slots, 0, ...)).
        chargeItemInSlot(SLOT_BATTERY);

        setChanged();
        sendUpdateToClient();
    }

    /** Оригинал getConPos: 4 кардинальных позиции на ±2 по X/Z. */
    private BlockPos[] getConPos(BlockPos pos) {
        return new BlockPos[] {
                pos.offset(2, 0, 0),
                pos.offset(-2, 0, 0),
                pos.offset(0, 0, 2),
                pos.offset(0, 0, -2)
        };
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

    // ==================== Управление (клапан / поджиг) ====================

    /** Оригинал hasPermission: не дальше 16 блоков. */
    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 256;
    }

    /** Оригинал receiveControl: "valve" → клапан, "dial" → поджиг. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("valve")) this.isOn = !this.isOn;
        if (data.contains("dial")) this.doesBurn = !this.doesBurn;
        setChanged();
        sendUpdateToClient();
    }

    // ==================== GUI-данные ====================

    public int getProgress() {
        return tank.getFill();
    }

    public int getMaxProgress() {
        return tank.getMaxFill();
    }

    public int getProgressScaled(int scale) {
        int max = tank.getMaxFill();
        return max <= 0 ? 0 : tank.getFill() * scale / max;
    }

    public boolean isActive() {
        return fluidUsed > 0;
    }

    public long getLastOutput() {
        return lastOutput;
    }

    public boolean isOn() {
        return isOn;
    }

    public boolean doesBurn() {
        return doesBurn;
    }

    public FluidTank getTank() {
        return tank;
    }

    /** Публичный мост для меню (isEnergyProviderItem защищён в базовом классе). */
    public boolean acceptsBattery(ItemStack stack) {
        return isEnergyProviderItem(stack);
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("doesBurn", doesBurn);
        tag.putInt("fluidUsed", fluidUsed);
        tag.putLong("lastOutput", lastOutput);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        isOn = tag.getBoolean("isOn");
        doesBurn = tag.getBoolean("doesBurn");
        fluidUsed = tag.getInt("fluidUsed");
        lastOutput = tag.getLong("lastOutput");
        tank.readFromNBT(tag, "tank");
    }

    // ==================== Меню / валидация слотов ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.flare_stack");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isEnergyProviderItem(stack);
            case SLOT_FLUID_IN -> true;
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_UPGRADE_1, SLOT_UPGRADE_2 -> stack.getItem() instanceof ItemMachineUpgrade;
            default -> false;
        };
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineFlareStackMenu.create(id, inventory, this);
    }

    // ==================== Устройство настройки (оригинал) ====================

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = com.hbm_m.interfaces.IFluidCopiable.super.getSettings(level, pos);
        nbt.putBoolean("isOn", isOn);
        nbt.putBoolean("doesBurn", doesBurn);
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        com.hbm_m.interfaces.IFluidCopiable.super.pasteSettings(nbt, index, level, player, pos);
        if (nbt.contains("isOn")) isOn = nbt.getBoolean("isOn");
        if (nbt.contains("doesBurn")) doesBurn = nbt.getBoolean("doesBurn");
        setChanged();
        sendUpdateToClient();
    }
}
