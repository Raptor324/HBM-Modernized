package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.PlatformHooks;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.machines.MachineAssemblerBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.inventory.menu.MachineAssemblerMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;
import com.hbm_m.sound.ClientSoundBootstrap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;
*///?}

/**
 * РЎР±РѕСЂРѕС‡РЅР°СЏ РјР°С€РёРЅР° (Assembler) - РјСѓР»СЊС‚РёР±Р»РѕС‡РЅР°СЏ СЃС‚СЂСѓРєС‚СѓСЂР° РґР»СЏ Р°РІС‚РѕРјР°С‚РёР·РёСЂРѕРІР°РЅРЅРѕРіРѕ РєСЂР°С„С‚Р°.
 * РђРґР°РїС‚РёСЂРѕРІР°РЅРѕ РґР»СЏ long-СЌРЅРµСЂРіРѕСЃРёСЃС‚РµРјС‹ СЃ РЅР°СЃР»РµРґРѕРІР°РЅРёРµРј РѕС‚ BaseMachineBlockEntity.
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineAssemblerBlockEntity extends BaseMachineBlockEntity {

    private static final String ASSEMBLER_SOUND_INSTANCE = "com.hbm_m.sound.AssemblerSoundInstance";

    // РЎР»РѕС‚С‹
    private static final int SLOT_COUNT = 18;
    private static final int ENERGY_SLOT = 0;
    private static final int TEMPLATE_SLOT = 4;
    private static final int OUTPUT_SLOT = 5;
    private static final int INPUT_SLOT_START = 6;
    private static final int INPUT_SLOT_END = 17;

    // РЎРѕСЃС‚РѕСЏРЅРёРµ РєСЂР°С„С‚Р°
    private boolean isCrafting = false;
    private int progress = 0;
    private int maxProgress = 100;

    // Proxy handlers РґР»СЏ multiblock parts
    //? if forge {
    private LazyOptional<IItemHandler> lazyInputProxy = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyOutputProxy = LazyOptional.empty();
    //?}

    //? if fabric {
    /*@Nullable private Storage<ItemVariant> inputProxy;
    @Nullable private Storage<ItemVariant> outputProxy;
    *///?}

    // РћС‚СЃР»РµР¶РёРІР°РЅРёРµ РёСЃС‚РѕС‡РЅРёРєРѕРІ РїСЂРµРґРјРµС‚РѕРІ
    private final Set<BlockPos> lastPullSources = new HashSet<>();

    // ContainerData РґР»СЏ GUI СЃ СѓРїР°РєРѕРІРєРѕР№ long
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> isCrafting ? 1 : 0; // РРЅРґРµРєСЃ СЃС‚Р°Р» 2!
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> isCrafting = value != 0;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public MachineAssemblerBlockEntity(BlockPos pos, BlockState state) {
        // Р’С‹Р·С‹РІР°РµРј РєРѕРЅСЃС‚СЂСѓРєС‚РѕСЂ СЂРѕРґРёС‚РµР»СЏ СЃ РїР°СЂР°РјРµС‚СЂР°РјРё: (..., inventorySize, capacity, receiveRate)
        super(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), pos, state,
                SLOT_COUNT, // 18
                100_000L,   // Р•РјРєРѕСЃС‚СЊ
                100_000L);  // РЎРєРѕСЂРѕСЃС‚СЊ РїСЂРёРµРјР°
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_assembler");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }


    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == ENERGY_SLOT) {
            if (stack.getItem() instanceof ModBatteryItem) {
                return true;
            }
            return isEnergyProviderItem(stack);
        }
        if (slot == TEMPLATE_SLOT) {
            return stack.getItem() instanceof ItemAssemblyTemplate;
        }
        if (slot == OUTPUT_SLOT) {
            return false;
        }
        return slot >= INPUT_SLOT_START && slot <= INPUT_SLOT_END;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        sendUpdateToClient();
        return new MachineAssemblerMenu(containerId, playerInventory, this, this.data);
    }

    // ==================== MULTIBLOCK PART SUPPORT ====================

    //? if forge {
    public LazyOptional<IItemHandler> getItemHandlerForPart(PartRole role) {
        if (role == PartRole.ITEM_INPUT) {
            if (!lazyInputProxy.isPresent()) {
                lazyInputProxy = LazyOptional.of(this::createInputProxy);
            }
            return lazyInputProxy;
        }
        if (role == PartRole.ITEM_OUTPUT) {
            if (!lazyOutputProxy.isPresent()) {
                lazyOutputProxy = LazyOptional.of(this::createOutputProxy);
            }
            return lazyOutputProxy;
        }
        return LazyOptional.empty();
    }

    @NotNull
    private IItemHandler createInputProxy() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return INPUT_SLOT_END - INPUT_SLOT_START + 1;
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int slot) {
                return inventory.getStackInSlot(slot + INPUT_SLOT_START);
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return inventory.insertItem(slot + INPUT_SLOT_START, stack, simulate);
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return inventory.getSlotLimit(slot + INPUT_SLOT_START);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return inventory.isItemValid(slot + INPUT_SLOT_START, stack);
            }
        };
    }

    @NotNull
    private IItemHandler createOutputProxy() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot == 0 ? inventory.getStackInSlot(OUTPUT_SLOT) : ItemStack.EMPTY;
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return stack;
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot == 0 ? inventory.extractItem(OUTPUT_SLOT, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == 0 ? inventory.getSlotLimit(OUTPUT_SLOT) : 0;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return false;
            }
        };
    }
    //?}

    //? if fabric {
    /*public Storage<ItemVariant> getItemStorageForPart(PartRole role) {
        if (role == PartRole.ITEM_INPUT) {
            if (inputProxy == null) inputProxy = createInputProxyStorage();
            return inputProxy;
        }
        if (role == PartRole.ITEM_OUTPUT) {
            if (outputProxy == null) outputProxy = createOutputProxyStorage();
            return outputProxy;
        }
        return Storage.empty();
    }

    private Storage<ItemVariant> createInputProxyStorage() {
        java.util.List<SingleSlotStorage<ItemVariant>> slots = new java.util.ArrayList<>();
        for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
            slots.add(inventory.getSlotStorage(i));
        }
        return new CombinedStorage<>(slots);
    }

    private Storage<ItemVariant> createOutputProxyStorage() {
        return inventory.getSlotStorage(OUTPUT_SLOT);
    }
    *///?}

    // ==================== TICK LOGIC ====================

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAssemblerBlockEntity entity) {
        if (level.isClientSide) {
            entity.clientTick();
        } else {
            entity.serverTick();
        }
    }

    private void clientTick() {

        ClientSoundBootstrap.updateSound(this, this.isCrafting(), () -> newAssemblerSoundInstance());
    }

    private Object newAssemblerSoundInstance() {
        try {
            return Class.forName(ASSEMBLER_SOUND_INSTANCE).getConstructor(BlockPos.class).newInstance(this.getBlockPos());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void serverTick() {

        ensureNetworkInitialized();

        long gameTime = level.getGameTime();

        chargeFromEnergySlot();

        if (gameTime % 10 == 0) {
            updateEnergyDelta(this.getEnergyStored());
        }

        Optional<AssemblerRecipe> recipeOpt = getRecipeFromTemplate();

        if (recipeOpt.isPresent()) {
            pullIngredientsForOneCraft(recipeOpt.get());
        }

        boolean hasRecipe = recipeOpt.isPresent();
        boolean hasResources = hasRecipe && hasResources(recipeOpt.get());
        boolean canInsert = hasRecipe && canInsertResult(recipeOpt.get().getResultItemSafe());

        if (hasRecipe && hasResources && canInsert) {
            AssemblerRecipe recipe = recipeOpt.get();
            long energyPerTick = recipe.getPowerConsumption();

            if (this.getEnergyStored() >= energyPerTick) {
                if (!isCrafting) {
                    isCrafting = true;
                    maxProgress = recipe.getDuration();
                    setChanged();
                    sendUpdateToClient();
                }

                this.setEnergyStored(this.getEnergyStored() - energyPerTick);
                progress++;
                setChanged();

                if (progress >= maxProgress) {
                    craftItem(recipe);
                    progress = 0;
                    pushOutputToNeighbors();
                    getRecipeFromTemplate().ifPresent(this::pullIngredientsForOneCraft);
                }
            } else {
                stopCrafting();
            }
        } else {
            stopCrafting();
        }
    }

    private void stopCrafting() {
        if (isCrafting) {
            progress = 0;
            isCrafting = false;
            setChanged();
            sendUpdateToClient();
        }
    }

    // ==================== ENERGY ====================

    private void chargeFromEnergySlot() {
        ItemStack energySourceStack = inventory.getStackInSlot(ENERGY_SLOT);
        if (energySourceStack.isEmpty()) return;

        // РљСЂРµР°С‚РёРІРЅР°СЏ Р±Р°С‚Р°СЂРµСЏ
        if (energySourceStack.getItem() instanceof ItemCreativeBattery) {
            this.setEnergyStored(this.getMaxEnergyStored());
            return;
        }

        //? if forge {
        // РћР±С‹С‡РЅР°СЏ Р±Р°С‚Р°СЂРµСЏ С‡РµСЂРµР· HBM capability
        energySourceStack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).ifPresent(itemEnergy -> {
            long energyNeeded = this.getMaxEnergyStored() - this.getEnergyStored();
            if (energyNeeded <= 0) return;

            long maxCanReceive = this.getReceiveSpeed();
            long energyToTransfer = Math.min(energyNeeded, maxCanReceive);

            if (energyToTransfer > 0) {
                long extracted = itemEnergy.extractEnergy(energyToTransfer, false);
                if (extracted > 0) {
                    this.setEnergyStored(this.getEnergyStored() + extracted);
                    setChanged();
                }
            }
        });

        // Fallback РЅР° Forge Energy РґР»СЏ СЃРѕРІРјРµСЃС‚РёРјРѕСЃС‚Рё
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
    }



    // ==================== CRAFTING ====================

    private Optional<AssemblerRecipe> getRecipeFromTemplate() {
        ItemStack templateStack = inventory.getStackInSlot(TEMPLATE_SLOT);
        if (!(templateStack.getItem() instanceof ItemAssemblyTemplate)) {
            return Optional.empty();
        }

        ItemStack outputStack = ItemAssemblyTemplate.getRecipeOutput(templateStack);
        if (outputStack.isEmpty()) {
            return Optional.empty();
        }

        return RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE).stream()
                .filter(r -> PlatformHooks.isSameItemSameTags(r.getResultItemSafe(), outputStack))
                .findFirst();
    }

    @Override
    public NonNullList<ItemStack> getGhostItems() {
        Optional<AssemblerRecipe> recipeOpt = getRecipeFromTemplate();

        if (recipeOpt.isEmpty()) {
            return NonNullList.create();
        }

        AssemblerRecipe recipe = recipeOpt.get();
        return BaseMachineBlockEntity.createGhostItemsFromIngredients(recipe.getIngredients());
    }

    private boolean hasResources(AssemblerRecipe recipe) {
        SimpleContainer container = new SimpleContainer(INPUT_SLOT_END - INPUT_SLOT_START + 1);
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, inventory.getStackInSlot(INPUT_SLOT_START + i));
        }
        // 1.21.1: SimpleContainer больше не RecipeInput — используем RecipeInputWrapper + matchesRecipe.
        return recipe.matchesRecipe(new RecipeInputWrapper(container), level);
    }

    private boolean canInsertResult(ItemStack result) {
        ItemStack outputSlotStack = inventory.getStackInSlot(OUTPUT_SLOT);
        return outputSlotStack.isEmpty() ||
                (PlatformHooks.isSameItemSameTags(outputSlotStack, result) &&
                        outputSlotStack.getCount() + result.getCount() <= outputSlotStack.getMaxStackSize());
    }

    private void craftItem(AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        ItemStack result = recipe.getResultItemSafe().copy();

        for (Ingredient ingredient : ingredients) {
            for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                ItemStack stackInSlot = inventory.getStackInSlot(i);
                if (ingredient.test(stackInSlot)) {
                    inventory.extractItem(i, 1, false);
                    break;
                }
            }
        }

        ItemStack outputSlot = inventory.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            outputSlot.grow(result.getCount());
        }

        setChanged();
        sendUpdateToClient();
    }

    // ==================== AUTOMATION ====================

    private void pullIngredientsForOneCraft(AssemblerRecipe recipe) {
        if (level == null || hasResources(recipe)) return;

        lastPullSources.clear();

        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        Map<Ingredient, Integer> required = new IdentityHashMap<>();
        for (Ingredient ing : ingredients) {
            required.put(ing, required.getOrDefault(ing, 0) + 1);
        }

        Direction facing = getBlockState().getValue(MachineAssemblerBlock.FACING);
        MultiblockStructureHelper helper = ((MachineAssemblerBlock) getBlockState().getBlock()).getStructureHelper();

        for (BlockPos localOffset : helper.getPartOffsets()) {
            int x = localOffset.getX();
            int y = localOffset.getY();
            int z = localOffset.getZ();
            boolean isInputConveyor = (y == 0) && (x == 2) && (z == 0 || z == 1);

            if (!isInputConveyor) continue;

            BlockPos partPos = helper.getRotatedPos(worldPosition, localOffset, facing);
            BlockEntity partBE = level.getBlockEntity(partPos);

            if (!(partBE instanceof UniversalMachinePartBlockEntity)) continue;

            int dx = Integer.signum(partPos.getX() - worldPosition.getX());
            int dz = Integer.signum(partPos.getZ() - worldPosition.getZ());
            BlockPos neighborPosGlobal = partPos.offset(dx, 0, dz);
            BlockEntity neighbor = level.getBlockEntity(neighborPosGlobal);

            if (neighbor == null || neighbor instanceof UniversalMachinePartBlockEntity ||
                    neighbor == this || lastPullSources.contains(neighborPosGlobal)) continue;

            int dxN = partPos.getX() - neighborPosGlobal.getX();
            int dzN = partPos.getZ() - neighborPosGlobal.getZ();
            Direction dirToNeighbor;

            if (dxN == 1 && dzN == 0) dirToNeighbor = Direction.EAST;
            else if (dxN == -1 && dzN == 0) dirToNeighbor = Direction.WEST;
            else if (dxN == 0 && dzN == 1) dirToNeighbor = Direction.SOUTH;
            else if (dxN == 0 && dzN == -1) dirToNeighbor = Direction.NORTH;
            else continue;

            for (Map.Entry<Ingredient, Integer> entry : required.entrySet()) {
                Ingredient ingredient = entry.getKey();
                int need = entry.getValue();

                int present = 0;
                for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                    ItemStack s = inventory.getStackInSlot(i);
                    if (!s.isEmpty() && ingredient.test(s)) {
                        present += s.getCount();
                    }
                }

                int missing = need - present;
                if (missing <= 0) continue;

                //? if forge {
                IItemHandler cap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dirToNeighbor).orElse(null);
                if (cap == null) continue;

                for (int slot = 0; slot < cap.getSlots() && missing > 0; slot++) {
                    ItemStack possible = cap.getStackInSlot(slot);
                    if (possible.isEmpty() || !ingredient.test(possible)) continue;

                    ItemStack simulated = cap.extractItem(slot, missing, true);
                    if (simulated.isEmpty()) continue;

                    ItemStack toInsert = simulated.copy();
                    for (int dest = INPUT_SLOT_START; dest <= INPUT_SLOT_END && !toInsert.isEmpty(); dest++) {
                        ItemStack remain = inventory.insertItem(dest, toInsert.copy(), true);
                        int inserted = toInsert.getCount() - remain.getCount();

                        if (inserted > 0) {
                            ItemStack actuallyExtracted = cap.extractItem(slot, inserted, false);
                            inventory.insertItem(dest, actuallyExtracted.copy(), false);
                            lastPullSources.add(neighborPosGlobal);
                            setChanged();
                            missing -= inserted;
                            toInsert = remain;
                        }
                    }
                }
                //?}

                //? if fabric {
                /*Storage<ItemVariant> cap = ItemStorage.SIDED.find(level, neighborPosGlobal, dirToNeighbor);
                if (cap == null) continue;

                // Р’С‹С‚Р°СЃРєРёРІР°РµРј РїРѕ РѕРґРЅРѕРјСѓ РґРѕ missing (Transfer API РѕРїРµСЂРёСЂСѓРµС‚ ItemVariant/count)
                // Рё РїС‹С‚Р°РµРјСЃСЏ РІСЃС‚Р°РІРёС‚СЊ РІ РЅР°С€Рё РІС…РѕРґРЅС‹Рµ СЃР»РѕС‚С‹.
                for (int attempt = 0; attempt < missing; attempt++) {
                    boolean movedOne = false;
                    try (Transaction tx = Transaction.openOuter()) {
                        for (var view : cap) {
                            ItemVariant v = view.getResource();
                            if (v.isBlank()) continue;
                            ItemStack one = v.toStack(1);
                            if (!ingredient.test(one)) continue;

                            long extracted = view.extract(v, 1, tx);
                            if (extracted != 1) continue;

                            // Р’СЃС‚Р°РІР»СЏРµРј 1 РїСЂРµРґРјРµС‚ РІРѕ РІС…РѕРґРЅС‹Рµ СЃР»РѕС‚С‹ (С‡РµСЂРµР· ModItemStackHandler insertItem)
                            ItemStack toInsert = one;
                            for (int dest = INPUT_SLOT_START; dest <= INPUT_SLOT_END && !toInsert.isEmpty(); dest++) {
                                toInsert = inventory.insertItem(dest, toInsert, false);
                            }
                            if (toInsert.isEmpty()) {
                                tx.commit();
                                movedOne = true;
                                break;
                            } else {
                                // РѕС‚РєР°С‚РёРј (РЅРµ РєРѕРјРјРёС‚РёРј)
                                break;
                            }
                        }
                    }
                    if (!movedOne) break;
                    lastPullSources.add(neighborPosGlobal);
                    setChanged();
                }
                *///?}
            }
        }
    }

    private void pushOutputToNeighbors() {
        if (level == null) return;

        ItemStack out = inventory.getStackInSlot(OUTPUT_SLOT);
        if (out.isEmpty()) return;

        Direction facing = getBlockState().getValue(MachineAssemblerBlock.FACING);
        MultiblockStructureHelper helper = ((MachineAssemblerBlock) getBlockState().getBlock()).getStructureHelper();

        for (BlockPos localOffset : helper.getPartOffsets()) {
            if (out.isEmpty()) break;

            int x = localOffset.getX();
            int y = localOffset.getY();
            int z = localOffset.getZ();
            boolean isOutputConveyor = (y == 0) && (x == -1) && (z == 0 || z == 1);

            if (!isOutputConveyor) continue;

            BlockPos partPos = helper.getRotatedPos(worldPosition, localOffset, facing);

            int dxOut = Integer.signum(partPos.getX() - worldPosition.getX());
            int dzOut = Integer.signum(partPos.getZ() - worldPosition.getZ());
            Direction outDir = Direction.getNearest(dxOut, 0, dzOut);
            Direction facingDir = getBlockState().getValue(MachineAssemblerBlock.FACING);

            BlockPos neighborPos = partPos.relative(outDir).relative(facingDir.getOpposite());
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            if (neighbor == null || neighbor instanceof UniversalMachinePartBlockEntity ||
                    neighbor == this || lastPullSources.contains(neighborPos)) continue;

            Direction side1 = outDir.getOpposite();
            Direction side2 = facingDir;
            //? if forge {
            IItemHandler cap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side1)
                    .orElse(neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side2)
                            .orElse(null));

            if (cap == null) continue;

            ItemStack toInsert = out.copy();
            for (int slot = 0; slot < cap.getSlots() && !toInsert.isEmpty(); slot++) {
                ItemStack remaining = cap.insertItem(slot, toInsert.copy(), false);

                if (remaining.getCount() < toInsert.getCount()) {
                    inventory.getStackInSlot(OUTPUT_SLOT).shrink(toInsert.getCount() - remaining.getCount());
                    toInsert = remaining;
                }
            }

            out = inventory.getStackInSlot(OUTPUT_SLOT);
            //?}

            //? if fabric {
            /*Storage<ItemVariant> cap = ItemStorage.SIDED.find(level, neighborPos, side1);
            if (cap == null) cap = ItemStorage.SIDED.find(level, neighborPos, side2);
            if (cap == null) continue;

            ItemStack stack = inventory.getStackInSlot(OUTPUT_SLOT);
            if (stack.isEmpty()) continue;

            ItemVariant variant = ItemVariant.of(stack);
            long amount = stack.getCount();
            try (Transaction tx = Transaction.openOuter()) {
                long inserted = cap.insert(variant, amount, tx);
                if (inserted > 0) {
                    stack.shrink((int) inserted);
                    tx.commit();
                }
            }
            out = inventory.getStackInSlot(OUTPUT_SLOT);
            *///?}
        }
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries); // РЎРѕС…СЂР°РЅСЏРµС‚ РёРЅРІРµРЅС‚Р°СЂСЊ Рё Р­РќР•Р Р“РР®
        // РЎРѕС…СЂР°РЅСЏРµРј С‚РѕР»СЊРєРѕ С‚Рѕ, С‡РµРіРѕ РЅРµС‚ РІ СЂРѕРґРёС‚РµР»Рµ
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putBoolean("isCrafting", isCrafting);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries); // Р—Р°РіСЂСѓР¶Р°РµС‚ РёРЅРІРµРЅС‚Р°СЂСЊ Рё Р­РќР•Р Р“РР®
        // Р—Р°РіСЂСѓР¶Р°РµРј С‚РѕР»СЊРєРѕ С‚Рѕ, С‡РµРіРѕ РЅРµС‚ РІ СЂРѕРґРёС‚РµР»Рµ
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        isCrafting = tag.getBoolean("isCrafting");
    }


    //? if forge {
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyInputProxy.invalidate();
        lazyOutputProxy.invalidate();
    }
    //?}

    // ==================== CLIENT ====================


    //? if forge || neoforge {
    private ItemStack clientRecipeIconTemplate = ItemStack.EMPTY;

    private ItemStack clientRecipeIconCache = ItemStack.EMPTY;

    /** Cached recipe output icon for BER; refreshed when assembly template slot changes. */
    @OnlyIn(Dist.CLIENT)
    public ItemStack getClientRecipeIcon() {
        ItemStack template = getInventory().getStackInSlot(TEMPLATE_SLOT);
        if (ItemStack.matches(template, clientRecipeIconTemplate)) {
            return clientRecipeIconCache;
        }
        clientRecipeIconTemplate = template.copy();
        if (template.isEmpty() || !(template.getItem() instanceof ItemAssemblyTemplate)) {
            clientRecipeIconCache = ItemStack.EMPTY;
            return ItemStack.EMPTY;
        }
        clientRecipeIconCache = ItemAssemblyTemplate.getRecipeOutput(template);
        return clientRecipeIconCache;
    }

    @OnlyIn(Dist.CLIENT)
    public void setCrafting(boolean crafting) {
        this.isCrafting = crafting;
    }
    //?}

    public boolean isCrafting() {
        return isCrafting;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MachineAssemblerBlock block)) {
            // AABB(Vec3, Vec3) не принимает BlockPos — собираем через double-конструктор (версионно-инвариантно).
            net.minecraft.core.BlockPos min = worldPosition.offset(-2, -1, -2);
            net.minecraft.core.BlockPos max = worldPosition.offset(3, 3, 3);
            return new net.minecraft.world.phys.AABB(
                    min.getX(), min.getY(), min.getZ(),
                    max.getX(), max.getY(), max.getZ());
        }
        Direction facing = state.getValue(MachineAssemblerBlock.FACING);
        return block.getStructureHelper().getRenderBoundingBox(worldPosition, facing, 1.35);
    }

    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);

    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        //? if forge {
        if (this.level != null && this.level.isClientSide) {
            ClientSoundBootstrap.updateSound(this, false, null);
        }
        //?}

        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }
}
