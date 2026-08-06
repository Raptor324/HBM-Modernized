package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineFurnaceIronBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineFurnaceIronMenu;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Iron Furnace: Direktport der Kernlogik aus {@code TileEntityFurnaceIron} (1.7.10 Original).
 * <p>
 * Vereinfachung: das Original ist ein 2x2x2 Multiblock (BlockDummyable) mit optionalem Upgrade-Slot.
 * Dieser Port folgt der in diesem Repo etablierten Konvention (siehe z.B. MachineHydrotreaterBlock)
 * fuer einfache Maschinen: ein einzelner Block/BlockEntity ohne Dummy-Teile. Der Upgrade-Slot entfaellt,
 * da dieser Port noch kein Item-Upgrade-System besitzt (kein ItemMachineUpgrade/UpgradeManager
 * gefunden) - stattdessen feste processingTime = 160 Ticks wie im Original ohne Upgrade.
 * <p>
 * 100% Vanilla-Schmelzrezepte (wie im Original), zwei parallele Brennstoff-Slots wie im Original
 * (slot 1 + slot 2), Brenndauer via {@link AbstractFurnaceBlockEntity#getFuel()}.
 */
public class MachineFurnaceIronBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL_1 = 1;
    public static final int SLOT_FUEL_2 = 2;
    public static final int SLOT_OUTPUT = 3;
    private static final int SLOT_COUNT = 4;

    private static final int PROCESSING_TIME = 160;

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_FUEL_1, SLOT_FUEL_2 -> isFuel(stack);
                case SLOT_OUTPUT -> false;
                default -> true;
            };
        }
    };

    private int litTime = 0;
    private int litDuration = 0;
    private int progress = 0;

    private static final int DATA_COUNT = 3;
    private static final int DATA_LIT_TIME = 0;
    private static final int DATA_LIT_DURATION = 1;
    private static final int DATA_PROGRESS = 2;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_LIT_TIME -> litTime;
                case DATA_LIT_DURATION -> litDuration;
                case DATA_PROGRESS -> progress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_LIT_TIME -> litTime = value;
                case DATA_LIT_DURATION -> litDuration = value;
                case DATA_PROGRESS -> progress = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private final SimpleContainer recipeInput = new SimpleContainer(1);

    public MachineFurnaceIronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_IRON_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() {
        return inventory;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFurnaceIronBlockEntity be) {
        if (level.isClientSide()) return;

        boolean wasLit = be.isLit();
        boolean dirty = false;

        if (be.litTime <= 0 && be.canSmelt(level)) {
            if (be.tryConsumeFuel()) {
                dirty = true;
            }
        }

        if (be.litTime > 0) {
            be.litTime--;
            if (be.canSmelt(level)) {
                be.progress++;
                if (be.progress >= PROCESSING_TIME) {
                    be.craftItem(level);
                    be.progress = 0;
                }
            } else if (be.progress != 0) {
                be.progress = 0;
            }
            dirty = true;
        } else if (be.progress != 0) {
            be.progress = 0;
            dirty = true;
        }

        if (wasLit != be.isLit()) {
            level.setBlock(pos, state.setValue(MachineFurnaceIronBlock.LIT, be.isLit()), 3);
        }

        if (dirty) {
            be.setChanged();
        }
    }

    private boolean isLit() {
        return litTime > 0;
    }

    private boolean tryConsumeFuel() {
        for (int slot : new int[] { SLOT_FUEL_1, SLOT_FUEL_2 }) {
            ItemStack fuelStack = inventory.getStackInSlot(slot);
            if (fuelStack.isEmpty()) continue;
            int burnTicks = AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuelStack.getItem(), 0);
            if (burnTicks <= 0) continue;

            litDuration = burnTicks;
            litTime = burnTicks;

            var remainderItem = fuelStack.getItem().getCraftingRemainingItem();
            fuelStack.shrink(1);
            if (fuelStack.isEmpty() && remainderItem != null) {
                inventory.setStackInSlot(slot, new ItemStack(remainderItem));
            }
            return true;
        }
        return false;
    }

    private boolean canSmelt(Level level) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        var recipe = getRecipe(level, input);
        if (recipe.isEmpty()) return false;

        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        if (result.isEmpty()) return false;

        return canAcceptResult(result);
    }

    private boolean canAcceptResult(ItemStack result) {
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void craftItem(Level level) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        var recipe = getRecipe(level, input);
        if (recipe.isEmpty()) return;

        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();

        input.shrink(1);

        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            output.grow(result.getCount());
            inventory.setStackInSlot(SLOT_OUTPUT, output);
        }
    }

    private java.util.Optional<SmeltingRecipe> getRecipe(Level level, ItemStack input) {
        recipeInput.setItem(0, input);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
    }

    public static boolean isFuel(ItemStack stack) {
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0) > 0;
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, container);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putInt("litTime", litTime);
        tag.putInt("litDuration", litDuration);
        tag.putInt("progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        litTime = tag.getInt("litTime");
        litDuration = tag.getInt("litDuration");
        progress = tag.getInt("progress");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    // ==================== GUI ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.hbm_m.furnace_iron");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineFurnaceIronMenu(id, inv, this, data);
    }
}
