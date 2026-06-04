package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.Registries;


public final class CyclotronRecipes {

    private static final String MOD_ID = "hbm_m";

    public record Output(ItemStack output, int amatProduced) {
    }

    private record Recipe(Ingredient target, Ingredient input, ItemStack output, int amatProduced) {
        private boolean matches(ItemStack targetStack, ItemStack inputStack) {
            return target.test(targetStack) && input.test(inputStack);
        }
    }

    private static final List<Recipe> RECIPES = new ArrayList<>();

    public record JeiRecipe(Ingredient target, Ingredient input, ItemStack output, int amatProduced) {
    }

    private CyclotronRecipes() {
    }

    public static void registerRecipes() {
        RECIPES.clear();

        // Legacy 1.7.10 defaults from com.hbm.inventory.recipes.CyclotronRecipes.
        int liA = 50;
        addLegacyTag("part_lithium", "lithium", liA, "beryllium_powder", "powder_beryllium");
        addLegacyTag("part_lithium", "beryllium", liA, "boron_powder", "powder_boron");
        addLegacyTag("part_lithium", "boron", liA, "coal_powder", "powder_coal");
        addLegacyTag("part_lithium", "quartz", liA, "fire_powder", "powder_fire");
        addLegacyTag("part_lithium", "phosphorus", liA, "sulfur");
        addLegacyTag("part_lithium", "iron", liA, "cobalt_powder", "powder_cobalt");
        addLegacyItem("part_lithium", "strontium_powder", liA, "zirconium_powder", "powder_zirconium");
        addLegacyTag("part_lithium", "gold", liA, "mercury_ingot", "ingot_mercury");
        addLegacyTag("part_lithium", "polonium", liA, "astatine_powder", "powder_astatine");
        addLegacyTag("part_lithium", "lanthanium", liA, "cerium_powder", "powder_cerium");
        addLegacyTag("part_lithium", "actinium", liA, "thorium_powder", "powder_thorium");
        addLegacyTag("part_lithium", "uranium", liA, "neptunium_powder", "powder_neptunium");
        addLegacyTag("part_lithium", "neptunium", liA, "plutonium_powder", "powder_plutonium");

        int beA = 25;
        addLegacyTag("part_beryllium", "lithium", beA, "boron_powder", "powder_boron");
        addLegacyTag("part_beryllium", "quartz", beA, "sulfur");
        addLegacyTag("part_beryllium", "titanium", beA, "iron_powder", "powder_iron");
        addLegacyTag("part_beryllium", "cobalt", beA, "copper_powder", "powder_copper");
        addLegacyItem("part_beryllium", "strontium_powder", beA, "niobium_powder", "powder_niobium");
        addLegacyItem("part_beryllium", "cerium_powder", beA, "neodymium_powder", "powder_neodymium");
        addLegacyTag("part_beryllium", "thorium", beA, "uranium_powder", "powder_uranium");

        int caA = 10;
        addLegacyTag("part_carbon", "boron", caA, "aluminium_powder", "powder_aluminium");
        addLegacyTag("part_carbon", "sulfur", caA, "titanium_powder", "powder_titanium");
        addLegacyTag("part_carbon", "titanium", caA, "cobalt_powder", "powder_cobalt");
        addLegacyItem("part_carbon", "caesium_powder", caA, "lanthanium_powder", "powder_lanthanium");
        addLegacyItem("part_carbon", "neodymium_powder", caA, "gold_powder", "powder_gold");
        addLegacyItem("part_carbon", "mercury_ingot", caA, "polonium_powder", "powder_polonium");
        addLegacyTag("part_carbon", "lead", caA, "ra226_powder", "powder_ra226");
        addLegacyItem("part_carbon", "astatine_powder", caA, "actinium_powder", "powder_actinium");

        int coA = 15;
        addLegacyTag("part_copper", "beryllium", coA, "quartz_powder", "powder_quartz");
        addLegacyTag("part_copper", "coal", coA, "bromine_powder", "powder_bromine");
        addLegacyTag("part_copper", "titanium", coA, "strontium_powder", "powder_strontium");
        addLegacyTag("part_copper", "iron", coA, "niobium_powder", "powder_niobium");
        addLegacyItem("part_copper", "bromine_powder", coA, "iodine_powder", "powder_iodine");
        addLegacyItem("part_copper", "strontium_powder", coA, "neodymium_powder", "powder_neodymium");
        addLegacyItem("part_copper", "niobium_powder", coA, "caesium_powder", "powder_caesium");
        addLegacyItem("part_copper", "iodine_powder", coA, "polonium_powder", "powder_polonium");
        addLegacyItem("part_copper", "caesium_powder", coA, "actinium_powder", "powder_actinium");
        addLegacyTag("part_copper", "gold", coA, "uranium_powder", "powder_uranium");

        int plA = 100;
        addLegacyTag("part_plutonium", "phosphorus", plA, "tennessine_powder", "powder_tennessine");
        addLegacyTag("part_plutonium", "plutonium", plA, "tennessine_powder", "powder_tennessine");
        addLegacyItem("part_plutonium", "tennessine_powder", plA, "australium_powder", "powder_australium");
        addLegacyItem("part_plutonium", "pellet_charged", 1000, "nugget_schrabidium");
    }

    public static void addItemRecipe(Item target, Item input, ItemStack output, int amatProduced) {
        if (target == null || input == null) {
            return;
        }
        addRecipe(Ingredient.of(target), Ingredient.of(input), output, amatProduced);
    }

    public static void addItemRecipe(String targetId, String inputId, ItemStack output, int amatProduced) {
        Item target = byId(targetId);
        Item input = byId(inputId);
        if (target == null || input == null) {
            return;
        }
        addItemRecipe(target, input, output, amatProduced);
    }

    public static void addTagRecipe(String targetTagId, String inputTagId, ItemStack output, int amatProduced) {
        TagKey<Item> targetTag = tagById(targetTagId);
        TagKey<Item> inputTag = tagById(inputTagId);
        if (targetTag == null || inputTag == null) {
            return;
        }
        addRecipe(Ingredient.of(targetTag), Ingredient.of(inputTag), output, amatProduced);
    }

    public static void addRecipe(Ingredient target, Ingredient input, ItemStack output, int amatProduced) {
        if (target == null || input == null || output == null || output.isEmpty()) {
            return;
        }
        RECIPES.add(new Recipe(target, input, output.copy(), Math.max(0, amatProduced)));
    }

    public static Output getOutput(ItemStack target, ItemStack input) {
        if (target == null || input == null || target.isEmpty() || input.isEmpty()) {
            return null;
        }

        for (Recipe recipe : RECIPES) {
            if (recipe.matches(target, input)) {
                return new Output(recipe.output.copy(), recipe.amatProduced);
            }
        }

        return null;
    }

    public static boolean isValidInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (Recipe recipe : RECIPES) {
            if (recipe.input.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidTarget(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (Recipe recipe : RECIPES) {
            if (recipe.target.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static List<ItemStack> getMatchingInputsForTarget(ItemStack target) {
        if (target == null || target.isEmpty()) {
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>();
        for (Recipe recipe : RECIPES) {
            if (recipe.target.test(target)) {
                Collections.addAll(result, recipe.input.getItems());
            }
        }
        return result;
    }

    public static List<JeiRecipe> getJeiRecipes() {
        List<JeiRecipe> recipes = new ArrayList<>(RECIPES.size());
        for (Recipe recipe : RECIPES) {
            recipes.add(new JeiRecipe(recipe.target, recipe.input, recipe.output.copy(), recipe.amatProduced));
        }
        return recipes;
    }

    private static void addLegacyTag(String targetId, String inputDust, int amatProduced, String... outputIds) {
        Item target = byId(modItem(targetId));
        if (target == null) {
            return;
        }

        ItemStack output = stackAny(1, outputIds);
        if (output.isEmpty()) {
            return;
        }

        Ingredient targetIngredient = Ingredient.of(target);
        for (String tagId : dustTagCandidates(inputDust)) {
            TagKey<Item> tag = tagById(tagId);
            if (tag != null) {
                addRecipe(targetIngredient, Ingredient.of(tag), output, amatProduced);
            }
        }
    }

    private static void addLegacyItem(String targetId, String inputId, int amatProduced, String... outputIds) {
        Item target = byId(modItem(targetId));
        Item input = byId(modItem(inputId));
        if (target == null || input == null) {
            return;
        }

        ItemStack output = stackAny(1, outputIds);
        if (output.isEmpty()) {
            return;
        }

        addRecipe(Ingredient.of(target), Ingredient.of(input), output, amatProduced);
    }

    private static List<String> dustTagCandidates(String element) {
        return List.of("forge:powders/" + element, "forge:dusts/" + element);
    }

    private static ItemStack stackAny(int count, String... ids) {
        for (String id : ids) {
            Item item = byId(modItem(id));
            if (item != null) {
                return new ItemStack(item, count);
            }
        }
        return ItemStack.EMPTY;
    }

    private static String modItem(String path) {
        return path.contains(":") ? path : MOD_ID + ":" + path;
    }

    private static Item byId(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        return item == null ? null : item;
    }

    private static TagKey<Item> tagById(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return null;
        }
        return TagKey.create(Registries.ITEM, key);
    }
}
