package com.hbm_m.block.entity;

import com.hbm_m.block.MachineAdvancedAssemblerBlock;
import com.hbm_m.client.ClientSoundManager;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.item.ItemBlueprintFolder;
import com.hbm_m.menu.MachineAdvancedAssemblerMenu;
import com.hbm_m.module.machine.MachineModuleAdvancedAssembler;
import com.hbm_m.multiblock.IFrameSupportable;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.recipe.AssemblerRecipe;

import com.hbm_m.sound.ModSounds;
import com.hbm_m.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class MachineAdvancedAssemblerBlockEntity extends BaseMachineBlockEntity implements IFrameSupportable {

    private static final int SLOT_COUNT = 17;
    private static final int ENERGY_SLOT = 0;
    private static final int BLUEPRINT_FOLDER_SLOT = 1;
    private static final int OUTPUT_SLOT_START = 2;
    private static final int OUTPUT_SLOT_END = 3;
    private static final int INPUT_SLOT_START = 4;
    private static final int INPUT_SLOT_END = 15;
    private static final int STAMPING_SLOT = 16;

    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100_000, 1000);
    private final FluidTank inputTank = new FluidTank(4000);
    private final FluidTank outputTank = new FluidTank(4000);

    public boolean frame = false;
    private ResourceLocation selectedRecipeId = null;
    
    @Nullable
    private AssemblerRecipe cachedRecipe = null;
    private boolean recipeCacheDirty = false;
    
    private MachineModuleAdvancedAssembler assemblerModule;
    private boolean clientIsCrafting = false;
    private boolean wasCraftingLastTick = false;
    public long lastUseTick = 0;

    // ИСПРАВЛЕНИЕ: добавлена параметризация типа для LazyOptional
    protected LazyOptional<IFluidHandler> fluidInputHandler = LazyOptional.empty();
    protected LazyOptional<IFluidHandler> fluidOutputHandler = LazyOptional.empty();

    private boolean needsClientSync = false;
    private int ticksSinceLastSync = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> assemblerModule != null ? assemblerModule.getProgressInt() : 0;
                case 1 -> assemblerModule != null ? assemblerModule.getMaxProgress() : 0;
                case 2 -> energyStorage.getEnergyStored();
                case 3 -> energyStorage.getMaxEnergyStored();
                case 4 -> energyDelta;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 5;
        }
    };

    @OnlyIn(Dist.CLIENT)
    public final AssemblerArm[] arms = new AssemblerArm[2];
    @OnlyIn(Dist.CLIENT)
    public float ringAngle;
    @OnlyIn(Dist.CLIENT)
    public float prevRingAngle;
    @OnlyIn(Dist.CLIENT)
    private float ringTarget;
    @OnlyIn(Dist.CLIENT)
    private float ringSpeed;
    @OnlyIn(Dist.CLIENT)
    private int ringDelay;

    public MachineAdvancedAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), pos, state, SLOT_COUNT);
        // initClientAnimations();
    }

    // @OnlyIn(Dist.CLIENT)
    // private void initClientAnimations() {
    //     for (int i = 0; i < this.arms.length; i++) {
    //         this.arms[i] = new AssemblerArm();
    //     }
    // }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.advanced_assembly_machine");
    }

    @Override
    protected void setupEnergyCapability() {
        energyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    protected void setupFluidCapability() {
        fluidInputHandler = LazyOptional.of(() -> inputTank);
        fluidOutputHandler = LazyOptional.of(() -> outputTank);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MachineAdvancedAssemblerBlock block)) {
            // Fallback на стандартный AABB с запасом
            return new AABB(worldPosition.offset(-2, -1, -2), worldPosition.offset(3, 4, 3));
        }
        
        //  Получаем структуру через правильное имя метода
        var structureHelper = block.getStructureHelper();
        var structureMap = structureHelper.getStructureMap();
        
        if (structureMap == null || structureMap.isEmpty()) {
            // Fallback для 3x3x3 структуры
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
        }
        
        // Находим минимальные и максимальные координаты из структуры
        int minX = 0, minY = 0, minZ = 0;
        int maxX = 0, maxY = 0, maxZ = 0;
        
        for (BlockPos offset : structureMap.keySet()) {
            minX = Math.min(minX, offset.getX());
            minY = Math.min(minY, offset.getY());
            minZ = Math.min(minZ, offset.getZ());
            maxX = Math.max(maxX, offset.getX());
            maxY = Math.max(maxY, offset.getY());
            maxZ = Math.max(maxZ, offset.getZ());
        }
        
        //  Добавляем запас для анимированных частей (кольцо, руки, головы)
        // Кольцо вращается и выходит за пределы на ~1.5 блока
        double margin = 1.5;
        
        return new AABB(
            worldPosition.getX() + minX - margin,
            worldPosition.getY() + minY - margin,
            worldPosition.getZ() + minZ - margin,
            worldPosition.getX() + maxX + 1 + margin,
            worldPosition.getY() + maxY + 1 + margin,
            worldPosition.getZ() + maxZ + 1 + margin
        );
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == ENERGY_SLOT) {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        }
        if (slot == BLUEPRINT_FOLDER_SLOT) {
            return stack.getItem() instanceof ItemBlueprintFolder;
        }
        if (slot >= OUTPUT_SLOT_START && slot <= OUTPUT_SLOT_END) {
            return false;
        }
        if (slot >= INPUT_SLOT_START && slot <= INPUT_SLOT_END) {
            return assemblerModule == null || assemblerModule.isItemValidForSlot(slot, stack);
        }
        return slot == STAMPING_SLOT;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        sendUpdateToClient();
        return new MachineAdvancedAssemblerMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    public boolean setFrameVisible(boolean visible) {
        if (this.frame != visible) {
            this.frame = visible;
            setChanged();
            if (level != null && !level.isClientSide) {
                sendUpdateToClient();
                MainRegistry.LOGGER.debug("[FRAME SET] Состояние рамки изменено на " + visible);
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
        if (level != null && !level.isClientSide) {
            MultiblockStructureHelper.updateFrameForController(level, worldPosition);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAdvancedAssemblerBlockEntity entity) {
        if (level.isClientSide) {
            entity.clientTick(level, pos, state);
        } else {
            entity.serverTick();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void clientTick(Level level, BlockPos pos, BlockState state) {
        ClientSoundManager.updateSound(this, this.isCrafting(), 
            () -> new com.hbm_m.sound.AdvancedAssemblerSoundInstance(this.getBlockPos()));
        
        this.prevRingAngle = this.ringAngle;
        
        boolean craftingNow = isCrafting();
        
        if (craftingNow) {
            for (AssemblerArm arm : arms) {
                arm.updateInterp();
                arm.updateArm(level, pos, level.random);
            }
        } else {
            for (AssemblerArm arm : arms) {
                arm.updateInterp();
                arm.returnToNullPos();
            }
        }

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
                float absDelta = Math.abs(ringDelta);
                
                if (absDelta <= this.ringSpeed) {
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
            if (Math.abs(this.ringAngle) > 0.1f) {
                this.ringAngle = Mth.lerp(0.1f, this.ringAngle, 0);
            } else {
                this.ringAngle = 0;
            }
        }
    }

    private void serverTick() {
        long gameTime = level.getGameTime();
        
        if (gameTime % 5 == 0) {
            chargeMachineFromBattery();
        }
    
        if (gameTime % 10 == 0) {
            updateEnergyDelta(energyStorage.getEnergyStored());
        }
    
        if (assemblerModule == null && level != null) {
            assemblerModule = new MachineModuleAdvancedAssembler(0, energyStorage, inventory, level);
        }
    
        if (assemblerModule != null) {
            boolean wasCrafting = assemblerModule.isProcessing();
            ItemStack blueprintStack = inventory.getStackInSlot(BLUEPRINT_FOLDER_SLOT);
    
            if (selectedRecipeId != null && (recipeCacheDirty || gameTime % 20 == 0)) {
                AssemblerRecipe currentRecipe = getCachedRecipe();
                
                if (currentRecipe != null && wasCrafting) {
                    String recipePool = currentRecipe.getBlueprintPool();
                    
                    if (recipePool != null && !recipePool.isEmpty()) {
                        String currentPool = ItemBlueprintFolder.getBlueprintPool(blueprintStack);
                        
                        if (!recipePool.equals(currentPool)) {
                            selectedRecipeId = null;
                            cachedRecipe = null;
                            assemblerModule.setPreferredRecipe(null);
                            assemblerModule.resetProgress();
                            level.playSound(null, worldPosition, ModSounds.ASSEMBLER_STOP.get(),
                                SoundSource.BLOCKS, 0.5f, 1.0f);
                            needsClientSync = true;
                            return;
                        }
                    }
                }
                recipeCacheDirty = false;
            }
    
            if (selectedRecipeId != null && assemblerModule.getPreferredRecipe() == null) {
                AssemblerRecipe recipe = getCachedRecipe();
                if (recipe != null) {
                    assemblerModule.setPreferredRecipe(recipe);
                }
            }
    
            assemblerModule.update(1.0, 1.0, true, blueprintStack);
            boolean isCraftingNow = assemblerModule.isProcessing();
    
            if (isCraftingNow && assemblerModule.getPreferredRecipe() != null) {
                ResourceLocation autoSelectedRecipeId = assemblerModule.getPreferredRecipe().getId();
                // Если модуль автоматически выбрал рецепт, а мы его ещё не знаем
                if (selectedRecipeId == null || !selectedRecipeId.equals(autoSelectedRecipeId)) {
                    selectedRecipeId = autoSelectedRecipeId;
                    cachedRecipe = assemblerModule.getPreferredRecipe();
                    recipeCacheDirty = false;
                    needsClientSync = true; // Отправляем обновление клиенту
                }
            }
            
            // ДОПОЛНИТЕЛЬНО: Синхронизируем рецепт модуля с кешем BE
            if (assemblerModule.getCurrentRecipe() != null) {
                ResourceLocation currentRecipeId = assemblerModule.getCurrentRecipe().getId();
                if (selectedRecipeId == null || !selectedRecipeId.equals(currentRecipeId)) {
                    selectedRecipeId = currentRecipeId;
                    cachedRecipe = assemblerModule.getCurrentRecipe();
                    recipeCacheDirty = false;
                    needsClientSync = true;
                }
            }
    
            if (wasCrafting && !isCraftingNow) {
                level.playSound(null, worldPosition, ModSounds.ASSEMBLER_STOP.get(),
                    SoundSource.BLOCKS, 0.5f, 1.0f);
            }
    
            if (wasCrafting != isCraftingNow) {
                needsClientSync = true;
            } else if (isCraftingNow) {
                ticksSinceLastSync++;
                if (ticksSinceLastSync >= 40) {
                    needsClientSync = true;
                }
            }
    
            if (needsClientSync) {
                setChanged();
                sendUpdateToClient();
                needsClientSync = false;
                ticksSinceLastSync = 0;
            }
        }
    }    

    private void chargeMachineFromBattery() {
        ItemStack batteryStack = inventory.getStackInSlot(ENERGY_SLOT);
        if (batteryStack.isEmpty()) return;

        batteryStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(batteryEnergy -> {
            int spaceAvailable = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
            if (spaceAvailable <= 0) return;

            int maxTransfer = Math.min(spaceAvailable, energyStorage.getMaxReceive());
            int extracted = batteryEnergy.extractEnergy(maxTransfer, false);
            
            if (extracted > 0) {
                energyStorage.receiveEnergy(extracted, false);
                setChanged();
            }
        });
    }

    @Nullable
    private AssemblerRecipe getCachedRecipe() {
        if (selectedRecipeId == null || level == null) {
            cachedRecipe = null;
            return null;
        }
        
        if (cachedRecipe == null || recipeCacheDirty) {
            cachedRecipe = level.getRecipeManager()
                .byKey(selectedRecipeId)
                .filter(recipe -> recipe instanceof AssemblerRecipe)
                .map(recipe -> (AssemblerRecipe) recipe)
                .orElse(null);
            recipeCacheDirty = false;
        }
        
        return cachedRecipe;
    }

    public List<AssemblerRecipe> getAvailableRecipes() {
        if (level == null) return List.of();

        ItemStack folderStack = getBlueprintFolder();
        String activePool = ItemBlueprintFolder.getBlueprintPool(folderStack);

        List<AssemblerRecipe> allRecipes = level.getRecipeManager()
            .getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);

        return allRecipes.stream()
            .filter(recipe -> {
                String recipePool = recipe.getBlueprintPool();
                if (recipePool == null || recipePool.isEmpty()) {
                    return true;
                }
                return activePool != null && !activePool.isEmpty() && activePool.equals(recipePool);
            })
            .toList();
    }

    public void setSelectedRecipe(ResourceLocation recipeId) {
        boolean wasCrafting = assemblerModule != null && assemblerModule.isProcessing();
        this.selectedRecipeId = recipeId;
        this.recipeCacheDirty = true;
        this.cachedRecipe = null;

        if (assemblerModule != null && level != null) {
            AssemblerRecipe recipe = getCachedRecipe();
            assemblerModule.setPreferredRecipe(recipe);
            assemblerModule.resetProgress();

            if (wasCrafting) {
                level.playSound(null, worldPosition, ModSounds.ASSEMBLER_STOP.get(),
                    SoundSource.BLOCKS, 0.5f, 1.0f);
            }
        }

        setChanged();
        if (level != null && !level.isClientSide) {
            sendUpdateToClient();
        }
    }

    @Nullable
    public AssemblerRecipe getSelectedRecipe() {
        return getCachedRecipe();
    }

    public ResourceLocation getSelectedRecipeId() {
        return selectedRecipeId;
    }

    @Override
    public NonNullList<ItemStack> getGhostItems() {
        if (assemblerModule != null && assemblerModule.getPreferredRecipe() != null) {
            return assemblerModule.getGhostItems();
        }
        
        // Затем проверяем selectedRecipeId
        if (selectedRecipeId != null && level != null) {
            AssemblerRecipe recipe = getCachedRecipe();
            if (recipe != null) {
                return BaseMachineBlockEntity.createGhostItemsFromIngredients(recipe.getIngredients());
            }
        }

        return NonNullList.create();
    }

    public ItemStack getBlueprintFolder() {
        return inventory.getStackInSlot(BLUEPRINT_FOLDER_SLOT);
    }

    public boolean isCrafting() {
        if (level != null && level.isClientSide) {
            return clientIsCrafting;
        }
        return assemblerModule != null && assemblerModule.isProcessing();
    }

    public int getProgress() {
        return assemblerModule != null ? assemblerModule.getProgressInt() : 0;
    }

    public int getMaxProgress() {
        return assemblerModule != null ? assemblerModule.getMaxProgress() : 100;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    protected boolean isCriticalSlot(int slot) {
        // Слоты ввода ресурсов критичны для синхронизации, чтобы ghost items обновлялись быстрее
        return slot >= INPUT_SLOT_START && slot <= INPUT_SLOT_END;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.put("input_tank", inputTank.writeToNBT(new CompoundTag()));
        tag.put("output_tank", outputTank.writeToNBT(new CompoundTag()));
        tag.putLong("last_use_tick", lastUseTick);
        tag.putBoolean("hasFrame", frame);
        tag.putBoolean("HasRecipe", selectedRecipeId != null);
        
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }

        if (assemblerModule != null) {
            CompoundTag moduleTag = new CompoundTag();
            assemblerModule.writeToNBT(moduleTag);
            tag.put("AssemblerModule", moduleTag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergy(tag.getInt("energy"));
        inputTank.readFromNBT(tag.getCompound("input_tank"));
        outputTank.readFromNBT(tag.getCompound("output_tank"));
        lastUseTick = tag.getLong("last_use_tick");
        frame = tag.getBoolean("hasFrame");
        clientIsCrafting = tag.getBoolean("is_crafting");

        if (tag.contains("HasRecipe") && tag.getBoolean("HasRecipe")) {
            selectedRecipeId = ResourceLocation.tryParse(tag.getString("SelectedRecipe"));
            recipeCacheDirty = true;
        } else {
            selectedRecipeId = null;
            cachedRecipe = null;
        }

        if (tag.contains("AssemblerModule") && level != null) {
            if (assemblerModule == null) {
                assemblerModule = new MachineModuleAdvancedAssembler(0, energyStorage, inventory, level);
            }
            assemblerModule.readFromNBT(tag.getCompound("AssemblerModule"));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("HasRecipe", selectedRecipeId != null);
        
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }

        tag.putBoolean("is_crafting", isCrafting());
        tag.putBoolean("hasFrame", frame);
        tag.putInt("energy", energyStorage.getEnergyStored());
        return tag;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidInputHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        
        //  ИСПРАВЛЕНИЕ 1: Инициализация arms только на клиенте с проверкой null
        if (level != null && level.isClientSide && arms[0] == null) {
            //  Используем DistExecutor для безопасности
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, 
                () -> () -> initClientArms()
            );
        }

        //  ИСПРАВЛЕНИЕ 2: Инициализация assemblerModule только на сервере
        if (level != null && !level.isClientSide && assemblerModule == null) {
            assemblerModule = new MachineModuleAdvancedAssembler(0, energyStorage, inventory, level);
        }
    }

    /**
     * Инициализация клиентских рук (будет вызвана только на клиенте)
     */
    @OnlyIn(Dist.CLIENT)
    private void initClientArms() {
        for (int i = 0; i < arms.length; i++) {
            arms[i] = new AssemblerArm();
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidInputHandler.invalidate();
        fluidOutputHandler.invalidate();
    }

    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide) {
            ClientSoundManager.updateSound(this, false, null);
        }
        super.setRemoved();
    }

    // ==================== ANIMATION ARM CLASS ====================
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
            Arrays.fill(targetAngles, 0);
            speed[0] = speed[1] = speed[2] = 3;
            speed[3] = 0.25f;
            state = ArmActionState.RETRACT_STRIKER;
            move();
        }

        private void resetSpeed() {
            speed[0] = 15; 
            speed[1] = 15; 
            speed[2] = 15; 
            speed[3] = 0.5f;
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

        private static final float[][] POSITIONS = {
            {45, -15, -5}, {15, 15, -15}, {25, 10, -15}, 
            {30, 0, -10}, {70, -10, -25}
        };

        public void chooseNewArmPosition(RandomSource random) {
            int chosen = random.nextInt(POSITIONS.length);
            targetAngles[0] = POSITIONS[chosen][0];
            targetAngles[1] = POSITIONS[chosen][1];
            targetAngles[2] = POSITIONS[chosen][2];
        }

        private boolean move() {
            boolean allReached = true;
            
            for (int i = 0; i < angles.length; i++) {
                float current = angles[i];
                float target = targetAngles[i];
                
                if (current == target) continue;
                
                allReached = false;
                float delta = target - current;
                float absDelta = Math.abs(delta);
                
                if (absDelta <= speed[i]) {
                    angles[i] = target;
                } else {
                    angles[i] += Math.signum(delta) * speed[i];
                }
            }
            
            return allReached;
        }
    }
}
