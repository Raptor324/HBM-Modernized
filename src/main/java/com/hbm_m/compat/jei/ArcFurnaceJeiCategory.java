package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MatDistribution;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm_m.inventory.recipes.ArcFurnaceRecipes;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.RecipeHooks;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * JEI category for Arc Furnace recipes — 1 Item-Eingang -&gt; bis zu 3 Item-Ausgaenge.
 *
 * <p>Port der 1.7.10 NEI-Handler {@code ArcFurnaceSolidHandler}/{@code ArcFurnaceFluidHandler}
 * (beide {@code NEIUniversalHandler}-Unterklassen, NEIRegistry.java:76-77): ein Eingang bei (48, 24),
 * Ergebnisse ueber {@code getOutputCoords}, Maschine im Rahmen bei (75, 31). Im Original wurden die
 * Solid- und Fluid-Rezeptlisten als zwei NEI-Seiten ({@code ntmArcFurnaceSolid}/
 * {@code ntmArcFurnaceFluid}) gefuehrt; hier erscheinen beide Mengen in einer Kategorie.</p>
 *
 * <p>Rezeptquelle ist dieselbe wie die der Maschine: die dynamische
 * {@link ArcFurnaceRecipes#getOutput} (Handrezepte, Bedrock-Erze, Autogen). Die Datapack-JSONs
 * {@code hbm_m:arc_furnace} sind keine Rezeptquelle der Maschine und werden - wie im Original -
 * nicht angezeigt. Die beiden Original-Seiten:</p>
 * <ul>
 *   <li><b>Solid</b> (Original {@code ArcFurnaceRecipes.getSolidRecipes}, :200-206): alle
 *       {@code recipeList}-Eintraege mit festem Ausgang (13 Handrezepte Sand/Kiesel/Quarz/Glasfaser/
 *       Asbest/Borax/Ilmenit, :53-66, plus Bedrock-Erz-Konversionen) gefolgt vom Ofen-Autogen
 *       (Original {@code registerFurnaceSmeltables}, :116,119-134: jeder Ofen-Eintrag, dessen
 *       Eingang oder Ausgang arcSmeltable ist, solange der Eingang nicht schon belegt ist).</li>
 *   <li><b>Fluid</b> (Original {@code ArcFurnaceRecipes.getFluidRecipes}, :208-223): alle
 *       {@code recipeList}-Eintraege mit Schmelz-Ausgang, angezeigt als {@code ItemScraps}-Stacks
 *       mit der Menge im NBT (Original :213,219 + ItemScraps.create), dazu der Formen-Autogen
 *       (Original :87-104), die Spezial-Schmelzwerte ({@code MatDistribution}, Original :107-113)
 *       und ein synthetischer Scraps&rarr;Schmelze-Eintrag je schmelzbarem Material (:217-221).</li>
 * </ul>
 *
 * <p>Wie im Original ({@code NEIUniversalHandler} :299-300,317-318,348-349) werden Rezepte mit
 * Geheim-Items (Port der {@code item_secret}-Familie) nicht angezeigt.</p>
 */
@OnlyIn(Dist.CLIENT)
public class ArcFurnaceJeiCategory extends JeiGenericRecipeCategory<ArcFurnaceJeiCategory.Entry> {

    public static final RecipeType<Entry> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "arc_furnace", Entry.class);

    /** Hoechste Slotanzahl des Original-Layouts ({@code NEIUniversalHandler.getOutputCoords}). */
    private static final int MAX_OUTPUT_SLOTS = 8;

    public ArcFurnaceJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModBlocks.ARC_FURNACE.get())
        });
    }

    /**
     * Ein angezeigtes Rezept: Eingangsvarianten (Original: {@code PositionedStack} mit
     * {@code extractForNEI}-Liste) und die Item-Ausgaenge (Fluid-Ausgaenge als {@code ItemScraps}).
     */
    public record Entry(List<ItemStack> inputs, List<ItemStack> outputs) { }

    @Override
    public RecipeType<Entry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.arc_furnace");
    }

    // ═══════════════════════════ Rezeptliste ═══════════════════════════

    /** Beide Original-Seiten: feste Liste + Autogen, gleiche Reihenfolge wie im Original. */
    public static List<Entry> recipes(Level level) {
        ArcFurnaceRecipes.ensureLoaded();
        List<Entry> list = new ArrayList<>();
        addSolidRecipes(list, level);
        addFluidRecipes(list);

        // Оригинал NEIUniversalHandler: Rezepte mit item_secret werden nicht angezeigt.
        Set<Item> secrets = secretItems();
        list.removeIf(entry -> containsSecret(entry, secrets));
        return list;
    }

    /** Original {@code getSolidRecipes} (:200-206) + Ofen-Autogen ({@code registerFurnaceSmeltables}, :119-134). */
    private static void addSolidRecipes(List<Entry> list, Level level) {

        // Feste Eintraege mit festem Ausgang (Handrezepte + Bedrock-Erz-Konversionen).
        for (ArcFurnaceRecipes.Entry entry : ArcFurnaceRecipes.recipeList) {
            ArcFurnaceRecipes.ArcFurnaceRecipe recipe = entry.recipe();
            if (recipe.solidOutput == null || recipe.solidOutput.isEmpty()) continue;
            List<ItemStack> inputs = resolveInputs(entry);
            if (inputs.isEmpty()) continue;
            list.add(new Entry(inputs, List.of(recipe.solidOutput.copy())));
        }

        // Ofen-Autogen: jeder Smelting-Eintrag, dessen Eingang oder Ausgang arcSmeltable ist und
        // dessen Eingang noch nicht durch ein festes Rezept belegt ist (Original: occupiedSolid).
        // Die Maschine nimmt den ersten passenden Ofen-Eintrag (RecipeManager.getRecipeFor) -
        // genau dieser erscheint hier, kein Doppel-Eintrag fuer denselben Eingang.
        for (SmeltingRecipe recipe : RecipeHooks.getAllRecipes(level, net.minecraft.world.item.crafting.RecipeType.SMELTING)) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;
            if (recipe.getIngredients().isEmpty()) continue;
            for (ItemStack variant : recipe.getIngredients().get(0).getItems()) {
                if (variant.isEmpty()) continue;
                ItemStack input = variant.copyWithCount(1);
                if (occupiedByStaticSolid(input)) continue;
                if (!ArcFurnaceRecipes.isArcSmeltable(input) && !ArcFurnaceRecipes.isArcSmeltable(result)) continue;
                SmeltingRecipe actual = RecipeHooks.getRecipeFor(level, net.minecraft.world.item.crafting.RecipeType.SMELTING, input).orElse(null);
                if (actual != recipe) continue;
                list.add(new Entry(List.of(input), List.of(result.copy())));
            }
        }
    }

    /** Original {@code getFluidRecipes} (:208-223) plus die im Original fest registrierten Autogene. */
    private static void addFluidRecipes(List<Entry> list) {
        // Original occupiedLiquid: belegte Eingaben werden vom Autogen nicht ueberschrieben.
        Set<Item> occupied = new HashSet<>();

        // Feste Eintraege mit Schmelz-Ausgang, angezeigt als ItemScraps-Stacks (Original :210-215).
        for (ArcFurnaceRecipes.Entry entry : ArcFurnaceRecipes.recipeList) {
            ArcFurnaceRecipes.ArcFurnaceRecipe recipe = entry.recipe();
            if (recipe.fluidOutput == null || recipe.fluidOutput.length == 0) continue;
            List<ItemStack> outputs = scrapOutputs(recipe.fluidOutput);
            if (outputs.isEmpty()) continue;
            List<ItemStack> inputs = resolveInputs(entry);
            if (inputs.isEmpty()) continue;
            for (ItemStack input : inputs) occupied.add(input.getItem());
            list.add(new Entry(inputs, outputs));
        }

        // Formen-Autogen (Original :86-104, Port: lookup in ArcFurnaceRecipes): jede Form eines
        // Materials, das in ein schmelzbares Material uebergeht, wird fluessig.
        for (NTMMaterial material : Mats.orderedList) {
            NTMMaterial convert = material.smeltsInto;
            if (convert.smeltable != SmeltingBehavior.SMELTABLE) continue;
            for (MaterialShapes shape : MaterialShapes.allShapes) {
                if (shape.noAutogen) continue;
                Item item = Mats.getItemForShape(material, shape);
                if (item == null || item == Items.AIR || occupied.contains(item)) continue;
                ItemStack input = new ItemStack(item);
                if (occupiedByStaticFluid(input)) continue;
                occupied.add(item);
                int amount = (int) (shape.q(1) * material.convOut / material.convIn);
                ItemStack output = ItemScraps.create(new MaterialStack(convert, amount), true);
                if (output.isEmpty()) continue;
                list.add(new Entry(List.of(input), List.of(output)));
            }
        }

        // Spezial-Schmelzwerte (Original :106-113, Port: MatDistribution) - erst die Erz-Eintraege
        // (Forge-Tags + Port-Erzbloecke ohne Tag), dann die festen Item-Eintraege.
        for (Map.Entry<String, List<MaterialStack>> entry : Mats.oreEntries()) {
            addCustomSmeltable(list, occupied, itemsForOreKey(entry.getKey()), entry.getValue());
        }
        MatDistribution.ensureLoaded();
        for (Map.Entry<Item, List<MaterialStack>> entry : Mats.materialEntries.entrySet()) {
            if (entry.getKey() == null || entry.getKey() == Items.AIR) continue;
            addCustomSmeltable(list, occupied, List.of(new ItemStack(entry.getKey())), entry.getValue());
        }

        // Synthetische Scraps-Eintraege je schmelzbarem Material (Original :217-221):
        // ein trockener Scraps-Stack wird zur Schmelze desselben Materials (Ingot-Menge).
        for (NTMMaterial material : Mats.orderedList) {
            if (material.smeltable != SmeltingBehavior.SMELTABLE) continue;
            MaterialStack stack = new MaterialStack(material, MaterialShapes.INGOT.q(1));
            ItemStack input = ItemScraps.create(stack, false);
            ItemStack output = ItemScraps.create(stack, true);
            if (input.isEmpty() || output.isEmpty()) continue;
            list.add(new Entry(List.of(input), List.of(output)));
        }
    }

    /** Original {@code addCustomSmeltable} (:153-164): schmelzbare Anteile fluessig, belegte Eingaben uebersprungen. */
    private static void addCustomSmeltable(List<Entry> list, Set<Item> occupied, List<ItemStack> inputs, List<MaterialStack> mats) {
        if (inputs.isEmpty()) return;
        List<MaterialStack> smeltables = new ArrayList<>();
        for (MaterialStack mat : mats) {
            if (mat != null && mat.material != null && mat.material.smeltable == SmeltingBehavior.SMELTABLE) smeltables.add(mat);
        }
        if (smeltables.isEmpty()) return;
        for (ItemStack input : inputs) {
            if (occupied.contains(input.getItem())) return;
        }
        List<ItemStack> outputs = scrapOutputs(smeltables.toArray(new MaterialStack[0]));
        if (outputs.isEmpty()) return;
        for (ItemStack input : inputs) occupied.add(input.getItem());
        list.add(new Entry(inputs, outputs));
    }

    /** Fluid-Ausgaenge als ItemScraps-Item-Stacks mit der Menge im NBT (Original getFluidRecipes :213,219). */
    private static List<ItemStack> scrapOutputs(MaterialStack[] fluidOutput) {
        List<ItemStack> outputs = new ArrayList<>();
        for (MaterialStack mat : fluidOutput) {
            if (mat == null || mat.material == null) continue;
            ItemStack scrap = ItemScraps.create(mat, true);
            if (!scrap.isEmpty()) outputs.add(scrap);
        }
        return outputs;
    }

    /** Display-Stacks des festen Eintrags; leeres Display (Tag vor dem Binden) ueber das Praedikat aufloesen. */
    private static List<ItemStack> resolveInputs(ArcFurnaceRecipes.Entry entry) {
        List<ItemStack> display = entry.display();
        if (display != null && !display.isEmpty()) return display;
        List<ItemStack> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (entry.input().test(stack)) {
                out.add(stack);
                if (out.size() >= 64) break;
            }
        }
        return out;
    }

    /** Items eines Erz-Schluessels: Tag-Mitglieder plus Port-Erzbloecke ohne Tag ({@code Mats.oreItemKeys}). */
    private static List<ItemStack> itemsForOreKey(String key) {
        List<ItemStack> out = new ArrayList<>();
        Set<Item> seen = new HashSet<>();
        ResourceLocation loc = ResourceLocation.tryParse(key);
        if (loc != null) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, loc);
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> {
                if (seen.add(holder.value())) out.add(new ItemStack(holder.value()));
            });
        }
        for (Map.Entry<String, String> mapped : Mats.oreItemKeys.entrySet()) {
            if (!mapped.getValue().equals(key)) continue;
            ResourceLocation itemId = ResourceLocation.tryParse(mapped.getKey());
            if (itemId == null) continue;
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != Items.AIR && seen.add(item)) out.add(new ItemStack(item));
        }
        return out;
    }

    /** Original occupiedSolid: festes Rezept mit festem Ausgang besiegt den Eingang (Lookup-Reihenfolge). */
    private static boolean occupiedByStaticSolid(ItemStack stack) {
        for (ArcFurnaceRecipes.Entry entry : ArcFurnaceRecipes.recipeList) {
            ArcFurnaceRecipes.ArcFurnaceRecipe recipe = entry.recipe();
            if (recipe.solidOutput == null || recipe.solidOutput.isEmpty()) continue;
            if (entry.input().test(stack)) return true;
        }
        return false;
    }

    /** Original occupiedLiquid: festes Rezept mit Schmelz-Ausgang besiegt den Eingang (Lookup-Reihenfolge). */
    private static boolean occupiedByStaticFluid(ItemStack stack) {
        for (ArcFurnaceRecipes.Entry entry : ArcFurnaceRecipes.recipeList) {
            ArcFurnaceRecipes.ArcFurnaceRecipe recipe = entry.recipe();
            if (recipe.fluidOutput == null || recipe.fluidOutput.length == 0) continue;
            if (entry.input().test(stack)) return true;
        }
        return false;
    }

    /** Port der Original-{@code item_secret}-Familie (ModItems, rote Kammer). */
    private static Set<Item> secretItems() {
        Set<Item> secrets = new HashSet<>();
        secrets.add(ModItems.ITEM_SECRET_CANISTER.get());
        secrets.add(ModItems.ITEM_SECRET_CONTROLLER.get());
        secrets.add(ModItems.ITEM_SECRET_SELENIUM_STEEL.get());
        secrets.add(ModItems.ITEM_SECRET_ABERRATOR.get());
        secrets.add(ModItems.ITEM_SECRET_FOLLY.get());
        return secrets;
    }

    /** Оригинал NEIUniversalHandler (:299-300,317-318,348-349): Geheim-Items in Eingang oder Ausgang. */
    private static boolean containsSecret(Entry entry, Set<Item> secrets) {
        for (ItemStack stack : entry.inputs()) {
            if (!stack.isEmpty() && secrets.contains(stack.getItem())) return true;
        }
        for (ItemStack stack : entry.outputs()) {
            if (!stack.isEmpty() && secrets.contains(stack.getItem())) return true;
        }
        return false;
    }

    // ═══════════════════════════ Layout ═══════════════════════════

    @Override
    protected int getInputCount(Entry recipe) {
        return 1;
    }

    @Override
    protected int getOutputCount(Entry recipe) {
        return Math.min(recipe.outputs().size(), MAX_OUTPUT_SLOTS);
    }

    @Override
    protected boolean hasBlueprintTemplate(Entry recipe) {
        return false;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, Entry recipe, int inputXOffset) {
        // Original NEIUniversalHandler.getInputCoords(1) = {48, 24} (Slot-Rahmen auf (47, 23)).
        int[][] positions = JeiNeiLayout.getUniversalInputCoords(1);
        addItemSlot(builder, RecipeIngredientRole.INPUT,
                positions[0][0] + inputXOffset, positions[0][1])
                .addItemStacks(recipe.inputs());
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, Entry recipe, int outputXOffset) {
        // 1:1 NEIUniversalHandler.getOutputCoords: 1-3 Ausgaenge in einer Reihe, der Fluid-Anteil
        // erscheint wie im Original als ItemScraps-Stack mit der Menge im NBT.
        int count = getOutputCount(recipe);
        int[][] positions = JeiNeiLayout.getUniversalOutputCoords(count);
        for (int i = 0; i < count && i < recipe.outputs().size(); i++) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT,
                    positions[i][0] + outputXOffset, positions[i][1])
                    .addItemStack(recipe.outputs().get(i));
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, Entry recipe, int machineXOffset) {
        // Kein Blueprint-Slot fuer Arc-Furnace-Rezepte.
    }

    // Kein drawRecipeExtras: das Original NEIUniversalHandler zeichnet ausser den Slot-Rahmen
    // und dem Maschinenrahmen keine Dauer/Verbrauchs-Texte.
}
