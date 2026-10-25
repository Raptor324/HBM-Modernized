//? if forge {
package com.hbm_m.datagen.recipes.custom;

import java.util.function.Consumer;

import com.hbm_m.lib.RefStrings;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Bedrock-Erz-Fragmente zurueck zum Material, 1:1 NTM {@code OreDictManager.registerOres} (Schleife ueber alle
 * Materialien mit {@code MaterialShapes.FRAGMENT}). Erste passende Regel ueber den Namen {@code mat.names[0]}:
 * <ol>
 *   <li>{@code dust<Name>} vorhanden: 9 Fragmente -> 1 erster Eintrag ({@code MineralRecipes.add9To1})</li>
 *   <li>sonst {@code gem<Name>}: 9 -> 1</li>
 *   <li>sonst {@code crystal<Name>}: 9 -> 1</li>
 *   <li>sonst {@code ingot<Name>}: 9 -> 1</li>
 *   <li>sonst {@code billet<Name>}: {@code addBilletFragment}, Muster "###"/"###" (6 Fragmente) -> 1 Billet</li>
 *   <li>sonst 9 -> {@code nothing}</li>
 * </ol>
 * {@code add9To1} hat keine Rueckrichtung. Eingabe ist wie im Original das konkrete Fragment ({@code mat.make}),
 * Ausgabe der erste OreDict-Eintrag ({@code getOres(..).get(0)}) - deshalb Gegenstaende statt Tags. Die Tabelle
 * ist aus den DictFrames des Originals abgeleitet (Vanilla-Eintraege von Forge stehen vorn, z. B. dustRedstone).
 * NTM Space (twopointfive) ist fuer diese 44 Materialien identisch; die 30 Space-eigenen Fragmente erzeugt das Addon.
 */
public final class BedrockFragmentRecipeGenerator {

    private BedrockFragmentRecipeGenerator() {}

    private static final int DUST = 1, GEM = 2, CRYSTAL = 3, INGOT = 4, BILLET = 5, NOTHING = 6;

    /** Fragment-Suffix, Regel, Ausgabe-ID (Original-Eintrag im Kommentar). */
    private static final Object[][] TABLE = {
            {"coal", DUST, "hbm_m:coal_powder"},                  // dustCoal -> powder_coal
            {"lignite", DUST, "hbm_m:lignite_powder"},            // dustLignite -> powder_lignite
            {"diamond", DUST, "hbm_m:diamond_powder"},            // dustDiamond -> powder_diamond
            {"iron", DUST, "hbm_m:iron_powder"},                  // dustIron -> powder_iron
            {"gold", DUST, "hbm_m:gold_powder"},                  // dustGold -> powder_gold
            {"redstone", DUST, "minecraft:redstone"},             // dustRedstone -> Items.redstone (Forge)
            {"bauxite", GEM, "hbm_m:stone_resource_bauxite"},     // gemBauxite -> stone_resource:BAUXITE
            {"cryolite", CRYSTAL, "hbm_m:cryolite_chunk"},        // crystalCryolite -> chunk_ore:CRYOLITE
            {"uranium", DUST, "hbm_m:uranium_powder"},            // dustUranium -> powder_uranium
            {"u238", INGOT, "hbm_m:u238_ingot"},                  // ingotUranium238 -> ingot_u238
            {"thorium", DUST, "hbm_m:thorium_powder"},            // dustThorium232 -> powder_thorium
            {"po210", DUST, "hbm_m:polonium_powder"},             // dustPolonium210 -> powder_polonium
            {"tc99", INGOT, "hbm_m:technetium_ingot"},            // ingotTechnetium99 -> ingot_technetium
            {"ra226", DUST, "hbm_m:ra226_powder"},                // dustRadium226 -> powder_ra226
            {"titanium", DUST, "hbm_m:titanium_powder"},          // dustTitanium
            {"copper", DUST, "hbm_m:copper_powder"},              // dustCopper
            {"tungsten", DUST, "hbm_m:tungsten_powder"},          // dustTungsten
            {"aluminium", DUST, "hbm_m:aluminum_powder"},         // dustAluminum
            {"lead", DUST, "hbm_m:lead_powder"},                  // dustLead
            {"bismuth", DUST, "hbm_m:bismuth_powder"},            // dustBismuth
            {"tantalium", DUST, "hbm_m:tantalium_powder"},        // dustTantalum
            {"neodymium", DUST, "hbm_m:neodymium_powder"},        // dustNeodymium
            {"niobium", DUST, "hbm_m:niobium_powder"},            // dustNiobium
            {"beryllium", DUST, "hbm_m:beryllium_powder"},        // dustBeryllium
            {"emerald", DUST, "hbm_m:emerald_powder"},            // dustEmerald -> powder_emerald
            {"cobalt", DUST, "hbm_m:cobalt_powder"},              // dustCobalt
            {"boron", DUST, "hbm_m:boron_powder"},                // dustBoron
            {"borax", DUST, "hbm_m:borax"},                       // dustBorax -> powder_borax
            {"lanthanium", DUST, "hbm_m:lanthanium_powder"},      // dustLanthanum
            {"zirconium", DUST, "hbm_m:zirconium_powder"},        // dustZirconium
            {"sodium", DUST, "hbm_m:powder_sodium"},              // dustSodium
            {"sodalite", GEM, "hbm_m:gem_sodalite"},              // gemSodalite
            {"strontium", DUST, "hbm_m:strontium_powder"},        // dustStrontium
            {"lithium", DUST, "hbm_m:lithium_powder"},            // dustLithium
            {"sulfur", DUST, "hbm_m:sulfur"},                     // dustSulfur -> sulfur
            {"kno", DUST, "hbm_m:niter"},                         // dustSaltpeter -> niter
            {"fluorite", DUST, "hbm_m:fluorite"},                 // dustFluorite -> fluorite
            {"phosphorus", DUST, "hbm_m:fire_powder"},            // dustRedPhosphorus -> powder_fire
            {"chlorocalcite", DUST, "hbm_m:powder_chlorocalcite"},// dustChlorocalcite
            {"molysite", DUST, "hbm_m:molysite"},                 // dustMolysite -> powder_molysite
            {"cinnabar", GEM, "hbm_m:cinnebar"},                  // gemCinnabar -> cinnebar
            {"silicon", INGOT, "hbm_m:silicon_ingot"},            // ingotSilicon -> ingot_silicon
            {"asbestos", DUST, "hbm_m:asbestos_powder"},          // dustAsbestos
            {"rare_earth", INGOT, "hbm_m:rareground_ore_chunk"},  // ingotRareEarth -> chunk_ore:RARE
    };

    public static void generate(Consumer<FinishedRecipe> w) {
        for (Object[] row : TABLE) {
            String mat = (String) row[0];
            int rule = (Integer) row[1];
            Item frag = item("hbm_m:bedrock_ore_fragment_" + mat);
            Item out = item((String) row[2]);
            add(w, mat, frag, out, rule);
        }
    }

    private static void add(Consumer<FinishedRecipe> w, String name, Item frag, Item out, int rule) {
        ShapedRecipeBuilder b;
        if (rule == BILLET) {
            // MineralRecipes.addBilletFragment(billet, fragment): "###","###" -> billet
            b = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, out, 1).pattern("###").pattern("###");
        } else {
            // MineralRecipes.add9To1(fragment, out): "###","###","###"
            Item result = rule == NOTHING ? item("hbm_m:nothing") : out;
            b = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, 1).pattern("###").pattern("###").pattern("###");
        }
        b.define('#', frag);
        b.unlockedBy("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(frag));
        b.save(w, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "bedrock_fragment/" + name));
    }

    private static Item item(String id) {
        Item i = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (i == null || i == Items.AIR) throw new IllegalStateException("Bedrock-Fragment-Rezept: Gegenstand fehlt: " + id);
        return i;
    }
}
//?}
