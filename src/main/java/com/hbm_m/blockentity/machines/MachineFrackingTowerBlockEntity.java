package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.config.MachineConfig;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineFrackingTowerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

//?}

/**
 * BlockEntity для Fracking Tower (Гидроразрывная вышка).
 * Порт из версии 1.7.10 (TileEntityMachineFrackingTower).
 * 
 * Функционал:
 * - Добыча нефти и газа из руды (oil_ore, ore_bedrock_oil)
 * - Требует FrackSol для работы
 * - Производит сырую нефть и газ
 * - Поддержка апгрейдов скорости и энергии
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineFrackingTowerBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.block.IPersistentNBT, com.hbm_m.api.fluids.IFluidStandardTransceiverMK2 {
    
    // Энергия
    protected static long maxPower = 5_000_000L;
    protected static long consumption = 5000L;
    
    // Параметры добычи
    protected static int solutionRequired = 10;
    protected static int delay = 20;
    protected static int oilPerDeposit = 1000;
    protected static int gasPerDepositMin = 100;
    protected static int gasPerDepositMax = 500;
    protected static double drainChance = 0.02D;
    protected static int oilPerBedrockDeposit = 100;
    protected static int gasPerBedrockDepositMin = 10;
    protected static int gasPerBedrockDepositMax = 50;
    protected static int destructionRange = 75;

    //=====================================================================================//
    // ТАНКИ ЖИДКОСТЕЙ
    //=====================================================================================//

    // Танк для нефти (выход) - 64,000 mB
    protected final FluidTank oilTank;
    // Танк для газа (выход) - 64,000 mB  
    protected final FluidTank gasTank;
    // Танк для FrackSol (вход) - 64,000 mB
    protected final FluidTank fracksolTank;

    //=====================================================================================//
    // СОСТОЯНИЕ МАШИНЫ
    //=====================================================================================//

    // Таймер для задержки между операциями
    protected int progressTimer = 0;
    
    // Индикатор состояния (0=норма, 1=нет энергии, 2=нет руды, 3=нет раствора)
    protected int indicator = 0;
    
    // Уровни апгрейдов
    protected int speedUpgradeLevel = 0;
    protected int powerUpgradeLevel = 0;
    protected int afterburnerUpgradeLevel = 0;
    protected int overdriveUpgradeLevel = 0;

    private final com.hbm_m.inventory.UpgradeManager upgradeManager = new com.hbm_m.inventory.UpgradeManager();

    //=====================================================================================//
    // СЛОТЫ ИНВЕНТАРЯ (как в оригинале ContainerMachineOilWell)
    //=====================================================================================//

    public static final int SLOT_BATTERY = 0;        // Батарея (8, 53)
    public static final int SLOT_CANISTER_IN = 1;    // Канистра ввод (80, 17)
    public static final int SLOT_CANISTER_OUT = 2;   // Канистра вывод (80, 53)
    public static final int SLOT_GAS_IN = 3;         // Газ баллон ввод (125, 17)
    public static final int SLOT_GAS_OUT = 4;        // Газ баллон вывод (125, 53)
    public static final int SLOT_UPGRADE_1 = 5;      // Апгрейд 1 (152, 17)
    public static final int SLOT_UPGRADE_2 = 6;      // Апгрейд 2 (152, 35)
    public static final int SLOT_UPGRADE_3 = 7;      // Апгрейд 3 (152, 53)

    public static final int INVENTORY_SIZE = 8;

    //=====================================================================================//
    // КОНСТРУКТОР
    //=====================================================================================//

    public MachineFrackingTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), pos, state,
              INVENTORY_SIZE, maxPower, consumption * 2, 0L);
        
        // Инициализация танков - единая инициализация для всех лоадеров
        // Capacities: oil/gas = 128_000 mB (Forge/NeoForge) / 64_000 mB (Fabric), fracksol = 64_000 mB
        // Using 128_000 as base capacity - Fabric will handle droplet conversion via FluidTank
        this.oilTank = new FluidTank(64_000) { // Original 64.000 mB
            @Override
            public boolean isFluidValid(net.minecraft.world.level.material.Fluid fluid) {
                // Space-Addon: Lagerstaette setzt den Tanktyp (Original setTankType), ohne Addon nur Rohoel
                return fluid.isSame(ModFluids.CRUDE_OIL.getSource())
                        || (com.hbm_m.api.space.SpaceApi.isInstalled() && fluid.isSame(getTankType()));
            }
        };

        this.gasTank = new FluidTank(64_000) { // Original 64.000 mB
            @Override
            public boolean isFluidValid(net.minecraft.world.level.material.Fluid fluid) {
                return fluid.isSame(ModFluids.GAS.getSource())
                        || (com.hbm_m.api.space.SpaceApi.isInstalled() && fluid.isSame(getTankType()));
            }
        };

        this.fracksolTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(net.minecraft.world.level.material.Fluid fluid) {
                return fluid.isSame(ModFluids.FRACKSOL.getSource());
            }
        };
    }

    //=====================================================================================//
    // РЕАЛИЗАЦИЯ MenuProvider
    //=====================================================================================//

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frackingTower");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return MachineFrackingTowerMenu.create(containerId, playerInventory, this);
    }

    //=====================================================================================//
    // БАЗОВЫЕ МЕТОДЫ
    //=====================================================================================//

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.frackingTower");
    }

    // ════════════════════════ Валидация слотов ════════════════════════════════

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY                              -> isEnergyItem(stack);
            case SLOT_CANISTER_IN, SLOT_GAS_IN            -> isFluidContainer(stack);
            case SLOT_UPGRADE_1, SLOT_UPGRADE_2,
                 SLOT_UPGRADE_3                            -> isUpgradeItem(stack);
            default -> false;
        };
    }

    private boolean isEnergyItem(ItemStack stack) {
        return isEnergyProviderItem(stack);
    }

    private boolean isFluidContainer(ItemStack stack) {
        // Registry-Behaelter (Kanister, Gasflaschen ...) und Capability-Behaelter
        return com.hbm_m.api.fluids.FluidItemAccess.isFluidContainer(stack);
    }

    private boolean isUpgradeItem(ItemStack stack) {
        return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
    }

    //=====================================================================================//
    // ГЕТТЕРЫ ДЛЯ GUI
    //=====================================================================================//

    public FluidTank  getOilTank() {
        return this.oilTank;
    }

    public FluidTank  getGasTank() {
        return this.gasTank;
    }

    public FluidTank  getFracksolTank() {
        return this.fracksolTank;
    }

    public int getIndicator() {
        return this.indicator;
    }

    public int getProgressTimer() {
        return this.progressTimer;
    }

    public int getDelay() {
        // С учётом апгрейдов скорости
        int baseDelay = delay;
        if (speedUpgradeLevel > 0) {
            baseDelay = (int) (baseDelay * (1.0 - speedUpgradeLevel * 0.25));
        }
        if (powerUpgradeLevel > 0) {
            baseDelay = (int) (baseDelay * (1.0 + powerUpgradeLevel * 0.10));
        }
        return Math.max(1, baseDelay / (overdriveUpgradeLevel + 1));
    }

    public long getConsumption() {
        // С учётом апгрейдов
        long baseConsumption = consumption;
        if (speedUpgradeLevel > 0) {
            baseConsumption = (long) (baseConsumption * (1.0 + speedUpgradeLevel * 0.25));
        }
        if (powerUpgradeLevel > 0) {
            baseConsumption = (long) (baseConsumption * (1.0 - powerUpgradeLevel * 0.25));
        }
        return baseConsumption * (overdriveUpgradeLevel + 1);
    }

    //=====================================================================================//
    // ОСНОВНАЯ ЛОГИКА (ТИК ОБНОВЛЕНИЯ)
    //=====================================================================================//

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFrackingTowerBlockEntity entity) {
        if (level.isClientSide) return;

        entity.ensureNetworkInitialized();
        entity.chargeFromBatterySlot(SLOT_BATTERY);
        entity.updateUpgrades();
        entity.burnAfterburnerGas();
        entity.processFluidContainers();


        
        // Проверка условий для работы
        if (!entity.canOperate()) {
            return;
        }

        // Потребление энергии
        long energyRequired = entity.getConsumption();
        if (entity.energy < energyRequired) {
            entity.indicator = 1; // Нет энергии
            return;
        }

        // Проверка наличия FrackSol
        if (entity.fracksolTank.getFluidAmountMb() < solutionRequired) {
            entity.indicator = 3; // Нет раствора
            return;
        }

        // Таймер прогресса
        entity.progressTimer++;
        
        if (entity.progressTimer >= entity.getDelay()) {
            entity.progressTimer = 0;
            entity.performFrackingOperation();
        }

        entity.indicator = 0; // Норма
        entity.syncIfStateChanged(frackSyncLo(entity), frackSyncHi(entity));
    }

    // P6 anti-sync-spam: 128-битная подпись отрисовываемого состояния (прогресс, индикатор,
    // уровни апгрейдов и все три танка как fluid-фингерпринты). setChanged/sendBlockUpdated
    // только при реальном изменении — контент пакета прежний (весь writeNbtData).
    private static long frackSyncLo(MachineFrackingTowerBlockEntity e) {
        return (e.progressTimer & 0xFFFFFFFFL)
                | ((long) (e.indicator & 0xF)) << 32
                | ((long) (e.speedUpgradeLevel & 0xF)) << 36
                | ((long) (e.powerUpgradeLevel & 0xF)) << 40
                | ((long) (e.afterburnerUpgradeLevel & 0xF)) << 44
                | ((long) (e.overdriveUpgradeLevel & 0xF)) << 48;
    }

    private static long frackSyncHi(MachineFrackingTowerBlockEntity e) {
        return com.hbm_m.recipe.index.CachedRecipeResolver.sign(1L,
                tankFingerprint(e.oilTank), tankFingerprint(e.gasTank), tankFingerprint(e.fracksolTank));
    }

    private static long tankFingerprint(com.hbm_m.inventory.fluid.tank.FluidTank tank) {
        Fluid type = tank.getTankType();
        return com.hbm_m.recipe.index.CachedRecipeResolver.sign(2L,
                type == null ? 0L : type.hashCode(), tank.getFill());
    }

    private static final java.util.Map<com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType, Integer> VALID_UPGRADES_FRACK = java.util.Map.of(
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.SPEED, 3,
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.POWER, 3,
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.AFTERBURN, 3,
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3
    );

    protected void updateUpgrades() {
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_3, VALID_UPGRADES_FRACK);
        speedUpgradeLevel = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.SPEED);
        powerUpgradeLevel = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.POWER);
        afterburnerUpgradeLevel = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.AFTERBURN);
        overdriveUpgradeLevel = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.OVERDRIVE);
    }

    /**
     * Обработка жидкостных контейнеров (канистр и баллонов).
     */
    protected void processFluidContainers() {
        // Original TileEntityOilDrillBase: tanks[0].unloadTank(1, 2, slots); tanks[1].unloadTank(3, 4, slots) - ueber die
        // Loader-Kette, damit auch Kanister/Gasflaschen/Tanks der FluidContainerRegistry (ohne Capability) gefuellt werden.
        ItemStack[] slots = new ItemStack[inventory.getSlots()];
        for (int i = 0; i < slots.length; i++) slots[i] = inventory.getStackInSlot(i);
        boolean changed = oilTank.unloadTank(SLOT_CANISTER_IN, SLOT_CANISTER_OUT, slots);
        changed |= gasTank.unloadTank(SLOT_GAS_IN, SLOT_GAS_OUT, slots);
        if (changed) {
            for (int i : new int[] { SLOT_CANISTER_IN, SLOT_CANISTER_OUT, SLOT_GAS_IN, SLOT_GAS_OUT }) inventory.setStackInSlot(i, slots[i]);
            setChanged();
        }
    }

    /**
     * Проверка возможности работы машины.
     */
    protected boolean canOperate() {
        return oilTank.getSpaceMb() > 0 || gasTank.getSpaceMb() > 0;
    }

    /**
     * Выполнение операции гидроразрыва (добыча).
     * Порт из оригинального onSuck().
     */
    protected void performFrackingOperation() {
        if (level == null || level.isClientSide) return;

        // Потребление энергии
        long energyRequired = getConsumption();
        if (energy < energyRequired) return;

        // Потребление FrackSol
        if (fracksolTank.getFluidAmountMb() < solutionRequired) return;

        // Сначала бурим/прокладываем вертикальную нефтяную трубу вниз,
        // затем работаем только с достигнутым месторождением.
        BlockPos orePos = drillAndFindOilDepositBelow();
        if (orePos == null) {
            indicator = 2; // Нет руды
            return;
        }

        // Потребление ресурсов
        energy -= energyRequired;
        fracksolTank.drainMb(solutionRequired);

        // Добыча
        Block depositBlock = level.getBlockState(orePos).getBlock();

        // NTM Space: Addon-Lagerstaette (BlockOreFluid) -> TileEntityOilDrillBase.onSuck(block, ...) (W-R8B-2)
        com.hbm_m.api.space.SpaceResources.FluidDepositBlock dep = com.hbm_m.api.space.SpaceResources.fluidDepositBlock(depositBlock);
        if (dep != null) {
            oilTank.setTankType(com.hbm_m.blockentity.OilDrillBaseBlockEntity.depositFluid(dep.getPrimaryFluid()));
            gasTank.setTankType(com.hbm_m.blockentity.OilDrillBaseBlockEntity.depositFluid(dep.getSecondaryFluid()));
            oilTank.setFill(Math.min(oilTank.getFill() + dep.getPrimaryFluidAmount(), oilTank.getMaxFill()));
            if (gasTank.getTankType() != ModFluids.NONE.getSource())
                gasTank.setFill(Math.min(gasTank.getFill() + dep.getSecondaryFluidAmount(), gasTank.getMaxFill()));
            dep.drain(level, orePos, 1D);
            indicator = 0;
            return;
        }
        if (com.hbm_m.api.space.SpaceApi.isInstalled()) {
            // Tanks koennen von einer Addon-Lagerstaette umgestellt sein; HBM-Lagerstaetten liefern Rohoel/Erdgas
            oilTank.setTankType(ModFluids.CRUDE_OIL.getSource());
            gasTank.setTankType(ModFluids.GAS.getSource());
        }

        boolean isBedrockDeposit = isBedrockOilDeposit(depositBlock);
        int oil, gas;

        if (isBedrockDeposit) {
            oil = oilPerBedrockDeposit;
            gas = gasPerBedrockDepositMin + level.random.nextInt(
                    gasPerBedrockDepositMax - gasPerBedrockDepositMin + 1);
        } else {
            oil = oilPerDeposit;
            gas = gasPerDepositMin + level.random.nextInt(
                    gasPerDepositMax - gasPerDepositMin + 1);
            applyNormalDepositDrain(orePos);
        }

        if (oil > 0) oilTank.fillMb(ModFluids.CRUDE_OIL.getSource(), oil);
        if (gas > 0) gasTank.fillMb(ModFluids.GAS.getSource(),       gas);

        indicator = 0;
    }

    /**
     * Поиск руды нефти в радиусе действия.
     */
    @Nullable
    protected BlockPos findOilOre() {
        if (level == null) return null;

        int range = 5; // Радиус поиска
        
        for (int y = worldPosition.getY() - 1; y > level.getMinBuildHeight(); y--) {
            for (int x = worldPosition.getX() - range; x <= worldPosition.getX() + range; x++) {
                for (int z = worldPosition.getZ() - range; z <= worldPosition.getZ() + range; z++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    Block block = level.getBlockState(checkPos).getBlock();
                    String blockName = block.getDescriptionId().toLowerCase(java.util.Locale.ROOT);
                    
                    if (blockName.contains("oil") && blockName.contains("ore")) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Пробивает вертикальный ствол трубы OIL_PIPE вниз и возвращает достигнутое нефтяное месторождение.
     */
    @Nullable
    protected BlockPos drillAndFindOilDepositBelow() {
        if (level == null) return null;

        int minY = level.getMinBuildHeight();
        for (int y = worldPosition.getY() - 1; y >= minY; y--) {
            BlockPos checkPos = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
            Block block = level.getBlockState(checkPos).getBlock();

            if (block == ModBlocks.OIL_PIPE.get()) {
                continue;
            }
            if (isAnyOilDeposit(block)) {
                return checkPos;
            }

            if (block.getExplosionResistance() >= 1000.0F) {
                return null;
            }

            level.setBlockAndUpdate(checkPos, ModBlocks.OIL_PIPE.get().defaultBlockState());
            return null;
        }

        return null;
    }

    protected boolean isAnyOilDeposit(Block block) {
        return com.hbm_m.api.space.SpaceResources.fluidDepositBlock(block) != null
                || block == ModBlocks.ORE_OIL.get()
                || block == ModBlocks.ORE_BEDROCK_OIL.get()
                || block == ModBlocks.BEDROCK_OIL.get();
    }

    protected boolean isBedrockOilDeposit(Block block) {
        return block == ModBlocks.ORE_BEDROCK_OIL.get();
    }

    /**
     * Обычное нефтяное месторождение постепенно истощается.
     * Bedrock Oil не истощается (обрабатывается отдельной веткой).
     */
    protected void applyNormalDepositDrain(BlockPos orePos) {
        if (level == null) return;
        if (level.random.nextDouble() < drainChance) {
            level.setBlockAndUpdate(orePos, Blocks.STONE.defaultBlockState());
        }
    }

    /** GIT TileEntityOilDrillBase: the afterburner burns associated gas every tick, not on extraction. */
    protected void burnAfterburnerGas() {
        int toBurn = Math.min(gasTank.getFluidAmountMb(), afterburnerUpgradeLevel * 10);
        if (toBurn <= 0) return;
        gasTank.drainMb(toBurn);
        energy = Math.min(energy + toBurn * 5L, getMaxEnergyStored());
    }

    //=====================================================================================//
    // NBT СЕРИАЛИЗАЦИЯ
    //=====================================================================================//


    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("oilTank", oilTank.writeNBT(new CompoundTag()));
        tag.put("gasTank", gasTank.writeNBT(new CompoundTag()));
        tag.put("fracksolTank", fracksolTank.writeNBT(new CompoundTag()));
        tag.putInt("progressTimer", progressTimer);
        tag.putInt("indicator", indicator);
        tag.putInt("speedUpgrade", speedUpgradeLevel);
        tag.putInt("powerUpgrade", powerUpgradeLevel);
        tag.putInt("afterburnerUpgrade", afterburnerUpgradeLevel);
        tag.putInt("overdriveUpgrade", overdriveUpgradeLevel);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        oilTank.readNBT(tag.getCompound("oilTank"));
        gasTank.readNBT(tag.getCompound("gasTank"));
        fracksolTank.readNBT(tag.getCompound("fracksolTank"));
        progressTimer = tag.getInt("progressTimer");
        indicator = tag.getInt("indicator");
        speedUpgradeLevel = tag.getInt("speedUpgrade");
        powerUpgradeLevel = tag.getInt("powerUpgrade");
        afterburnerUpgradeLevel = tag.getInt("afterburnerUpgrade");
        overdriveUpgradeLevel = tag.getInt("overdriveUpgrade");
    }

    //=====================================================================================//
    // CAPABILITIES
    //=====================================================================================//

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { oilTank, gasTank, fracksolTank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { fracksolTank }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { oilTank, gasTank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(net.minecraft.world.level.material.Fluid fluid, net.minecraft.core.Direction fromDir) {
        return fromDir != null;
    }

    @Override
    protected void setupFluidCapability() {
        setFluidHandler(new FrackingTowerFluidHandler(this));
    }

    //=====================================================================================//
    // РЕНДЕРИНГ
    //=====================================================================================//

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(this.worldPosition)
                .inflate(3.0, 0.0, 3.0)
                .expandTowards(0.0, 24.0, 0.0);
    }


    //=====================================================================================//
    // КОНФИГУРАЦИЯ
    //=====================================================================================//

    public static void loadConfig(MachineConfig.FrackingTowerConfig config) {
        maxPower = config.maxPower;
        consumption = config.consumption;
        solutionRequired = config.solutionRequired;
        delay = config.delay;
        oilPerDeposit = config.oilPerDeposit;
        gasPerDepositMin = config.gasPerDepositMin;
        gasPerDepositMax = config.gasPerDepositMax;
        drainChance = config.drainChance;
        oilPerBedrockDeposit = config.oilPerBedrockDeposit;
        gasPerBedrockDepositMin = config.gasPerBedrockDepositMin;
        gasPerBedrockDepositMax = config.gasPerBedrockDepositMax;
        destructionRange = config.destructionRange;
    }

    /** Original {@code TileEntityOilDrillBase.writeNBT}: Strom und Tanks, sofern nicht leer. */
    @Override
    public void writeNBT(CompoundTag nbt) {
        if (getEnergyStored() == 0 && oilTank.getFill() == 0 && gasTank.getFill() == 0 && fracksolTank.getFill() == 0) return;
        nbt.putLong("energy", getEnergyStored());
        nbt.put("oilTank", oilTank.writeNBT(new CompoundTag()));
        nbt.put("gasTank", gasTank.writeNBT(new CompoundTag()));
        nbt.put("fracksolTank", fracksolTank.writeNBT(new CompoundTag()));
    }
    @Override
    public @org.jetbrains.annotations.Nullable Object getItemHandler(@org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        return this.inventory;
    }
}
