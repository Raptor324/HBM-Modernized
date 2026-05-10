package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import dev.architectury.fluid.FluidStack;

/**
 * Реестр рецептов для рудного окислителя (Crystallizer).
 *
 * <p>Перенесены ключевые рецепты из 1.7.10 {@code CrystallizerRecipes#registerDefaults}.
 * Полный список из оригинала имеет ~70 рецептов (все bedrock-руды + специальные);
 * здесь покрыты самые востребованные. Дополнять можно либо здесь же (статически),
 * либо позже — через JSON datagen.</p>
 *
 * <p>Поиск рецепта идёт линейно по списку. На ~50-100 рецептах это норма; если
 * вырастет до 500+ — стоит закэшировать по {@code Item} входа.</p>
 */
public final class CrystallizerRecipes {

    private static final List<CrystallizerRecipe> RECIPES = new ArrayList<>();

    private static final int BASE_TIME = 600;     // 30 сек
    private static final int UTILITY_TIME = 100;  // 5 сек
    private static final int MIXING_TIME = 20;    // 1 сек

    private CrystallizerRecipes() {}

    /**
     * Возвращает первый подходящий рецепт для пары (входной предмет, жидкость в баке).
     * Жидкость может быть пустой — тогда подойдут только рецепты без требования
     * к кислоте (acid == null).
     */
    @Nullable
    public static CrystallizerRecipe findRecipe(ItemStack input, FluidStack tankFluid) {
        if (input.isEmpty()) return null;
        for (CrystallizerRecipe recipe : RECIPES) {
            if (recipe.matchesInput(input) && recipe.matchesAcid(tankFluid)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Все рецепты — для JEI-интеграции и отладки.
     */
    public static List<CrystallizerRecipe> getAll() {
        return List.copyOf(RECIPES);
    }

    /**
     * Регистрирует все встроенные рецепты. Вызывается один раз при старте мода
     * (например, из FMLCommonSetupEvent в MainRegistry).
     */
    public static void registerDefaults() {
        if (!RECIPES.isEmpty()) return; // защита от двойной регистрации
        FluidStack nitric   = FluidStack.create(ModFluids.NITRIC_ACID.getSource(),   500L);
        FluidStack hiperf   = FluidStack.create(ModFluids.RADIOSOLVENT.getSource(),  500L);
        FluidStack sulfur   = FluidStack.create(ModFluids.SULFURIC_ACID.getSource(), 500L);

        // === Базовые руды (через теги Forge — ловят и наши, и ванильные руды) ===
        addOreTag("forge:ores/coal",     ModItems.CRYSTAL_COAL.get(),     0.05f, null);
        addOreTag("forge:ores/iron",     ModItems.CRYSTAL_IRON.get(),     0.05f, null);
        addOreTag("forge:ores/gold",     ModItems.CRYSTAL_GOLD.get(),     0.05f, null);
        addOreTag("forge:ores/redstone", ModItems.CRYSTAL_REDSTONE.get(), 0.05f, null);
        addOreTag("forge:ores/lapis",    ModItems.CRYSTAL_LAPIS.get(),    0.05f, null);
        addOreTag("forge:ores/diamond",  ModItems.CRYSTAL_DIAMOND.get(),  0.05f, null);
        addOreTag("forge:ores/copper",   ModItems.CRYSTAL_COPPER.get(),   0.05f, null);

        // === Радиоактивные / тугоплавкие — требуют серную кислоту ===
        addOreTag("forge:ores/uranium",   ModItems.CRYSTAL_URANIUM.get(),    0.05f, sulfur);
        addOreTag("forge:ores/thorium",   ModItems.CRYSTAL_THORIUM.get(),    0.05f, sulfur);
        addOreTag("forge:ores/plutonium", ModItems.CRYSTAL_PLUTONIUM.get(),  0.05f, sulfur);
        addOreTag("forge:ores/titanium",  ModItems.CRYSTAL_TITANIUM.get(),   0.05f, sulfur);
        addOreTag("forge:ores/tungsten",  ModItems.CRYSTAL_TUNGSTEN.get(),   0.05f, sulfur);
        addOreTag("forge:ores/cobalt",    ModItems.CRYSTAL_COBALT.get(),     0.05f, sulfur);
        addOreTag("forge:ores/lithium",   ModItems.CRYSTAL_LITHIUM.get(),    0.05f, sulfur);
        addOreTag("forge:ores/schrabidium", ModItems.CRYSTAL_SCHRABIDIUM.get(), 0.05f, sulfur);

        // === Без кислоты — лёгкая обработка ===
        addOreTag("forge:ores/sulfur",    ModItems.CRYSTAL_SULFUR.get(),     0.05f, null);
        addOreTag("forge:ores/niter",     ModItems.CRYSTAL_NITER.get(),      0.05f, null);
        addOreTag("forge:ores/aluminum",  ModItems.CRYSTAL_ALUMINIUM.get(),  0.05f, null);
        addOreTag("forge:ores/fluorite",  ModItems.CRYSTAL_FLUORITE.get(),   0.05f, null);
        addOreTag("forge:ores/beryllium", ModItems.CRYSTAL_BERYLLIUM.get(),  0.05f, null);
        addOreTag("forge:ores/lead",      ModItems.CRYSTAL_LEAD.get(),       0.05f, null);

        // === Прямые рецепты блок -> предмет ===
        // Перевод: REDSTONE.block (блок редстоуна) -> ингот ртути с шансом 0.25
        // (если у вас есть ModItems.INGOT_MERCURY — раскомментируйте)
        // addItem(Items.REDSTONE_BLOCK, ModItems.INGOT_MERCURY.get(), 0.25f, BASE_TIME, null);

        // === Утилитарные крафты ===
        // Гнилое мясо -> кожа (mixingTime, productivity 0.25)
        addItem(Items.ROTTEN_FLESH, new ItemStack(Items.LEATHER), 0.25f, UTILITY_TIME, null);

        // Булыжник -> укреплённый камень (если есть ModBlocks.REINFORCED_STONE)
        // addItem(Items.COBBLESTONE, new ItemStack(ModBlocks.REINFORCED_STONE.get()), 0f, UTILITY_TIME, null);

        // Кость -> 16 слизи с серной кислотой (1000 mB)
        addItem(Items.BONE, new ItemStack(Items.SLIME_BALL, 16), 0f, MIXING_TIME,
                FluidStack.create(ModFluids.SULFURIC_ACID.getSource(), 1_000L));

        // Алмазная пыль -> алмаз (нужен ModItems.POWDER_DIAMOND)
        // addItem(ModItems.POWDER_DIAMOND.get(), new ItemStack(Items.DIAMOND), 0f, UTILITY_TIME, null);

        // Изумрудная пыль -> изумруд
        // addItem(ModItems.POWDER_EMERALD.get(), new ItemStack(Items.EMERALD), 0f, UTILITY_TIME, null);

        // === Резина из латекса (если есть items) ===
        // addItem(ModItems.INGOT_LATEX.get(), new ItemStack(ModItems.INGOT_RUBBER.get()), 0.15f, MIXING_TIME,
        //         new FluidStack(ModFluids.SOURGAS.getSource(), 25));
    }

    // ───────────────── helpers ─────────────────

    private static void addOreTag(String tagId, Item output, float productivity, @Nullable FluidStack acid) {
        ResourceLocation id = ResourceLocation.tryParse(tagId);
        if (id == null) {
            throw new IllegalArgumentException("Invalid item tag id: " + tagId);
        }
        TagKey<Item> tag = TagKey.create(Registries.ITEM, id);
        Ingredient ing = Ingredient.of(tag);
        RECIPES.add(new CrystallizerRecipe(ing, 1, acid, new ItemStack(output), BASE_TIME, productivity));
    }

    private static void addItem(ItemLike input, ItemStack output, float productivity, int duration, @Nullable FluidStack acid) {
        Ingredient ing = Ingredient.of(input);
        RECIPES.add(new CrystallizerRecipe(ing, 1, acid, output, duration, productivity));
    }

    @SuppressWarnings("unused")
    private static void addItem(Item input, Item output, float productivity, int duration, @Nullable FluidStack acid) {
        addItem(input, new ItemStack(output), productivity, duration, acid);
    }
}
