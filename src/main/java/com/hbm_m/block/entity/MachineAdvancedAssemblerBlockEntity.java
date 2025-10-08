package com.hbm_m.block.entity;

import com.hbm_m.client.ClientSoundManager;
// Блок-энтити для Продвинутой Сборочной Машины с поддержкой энергии, жидкостей, предметов и анимаций.
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.menu.MachineAdvancedAssemblerMenu;
import com.hbm_m.multiblock.IFrameSupportable;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.sound.AdvancedAssemblerSoundInstance;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.main.MainRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MachineAdvancedAssemblerBlockEntity extends BlockEntity implements MenuProvider, IFrameSupportable {
    private final ItemStackHandler itemHandler = new ItemStackHandler(17) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // КРИТИЧНО: Синхронизируем изменения в слотах с клиентом
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100_000, 1000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
                // Также не вызываем sendBlockUpdated
            }
            return extracted;
        }
    };

    private final FluidTank inputTank = new FluidTank(4000);
    private final FluidTank outputTank = new FluidTank(4000);

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<BlockEntityEnergyStorage> lazyEnergyHandler = LazyOptional.empty();
    private LazyOptional<FluidTank> lazyFluidHandler = LazyOptional.empty();
    public boolean frame = false;
    private ResourceLocation selectedRecipeId = null;
    
    private int syncCounter = 0;
    private int lastSyncedEnergy = 0;

    @OnlyIn(Dist.CLIENT)
    private AdvancedAssemblerSoundInstance soundInstance;
    
    // --- ЛОГИКА ПРОЦЕССОВ ---
    // TODO: Заменить это на систему рецептов и модуль обработки
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

    public MachineAdvancedAssemblerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), pPos, pBlockState);
        for (int i = 0; i < this.arms.length; i++) {
            this.arms[i] = new AssemblerArm();
        }

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> MachineAdvancedAssemblerBlockEntity.this.progress;
                    case 1 -> MachineAdvancedAssemblerBlockEntity.this.maxProgress;
                    case 2 -> MachineAdvancedAssemblerBlockEntity.this.energyStorage.getEnergyStored();
                    case 3 -> MachineAdvancedAssemblerBlockEntity.this.energyStorage.getMaxEnergyStored();
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> MachineAdvancedAssemblerBlockEntity.this.progress = pValue;
                    case 1 -> MachineAdvancedAssemblerBlockEntity.this.maxProgress = pValue;
                    case 2 -> MachineAdvancedAssemblerBlockEntity.this.energyStorage.setEnergy(pValue);
                    case 3 -> {} // maxEnergy - только для чтения
                }
            }

            @Override
            public int getCount() {
                return 4; // Теперь 4 поля: progress, maxProgress, energy, maxEnergy
            }
        };
    }

    public int getEnergyStored() { return this.energyStorage.getEnergyStored(); }
    public int getMaxEnergyStored() { return this.energyStorage.getMaxEnergyStored(); }
    public boolean isCrafting() { return this.isCrafting; }
    public int getProgress() { return this.progress; }
    public int getMaxProgress() { return this.maxProgress; }
    public long lastUseTick = 0;

    /**
     * Этот метод устанавливает состояние видимости рамки и, если оно изменилось,
     * синхронизирует его с клиентом. Это центральная точка обновления.
     */
    @Override
    public boolean setFrameVisible(boolean visible) {
        // Проверяем, нужно ли вообще что-то менять
        if (this.frame != visible) {
            this.frame = visible;
            this.setChanged(); // Помечаем BlockEntity как "грязный" для сохранения в чанке

            // Убеждаемся, что у нас есть мир и мы на сервере, прежде чем отправлять пакет
            if (this.level != null && !this.level.isClientSide()) {
                // Отправляем пакет обновления клиенту. Это заставит клиент запросить getUpdateTag()
                // и обновить свой BlockEntity, включая поле 'frame'.
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
                MainRegistry.LOGGER.debug("[FRAME SET] Состояние рамки изменено на " + visible + ". Отправлен пакет клиенту.");
            }
            return true; // Состояние изменилось
        }
        return false; // Состояние не изменилось
    }

    /**
     * ИЗМЕНЕНИЕ: Новый метод из интерфейса.
     * Просто возвращает текущее состояние поля 'frame'.
     * Используется на клиенте рендерером для определения, рисовать ли рамку.
     */
    @Override
    public boolean isFrameVisible() {
        return this.frame;
    }

    @Override
    public void checkForFrame() {
        // Запускаем проверку только на сервере
        if (this.level != null && !this.level.isClientSide()) {
            MainRegistry.LOGGER.debug("[FRAME CHECK] Запущена проверка checkForFrame() для контроллера на " + this.worldPosition);
            MultiblockStructureHelper.updateFrameForController(this.level, this.worldPosition);
        }
    }

    // --- TICK ЛОГИКА ---
    public static void tick(Level level, BlockPos pos, BlockState state, MachineAdvancedAssemblerBlockEntity pEntity) {
        if (level.isClientSide()) {
            pEntity.clientTick(level, pos, state);
        } else {
            pEntity.serverTick();
        }
    }

    private void serverTick() {
        chargeMachineFromBattery();
        
        // Синхронизируем данные через ContainerData каждые 5 тиков
        syncCounter++;
        if (syncCounter >= 5) {
            syncCounter = 0;
            // Принудительная синхронизация энергии если изменилась
            int currentEnergy = energyStorage.getEnergyStored();
            if (Math.abs(currentEnergy - lastSyncedEnergy) > 10) { // Порог 10 FE
                lastSyncedEnergy = currentEnergy;
                setChanged();
            }
        }
        
        if (isCrafting) {
            energyStorage.extractEnergy(100, false);
            progress++;
            setChanged();
            
            if (progress >= maxProgress) {
                progress = 0;
                isCrafting = false;
                setChanged();
                // Синхронизация остановки
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        }
    }

    /**
     * Извлекает энергию из батареи в слоте 0 и передаёт её во внутренний буфер машины.
     */
    private void chargeMachineFromBattery() {
        ItemStack batteryStack = itemHandler.getStackInSlot(0);
        if (batteryStack.isEmpty()) return;
        
        // Проверяем, есть ли у предмета энергетическая capability
        batteryStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(batteryEnergy -> {
            // Сколько энергии может принять наш внутренний буфер?
            int spaceAvailable = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
            if (spaceAvailable <= 0) return; // Буфер полон
            
            // Пытаемся извлечь энергию из батареи
            // Ограничиваем скорость зарядки максимальным вводом энергии (1000 FE/t из конструктора)
            int maxTransfer = Math.min(spaceAvailable, energyStorage.getMaxReceive());
            int extracted = batteryEnergy.extractEnergy(maxTransfer, false);
            
            if (extracted > 0) {
                // Добавляем извлечённую энергию во внутренний буфер
                energyStorage.receiveEnergy(extracted, false);
                setChanged(); // Помечаем для сохранения
            }
        });
    }
    
    // private void craftItem() {
    //     // TODO: Реализуйте логику создания предмета
    //     // Например: this.itemHandler.insertItem(16, new ItemStack(Items.DIAMOND), false);
    //     setChanged();
    // }

    @Override
    public void setRemoved() {
        // Этот метод вызывается при удалении BlockEntity.
        // Важно выполнять операции со звуком только на стороне клиента.
        if (level.isClientSide) {
            // Используем существующий менеджер звуков, чтобы остановить звук.
            // Передаем 'false', чтобы указать, что звук больше не должен играть.
            // Supplier звука может быть null, так как он не будет использоваться при остановке.
            ClientSoundManager.updateSound(this, false, null);
        }
        super.setRemoved();
    }

    public void clientTick(Level level, BlockPos pos, BlockState state) {
        ClientSoundManager.updateSound(this, this.isCrafting, () -> new AdvancedAssemblerSoundInstance(this.getBlockPos()));

        this.prevRingAngle = this.ringAngle;

        for (AssemblerArm arm : arms) {
            arm.updateInterp();
            if (isCrafting) {
                // Передаем level и pos в метод для воспроизведения звуков
                arm.updateArm(level, pos, level.random);
            } else {
                arm.returnToNullPos();
            }
        }
        
        if (isCrafting) {
            if (this.ringAngle != this.ringTarget) {
                float ringDelta = Mth.wrapDegrees(this.ringTarget - this.ringAngle);

                if (Math.abs(ringDelta) <= this.ringSpeed) {
                    this.ringAngle = this.ringTarget;
                    // --- ЗВУК ОСТАНОВКИ АНИМАЦИИ ---
                    level.playSound(null, pos, ModSounds.ASSEMBLER_STOP.get(), SoundSource.BLOCKS, 0.25f, 1.5F);
                    this.ringDelay = 20 + level.random.nextInt(21);
                } else {
                    this.ringAngle += Math.signum(ringDelta) * this.ringSpeed;
                }
            } else if (this.ringDelay > 0) {
                this.ringDelay--;
                if (this.ringDelay == 0) {
                    this.ringTarget = (level.random.nextFloat() * 2 - 1) * 135;
                    this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
                    // --- ЗВУК СТАРТА АНИМАЦИИ ---
                    level.playSound(null, pos, ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.25f, 1.25F + level.random.nextFloat() * 0.25F);
                }
            }
        } else {
            this.ringAngle = Mth.lerp(0.1f, this.ringAngle, 0);
        }
    }

    /**
     * Устанавливает выбранный рецепт для машины.
     * Вызывается из сетевого пакета.
     */
    public void setSelectedRecipe(ResourceLocation recipeId) {
        this.selectedRecipeId = recipeId;
        this.setChanged();
        
        // Синхронизируем с клиентом
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Получает текущий выбранный рецепт.
     */
    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    /**
     * Находит рецепт по его ID.
     */
    @Nullable
    public AssemblerRecipe getSelectedRecipe() {
        if (selectedRecipeId == null || level == null) return null;
        
        return level.getRecipeManager()
            .byKey(selectedRecipeId)
            .filter(recipe -> recipe instanceof AssemblerRecipe)
            .map(recipe -> (AssemblerRecipe) recipe)
            .orElse(null);
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
        nbt.putBoolean("is_crafting", this.isCrafting);
        nbt.putLong("last_use_tick", this.lastUseTick);
        nbt.putBoolean("hasFrame", this.frame);

        nbt.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) {
            nbt.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        
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
        this.frame = nbt.getBoolean("hasFrame");
        if (nbt.contains("HasRecipe") && nbt.getBoolean("HasRecipe")) {
            this.selectedRecipeId = ResourceLocation.tryParse(nbt.getString("SelectedRecipe"));
        } else {
            this.selectedRecipeId = null; // Явно сбрасываем
        }
        
        boolean wasCrafting = this.isCrafting;
        this.isCrafting = nbt.getBoolean("is_crafting");
        this.lastUseTick = nbt.getLong("last_use_tick");
        
        if (this.level != null && this.level.isClientSide() && wasCrafting && !this.isCrafting) {
            ClientSoundManager.updateSound(this, false, null);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        
        // Синхронизируем всё необходимое для рендера
        tag.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        tag.putBoolean("is_crafting", this.isCrafting);
        tag.putBoolean("hasFrame", this.frame);
        
        // КРИТИЧНО: Синхронизируем инвентарь для отображения предметов
        tag.put("inventory", itemHandler.serializeNBT());
        
        // Энергия для GUI (хотя она идёт через ContainerData)
        tag.putInt("energy", energyStorage.getEnergyStored());
        
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag nbt = pkt.getTag();
        if (nbt != null) {
            boolean wasCrafting = this.isCrafting;
            handleUpdateTag(nbt);
            // --- ЗВУК ОСТАНОВКИ ПРИ ОБНОВЛЕНИИ С СЕРВЕРА ---
            if (this.level != null && this.level.isClientSide && wasCrafting && !this.isCrafting) {
                level.playSound(null, worldPosition, ModSounds.ASSEMBLER_STOP.get(), SoundSource.BLOCKS, 0.25f, 1.5F);
            }
        }
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
        return new MachineAdvancedAssemblerMenu(pContainerId, pPlayerInventory, this, this.data);
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

        public void updateArm(Level level, BlockPos pos, RandomSource random) {
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
                        // --- ЗВУК УДАРА ---
                        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            ModSounds.ASSEMBLER_STRIKE_RANDOM.get(), SoundSource.BLOCKS, 0.5f, 1.0F, false);
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

    public class AssemblerRecipeConfig {
        // Список отключенных базовых рецептов
        private static final Set<ResourceLocation> DISABLED_BASE_RECIPES = new HashSet<>();
        
        static {
            // Пример: отключаем алмазный меч из базовых рецептов
            // DISABLED_BASE_RECIPES.add(new ResourceLocation("hbm_m", "diamond_sword_from_assembler"));
        }
        
        public static boolean isRecipeEnabled(ResourceLocation recipeId) {
            return !DISABLED_BASE_RECIPES.contains(recipeId);
        }
        
        // Метод для загрузки конфигурации из файла
        public static void loadConfig() {
            // TODO: Реализовать чтение из config файла
        }
    }
}