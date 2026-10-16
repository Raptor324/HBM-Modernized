package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineChemicalFactoryMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.module.machine.MachineModuleChemFactoryLane;
import com.hbm_m.recipe.ChemicalPlantRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Chemical Factory — порт 1.7.10 {@code TileEntityMachineChemicalFactory}.
 *
 * <p>Оригинал держит 4 экземпляра {@code ModuleMachineChemplant} (по сути 4 параллельных Chemical
 * Plant), общий контур охлаждения (вода -&gt; отработанный пар) и элаборированную геометрию портов
 * ввода/вывода для соседних труб/кабелей 1.7.10. В этом порту:
 * <ul>
 *   <li>4 линии реализованы через {@link MachineModuleChemFactoryLane} — авто-подбор рецепта
 *       {@link ChemicalPlantRecipe.Type} по содержимому слотов/баков линии (см. класс модуля:
 *       ручной blueprint-выбор рецепта из оригинала сознательно пропущен как nice-to-have);</li>
 *   <li>контур охлаждения упрощён до пары {@link FluidTank} (вода/спент-стим) — при обработке
 *       каждой активной линии списывается 100 мБ воды и добавляется 100 мБ спент-стима, как в оригинале;</li>
 *   <li>внешний ввод/вывод жидкостей — через стандартный {@link IFluidStandardTransceiverMK2} (как у
 *       Chemical Plant), а не через ручную геометрию портов 1.7.10: капабилити-система этого порта
 *       уже решает "к какой трубе что течёт" без вычисления фиксированных мировых координат портов.</li>
 * </ul>
 */
public class MachineChemicalFactoryBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int LANE_COUNT = 4;

    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_UPGRADE_START = 1;
    private static final int SLOT_UPGRADE_END = 3;
    private static final int LANE_SLOT_BASE = 4;
    private static final int LANE_SLOT_STRIDE = 6; // 3 solid in + 3 solid out per lane
    private static final int SLOT_COUNT = LANE_SLOT_BASE + LANE_COUNT * LANE_SLOT_STRIDE; // 28

    private static final int TANK_CAPACITY = 24_000;
    private static final int COOLANT_TANK_CAPACITY = 4_000;
    private static final int COOLANT_PER_PROCESS = 100;
    private static final long BASE_MAX_POWER = 1_000_000L;
    private static final long MAX_RECEIVE = 20_000L;

    private final FluidTank[] inputTanks = new FluidTank[LANE_COUNT * 3];
    private final FluidTank[] outputTanks = new FluidTank[LANE_COUNT * 3];
    private final FluidTank water;
    private final FluidTank lps;
    private boolean tanksDirty = false;

    private final MachineModuleChemFactoryLane[] lanes = new MachineModuleChemFactoryLane[LANE_COUNT];
    private final boolean[] didProcess = new boolean[LANE_COUNT];
    private final UpgradeManager upgradeManager = new UpgradeManager();

    private static final java.util.Map<UpgradeType, Integer> VALID_UPGRADES = java.util.Map.of(
            UpgradeType.SPEED, 3,
            UpgradeType.POWER, 3,
            UpgradeType.OVERDRIVE, 3
    );

    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= 0 && index < LANE_COUNT) {
                return lanes[index] != null ? lanes[index].getProgressInt() : 0;
            }
            if (index >= LANE_COUNT && index < LANE_COUNT * 2) {
                return lanes[index - LANE_COUNT] != null ? lanes[index - LANE_COUNT].getMaxProgress() : 100;
            }
            return switch (index - LANE_COUNT * 2) {
                case 0 -> (int) (getEnergyStored() & 0xFFFFFFFFL);
                case 1 -> (int) ((getEnergyStored() >> 32) & 0xFFFFFFFFL);
                case 2 -> (int) (getMaxEnergyStored() & 0xFFFFFFFFL);
                case 3 -> (int) ((getMaxEnergyStored() >> 32) & 0xFFFFFFFFL);
                case 4 -> didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3] ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return LANE_COUNT * 2 + 5; }
    };

    public MachineChemicalFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_FACTORY_BE.get(), pos, state, SLOT_COUNT, BASE_MAX_POWER, MAX_RECEIVE);

        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
            };
            outputTanks[i] = new FluidTank(TANK_CAPACITY) {
                @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
            };
        }
        water = new FluidTank(Fluids.WATER, COOLANT_TANK_CAPACITY) {
            @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
        };
        lps = new FluidTank(ModFluids.SPENTSTEAM.getSource(), COOLANT_TANK_CAPACITY) {
            @Override public void onContentsChanged() { setChanged(); tanksDirty = true; }
        };

        for (int i = 0; i < LANE_COUNT; i++) {
            int base = LANE_SLOT_BASE + i * LANE_SLOT_STRIDE;
            int[] solidIn = { base, base + 1, base + 2 };
            int[] solidOut = { base + 3, base + 4, base + 5 };
            FluidTank[] laneIn = { inputTanks[i * 3], inputTanks[i * 3 + 1], inputTanks[i * 3 + 2] };
            FluidTank[] laneOut = { outputTanks[i * 3], outputTanks[i * 3 + 1], outputTanks[i * 3 + 2] };
            lanes[i] = new MachineModuleChemFactoryLane(i, this, inventory, solidIn, solidOut, laneIn, laneOut, this.level);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChemicalFactoryBlockEntity be) {
        be.prevAnim = be.anim;

        if (level.isClientSide) {
            for (MachineModuleChemFactoryLane lane : be.lanes) lane.setLevel(level);
            if (be.isAnyLaneActive()) be.anim++;
            return;
        }

        be.ensureNetworkInitialized();
        for (MachineModuleChemFactoryLane lane : be.lanes) lane.setLevel(level);

        long nextMaxPower = 0;
        for (MachineModuleChemFactoryLane lane : be.lanes) {
            ChemicalPlantRecipe recipe = lane.peekRecipe();
            if (recipe != null) nextMaxPower += recipe.getPowerConsumption() * 100L;
        }
        long desiredCap = Math.max(BASE_MAX_POWER, nextMaxPower);
        desiredCap = Math.max(desiredCap, be.getEnergyStored());
        if (desiredCap != be.getMaxEnergyStored()) {
            be.setEnergyCapacity(desiredCap);
        }

        be.chargeFromBattery();
        be.upgradeManager.checkSlots(be.inventory, SLOT_UPGRADE_START, SLOT_UPGRADE_END, VALID_UPGRADES);

        if (level.getGameTime() % 10L == 0L) {
            be.updateEnergyDelta(be.getEnergyStored());
        }

        int s = Math.min(be.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int p = Math.min(be.upgradeManager.getLevel(UpgradeType.POWER), 3);
        int o = Math.min(be.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        double speed = 1.0 + s / 3.0 + o;
        double pow = 1.0 - 0.25 * p + s + (10.0 / 3.0) * o;

        boolean dirty = false;
        boolean canCool = be.canCool();
        for (int i = 0; i < LANE_COUNT; i++) {
            dirty |= be.lanes[i].updateAndGetDirty(speed, pow, canCool);
            boolean processed = be.lanes[i].getDidProcess();
            be.didProcess[i] = processed;
            if (processed) {
                be.water.drainMb(COOLANT_PER_PROCESS);
                be.lps.fillMb(be.lps.getConfiguredFluid(), COOLANT_PER_PROCESS);
            }
        }

        // Внутреннее выравнивание жидкостей: если у одной линии не хватает входа, а у другой лежит
        // избыток того же типа/давления на выходе — перекачиваем немного между баками (как в оригинале).
        for (FluidTank in : be.inputTanks) {
            if (in.isEmpty() && !com.hbm_m.inventory.fluid.tank.FluidTank.isFluidTypeExplicitlySet(in.getConfiguredFluid())) continue;
            for (FluidTank out : be.outputTanks) {
                if (out.isEmpty()) continue;
                if (!com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(out.getStoredFluid(), in.getConfiguredFluid())) continue;
                if (out.getPressure() != in.getPressure()) continue;
                int toMove = Math.min(Math.min(in.getSpaceMb(), out.getFluidAmountMb()), 50);
                if (toMove > 0) {
                    in.fillMb(out.getStoredFluid(), toMove);
                    out.drainMb(toMove);
                }
            }
        }

        if (be.tanksDirty) {
            dirty = true;
            be.tanksDirty = false;
        }

        if (dirty) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    private boolean isAnyLaneActive() {
        return didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3];
    }

    public boolean canCool() {
        return water.getFluidAmountMb() >= COOLANT_PER_PROCESS
                && lps.getFluidAmountMb() <= lps.getCapacityMb() - COOLANT_PER_PROCESS;
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    public MachineModuleChemFactoryLane[] getLanes() { return lanes; }
    public FluidTank[] getInputTanks() { return inputTanks; }
    public FluidTank[] getOutputTanks() { return outputTanks; }
    public FluidTank getWaterTank() { return water; }
    public FluidTank getSpentSteamTank() { return lps; }
    public boolean getLaneDidProcess(int lane) { return didProcess[lane]; }

    public void drops() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    // =====================================================================================
    // IFluidStandardTransceiverMK2 — включая контур охлаждения в общий пул баков.
    // =====================================================================================

    @Override
    public FluidTank[] getReceivingTanks() {
        FluidTank[] all = new FluidTank[inputTanks.length + 1];
        System.arraycopy(inputTanks, 0, all, 0, inputTanks.length);
        all[inputTanks.length] = water;
        return all;
    }

    @Override
    public FluidTank[] getSendingTanks() {
        FluidTank[] all = new FluidTank[outputTanks.length + 1];
        System.arraycopy(outputTanks, 0, all, 0, outputTanks.length);
        all[outputTanks.length] = lps;
        return all;
    }

    @Override
    public FluidTank[] getAllTanks() {
        FluidTank[] all = new FluidTank[inputTanks.length + outputTanks.length + 2];
        System.arraycopy(inputTanks, 0, all, 0, inputTanks.length);
        System.arraycopy(outputTanks, 0, all, inputTanks.length, outputTanks.length);
        all[all.length - 2] = water;
        all[all.length - 1] = lps;
        return all;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public java.util.Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.hbm_m.chemical_factory"); }

    @Override
    public Component getDisplayName() { return getDefaultName(); }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            if (stack.isEmpty()) return false;
            if (stack.getItem() instanceof ItemCreativeBattery) return true;
            return isEnergyProviderItem(stack);
        }
        if (slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END) {
            return stack.getItem() instanceof ItemMachineUpgrade;
        }
        for (int i = 0; i < LANE_COUNT; i++) {
            int base = LANE_SLOT_BASE + i * LANE_SLOT_STRIDE;
            if (slot >= base + 3 && slot <= base + 5) return false; // solid output — не вставляем руками
            if (slot >= base && slot <= base + 2) return true; // solid input — принимаем любой предмет
        }
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineChemicalFactoryMenu(containerId, playerInventory, this, data);
    }

    public boolean isAnyProcessing() { return isAnyLaneActive(); }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < inputTanks.length; i++) tag.put("inputTank" + i, inputTanks[i].writeNBT(new CompoundTag()));
        for (int i = 0; i < outputTanks.length; i++) tag.put("outputTank" + i, outputTanks[i].writeNBT(new CompoundTag()));
        tag.put("water", water.writeNBT(new CompoundTag()));
        tag.put("lps", lps.writeNBT(new CompoundTag()));
        for (int i = 0; i < LANE_COUNT; i++) {
            tag.putBoolean("didProcess" + i, didProcess[i]);
            CompoundTag laneTag = new CompoundTag();
            lanes[i].writeToNBT(laneTag);
            tag.put("lane" + i, laneTag);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < inputTanks.length; i++) if (tag.contains("inputTank" + i)) inputTanks[i].readNBT(tag.getCompound("inputTank" + i));
        for (int i = 0; i < outputTanks.length; i++) if (tag.contains("outputTank" + i)) outputTanks[i].readNBT(tag.getCompound("outputTank" + i));
        if (tag.contains("water")) water.readNBT(tag.getCompound("water"));
        if (tag.contains("lps")) lps.readNBT(tag.getCompound("lps"));
        for (int i = 0; i < LANE_COUNT; i++) {
            didProcess[i] = tag.getBoolean("didProcess" + i);
            if (tag.contains("lane" + i)) lanes[i].readFromNBT(tag.getCompound("lane" + i));
        }
    }
}
