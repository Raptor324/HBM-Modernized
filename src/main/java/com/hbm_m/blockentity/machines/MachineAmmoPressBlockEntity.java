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
 * <p>Nach jedem Pressen laeuft 40 Ticks lang die Animation des Originals (Heben, Pressen, Zurueckfahren,
 * Absenken), gezeichnet vom {@code AmmoPressRenderer}. Es wird im selben Tick so oft gepresst, wie Material
 * und Ausgabeplatz reichen.</p>
 */
public class MachineAmmoPressBlockEntity extends BaseMachineBlockEntity {

    private static final int GRID_SIZE = AmmoPressRecipe.GRID_SIZE;
    public static final int SLOT_OUTPUT = GRID_SIZE;
    private static final int SLOT_COUNT = GRID_SIZE + 1;

    /** Original {@code playAnimation}. */
    private int animTicks = 0;

    public enum AnimationState { LIFTING, PRESSING, RETRACTING, LOWERING }

    public AnimationState animState = AnimationState.LIFTING;
    public float prevLift = 0F;
    public float lift = 0F;
    public float prevPress = 0F;
    public float press = 0F;

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
            be.clientTick();
            return;
        }
        be.serverTick(level);
    }

    private void clientTick() {
        this.prevLift = this.lift;
        this.prevPress = this.press;

        if (animTicks > 0 || lift > 0) switch (animState) {
            case LIFTING -> {
                this.lift += 1F / 40F;
                if (this.lift >= 1F) { this.lift = 1F; this.animState = AnimationState.PRESSING; }
            }
            case PRESSING -> {
                this.press += 1F / 20F;
                if (this.press >= 1F) { this.press = 1F; this.animState = AnimationState.RETRACTING; }
            }
            case RETRACTING -> {
                this.press -= 1F / 20F;
                if (this.press <= 0F) { this.press = 0F; this.animState = AnimationState.LOWERING; }
            }
            case LOWERING -> {
                this.lift -= 1F / 40F;
                if (this.lift <= 0F) { this.lift = 0F; this.animState = AnimationState.LIFTING; }
            }
        }
    }

    private void serverTick(Level level) {
        if (animTicks > 0) animTicks--;
        // Original performRecipe: rekursiv, solange es geht
        while (performRecipe(level)) { }
        sendUpdateToClient();
    }

    private boolean performRecipe(Level level) {
        AmmoPressRecipe recipe = getSelectedRecipe(level);
        if (recipe == null) return false;

        ItemStack output = recipe.getOutput();
        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (!outSlot.isEmpty()) {
            if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(outSlot, output)) return false;
            if (outSlot.getCount() + output.getCount() > outSlot.getMaxStackSize()) return false;
        }

        if (!matchesInputs(recipe)) return false;

        for (int i = 0; i < GRID_SIZE; i++) {
            if (!recipe.getInputs().get(i).isEmpty()) {
                ItemStack s = inventory.getStackInSlot(i).copy();
                s.shrink(recipe.getCount(i));
                inventory.setStackInSlot(i, s);
            }
        }
        if (outSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, output);
        } else {
            ItemStack grown = outSlot.copy();
            grown.grow(output.getCount());
            inventory.setStackInSlot(SLOT_OUTPUT, grown);
        }

        animTicks = 40;
        setChanged();
        return true;
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

    /** Original: 3x2x3 um den Kern. */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
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

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0-9}; Zutaten nach Rezept in 0-8, nur das Ergebnis heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot < 9 && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 9; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?}
}
