package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.machines.MachineWoodBurnerBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.module.ModuleBurnTime;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code TileEntityMachineWoodBurner} (1.7.10 Original) — дровяной генератор
 * (мультиблок 2×2×2): сжигает твёрдое топливо (слот 0) или жидкость из бака
 * ({@link ModuleBurnTime} с множителями log ×4 / wood ×2), выдаёт 100 HE/t в буфер
 * 100k HE и заряжает предмет-батарею в слоте 5 ({@code Library.chargeItemsFromTE}).
 *
 * <p>Два режима, переключаются кнопкой GUI ({@code receiveControl("switch")}):
 * твёрдый ({@code liquidBurn = false}) и жидкий ({@code liquidBurn = true}, бак
 * WOODOIL 16k mB, смена типа fluid identifier'ом, контейнеры в слотах 3/4).
 * Кнопка {@code receiveControl("toggle")} — вкл/выкл: при выключенной горелке
 * топливо НЕ тратится ({@code burnTime} замирает), при полном буфере — тоже.
 *
 * <p>Зола — порт оригинальной схемы WoodBurner: три счётчика (WOOD/COAL/MISC),
 * каждый тик горения накапливает время горения топлива, на пороге 2000 — предмет
 * золы в слот 1. Загрязнение — {@code PollutionHandler.SOOT_PER_SECOND} раз в секунду;
 * мировой сетки загрязнения в порте нет, конвертация в дымовой бак — как у
 * {@link FireboxBaseBlockEntity}/{@link MachineOilburnerBlockEntity}.
 *
 * <p>Частицы: дым из трубы (y+4, задний угол структуры) каждый тик генерации —
 * порт клиентской ветки {@code updateEntity} ({@code spawnParticle("smoke")}).
 */
public class MachineWoodBurnerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_ASH = 1;
    public static final int SLOT_FLUID_ID = 2;
    public static final int SLOT_FLUID_IN = 3;
    public static final int SLOT_FLUID_OUT = 4;
    public static final int SLOT_BATTERY = 5;
    public static final int INVENTORY_SIZE = 6;

    /** Оригинальный {@code maxPower = 100_000}. */
    public static final long MAX_POWER = 100_000L;
    /** Оригинальный темп генерации: {@code powerGen += 100} за тик горения. */
    public static final int GENERATION_PER_TICK = 100;
    /** Порог золы оригинала ({@code int threshold = 2000}). */
    public static final int ASH_THRESHOLD = 2000;
    /** Ёмкость бака оригинала: {@code new FluidTank(Fluids.WOODOIL, 16_000)}. */
    public static final int TANK_CAPACITY = 16_000;
    /** Буфер дымового бака — как у портированной TileEntityMachinePolluting(2, 50). */
    public static final int SMOKE_BUFFER = 50;
    /** {@code PollutionHandler.SOOT_PER_SECOND} оригинала. */
    public static final float SOOT_PER_SECOND = 1.0F / 25.0F;

    /** Оригинал: {@code burnModule = new ModuleBurnTime().setLogTimeMod(4).setWoodTimeMod(2)}. */
    public static final ModuleBurnTime BURN_MODULE = new ModuleBurnTime().setLogTimeMod(4).setWoodTimeMod(2);

    /** Оригинальный {@code tank}. */
    private final FluidTank tank = new FluidTank(ModFluids.WOODOIL.getSource(), TANK_CAPACITY);
    /** Конверсия SOOT-загрязнения (мировой сетки в порте нет). */
    private final FluidTank smoke = new FluidTank(ModFluids.SMOKE.getSource(), SMOKE_BUFFER);

    public int burnTime;
    public int maxBurnTime;
    /** Оригинальный {@code liquidBurn} — режим сжигания жидкости. */
    private boolean liquidBurn = false;
    /** Оригинальный {@code isOn} — кнопка GUI. */
    private boolean isOn = false;
    /** {@code powerGen} — сбрасывается в начале тика, синхронизируется для частиц/GUI. */
    public int powerGen = 0;

    private final int[] ashLevel = new int[3]; // WOOD, COAL, MISC — порядок EnumAshType оригинала

    public MachineWoodBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_BURNER_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, 0L, GENERATION_PER_TICK * 2);
    }

    public ModuleBurnTime getBurnModule() {
        return BURN_MODULE;
    }

    public FluidTank getTank() {
        return tank;
    }

    // ─────────────────────────── Тик ───────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineWoodBurnerBlockEntity be) {
        be.powerGen = 0;

        // Оригинал: tank.setType(2, slots); tank.loadTank(3, 4, slots);
        ItemStack[] slots = be.getSlotsArray();
        boolean slotsChanged = false;
        if (be.tank.setType(SLOT_FLUID_ID, slots)) slotsChanged = true;
        if (be.tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, slots)) slotsChanged = true;
        if (slotsChanged) be.applySlotsArray(slots);

        // Оригинал: Library.chargeItemsFromTE(slots, 5, power, maxPower) — заряд предмета из буфера.
        be.chargeItemInSlot(SLOT_BATTERY);

        // Оригинал: раз в секунду trySubscribe(tank.getTankType()) на обеих задних позициях.
        if (level.getGameTime() % 20 == 0) {
            for (BlockPos portPos : be.getExtraEnergyPorts()) {
                be.trySubscribe(be.tank.getTankType(), level, portPos, getDirectionTo(pos, portPos));
            }
        }

        if (!be.liquidBurn) {

            if (be.burnTime <= 0) {
                // Оригинальная ветка потребления топлива: работает и при isOn = false —
                // горелка «держит» burnTime до включения или заполнения буфера.
                ItemStack fuel = be.inventory.getStackInSlot(SLOT_FUEL);
                if (!fuel.isEmpty()) {
                    int burn = be.BURN_MODULE.getBurnTime(fuel);
                    if (burn > 0) {
                        be.addAsh(FireboxBaseBlockEntity.getAshFromFuel(fuel), burn);
                        be.processAsh();

                        be.maxBurnTime = be.burnTime = burn;

                        // Оригинал: getContainerItem → decrStackSize → контейнер, если слот опустел.
                        Item containerItem = fuel.getItem().getCraftingRemainingItem();
                        if (fuel.getCount() == 1) {
                            be.inventory.setStackInSlot(SLOT_FUEL, containerItem == null ? ItemStack.EMPTY : new ItemStack(containerItem));
                        } else {
                            fuel.shrink(1);
                        }
                        be.setChanged();
                    }
                }

            } else if (be.energy < be.capacity && be.isOn) {
                be.burnTime--;
                be.powerGen += GENERATION_PER_TICK;
                if (level.getGameTime() % 20 == 0) be.polluteSoot(SOOT_PER_SECOND);
            }

        } else {

            if (be.energy < be.capacity && be.tank.getFill() > 0 && be.isOn) {
                FT_Flammable trait = FluidType.getTrait(be.tank.getTankType(), FT_Flammable.class);

                if (trait != null) {
                    // Оригинал: 2 mB за тик, heatEnergy * toBurn / 2000.
                    int toBurn = Math.min(be.tank.getFill(), 2);

                    if (toBurn > 0) {
                        be.powerGen += (int) (trait.getHeatEnergy() * toBurn / 2_000L);
                        be.tank.setFill(be.tank.getFill() - toBurn);
                        if (level.getGameTime() % 20 == 0) be.polluteSoot(SOOT_PER_SECOND * toBurn / 2.0F);
                    }
                }
            }
        }

        // Оригинал: power += powerGen; кламп сверху.
        be.setEnergyStored(be.energy + be.powerGen);

        // LIT-свойство повторяет факт генерации (в блокстейте обе версии ведут к одной модели).
        boolean burning = be.powerGen > 0;
        if (state.getValue(MachineWoodBurnerBlock.LIT) != burning) {
            level.setBlock(pos, state.setValue(MachineWoodBurnerBlock.LIT, burning), 3);
        }

        // Оригинал: networkPackNT(25) — полная синхронизация каждый тик.
        be.sendUpdateToClient();
    }

    /** Клиентский тик: дым из трубы при генерации (порт spawnParticle("smoke")). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, MachineWoodBurnerBlockEntity be) {
        if (be.powerGen <= 0) return;

        // Оригинал: dir = facing; smoke в (0.5 - dir + rot, y + 4), rot = dir.getRotation(UP).
        Direction dir = state.getValue(MachineWoodBurnerBlock.FACING).getOpposite();
        Direction rot = dir.getClockWise();
        double px = pos.getX() + 0.5 - dir.getStepX() + rot.getStepX();
        double pz = pos.getZ() + 0.5 - dir.getStepZ() + rot.getStepZ();
        level.addParticle(ParticleTypes.SMOKE, px, pos.getY() + 4, pz, 0, 0.05, 0);
    }

    private static Direction getDirectionTo(BlockPos from, BlockPos to) {
        return Direction.getNearest(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    // ─────────────────────────── Зола ───────────────────────────

    /** Оригинал: {@code ashLevelWood/Coal/Misc += burn} (burn — время с множителем модуля). */
    private void addAsh(MachineAshpitBlockEntity.AshType type, int amount) {
        switch (type) {
            case WOOD -> ashLevel[0] += amount;
            case COAL -> ashLevel[1] += amount;
            default -> ashLevel[2] += amount;
        }
    }

    /** Оригинал: while(processAsh(level, type, 2000)) level -= 2000 — предмет золы в слот 1. */
    private void processAsh() {
        for (int i = 0; i < ashLevel.length; i++) {
            Item ashItem = ashItemFor(i);
            while (ashLevel[i] >= ASH_THRESHOLD) {
                if (!tryInsertAsh(ashItem)) break;
                ashLevel[i] -= ASH_THRESHOLD;
            }
        }
    }

    private static Item ashItemFor(int index) {
        return switch (index) {
            case 0 -> com.hbm_m.item.ModItems.ASH_WOOD.get();
            case 1 -> com.hbm_m.item.ModItems.ASH_COAL.get();
            default -> com.hbm_m.item.ModItems.ASH_MISC.get();
        };
    }

    private boolean tryInsertAsh(Item ashItem) {
        ItemStack ash = inventory.getStackInSlot(SLOT_ASH);
        if (ash.isEmpty()) {
            inventory.setStackInSlot(SLOT_ASH, new ItemStack(ashItem));
            return true;
        }
        if (ash.is(ashItem) && ash.getCount() < ash.getMaxStackSize()) {
            ash.grow(1);
            return true;
        }
        return false;
    }

    // ─────────────────────────── Загрязнение ───────────────────────────

    /** SOOT → дымовой бак: ceil(amount * 100) mB, переполнение отбрасывается (сетки pollution нет). */
    private void polluteSoot(float amount) {
        int mB = (int) Math.ceil(amount * 100);
        smoke.setFill(Math.min(smoke.getMaxFill(), smoke.getFill() + mB));
    }

    // ─────────────────────────── Жидкости ───────────────────────────

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank, smoke };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    /** Оригинал canConnect(FluidType, dir): только задняя сторона. */
    @Override
    public boolean canConnect(net.minecraft.world.level.material.Fluid fluid, Direction dir) {
        return dir == getBlockState().getValue(MachineWoodBurnerBlock.FACING).getOpposite();
    }

    private ItemStack[] getSlotsArray() {
        ItemStack[] arr = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotsArray(ItemStack[] arr) {
        for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, arr[i]);
    }

    // ─────────────────────────── Управление (receiveControl) ───────────────────────────

    /** Оригинал {@code receiveControl("toggle")} — без NBT-флага всегда инвертирует. */
    public void toggleOn() {
        this.isOn = !this.isOn;
        setChanged();
        sendUpdateToClient();
    }

    /** Оригинал {@code receiveControl("switch")}. */
    public void switchMode() {
        this.liquidBurn = !this.liquidBurn;
        setChanged();
        sendUpdateToClient();
    }

    public boolean isOn() {
        return isOn;
    }

    public boolean isLiquidBurn() {
        return liquidBurn;
    }

    // ─────────────────────────── Автоматизация ───────────────────────────

    /** Оригинал getAccessibleSlotsFromSide {0, 1} / canExtractItem(slot 1) — воронки: топливо внутрь, зола наружу. */
    private ModItemStackHandler automationHandler;

    @Override
    protected ModItemStackHandler getAutomationItemHandler() {
        if (automationHandler == null) {
            automationHandler = new ModItemStackHandler(INVENTORY_SIZE) {
                @Override public int getSlots() { return INVENTORY_SIZE; }
                @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public void setStackInSlot(int slot, ItemStack stack) { inventory.setStackInSlot(slot, stack); }
                @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                    if (slot != SLOT_FUEL) return stack;
                    return inventory.insertItem(slot, stack, simulate);
                }
                @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot != SLOT_ASH) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, ItemStack stack) { return inventory.isItemValid(slot, stack); }
            };
        }
        return automationHandler;
    }

    /** Оригинал isItemValidForSlot: только слот 0 и только топливо (по модулю с множителями). */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_FUEL && !stack.isEmpty() && BURN_MODULE.getBurnTime(stack) > 0;
    }

    // ─────────────────────────── NBT ───────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("liquidBurn", liquidBurn);
        tag.putInt("powerGen", powerGen);
        tank.writeToNBT(tag, "tank");
        smoke.writeToNBT(tag, "smoke");
        tag.putInt("ashWood", ashLevel[0]);
        tag.putInt("ashCoal", ashLevel[1]);
        tag.putInt("ashMisc", ashLevel[2]);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        isOn = tag.getBoolean("isOn");
        liquidBurn = tag.getBoolean("liquidBurn");
        powerGen = tag.getInt("powerGen");
        if (tag.contains("tank")) tank.readFromNBT(tag, "tank");
        if (tag.contains("smoke")) smoke.readFromNBT(tag, "smoke");
        ashLevel[0] = tag.getInt("ashWood");
        ashLevel[1] = tag.getInt("ashCoal");
        ashLevel[2] = tag.getInt("ashMisc");
    }

    // ─────────────────────────── GUI / Menu ───────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.wood_burner");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.hbm_m.inventory.menu.MachineWoodBurnerMenu(id, inventory, this);
    }

    /** Оригинальный getRenderBoundingBox: задняя 3×3, высота 6 (труба). */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(
                worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 6, worldPosition.getZ() + 2);
    }

    // ─────────────────────────── Энергопорты мультиблока ───────────────────────────

    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide) return new BlockPos[0];
        if (!(getBlockState().getBlock() instanceof MachineWoodBurnerBlock block)) return new BlockPos[0];

        var helper = block.getStructureHelper();
        Direction facing = getBlockState().getValue(MachineWoodBurnerBlock.FACING);

        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        for (BlockPos localPos : helper.getStructureMap().keySet()) {
            if (block.getPartRole(localPos) == com.hbm_m.multiblock.PartRole.ENERGY_CONNECTOR) {
                ports.add(helper.getRotatedPos(worldPosition, localPos, facing));
            }
        }
        return ports.toArray(new BlockPos[0]);
    }
}
