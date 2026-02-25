package com.hbm_m.block.entity.custom.machines;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.config.MachineConfig;
import com.hbm_m.menu.MachineFrackingTowerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

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
public class MachineHydraulicFrackiningTowerBlockEntity extends BaseMachineBlockEntity {

    //=====================================================================================//
    // КОНСТАНТЫ И КОНФИГУРАЦИЯ (из оригинала)
    //=====================================================================================//

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

    protected LazyOptional<IFluidHandler> fluidHandler = LazyOptional.empty();

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

    public MachineHydraulicFrackiningTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_FRACKINING_TOWER_BE.get(), pos, state, 
              INVENTORY_SIZE, maxPower, consumption * 2, 0L);
        
        // Инициализация танков
        this.oilTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid().isSame(ModFluids.CRUDE_OIL.getSource());
            }
        };
        
        this.gasTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid().isSame(ModFluids.GAS.getSource());
            }
        };
        
        this.fracksolTank = new FluidTank(64_000) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid().isSame(ModFluids.FRACKSOL.getSource());
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

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isEnergyItem(stack);
            case SLOT_CANISTER_IN, SLOT_GAS_IN -> isFluidContainer(stack);
            case SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3 -> isUpgradeItem(stack);
            default -> false;
        };
    }

    private boolean isEnergyItem(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
    }

    private boolean isFluidContainer(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
    }

    private boolean isUpgradeItem(ItemStack stack) {
        // Проверка на ItemMachineUpgrade
        String className = stack.getItem().getClass().getSimpleName();
        return className.contains("MachineUpgrade") || className.contains("Upgrade");
    }

    //=====================================================================================//
    // ГЕТТЕРЫ ДЛЯ GUI
    //=====================================================================================//

    public FluidTank getOilTank() {
        return this.oilTank;
    }

    public FluidTank getGasTank() {
        return this.gasTank;
    }

    public FluidTank getFracksolTank() {
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

    public static void tick(Level level, BlockPos pos, BlockState state, MachineHydraulicFrackiningTowerBlockEntity entity) {
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
        if (entity.fracksolTank.getFluidAmount() < solutionRequired) {
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

    /**
     * Обновление уровней апгрейдов из слотов.
     */
    protected void updateUpgrades() {
        speedUpgradeLevel = 0;
        powerUpgradeLevel = 0;
        afterburnerUpgradeLevel = 0;

        for (int i = SLOT_UPGRADE_1; i <= SLOT_UPGRADE_3; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // Определение типа апгрейда по Item
                String itemName = stack.getItem().getDescriptionId().toLowerCase();
                if (itemName.contains("speed")) {
                    speedUpgradeLevel += stack.getCount();
                } else if (itemName.contains("power") || itemName.contains("energy")) {
                    powerUpgradeLevel += stack.getCount();
                } else if (itemName.contains("afterburn") || itemName.contains("overdrive")) {
                    afterburnerUpgradeLevel += stack.getCount();
                }
            }
        }
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
     * Обработка пары слотов ввода/вывода для жидкостей.
     */
    protected void processContainerPair(int inputSlot, int outputSlot, FluidTank sourceTank) {
        ItemStack inputStack = inventory.getStackInSlot(inputSlot);
        ItemStack outputStack = inventory.getStackInSlot(outputSlot);

        if (inputStack.isEmpty()) return;

        inputStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
            // Пытаемся заполнить контейнер из танка
            FluidStack fluidInTank = sourceTank.getFluid();
            if (!fluidInTank.isEmpty() && fluidInTank.getAmount() > 0) {
                int filled = handler.fill(fluidInTank, IFluidHandler.FluidAction.SIMULATE);
                if (filled > 0) {
                    FluidStack toFill = sourceTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    handler.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
                    
                    // Обновляем стек в инвентаре
                    ItemStack result = handler.getContainer();
                    if (outputStack.isEmpty()) {
                        inventory.setStackInSlot(inputSlot, ItemStack.EMPTY);
                        inventory.setStackInSlot(outputSlot, result);
                    } else if (ItemStack.isSameItemSameTags(outputStack, result)) {
                        outputStack.grow(result.getCount());
                        inventory.setStackInSlot(inputSlot, ItemStack.EMPTY);
                    }
                }
            }
        });
    }

    /**
     * Проверка возможности работы машины.
     */
    protected boolean canOperate() {
        // Проверка, что выходные танки не полны
        return oilTank.getSpace() > 0 || gasTank.getSpace() > 0;
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
        if (fracksolTank.getFluidAmount() < solutionRequired) return;

        // Поиск руды в радиусе
        BlockPos orePos = findOilOre();
        if (orePos == null) {
            indicator = 2; // Нет руды
            return;
        }

        // Потребление ресурсов
        energy -= energyRequired;
        fracksolTank.drain(solutionRequired, IFluidHandler.FluidAction.EXECUTE);

        // Добыча
        Block oreBlock = level.getBlockState(orePos).getBlock();
        int oil = 0;
        int gas = 0;

        // Определение количества по типу руды
        String blockName = oreBlock.getDescriptionId().toLowerCase();
        
        if (blockName.contains("bedrock")) {
            // Bedrock oil ore
            oil = oilPerBedrockDeposit;
            gas = gasPerBedrockDepositMin + level.random.nextInt(gasPerBedrockDepositMax - gasPerBedrockDepositMin + 1);
        } else {
            // Обычная oil ore
            oil = oilPerDeposit;
            gas = gasPerDepositMin + level.random.nextInt(gasPerDepositMax - gasPerDepositMin + 1);
            
            // Шанс истощения руды
            if (level.random.nextDouble() < drainChance) {
                // Заменяем на истощённую руду
                // TODO: level.setBlock(orePos, ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState(), 3);
            }
        }

        // Добавление жидкости в танки
        if (oil > 0) {
            FluidStack oilStack = new FluidStack(ModFluids.CRUDE_OIL.getSource(), oil);
            oilTank.fill(oilStack, IFluidHandler.FluidAction.EXECUTE);
        }
        
        if (gas > 0) {
            FluidStack gasStack = new FluidStack(ModFluids.GAS.getSource(), gas);
            gasTank.fill(gasStack, IFluidHandler.FluidAction.EXECUTE);
        }

        // Эффект Afterburner (дополнительный урон/бонус)
        if (afterburnerUpgradeLevel > 0) {
            applyAfterburnerEffect(orePos);
        }

        // Генерация OilSpot (разрушение области)
        // TODO: OilSpot.generateOilSpot(level, orePos.getX(), orePos.getZ(), destructionRange, 10, false);
        
        indicator = 0; // Норма
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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("oilTank", oilTank.writeToNBT(new CompoundTag()));
        tag.put("gasTank", gasTank.writeToNBT(new CompoundTag()));
        tag.put("fracksolTank", fracksolTank.writeToNBT(new CompoundTag()));
        tag.putInt("progressTimer", progressTimer);
        tag.putInt("indicator", indicator);
        tag.putInt("speedUpgrade", speedUpgradeLevel);
        tag.putInt("powerUpgrade", powerUpgradeLevel);
        tag.putInt("afterburnerUpgrade", afterburnerUpgradeLevel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        oilTank.readFromNBT(tag.getCompound("oilTank"));
        gasTank.readFromNBT(tag.getCompound("gasTank"));
        fracksolTank.readFromNBT(tag.getCompound("fracksolTank"));
        progressTimer = tag.getInt("progressTimer");
        indicator = tag.getInt("indicator");
        speedUpgradeLevel = tag.getInt("speedUpgrade");
        powerUpgradeLevel = tag.getInt("powerUpgrade");
        afterburnerUpgradeLevel = tag.getInt("afterburnerUpgrade");
    }

    //=====================================================================================//
    // CAPABILITIES
    //=====================================================================================//

    @Override
    protected void setupFluidCapability() {
        fluidHandler = LazyOptional.of(() -> new FrackingTowerFluidHandler(this));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }

    //=====================================================================================//
    // РЕНДЕРИНГ
    //=====================================================================================//

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        // Контроллер находится на полу (y=0) в центре структуры 7х7.
        // Вышка имеет ширину 7 блоков (от -3 до +3 относительно контроллера).
        // Высота башни — 24 блока (от 0 до +24).
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
