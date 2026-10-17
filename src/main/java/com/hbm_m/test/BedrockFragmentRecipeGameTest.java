package com.hbm_m.test;

import java.util.Map;
import java.util.Optional;

import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.util.SimpleCraftingContainer;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Bedrock-Fragment -> Material (NTM {@code OreDictManager}, {@code MineralRecipes.add9To1}): Stichproben je Regel
 * ueber die echte Werkbank-Suche, Gesamtzahl 44, keine Rueckrichtung.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class BedrockFragmentRecipeGameTest {

    private BedrockFragmentRecipeGameTest() {}

    private static Item item(String id) {
        Item i = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (i == null || i == Items.AIR) throw new GameTestAssertException("Gegenstand fehlt: " + id);
        return i;
    }

    private static SimpleCraftingContainer grid(Item in, int filled) {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < filled; i++) items.set(i, new ItemStack(in));
        return new SimpleCraftingContainer(items, 3, 3);
    }

    private static void expect(GameTestHelper helper, String frag, String out) {
        SimpleCraftingContainer g = grid(item("hbm_m:bedrock_ore_fragment_" + frag), 9);
        Optional<CraftingRecipe> r = RecipeHooks.getCraftingRecipeFor(helper.getLevel(), g);
        if (r.isEmpty()) throw new GameTestAssertException("Kein Rezept fuer 9x Fragment " + frag);
        ItemStack res = RecipeHooks.assembleCrafting(r.get(), g, helper.getLevel());
        if (res.getItem() != item(out) || res.getCount() != 1)
            throw new GameTestAssertException(frag + ": erwartet 1x " + out + ", bekommen " + res.getCount() + "x " + BuiltInRegistries.ITEM.getKey(res.getItem()));
    }

    @GameTest(template = "empty3x3x3", batch = "bedrock_fragment", timeoutTicks = 100)
    public static void fragmentRules(GameTestHelper helper) {
        expect(helper, "iron", "hbm_m:iron_powder");           // Regel 1 dust
        expect(helper, "diamond", "hbm_m:diamond_powder");     // Regel 1 (dustDiamond vor gemDiamond)
        expect(helper, "redstone", "minecraft:redstone");      // Regel 1, Vanilla-Eintrag
        expect(helper, "sodalite", "hbm_m:gem_sodalite");      // Regel 2 gem
        expect(helper, "cinnabar", "hbm_m:cinnebar");          // Regel 2 (gem vor crystal)
        expect(helper, "cryolite", "hbm_m:cryolite_chunk");    // Regel 3 crystal
        expect(helper, "u238", "hbm_m:u238_ingot");            // Regel 4 ingot
        expect(helper, "rare_earth", "hbm_m:rareground_ore_chunk"); // Regel 4 (ingotRareEarth = chunk_ore)
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "bedrock_fragment", timeoutTicks = 100)
    public static void fragmentCountAndNoReverse(GameTestHelper helper) {
        Map<ResourceLocation, CraftingRecipe> all = RecipeHooks.getAllRecipesById(helper.getLevel(), RecipeType.CRAFTING);
        long n = all.keySet().stream().filter(id -> id.getNamespace().equals("hbm_m") && id.getPath().startsWith("bedrock_fragment/")).count();
        if (n != 44) throw new GameTestAssertException("Erwartet 44 Fragment-Rezepte, gefunden " + n);
        // 8 Fragmente reichen nicht (9er-Muster)
        SimpleCraftingContainer eight = grid(item("hbm_m:bedrock_ore_fragment_iron"), 8);
        Optional<CraftingRecipe> r8 = RecipeHooks.getCraftingRecipeFor(helper.getLevel(), eight);
        if (r8.isPresent() && RecipeHooks.assembleCrafting(r8.get(), eight, helper.getLevel()).getItem() == item("hbm_m:iron_powder"))
            throw new GameTestAssertException("8 Eisen-Fragmente duerfen kein Eisenpulver ergeben");
        // add9To1 hat keine Rueckrichtung: kein Werkbankrezept ergibt ein Bedrock-Fragment
        for (Map.Entry<ResourceLocation, CraftingRecipe> e : all.entrySet()) {
            ItemStack res = e.getValue().getResultItem(helper.getLevel().registryAccess());
            if (BuiltInRegistries.ITEM.getKey(res.getItem()).getPath().startsWith("bedrock_ore_fragment_"))
                throw new GameTestAssertException("Rueckrichtung gefunden: " + e.getKey());
        }
        helper.succeed();
    }
}
