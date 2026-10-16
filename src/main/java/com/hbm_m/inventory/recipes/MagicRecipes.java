package com.hbm_m.inventory.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * «Магические» рецепты книги Вагонов ({@code book_of_}) — порт оригинального
 * {@code com.hbm.inventory.recipes.MagicRecipes} из 1.7.10.
 *
 * <p>Рецепты бесформенные по сути (сетка 2×2, позиция не важна), но сопоставление в
 * оригинале ПОСЛЕДОВАТЕЛЬНОЕ: непустые слоты собираются в список по порядку индексов
 * сетки и сравниваются с входами рецепта попозиционно (сортировка в оригинале была
 * закомментирована — это часть «магии»). Поведение сохранено 1:1.</p>
 *
 * <p>Рецепты, ссылающиеся на ещё не портированные предметы (mysteryshovel,
 * ingot_electronium/pellet_charged, метаварианты ingot_u238m2), не переносятся —
 * в 1.7.10 {@code CyclotronRecipes} аналогично пропускал отсутствующие в реестре предметы.</p>
 */
public class MagicRecipes {

    private static List<MagicRecipe> recipes;

    private static List<MagicRecipe> recipes() {
        if (recipes == null) {
            recipes = new ArrayList<>();
            register();
        }
        return recipes;
    }

    /** Сопоставляет содержимое сетки 2×2 с зарегистрированными магическими рецептами. */
    public static ItemStack getRecipe(CraftingContainer matrix) {

        List<ItemStack> comps = new ArrayList<>();

        for(int i = 0; i < matrix.getContainerSize(); i++) {
            if(!matrix.getItem(i).isEmpty())
                comps.add(matrix.getItem(i));
        }

        for(MagicRecipe recipe : recipes()) {
            if(recipe.matches(comps))
                return recipe.getResult();
        }

        return ItemStack.EMPTY;
    }

    private static void register() {
        // Слиток урана-238M2 (в оригинале — три метаварианта одного слитка; меты не портированы)
        // Раковина дискордии
        recipes.add(new MagicRecipe(ModItems.ROD_OF_DISCORD,
                () -> Ingredient.of(Items.ENDER_PEARL),
                () -> Ingredient.of(Items.BLAZE_ROD),
                () -> Ingredient.of(ModItems.NUGGET_EUPHEMIUM.get())));

        // Balefire and Steel
        recipes.add(new MagicRecipe(ModItems.BALEFIRE_AND_STEEL,
                () -> Ingredient.of(ModItems.getIngot(ModIngots.STEEL).get()),
                () -> Ingredient.of(ModItems.EGG_BALEFIRE_SHARD.get())));

        // Алмазная кувалда
        recipes.add(new MagicRecipe(ModItems.DIAMOND_GAVEL,
                () -> Ingredient.of(ModBlocks.GRAVEL_DIAMOND.get().asItem()),
                () -> Ingredient.of(ModBlocks.GRAVEL_DIAMOND.get().asItem()),
                () -> Ingredient.of(ModBlocks.GRAVEL_DIAMOND.get().asItem()),
                () -> Ingredient.of(ModItems.LEAD_GAVEL.get())));

        // Мезозная кувалда
        recipes.add(new MagicRecipe(ModItems.MESE_GAVEL,
                () -> Ingredient.of(ModItems.SHIMMER_HANDLE.get()),
                () -> Ingredient.of(ModItems.getPowder(ModIngots.DINEUTRONIUM).get()),
                () -> Ingredient.of(ModItems.BLADES_DESH.get()),
                () -> Ingredient.of(ModItems.DIAMOND_GAVEL.get())));
    }

    public static List<MagicRecipe> getRecipes() {
        return List.copyOf(recipes());
    }

    public static class MagicRecipe {

        public final List<Supplier<Ingredient>> in;
        public final Supplier<Item> out;

        /**
         * @param out поставщик результата (лениво — на момент построения списка реестр ещё
         *            может быть не готов)
         * @param in  поставщики ингредиентов (лениво по той же причине)
         */
        public MagicRecipe(Supplier<Item> out, Supplier<Ingredient>... in) {
            this.out = out;
            this.in = List.of(in);
        }

        public boolean matches(List<ItemStack> comps) {

            if(comps.size() != in.size())
                return false;

            for(int i = 0; i < in.size(); i++) {
                if(!in.get(i).get().test(comps.get(i))) return false;
            }

            return true;
        }

        public ItemStack getResult() {
            return new ItemStack(out.get());
        }
    }
}
