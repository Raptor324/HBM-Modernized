package com.hbm_m.block.entity;

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

public abstract class OilDrillBaseBlockEntity extends BaseMachineBlockEntity implements IFluidStandardSenderMK2 {

    public int indicator = 0;
    public FluidTank[] tanks;

    protected UpgradeManager upgradeManager = new UpgradeManager();
    public int speedLevel;
    public int energyLevel;
    public int overLevel;
    public int afterburnerLevel;

    protected Set<BlockPos> trace = new HashSet<>();

    public OilDrillBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, 0, 0, 0); // maxEnergy и consumption переопределяются в getPowerReq

        tanks = new FluidTank[2];
        tanks[0] = new FluidTank(ModFluids.CRUDE_OIL.getSource(), 64_000);
        tanks[1] = new FluidTank(ModFluids.GAS.getSource(), 64_000);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.energy = tag.getLong("power"); // для обратной совместимости логики
        for (int i = 0; i < this.tanks.length; i++) {
            this.tanks[i].readFromNBT(tag, "t" + i);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("power", this.energy);
        for (int i = 0; i < this.tanks.length; i++) {
            this.tanks[i].writeToNBT(tag, "t" + i);
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

        // 1.7.10 getConPos() → 1.20.1 MK2: подписываемся в трубы по разрешённым направлениям
        // и провайдим обе жидкости (нефть/газ) в сети соответствующих типов.
        for (Direction dir : entity.getConPos()) {
            BlockPos pipePos = pos.relative(dir);
            net.minecraft.world.level.block.entity.BlockEntity pipeBe = level.getBlockEntity(pipePos);
            if (!(pipeBe instanceof com.hbm_m.api.fluids.IFluidConnectorMK2)) continue;

            for (FluidTank t : entity.tanks) {
                if (t.getFill() <= 0) continue;
                entity.tryProvide(t, level, pipePos, dir);
            }
        }

        // Логика бурения и выкачивания
        if (entity.energy >= entity.getPowerReqEff() &&
                entity.tanks[0].getSpaceMb() > 0 &&
                entity.tanks[1].getSpaceMb() > 0) {

            entity.energy -= entity.getPowerReqEff();

            if (level.getGameTime() % entity.getDelayEff() == 0) {
                entity.indicator = 0;

                for (int y = pos.getY() - 1; y >= entity.getDrillDepth(); y--) {
                    BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
                    Block b = level.getBlockState(checkPos).getBlock();

                    // Если это не труба нефтяной вышки
                    if (b != ModBlocks.FLUID_DUCT.get()) {
                        if (entity.trySuck(checkPos)) {
                            break;
                        } else {
                            entity.tryDrill(checkPos);
                            break;
                        }
                    }

                    if (y == entity.getDrillDepth()) {
                        entity.indicator = 1;
                    }
                }
            }
        } else {
            entity.indicator = 2;
        }

        entity.setChanged();
        entity.sendUpdateToClient();
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
            level.setBlockAndUpdate(pos, ModBlocks.FLUID_DUCT.get().defaultBlockState()); //TODO: ADD CRUDE OIL EXTRACTION PIPE!
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
        return b == ModBlocks.ORE_OIL.get() || b == ModBlocks.ORE_OIL.get();//TODO: CHANGE 2ND TO OIL_EMPTY
    }

    public boolean suckRec(BlockPos pos, int layer) {
        if (trace.contains(pos)) return false;
        trace.add(pos);

        if (layer > 64) return false;

        Block b = level.getBlockState(pos).getBlock();

        if (b == ModBlocks.ORE_OIL.get() || b == ModBlocks.BEDROCK_OIL.get()) {
            doSuck(pos);
            return true;
        }

        if (b == ModBlocks.BEDROCK_OIL.get()) { //TODO: CHANGE TO BEDROCK_OIL_EMPTY
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
            onSuck(pos);
        }
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
}