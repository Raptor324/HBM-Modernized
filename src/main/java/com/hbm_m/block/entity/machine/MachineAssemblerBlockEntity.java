package com.hbm_m.block.entity.machine;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.WireBlockEntity;
import com.hbm_m.block.machine.MachineAssemblerBlock;
import com.hbm_m.client.ClientSoundManager;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.energy.LongToForgeWrapper;
import com.hbm_m.energy.LongDataPacker;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.item.ItemAssemblyTemplate;
import com.hbm_m.item.ItemCreativeBattery;
import com.hbm_m.menu.MachineAssemblerMenu;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.recipe.AssemblerRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Сборочная машина (Assembler) - мультиблочная структура для автоматизированного крафта.
 * Адаптировано для long-энергосистемы с наследованием от BaseMachineBlockEntity.
 */
public class MachineAssemblerBlockEntity extends BaseMachineBlockEntity {
    
    // Слоты
    private static final int SLOT_COUNT = 18;
    private static final int ENERGY_SLOT = 0;
    private static final int TEMPLATE_SLOT = 4;
    private static final int OUTPUT_SLOT = 5;
    private static final int INPUT_SLOT_START = 6;
    private static final int INPUT_SLOT_END = 17;
    
    // Long-энергия
    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100_000L, 100_000L);
    
    // Состояние крафта
    private boolean isCrafting = false;
    private int progress = 0;
    private int maxProgress = 100;
    
    // Proxy handlers для multiblock parts
    private LazyOptional<IItemHandler> lazyInputProxy = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyOutputProxy = LazyOptional.empty();
    
    // Отслеживание источников предметов
    private final Set<BlockPos> lastPullSources = new HashSet<>();
    
    // ContainerData для GUI с упаковкой long
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            long energy = energyStorage.getEnergyStored();
            long maxEnergy = energyStorage.getMaxEnergyStored();
            long delta = getEnergyDelta();
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> LongDataPacker.packHigh(energy);
                case 3 -> LongDataPacker.packLow(energy);
                case 4 -> LongDataPacker.packHigh(maxEnergy);
                case 5 -> LongDataPacker.packLow(maxEnergy);
                case 6 -> LongDataPacker.packHigh(delta);
                case 7 -> LongDataPacker.packLow(delta);
                case 8 -> isCrafting ? 1 : 0;
                default -> 0;
            };
        }
        
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 8 -> isCrafting = value != 0;
            }
        }
        
        @Override
        public int getCount() {
            return 9;
        }
    };
    
    public MachineAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), pos, state, SLOT_COUNT);
    }
    
    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_assembler");
    }
    
    @Override
    protected void setupEnergyCapability() {
        longEnergyHandler = LazyOptional.of(() -> energyStorage);
        forgeEnergyHandler = longEnergyHandler.lazyMap(LongToForgeWrapper::new);
    }
    
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == ENERGY_SLOT) {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent() || 
                   stack.getItem() instanceof ItemCreativeBattery;
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
    
    // ==================== TICK LOGIC ====================
    
    public static void tick(Level level, BlockPos pos, BlockState state, MachineAssemblerBlockEntity entity) {
        if (level.isClientSide) {
            entity.clientTick();
        } else {
            entity.serverTick();
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private void clientTick() {
        ClientSoundManager.updateSound(this, this.isCrafting(), 
            () -> new com.hbm_m.sound.AssemblerSoundInstance(this.getBlockPos()));
    }
    
    private void serverTick() {
        long gameTime = level.getGameTime();
        
        if (gameTime % 5 == 0) {
            requestEnergy();
            chargeFromEnergySlot();
        }
        
        if (gameTime % 10 == 0) {
            updateEnergyDelta(energyStorage.getEnergyStored());
        }
        
        Optional<AssemblerRecipe> recipeOpt = getRecipeFromTemplate();
        
        if (recipeOpt.isPresent()) {
            pullIngredientsForOneCraft(recipeOpt.get());
        }
        
        boolean hasRecipe = recipeOpt.isPresent();
        boolean hasResources = hasRecipe && hasResources(recipeOpt.get());
        boolean canInsert = hasRecipe && canInsertResult(recipeOpt.get().getResultItem(null));
        
        if (hasRecipe && hasResources && canInsert) {
            AssemblerRecipe recipe = recipeOpt.get();
            long energyPerTick = recipe.getPowerConsumption();
            
            if (energyStorage.getEnergyStored() >= energyPerTick) {
                if (!isCrafting) {
                    isCrafting = true;
                    maxProgress = recipe.getDuration();
                    setChanged();
                    sendUpdateToClient();
                }
                
                energyStorage.extractEnergy(energyPerTick, false);
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
        
        if (energySourceStack.getItem() instanceof ItemCreativeBattery) {
            energyStorage.receiveEnergy(Long.MAX_VALUE, false);
        } else {
            energySourceStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
                long energyNeeded = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
                long maxCanReceive = energyStorage.getMaxReceive();
                long energyToTransfer = Math.min(energyNeeded, maxCanReceive);
                
                if (energyToTransfer > 0) {
                    int energyToTransferInt = (int) Math.min(energyToTransfer, Integer.MAX_VALUE);
                    int extracted = itemEnergy.extractEnergy(energyToTransferInt, false);
                    energyStorage.receiveEnergy(extracted, false);
                }
            });
        }
    }
    
    private void requestEnergy() {
        if (energyStorage.getEnergyStored() >= energyStorage.getMaxEnergyStored()) return;
        
        long energyNeeded = energyStorage.getMaxReceive();
        if (energyNeeded <= 0) return;
        
        Direction facing = getBlockState().getValue(MachineAssemblerBlock.FACING);
        MultiblockStructureHelper helper = ((MachineAssemblerBlock) getBlockState().getBlock()).getStructureHelper();
        
        for (BlockPos localOffset : helper.getPartOffsets()) {
            int x = localOffset.getX();
            int y = localOffset.getY();
            int z = localOffset.getZ();
            boolean isEnergyConnector = (y == 0) && (x == 0 || x == 1) && (z == -1 || z == 2);
            
            if (!isEnergyConnector) continue;
            
            BlockPos partPos = helper.getRotatedPos(worldPosition, localOffset, facing);
            BlockEntity partBE = level.getBlockEntity(partPos);
            
            if (!(partBE instanceof UniversalMachinePartBlockEntity)) continue;
            
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(partPos.relative(dir));
                if (neighbor == null || neighbor instanceof UniversalMachinePartBlockEntity || neighbor == this) {
                    continue;
                }
                
                long extracted = 0L;
                
                LazyOptional<ILongEnergyStorage> longCap = neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite());
                if (longCap.isPresent()) {
                    ILongEnergyStorage longStorage = longCap.resolve().orElse(null);
                    if (longStorage != null && longStorage.canExtract()) {
                        extracted = longStorage.extractEnergy(energyNeeded, false);
                    }
                } else if (neighbor instanceof WireBlockEntity wire) {
                    int energyNeededInt = (int) Math.min(energyNeeded, Integer.MAX_VALUE);
                    extracted = wire.requestEnergy(energyNeededInt, false);
                } else {
                    IEnergyStorage source = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
                    if (source != null && source.canExtract()) {
                        int energyNeededInt = (int) Math.min(energyNeeded, Integer.MAX_VALUE);
                        extracted = source.extractEnergy(energyNeededInt, false);
                    }
                }
                
                if (extracted > 0) {
                    long accepted = energyStorage.receiveEnergy(extracted, false);
                    energyNeeded -= accepted;
                    if (energyNeeded <= 0) break;
                }
            }
            
            if (energyNeeded <= 0) break;
        }
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
        
        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getAllRecipesFor(AssemblerRecipe.Type.INSTANCE)
                .stream()
                .filter(r -> ItemStack.isSameItemSameTags(r.getResultItem(null), outputStack))
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
        return recipe.matches(container, level);
    }
    
    private boolean canInsertResult(ItemStack result) {
        ItemStack outputSlotStack = inventory.getStackInSlot(OUTPUT_SLOT);
        return outputSlotStack.isEmpty() ||
                (ItemStack.isSameItemSameTags(outputSlotStack, result) &&
                        outputSlotStack.getCount() + result.getCount() <= outputSlotStack.getMaxStackSize());
    }
    
    private void craftItem(AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        ItemStack result = recipe.getResultItem(null).copy();
        
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
            
            IItemHandler cap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dirToNeighbor).orElse(null);
            if (cap == null) continue;
            
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
        }
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("EnergyStored", energyStorage.getEnergyStored());
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putBoolean("isCrafting", isCrafting);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergy(tag.getLong("EnergyStored"));
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        
        boolean wasCrafting = isCrafting;
        isCrafting = tag.getBoolean("isCrafting");
        
        if (level != null && level.isClientSide && wasCrafting && !isCrafting) {
            ClientSoundManager.updateSound(this, false, null);
        }
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("isCrafting", isCrafting);
        return tag;
    }
    
    // ==================== CAPABILITIES ====================
    
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        if (cap == ModCapabilities.LONG_ENERGY) {
            return longEnergyHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return forgeEnergyHandler.cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyInputProxy.invalidate();
        lazyOutputProxy.invalidate();
    }
    
    // ==================== CLIENT ====================
    
    @OnlyIn(Dist.CLIENT)
    public void setCrafting(boolean crafting) {
        this.isCrafting = crafting;
    }
    
    public boolean isCrafting() {
        return isCrafting;
    }
    
    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide) {
            ClientSoundManager.updateSound(this, false, null);
        }
        super.setRemoved();
    }
}
