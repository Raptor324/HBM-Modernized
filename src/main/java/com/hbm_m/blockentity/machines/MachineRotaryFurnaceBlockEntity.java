package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.machines.MachineRotaryFurnaceBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineRotaryFurnaceMenu;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.RotaryFurnaceRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Rotary Furnace: Direktport der Kernlogik aus {@code TileEntityMachineRotaryFurnace} (1.7.10 Original).
 * <p>
 * Vereinfachungen ggue. Original (gleiche Konvention wie andere einfache Maschinen diese Session,
 * siehe {@link MachineFurnaceIronBlockEntity}): einzelner Block statt 5x5 Multiblock, kein
 * Item-Upgrade-System (burnModule-Boni entfallen), kein Dampf-Erzeugungs-/Rueckgewinnungs-Kreislauf
 * und keine Pollution - stattdessen ein einzelner Input-FluidTank fuer die von manchen Rezepten
 * benoetigte Fluessigkeit. Das Original giesst den Output als fluessiges Metall auf den Boden
 * ({@code CrucibleUtil.pourSingleStack}); da dieser Port keine solche Mechanik besitzt, wird der
 * Output stattdessen als echter Ingot-ItemStack in einen Output-Slot gelegt (gleiche Vereinfachung
 * wie bei anderen Maschinen diese Session).
 */
public class MachineRotaryFurnaceBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements MenuProvider, IFluidStandardReceiverMK2 {

    public static final int SLOT_IN1 = 0, SLOT_IN2 = 1, SLOT_IN3 = 2;
    public static final int SLOT_FUEL = 3;
    public static final int SLOT_OUTPUT = 4;
    private static final int SLOT_COUNT = 5;

    private final FluidTank tank = new FluidTank(16_000);

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
    private float progress = 0f;

    private static final int DATA_LIT_TIME = 0;
    private static final int DATA_LIT_DURATION = 1;
    private static final int DATA_PROGRESS_SCALED = 2;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_LIT_TIME -> litTime;
                case DATA_LIT_DURATION -> litDuration;
                case DATA_PROGRESS_SCALED -> (int) (progress * 1000);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_LIT_TIME -> litTime = value;
                case DATA_LIT_DURATION -> litDuration = value;
                case DATA_PROGRESS_SCALED -> progress = value / 1000f;
                default -> { }
            }
        }

        @Override
        public int getCount() { return 3; }
    };

    public MachineRotaryFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROTARY_FURNACE_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() { return inventory; }
    public FluidTank getTank() { return tank; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRotaryFurnaceBlockEntity be) {
        if (level.isClientSide()) return;

        if (level.getGameTime() % 10 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        boolean wasLit = be.isLit();

        RotaryFurnaceRecipe recipe = findRotaryFurnaceRecipe(level, be);

        if (be.litTime <= 0 && recipe != null && be.canAcceptResult(recipe.getOutput())) {
            be.tryConsumeFuel();
        }

        if (be.litTime > 0) {
            be.litTime--;

            if (recipe != null && be.canAcceptResult(recipe.getOutput())) {
                be.progress += 1f / recipe.getDuration();
                if (be.progress >= 1f) {
                    be.progress -= 1f;
                    be.craftItem(recipe);
                }
            } else {
                be.progress = 0;
            }
        } else if (be.progress != 0) {
            be.progress = 0;
        }

        if (wasLit != be.isLit()) {
            level.setBlock(pos, state.setValue(MachineRotaryFurnaceBlock.LIT, be.isLit()), 3);
        }

        be.setChanged();
    }

    private boolean isLit() { return litTime > 0; }

    /**
     * Data-driven поиск RotaryFurnaceRecipe по 3 входным слотам + баку
     * (заменяет статический RotaryFurnaceRecipes.getRecipe).
     */
    @Nullable
    private static RotaryFurnaceRecipe findRotaryFurnaceRecipe(Level level, MachineRotaryFurnaceBlockEntity be) {
        ItemStack s0 = be.inventory.getStackInSlot(SLOT_IN1);
        ItemStack s1 = be.inventory.getStackInSlot(SLOT_IN2);
        ItemStack s2 = be.inventory.getStackInSlot(SLOT_IN3);
        for (RotaryFurnaceRecipe recipe : RecipeHooks.getAllRecipes(level, RotaryFurnaceRecipe.Type.INSTANCE)) {
            if (recipe.matchesInputs(s0, s1, s2)
                    && recipe.matchesFluid(be.tank.getTankType(), be.tank.getFill())) {
                return recipe;
            }
        }
        return null;
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

    private boolean canAcceptResult(ItemStack result) {
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) return true;
        if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void craftItem(RotaryFurnaceRecipe recipe) {
        int[] inputSlots = { SLOT_IN1, SLOT_IN2, SLOT_IN3 };
        // Поглощение зеркалит matchesInputs: каждый ингредиент снимается с первого подходящего слота.
        for (int i = 0; i < recipe.getInputs().length; i++) {
            for (int slot : inputSlots) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && recipe.getInputs()[i].test(stack) && stack.getCount() >= recipe.getInputCount(i)) {
                    inventory.extractItem(slot, recipe.getInputCount(i), false);
                    break;
                }
            }
        }

        if (recipe.getFluid() != null) {
            tank.drainMb(recipe.getFluidAmountMb());
        }

        ItemStack result = recipe.getOutput();
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            output.grow(result.getCount());
            inventory.setStackInSlot(SLOT_OUTPUT, output);
        }
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

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        tag.putInt("litTime", litTime);
        tag.putInt("litDuration", litDuration);
        tag.putFloat("progress", progress);
        tank.writeToNBT(tag, "tank");
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
        progress = tag.getFloat("progress");
        tank.readFromNBT(tag, "tank");
    }

    // ==================== GUI ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.hbm_m.rotary_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineRotaryFurnaceMenu(id, inv, this, data);
    }
}
