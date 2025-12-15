package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.custom.machines.MachineAdvancedAssemblerBlock;
// import com.hbm_m.client.ClientSoundManager;
import com.hbm_m.item.custom.industrial.ItemBlueprintFolder;
import com.hbm_m.item.custom.fekal_electric.ItemCreativeBattery;
import com.hbm_m.menu.MachineAdvancedAssemblerMenu;
import com.hbm_m.module.machine.MachineModuleAdvancedAssembler;
import com.hbm_m.multiblock.IFrameSupportable;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.util.LongDataPacker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.fml.DistExecutor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Advanced Assembler Block Entity:
 * - Наследование от BaseMachineBlockEntity
 * - Полная логика мультиблока, GUI, анимации и рецептов перенесена из старой системы
 * - Энергосистема остается long-ориентированной с совместимостью Forge Energy
 */
public class MachineAdvancedAssemblerBlockEntity extends BaseMachineBlockEntity implements IFrameSupportable {

    private static final int SLOT_COUNT = 17;
    private static final int ENERGY_SLOT = 0;
    private static final int BLUEPRINT_FOLDER_SLOT = 1;
    private static final int OUTPUT_SLOT_START = 2;
    private static final int OUTPUT_SLOT_END = 3;
    private static final int INPUT_SLOT_START = 4;
    private static final int INPUT_SLOT_END = 15;
    private static final int STAMPING_SLOT = 16;


    // Флюиды
    private final FluidTank inputTank = new FluidTank(4000);
    private final FluidTank outputTank = new FluidTank(4000);
    protected LazyOptional<IFluidHandler> fluidInputHandler = LazyOptional.empty();
    protected LazyOptional<IFluidHandler> fluidOutputHandler = LazyOptional.empty();

    // Состояние мультиблочной "рамки"
    public boolean frame = false;

    // Выбор рецепта и кеш
    @Nullable private ResourceLocation selectedRecipeId = null;
    @Nullable private AssemblerRecipe cachedRecipe = null;
    private boolean recipeCacheDirty = false;

    // Модуль крафта
    @Nullable private MachineModuleAdvancedAssembler assemblerModule = null;

    // Крафт состояние на клиенте
    private boolean clientIsCrafting = false;
    private boolean wasCraftingLastTick = false;
    public long lastUseTick = 0;

    private boolean needsClientSync = false;
    private int ticksSinceLastSync = 0;

    // Поле для хранения клиентского обработчика.
    // LazyOptional используется для безопасной инициализации только на клиенте.
    private final LazyOptional<ClientTicker> clientTicker = DistExecutor.unsafeRunForDist(
            () -> () -> LazyOptional.of(() -> new ClientTicker()),
            () -> () -> LazyOptional.empty()
    );

    // ContainerData: упаковываем long как два int через LongDataPacker
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                // Только прогресс (индексы 0 и 1)
                case 0 -> assemblerModule != null ? assemblerModule.getProgressInt() : 0;
                case 1 -> assemblerModule != null ? assemblerModule.getMaxProgress() : 0;
                // Все индексы с энергией (2-7) удалены
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { /* read-only on client */ }

        @Override
        public int getCount() {
            return 2;
        }
    };

    // Клиентские анимации
    // @OnlyIn(Dist.CLIENT) public final AssemblerArm[] arms = new AssemblerArm[2];
    // @OnlyIn(Dist.CLIENT) public float ringAngle;
    // @OnlyIn(Dist.CLIENT) public float prevRingAngle;
    // @OnlyIn(Dist.CLIENT) private float ringTarget;
    // @OnlyIn(Dist.CLIENT) private float ringSpeed;
    // @OnlyIn(Dist.CLIENT) private int ringDelay;

    public MachineAdvancedAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_ASSEMBLY_MACHINE_BE.get(), pos, state, 17, 100_000L, 100_000L);

        // Инициализируем модуль здесь, передавая СЕБЯ (this) как IEnergyReceiver
        if (this.level != null && !this.level.isClientSide) {
            this.assemblerModule = new MachineModuleAdvancedAssembler(0, this, this.inventory, this.level);
        }
    }

    public boolean isCrafting() {
        if (level != null && level.isClientSide) {
            return clientIsCrafting;
        }
        return assemblerModule != null && assemblerModule.isProcessing();
    }
    
    public boolean isClientCrafting() {
        return this.clientIsCrafting;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.advanced_assembly_machine");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected void setupFluidCapability() {
        fluidInputHandler = LazyOptional.of(() -> inputTank);
        fluidOutputHandler = LazyOptional.of(() -> outputTank);
    }

    // Ограничиваем валидность слотов
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

    // Крупный AABB под анимации и структуру
    @Override
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MachineAdvancedAssemblerBlock block)) {
            return new AABB(worldPosition.offset(-2, -1, -2), worldPosition.offset(3, 4, 3));
        }
        var structureHelper = block.getStructureHelper();
        var structureMap = structureHelper.getStructureMap();
        if (structureMap == null || structureMap.isEmpty()) {
            return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
        }
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

    // GUI
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        sendUpdateToClient();
        return new MachineAdvancedAssemblerMenu(containerId, playerInventory, this, this.data);
    }

    // Рамка мультиблока: централизованный вызов
    @Override
    public void checkForFrame() {
        if (level != null && !level.isClientSide) {
            MultiblockStructureHelper.updateFrameForController(level, worldPosition);
        }
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

    // Tick-хуки
    public static void tick(Level level, BlockPos pos, BlockState state, MachineAdvancedAssemblerBlockEntity entity) {
        if (level.isClientSide) {
            // Вызываем клиентский tick через LazyOptional
            entity.clientTicker.ifPresent(ticker -> ticker.clientTick(level, pos, state, entity));
        } else {
            entity.serverTick();
        }
    }

    // @OnlyIn(Dist.CLIENT)
    // public void clientTick(Level level, BlockPos pos, BlockState state) {
    //     ClientSoundManager.updateSound(this, this.isCrafting(),
    //             () -> new com.hbm_m.sound.AdvancedAssemblerSoundInstance(this.getBlockPos()));
    //     this.prevRingAngle = this.ringAngle;
    //     boolean craftingNow = isCrafting();
    //     if (craftingNow) {
    //         for (AssemblerArm arm : arms) {
    //             arm.updateInterp();
    //             arm.updateArm(level, pos, level.random);
    //         }
    //     } else {
    //         for (AssemblerArm arm : arms) {
    //             arm.updateInterp();
    //             arm.returnToNullPos();
    //         }
    //     }
    //     if (craftingNow && !wasCraftingLastTick) {
    //         this.ringTarget = (level.random.nextFloat() * 2 - 1) * 135;
    //         this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
    //         this.ringDelay = 0;
    //         level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
    //                 ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f, false);
    //     }
    //     wasCraftingLastTick = craftingNow;
    //     if (craftingNow) {
    //         if (this.ringAngle != this.ringTarget) {
    //             float ringDelta = Mth.wrapDegrees(this.ringTarget - this.ringAngle);
    //             float absDelta = Math.abs(ringDelta);
    //             if (absDelta <= this.ringSpeed) {
    //                 this.ringAngle = this.ringTarget;
    //                 this.ringDelay = 20 + level.random.nextInt(21);
    //             } else {
    //                 this.ringAngle += Math.signum(ringDelta) * this.ringSpeed;
    //             }
    //         } else if (this.ringDelay > 0) {
    //             this.ringDelay--;
    //             if (this.ringDelay == 0) {
    //                 level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
    //                         ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.3f, 1.0f, false);
    //                 this.ringTarget = (level.random.nextFloat() * 2 - 1) * 135;
    //                 this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
    //             }
    //         }
    //     } else {
    //         if (Math.abs(this.ringAngle) > 0.1f) {
    //             this.ringAngle = Mth.lerp(0.1f, this.ringAngle, 0);
    //         } else {
    //             this.ringAngle = 0;
    //         }
    //     }
    // }

    private void serverTick() {

        ensureNetworkInitialized();

        long gameTime = level.getGameTime();
        if (gameTime % 5 == 0) {
            chargeMachineFromBattery();
        }
        if (gameTime % 10 == 0) {
            // НОВАЯ ПРАВИЛЬНАЯ СТРОКА
            updateEnergyDelta(this.getEnergyStored());
        }
        if (assemblerModule == null && level != null) {
            // Передаем 'this' как IEnergyReceiver
            assemblerModule = new MachineModuleAdvancedAssembler(0, this, inventory, level);
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
                if (selectedRecipeId == null || !selectedRecipeId.equals(autoSelectedRecipeId)) {
                    selectedRecipeId = autoSelectedRecipeId;
                    cachedRecipe = assemblerModule.getPreferredRecipe();
                    recipeCacheDirty = false;
                    needsClientSync = true;
                }
            }

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
        ItemStack energySourceStack = inventory.getStackInSlot(ENERGY_SLOT);
        if (energySourceStack.isEmpty()) return;

        // Креативная батарея
        if (energySourceStack.getItem() instanceof ItemCreativeBattery) {
            this.setEnergyStored(this.getMaxEnergyStored());
            return;
        }

        // Обычная батарея через HBM capability
        energySourceStack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).ifPresent(itemEnergy -> {
            long energyNeeded = this.getMaxEnergyStored() - this.getEnergyStored();
            if (energyNeeded <= 0) return;

            long maxCanReceive = this.getReceiveSpeed();
            long energyToTransfer = Math.min(energyNeeded, maxCanReceive);

            if (energyToTransfer > 0) {
                // ПРАВИЛЬНО: используем extractEnergy вместо прямого доступа
                long extracted = itemEnergy.extractEnergy(energyToTransfer, false);

                if (extracted > 0) {
                    this.setEnergyStored(this.getEnergyStored() + extracted);
                    setChanged();
                }
            }
        });

        // Fallback на Forge Energy для совместимости
        if (!energySourceStack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()) {
            energySourceStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
                long energyNeeded = this.getMaxEnergyStored() - this.getEnergyStored();
                if (energyNeeded <= 0) return;

                int maxTransfer = (int) Math.min(Integer.MAX_VALUE, Math.min(energyNeeded, this.getReceiveSpeed()));
                int extracted = itemEnergy.extractEnergy(maxTransfer, false);

                if (extracted > 0) {
                    this.setEnergyStored(this.getEnergyStored() + extracted);
                    setChanged();
                }
            });
        }
    }

    // Рецепты и ghost-предметы
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
        AssemblerRecipe recipe = null;

        // 1. Сначала пытаемся получить рецепт из работающего модуля
        if (assemblerModule != null && assemblerModule.getPreferredRecipe() != null) {
            recipe = assemblerModule.getPreferredRecipe();
        }

        // 2. Если не вышло (или модуль не запущен), берем выбранный рецепт из кэша
        if (recipe == null && selectedRecipeId != null && level != null) {
            recipe = getCachedRecipe();
        }

        // 3. Если у нас есть *хоть какой-то* рецепт, ИСПОЛЬЗУЕМ "СТАКАЮЩУЮ" ЛОГИКУ
        if (recipe != null) {
            // Этот метод (из BaseMachineBlockEntity) должен стакать ингредиенты
            return BaseMachineBlockEntity.createGhostItemsFromIngredients(recipe.getIngredients());
        }

        // 4. Иначе возвращаем пустой список
        return NonNullList.create();
    }

    public ItemStack getBlueprintFolder() {
        return inventory.getStackInSlot(BLUEPRINT_FOLDER_SLOT);
    }

    public int getProgress() {
        return assemblerModule != null ? assemblerModule.getProgressInt() : 0;
    }

    public int getMaxProgress() {
        return assemblerModule != null ? assemblerModule.getMaxProgress() : 100;
    }


    @Override
    protected boolean isCriticalSlot(int slot) {
        // Ускоренная синхронизация для слотов ввода (обновление ghost items)
        return slot >= INPUT_SLOT_START && slot <= INPUT_SLOT_END;
    }

    // NBT
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("input_tank", inputTank.writeToNBT(new CompoundTag()));
        tag.put("output_tank", outputTank.writeToNBT(new CompoundTag()));
        tag.putLong("last_use_tick", lastUseTick);
        tag.putBoolean("FrameVisible", frame);
        tag.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        if (assemblerModule != null) {
            CompoundTag moduleTag = new CompoundTag();
            assemblerModule.writeToNBT(moduleTag);
            tag.put("AssemblerModule", moduleTag);
        }
        tag.putBoolean("is_crafting", isCrafting());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        inputTank.readFromNBT(tag.getCompound("input_tank"));
        outputTank.readFromNBT(tag.getCompound("output_tank"));
        lastUseTick = tag.getLong("last_use_tick");
        frame = tag.getBoolean("FrameVisible");
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
                // НОВЫЙ ПРАВИЛЬНЫЙ СПОСОБ
                assemblerModule = new MachineModuleAdvancedAssembler(0, this, inventory, level);
            }
            assemblerModule.readFromNBT(tag.getCompound("AssemblerModule"));
        }

    }

    // Пакеты синхронизации
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(pkt.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return ClientboundBlockEntityDataPacket.create(this, be -> tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    // Capability: используем базовые item/energy/fluids, плюс локальные хэндлеры флюидов
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            // По умолчанию экспонируем входной бак; при необходимости добавьте роутинг по side
            return fluidInputHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && assemblerModule == null) {
            this.assemblerModule = new MachineModuleAdvancedAssembler(0, this, this.inventory, this.level);
        }
    }

    // @OnlyIn(Dist.CLIENT)
    // private void initClientArms() {
    //     for (int i = 0; i < arms.length; i++) {
    //         arms[i] = new AssemblerArm();
    //     }
    // }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidInputHandler.invalidate();
        fluidOutputHandler.invalidate();
        clientTicker.invalidate();
    }

    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide) {
            // Звук теперь тоже можно остановить через ClientTicker
            clientTicker.ifPresent(ClientTicker::onRemoved);
        }
        super.setRemoved();
    }

    // ==================== АНИМАЦИОННЫЕ РУКИ ====================
    // @OnlyIn(Dist.CLIENT)
    // public static class AssemblerArm {
    //     public float[] angles = new float[4];
    //     public float[] prevAngles = new float[4];
    //     private float[] targetAngles = new float[4];
    //     private float[] speed = new float[4];
    //     private ArmActionState state = ArmActionState.ASSUME_POSITION;
    //     private int actionDelay = 0;

    //     private enum ArmActionState {
    //         ASSUME_POSITION, EXTEND_STRIKER, RETRACT_STRIKER
    //     }

    //     public AssemblerArm() {
    //         resetSpeed();
    //     }

    //     public void updateInterp() {
    //         System.arraycopy(angles, 0, prevAngles, 0, angles.length);
    //     }

    //     public void returnToNullPos() {
    //         Arrays.fill(targetAngles, 0);
    //         speed[0] = speed[1] = speed[2] = 3;
    //         speed[3] = 0.25f;
    //         state = ArmActionState.RETRACT_STRIKER;
    //         move();
    //     }

    //     private void resetSpeed() {
    //         speed[0] = 15;
    //         speed[1] = 15;
    //         speed[2] = 15;
    //         speed[3] = 0.5f;
    //     }

    //     public void updateArm(Level level, BlockPos pos, RandomSource random) {
    //         resetSpeed();
    //         if (actionDelay > 0) {
    //             actionDelay--;
    //             return;
    //         }
    //         switch (state) {
    //             case ASSUME_POSITION:
    //                 if (move()) {
    //                     actionDelay = 2;
    //                     state = ArmActionState.EXTEND_STRIKER;
    //                     targetAngles[3] = -0.75f;
    //                 }
    //                 break;
    //             case EXTEND_STRIKER:
    //                 if (move()) {
    //                     level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
    //                             ModSounds.ASSEMBLER_STRIKE_RANDOM.get(), SoundSource.BLOCKS, 0.5f, 1.0F, false);
    //                     state = ArmActionState.RETRACT_STRIKER;
    //                     targetAngles[3] = 0f;
    //                 }
    //                 break;
    //             case RETRACT_STRIKER:
    //                 if (move()) {
    //                     actionDelay = 2 + random.nextInt(5);
    //                     chooseNewArmPosition(random);
    //                     state = ArmActionState.ASSUME_POSITION;
    //                 }
    //                 break;
    //         }
    //     }

    //     private static final float[][] POSITIONS = {
    //             {45, -15, -5}, {15, 15, -15}, {25, 10, -15},
    //             {30, 0, -10}, {70, -10, -25}
    //     };

    //     public void chooseNewArmPosition(RandomSource random) {
    //         int chosen = random.nextInt(5);
    //         targetAngles[0] = POSITIONS[chosen][0];
    //         targetAngles[1] = POSITIONS[chosen][1];
    //         targetAngles[2] = POSITIONS[chosen][2];
    //     }

    //     private boolean move() {
    //         boolean allReached = true;
    //         for (int i = 0; i < angles.length; i++) {
    //             float current = angles[i];
    //             float target = targetAngles[i];
    //             if (current == target) continue;
    //             allReached = false;
    //             float delta = target - current;
    //             float absDelta = Math.abs(delta);
    //             if (absDelta <= speed[i]) {
    //                 angles[i] = target;
    //             } else {
    //                 angles[i] += Math.signum(delta) * speed[i];
    //             }
    //         }
    //         return allReached;
    //     }
    // }

    @OnlyIn(Dist.CLIENT)
    public float getRingAngle() {
        if (clientTicker.isPresent()) {
             return ((ClientTicker)clientTicker.orElseThrow(IllegalStateException::new)).ringAngle;
        }
        return 0;
    }
    
    @OnlyIn(Dist.CLIENT)
    public float getPrevRingAngle() {
        if (clientTicker.isPresent()) {
             return ((ClientTicker)clientTicker.orElseThrow(IllegalStateException::new)).prevRingAngle;
        }
        return 0;
    }
    
    @OnlyIn(Dist.CLIENT)
    public ClientTicker.AssemblerArm[] getArms() {
         if (clientTicker.isPresent()) {
             return ((ClientTicker)clientTicker.orElseThrow(IllegalStateException::new)).arms;
        }
        return new ClientTicker.AssemblerArm[0];
    }

    // ==================== КЛИЕНТСКИЙ ТИКЕР ====================
    @OnlyIn(Dist.CLIENT)
    public static class ClientTicker {

        private final AssemblerArm[] arms = new AssemblerArm[2];
        private float ringAngle;
        private float prevRingAngle;
        private float ringTarget;
        private float ringSpeed;
        private int ringDelay;
        private boolean wasCraftingLastTick = false;
        private com.hbm_m.sound.AdvancedAssemblerSoundInstance soundInstance;

        public ClientTicker() {
            for (int i = 0; i < arms.length; i++) {
                arms[i] = new AssemblerArm();
            }
        }

        // Логика из вашего старого clientTick() переезжает сюда
        public void clientTick(Level level, BlockPos pos, BlockState state, MachineAdvancedAssemblerBlockEntity entity) {
            // Обновление звука
            updateSound(entity);

            this.prevRingAngle = this.ringAngle;
            boolean craftingNow = entity.isClientCrafting(); // Используем метод из внешнего класса

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
                if (Math.abs(this.ringAngle) > 0.1f) {
                    this.ringAngle = Mth.lerp(0.1f, this.ringAngle, 0);
                } else {
                    this.ringAngle = 0;
                }
            }
        }
        
        private void updateSound(MachineAdvancedAssemblerBlockEntity entity) {
            boolean isCrafting = entity.isClientCrafting();
            if (isCrafting && (this.soundInstance == null || this.soundInstance.isStopped())) {
                this.soundInstance = new com.hbm_m.sound.AdvancedAssemblerSoundInstance(entity.getBlockPos());
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(this.soundInstance);
            } else if (!isCrafting && this.soundInstance != null && !this.soundInstance.isStopped()) {
                // ИСПРАВЛЕНИЕ: Используем SoundManager для остановки звука
                net.minecraft.client.Minecraft.getInstance().getSoundManager().stop(this.soundInstance);
                this.soundInstance = null;
            }
        }

        public void onRemoved() {
            if (this.soundInstance != null) {
                // ИСПРАВЛЕНИЕ: Используем SoundManager для остановки звука
                net.minecraft.client.Minecraft.getInstance().getSoundManager().stop(this.soundInstance);
                this.soundInstance = null;
            }
        }

        // Класс AssemblerArm теперь находится внутри ClientTicker
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
            int chosen = random.nextInt(5);
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

    @Override
    protected void ensureNetworkInitialized() {
        // Если уже инициализировано - выходим
        if (this.networkInitialized) return;

        // 1. Вызываем родительский метод (он зарегистрирует сам контроллер и поставит flag = true)
        super.ensureNetworkInitialized();

        if (level != null && !level.isClientSide) {
            // 2. Регистрируем все части мультиблока, которые являются коннекторами
            if (getBlockState().getBlock() instanceof MachineAdvancedAssemblerBlock block) {
                MultiblockStructureHelper helper = block.getStructureHelper();
                Direction facing = getBlockState().getValue(MachineAdvancedAssemblerBlock.FACING);

                // Получаем менеджер сети
                EnergyNetworkManager manager = EnergyNetworkManager.get((ServerLevel) level);

                // Проходим по всем частям структуры
                for (BlockPos localPos : helper.getStructureMap().keySet()) {
                    // Если часть является энергетическим коннектором
                    if (block.getPartRole(localPos) == com.hbm_m.multiblock.PartRole.ENERGY_CONNECTOR) {
                        // Вычисляем её реальную позицию в мире
                        BlockPos partWorldPos = helper.getRotatedPos(worldPosition, localPos, facing);

                        // Принудительно добавляем узел части в сеть
                        manager.addNode(partWorldPos);
                    }
                }
            }
        }
    }
}
