package com.hbm_m.examples;

/**
 * ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ ЭНЕРГОСИСТЕМЫ HBM-M
 * 
 * Этот файл содержит примеры кода для работы с энергосистемой.
 * НЕ компилируется как часть мода - только для справки!
 */

import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.energy.ItemEnergyStorage;
import com.hbm_m.energy.WireNetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class EnergySystemExamples {

    // ======================
    // ПРИМЕР 1: Простой генератор энергии
    // ======================
    public static class SimpleGeneratorBlockEntity extends BlockEntity {
        
        // Создаем хранилище энергии
        // 10000 FE емкость, не принимает энергию (0), отдает до 100 FE/тик
        private final BlockEntityEnergyStorage energy = 
            new BlockEntityEnergyStorage(10000L, 0L, 100L);
        
        private final LazyOptional<ILongEnergyStorage> energyCap = 
            LazyOptional.of(() -> energy);
        
        private int burnTime = 0;
        private int maxBurnTime = 0;
        
        public SimpleGeneratorBlockEntity(BlockPos pos, BlockState state) {
            super(null, pos, state); // ModBlockEntities.GENERATOR.get()
        }
        
        // Серверный тик - вызывается каждый игровой тик
        public static void serverTick(Level level, BlockPos pos, 
                                      BlockState state, SimpleGeneratorBlockEntity be) {
            boolean wasLit = be.burnTime > 0;
            
            if (be.burnTime > 0) {
                be.burnTime--;
                // Генерируем 20 FE за тик работы
                be.energy.receiveEnergy(20L, false);
            }
            
            // Раздаем энергию соседям
            be.pushEnergyToNeighbors();
            
            // Обновляем состояние блока если нужно
            if (wasLit != (be.burnTime > 0)) {
                level.setBlock(pos, state.setValue(/* LIT property */, be.burnTime > 0), 3);
            }
        }
        
        // Метод для раздачи энергии соседним блокам
        private void pushEnergyToNeighbors() {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                
                if (neighbor != null) {
                    // Пытаемся получить энергокапабилити соседа
                    neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite())
                        .ifPresent(storage -> {
                            if (storage.canReceive()) {
                                // Извлекаем до 100 FE из нашего хранилища
                                long extracted = energy.extractEnergy(100L, false);
                                if (extracted > 0) {
                                    // Пытаемся отдать соседу
                                    long received = storage.receiveEnergy(extracted, false);
                                    // Возвращаем то, что сосед не принял
                                    if (received < extracted) {
                                        energy.receiveEnergy(extracted - received, false);
                                    }
                                }
                            }
                        });
                }
            }
        }
        
        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == ModCapabilities.LONG_ENERGY) {
                return energyCap.cast();
            }
            return super.getCapability(cap, side);
        }
        
        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putLong("Energy", energy.getEnergyStored());
            tag.putInt("BurnTime", burnTime);
            tag.putInt("MaxBurnTime", maxBurnTime);
        }
        
        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            energy.setEnergy(tag.getLong("Energy"));
            burnTime = tag.getInt("BurnTime");
            maxBurnTime = tag.getInt("MaxBurnTime");
        }
    }
    
    // ======================
    // ПРИМЕР 2: Машина-потребитель энергии
    // ======================
    public static class EnergyConsumerMachineBlockEntity extends BlockEntity {
        
        // Большая емкость, принимает до 1000 FE/тик, не отдает (0)
        private final BlockEntityEnergyStorage energy = 
            new BlockEntityEnergyStorage(50000L, 1000L, 0L);
        
        private final LazyOptional<ILongEnergyStorage> energyCap = 
            LazyOptional.of(() -> energy);
        
        private int progress = 0;
        private static final int MAX_PROGRESS = 200;
        private static final long ENERGY_PER_TICK = 50L;
        
        public EnergyConsumerMachineBlockEntity(BlockPos pos, BlockState state) {
            super(null, pos, state);
        }
        
        public static void serverTick(Level level, BlockPos pos, 
                                      BlockState state, EnergyConsumerMachineBlockEntity be) {
            
            if (be.canWork()) {
                // Проверяем, хватает ли энергии
                if (be.energy.getEnergyStored() >= ENERGY_PER_TICK) {
                    // Потребляем энергию
                    be.energy.extractEnergy(ENERGY_PER_TICK, false);
                    
                    // Увеличиваем прогресс
                    be.progress++;
                    
                    if (be.progress >= MAX_PROGRESS) {
                        be.finishWork();
                        be.progress = 0;
                    }
                }
            } else {
                // Сбрасываем прогресс если не можем работать
                be.progress = 0;
            }
        }
        
        private boolean canWork() {
            // Проверяем условия работы: есть ли входные предметы, место для выхода и т.д.
            return true; // Упрощенно
        }
        
        private void finishWork() {
            // Завершаем процесс обработки: создаем выходные предметы и т.д.
        }
        
        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == ModCapabilities.LONG_ENERGY) {
                return energyCap.cast();
            }
            return super.getCapability(cap, side);
        }
        
        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putLong("Energy", energy.getEnergyStored());
            tag.putInt("Progress", progress);
        }
        
        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            energy.setEnergy(tag.getLong("Energy"));
            progress = tag.getInt("Progress");
        }
    }
    
    // ======================
    // ПРИМЕР 3: Машина с запросом энергии из сети проводов
    // ======================
    public static class WirePoweredMachineBlockEntity extends BlockEntity {
        
        private int progress = 0;
        private static final int MAX_PROGRESS = 100;
        private static final long ENERGY_PER_TICK = 100L;
        
        public WirePoweredMachineBlockEntity(BlockPos pos, BlockState state) {
            super(null, pos, state);
        }
        
        public static void serverTick(Level level, BlockPos pos, 
                                      BlockState state, WirePoweredMachineBlockEntity be) {
            
            if (be.canWork()) {
                // Запрашиваем энергию из сети проводов
                long received = WireNetworkManager.get()
                    .requestEnergy(level, pos, ENERGY_PER_TICK, false);
                
                if (received >= ENERGY_PER_TICK) {
                    be.progress++;
                    
                    if (be.progress >= MAX_PROGRESS) {
                        be.finishWork();
                        be.progress = 0;
                    }
                }
            } else {
                be.progress = 0;
            }
        }
        
        private boolean canWork() {
            return true; // Упрощенно
        }
        
        private void finishWork() {
            // Завершаем работу
        }
        
        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putInt("Progress", progress);
        }
        
        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            progress = tag.getInt("Progress");
        }
    }
    
    // ======================
    // ПРИМЕР 4: Энергохранящий предмет (батарея)
    // ======================
    public static class BatteryItem extends Item {
        
        public static final long CAPACITY = 1000000L;
        public static final long MAX_TRANSFER = 10000L;
        
        public BatteryItem(Properties props) {
            super(props.stacksTo(1));
        }
        
        @Override
        public <T> LazyOptional<T> getCapability(ItemStack stack, Capability<T> cap) {
            if (cap == ModCapabilities.LONG_ENERGY && !stack.isEmpty()) {
                ItemEnergyStorage storage = new ItemEnergyStorage(
                    stack, 
                    CAPACITY,      // емкость
                    MAX_TRANSFER,  // макс. прием
                    MAX_TRANSFER   // макс. извлечение
                );
                return LazyOptional.of(() -> storage).cast();
            }
            return LazyOptional.empty();
        }
        
        // Показываем уровень заряда в подсказке
        /*
        @Override
        public void appendHoverText(ItemStack stack, Level level, 
                                   List<Component> tooltip, TooltipFlag flag) {
            stack.getCapability(ModCapabilities.LONG_ENERGY)
                .ifPresent(energy -> {
                    long stored = energy.getEnergyStored();
                    long max = energy.getMaxEnergyStored();
                    tooltip.add(Component.literal(
                        String.format("Energy: %,d / %,d FE", stored, max)
                    ));
                });
        }
        */
    }
    
    // ======================
    // ПРИМЕР 5: Универсальная машина (и генератор, и потребитель)
    // ======================
    public static class UniversalMachineBlockEntity extends BlockEntity {
        
        // Может и принимать, и отдавать энергию
        private final BlockEntityEnergyStorage energy = 
            new BlockEntityEnergyStorage(100000L, 1000L, 1000L);
        
        private final LazyOptional<ILongEnergyStorage> energyCap = 
            LazyOptional.of(() -> energy);
        
        public UniversalMachineBlockEntity(BlockPos pos, BlockState state) {
            super(null, pos, state);
        }
        
        public static void serverTick(Level level, BlockPos pos, 
                                      BlockState state, UniversalMachineBlockEntity be) {
            
            // В режиме генератора - производим энергию
            if (be.isGeneratorMode()) {
                be.energy.receiveEnergy(50L, false);
            }
            
            // В режиме потребителя - используем энергию
            if (be.isConsumerMode() && be.energy.getEnergyStored() >= 100L) {
                be.energy.extractEnergy(100L, false);
                be.doWork();
            }
            
            // Всегда пытаемся балансировать энергию с соседями
            be.balanceEnergyWithNeighbors();
        }
        
        private boolean isGeneratorMode() {
            return true; // Зависит от конфигурации
        }
        
        private boolean isConsumerMode() {
            return true; // Зависит от конфигурации
        }
        
        private void doWork() {
            // Выполняем работу
        }
        
        private void balanceEnergyWithNeighbors() {
            long ourEnergy = energy.getEnergyStored();
            long capacity = energy.getMaxEnergyStored();
            
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                
                if (neighbor != null) {
                    neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite())
                        .ifPresent(storage -> {
                            long neighborEnergy = storage.getEnergyStored();
                            long neighborCapacity = storage.getMaxEnergyStored();
                            
                            // Если у нас энергии больше в процентном отношении - отдаем
                            double ourRatio = (double)ourEnergy / capacity;
                            double neighborRatio = (double)neighborEnergy / neighborCapacity;
                            
                            if (ourRatio > neighborRatio + 0.1 && storage.canReceive()) {
                                long toTransfer = Math.min(100L, ourEnergy / 10);
                                long extracted = energy.extractEnergy(toTransfer, false);
                                long received = storage.receiveEnergy(extracted, false);
                                
                                if (received < extracted) {
                                    energy.receiveEnergy(extracted - received, false);
                                }
                            }
                        });
                }
            }
        }
        
        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == ModCapabilities.LONG_ENERGY) {
                return energyCap.cast();
            }
            return super.getCapability(cap, side);
        }
        
        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putLong("Energy", energy.getEnergyStored());
        }
        
        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            energy.setEnergy(tag.getLong("Energy"));
        }
    }
    
    // ======================
    // ПРИМЕР 6: Утилиты для работы с энергией
    // ======================
    public static class EnergyUtils {
        
        /**
         * Проверяет, есть ли у блока энергокапабилити
         */
        public static boolean hasEnergyCapability(BlockEntity be, Direction side) {
            if (be == null) return false;
            return be.getCapability(ModCapabilities.LONG_ENERGY, side).isPresent();
        }
        
        /**
         * Получает текущую энергию блока
         */
        public static long getEnergy(BlockEntity be, Direction side) {
            if (be == null) return 0L;
            return be.getCapability(ModCapabilities.LONG_ENERGY, side)
                .map(ILongEnergyStorage::getEnergyStored)
                .orElse(0L);
        }
        
        /**
         * Передает энергию от одного блока к другому
         */
        public static long transferEnergy(BlockEntity from, Direction fromSide,
                                         BlockEntity to, Direction toSide,
                                         long maxAmount) {
            if (from == null || to == null) return 0L;
            
            LazyOptional<ILongEnergyStorage> fromCap = 
                from.getCapability(ModCapabilities.LONG_ENERGY, fromSide);
            LazyOptional<ILongEnergyStorage> toCap = 
                to.getCapability(ModCapabilities.LONG_ENERGY, toSide);
            
            if (fromCap.isPresent() && toCap.isPresent()) {
                ILongEnergyStorage fromStorage = fromCap.resolve().get();
                ILongEnergyStorage toStorage = toCap.resolve().get();
                
                if (fromStorage.canExtract() && toStorage.canReceive()) {
                    long extracted = fromStorage.extractEnergy(maxAmount, false);
                    if (extracted > 0) {
                        long received = toStorage.receiveEnergy(extracted, false);
                        
                        // Возвращаем неиспользованное
                        if (received < extracted) {
                            fromStorage.receiveEnergy(extracted - received, false);
                        }
                        
                        return received;
                    }
                }
            }
            
            return 0L;
        }
        
        /**
         * Форматирует число энергии для отображения
         */
        public static String formatEnergy(long energy) {
            if (energy < 1000) {
                return energy + " FE";
            } else if (energy < 1000000) {
                return String.format("%.1f kFE", energy / 1000.0);
            } else if (energy < 1000000000) {
                return String.format("%.1f MFE", energy / 1000000.0);
            } else {
                return String.format("%.1f GFE", energy / 1000000000.0);
            }
        }
    }
}
