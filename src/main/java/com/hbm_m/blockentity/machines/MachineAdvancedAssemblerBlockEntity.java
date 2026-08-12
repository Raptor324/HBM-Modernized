package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.interfaces.IFrameSupportable;
import com.hbm_m.interfaces.IMultiblockSidedIO;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineAdvancedAssemblerMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemBlueprintFolder;
import com.hbm_m.module.machine.MachineModuleAdvancedAssembler;
import com.hbm_m.multiblock.MultiblockFrameHelper;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.recipe.RecipeHooks;

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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.DistExecutor;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import team.reborn.energy.api.EnergyStorage;
import com.hbm_m.client.machine.AdvancedAssemblerClientTicker;
import com.hbm_m.platform.PlatformHooks;
*///?}
/**
 * Advanced Assembler Block Entity:
 * - Наследование от BaseMachineBlockEntity
 * - Полная логика мультиблока, GUI, анимации и рецептов перенесена из старой системы
 * - Энергосистема остается long-ориентированной с совместимостью Forge Energy
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineAdvancedAssemblerBlockEntity extends BaseMachineBlockEntity implements IFrameSupportable, IMultiblockSidedIO {

    private static final int SLOT_COUNT = 17;
    private static final int ENERGY_SLOT = 0;
    private static final int BLUEPRINT_FOLDER_SLOT = 1;
    private static final int OUTPUT_SLOT_START = 2;
    private static final int OUTPUT_SLOT_END = 3;
    private static final int INPUT_SLOT_START = 4;
    private static final int INPUT_SLOT_END = 15;
    private static final int STAMPING_SLOT = 16;


    // Флюиды
    private final FluidTank inputTank = new FluidTank(4000) {
        @Override
        public void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank outputTank = new FluidTank(4000) {
        @Override
        public void onContentsChanged() {
            setChanged();
        }
    };

    /** Разрешённые стороны прямого подключения к контроллеру (пусто = все). */
    private java.util.Set<Direction> allowedEnergySides = java.util.EnumSet.noneOf(Direction.class);
    /** Разрешённые стороны жидкости. Если {@link #fluidSidesFromMultiblockStructure} — пусто может означать «ни одной»; иначе пусто = все. */
    private java.util.Set<Direction> allowedFluidSides = java.util.EnumSet.noneOf(Direction.class);
    private boolean fluidSidesFromMultiblockStructure = false;

    // Выбор рецепта и кеш
    @Nullable private ResourceLocation selectedRecipeId = null;
    @Nullable private AssemblerRecipe cachedRecipe = null;
    private boolean recipeCacheDirty = false;

    // Модуль крафта
    @Nullable private MachineModuleAdvancedAssembler assemblerModule = null;

    // Крафт состояние на клиенте
    private boolean clientIsCrafting = false;
    public long lastUseTick = 0;

    private boolean needsClientSync = false;
    private int ticksSinceLastSync = 0;

    private static Object newAdvAssemblerClientTickerInstance() {
        try {
            return Class.forName("com.hbm_m.client.machine.AdvancedAssemblerClientTicker").getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private Object clientTicker = null;

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
        //? if forge {
        // Экспонируем входной бак (inputTank) через базовый fluidHandlerOpt.
        setFluidHandler(inputTank);
        //?}
    }

    // Ограничиваем валидность слотов
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == ENERGY_SLOT) {
            return isEnergyProviderItem(stack);
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

    // Крупный AABB под анимации и структуру.
    //
    // Делегирует MultiblockStructureHelper, который кэширует AABB по facing на
    // ВЕСЬ helper (а не на каждый BE) - один computeIfAbsent на 4 направления и
    // дальше O(1) lookup. Раньше мы каждый раз перебирали structureMap.keySet()
    // вручную и не учитывали facing rotation, что давало неправильный bbox при
    // несимметричных структурах и постоянно слегка путало occlusion culler.
    // 1.5-блочный inflate сохранён - дальние Spike-части анимации руки
    // вылезают за статическую footprint structure'ы.
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MachineAdvancedAssemblerBlock block)) {
            BlockPos min = worldPosition.offset(-2, -1, -2);
            BlockPos max = worldPosition.offset(3, 4, 3);
            return new AABB(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        }
        Direction facing = state.getValue(MachineAdvancedAssemblerBlock.FACING);
        return block.getStructureHelper().getRenderBoundingBox(worldPosition, facing, 1.5);
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
        if (level != null && !level.isClientSide) {
            return MultiblockFrameHelper.applyFrameToBlockState(level, worldPosition, visible);
        }
        return false;
    }

    @Override
    public boolean isFrameVisible() {
        return MultiblockFrameHelper.isFrameVisible(getBlockState());
    }

    // Tick-хуки
    public static void tick(Level level, BlockPos pos, BlockState state, MachineAdvancedAssemblerBlockEntity entity) {
        if (level.isClientSide) {
            if (entity.clientTicker == null) {
                entity.clientTicker = newAdvAssemblerClientTickerInstance();
            }
            invokeAdvAssemblerClientTick(entity.clientTicker, level, pos, state, entity);
        } else {
            entity.serverTick();
        }
    }

    private static final String ADV_ASM_CLIENT_TICKER = "com.hbm_m.client.machine.AdvancedAssemblerClientTicker";

    private static volatile java.lang.reflect.Method cachedAdvAsmClientTick;
    private static volatile java.lang.reflect.Method cachedAdvAsmOnRemoved;
    private static volatile java.lang.reflect.Method cachedAdvAsmGetRingAngle;
    private static volatile java.lang.reflect.Method cachedAdvAsmGetPrevRingAngle;
    private static volatile java.lang.reflect.Method cachedAdvAsmGetArms;
    private static volatile Class<?> cachedAdvAsmArmComponentType;

    private static void ensureAdvAssemblerReflectCache() {
        if (cachedAdvAsmGetRingAngle != null) {
            return;
        }
        synchronized (MachineAdvancedAssemblerBlockEntity.class) {
            if (cachedAdvAsmGetRingAngle != null) {
                return;
            }
            try {
                Class<?> cl = Class.forName(ADV_ASM_CLIENT_TICKER);
                cachedAdvAsmClientTick = cl.getMethod("clientTick", Level.class, BlockPos.class, BlockState.class, MachineAdvancedAssemblerBlockEntity.class);
                cachedAdvAsmOnRemoved = cl.getMethod("onRemoved");
                cachedAdvAsmGetRingAngle = cl.getMethod("getRingAngle");
                cachedAdvAsmGetPrevRingAngle = cl.getMethod("getPrevRingAngle");
                cachedAdvAsmGetArms = cl.getMethod("getArms");
                cachedAdvAsmArmComponentType = Class.forName(ADV_ASM_CLIENT_TICKER + "$AssemblerArm");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void invokeAdvAssemblerClientTick(Object ticker, Level level, BlockPos pos, BlockState state, MachineAdvancedAssemblerBlockEntity entity) {
        ensureAdvAssemblerReflectCache();
        try {
            cachedAdvAsmClientTick.invoke(ticker, level, pos, state, entity);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void invokeAdvAssemblerClientTickerOnRemoved(Object ticker) {
        ensureAdvAssemblerReflectCache();
        try {
            cachedAdvAsmOnRemoved.invoke(ticker);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }


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
                            // return;
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
                ResourceLocation autoSelectedRecipeId = RecipeHooks.recipeId(level.getRecipeManager(), AssemblerRecipe.Type.INSTANCE, assemblerModule.getPreferredRecipe());
                if (selectedRecipeId == null || !selectedRecipeId.equals(autoSelectedRecipeId)) {
                    selectedRecipeId = autoSelectedRecipeId;
                    cachedRecipe = assemblerModule.getPreferredRecipe();
                    recipeCacheDirty = false;
                    needsClientSync = true;
                }
            }

            if (assemblerModule.getCurrentRecipe() != null) {
                ResourceLocation currentRecipeId = RecipeHooks.recipeId(level.getRecipeManager(), AssemblerRecipe.Type.INSTANCE, assemblerModule.getCurrentRecipe());
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

        //? if forge {
        energySourceStack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
            long energyNeeded = this.getMaxEnergyStored() - this.getEnergyStored();
            if (energyNeeded <= 0) return;

            int maxTransfer = (int) Math.min(Integer.MAX_VALUE, Math.min(energyNeeded, this.getReceiveSpeed()));
            int extracted = itemEnergy.extractEnergy(maxTransfer, false);

            if (extracted > 0) {
                this.setEnergyStored(this.getEnergyStored() + extracted);
                setChanged();
            }
        });
        //?}

        //? if fabric {
        /*var itemEnergy = EnergyStorage.ITEM.find(energySourceStack, null);
        if (itemEnergy == null) return;

        long energyNeeded = this.getMaxEnergyStored() - this.getEnergyStored();
        if (energyNeeded <= 0) return;

        long maxTransfer = Math.min(energyNeeded, this.getReceiveSpeed());
        if (maxTransfer <= 0) return;

        try (Transaction tx = Transaction.openOuter()) {
            long extracted = itemEnergy.extract(maxTransfer, tx);
            if (extracted > 0) {
                setEnergyStored(getEnergyStored() + extracted);
                tx.commit();
            }
        }
        *///?}

        //? if neoforge {
        /*var itemEnergy = energySourceStack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        if (itemEnergy == null) return;

        long energyNeeded = this.getMaxEnergyStored() - this.getEnergyStored();
        if (energyNeeded <= 0) return;

        int maxTransfer = (int) Math.min(Integer.MAX_VALUE, Math.min(energyNeeded, this.getReceiveSpeed()));
        if (maxTransfer <= 0) return;

        int extracted = itemEnergy.extractEnergy(maxTransfer, false);
        if (extracted > 0) {
            this.setEnergyStored(this.getEnergyStored() + extracted);
            setChanged();
        }
        *///?}
    }

    // Рецепты и ghost-предметы
    @Nullable
    private AssemblerRecipe getCachedRecipe() {
        if (selectedRecipeId == null || level == null) {
            cachedRecipe = null;
            return null;
        }
        if (cachedRecipe == null || recipeCacheDirty) {
            cachedRecipe = RecipeHooks.getRecipeByKey(level.getRecipeManager(), selectedRecipeId)
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
        List<AssemblerRecipe> allRecipes = com.hbm_m.recipe.index.ModRecipeIndex.of(level.getRecipeManager())
                .getAll(AssemblerRecipe.Type.INSTANCE);
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
    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("input_tank", inputTank.writeNBT(new CompoundTag()));
        tag.put("output_tank", outputTank.writeNBT(new CompoundTag()));
        tag.putLong("last_use_tick", lastUseTick);
        // Frame хранится в BlockState (FRAME property)
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

        if (!allowedEnergySides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedEnergySides) mask |= (1 << dir.get3DDataValue());
            tag.putInt("AllowedEnergySides", mask);
        }
        if (fluidSidesFromMultiblockStructure) {
            tag.putBoolean("FluidSidesFromMbStructure", true);
            int mask = 0;
            for (Direction dir : allowedFluidSides) {
                mask |= (1 << dir.get3DDataValue());
            }
            tag.putInt("AllowedFluidSides", mask);
        } else if (!allowedFluidSides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedFluidSides) mask |= (1 << dir.get3DDataValue());
            tag.putInt("AllowedFluidSides", mask);
        }
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.put("input_tank", inputTank.writeNBT(new CompoundTag()));
        tag.put("output_tank", outputTank.writeNBT(new CompoundTag()));
        tag.putLong("last_use_tick", lastUseTick);
        // Frame хранится в BlockState (FRAME property)
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

        if (!allowedEnergySides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedEnergySides) mask |= (1 << dir.get3DDataValue());
            tag.putInt("AllowedEnergySides", mask);
        }
        if (fluidSidesFromMultiblockStructure) {
            tag.putBoolean("FluidSidesFromMbStructure", true);
            int mask = 0;
            for (Direction dir : allowedFluidSides) {
                mask |= (1 << dir.get3DDataValue());
            }
            tag.putInt("AllowedFluidSides", mask);
        } else if (!allowedFluidSides.isEmpty()) {
            int mask = 0;
            for (Direction dir : allowedFluidSides) mask |= (1 << dir.get3DDataValue());
            tag.putInt("AllowedFluidSides", mask);
        }
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        inputTank.readNBT(tag.getCompound("input_tank"));
        outputTank.readNBT(tag.getCompound("output_tank"));
        lastUseTick = tag.getLong("last_use_tick");
        // Миграция: старые сохранения имели FrameVisible в NBT - синхронизируем в BlockState
        if (tag.contains("FrameVisible") && level != null && !level.isClientSide) {
            MultiblockFrameHelper.applyFrameToBlockState(level, worldPosition, tag.getBoolean("FrameVisible"));
        }
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

        if (tag.contains("AllowedEnergySides")) {
            int mask = tag.getInt("AllowedEnergySides");
            allowedEnergySides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) {
                    allowedEnergySides.add(dir);
                }
            }
        }
        if (tag.contains("AllowedFluidSides")) {
            int mask = tag.getInt("AllowedFluidSides");
            allowedFluidSides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) {
                    allowedFluidSides.add(dir);
                }
            }
        }
        if (tag.contains("FluidSidesFromMbStructure")) {
            fluidSidesFromMultiblockStructure = tag.getBoolean("FluidSidesFromMbStructure");
        }
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);

        inputTank.readNBT(tag.getCompound("input_tank"));
        outputTank.readNBT(tag.getCompound("output_tank"));
        lastUseTick = tag.getLong("last_use_tick");
        // Миграция: старые сохранения имели FrameVisible в NBT - синхронизируем в BlockState
        if (tag.contains("FrameVisible") && level != null && !level.isClientSide) {
            MultiblockFrameHelper.applyFrameToBlockState(level, worldPosition, tag.getBoolean("FrameVisible"));
        }
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

        if (tag.contains("AllowedEnergySides")) {
            int mask = tag.getInt("AllowedEnergySides");
            allowedEnergySides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) {
                    allowedEnergySides.add(dir);
                }
            }
        }
        if (tag.contains("AllowedFluidSides")) {
            int mask = tag.getInt("AllowedFluidSides");
            allowedFluidSides.clear();
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.get3DDataValue())) != 0) {
                    allowedFluidSides.add(dir);
                }
            }
        }
        if (tag.contains("FluidSidesFromMbStructure")) {
            fluidSidesFromMultiblockStructure = tag.getBoolean("FluidSidesFromMbStructure");
        }
    
    }
    *///?}

    // Пакеты синхронизации
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = PlatformHooks.getItemTag(pkt);
        if (tag != null) {
            //? if < 1.21.1 {
            load(tag);
            //?} else {
            /*if (level != null) {
                loadAdditional(tag, level.registryAccess());
            }
            *///?}
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        // Минимальная совместимость по сигнатуре
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if < 1.21.1 {
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {

        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    
    }
    *///?}

    // Capability: используем базовые item/energy/fluids, плюс локальные хэндлеры флюидов
    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }

        // === Sided IO для контроллера ===
        if (side != null) {
            boolean wantsEnergy =
                    cap == ModCapabilities.HBM_ENERGY_PROVIDER ||
                            cap == ModCapabilities.HBM_ENERGY_RECEIVER ||
                            cap == ModCapabilities.HBM_ENERGY_CONNECTOR ||
                            cap == ForgeCapabilities.ENERGY;
            if (wantsEnergy && !allowedEnergySides.isEmpty() && !allowedEnergySides.contains(side)) {
                return LazyOptional.empty();
            }
            if (cap == ForgeCapabilities.FLUID_HANDLER && side != null) {
                if (fluidSidesFromMultiblockStructure) {
                    if (!allowedFluidSides.contains(side)) {
                        return LazyOptional.empty();
                    }
                } else if (!allowedFluidSides.isEmpty() && !allowedFluidSides.contains(side)) {
                    return LazyOptional.empty();
                }
            }
        }

        // FLUID_HANDLER для разрешённых сторон отдаёт базовый fluidHandlerOpt (см. setupFluidCapability).
        return super.getCapability(cap, side);
    }
    //?}

    //? if fabric {
    /*@Nullable
    public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) {
        if (side != null) {
            if (fluidSidesFromMultiblockStructure) {
                if (!allowedFluidSides.contains(side)) return null;
            } else if (!allowedFluidSides.isEmpty() && !allowedFluidSides.contains(side)) return null;
        }
        // По умолчанию: вниз = вход, вверх = выход, иначе вход
        if (side == Direction.UP) return outputTank.getStorage();
        return inputTank.getStorage();
    }
    *///?}

    @Override
    public void setAllowedEnergySides(java.util.Set<Direction> sides) {
        this.allowedEnergySides = java.util.EnumSet.copyOf(sides);
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public java.util.Set<Direction> getAllowedEnergySides() {
        return this.allowedEnergySides;
    }

    @Override
    public void setAllowedFluidSidesFromMultiblockStructure(java.util.Set<Direction> sides) {
        this.allowedFluidSides = java.util.EnumSet.copyOf(sides);
        this.fluidSidesFromMultiblockStructure = true;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public void setAllowedFluidSides(java.util.Set<Direction> sides) {
        this.allowedFluidSides = java.util.EnumSet.copyOf(sides);
        this.fluidSidesFromMultiblockStructure = false;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public java.util.Set<Direction> getAllowedFluidSides() {
        return this.allowedFluidSides;
    }

    //? if forge {
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && assemblerModule == null) {
            this.assemblerModule = new MachineModuleAdvancedAssembler(0, this, this.inventory, this.level);
        }
    }
    //?}

    //? if forge {
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
    }
    //?}

    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide && clientTicker != null) {
            invokeAdvAssemblerClientTickerOnRemoved(clientTicker);
        }
        super.setRemoved();
    }

    //? if fabric {
    /*@Environment(EnvType.CLIENT)
    public float getRingAngle() {
        return clientTicker != null ? clientTicker.getRingAngle() : 0;
    }

    @Environment(EnvType.CLIENT)
    public float getPrevRingAngle() {
        return clientTicker != null ? clientTicker.getPrevRingAngle() : 0;
    }

    @Environment(EnvType.CLIENT)
    public AdvancedAssemblerClientTicker.AssemblerArm[] getArms() {
        return clientTicker != null ? clientTicker.getArms() : new AdvancedAssemblerClientTicker.AssemblerArm[0];
    }

    @Environment(EnvType.CLIENT)
    @Nullable
    private ResourceLocation clientRecipeIconCacheId;

    @Environment(EnvType.CLIENT)
    private ItemStack clientRecipeIconCache = ItemStack.EMPTY;

    @Environment(EnvType.CLIENT)
    public ItemStack getClientRecipeIcon() {
        ResourceLocation id = selectedRecipeId;
        if (id == null) {
            clientRecipeIconCacheId = null;
            clientRecipeIconCache = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }
        if (id.equals(clientRecipeIconCacheId)) {
            return clientRecipeIconCache;
        }
        clientRecipeIconCacheId = id;
        Level level = getLevel();
        if (level == null) {
            clientRecipeIconCache = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }
        clientRecipeIconCache = RecipeHooks.getRecipeByKey(level.getRecipeManager(), id)
                .filter(r -> r instanceof AssemblerRecipe)
                .map(r -> ((AssemblerRecipe) r).getResultItemSafe())
                .orElse(ItemStack.EMPTY);
        return clientRecipeIconCache;
    }
    *///?}

    //? if forge || neoforge {
    @Nullable
    private ResourceLocation clientRecipeIconCacheId;

    private ItemStack clientRecipeIconCache = ItemStack.EMPTY;

    /** Cached recipe output icon for BER; refreshed when {@link #selectedRecipeId} changes. */
    @OnlyIn(Dist.CLIENT)
    public ItemStack getClientRecipeIcon() {
        ResourceLocation id = selectedRecipeId;
        if (id == null) {
            clientRecipeIconCacheId = null;
            clientRecipeIconCache = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }
        if (id.equals(clientRecipeIconCacheId)) {
            return clientRecipeIconCache;
        }
        clientRecipeIconCacheId = id;
        Level level = getLevel();
        if (level == null) {
            clientRecipeIconCache = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }
        clientRecipeIconCache = RecipeHooks.getRecipeByKey(level.getRecipeManager(), id)
                .filter(r -> r instanceof AssemblerRecipe)
                .map(r -> ((AssemblerRecipe) r).getResultItemSafe())
                .orElse(ItemStack.EMPTY);
        return clientRecipeIconCache;
    }

    @OnlyIn(Dist.CLIENT)
    public float getRingAngle() {
        if (clientTicker == null) {
            return 0f;
        }
        ensureAdvAssemblerReflectCache();
        try {
            return (Float) cachedAdvAsmGetRingAngle.invoke(clientTicker);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public float getPrevRingAngle() {
        if (clientTicker == null) {
            return 0f;
        }
        ensureAdvAssemblerReflectCache();
        try {
            return (Float) cachedAdvAsmGetPrevRingAngle.invoke(clientTicker);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public Object getArms() {
        if (clientTicker == null) {
            return emptyAssemblerArmsArray();
        }
        ensureAdvAssemblerReflectCache();
        try {
            return cachedAdvAsmGetArms.invoke(clientTicker);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object emptyAssemblerArmsArray() {
        ensureAdvAssemblerReflectCache();
        return java.lang.reflect.Array.newInstance(cachedAdvAsmArmComponentType, 0);
    }
    //?}

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
