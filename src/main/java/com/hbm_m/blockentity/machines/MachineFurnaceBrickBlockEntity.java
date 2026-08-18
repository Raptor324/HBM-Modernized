package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineFurnaceBrickBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineFurnaceBrickMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Brick Furnace: Direktport der Kernlogik aus {@code TileEntityFurnaceBrick} (1.7.10 Original).
 * <p>
 * Vereinfachung: Das Aschen-Byprodukt-System des Originals (Slot 3, {@code powder_ash} mit
 * Holz-/Kohle-/Sonstiges-Schadenswerten via {@code EnumAshType}) wird NICHT portiert - es
 * basiert auf einem 1.7.10-Item/Klassifikationssystem, das in diesem Port keine Entsprechung
 * hat (kein {@code powder_ash}, kein {@code EnumAshType}). Slot 3 entfaellt daher ersatzlos.
 * <p>
 * 100% Vanilla-Schmelzrezepte, Vanilla-Brennstoff-Erkennung wie bei {@code MachineFurnaceIron}.
 * Einzigartig an dieser Maschine: eine vom Eingangs-Item abhaengige Geschwindigkeitsmultiplikator
 * ({@link #getBurnSpeed(ItemStack)}), 1:1 aus dem Original uebernommen.
 */
public class MachineFurnaceBrickBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    private static final int SLOT_COUNT = 3;

    private static final int PROCESSING_THRESHOLD = 200;

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
                case SLOT_FUEL -> isFuel(stack);
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

    public MachineFurnaceBrickBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_BRICK_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() {
        return inventory;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFurnaceBrickBlockEntity be) {
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
                be.progress += be.getBurnSpeed(be.inventory.getStackInSlot(SLOT_INPUT));
                if (be.progress >= PROCESSING_THRESHOLD) {
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
            level.setBlock(pos, state.setValue(MachineFurnaceBrickBlock.LIT, be.isLit()), 3);
        }

        if (dirty) {
            be.setChanged();
        }
    }

    private boolean isLit() {
        return litTime > 0;
    }

    /** Direktport von {@code getBurnSpeed()}/{@code burnSpeed} (1.7.10 Original). */
    private int getBurnSpeed(ItemStack input) {
        if (input.isEmpty()) return 1;
        var item = input.getItem();
        if (item == Items.CLAY_BALL) return 4;
        if (item == ModItems.FIRECLAY_BALL.get()) return 4;
        if (item == Blocks.NETHERRACK.asItem()) return 4;
        if (item == Blocks.COBBLESTONE.asItem()) return 2;
        if (item == Blocks.SAND.asItem()) return 2;
        if (input.is(ItemTags.LOGS)) return 2;
        return 1;
    }

    private boolean tryConsumeFuel() {
        ItemStack fuelStack = inventory.getStackInSlot(SLOT_FUEL);
        if (fuelStack.isEmpty()) return false;
        int burnTicks = AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuelStack.getItem(), 0);
        if (burnTicks <= 0) return false;

        litDuration = burnTicks;
        litTime = burnTicks;

        var remainderItem = fuelStack.getItem().getCraftingRemainingItem();
        fuelStack.shrink(1);
        if (fuelStack.isEmpty() && remainderItem != null) {
            inventory.setStackInSlot(SLOT_FUEL, new ItemStack(remainderItem));
        }
        return true;
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
        if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, result)) return false;
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
        // 1.21.1: getRecipeFor требует RecipeInput (SingleRecipeInput) и возвращает RecipeHolder.
        return com.hbm_m.platform.recipe.RecipeHooks.getRecipeFor(level, RecipeType.SMELTING, input);
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
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        tag.putInt("litTime", litTime);
        tag.putInt("litDuration", litDuration);
        tag.putInt("progress", progress);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        //? if < 1.21.1 {
        inventory.deserializeNBT(tag.getCompound("inventory"));
        //?} else {
        /*inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        *///?}
        litTime = tag.getInt("litTime");
        litDuration = tag.getInt("litDuration");
        progress = tag.getInt("progress");
    }

    // ==================== GUI ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.hbm_m.furnace_brick");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineFurnaceBrickMenu(id, inv, this, data);
    }
}
