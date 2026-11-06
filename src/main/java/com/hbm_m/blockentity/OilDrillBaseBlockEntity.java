package com.hbm_m.blockentity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.industrial.ItemMachineUpgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm_m.platform.ModItemStackHandler;

public abstract class OilDrillBaseBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.block.IPersistentNBT, IFluidStandardSenderMK2 {

    /** Original {@code setInventorySlotContents}: Aufwertung einstecken macht das Steckgeraeusch. */
    @Override
    protected com.hbm_m.platform.ModItemStackHandler createInventoryHandler(int size) {
        return new com.hbm_m.platform.ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (isCriticalSlot(slot)) sendUpdateToClient();
                net.minecraft.world.item.ItemStack stack = getStackInSlot(slot);
                if (level != null && !level.isClientSide && slot >= 5 && slot <= 7
                        && stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                            com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            @Override
            public boolean isItemValid(int slot, @org.jetbrains.annotations.NotNull net.minecraft.world.item.ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }
        };
    }


    public int indicator = 0;
    public FluidTank[] tanks;

    protected UpgradeManager upgradeManager = new UpgradeManager();
    public int speedLevel;
    public int energyLevel;
    public int overLevel;
    public int afterburnerLevel;

    protected Set<BlockPos> trace = new HashSet<>();

    public OilDrillBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        // capacity = Long.MAX_VALUE so chargeFromBatterySlot sees available headroom;
        // actual energy cap is enforced by getMaxPower() in tick and getMaxEnergyStored().
        super(type, pos, state, 8, Long.MAX_VALUE, Long.MAX_VALUE, 0);

        tanks = new FluidTank[2];
        tanks[0] = new FluidTank(ModFluids.CRUDE_OIL.getSource(), 64_000);
        tanks[1] = new FluidTank(ModFluids.GAS.getSource(), 64_000);
    }

    @Override
    public long getMaxEnergyStored() {
        return getMaxPower();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putLong("power", this.energy);
        for (int i = 0; i < this.tanks.length; i++) {
            this.tanks[i].writeToNBT(tag, "t" + i);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.energy = tag.getLong("power"); // для обратной совместимости логики
        for (int i = 0; i < this.tanks.length; i++) {
            this.tanks[i].readFromNBT(tag, "t" + i);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OilDrillBaseBlockEntity entity) {
        if (level.isClientSide) return;

        entity.ensureNetworkInitialized();

        // Обработка инвентаря
        ItemStack[] slots = entity.getSlotsArray();
        boolean changed = false;
        if (entity.tanks[0].unloadTank(1, 2, slots)) changed = true;
        if (entity.tanks[1].unloadTank(3, 4, slots)) changed = true;
        if (changed) entity.applySlotsArray(slots);

        // Обновление апгрейдов
        entity.updateUpgrades();

        // Сжигание попутного газа для генерации энергии (Afterburn)
        int toBurn = Math.min(entity.tanks[1].getFluidAmountMb(), entity.afterburnerLevel * 10);
        if (toBurn > 0) {
            entity.tanks[1].drainMb(toBurn);
            entity.energy += toBurn * 5L;
            if (entity.energy > entity.getMaxPower()) {
                entity.energy = entity.getMaxPower();
            }
        }

        // Зарядка от батарейки (условный метод базы)
        entity.chargeFromBattery();

        // 1.7.10 getConPos() → 1.20.1 MK2: провайдим обе жидкости (нефть/газ) в трубы
        // по разрешённым направлениям. tryProvide — каждый тик (гейт снят, Pump-паттерн):
        // его вторая ветка — прямой пуш в соседний приёмник, гейт делит пропускную
        // способность выкачивания. Обход коннекторов и fill-проверка не выполняются,
        // пока все баки пусты.
        boolean anyFluid = false;
        for (FluidTank t : entity.tanks) {
            if (t.getFill() > 0) { anyFluid = true; break; }
        }
        if (anyFluid) {
            for (Direction dir : entity.getConPos()) {
                BlockPos pipePos = pos.relative(dir);
                net.minecraft.world.level.block.entity.BlockEntity pipeBe = level.getBlockEntity(pipePos);
                if (!(pipeBe instanceof com.hbm_m.api.fluids.IFluidConnectorMK2)) continue;

                for (FluidTank t : entity.tanks) {
                    if (t.getFill() <= 0) continue;
                    entity.tryProvide(t, level, pipePos, dir);
                }
            }
        }

        // Логика бурения и выкачивания
        if (entity.energy >= entity.getPowerReqEff() &&
                entity.tanks[0].getSpaceMb() > 0 &&
                entity.tanks[1].getSpaceMb() > 0) {

            entity.energy -= entity.getPowerReqEff();

            if (level.getGameTime() % entity.getDelayEff() == 0) {

                // Only drill/suck if ORE_OIL actually exists somewhere in the column or adjacent to it
                boolean oilInColumn = false;
                outer:
                for (int y = pos.getY() - 1; y >= entity.getDrillDepth(); y--) {
                    BlockPos colPos = new BlockPos(pos.getX(), y, pos.getZ());
                    if (entity.isColumnDeposit(level.getBlockState(colPos).getBlock())) {
                        oilInColumn = true;
                        break;
                    }
                    for (Direction hDir : Direction.Plane.HORIZONTAL) {
                        // The column itself sits in the drill's chunk, but horizontal neighbours on
                        // the border cross into the next one - the scan pulled it in from the tick
                        // thread.
                        BlockPos sidePos = colPos.relative(hDir);
                        if (!level.isLoaded(sidePos)) continue;
                        if (entity.isColumnDeposit(level.getBlockState(sidePos).getBlock())) {
                            oilInColumn = true;
                            break outer;
                        }
                    }
                }

                if (!oilInColumn) {
                    entity.indicator = 1; // reached bottom / no oil
                } else {
                    entity.indicator = 0;

                    for (int y = pos.getY() - 1; y >= entity.getDrillDepth(); y--) {
                        BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
                        Block b = level.getBlockState(checkPos).getBlock();

                        if (b == ModBlocks.OIL_PIPE.get()) {
                            // Pipe already drilled — check horizontal neighbors for accessible oil
                            boolean sucked = false;
                            for (Direction hDir : Direction.Plane.HORIZONTAL) {
                                if (entity.trySuck(checkPos.relative(hDir))) {
                                    sucked = true;
                                    break;
                                }
                            }
                            if (sucked) break;
                        } else {
                            if (entity.trySuck(checkPos)) {
                                break;
                            } else {
                                entity.tryDrill(checkPos);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            entity.indicator = 2;
        }

        // Анти-спам синка: полный пакет только при изменении отрисовываемого состояния
        // (энергия, индикатор, содержимое/уровень баков).
        long tankFp = 0L;
        for (FluidTank t : entity.tanks) {
            tankFp = tankFp * 31L + (t.getTankType() == null ? 0L : t.getTankType().hashCode());
            tankFp = tankFp * 31L + t.getFill();
        }
        entity.syncIfStateChanged(entity.energy, tankFp * 31L + entity.indicator);
    }

    private ItemStack[] getSlotsArray() {
        ItemStack[] arr = new ItemStack[inventory.getSlots()];
        for (int i = 0; i < inventory.getSlots(); i++) {
            arr[i] = inventory.getStackInSlot(i);
        }
        return arr;
    }

    private void applySlotsArray(ItemStack[] arr) {
        int limit = Math.min(arr.length, inventory.getSlots());
        for (int i = 0; i < limit; i++) {
            inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
        }
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() instanceof com.hbm_m.item.fekal_electric.ItemCreativeBattery) {
            setEnergyStored(getMaxPower());
            return;
        }
        chargeFromBatterySlot(0);
    }

    protected void updateUpgrades() {
        upgradeManager.checkSlots(inventory, 5, 7, getValidUpgrades());
        this.speedLevel = upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.SPEED);
        this.energyLevel = upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.POWER);
        this.overLevel = upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.OVERDRIVE) + 1;
        this.afterburnerLevel = upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.AFTERBURN);
    }

    public int getPowerReqEff() {
        int req = this.getPowerReq();
        return (req + (req / 4 * this.speedLevel) - (req / 4 * this.energyLevel)) * this.overLevel;
    }

    public int getDelayEff() {
        int delay = getDelay();
        return Math.max((delay - (delay / 4 * this.speedLevel) + (delay / 10 * this.energyLevel)) / this.overLevel, 1);
    }

    public abstract int getPowerReq();
    public abstract int getDelay();
    public abstract long getMaxPower();

    public boolean canPump() {
        return true;
    }

    public int getDrillDepth() {
        return 5;
    }

    public void tryDrill(BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        if (b.getExplosionResistance() < 1000) {
            onDrill(pos);
            level.setBlockAndUpdate(pos, ModBlocks.OIL_PIPE.get().defaultBlockState());
        } else {
            this.indicator = 2;
        }
    }

    public void onDrill(BlockPos pos) { }

    public boolean trySuck(BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();

        if (!canSuckBlock(b)) return false;
        if (!this.canPump()) return true;

        trace.clear();
        return suckRec(pos, 0);
    }

    public boolean canSuckBlock(Block b) {
        if (b == ModBlocks.ORE_OIL.get() || b == ModBlocks.ORE_OIL_EMPTY.get()) return true;
        // NTM Space isValid: (b instanceof BlockOreFluid && b != ore_bedrock_oil) || getFullBlock(b) != null
        com.hbm_m.api.space.SpaceResources.FluidDepositBlock dep = com.hbm_m.api.space.SpaceResources.fluidDepositBlock(b);
        return (dep != null && !dep.requiresFracking()) || com.hbm_m.api.space.SpaceResources.isEmptyFluidDeposit(b);
    }

    /** Vorpruefung der Bohrsaeule: HBM {@code ore_oil} oder volle Addon-Lagerstaette ohne Fracking (W-R8B-2). */
    protected boolean isColumnDeposit(Block b) {
        if (b == ModBlocks.ORE_OIL.get()) return true;
        com.hbm_m.api.space.SpaceResources.FluidDepositBlock dep = com.hbm_m.api.space.SpaceResources.fluidDepositBlock(b);
        return dep != null && !dep.requiresFracking();
    }

    public boolean suckRec(BlockPos pos, int layer) {
        if (trace.contains(pos)) return false;
        trace.add(pos);

        if (layer > 64) return false;

        Block b = level.getBlockState(pos).getBlock();

        // suck filled oil blocks directly
        if (b == ModBlocks.ORE_OIL.get() || b == ModBlocks.ORE_BEDROCK_OIL.get()) {
            doSuck(pos);
            return true;
        }

        // NTM Space: jede BlockOreFluid-Lagerstaette (Addon) wird gefoerdert
        com.hbm_m.api.space.SpaceResources.FluidDepositBlock dep = com.hbm_m.api.space.SpaceResources.fluidDepositBlock(b);
        if (dep != null) {
            onSuckDeposit(dep, pos);
            return true;
        }

        // spread through already-emptied holes to reach adjacent filled oil
        if (b == ModBlocks.ORE_OIL_EMPTY.get() || com.hbm_m.api.space.SpaceResources.isEmptyFluidDeposit(b)) {
            List<Direction> dirs = Arrays.asList(Direction.values());
            Collections.shuffle(dirs);

            for (Direction dir : dirs) {
                if (suckRec(pos.relative(dir), layer + 1)) return true;
            }
        }
        return false;
    }

    public void doSuck(BlockPos pos) {
        if (level.getBlockState(pos).getBlock() == ModBlocks.ORE_OIL.get()) {
            // Mit Space-Addon koennen die Tanks von einer Addon-Lagerstaette umgestellt sein (Original setTankType je
            // Foerderung); ohne Addon aendert sich der Tanktyp nie -> no-op.
            if (com.hbm_m.api.space.SpaceApi.isInstalled()) {
                tanks[0].setTankType(com.hbm_m.inventory.fluid.ModFluids.CRUDE_OIL.getSource());
                tanks[1].setTankType(com.hbm_m.inventory.fluid.ModFluids.GAS.getSource());
            }
            onSuck(pos);
        }
    }

    /**
     * 1:1 NTM Space {@code TileEntityOilDrillBase.onSuck(BlockOreFluid, x, y, z)} fuer Addon-Lagerstaetten (W-R8B-2):
     * Tanktypen aus der Lagerstaette, Mengen ueber {@link #depositPrimaryAmount}/{@link #depositSecondaryAmount},
     * Erschoepfung ueber {@link #depositDrainMultiplier}.
     */
    protected void onSuckDeposit(com.hbm_m.api.space.SpaceResources.FluidDepositBlock dep, BlockPos pos) {
        tanks[0].setTankType(depositFluid(dep.getPrimaryFluid()));
        tanks[1].setTankType(depositFluid(dep.getSecondaryFluid()));

        tanks[0].setFill(Math.min(tanks[0].getFill() + depositPrimaryAmount(dep), tanks[0].getMaxFill()));
        if (tanks[1].getTankType() != com.hbm_m.inventory.fluid.ModFluids.NONE.getSource())
            tanks[1].setFill(Math.min(tanks[1].getFill() + depositSecondaryAmount(dep), tanks[1].getMaxFill()));

        dep.drain(level, pos, depositDrainMultiplier());
    }

    /** Original {@code getPrimaryFluidAmount(block, meta)} (Pumpjack: x1.5). */
    protected int depositPrimaryAmount(com.hbm_m.api.space.SpaceResources.FluidDepositBlock dep) {
        return dep.getPrimaryFluidAmount();
    }

    /** Original {@code getSecondaryFluidAmount(block, meta)} (Pumpjack: x0.5). */
    protected int depositSecondaryAmount(com.hbm_m.api.space.SpaceResources.FluidDepositBlock dep) {
        return dep.getSecondaryFluidAmount();
    }

    /** Original {@code attemptDrain}: Erschoepfungs-Multiplikator (Pumpjack: 0.5). */
    protected double depositDrainMultiplier() {
        return 1D;
    }

    /** Fluid-ID der Lagerstaette -> Fluid; null/unbekannt = {@code NONE}. */
    public static net.minecraft.world.level.material.Fluid depositFluid(@org.jetbrains.annotations.Nullable net.minecraft.resources.ResourceLocation id) {
        if (id == null) return com.hbm_m.inventory.fluid.ModFluids.NONE.getSource();
        net.minecraft.world.level.material.Fluid f = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
        return f == null || f == net.minecraft.world.level.material.Fluids.EMPTY ? com.hbm_m.inventory.fluid.ModFluids.NONE.getSource() : f;
    }

    public abstract void onSuck(BlockPos pos);
    public abstract Direction[] getConPos();

    public Map<ItemMachineUpgrade.UpgradeType, Integer> getValidUpgrades() {
        Map<ItemMachineUpgrade.UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(ItemMachineUpgrade.UpgradeType.SPEED, 3);
        upgrades.put(ItemMachineUpgrade.UpgradeType.POWER, 3);
        upgrades.put(ItemMachineUpgrade.UpgradeType.AFTERBURN, 3);
        upgrades.put(ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    // =====================================================================================
    // IFluidStandardSenderMK2
    // =====================================================================================

    @Override
    public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public FluidTank[] getSendingTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(net.minecraft.world.level.material.Fluid fluid, Direction fromDir) {
        if (fromDir == null) return false;
        // Принимаем подключение к трубам нефти или попутного газа.
        return VanillaFluidEquivalence.sameSubstance(fluid, tanks[0].getTankType())
                || VanillaFluidEquivalence.sameSubstance(fluid, tanks[1].getTankType());
    }

    /** Original {@code TileEntityOilDrillBase.writeNBT}: Strom und Tanks, sofern nicht leer. */
    @Override
    public void writeNBT(CompoundTag nbt) {
        boolean empty = this.energy == 0;
        for (var tank : tanks) if (tank.getFill() > 0) empty = false;
        if (empty) return;
        nbt.putLong("power", this.energy);
        for (int i = 0; i < this.tanks.length; i++) this.tanks[i].writeToNBT(nbt, "t" + i);
    }
}
