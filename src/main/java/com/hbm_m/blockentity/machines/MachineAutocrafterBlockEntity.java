package com.hbm_m.blockentity.machines;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineAutocrafterMenu;
import com.hbm_m.util.SimpleCraftingContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Autocrafter - Port von {@code TileEntityMachineAutocrafter} (1.7.10 Original). Craftet
 * fortlaufend jedes registrierte VANILLA-3x3-Rezept (shaped/shapeless), sobald die 9 Gitter-Slots
 * passen und genug Energie vorhanden ist - matcht ueber das echte {@link RecipeType#CRAFTING}-
 * Rezeptsystem dieses Ports (kein eigenes Rezeptformat), analog zum Original, das {@code
 * CraftingManager.getRecipeList()} durchsucht.
 * <p>
 * <p><b>Zwei Gitter, nicht eines.</b> Oben liegt die <b>Vorlage</b> (Plaetze 0-8): dort legt man
 * ein Muster hinein, das nicht verbraucht wird. Unten das <b>Arbeitsgitter</b> (10-18), in das
 * Rohre und Trichter die tatsaechlichen Zutaten schieben. Das ist der Unterschied zu einem
 * Werktisch: die Maschine weiss, was sie bauen soll, auch wenn gerade nichts da ist.</p>
 *
 * <p>Jeder Vorlagenplatz hat einen eigenen <b>Filtermodus</b> - genau, mit Platzhalter oder ueber
 * eine Materialgruppe ({@link com.hbm_m.inventory.filter.ModulePatternMatcher}). Ein Rechtsklick
 * auf den Platz schaltet ihn weiter. Damit laesst sich zum Beispiel "irgendein Brett" statt
 * "genau Eichenbrett" fordern.</p>
 *
 * <p>Passen mehrere Rezepte auf dieselbe Vorlage, blaettert ein Rechtsklick auf das Vorschaufeld
 * (Platz 9) durch sie hindurch.</p>
 */
public class MachineAutocrafterBlockEntity extends BaseMachineBlockEntity {

    /** Original: Plaetze 0-8 sind die Vorlage - sie werden nie verbraucht. */
    public static final int TEMPLATE_START = 0;
    public static final int GRID_SIZE = 9;
    /** Original: Platz 9 zeigt, was aus der Vorlage entstehen wuerde. */
    public static final int SLOT_TEMPLATE_RESULT = 9;
    /** Original: Plaetze 10-18 sind das Arbeitsgitter. */
    public static final int RECIPE_START = 10;
    /** Original: Platz 19 ist die Ausgabe. */
    public static final int SLOT_OUTPUT = 19;
    /** Original: Platz 20 ist die Batterie. */
    public static final int SLOT_BATTERY = 20;
    private static final int SLOT_COUNT = 21;

    private static final long MAX_POWER = 10_000L;
    private static final long CONSUMPTION = 100L;

    /** Original: {@code matcher = new ModulePatternMatcher(9)}. */
    private final com.hbm_m.inventory.filter.ModulePatternMatcher matcher =
            new com.hbm_m.inventory.filter.ModulePatternMatcher(GRID_SIZE);

    /** Alle Rezepte, die auf die aktuelle Vorlage passen - im Original {@code recipes}. */
    private java.util.List<CraftingRecipe> recipes = new java.util.ArrayList<>();
    /** Welches davon gebaut wird - im Original {@code recipeIndex}. */
    private int recipeIndex = 0;

    public MachineAutocrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOCRAFTER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public com.hbm_m.inventory.filter.ModulePatternMatcher getMatcher() { return matcher; }
    public int getRecipeIndex() { return recipeIndex; }
    public int getRecipeCount() { return level != null && level.isClientSide ? recipeCountClient : recipes.size(); }

    /** 1:1-Port von {@code nextMode}: ein Rechtsklick schaltet den Filter dieses Platzes weiter. */
    public void nextMode(int index) {
        if (index < 0 || index >= GRID_SIZE) return;
        matcher.nextMode(index, inventory.getStackInSlot(index));
        setChanged();
        sendUpdateToClient();
    }

    /** 1:1-Port von {@code nextTemplate}: das naechste passende Rezept. */
    public void nextTemplate() {
        if (level == null || level.isClientSide()) return;

        recipeIndex++;
        if (recipeIndex >= recipes.size()) recipeIndex = 0;

        updateTemplateResult();
        setChanged();
        sendUpdateToClient();
    }

    /**
     * 1:1-Port von {@code updateTemplateGrid}: nach jeder Aenderung an der Vorlage wird neu
     * gesucht, welche Rezepte darauf passen.
     */
    public void updateTemplateGrid() {
        if (level == null || level.isClientSide()) return;

        recipes = com.hbm_m.platform.recipe.RecipeHooks.getAllCraftingRecipesFor(level, buildGrid(TEMPLATE_START));
        recipeIndex = 0;
        updateTemplateResult();
    }

    private void updateTemplateResult() {
        if (level == null) return;

        if (recipes.isEmpty()) {
            inventory.setStackInSlot(SLOT_TEMPLATE_RESULT, ItemStack.EMPTY);
            return;
        }

        CraftingRecipe recipe = recipes.get(Math.min(recipeIndex, recipes.size() - 1));
        inventory.setStackInSlot(SLOT_TEMPLATE_RESULT,
                com.hbm_m.platform.recipe.RecipeHooks.assembleCrafting(recipe, buildGrid(TEMPLATE_START), level));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAutocrafterBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level);
    }

    /** Original {@code readFromNBT}: nach dem Laden die passenden Rezepte neu suchen, Index behalten. */
    private boolean recipesLoaded = false;
    private int recipeCountClient = 0;

    private void serverTick(Level level) {
        if (!recipesLoaded) {
            recipesLoaded = true;
            int keep = recipeIndex;
            recipes = com.hbm_m.platform.recipe.RecipeHooks.getAllCraftingRecipesFor(level, buildGrid(TEMPLATE_START));
            recipeIndex = recipes.isEmpty() ? 0 : Math.min(keep, recipes.size() - 1);
            updateTemplateResult();
        }

        chargeFromBatterySlot(SLOT_BATTERY);
        if (level instanceof net.minecraft.server.level.ServerLevel world) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                BlockPos p = worldPosition.relative(dir);
                this.trySubscribe(world, p.getX(), p.getY(), p.getZ(), dir);
            }
        }
        sendUpdateToClient();
        if (getEnergyStored() < CONSUMPTION) return;
        if (recipes.isEmpty()) return;

        // 1:1: gebaut wird ausschliesslich das gewaehlte Rezept - nicht irgendeines, das passt.
        CraftingRecipe recipe = recipes.get(Math.min(recipeIndex, recipes.size() - 1));
        SimpleCraftingContainer grid = buildGrid(RECIPE_START);

        if (!com.hbm_m.platform.recipe.RecipeHooks.craftingMatches(recipe, grid, level)) return;

        ItemStack result = com.hbm_m.platform.recipe.RecipeHooks.assembleCrafting(recipe, grid, level);
        if (result.isEmpty()) return;

        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (!outSlot.isEmpty()) {
            if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(outSlot, result)) return;
            if (outSlot.getCount() + result.getCount() > outSlot.getMaxStackSize()) return;
        }

        //? if < 1.21.1 {
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(grid);
        //?} else {
        /*NonNullList<ItemStack> remaining = recipe.getRemainingItems(grid.toCraftingInput());
        *///?}
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack ingredient = inventory.getStackInSlot(RECIPE_START + i);
            if (ingredient.isEmpty()) continue;

            ItemStack shrunk = ingredient.copy();
            shrunk.shrink(1);
            inventory.setStackInSlot(RECIPE_START + i, shrunk);

            // Original: der Behaelter kommt nur in einen leer gewordenen Platz, kaputte Werkzeuge verschwinden
            ItemStack leftover = remaining.get(i);
            if (shrunk.isEmpty() && !leftover.isEmpty()) {
                if (leftover.isDamageableItem() && leftover.getDamageValue() > leftover.getMaxDamage()) continue;
                inventory.setStackInSlot(RECIPE_START + i, leftover);
            }
        }

        if (outSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            outSlot.grow(result.getCount());
        }

        setEnergyStored(getEnergyStored() - CONSUMPTION);
        setChanged();
        sendUpdateToClient();
    }

    /** Das 3x3-Gitter ab einem Startplatz - einmal fuer die Vorlage, einmal fuer die Arbeit. */
    private SimpleCraftingContainer buildGrid(int start) {
        NonNullList<ItemStack> items = NonNullList.withSize(GRID_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < GRID_SIZE; i++) {
            items.set(i, inventory.getStackInSlot(start + i));
        }
        return new SimpleCraftingContainer(items, 3, 3);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.autocrafter");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    /**
     * 1:1-Port von {@code isItemValidForSlot}: eingelegt wird ausschliesslich ins Arbeitsgitter,
     * und nur, was der Filter des daruemberliegenden Vorlagenplatzes zulaesst.
     */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);

        // Original: nur die neun Arbeitsplaetze nehmen etwas an.
        if (slot < RECIPE_START || slot >= RECIPE_START + GRID_SIZE) return false;

        int filterIndex = slot - RECIPE_START;
        ItemStack filter = inventory.getStackInSlot(TEMPLATE_START + filterIndex);

        // Ohne Vorlage an dieser Stelle geht nichts hinein.
        if (filter.isEmpty()) return false;

        // Original: gestapelte Behaelter-Items nie
        if (stack.getCount() > 1 && stack.hasCraftingRemainingItem()) return false;

        // Original: hoechstens vier Stueck je Platz - der Autocrafter ist kein Lager.
        ItemStack present = inventory.getStackInSlot(slot);
        if (!present.isEmpty() && present.getCount() + stack.getCount() > 4) return false;
        if (stack.getCount() > 4) return false;

        // Original: alle Plaetze sammeln, in die das Item passen wuerde
        java.util.List<Integer> validSlots = new java.util.ArrayList<>();
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack f = inventory.getStackInSlot(TEMPLATE_START + i);
            if (f.isEmpty() || matcher.getMode(i) == null || matcher.getMode(i).isEmpty()) continue;

            if (matcher.isValidForFilter(f, i, stack)) {
                validSlots.add(i + RECIPE_START);
                // leerer, passender Platz: sofort ja
                if (i + RECIPE_START == slot && present.isEmpty()) return true;
            }
        }

        if (!validSlots.contains(slot)) return false;

        int size = present.getCount();

        // Original: ein anderer Platz, der dasselbe Item dringender braucht, hat Vorrang
        for (Integer i : validSlots) {
            ItemStack valid = inventory.getStackInSlot(i);
            if (valid.isEmpty()) return false;
            if (valid.getItem() != stack.getItem()) continue;
            if (valid.getCount() < size) return false;
        }

        // Original: Behaelter-Items nicht stapeln
        return !stack.hasCraftingRemainingItem();
    }

    /** Original {@code breakBlock}: nur die echten Plaetze ab 10 fallen heraus, die Vorlage ist ein Geisterbild. */
    @Override
    public void dropInventoryContents() {
        if (level == null) return;
        if (com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) return;
        for (int i = RECIPE_START; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /** Die Vorlage aendert sich nur ueber die Oberflaeche - danach muss neu gesucht werden. */
    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    protected void writeNbtData(net.minecraft.nbt.CompoundTag tag,
                                net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        matcher.writeToNBT(tag);
        tag.putInt("recipeIndex", recipeIndex);
        tag.putInt("recipeCount", recipes.size());
    }

    @Override
    protected void readNbtData(net.minecraft.nbt.CompoundTag tag,
                               net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        matcher.readFromNBT(tag);
        recipeIndex = tag.getInt("recipeIndex");
        recipeCountClient = tag.getInt("recipeCount");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineAutocrafterMenu(containerId, playerInventory, this);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {10-19}; Zutaten nach Vorlage hinein, das Ergebnis und nicht mehr passende Zutaten heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(10, 19); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { if (slot == 19) return true;
                    if (slot > 9 && slot < 19) {
                        net.minecraft.world.item.ItemStack filter = inventory.getStackInSlot(slot - 10);
                        if (filter.isEmpty() || matcher.getMode(slot - 10) == null || matcher.getMode(slot - 10).isEmpty()) return true;
                        return !matcher.isValidForFilter(filter, slot - 10, stack);
                    }
                    return false; }
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
