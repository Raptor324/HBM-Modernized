package com.hbm_m.block.entity;

import com.hbm_m.client.ClientSoundManager;
import com.hbm_m.energy.*;
import com.hbm_m.item.ItemBlueprintFolder;
import com.hbm_m.menu.MachineAdvancedAssemblerMenu;
import com.hbm_m.module.machine.MachineModuleAdvancedAssembler;
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
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

// НОВЫЕ ИМПОРТЫ для long-энергии
import com.hbm_m.capability.ModCapabilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MachineAdvancedAssemblerBlockEntity extends BlockEntity implements MenuProvider, IFrameSupportable {

    private final ItemStackHandler itemHandler = new ItemStackHandler(17) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == BLUEPRINT_FOLDER_SLOT && level != null && !level.isClientSide()) {
                ItemStack folderStack = getBlueprintFolder();
                if (folderStack.isEmpty() && selectedRecipeId != null) {
                    AssemblerRecipe currentRecipe = getSelectedRecipe();
                    if (currentRecipe != null) {
                        String recipePool = currentRecipe.getBlueprintPool();
                        if (recipePool != null && !recipePool.isEmpty()) {
                            boolean wasCrafting = assemblerModule != null && assemblerModule.isProcessing();

                            selectedRecipeId = null;
                            if (assemblerModule != null) {
                                assemblerModule.setPreferredRecipe(null);
                                assemblerModule.resetProgress();
                            }

                            if (wasCrafting) {
                                level.playSound(null, worldPosition, ModSounds.ASSEMBLER_STOP.get(),
                                        SoundSource.BLOCKS, 0.5f, 1.0f);
                            }

                            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                        }
                    }
                }
            }
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
            if (slot >= 4 && slot <= 15) {
                return assemblerModule == null || assemblerModule.isItemValidForSlot(slot, stack);
            }
            return super.isItemValid(slot, stack);
        }

        @Override
        @NotNull
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return super.insertItem(slot, stack, simulate);
        }
    };

    // ИЗМЕНЕНИЕ: Теперь используем BlockEntityEnergyStorage с long (уже реализован в твоих файлах)
    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100_000L, 100000L) {
        @Override
        public long receiveEnergy(long maxReceive, boolean simulate) {
            long received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
                syncEnergyToClients();
            }
            return received;
        }

        @Override
        public long extractEnergy(long maxExtract, boolean simulate) {
            long extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
                syncEnergyToClients();
            }
            return extracted;
        }
    };

    public boolean frame = false;

    private final FluidTank inputTank = new FluidTank(4000);
    private final FluidTank outputTank = new FluidTank(4000);

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    // ИЗМЕНЕНИЕ: Добавляем LazyOptional для ILongEnergyStorage
    private LazyOptional<ILongEnergyStorage> lazyLongEnergyHandler = LazyOptional.empty();
    // ИЗМЕНЕНИЕ: Создаём wrapper для обратной совместимости с Forge Energy
    private LazyOptional<IEnergyStorage> lazyForgeEnergyHandler = LazyOptional.empty();
    private LazyOptional<FluidTank> lazyFluidHandler = LazyOptional.empty();

    private ResourceLocation selectedRecipeId = null;

    private int syncCounter = 0;
    // ИЗМЕНЕНИЕ: Переводим на long для корректного отображения больших значений
    private long previousEnergy = 0L;
    private long energyDelta = 0L;
    private int energyDeltaUpdateCounter = 0;

    private boolean clientIsCrafting = false;
    private boolean wasCraftingLastTick = false;

    private MachineModuleAdvancedAssembler assemblerModule;

    protected final ContainerData data;

    private static final int BLUEPRINT_FOLDER_SLOT = 1;

    @OnlyIn(Dist.CLIENT)
    private AdvancedAssemblerSoundInstance soundInstance;

    public final AssemblerArm[] arms = new AssemblerArm[2];
    public float ringAngle;
    public float prevRingAngle;
    private float ringTarget;
    private float ringSpeed;
    private int ringDelay;

    public MachineAdvancedAssemblerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), pPos, pBlockState);
        for (int i = 0; i < this.arms.length; i++) {
            this.arms[i] = new AssemblerArm();
        }

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {

                // --- ИЗМЕНЕНИЕ: ИСПОЛЬЗУЕМ УПАКОВЩИК ---

                // Получаем значения 1 раз
                long currentEnergy = energyStorage.getEnergyStored();
                long maxEnergy = energyStorage.getMaxEnergyStored();
                long delta = energyDelta; // energyDelta у тебя уже long

                return switch (pIndex) {
                    // Прогресс (остаётся int)
                    case 0 -> assemblerModule != null ? assemblerModule.getProgressInt() : 0;
                    case 1 -> assemblerModule != null ? assemblerModule.getMaxProgress() : 0;

                    // Текущая энергия (long -> 2x int)
                    case 2 -> LongDataPacker.packHigh(currentEnergy);
                    case 3 -> LongDataPacker.packLow(currentEnergy);

                    // Макс. энергия (long -> 2x int)
                    case 4 -> LongDataPacker.packHigh(maxEnergy);
                    case 5 -> LongDataPacker.packLow(maxEnergy);

                    // Дельта энергии (long -> 2x int)
                    case 6 -> LongDataPacker.packHigh(delta);
                    case 7 -> LongDataPacker.packLow(delta);

                    default -> 0;
                };
                // --- КОНЕЦ ИЗМЕНЕНИЙ ---
            }

            @Override
            public void set(int pIndex, int pValue) {
                // Только для чтения на клиенте
            }

            @Override
            public int getCount() {
                return 8;
            }
        };
    }

    // ИЗМЕНЕНИЕ: Методы теперь возвращают long
    public long getEnergyStored() { return this.energyStorage.getEnergyStored(); }
    public long getMaxEnergyStored() { return this.energyStorage.getMaxEnergyStored(); }

    public int getMaxProgress() {
        return assemblerModule != null ? assemblerModule.getMaxProgress() : 100;
    }
    public long lastUseTick = 0;

    @Override
    public boolean setFrameVisible(boolean visible) {
        if (this.frame != visible) {
            this.frame = visible;
            this.setChanged();

            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
                MainRegistry.LOGGER.debug("[FRAME SET] Состояние рамки изменено на " + visible + ". Отправлен пакет клиенту.");
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isFrameVisible() {
        return this.frame;
    }

    @Override
    public void checkForFrame() {
        if (this.level != null && !this.level.isClientSide()) {
            MultiblockStructureHelper.updateFrameForController(this.level, this.worldPosition);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAdvancedAssemblerBlockEntity pEntity) {
        if (level.isClientSide()) {
            pEntity.clientTick(level, pos, state);
        } else {
            pEntity.serverTick();
        }
    }

    public boolean isCrafting() {
        if (level != null && level.isClientSide()) {
            return clientIsCrafting;
        }
        return assemblerModule != null && assemblerModule.isProcessing();
    }

    public int getProgress() {
        return assemblerModule != null ? assemblerModule.getProgressInt() : 0;
    }

    private void serverTick() {
        chargeMachineFromBattery();
        if (assemblerModule == null && level != null) {
            assemblerModule = new MachineModuleAdvancedAssembler(0, energyStorage, itemHandler, level);
        }

        if (assemblerModule != null) {
            boolean wasCrafting = assemblerModule.isProcessing();
            ItemStack blueprintStack = itemHandler.getStackInSlot(1);

            if (selectedRecipeId != null) {
                AssemblerRecipe currentRecipe = getSelectedRecipe();
                if (currentRecipe != null) {
                    String recipePool = currentRecipe.getBlueprintPool();
                    if (recipePool != null && !recipePool.isEmpty()) {
                        String currentPool = ItemBlueprintFolder.getBlueprintPool(blueprintStack);
                        if (!recipePool.equals(currentPool)) {
                            selectedRecipeId = null;
                            assemblerModule.setPreferredRecipe(null);
                            assemblerModule.resetProgress();

                            if (wasCrafting) {
                                level.playSound(null, worldPosition, ModSounds.ASSEMBLER_STOP.get(),
                                        SoundSource.BLOCKS, 0.5f, 1.0f);
                            }

                            setChanged();
                            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                            return;
                        }
                    }
                }
            }

            if (selectedRecipeId != null && assemblerModule.getPreferredRecipe() == null) {
                AssemblerRecipe recipe = getSelectedRecipe();
                if (recipe != null) {
                    assemblerModule.setPreferredRecipe(recipe);
                }
            }

            assemblerModule.update(1.0, 1.0, true, blueprintStack);
            boolean isCraftingNow = assemblerModule.isProcessing();

            if (wasCrafting && !isCraftingNow) {
                level.playSound(null, worldPosition, ModSounds.ASSEMBLER_STOP.get(),
                        SoundSource.BLOCKS, 0.5f, 1.0f);
            }

            if (selectedRecipeId == null) {
                ResourceLocation moduleRecipeId = assemblerModule.getCurrentRecipe() != null
                        ? assemblerModule.getCurrentRecipe().getId()
                        : null;
                if (!Objects.equals(selectedRecipeId, moduleRecipeId)) {
                    selectedRecipeId = moduleRecipeId;
                    setChanged();
                    if (level != null) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                }
            }

            if (wasCrafting != isCraftingNow) {
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }

            if (assemblerModule.needsSync) {
                setChanged();
            }

            if (isCraftingNow) {
                setChanged();
            }
        }

        // ИЗМЕНЕНИЕ: energyDelta теперь long
        energyDeltaUpdateCounter++;
        if (energyDeltaUpdateCounter >= 20) {
            long currentEnergy = energyStorage.getEnergyStored();
            long totalChange = currentEnergy - previousEnergy;
            energyDelta = totalChange / 20;
            previousEnergy = currentEnergy;
            energyDeltaUpdateCounter = 0;
        }

        syncCounter++;
        if (syncCounter >= 5) {
            syncCounter = 0;
            setChanged();
        }
    }

    // ИЗМЕНЕНИЕ: Метод возвращает long
    public long getEnergyDelta() {
        return energyDelta;
    }

    private void syncEnergyToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack getBlueprintFolder() {
        return this.itemHandler.getStackInSlot(BLUEPRINT_FOLDER_SLOT);
    }

    public List<AssemblerRecipe> getAvailableRecipes() {
        if (this.level == null) return List.of();

        ItemStack folderStack = getBlueprintFolder();
        String activePool = ItemBlueprintFolder.getBlueprintPool(folderStack);

        List<AssemblerRecipe> allRecipes = this.level.getRecipeManager()
                .getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);

        List<AssemblerRecipe> result = new ArrayList<>();

        for (AssemblerRecipe recipe : allRecipes) {
            String recipePool = recipe.getBlueprintPool();

            if (recipePool == null || recipePool.isEmpty()) {
                result.add(recipe);
                continue;
            }

            if (activePool != null && !activePool.isEmpty() && activePool.equals(recipePool)) {
                result.add(recipe);
            }
        }

        return result;
    }

    /**
     * ИЗМЕНЕНИЕ: Обновлённый метод для зарядки от батареи
     * Теперь поддерживает как ILongEnergyStorage, так и IEnergyStorage
     */
    private void chargeMachineFromBattery() {
        ItemStack batteryStack = itemHandler.getStackInSlot(0);
        if (batteryStack.isEmpty()) return;

        long spaceAvailable = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (spaceAvailable <= 0) return;

        // Пытаемся получить long-capability
        LazyOptional<ILongEnergyStorage> longCap = batteryStack.getCapability(ModCapabilities.LONG_ENERGY);
        if (longCap.isPresent()) {
            longCap.ifPresent(batteryEnergy -> {
                long maxTransfer = Math.min(spaceAvailable, energyStorage.getMaxReceive());
                long extracted = batteryEnergy.extractEnergy(maxTransfer, false);

                if (extracted > 0) {
                    energyStorage.receiveEnergy(extracted, false);
                    setChanged();
                }
            });
        } else {
            // Fallback на старую Forge Energy (int)
            batteryStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(batteryEnergy -> {
                int spaceInt = (int) Math.min(Integer.MAX_VALUE, spaceAvailable);
                int maxTransferInt = (int) Math.min(Integer.MAX_VALUE, energyStorage.getMaxReceive());
                int maxTransfer = Math.min(spaceInt, maxTransferInt);
                int extracted = batteryEnergy.extractEnergy(maxTransfer, false);

                if (extracted > 0) {
                    energyStorage.receiveEnergy(extracted, false);
                    setChanged();
                }
            });
        }
    }

    @Override
    public void setRemoved() {
        if (level.isClientSide) {
            ClientSoundManager.updateSound(this, false, null);
        }
        super.setRemoved();
    }

    public void clientTick(Level level, BlockPos pos, BlockState state) {
        ClientSoundManager.updateSound(this, this.isCrafting(), () -> new AdvancedAssemblerSoundInstance(this.getBlockPos()));

        this.prevRingAngle = this.ringAngle;

        for (AssemblerArm arm : arms) {
            arm.updateInterp();
            if (isCrafting()) {
                arm.updateArm(level, pos, level.random);
            } else {
                arm.returnToNullPos();
            }
        }

        boolean craftingNow = isCrafting();

        if (craftingNow && !wasCraftingLastTick) {
            this.ringTarget = (level.random.nextFloat() * 2 - 1) * 135;
            this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
            this.ringDelay = 0;

            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f, false);
        }

        wasCraftingLastTick = craftingNow;

        if (craftingNow) {
            if (this.ringAngle != this.ringTarget) {
                float ringDelta = Mth.wrapDegrees(this.ringTarget - this.ringAngle);
                if (Math.abs(ringDelta) <= this.ringSpeed) {
                    this.ringAngle = this.ringTarget;
                    this.ringDelay = 20 + level.random.nextInt(21);
                } else {
                    this.ringAngle += Math.signum(ringDelta) * this.ringSpeed;
                }
            } else if (this.ringDelay > 0) {
                this.ringDelay--;
                if (this.ringDelay == 0) {
                    level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.3f, 1.0f, false);
                    this.ringTarget = (level.random.nextFloat() * 2 - 1) * 135;
                    this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
                }
            }
        } else {
            this.ringAngle = Mth.lerp(0.1f, this.ringAngle, 0);
        }
    }

    public void setSelectedRecipe(ResourceLocation recipeId) {
        boolean wasCrafting = assemblerModule != null && assemblerModule.isProcessing();

        this.selectedRecipeId = recipeId;
        if (assemblerModule != null && level != null) {
            AssemblerRecipe recipe = level.getRecipeManager()
                    .byKey(recipeId)
                    .filter(r -> r instanceof AssemblerRecipe)
                    .map(r -> (AssemblerRecipe) r)
                    .orElse(null);

            assemblerModule.setPreferredRecipe(recipe);
            assemblerModule.resetProgress();

            if (wasCrafting) {
                level.playSound(null, worldPosition, ModSounds.ASSEMBLER_STOP.get(),
                        SoundSource.BLOCKS, 0.5f, 1.0f);
            }
        }

        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    @Nullable
    public AssemblerRecipe getSelectedRecipe() {
        if (selectedRecipeId == null || level == null) return null;

        return level.getRecipeManager()
                .byKey(selectedRecipeId)
                .filter(recipe -> recipe instanceof AssemblerRecipe)
                .map(recipe -> (AssemblerRecipe) recipe)
                .orElse(null);
    }

    // ИЗМЕНЕНИЕ: Обновляем getCapability для поддержки обеих систем
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Приоритет: сначала наша long-система
        if (cap == ModCapabilities.LONG_ENERGY) {
            return lazyLongEnergyHandler.cast();
        }
        // Затем Forge Energy для обратной совместимости
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyForgeEnergyHandler.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
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
        // ИЗМЕНЕНИЕ: Инициализируем long-capability
        lazyLongEnergyHandler = LazyOptional.of(() -> energyStorage);
        // ИЗМЕНЕНИЕ: Создаём wrapper для Forge Energy
        lazyForgeEnergyHandler = lazyLongEnergyHandler.lazyMap(LongToForgeWrapper::new);
        lazyFluidHandler = LazyOptional.of(() -> inputTank);

        if (level != null && assemblerModule == null) {
            assemblerModule = new MachineModuleAdvancedAssembler(0, energyStorage, itemHandler, level);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyLongEnergyHandler.invalidate();
        lazyForgeEnergyHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    // ИЗМЕНЕНИЕ: Обновляем NBT для работы с long
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        nbt.putLong("energy", energyStorage.getEnergyStored()); // long вместо int
        nbt.put("input_tank", inputTank.writeToNBT(new CompoundTag()));
        nbt.put("output_tank", outputTank.writeToNBT(new CompoundTag()));
        nbt.putLong("last_use_tick", this.lastUseTick);
        nbt.putBoolean("hasFrame", this.frame);
        nbt.putLong("PreviousEnergy", previousEnergy); // long
        nbt.putLong("EnergyDelta", energyDelta); // long
        nbt.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) {
            nbt.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        if (assemblerModule != null) {
            CompoundTag moduleTag = new CompoundTag();
            assemblerModule.writeToNBT(moduleTag);
            nbt.put("AssemblerModule", moduleTag);
        }

        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));

        if (nbt.contains("folder_data")) {
            CompoundTag folderTag = nbt.getCompound("folder_data");
            ItemStack folderStack = getBlueprintFolder();
            if (!folderStack.isEmpty()) {
                String pool = folderTag.getString("blueprintPool");
                ItemBlueprintFolder.writeBlueprintPool(folderStack, pool);
            }
        }

        energyStorage.setEnergy(nbt.getLong("energy")); // long
        inputTank.readFromNBT(nbt.getCompound("input_tank"));
        outputTank.readFromNBT(nbt.getCompound("output_tank"));
        this.frame = nbt.getBoolean("hasFrame");
        this.clientIsCrafting = nbt.getBoolean("is_crafting");
        this.previousEnergy = nbt.getLong("PreviousEnergy"); // long
        this.energyDelta = nbt.getLong("EnergyDelta"); // long

        if (nbt.contains("HasRecipe") && nbt.getBoolean("HasRecipe")) {
            this.selectedRecipeId = ResourceLocation.tryParse(nbt.getString("SelectedRecipe"));
        } else {
            this.selectedRecipeId = null;
        }

        if (nbt.contains("AssemblerModule") && level != null) {
            if (assemblerModule == null) {
                assemblerModule = new MachineModuleAdvancedAssembler(0, energyStorage, itemHandler, level);
            }
            assemblerModule.readFromNBT(nbt.getCompound("AssemblerModule"));
        }

        this.lastUseTick = nbt.getLong("last_use_tick");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();

        tag.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        tag.putBoolean("is_crafting", this.isCrafting());
        tag.putBoolean("hasFrame", this.frame);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putLong("energy", energyStorage.getEnergyStored()); // long

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
            boolean wasCrafting = this.isCrafting();
            handleUpdateTag(nbt);
            if (this.level != null && this.level.isClientSide && wasCrafting && !this.isCrafting()) {
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.advanced_assembly_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new MachineAdvancedAssemblerMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    // Классы для анимации
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
        private static final Set<ResourceLocation> DISABLED_BASE_RECIPES = new HashSet<>();

        static {
            // Пример: отключаем алмазный меч из базовых рецептов
            // DISABLED_BASE_RECIPES.add(new ResourceLocation("hbm_m", "diamond_sword_from_assembler"));
        }

        public static boolean isRecipeEnabled(ResourceLocation recipeId) {
            return !DISABLED_BASE_RECIPES.contains(recipeId);
        }

        public static void loadConfig() {
            // TODO: Реализовать чтение из config файла
        }
    }
}