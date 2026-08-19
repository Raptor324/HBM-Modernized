package com.hbm_m.blockentity.machines;

import java.util.Optional;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.recipe.BlastFurnaceRecipe;
import com.hbm_m.util.RtgPelletHeat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityDiFurnaceRTG} (1.7.10 Original) - the RTG-heated variant of the
 * Blast/DiFurnace: same 2-ingredient recipe lookup as {@link BlastFurnaceBlockEntity} (reused
 * directly via {@link BlastFurnaceRecipe}), but heated by RTG pellets (6 slots) instead of solid
 * fuel items, using the same heat table as {@link MachineRtgBlockEntity} via {@link RtgPelletHeat}.
 */
public class MachineDifurnaceRtgBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_INPUT_TOP = 0;
    public static final int SLOT_INPUT_BOTTOM = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int PELLET_SLOT_START = 3;
    public static final int PELLET_SLOT_COUNT = 6;
    public static final int INVENTORY_SIZE = PELLET_SLOT_START + PELLET_SLOT_COUNT;

    private static final int TIME_REQUIRED = 1200;
    private static final int HEAT_MAX = 200;

    private int progress = 0;

    public MachineDifurnaceRtgBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DIFURNACE_RTG_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDifurnaceRtgBlockEntity be) {
        if (level.isClientSide) return;

        int heat = 0;
        for (int i = 0; i < PELLET_SLOT_COUNT; i++) {
            ItemStack stack = be.inventory.getStackInSlot(PELLET_SLOT_START + i);
            if (stack.isEmpty()) continue;
            heat += RtgPelletHeat.getHeat(stack.getItem());
        }
        heat = Math.min(heat, HEAT_MAX);

        if (heat > 0 && be.canProcess(level)) {
            be.progress += heat;
            if (be.progress >= TIME_REQUIRED) {
                be.progress = 0;
                be.craftItem(level);
            }
        } else {
            be.progress = 0;
        }

        be.setChanged();
    }

    private boolean canProcess(Level level) {
        return getRecipe(level).isPresent();
    }

    private Optional<BlastFurnaceRecipe> getRecipe(Level level) {
        ItemStack top = inventory.getStackInSlot(SLOT_INPUT_TOP);
        ItemStack bottom = inventory.getStackInSlot(SLOT_INPUT_BOTTOM);
        if (top.isEmpty() || bottom.isEmpty()) return Optional.empty();

        SimpleContainer container = new SimpleContainer(4);
        container.setItem(1, top);
        container.setItem(2, bottom);
        // 1.21.1: getRecipeFor требует RecipeInput — используем getAllRecipes + matchesRecipe.
        return com.hbm_m.platform.recipe.RecipeHooks
                .getAllRecipes(level, BlastFurnaceRecipe.Type.INSTANCE).stream()
                .filter(r -> r.matchesRecipe(new com.hbm_m.platform.recipe.RecipeInputWrapper(container), level))
                .findFirst();
    }

    private void craftItem(Level level) {
        Optional<BlastFurnaceRecipe> recipe = getRecipe(level);
        if (recipe.isEmpty()) return;

        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else if (output.is(result.getItem()) && output.getCount() + result.getCount() <= output.getMaxStackSize()) {
            output.grow(result.getCount());
        } else {
            return;
        }

        inventory.getStackInSlot(SLOT_INPUT_TOP).shrink(1);
        inventory.getStackInSlot(SLOT_INPUT_BOTTOM).shrink(1);
    }

    public int getProgress() { return progress; }
    public int getMaxProgress() { return TIME_REQUIRED; }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot >= PELLET_SLOT_START) return RtgPelletHeat.getHeat(stack.getItem()) > 0;
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_difurnace_rtg");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineDifurnaceRtgMenu.create(id, inventory, this);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress", progress);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getInt("progress");
    }
}
