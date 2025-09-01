package com.hbm_m.block.entity;

import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.menu.AdvancedAssemblyMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;


public class AdvancedAssemblyMachineBlockEntity extends BlockEntity implements MenuProvider {
private final ItemStackHandler itemHandler = new ItemStackHandler(17) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100_000, 1000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
            return received;
        }
    };

    private final FluidTank inputTank = new FluidTank(4000);
    private final FluidTank outputTank = new FluidTank(4000);

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<BlockEntityEnergyStorage> lazyEnergyHandler = LazyOptional.empty(); // Тип изменен
    private LazyOptional<FluidTank> lazyFluidHandler = LazyOptional.empty();
    
    // --- ЛОГИКА ПРОЦЕССОВ ---
    // TODO: Замените это на вашу систему рецептов и модуль обработки
    // public ModuleMachineAssembler assemblerModule;
    public int progress = 0;
    public int maxProgress = 100; // Пример
    public boolean isCrafting = false;

    // --- ЛОГИКА АНИМАЦИЙ (КЛИЕНТ) ---
    public final AssemblerArm[] arms = new AssemblerArm[2];
    public float ringAngle;
    public float prevRingAngle;
    private float ringTarget;
    private float ringSpeed;
    private int ringDelay;

    protected final ContainerData data;

    public AdvancedAssemblyMachineBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE.get(), pPos, pBlockState); // Укажите ваш тип BlockEntity
        for (int i = 0; i < this.arms.length; i++) {
            this.arms[i] = new AssemblerArm();
        }

        this.data = new ContainerData() {

            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> AdvancedAssemblyMachineBlockEntity.this.progress;
                    case 1 -> AdvancedAssemblyMachineBlockEntity.this.maxProgress;
                    // Можно добавить еще 2 поля, например, для энергии, если нужно
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> AdvancedAssemblyMachineBlockEntity.this.progress = pValue;
                    case 1 -> AdvancedAssemblyMachineBlockEntity.this.maxProgress = pValue;
                }
            }

            @Override
            public int getCount() {
                return 2; // Или 4, если добавите энергию
            }
        };
    }

    public int getEnergyStored() { return this.energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return this.energyStorage.getMaxEnergyStored(); }
    public boolean isCrafting() { return this.isCrafting; }
    public int getProgress() { return this.progress; }
    public int getMaxProgress() { return this.maxProgress; }

    // --- TICK ЛОГИКА ---
    public static void tick(Level level, BlockPos pos, BlockState state, AdvancedAssemblyMachineBlockEntity pEntity) {
        if (level.isClientSide()) {
            pEntity.clientTick();
        } else {
            pEntity.serverTick();
        }
    }

    private void serverTick() {
        // TODO: Перенести сюда логику из assemblerModule.update()
        // 1. Проверить наличие рецепта (по чертежу в слоте 1)
        // 2. Проверить наличие ресурсов (предметы, энергия, жидкость)
        // 3. Если все есть, начать крафт (isCrafting = true)
        
        if (isCrafting) {
            // Потреблять энергию
            energyStorage.extractEnergy(100, false); // Примерное значение
            progress++;
            setChanged();
            
            if (progress >= maxProgress) {
                craftItem();
                progress = 0;
                isCrafting = false;
                // Отправить обновление клиенту, чтобы остановить анимацию
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        } else {
            // Попытаться начать новый крафт
            // ... ваша логика ...
            // Для теста можно сделать так:
            if (energyStorage.getEnergyStored() > 1000) {
                 isCrafting = true;
                 // Отправить обновление клиенту, чтобы запустить анимацию
                 level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    
    private void craftItem() {
        // TODO: Реализуйте логику создания предмета
        // Например: this.itemHandler.insertItem(16, new ItemStack(Items.DIAMOND), false);
        setChanged();
    }

    private void clientTick() {
        this.prevRingAngle = this.ringAngle;

        // Обновление анимации рук
        for (AssemblerArm arm : arms) {
            arm.updateInterp();
            if (isCrafting) {
                arm.updateArm(level.random);
            } else {
                arm.returnToNullPos();
            }
        }
        
        // Обновление анимации кольца
        if (isCrafting) {
            if (this.ringAngle != this.ringTarget) {
                float ringDelta = Mth.wrapDegrees(this.ringTarget - this.ringAngle);
                if (Math.abs(ringDelta) <= this.ringSpeed) {
                    this.ringAngle = this.ringTarget;
                } else {
                    this.ringAngle += Math.signum(ringDelta) * this.ringSpeed;
                }
                
                if (this.ringAngle == this.ringTarget) {
                    this.ringDelay = 20 + level.random.nextInt(21);
                }
            } else {
                if (this.ringDelay > 0) this.ringDelay--;
                if (this.ringDelay <= 0) {
                    this.ringTarget += (level.random.nextFloat() * 2 - 1) * 135;
                    this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
                }
            }
        } else {
            // Плавно вернуть кольцо в исходное положение, если не крафтит
            this.ringAngle = Mth.lerp(0.1f, this.ringAngle, 0);
        }
    }
    
    // --- CAPABILITIES ---
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        // TODO: Если у вас разные танки для входа/выхода с разных сторон, нужна более сложная логика
        lazyFluidHandler = LazyOptional.of(() -> inputTank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    // --- СОХРАНЕНИЕ И СИНХРОНИЗАЦИЯ ДАННЫХ ---
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        nbt.putInt("energy", energyStorage.getEnergyStored());
        nbt.put("input_tank", inputTank.writeToNBT(new CompoundTag()));
        nbt.put("output_tank", outputTank.writeToNBT(new CompoundTag()));
        nbt.putInt("progress", progress);
        
        // ===== ИСПРАВЛЕНИЕ: ДОБАВЛЯЕМ СОХРАНЕНИЕ ФЛАГА =====
        nbt.putBoolean("is_crafting", this.isCrafting);

        super.saveAdditional(nbt);
    }

    // Этот метод теперь будет использоваться и при загрузке чанка, и при получении пакета
    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        energyStorage.setEnergy(nbt.getInt("energy"));
        inputTank.readFromNBT(nbt.getCompound("input_tank"));
        outputTank.readFromNBT(nbt.getCompound("output_tank"));
        progress = nbt.getInt("progress");
        
        // Сохраняем предыдущее состояние для сравнения
        boolean wasCrafting = this.isCrafting;
        this.isCrafting = nbt.getBoolean("is_crafting");

        // --- ДИАГНОСТИКА ---
        // this.level может быть null при первой загрузке, поэтому проверяем
        if (this.level != null && this.level.isClientSide() && wasCrafting != this.isCrafting) {
            System.out.println("[КЛИЕНТ] Получено обновление! isCrafting теперь: " + this.isCrafting);
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        // Получаем NBT-тег из пакета. Это тот самый тег, который был создан 
        // на сервере с помощью saveAdditional().
        CompoundTag nbt = pkt.getTag();
        if (nbt != null) {
            // Запоминаем старое значение для диагностики
            boolean wasCrafting = this.isCrafting;
            
            // Вызываем стандартный обработчик, который в свою очередь вызовет наш метод load()
            // Это гарантирует, что все данные из NBT будут загружены как положено.
            this.handleUpdateTag(nbt);
            
            // Наша диагностика. Теперь она должна сработать.
            if (this.level != null && this.level.isClientSide() && wasCrafting != this.isCrafting) {
                System.out.println("[КЛИЕНТ onDataPacket] Пакет успешно получен! isCrafting теперь: " + this.isCrafting);
            }
        }
        
        // ВАЖНО: Вызываем super, чтобы ванильный код тоже мог обработать пакет, если ему нужно
        super.onDataPacket(net, pkt);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- GUI ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.advanced_assembly_machine"); // Ключ для локализации
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new AdvancedAssemblyMachineMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    // --- ВНУТРЕННИЙ КЛАСС ДЛЯ АНИМАЦИИ РУК-МАНИПУЛЯТОРОВ ---
    public static class AssemblerArm {
        public float[] angles = new float[4];
        public float[] prevAngles = new float[4];
        private float[] targetAngles = new float[4];
        private float[] speed = new float[4];

        private ArmActionState state = ArmActionState.ASSUME_POSITION;
        private int actionDelay = 0;

        private enum ArmActionState {
            ASSUME_POSITION, EXTEND_STRIKER, RETRACT_STRIKER
        }

        public AssemblerArm() {
            resetSpeed();
        }
        
        public void updateInterp() {
            System.arraycopy(angles, 0, prevAngles, 0, angles.length);
        }

        public void returnToNullPos() {
            for (int i = 0; i < 4; i++) this.targetAngles[i] = 0;
            for (int i = 0; i < 3; i++) this.speed[i] = 3;
            this.speed[3] = 0.25f;
            this.state = ArmActionState.RETRACT_STRIKER;
            this.move();
        }
        
        private void resetSpeed() {
            speed[0] = 15; speed[1] = 15; speed[2] = 15; speed[3] = 0.5f;
        }

        public void updateArm(RandomSource random) {
            resetSpeed();
            if (actionDelay > 0) {
                actionDelay--;
                return;
            }

            switch (state) {
                case ASSUME_POSITION:
                    if (move()) {
                        actionDelay = 2;
                        state = ArmActionState.EXTEND_STRIKER;
                        targetAngles[3] = -0.75f;
                    }
                    break;
                case EXTEND_STRIKER:
                    if (move()) {
                        state = ArmActionState.RETRACT_STRIKER;
                        targetAngles[3] = 0f;
                    }
                    break;
                case RETRACT_STRIKER:
                    if (move()) {
                        actionDelay = 2 + random.nextInt(5);
                        chooseNewArmPosition(random);
                        state = ArmActionState.ASSUME_POSITION;
                    }
                    break;
            }
        }

        private static final float[][] POSITIONS = new float[][]{
                {45, -15, -5}, {15, 15, -15}, {25, 10, -15}, {30, 0, -10}, {70, -10, -25},
        };

        public void chooseNewArmPosition(RandomSource random) { 
            int chosen = random.nextInt(POSITIONS.length);
            this.targetAngles[0] = POSITIONS[chosen][0];
            this.targetAngles[1] = POSITIONS[chosen][1];
            this.targetAngles[2] = POSITIONS[chosen][2];
        }

        private boolean move() {
            boolean didMove = false;
            for (int i = 0; i < angles.length; i++) {
                if (angles[i] == targetAngles[i]) continue;
                didMove = true;
                float delta = Math.abs(angles[i] - targetAngles[i]);
                if (delta <= speed[i]) {
                    angles[i] = targetAngles[i];
                    continue;
                }
                angles[i] += Math.signum(targetAngles[i] - angles[i]) * speed[i];
            }
            return !didMove;
        }
    }
}