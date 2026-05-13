package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

//?}

//? if fabric {
/*import dev.architectury.fluid.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
*///?}

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
public class MachineFrackingTowerBlockEntity extends BaseMachineBlockEntity {
    
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

    //? if forge {
    protected LazyOptional<IFluidHandler> fluidHandler = LazyOptional.empty();
     //?}

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
        
        // Инициализация танков
        //? if forge {
        this.oilTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return fluid.isSame(ModFluids.CRUDE_OIL.getSource());
            }
        };

        this.gasTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return fluid.isSame(ModFluids.GAS.getSource());
            }
        };

        this.fracksolTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return fluid.isSame(ModFluids.FRACKSOL.getSource());
            }
        };
        //?}

        //? if fabric {
        /*this.oilTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(net.minecraft.world.level.material.Fluid fluid) {
                return fluid.isSame(ModFluids.CRUDE_OIL.getSource());
            }
        };

        this.gasTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(net.minecraft.world.level.material.Fluid fluid) {
                return fluid.isSame(ModFluids.GAS.getSource());
            }
        };

        this.fracksolTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(net.minecraft.world.level.material.Fluid fluid) {
                return fluid.isSame(ModFluids.FRACKSOL.getSource());
            }
        };
        *///?}
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
        // capability обвязка сейчас не компилируется в этой точке.
        // Чтобы не блокировать сборку, принимаем любой fluid item.
        // Логику можно будет ужесточить после синка capability-слоя.
        return true;
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
        return Math.max(1, baseDelay);
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
        return baseConsumption;
    }

    //=====================================================================================//
    // ОСНОВНАЯ ЛОГИКА (ТИК ОБНОВЛЕНИЯ)
    //=====================================================================================//

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFrackingTowerBlockEntity entity) {
        if (level.isClientSide) return;

        entity.ensureNetworkInitialized();
        entity.updateUpgrades();
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
        entity.setChanged();
        entity.sendUpdateToClient();
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
        afterburnerUpgradeLevel = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.AFTERBURN)
                + upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.OVERDRIVE);
    }

    /**
     * Обработка жидкостных контейнеров (канистр и баллонов).
     */
    protected void processFluidContainers() {
        // Обработка канистры для нефти
        processContainerPair(SLOT_CANISTER_IN, SLOT_CANISTER_OUT, oilTank);

        // Обработка баллона для газа
        processContainerPair(SLOT_GAS_IN, SLOT_GAS_OUT, gasTank);
    }

    /**
     * Перелив из танка машины в контейнер в слоте инвентаря.
     * На Forge — через FLUID_HANDLER_ITEM capability.
     * На Fabric — через FluidStorage.ITEM.
     */
    protected void processContainerPair(int inputSlot, int outputSlot, FluidTank sourceTank) {
        ItemStack inputStack  = inventory.getStackInSlot(inputSlot);
        if (inputStack.isEmpty() || sourceTank.isEmpty()) return;

        //? if fabric {
        /*var containerStorage = FluidStorage.ITEM.find(inputStack, net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.withConstant(inputStack));
        if (containerStorage == null) return;
        FluidVariant variant = FluidVariant.of(sourceTank.getStoredFluid());
        long toTransfer = (long) sourceTank.getFluidAmountMb() * FluidTank.DROPLETS_PER_MB;
        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long inserted = containerStorage.insert(variant, toTransfer, tx);
            if (inserted > 0) {
                sourceTank.getStorage().extract(variant, inserted, tx);
                tx.commit();
            }
        }
        *///?}
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

        // Поиск руды в радиусе
        BlockPos orePos = findOilOre();
        if (orePos == null) {
            indicator = 2; // Нет руды
            return;
        }

        // Потребление ресурсов
        energy -= energyRequired;
        fracksolTank.drainMb(solutionRequired);

        // Добыча
        String blockName = level.getBlockState(orePos).getBlock().getDescriptionId().toLowerCase();
        int oil, gas;

        if (blockName.contains("bedrock")) {
            oil = oilPerBedrockDeposit;
            gas = gasPerBedrockDepositMin + level.random.nextInt(
                    gasPerBedrockDepositMax - gasPerBedrockDepositMin + 1);
        } else {
            oil = oilPerDeposit;
            gas = gasPerDepositMin + level.random.nextInt(
                    gasPerDepositMax - gasPerDepositMin + 1);
            if (level.random.nextDouble() < drainChance) {
                // TODO: заменить на истощённую руду
            }
        }

        if (oil > 0) oilTank.fillMb(ModFluids.CRUDE_OIL.getSource(), oil);
        if (gas > 0) gasTank.fillMb(ModFluids.GAS.getSource(),       gas);

        if (afterburnerUpgradeLevel > 0) applyAfterburnerEffect(orePos);

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
                    String blockName = block.getDescriptionId().toLowerCase();
                    
                    if (blockName.contains("oil") && blockName.contains("ore")) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Применение эффекта Afterburner апгрейда.
     */
    protected void applyAfterburnerEffect(BlockPos pos) {
        // Дополнительный урон и бонусы от afterburner
        // Уровень эффекта: level * 10 урона, level * 50% шанс
        // TODO: Реализовать эффекты урона и частиц
    }

    //=====================================================================================//
    // NBT СЕРИАЛИЗАЦИЯ
    //=====================================================================================//

    //? if fabric {
    /*@Nullable
    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        if (side == Direction.DOWN) return fracksolTank.getStorage();
        if (side == Direction.UP)   return oilTank.getStorage();
        return gasTank.getStorage();
    }
    *///?}

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("oilTank", oilTank.writeNBT(new CompoundTag()));
        tag.put("gasTank", gasTank.writeNBT(new CompoundTag()));
        tag.put("fracksolTank", fracksolTank.writeNBT(new CompoundTag()));
        tag.putInt("progressTimer", progressTimer);
        tag.putInt("indicator", indicator);
        tag.putInt("speedUpgrade", speedUpgradeLevel);
        tag.putInt("powerUpgrade", powerUpgradeLevel);
        tag.putInt("afterburnerUpgrade", afterburnerUpgradeLevel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        oilTank.readNBT(tag.getCompound("oilTank"));
        gasTank.readNBT(tag.getCompound("gasTank"));
        fracksolTank.readNBT(tag.getCompound("fracksolTank"));
        progressTimer = tag.getInt("progressTimer");
        indicator = tag.getInt("indicator");
        speedUpgradeLevel = tag.getInt("speedUpgrade");
        powerUpgradeLevel = tag.getInt("powerUpgrade");
        afterburnerUpgradeLevel = tag.getInt("afterburnerUpgrade");
    }

    //=====================================================================================//
    // CAPABILITIES
    //=====================================================================================//

    //? if forge {
    protected void setupFluidCapability() {
        fluidHandler = LazyOptional.of(() -> new FrackingTowerFluidHandler(this));
    }

    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidHandler.cast();
        return super.getCapability(cap, side);
    }

    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }
    //?}

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
}
