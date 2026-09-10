package com.hbm_m.recipe;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachineMassStorageBlock;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 1:1-Port von {@code ContainerUpgradeCraftingHandler} fuer den Massenspeicher: die Aufwertung
 * <b>nimmt den Inhalt der alten Kiste mit</b>.
 *
 * <p>Ohne das waere sie eine Falle - wer eine volle Deshkiste aufwertet, wuerde hunderttausend
 * Gegenstaende verlieren. Das Original loest es mit einem eigenen Rezepttyp, der die gespeicherten
 * Daten von der Vorlage auf das Ergebnis kopiert; genau das passiert hier.</p>
 *
 * <p>Zwei Stufen, beide im Muster {@code " C " / "PMP" / " P "}:</p>
 * <ul>
 *   <li>Eisen wird mit Deshbarren und einem Bismoid-Chip zu Desh,</li>
 *   <li>Desh wird mit widerstandsfaehiger Legierung und einer fortgeschrittenen Schaltung zu Stahl.</li>
 * </ul>
 */
public class MassStorageUpgradeRecipe extends CustomRecipe {

    //? if < 1.21.1 {
    public MassStorageUpgradeRecipe(net.minecraft.resources.ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }
    //?} else {
    /*public MassStorageUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }
    *///?}

    /** Eine Aufwertungsstufe: von welcher Kiste, mit welchem Barren und welcher Schaltung, wohin. */
    private record Step(Block from, java.util.function.Supplier<net.minecraft.world.item.Item> plate,
                        java.util.function.Supplier<net.minecraft.world.item.Item> circuit, Block to) {}

    private static java.util.List<Step> steps() {
        return java.util.List.of(
                new Step(ModBlocks.MASS_STORAGE_IRON.get(),
                        () -> ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT),
                        ModItems.BISMOID_CHIP::get,
                        ModBlocks.MASS_STORAGE_DESH.get()),
                new Step(ModBlocks.MASS_STORAGE_DESH.get(),
                        () -> ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.INGOT),
                        ModItems.ADVANCED_CIRCUIT::get,
                        ModBlocks.MASS_STORAGE.get()));
    }

    /** Findet die Stufe, deren Muster im Gitter liegt - oder {@code null}. */
    private static Step match(CraftingContainer container) {
        if (container.getWidth() < 3 || container.getHeight() < 3) return null;

        for (Step step : steps()) {
            if (fits(container, step)) return step;
        }
        return null;
    }

    /** Das Muster {@code " C " / "PMP" / " P "} - alles andere muss leer sein. */
    private static boolean fits(CraftingContainer container, Step step) {
        ItemStack plate = new ItemStack(step.plate().get());

        for (int y = 0; y < container.getHeight(); y++) {
            for (int x = 0; x < container.getWidth(); x++) {
                ItemStack stack = container.getItem(x + y * container.getWidth());

                boolean ok;
                if (y == 0 && x == 1)      ok = stack.is(step.circuit().get());
                else if (y == 1 && x == 0) ok = stack.is(plate.getItem());
                else if (y == 1 && x == 1) ok = Block.byItem(stack.getItem()) == step.from();
                else if (y == 1 && x == 2) ok = stack.is(plate.getItem());
                else if (y == 2 && x == 1) ok = stack.is(plate.getItem());
                else                       ok = stack.isEmpty();

                if (!ok) return false;
            }
        }
        return true;
    }

    //? if < 1.21.1 {
    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return match(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        Step step = match(container);
        if (step == null) return ItemStack.EMPTY;

        ItemStack out = new ItemStack(step.to());

        // Der eigentliche Sinn: den Inhalt der alten Kiste uebernehmen.
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (Block.byItem(stack.getItem()) != step.from()) continue;
            if (stack.hasTag()) out.setTag(stack.getTag().copy());
            break;
        }

        return out;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        // Die alte Kiste wird verbraucht - sie steckt jetzt im Ergebnis.
        return NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
    }
    //?}

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MASS_STORAGE_UPGRADE.get();
    }

    /** Wird nur als {@code MachineMassStorageBlock} verwendet - der Import haelt die Absicht fest. */
    static Class<?> marker() {
        return MachineMassStorageBlock.class;
    }
}
