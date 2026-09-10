package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineAmmoPressMenu;
import com.hbm_m.recipe.AmmoPressRecipe;
import com.hbm_m.recipe.ModRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ammo Press - Port von {@code TileEntityMachineAmmoPress} (1.7.10 Original). Anders als die
 * Press/E-Press/Conveyor-Press-Familie (Stempel-Item + Rohmaterial -> Blech) ist dies eine reine
 * 3x3-Raster-Rezept-Maschine ohne Energie/Treibstoff: sobald die 9 Eingangs-Slots exakt einem
 * {@link AmmoPressRecipe} entsprechen, wird sofort (und wiederholt, solange Material reicht)
 * gefertigt - siehe {@link AmmoPressRecipe} fuer die Rezept-Form.
 * <p>
 * <p>Wie im Original waehlt man in der Oberflaeche aus einer durchsuchbaren Liste <b>ein</b>
 * Zielrezept aus, und nur dieses wird gefertigt. Das ist keine Zierde: erst die Auswahl sagt der
 * Presse, welche der vielen Patronen sie aus aehnlichen Zutaten machen soll, und sie schraenkt
 * gleichzeitig ein, was ueberhaupt in die neun Eingabefelder passt. Ohne Auswahl steht sie
 * still.</p>
 *
 * <p>Nicht uebernommen: die animierte Press-Kolben-3D-Bewegung (Original-Renderer) -
 * {@link #animTicks} haelt nur einen kurzen Fortschritts-Flash-Zustand fuers GUI.</p>
 */
public class MachineAmmoPressBlockEntity extends BaseMachineBlockEntity {

    private static final int GRID_SIZE = AmmoPressRecipe.GRID_SIZE;
    public static final int SLOT_OUTPUT = GRID_SIZE;
    private static final int SLOT_COUNT = GRID_SIZE + 1;

    private static final int ANIM_DURATION = 20;

    private int animTicks = 0;

    /**
     * Original: {@code selectedRecipe} - dort ein Index in die globale Rezeptliste. Hier die
     * Rezept-Kennung, damit Client und Server auch dann dasselbe meinen, wenn die Datenpakete in
     * unterschiedlicher Reihenfolge geladen wurden.
     */
    @Nullable
    private net.minecraft.resources.ResourceLocation selectedRecipe = null;

    /**
     * Alle Rezepte in einer stabilen Reihenfolge - nach Kennung sortiert, damit die Liste in der
     * Oberflaeche bei jedem Oeffnen gleich aussieht.
     */
    public static java.util.List<java.util.Map.Entry<net.minecraft.resources.ResourceLocation, AmmoPressRecipe>>
            sortedRecipes(Level level) {
        var byId = com.hbm_m.platform.recipe.RecipeHooks.getAllRecipesById(level, AmmoPressRecipe.Type.INSTANCE);
        return byId.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey(
                        java.util.Comparator.comparing(net.minecraft.resources.ResourceLocation::toString)))
                .toList();
    }

    public static java.util.Map<net.minecraft.resources.ResourceLocation, AmmoPressRecipe> recipesById(Level level) {
        return com.hbm_m.platform.recipe.RecipeHooks.getAllRecipesById(level, AmmoPressRecipe.Type.INSTANCE);
    }

    public MachineAmmoPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMMO_PRESS_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAmmoPressBlockEntity be) {
        if (level.isClientSide()) {
            if (be.animTicks > 0) be.animTicks--;
            return;
        }
        be.serverTick(level);
    }

    private void serverTick(Level level) {
        // 1:1: ohne gewaehltes Zielrezept passiert nichts.
        AmmoPressRecipe recipe = getSelectedRecipe(level);
        if (recipe == null) return;
        if (!matchesInputs(recipe)) return;

        ItemStack output = recipe.getOutput();
        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (!outSlot.isEmpty()) {
            if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(outSlot, output)) return;
            if (outSlot.getCount() + output.getCount() > outSlot.getMaxStackSize()) return;
        }

        for (int i = 0; i < GRID_SIZE; i++) {
            inventory.getStackInSlot(i).shrink(1);
        }
        if (outSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, output);
        } else {
            outSlot.grow(output.getCount());
        }

        animTicks = ANIM_DURATION;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 1.4F);

        setChanged();
        sendUpdateToClient();
    }

    /** Das gewaehlte Rezept, oder {@code null} wenn keins gewaehlt ist oder es nicht mehr existiert. */
    @Nullable
    public AmmoPressRecipe getSelectedRecipe(Level level) {
        if (selectedRecipe == null) return null;
        return recipesById(level).get(selectedRecipe);
    }

    /** 1:1 aus {@code hasIngredients}: die neun Felder muessen dem Rezept exakt entsprechen. */
    private boolean matchesInputs(AmmoPressRecipe recipe) {
        NonNullList<ItemStack> grid = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < GRID_SIZE; i++) {
            grid.set(i, inventory.getStackInSlot(i));
        }
        return recipe.matchesGrid(grid);
    }

    @Nullable
    public net.minecraft.resources.ResourceLocation getSelectedRecipeId() {
        return selectedRecipe;
    }

    /**
     * Original: {@code receiveControl} - dieselbe Auswahl noch einmal anzuklicken hebt sie auf.
     */
    public void selectRecipe(@Nullable net.minecraft.resources.ResourceLocation id) {
        selectedRecipe = java.util.Objects.equals(selectedRecipe, id) ? null : id;
        setChanged();
        sendUpdateToClient();
    }

    public int getAnimTicks() {
        return animTicks;
    }

    public boolean isPressing() {
        return animTicks > 0;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("anim_ticks", animTicks);
        if (selectedRecipe != null) tag.putString("recipe", selectedRecipe.toString());
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        animTicks = tag.getInt("anim_ticks");
        selectedRecipe = tag.contains("recipe")
                ? net.minecraft.resources.ResourceLocation.tryParse(tag.getString("recipe"))
                : null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.ammo_press");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    /**
     * 1:1 aus dem Original: in die Eingabefelder passt nur, was das gewaehlte Rezept dort auch
     * braucht - so laesst sich die Presse automatisieren, ohne dass falsche Zutaten haengenbleiben.
     */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) return false;
        if (level == null) return true;

        AmmoPressRecipe recipe = getSelectedRecipe(level);
        if (recipe == null) return false;

        var inputs = recipe.getInputs();
        if (slot >= inputs.size()) return false;

        var ingredient = inputs.get(slot);
        return !ingredient.isEmpty() && ingredient.test(stack);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineAmmoPressMenu(containerId, playerInventory, this);
    }
}
