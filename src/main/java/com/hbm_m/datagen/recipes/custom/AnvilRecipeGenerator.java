package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipe.AnvilIngredient;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import dev.architectury.registry.registries.RegistrySupplier;

/**
 * Полный 1:1 порт рецептов наковальни из оригинального 1.7.10
 * ({@code com.hbm.inventory.recipes.anvil.AnvilRecipes}).
 *
 * <p>Структура повторяет оригинальный файл ({@code registerSmithing} /
 * {@code registerConstruction} + подсекции), чтобы диффы с оригиналом были читаемы.
 * У каждого рецепта — трейлинг-комментарий {@code // original: <файл>#<строка>}.
 *
 * <p>Семантика порта:
 * <ul>
 *   <li>Кузнечные рецепты ({@code AnvilSmithingRecipe}, два слота) идут через
 *       {@code input_a}/{@code input_b} и не попадают в GUI-список construction-рецептов
 *       ({@code withOrder} им не нужен).</li>
 *   <li>Construction-рецепты ({@code AnvilConstructionRecipe}) крафтятся из инвентаря
 *       игрока — портятся как {@code inventoryRecipe} + {@code required_items}; каждому
 *       назначается порядковый номер {@code withOrder(N)} в порядке регистрации оригинала.</li>
 *   <li>Оверлей берётся как в оригинале: из конструктора
 *       ({@code (AStack,AnvilOutput)=SMITHING}, {@code (AStack[],AnvilOutput)=CONSTRUCTION},
 *       {@code (AStack,AnvilOutput[])=RECYCLING}, {@code (AStack[],AnvilOutput[])=NONE}),
 *       если оригинал не переопределял его явным {@code .setOverlay(...)}.</li>
 *   <li>Тир — по legacy-id оригинала ({@code setTier(n)}):
 *       1=IRON, 2=STEEL, 3=OIL, 4=NUCLEAR, 5=RBMK, 6=FUSION, 7=PARTICLE,
 *       8=GERALD, 1916169=MURKY.</li>
 * </ul>
 *
 * <p>Ветка конфига оригинала — дефолтная: {@code enableExpensiveMode=false}
 * (сборочная машина — 4 вакуумные лампы), {@code enable528=false} (ветка
 * {@code if(!enable528)} с rbmk_rod/rod_mod/boiler/cooler/reactor_research ВКЛЮЧЕНА),
 * {@code enableLBSM=false} (тиры не разблокируются до 1).
 *
 * <p>Осознанно пропущены (см. отчёт генерации):
 * <ul>
 *   <li>Горячая кузня (ItemHot): цепочка ingot_steel_dusted 0..9, chainsteel,
 *       метеорит/кобальтовые клинки, meteorite_sword_reforged.</li>
 *   <li>wings_murk (тахион), flask_infusion (система фласок), переименование
 *       именным ярлыком, цианидный хлеб (AnvilSmithingCyanide/RenameRecipe).</li>
 *   <li>Рецепты, чьих предметов нет в порте: siren_track (все 20), platemetal,
 *       deco_asbestos, machine_rockmill, machine_deuterium_extractor,
 *       shell weaponsteel/saturnite, pipe_rubber, pile-rod RA226BE/PO210BE/ZR,
 *       боевые формы c9/c50 (id 16/17 оригинала; в порту вместо них нет предметов),
 *       варианты meta=1 (стёрлинг без шестерни), тостеры steel/wooden (один блок в порту).</li>
 *   <li>Рецепт deuterium_tower требует fluid-стек SOURGAS x8 — fluid-требования
 *       в AnvilRecipe не поддерживаются.</li>
 * </ul>
 */
public final class AnvilRecipeGenerator {
    private AnvilRecipeGenerator() { }

    /** Порядковый номер construction-рецепта в GUI (порядок регистрации оригинала). */
    private static int constructionOrder;

    public static void generate(Consumer<FinishedRecipe> writer) {
        constructionOrder = 0;
        registerSmithing(writer);
        registerConstruction(writer);
    }

    // =====================================================================================
    //  SMITHING (registerSmithing, AnvilRecipes.java #59-130)
    //  Двухслотовые рецепты: input_a + input_b. В GUI-список construction не попадают.
    // =====================================================================================
    private static void registerSmithing(Consumer<FinishedRecipe> writer) {
        registerAnvilUpgrades(writer);
        registerSmithingAlloys(writer);
        registerMoldRecipes(writer);
        // Пропущено осознанно:
        //  - AnvilSmithingHotRecipe: цепочка ingot_steel_dusted 1..9 + chainsteel (#76-80),
        //    метеорит (ingot/blade/sword, #82-84), кобальтовое оружие (#87-91) — в порте
        //    нет системы ItemHot (температурных предметов).
        //  - wings_murk (#93, wings_limp + particle_tachyon) и flask_infusion SHIELD (#94,
        //    gem_alexandrite + bottle_nuka) — нет соответствующих систем (тахион-крафт,
        //    фласки). Предметы в порте есть, но решением задачи рецепт исключён.
        //  - AnvilSmithingCyanideRecipe (#128) и AnvilSmithingRenameRecipe (#129) —
        //    спец-логика (яд на хлебе, переименование) не переносима датагеном.
    }

    /** Апгрейды наковален: [anvil_iron|anvil_lead] + 10 слитков -> наковальня выше (тир 1). */
    private static void registerAnvilUpgrades(Consumer<FinishedRecipe> writer) {
        Object[][] upgrades = {
                {"steel",         stack(ModBlocks.ANVIL_STEEL),         matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1)},          // original: AnvilRecipes.java#64
                {"desh",          stack(ModBlocks.ANVIL_DESH),          matStack(ModMaterials.DESH, MaterialShape.INGOT, 1)},           // original: AnvilRecipes.java#65
                {"saturnite",     stack(ModBlocks.ANVIL_SATURNITE),     matStack(ModMaterials.SATURNITE, MaterialShape.INGOT, 1)},      // original: AnvilRecipes.java#66
                {"ferrouranium",  stack(ModBlocks.ANVIL_FERROURANIUM),  matStack(ModMaterials.FERROURANIUM, MaterialShape.INGOT, 1)},   // original: AnvilRecipes.java#67
                {"bismuth_bronze",stack(ModBlocks.ANVIL_BISMUTH_BRONZE),matStack(ModMaterials.BISMUTH_BRONZE, MaterialShape.INGOT, 1)}, // original: AnvilRecipes.java#68
                {"arsenic_bronze",stack(ModBlocks.ANVIL_ARSENIC_BRONZE),matStack(ModMaterials.ARSENIC_BRONZE, MaterialShape.INGOT, 1)}, // original: AnvilRecipes.java#69
                {"schrabidate",   stack(ModBlocks.ANVIL_SCHRABIDATE),   matStack(ModMaterials.SCHRABIDATE, MaterialShape.INGOT, 1)},    // original: AnvilRecipes.java#70
                {"dnt",           stack(ModBlocks.ANVIL_DNT),           matStack(ModMaterials.DINEUTRONIUM, MaterialShape.INGOT, 1)},   // original: AnvilRecipes.java#71
                {"osmiridium",    stack(ModBlocks.ANVIL_OSMIRIDIUM),    matStack(ModMaterials.OSMIRIDIUM, MaterialShape.INGOT, 1)},     // original: AnvilRecipes.java#72
        };
        for (Object[] upgrade : upgrades) {
            String name = (String) upgrade[0];
            ItemStack output = (ItemStack) upgrade[1];
            ItemStack material = (ItemStack) upgrade[2];
            for (RegistrySupplier<Block> base : List.of(ModBlocks.ANVIL_IRON, ModBlocks.ANVIL_LEAD)) {
                String baseName = base == ModBlocks.ANVIL_IRON ? "iron" : "lead";
                AnvilRecipeBuilder.anvilRecipe(
                                AnvilIngredient.of(stack(base)),
                                AnvilIngredient.ofCount(material, 10),
                                output,
                                AnvilTier.IRON)
                        .withOverlay(AnvilRecipe.OverlayType.SMITHING)
                        .save(writer, anvilId("iron", "smithing", "anvil_" + name + "_from_" + baseName));
            }
        }
    }

    /** Пушечная бронза: медный слиток + алюминиевый слиток (тир 1). */
    private static void registerSmithingAlloys(Consumer<FinishedRecipe> writer) {
        AnvilRecipeBuilder.anvilRecipe(
                        AnvilIngredient.of(new ItemStack(Items.COPPER_INGOT)),
                        aluminiumIngot(),
                        matStack(ModMaterials.GUNMETAL, MaterialShape.INGOT, 1),
                        AnvilTier.IRON)
                .withOverlay(AnvilRecipe.OverlayType.SMITHING)
                .save(writer, anvilId("iron", "smithing", "gunmetal_ingot")); // original: AnvilRecipes.java#96
    }

    // =====================================================================================
    //  Формы (AnvilSmithingMold #98-126 + registerConstructionAmmo #610-627)
    //  Кузнечные формы: слева материал (НЕ расходуется, keepInputA), справа mold_base
    //  (расходуется), результат — форма. Оригинал матчил по ore-dict префиксу формы;
    //  в порту — конкретный предмет-представитель (для ванильных металлов — ваниль).
    //  Все кузнечные формы — тир 1. Оружейные формы id 22-28 — construction
    //  (mold_base + сталь x4, тир 2). Казённые формы c9/c50 (id 16/17 оригинала)
    //  не переносятся — в порту нет форм C9/C50, а порт-онли C357/CBUCKSHOT в
    //  оригинале отсутствуют.
    // =====================================================================================
    private static void registerMoldRecipes(Consumer<FinishedRecipe> writer) {

        /* ── Кузнечные формы (AnvilSmithingMold, тир 1, SMITHING) ── */
        // id 0:  nugget       <- GOLD.nugget()
        mold(writer, "nugget", AnvilIngredient.of(new ItemStack(Items.GOLD_NUGGET)), ModItems.MOLD_NUGGET);            // original: AnvilRecipes.java#98
        // id 1:  billet       <- U.billet()
        mold(writer, "billet", AnvilIngredient.of(matStack(ModMaterials.URANIUM, MaterialShape.BILLET, 1)), ModItems.MOLD_BILLET); // original: AnvilRecipes.java#99
        // id 2:  ingot        <- IRON.ingot() (любой железный слиток; в порту только ваниль)
        mold(writer, "ingot", AnvilIngredient.of(new ItemStack(Items.IRON_INGOT)), ModItems.MOLD_INGOT);               // original: AnvilRecipes.java#100
        // id 3:  plate        <- IRON.plate()
        mold(writer, "plate", AnvilIngredient.of(matStack(ModMaterials.IRON, MaterialShape.PLATE, 1)), ModItems.MOLD_PLATE); // original: AnvilRecipes.java#101
        // id 19: plateTriple  <- IRON.plateCast()
        mold(writer, "plate_cast", AnvilIngredient.of(matStack(ModMaterials.IRON, MaterialShape.PLATE_CAST, 1)), ModItems.MOLD_PLATE_CAST); // original: AnvilRecipes.java#102
        // id 13: plateTriple  x3
        mold(writer, "plates_cast", AnvilIngredient.ofCount(matStack(ModMaterials.IRON, MaterialShape.PLATE_CAST, 1), 3), ModItems.MOLD_PLATES_CAST); // original: AnvilRecipes.java#103
        // id 4:  wireFine     <- CU.wireFine()
        mold(writer, "wire", AnvilIngredient.of(matStack(ModMaterials.COPPER, MaterialShape.WIRE, 1)), ModItems.MOLD_WIRE); // original: AnvilRecipes.java#104
        // id 5:  blade        <- blade_titanium (также blade_tungsten)
        mold(writer, "blade", AnvilIngredient.of(stack(ModItems.BLADE_TITANIUM), stack(ModItems.BLADE_TUNGSTEN)), ModItems.MOLD_BLADE); // original: AnvilRecipes.java#105
        // id 6:  blades       <- blades_steel (также blades_titanium)
        mold(writer, "blades", AnvilIngredient.of(stack(ModItems.BLADES_STEEL), stack(ModItems.BLADES_TITANIUM)), ModItems.MOLD_BLADES); // original: AnvilRecipes.java#109
        // id 7:  stamp        <- stamp_iron_flat (также stone/steel/titanium/obsidian)
        mold(writer, "stamp", AnvilIngredient.of(
                stack(ModItems.STAMP_STONE_FLAT),
                stack(ModItems.STAMP_IRON_FLAT),
                stack(ModItems.STAMP_STEEL_FLAT),
                stack(ModItems.STAMP_TITANIUM_FLAT),
                stack(ModItems.STAMP_OBSIDIAN_FLAT)), ModItems.MOLD_STAMP);                                            // original: AnvilRecipes.java#113
        // id 8:  shell        <- STEEL.shell()
        mold(writer, "shell", AnvilIngredient.of(stack(ModItems.SHELL_STEEL)), ModItems.MOLD_SHELL);                   // original: AnvilRecipes.java#120
        // id 9:  pipe         <- STEEL.pipe()
        mold(writer, "pipe", AnvilIngredient.of(stack(ModItems.PIPE_STEEL)), ModItems.MOLD_PIPE);                      // original: AnvilRecipes.java#121
        // id 10: ingot        x9
        mold(writer, "ingots", AnvilIngredient.ofCount(new ItemStack(Items.IRON_INGOT), 9), ModItems.MOLD_INGOTS);      // original: AnvilRecipes.java#122
        // id 11: plate        x9
        mold(writer, "plates", AnvilIngredient.ofCount(matStack(ModMaterials.IRON, MaterialShape.PLATE, 1), 9), ModItems.MOLD_PLATES); // original: AnvilRecipes.java#123
        // id 12: block        <- IRON.block()
        mold(writer, "block", AnvilIngredient.of(new ItemStack(Items.IRON_BLOCK)), ModItems.MOLD_BLOCK);               // original: AnvilRecipes.java#124
        // id 20: wireDense    x1 <- MINGRADE.wireDense()
        mold(writer, "wire_dense", AnvilIngredient.of(matStack(ModMaterials.RED_COPPER, MaterialShape.WIRE_DENSE, 1)), ModItems.MOLD_WIRE_DENSE); // original: AnvilRecipes.java#125
        // id 21: wireDense    x9
        mold(writer, "wires_dense", AnvilIngredient.ofCount(matStack(ModMaterials.RED_COPPER, MaterialShape.WIRE_DENSE, 1), 9), ModItems.MOLD_WIRES_DENSE); // original: AnvilRecipes.java#126

        /* ── Оружейные формы (registerConstructionAmmo, mold_base + сталь x4, тир 2) ──
         * Регистрируются в registerConstructionAmmo (см. ниже) — их withOrder должен
         * продолжать construction-список оригинала, а не smithing-секцию.
         * Казённые формы c9/c50 (id 16/17) пропущены — в порту нет форм C9/C50,
         * а его C357/CBUCKSHOT в оригинале отсутствуют.
         */
    }

    /** Кузнечная форма: материал + mold_base -> форма, тир 1, материал не расходуется. */
    private static void mold(Consumer<FinishedRecipe> writer, String name, AnvilIngredient material, RegistrySupplier<Item> moldItem) {
        AnvilRecipeBuilder.anvilRecipe(
                        material,
                        AnvilIngredient.of(stack(ModItems.MOLD_BASE)),
                        stack(moldItem),
                        AnvilTier.IRON)
                .keepInputA() // материал остаётся (оригинальный AnvilSmithingMold.amountConsumed)
                .withOverlay(AnvilRecipe.OverlayType.SMITHING)
                .save(writer, anvilId("iron", "mold", name));
    }

    /** Конструкционная форма: mold_base + сталь x4, тир 2, CONSTRUCTION. */
    private static void constructionMold(Consumer<FinishedRecipe> writer, String name, RegistrySupplier<Item> moldItem) {
        AnvilRecipeBuilder.inventoryRecipe(stack(moldItem), AnvilTier.STEEL)
                .withOrder(nextOrder())
                .withOverlay(AnvilRecipe.OverlayType.CONSTRUCTION)
                .addInventoryRequirement(stack(ModItems.MOLD_BASE))
                .addInventoryRequirement(AnvilIngredient.ofCount(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1), 4))
                .save(writer, anvilId("steel", "mold", name));
    }

    // =====================================================================================
    //  CONSTRUCTION (registerConstruction, AnvilRecipes.java #139-1006)
    //  Крафт из инвентаря игрока: inventoryRecipe + required_items, порядок = оригинал.
    // =====================================================================================
    private static void registerConstruction(Consumer<FinishedRecipe> writer) {
        registerPlates(writer);
        registerWires(writer);
        registerDustCompression(writer);
        registerConstructionRecipes(writer);
        registerConstructionStamps(writer);
        registerConstructionAmmo(writer);
        // registerConstructionSirens (ориг. #629-633): в порте нет предмета siren_track —
        // все 20 рецептов треков сирены пропущены.
        // registerConstructionUpgrades (ориг. #635): пусто в оригинале.
        registerConstructionRecycling(writer);
    }

    /** Плашка: слиток -> плита (тир 3, SMITHING — оверлей конструктора 1:1). */
    private static void registerPlates(Consumer<FinishedRecipe> writer) {
        plate(writer, "iron",         new ItemStack(Items.IRON_INGOT),                                        matStack(ModMaterials.IRON, MaterialShape.PLATE, 1));          // original: AnvilRecipes.java#141
        plate(writer, "gold",         new ItemStack(Items.GOLD_INGOT),                                        matStack(ModMaterials.GOLD, MaterialShape.PLATE, 1));          // original: AnvilRecipes.java#142
        plate(writer, "titanium",     matStack(ModMaterials.TITANIUM, MaterialShape.INGOT, 1),                matStack(ModMaterials.TITANIUM, MaterialShape.PLATE, 1));      // original: AnvilRecipes.java#143
        plate(writer, "aluminium",    null,                                                                   matStack(ModMaterials.ALUMINUM, MaterialShape.PLATE, 1));      // original: AnvilRecipes.java#144 (AL.ingot — оба порт-предмета)
        plate(writer, "steel",        matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1),                   matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1));         // original: AnvilRecipes.java#145
        plate(writer, "lead",         matStack(ModMaterials.LEAD, MaterialShape.INGOT, 1),                    matStack(ModMaterials.LEAD, MaterialShape.PLATE, 1));          // original: AnvilRecipes.java#146
        plate(writer, "copper",       new ItemStack(Items.COPPER_INGOT),                                      matStack(ModMaterials.COPPER, MaterialShape.PLATE, 1));        // original: AnvilRecipes.java#147
        plate(writer, "gunmetal",     matStack(ModMaterials.GUNMETAL, MaterialShape.INGOT, 1),                matStack(ModMaterials.GUNMETAL, MaterialShape.PLATE, 1));      // original: AnvilRecipes.java#148
        plate(writer, "weaponsteel",  matStack(ModMaterials.WEAPONSTEEL, MaterialShape.INGOT, 1),             matStack(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE, 1));   // original: AnvilRecipes.java#149
        plate(writer, "saturnite",    matStack(ModMaterials.SATURNITE, MaterialShape.INGOT, 1),               matStack(ModMaterials.SATURNITE, MaterialShape.PLATE, 1));     // original: AnvilRecipes.java#150
        plate(writer, "dura_steel",   matStack(ModMaterials.DURA_STEEL, MaterialShape.INGOT, 1),              matStack(ModMaterials.DURA_STEEL, MaterialShape.PLATE, 1));    // original: AnvilRecipes.java#151
        plate(writer, "schrabidium",  matStack(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT, 1),             matStack(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE, 1));   // original: AnvilRecipes.java#152
        plate(writer, "combine_steel",matStack(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT, 1),           matStack(ModMaterials.COMBINE_STEEL, MaterialShape.PLATE, 1)); // original: AnvilRecipes.java#153
    }

    private static void plate(Consumer<FinishedRecipe> writer, String name, ItemStack ingot, ItemStack plate) {
        AnvilIngredient input = ingot != null ? AnvilIngredient.of(ingot) : aluminiumIngot();
        AnvilRecipeBuilder.inventoryRecipe(plate, AnvilTier.OIL)
                .withOrder(nextOrder())
                .withOverlay(AnvilRecipe.OverlayType.SMITHING) // конструктор (AStack, AnvilOutput) = SMITHING
                .addInventoryRequirement(input)
                .save(writer, anvilId("oil", "craft", "plate_" + name));
    }

    /**
     * Автоген тонкой проволоки (ориг. #155-159): для материалов с WIRE-автогеном и
     * существующим слитком: слиток -> wire_fine x8, тир 4. Состав и порядок списка
     * повторяют {@code Mats.orderedList} оригинала (CARBON, GOLD, SCHRABIDIUM, COPPER,
     * TUNGSTEN, ALUMINIUM, LEAD, ZIRCONIUM, STEEL, MINGRADE, MAGTUNG); углерод берёт
     * слиток графита (ориг. CARBON.ingot(ingot_graphite)).
     */
    private static void registerWires(Consumer<FinishedRecipe> writer) {
        wire(writer, "carbon",              ModMaterials.CARBON);             // original: AnvilRecipes.java#155-159 (MAT_CARBON, слиток = графит)
        wire(writer, "gold",                ModMaterials.GOLD);               // original: AnvilRecipes.java#155-159 (MAT_GOLD, ваниль)
        wire(writer, "schrabidium",         ModMaterials.SCHRABIDIUM);        // original: AnvilRecipes.java#155-159 (MAT_SCHRABIDIUM)
        wire(writer, "copper",              ModMaterials.COPPER);             // original: AnvilRecipes.java#155-159 (MAT_COPPER, ваниль)
        wire(writer, "tungsten",            ModMaterials.TUNGSTEN);           // original: AnvilRecipes.java#155-159 (MAT_TUNGSTEN)
        wire(writer, "aluminium",           ModMaterials.ALUMINIUM);          // original: AnvilRecipes.java#155-159 (MAT_ALUMINIUM)
        wire(writer, "lead",                ModMaterials.LEAD);               // original: AnvilRecipes.java#155-159 (MAT_LEAD)
        wire(writer, "zirconium",           ModMaterials.ZIRCONIUM);          // original: AnvilRecipes.java#155-159 (MAT_ZIRCONIUM)
        wire(writer, "steel",               ModMaterials.STEEL);              // original: AnvilRecipes.java#155-159 (MAT_STEEL)
        wire(writer, "red_copper",          ModMaterials.RED_COPPER);         // original: AnvilRecipes.java#155-159 (MAT_MINGRADE)
        wire(writer, "magnetized_tungsten", ModMaterials.MAGNETIZED_TUNGSTEN);// original: AnvilRecipes.java#155-159 (MAT_MAGTUNG)
    }

    private static void wire(Consumer<FinishedRecipe> writer, String name, ModMaterials mat) {
        ItemStack wire = matStackOrNull(mat, MaterialShape.WIRE, 1);
        AnvilIngredient ingot = fineWireIngot(mat);
        if (wire == null || ingot == null) {
            reportSkip("wire autogen (" + mat + "): нет проволоки/слитка");
            return;
        }
        AnvilRecipeBuilder.inventoryRecipe(new ItemStack(wire.getItem(), 8), AnvilTier.NUCLEAR)
                .withOrder(nextOrder())
                .withOverlay(AnvilRecipe.OverlayType.SMITHING)
                .addInventoryRequirement(ingot)
                .save(writer, anvilId("nuclear", "craft", "wire_" + name));
    }

    /** Слиток для автогена проволоки (ориг. условие doesOreNameExist(INGOT)); null = нет. */
    private static AnvilIngredient fineWireIngot(ModMaterials mat) {
        switch (mat) {
            case CARBON:     return AnvilIngredient.of(matStack(ModMaterials.GRAPHITE, MaterialShape.INGOT, 1)); // ориг. CARBON.ingot(ingot_graphite)
            case GOLD:       return AnvilIngredient.of(new ItemStack(Items.GOLD_INGOT));
            case COPPER:     return AnvilIngredient.of(new ItemStack(Items.COPPER_INGOT));
            case ALUMINIUM:  return aluminiumIngot();
            default:         return AnvilIngredient.of(matStack(mat, MaterialShape.INGOT, 1));
        }
    }

    /** Сжатие пыли в ванильные предметы (ориг. #161-165, тир 3, SMITHING). */
    private static void registerDustCompression(Consumer<FinishedRecipe> writer) {
        dust(writer, "coal",    matStack(ModMaterials.COAL, MaterialShape.POWDER, 1),    new ItemStack(Items.COAL));         // original: AnvilRecipes.java#161
        dust(writer, "quartz",  matStack(ModMaterials.QUARTZ, MaterialShape.POWDER, 1),  new ItemStack(Items.QUARTZ));       // original: AnvilRecipes.java#162
        dust(writer, "lapis",   matStack(ModMaterials.LAPIS, MaterialShape.POWDER, 1),   new ItemStack(Items.LAPIS_LAZULI)); // original: AnvilRecipes.java#163
        dust(writer, "diamond", matStack(ModMaterials.DIAMOND, MaterialShape.POWDER, 1), new ItemStack(Items.DIAMOND));      // original: AnvilRecipes.java#164
        dust(writer, "emerald", matStack(ModMaterials.EMERALD, MaterialShape.POWDER, 1), new ItemStack(Items.EMERALD));      // original: AnvilRecipes.java#165
    }

    private static void dust(Consumer<FinishedRecipe> writer, String name, ItemStack dust, ItemStack gem) {
        AnvilRecipeBuilder.inventoryRecipe(gem, AnvilTier.OIL)
                .withOrder(nextOrder())
                .withOverlay(AnvilRecipe.OverlayType.SMITHING)
                .addInventoryRequirement(dust)
                .save(writer, anvilId("oil", "craft", name));
    }

    /** Ориг. registerConstructionRecipes (#175-580). */
    private static void registerConstructionRecipes(Consumer<FinishedRecipe> writer) {

        // Аннигилятор (ориг. #177, тир 2)
        construction(writer, "steel", "annihilator", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.ANNIHILATOR),
                AnvilIngredient.ofCount(new ItemStack(Items.STONE_BRICKS), 16),
                AnvilIngredient.ofCount(stack(ModItems.FIREBRICK), 16),
                AnvilIngredient.ofCount(new ItemStack(Items.IRON_INGOT), 8),
                AnvilIngredient.ofCount(new ItemStack(Items.COPPER_INGOT), 8)); // original: AnvilRecipes.java#177-179

        // Деко-блоки (ориг. #181-188, тир 1, явный .setOverlay(CONSTRUCTION)); platemetal пропущен — нет блока
        deco(writer, "aluminium",  aluminiumIngot(), stack(ModBlocks.DECO_ALUMINIUM));   // original: AnvilRecipes.java#182
        deco(writer, "beryllium",  ing(matStack(ModMaterials.BERYLLIUM, MaterialShape.INGOT, 1)), stack(ModBlocks.DECO_BERYLLIUM)); // original: AnvilRecipes.java#183
        deco(writer, "lead",       ing(matStack(ModMaterials.LEAD, MaterialShape.INGOT, 1)), stack(ModBlocks.DECO_LEAD));           // original: AnvilRecipes.java#184
        deco(writer, "red_copper", ing(matStack(ModMaterials.RED_COPPER, MaterialShape.INGOT, 1)), stack(ModBlocks.DECO_RED_COPPER)); // original: AnvilRecipes.java#185
        deco(writer, "steel",      ing(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1)), stack(ModBlocks.DECO_STEEL));           // original: AnvilRecipes.java#186
        deco(writer, "titanium",   ing(matStack(ModMaterials.TITANIUM, MaterialShape.INGOT, 1)), stack(ModBlocks.DECO_TITANIUM));     // original: AnvilRecipes.java#187
        deco(writer, "tungsten",   ing(matStack(ModMaterials.TUNGSTEN, MaterialShape.INGOT, 1)), stack(ModBlocks.DECO_TUNGSTEN));       // original: AnvilRecipes.java#188

        // Глубинный ДНТ-кирпич (ориг. #190-192, тир 1916169)
        construction(writer, "murky", "depth_dnt", AnvilTier.MURKY, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.DEPTH_DNT),
                AnvilIngredient.ofCount(matStack(ModMaterials.DINEUTRONIUM, MaterialShape.INGOT, 1), 4),
                AnvilIngredient.of(stack(ModBlocks.DEPTH_BRICK))); // original: AnvilRecipes.java#190-192

        // Автоген корпусов (ориг. #194-196: плита x4 -> shell, тир 1). У weaponsteel и
        // saturnite предметов shell в порте нет — пропущены (см. reportSkip).
        shell(writer, "titanium", ModItems.SHELL_TITANIUM,  matStack(ModMaterials.TITANIUM, MaterialShape.PLATE, 1));     // original: AnvilRecipes.java#194-196 (MAT_TITANIUM)
        shell(writer, "copper",   ModItems.SHELL_COPPER,    matStack(ModMaterials.COPPER, MaterialShape.PLATE, 1));       // original: AnvilRecipes.java#194-196 (MAT_COPPER)
        shell(writer, "aluminium",ModItems.SHELL_ALUMINUM,  matStack(ModMaterials.ALUMINUM, MaterialShape.PLATE, 1));     // original: AnvilRecipes.java#194-196 (MAT_ALUMINIUM)
        shell(writer, "steel",    ModItems.SHELL_STEEL,     matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1));        // original: AnvilRecipes.java#194-196 (MAT_STEEL)
        // MAT_WEAPONSTEEL / MAT_SATURN: предметов part shell нет в ModItems — SKIP.

        // Автоген труб (ориг. #197-203: плита-или-слиток x3 -> pipe, тир 1); pipe_rubber в порту нет — SKIP
        pipe(writer, "iron",      ModItems.PIPE_IRON,       ingCount(matStack(ModMaterials.IRON, MaterialShape.PLATE, 1), 3));     // original: AnvilRecipes.java#197-203 (MAT_IRON, plate есть)
        pipe(writer, "copper",    ModItems.PIPE_COPPER,     ingCount(matStack(ModMaterials.COPPER, MaterialShape.PLATE, 1), 3));   // original: AnvilRecipes.java#197-203 (MAT_COPPER)
        pipe(writer, "aluminium", ModItems.PIPE_ALUMINUM,   ingCount(matStack(ModMaterials.ALUMINUM, MaterialShape.PLATE, 1), 3)); // original: AnvilRecipes.java#197-203 (MAT_ALUMINIUM)
        pipe(writer, "lead",      ModItems.PIPE_LEAD,       ingCount(matStack(ModMaterials.LEAD, MaterialShape.PLATE, 1), 3));     // original: AnvilRecipes.java#197-203 (MAT_LEAD)
        pipe(writer, "steel",     ModItems.PIPE_STEEL,      ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 3));    // original: AnvilRecipes.java#197-203 (MAT_STEEL)
        pipe(writer, "dura_steel",ModItems.PIPE_DURA_STEEL, ingCount(matStack(ModMaterials.DURA_STEEL, MaterialShape.PLATE, 1), 3)); // original: AnvilRecipes.java#197-203 (MAT_DURA)
        // MAT_RUBBER: только слиток; предмета pipe_rubber в порту нет — SKIP.

        // Катушки-торы (ориг. #205-210, тир 1, явный CONSTRUCTION)
        construction(writer, "iron", "coil_copper_torus", AnvilTier.IRON, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.COIL_COPPER_TORUS),
                ingCount(stack(ModItems.COIL_COPPER), 2)); // original: AnvilRecipes.java#205-207
        construction(writer, "iron", "coil_gold_torus", AnvilTier.IRON, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.COIL_GOLD_TORUS),
                ingCount(stack(ModItems.COIL_GOLD), 2)); // original: AnvilRecipes.java#208-210

        // Моторы (ориг. #212-217)
        construction(writer, "iron", "motor", AnvilTier.IRON, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.MOTOR, 2),
                ingCount(matStack(ModMaterials.IRON, MaterialShape.PLATE, 1), 2),
                ing(stack(ModItems.COIL_COPPER)),
                ing(stack(ModItems.COIL_COPPER_TORUS))); // original: AnvilRecipes.java#212-214
        construction(writer, "oil", "motor_desh", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.MOTOR_DESH),
                anyOf(2, matStack(ModMaterials.POLYMER, MaterialShape.INGOT, 1), matStack(ModMaterials.BAKELITE, MaterialShape.INGOT, 1)), // ANY_PLASTIC.ingot
                ingCount(matStack(ModMaterials.DESH, MaterialShape.INGOT, 1), 2),
                ingCount(matStack(ModMaterials.GOLD, MaterialShape.WIRE_DENSE, 1), 1)); // original: AnvilRecipes.java#215-217

        // Печь (ориг. #219-225, тир 1)
        construction(writer, "iron", "blast_furnace", AnvilTier.IRON, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.MACHINE_BLAST_FURNACE),
                AnvilIngredient.ofCount(new ItemStack(Items.STONE_BRICKS), 4),
                AnvilIngredient.ofCount(stack(ModItems.FIREBRICK), 32),
                ingCount(matStack(ModMaterials.COPPER, MaterialShape.PLATE, 1), 8)); // original: AnvilRecipes.java#219-225

        // machine_rockmill (ориг. #229-235) — блока rockmill в порте нет, SKIP.

        // Сборочная машина (ориг. #237-243, тир 2; дефолт enableExpensiveMode=false -> 4 лампы)
        construction(writer, "steel", "assembly_machine", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.MACHINE_ASSEMBLER), // ориг. machine_assembly_machine (порт: machine_assembler)
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1), 8),
                ingCount(matStack(ModMaterials.COPPER, MaterialShape.PLATE, 1), 4),
                ingCount(stack(ModItems.MOTOR), 2),
                ingCount(stack(ModItems.VACUUM_TUBE), 4)); // original: AnvilRecipes.java#237-243

        // Насосы (ориг. #245-260)
        construction(writer, "steel", "pump_steam", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.PUMP_STEAM),
                AnvilIngredient.ofCount(new ItemStack(Items.COBBLESTONE), 8),   // KEY_COBBLESTONE
                AnvilIngredient.ofCount(new ItemStack(Items.OAK_PLANKS), 16),   // KEY_PLANKS (представитель)
                ingCount(matStack(ModMaterials.COPPER, MaterialShape.PLATE, 1), 8),
                ingCount(stack(ModItems.PIPE_LEAD), 2)); // original: AnvilRecipes.java#245-251
        construction(writer, "oil", "pump_electric", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.PUMP_ELECTRIC),
                AnvilIngredient.ofCount(new ItemStack(Items.STONE_BRICKS), 8),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 16),
                ingCount(stack(ModItems.PIPE_LEAD), 4),
                ingCount(stack(ModItems.MOTOR), 2),
                ingCount(stack(ModItems.VACUUM_TUBE), 4)); // original: AnvilRecipes.java#253-260

        // Нагреватели (ориг. #262-306)
        construction(writer, "steel", "firebox", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.FIREBOX),
                AnvilIngredient.ofCount(new ItemStack(Items.FURNACE), 1),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 8),
                ingCount(new ItemStack(Items.COPPER_INGOT), 8)); // original: AnvilRecipes.java#262-267
        construction(writer, "steel", "heating_oven", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.HEATING_OVEN), // ориг. heater_oven
                ingCount(stack(ModItems.FIREBRICK), 16),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 4),
                ingCount(new ItemStack(Items.COPPER_INGOT), 8)); // original: AnvilRecipes.java#269-274
        construction(writer, "steel", "ashpit", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.ASHPIT),
                AnvilIngredient.ofCount(new ItemStack(Items.STONE), 8),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 2),
                ingCount(new ItemStack(Items.IRON_INGOT), 4)); // original: AnvilRecipes.java#276-281
        construction(writer, "steel", "oilburner", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.OILBURNER),
                ingCount(stack(ModItems.TANK_STEEL), 4),
                ingCount(stack(ModItems.PIPE_STEEL), 3),
                ingCount(matStack(ModMaterials.TITANIUM, MaterialShape.INGOT, 1), 12),
                ingCount(new ItemStack(Items.COPPER_INGOT), 8)); // original: AnvilRecipes.java#283-289
        construction(writer, "oil", "electric_heater", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.ELECTRIC_HEATER),
                anyOf(4, matStack(ModMaterials.POLYMER, MaterialShape.INGOT, 1), matStack(ModMaterials.BAKELITE, MaterialShape.INGOT, 1)),
                ingCount(new ItemStack(Items.COPPER_INGOT), 8),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 8),
                ingCount(stack(ModItems.COIL_TUNGSTEN), 8),
                ing(stack(ModItems.CIRCUIT))); // original: AnvilRecipes.java#291-298 (circuit BASIC)
        construction(writer, "oil", "heatex", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.HEATEX),
                ingCount(matStack(ModMaterials.RUBBER, MaterialShape.INGOT, 1), 4),
                ingCount(new ItemStack(Items.COPPER_INGOT), 16),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 16),
                ingCount(stack(ModItems.PIPE_STEEL), 3)); // original: AnvilRecipes.java#300-306

        // Прочие печи (ориг. #308-331)
        construction(writer, "steel", "furnace_steel", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.FURNACE_STEEL),
                AnvilIngredient.ofCount(new ItemStack(Items.STONE_BRICKS), 16),
                ingCount(new ItemStack(Items.IRON_INGOT), 4),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 16),
                ingCount(new ItemStack(Items.COPPER_INGOT), 8),
                ingCount(stack(ModBlocks.STEEL_GRATE), 16)); // original: AnvilRecipes.java#308-315
        construction(writer, "steel", "combination_oven", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.COMBINATION_OVEN),
                AnvilIngredient.ofCount(new ItemStack(Items.STONE_BRICKS), 8),
                AnvilIngredient.ofCount(new ItemStack(Items.OAK_LOG), 16),      // KEY_LOG (представитель)
                ingCount(matStack(ModMaterials.COPPER, MaterialShape.PLATE_CAST, 1), 2),
                AnvilIngredient.ofCount(new ItemStack(Items.BRICK), 16));       // KEY_BRICK = ingotBrick
        // original: AnvilRecipes.java#317-323
        construction(writer, "steel", "rotary_furnace", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.ROTARY_FURNACE),
                AnvilIngredient.ofCount(new ItemStack(Items.STONE_BRICKS), 8),
                ingCount(stack(ModItems.FIREBRICK), 16),
                ingCount(new ItemStack(Items.IRON_INGOT), 4),
                ingCount(matStack(ModMaterials.COPPER, MaterialShape.PLATE, 1), 8)); // original: AnvilRecipes.java#325-331

        // Двигатель Стирлинга (ориг. #333-349); gear_large meta 0/1 в порту единый предмет
        construction(writer, "steel", "stirling", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.STIRLING),
                AnvilIngredient.ofCount(new ItemStack(Items.OAK_PLANKS), 16),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 6),
                ingCount(new ItemStack(Items.COPPER_INGOT), 8),
                ingCount(stack(ModItems.COIL_COPPER), 4),
                ing(stack(ModItems.GEAR_LARGE))); // original: AnvilRecipes.java#333-340 (gear meta 0)
        construction(writer, "steel", "stirling_steel", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.STIRLING_STEEL),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 16),
                ingCount(matStack(ModMaterials.BERYLLIUM, MaterialShape.INGOT, 1), 6),
                ingCount(new ItemStack(Items.COPPER_INGOT), 4),
                ingCount(stack(ModItems.COIL_GOLD), 8),
                ing(stack(ModItems.GEAR_LARGE))); // original: AnvilRecipes.java#342-349 (gear meta 1)

        // Паровая машина, пилорама (ориг. #351-367)
        construction(writer, "steel", "steam_engine", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.STEAM_ENGINE),
                ingCount(stack(ModBlocks.REINFORCED_STONE), 16),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 12),
                ingCount(stack(ModItems.SHELL_STEEL), 2),
                ingCount(stack(ModItems.COIL_COPPER), 4),
                ing(stack(ModItems.GEAR_LARGE))); // original: AnvilRecipes.java#351-358
        construction(writer, "steel", "sawmill", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.SAWMILL),
                AnvilIngredient.ofCount(new ItemStack(Items.OAK_PLANKS), 16),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 6),
                ingCount(new ItemStack(Items.COPPER_INGOT), 8),
                ingCount(new ItemStack(Items.IRON_INGOT), 4),
                ing(stack(ModItems.SAWBLADE))); // original: AnvilRecipes.java#360-367

        // Тигель, котёл (ориг. #369-381)
        construction(writer, "steel", "crucible", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.CRUCIBLE),
                ingCount(stack(ModItems.FIREBRICK), 20),
                ingCount(new ItemStack(Items.COPPER_INGOT), 8),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 8)); // original: AnvilRecipes.java#369-374
        construction(writer, "steel", "boiler", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.BOILER),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1), 4),
                ingCount(matStack(ModMaterials.COPPER, MaterialShape.PLATE, 1), 16),
                ingCount(matStack(ModMaterials.POLYMER, MaterialShape.PLATE, 1), 8)); // original: AnvilRecipes.java#376-381

        // Паяльная станция, сварщик (ориг. #383-397)
        construction(writer, "steel", "soldering_station", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.SOLDERING_STATION),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE_CAST, 1), 2),
                ingCount(stack(ModItems.COIL_COPPER), 4),
                ingCount(stack(ModItems.BOLT_TUNGSTEN), 4),
                ingCount(stack(ModItems.VACUUM_TUBE), 2)); // original: AnvilRecipes.java#383-389
        construction(writer, "steel", "arc_welder", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.ARC_WELDER),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE_CAST, 1), 4),
                ingCount(matStack(ModMaterials.TUNGSTEN, MaterialShape.INGOT, 1), 8),
                ing(stack(ModBlocks.MACHINE_TRANSFORMER)),
                ingCount(stack(ModItems.ARC_ELECTRODE), 2)); // original: AnvilRecipes.java#391-397

        // Промышленный котёл, автопила, молотилка (ориг. #399-421)
        construction(writer, "oil", "industrial_boiler", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.INDUSTRIAL_BOILER),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE_CAST, 1), 8),
                ingCount(new ItemStack(Items.COPPER_INGOT), 8),
                anyOf(4, matStack(ModMaterials.POLYMER, MaterialShape.INGOT, 1), matStack(ModMaterials.BAKELITE, MaterialShape.INGOT, 1))); // original: AnvilRecipes.java#399-404
        construction(writer, "steel", "autosaw", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.AUTOSAW),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 4),
                ingCount(new ItemStack(Items.IRON_INGOT), 12),
                ingCount(new ItemStack(Items.COPPER_INGOT), 2),
                ingCount(stack(ModItems.VACUUM_TUBE), 2),
                ing(stack(ModItems.SAWBLADE))); // original: AnvilRecipes.java#406-413
        construction(writer, "steel", "thresher", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.THRESHER),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 8),
                ingCount(new ItemStack(Items.IRON_INGOT), 12),
                ingCount(new ItemStack(Items.COPPER_INGOT), 2),
                ingCount(stack(ModItems.VACUUM_TUBE), 1)); // original: AnvilRecipes.java#415-421

        // Градирни (ориг. #434-446; machine_condenser -> порт steam_condenser)
        construction(writer, "oil", "tower_small", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.TOWER_SMALL),
                ingCount(stack(ModBlocks.BRICK_CONCRETE), 64),
                AnvilIngredient.ofCount(new ItemStack(Items.IRON_BARS), 128),
                ingCount(stack(ModBlocks.STEAM_CONDENSER), 4)); // original: AnvilRecipes.java#434-439
        construction(writer, "nuclear", "cooling_tower", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.COOLING_TOWER), // ориг. machine_tower_large
                ingCount(stack(ModBlocks.CONCRETE), 128), // ориг. concrete_smooth; в порту один блок concrete
                ingCount(stack(ModBlocks.STEEL_SCAFFOLD), 32),
                ingCount(stack(ModBlocks.STEAM_CONDENSER), 16),
                ingCount(stack(ModItems.PIPE_STEEL), 8)); // original: AnvilRecipes.java#440-446

        // Крылья (ориг. #448-453)
        construction(writer, "steel", "wings_limp", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.WINGS_LIMP),
                AnvilIngredient.ofCount(new ItemStack(Items.BONE), 16),
                AnvilIngredient.ofCount(new ItemStack(Items.LEATHER), 4),
                AnvilIngredient.ofCount(new ItemStack(Items.FEATHER), 24)); // original: AnvilRecipes.java#448-453

        // Депьютеризатор и башня (ориг. #455-473) — extractor: блока в порте нет, SKIP;
        // tower: требует fluid-стек SOURGAS x8 — fluid-требования не поддерживаются, SKIP.

        // Пилоны и подстанция (ориг. #475-491; ANY_CONCRETE = concrete/asbestos/ducrete)
        construction(writer, "steel", "red_pylon_large", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.RED_PYLON_LARGE),
                anyOf(2, stack(ModBlocks.CONCRETE), stack(ModBlocks.CONCRETE_ASBESTOS), stack(ModBlocks.DUCRETE)),
                ingCount(stack(ModBlocks.STEEL_SCAFFOLD), 8),
                ingCount(matStack(ModMaterials.POLYMER, MaterialShape.PLATE, 1), 8),
                ingCount(stack(ModItems.COIL_COPPER), 4)); // original: AnvilRecipes.java#475-482
        construction(writer, "steel", "substation", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.SUBSTATION, 2),
                anyOf(8, stack(ModBlocks.CONCRETE), stack(ModBlocks.CONCRETE_ASBESTOS), stack(ModBlocks.DUCRETE)),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1), 8),
                ingCount(matStack(ModMaterials.POLYMER, MaterialShape.PLATE, 1), 12),
                ingCount(stack(ModItems.COIL_COPPER), 8)); // original: AnvilRecipes.java#484-491
        // (anyConcrete выше объявлен только для читаемости — не используется отдельно)

        // Дымоходы (ориг. #493-508)
        construction(writer, "steel", "chimney_brick", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.CHIMNEY_BRICK),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 4),
                AnvilIngredient.ofCount(new ItemStack(Items.BRICKS), 16),
                ingCount(stack(ModBlocks.STEEL_GRATE), 2)); // original: AnvilRecipes.java#493-499
        construction(writer, "oil", "chimney_industrial", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.CHIMNEY_INDUSTRIAL),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 1), 16),
                anyOf(64, stack(ModBlocks.CONCRETE), stack(ModBlocks.CONCRETE_ASBESTOS), stack(ModBlocks.DUCRETE)),
                ingCount(stack(ModBlocks.STEEL_GRATE), 4),
                ingCount(stack(ModItems.FILTER_COAL), 4)); // original: AnvilRecipes.java#501-508

        // Бочки с отходами (ориг. #510-521)
        construction(writer, "oil", "yellow_barrel", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.BARREL_YELLOW),
                ing(stack(ModItems.TANK_STEEL)),
                ingCount(matStack(ModMaterials.LEAD, MaterialShape.PLATE, 1), 2),
                ingCount(stack(ModItems.NUCLEAR_WASTE), 10)); // original: AnvilRecipes.java#510-515
        construction(writer, "oil", "vitrified_barrel", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.BARREL_VITRIFIED),
                ing(stack(ModItems.TANK_STEEL)),
                ingCount(matStack(ModMaterials.LEAD, MaterialShape.PLATE, 1), 2),
                ingCount(stack(ModItems.NUCLEAR_WASTE_VITRIFIED), 10)); // original: AnvilRecipes.java#516-521

        // Демоническое ядро (ориг. #523-528)
        construction(writer, "oil", "demon_core_open", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.DEMON_CORE_OPEN),
                ing(stack(ModItems.DEMON_CORE_CLOSED)), // ориг. man_core
                ingCount(matStack(ModMaterials.BERYLLIUM, MaterialShape.INGOT, 1), 4),
                ing(stack(ModItems.SCREWDRIVER))); // original: AnvilRecipes.java#523-528

        // Плашки деша и висмута (ориг. #530-535)
        construction(writer, "oil", "plate_desh", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                matStack(ModMaterials.DESH, MaterialShape.PLATE, 4),
                ingCount(matStack(ModMaterials.DESH, MaterialShape.INGOT, 1), 4),
                anyOf(2, matStack(ModMaterials.POLYMER, MaterialShape.POWDER, 1), matStack(ModMaterials.BAKELITE, MaterialShape.POWDER, 1)),
                ingCount(matStack(ModMaterials.DURA_STEEL, MaterialShape.INGOT, 1), 1)); // original: AnvilRecipes.java#530-532
        construction(writer, "nuclear", "plate_bismuth", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.CONSTRUCTION,
                matStack(ModMaterials.BISMUTH, MaterialShape.PLATE, 1),
                ingCount(matStack(ModMaterials.BISMUTH, MaterialShape.NUGGET, 1), 2),
                ingCount(matStack(ModMaterials.URANIUM238, MaterialShape.BILLET, 1), 2),
                ingCount(matStack(ModMaterials.NIOBIUM, MaterialShape.POWDER, 1), 1)); // original: AnvilRecipes.java#533-535

        // Броня (ориг. #537-554)
        construction(writer, "steel", "plate_armor_titanium", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.PLATE_ARMOR_TITANIUM),
                ingCount(matStack(ModMaterials.TITANIUM, MaterialShape.PLATE, 1), 2),
                ingCount(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1), 1),
                ingCount(stack(ModItems.BOLT_STEEL), 4)); // original: AnvilRecipes.java#537-539
        construction(writer, "oil", "plate_armor_ajr", AnvilTier.OIL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.PLATE_ARMOR_AJR, 2),
                ingCount(matStack(ModMaterials.IRON, MaterialShape.PLATE, 1), 6),
                ingCount(matStack(ModMaterials.NIOBIUM, MaterialShape.INGOT, 1), 1),
                ingCount(stack(ModItems.PLATE_ARMOR_TITANIUM), 1)); // original: AnvilRecipes.java#540-542
        construction(writer, "nuclear", "plate_armor_hev", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.PLATE_ARMOR_HEV),
                ingCount(matStack(ModMaterials.DURA_STEEL, MaterialShape.PLATE, 1), 4),
                ingCount(stack(ModItems.PLATE_ARMOR_TITANIUM), 1),
                ingCount(matStack(ModMaterials.TUNGSTEN, MaterialShape.WIRE, 1), 8)); // original: AnvilRecipes.java#543-545
        construction(writer, "nuclear", "plate_armor_lunar", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.PLATE_ARMOR_LUNAR),
                ingCount(matStack(ModMaterials.WEAPONSTEEL, MaterialShape.PLATE, 1), 4),
                ingCount(matStack(ModMaterials.STARMETAL, MaterialShape.INGOT, 1), 1),
                ingCount(matStack(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE, 1), 8)); // original: AnvilRecipes.java#546-548
        construction(writer, "fusion", "plate_armor_fau", AnvilTier.FUSION, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.PLATE_ARMOR_FAU),
                ingCount(matStack(ModMaterials.METEORITE_FORGED, MaterialShape.INGOT, 1), 4),
                ingCount(matStack(ModMaterials.DESH, MaterialShape.INGOT, 1), 1),
                ingCount(matStack(ModMaterials.YHARONITE, MaterialShape.BILLET, 1), 1)); // original: AnvilRecipes.java#549-551
        construction(writer, "particle", "plate_armor_dnt", AnvilTier.PARTICLE, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.PLATE_ARMOR_DNT),
                ingCount(matStack(ModMaterials.DINEUTRONIUM, MaterialShape.PLATE, 1), 4),
                ingCount(stack(ModItems.PARTICLE_SPARKTICLE), 1),
                ingCount(stack(ModItems.PLATE_ARMOR_FAU), 6)); // original: AnvilRecipes.java#552-554

        // Ракета судного дня (ориг. #556-562, тир 5)
        construction(writer, "rbmk", "missile_doomsday", AnvilTier.RBMK, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.MISSILE_DOOMSDAY),
                ing(stack(ModItems.MISSILE_DOOMSDAY_RUSTED)),
                anyOf(8, matStack(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT, 1), matStack(ModMaterials.PVC, MaterialShape.INGOT, 1)), // ANY_HARDPLASTIC.ingot
                ingCount(matStack(ModMaterials.ALUMINIUM, MaterialShape.PLATE_WELDED, 1), 2),
                ingCount(matStack(ModMaterials.PLUTONIUM239, MaterialShape.BILLET, 1), 3)); // original: AnvilRecipes.java#556-562

        // Жидкостные трубы (ориг. #564-574). Оригинал: 15 метаданных fluid_duct_box
        // (5 групп x металл) + 5 metadа exhaust; в порту один блок — схлопнуто в один
        // набор (4 рецепта вместо 40).
        construction(writer, "steel", "fluid_duct_box", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.FLUID_DUCT_BOX),
                ingCount(matStack(ModMaterials.IRON, MaterialShape.PLATE, 1), 1)); // original: AnvilRecipes.java#565 (мета 0; мета 1..14 схлопнуты)
        disassemble(writer, "steel", "fluid_duct_box", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(matStack(ModMaterials.IRON, MaterialShape.PLATE, 1)),
                ing(stack(ModBlocks.FLUID_DUCT_BOX))); // original: AnvilRecipes.java#568 (мета 0; мета 1..14 схлопнуты)
        construction(writer, "steel", "fluid_duct_exhaust", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModBlocks.FLUID_DUCT_EXHAUST, 8),
                ingCount(matStack(ModMaterials.IRON, MaterialShape.PLATE, 1), 1),
                ingCount(matStack(ModMaterials.POLYMER, MaterialShape.PLATE, 1), 1)); // original: AnvilRecipes.java#572
        disassemble(writer, "steel", "fluid_duct_exhaust", AnvilTier.STEEL, AnvilRecipe.OverlayType.NONE,
                b -> {
                    b.addOutput(matStack(ModMaterials.IRON, MaterialShape.PLATE, 1));
                    b.addOutput(matStack(ModMaterials.POLYMER, MaterialShape.PLATE, 1));
                },
                ing(stack(ModBlocks.FLUID_DUCT_EXHAUST))); // original: AnvilRecipes.java#573 (многие-ко-многим -> NONE)

        // Красные кабельные короба (ориг. #576-579; 5 метаданных -> 5 порт-блоков)
        List<RegistrySupplier<Block>> cableBoxes = List.of(
                ModBlocks.RED_CABLE_BOX, ModBlocks.RED_CABLE_BOX_1, ModBlocks.RED_CABLE_BOX_2,
                ModBlocks.RED_CABLE_BOX_3, ModBlocks.RED_CABLE_BOX_4);
        for (int i = 0; i < cableBoxes.size(); i++) {
            String suffix = i == 0 ? "" : "_" + i;
            RegistrySupplier<Block> box = cableBoxes.get(i);
            construction(writer, "steel", "red_cable_box" + suffix, AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                    stack(box, 16),
                    ingCount(matStack(ModMaterials.RED_COPPER, MaterialShape.INGOT, 1), 1),
                    ingCount(matStack(ModMaterials.POLYMER, MaterialShape.PLATE, 1), 1)); // original: AnvilRecipes.java#577
            disassemble(writer, "steel", "red_cable_box" + suffix, AnvilTier.STEEL, AnvilRecipe.OverlayType.NONE,
                    b -> {
                        b.addOutput(matStack(ModMaterials.RED_COPPER, MaterialShape.INGOT, 1));
                        b.addOutput(matStack(ModMaterials.POLYMER, MaterialShape.PLATE, 1));
                    },
                    ing(stack(box))); // original: AnvilRecipes.java#578
        }
    }

    /** Деко-блок x4 из слитка (ориг. #181-188, тир 1, CONSTRUCTION). */
    private static void deco(Consumer<FinishedRecipe> writer, String name, AnvilIngredient ingot, ItemStack deco) {
        AnvilRecipeBuilder.inventoryRecipe(new ItemStack(deco.getItem(), 4), AnvilTier.IRON)
                .withOrder(nextOrder())
                .withOverlay(AnvilRecipe.OverlayType.CONSTRUCTION)
                .addInventoryRequirement(ingot)
                .save(writer, anvilId("iron", "craft", "deco_" + name));
    }

    /** Корпус (shell) x4 плиты, тир 1, SMITHING. */
    private static void shell(Consumer<FinishedRecipe> writer, String name, RegistrySupplier<Item> shellItem, ItemStack plate) {
        if (shellItem == null) {
            reportSkip("shell autogen (" + name + "): предмет shell не зарегистрирован");
            return;
        }
        AnvilRecipeBuilder.inventoryRecipe(stack(shellItem), AnvilTier.IRON)
                .withOrder(nextOrder())
                .withOverlay(AnvilRecipe.OverlayType.SMITHING)
                .addInventoryRequirement(ingCount(plate, 4))
                .save(writer, anvilId("iron", "craft", "shell_" + name));
    }

    /** Труба (pipe) из 3 плит/слитков, тир 1, SMITHING. */
    private static void pipe(Consumer<FinishedRecipe> writer, String name, RegistrySupplier<Item> pipeItem, AnvilIngredient input) {
        if (pipeItem == null) {
            reportSkip("pipe autogen (" + name + "): предмет pipe не зарегистрирован");
            return;
        }
        AnvilRecipeBuilder.inventoryRecipe(stack(pipeItem), AnvilTier.IRON)
                .withOrder(nextOrder())
                .withOverlay(AnvilRecipe.OverlayType.SMITHING)
                .addInventoryRequirement(input)
                .save(writer, anvilId("iron", "craft", "pipe_" + name));
    }

    /** Ориг. registerConstructionStamps (#582-608). */
    private static void registerConstructionStamps(Consumer<FinishedRecipe> writer) {
        stampSet(writer, "stone",    ModItems.STAMP_STONE_FLAT,    ModItems.STAMP_STONE_PLATE,    ModItems.STAMP_STONE_WIRE,    ModItems.STAMP_STONE_CIRCUIT,    AnvilTier.IRON);   // original: AnvilRecipes.java#584-586
        stampSet(writer, "iron",     ModItems.STAMP_IRON_FLAT,     ModItems.STAMP_IRON_PLATE,     ModItems.STAMP_IRON_WIRE,     ModItems.STAMP_IRON_CIRCUIT,     AnvilTier.IRON);   // original: AnvilRecipes.java#588-590
        stampSet(writer, "steel",    ModItems.STAMP_STEEL_FLAT,    ModItems.STAMP_STEEL_PLATE,    ModItems.STAMP_STEEL_WIRE,    ModItems.STAMP_STEEL_CIRCUIT,    AnvilTier.STEEL);  // original: AnvilRecipes.java#592-594
        stampSet(writer, "titanium", ModItems.STAMP_TITANIUM_FLAT, ModItems.STAMP_TITANIUM_PLATE, ModItems.STAMP_TITANIUM_WIRE, ModItems.STAMP_TITANIUM_CIRCUIT, AnvilTier.STEEL);  // original: AnvilRecipes.java#596-598
        stampSet(writer, "obsidian", ModItems.STAMP_OBSIDIAN_FLAT, ModItems.STAMP_OBSIDIAN_PLATE, ModItems.STAMP_OBSIDIAN_WIRE, ModItems.STAMP_OBSIDIAN_CIRCUIT, AnvilTier.STEEL);  // original: AnvilRecipes.java#600-602
        stampSet(writer, "desh",     ModItems.STAMP_DESH_FLAT,     ModItems.STAMP_DESH_PLATE,     ModItems.STAMP_DESH_WIRE,     ModItems.STAMP_DESH_CIRCUIT,     AnvilTier.OIL);    // original: AnvilRecipes.java#604-606
    }

    private static void stampSet(Consumer<FinishedRecipe> writer, String material, RegistrySupplier<Item> flat,
                                 RegistrySupplier<Item> plate, RegistrySupplier<Item> wire, RegistrySupplier<Item> circuit, AnvilTier tier) {
        stamp(writer, tier, material + "_plate",   flat, plate);   // original: AnvilRecipes.java (первый рецепт набора)
        stamp(writer, tier, material + "_wire",    flat, wire);    // original: AnvilRecipes.java (второй рецепт набора)
        stamp(writer, tier, material + "_circuit", flat, circuit); // original: AnvilRecipes.java (третий рецепт набора)
    }

    private static void stamp(Consumer<FinishedRecipe> writer, AnvilTier tier, String name, RegistrySupplier<Item> flat, RegistrySupplier<Item> out) {
        String folder = tier.name().toLowerCase(java.util.Locale.ROOT);
        AnvilRecipeBuilder.inventoryRecipe(stack(out), tier)
                .withOrder(nextOrder())
                .withOverlay(AnvilRecipe.OverlayType.SMITHING)
                .addInventoryRequirement(stack(flat))
                .save(writer, anvilId(folder, "craft", "stamp_" + name));
    }

    /** Ориг. registerConstructionAmmo (#610-627). */
    private static void registerConstructionAmmo(Consumer<FinishedRecipe> writer) {
        construction(writer, "steel", "stamp_9", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.STAMP_9),
                ing(stack(ModItems.STAMP_IRON_FLAT)),
                ingCount(matStack(ModMaterials.GUNMETAL, MaterialShape.INGOT, 1), 2)); // original: AnvilRecipes.java#612
        construction(writer, "steel", "stamp_50", AnvilTier.STEEL, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.STAMP_50),
                ing(stack(ModItems.STAMP_IRON_FLAT)),
                ingCount(matStack(ModMaterials.GUNMETAL, MaterialShape.INGOT, 1), 2)); // original: AnvilRecipes.java#613
        construction(writer, "nuclear", "stamp_desh_9", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.STAMP_DESH_9),
                ing(stack(ModItems.STAMP_DESH_FLAT)),
                ingCount(matStack(ModMaterials.WEAPONSTEEL, MaterialShape.INGOT, 1), 4)); // original: AnvilRecipes.java#614
        construction(writer, "nuclear", "stamp_desh_50", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.CONSTRUCTION,
                stack(ModItems.STAMP_DESH_50),
                ing(stack(ModItems.STAMP_DESH_FLAT)),
                ingCount(matStack(ModMaterials.WEAPONSTEEL, MaterialShape.INGOT, 1), 4)); // original: AnvilRecipes.java#615
        // Формы c9/c50 (ориг. #617-618, id 16/17) — см. javadoc класса: в порту нет
        // предметов форм C9/C50, а порт-онли C357/CBUCKSHOT в оригинале отсутствуют. SKIP.
        // Оружейные формы id 22-28 (ориг. #620-626, mold_base + сталь x4, тир 2):
        constructionMold(writer, "barrel_light",    ModItems.MOLD_BARREL_LIGHT);    // original: AnvilRecipes.java#620
        constructionMold(writer, "barrel_heavy",    ModItems.MOLD_BARREL_HEAVY);    // original: AnvilRecipes.java#621
        constructionMold(writer, "receiver_light",  ModItems.MOLD_RECEIVER_LIGHT);  // original: AnvilRecipes.java#622
        constructionMold(writer, "receiver_heavy",  ModItems.MOLD_RECEIVER_HEAVY);  // original: AnvilRecipes.java#623
        constructionMold(writer, "mechanism",       ModItems.MOLD_MECHANISM);       // original: AnvilRecipes.java#624
        constructionMold(writer, "stock",           ModItems.MOLD_STOCK);           // original: AnvilRecipes.java#625
        constructionMold(writer, "grip",            ModItems.MOLD_GRIP);            // original: AnvilRecipes.java#626
    }

    /** Ориг. registerConstructionRecycling (#637-1006). */
    private static void registerConstructionRecycling(Consumer<FinishedRecipe> writer) {

        // Редкоземельный кусок (ориг. #639-651, тир 2, RECYCLING)
        disassemble(writer, "steel", "rare_chunk", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.FRAGMENT_BORON));
                    b.addOutput(stack(ModItems.FRAGMENT_BORON), 0.5F);
                    b.addOutput(stack(ModItems.FRAGMENT_LANTHANIUM), 0.1F);
                    b.addOutput(stack(ModItems.FRAGMENT_COBALT));
                    b.addOutput(stack(ModItems.FRAGMENT_COBALT), 0.5F);
                    b.addOutput(stack(ModItems.FRAGMENT_CERIUM), 0.1F);
                    b.addOutput(stack(ModItems.FRAGMENT_NEODYMIUM), 0.5F);
                    b.addOutput(stack(ModItems.FRAGMENT_NIOBIUM), 0.5F);
                },
                ing(stack(ModItems.RAREGROUND_ORE_CHUNK))); // original: AnvilRecipes.java#639-651 (chunk_ore RARE)

        // Деко-блоки -> слитки (ориг. #653-662, тир 1); deco_asbestos и platemetal — SKIP (нет блоков)
        disassemble(writer, "iron", "deco_titanium",  AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(matStack(ModMaterials.TITANIUM, MaterialShape.INGOT, 1)),
                ing(stack(ModBlocks.DECO_TITANIUM), 4));      // original: AnvilRecipes.java#653
        disassemble(writer, "iron", "deco_red_copper", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(matStack(ModMaterials.RED_COPPER, MaterialShape.INGOT, 1)),
                ing(stack(ModBlocks.DECO_RED_COPPER), 4));    // original: AnvilRecipes.java#654
        disassemble(writer, "iron", "deco_tungsten", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(matStack(ModMaterials.TUNGSTEN, MaterialShape.INGOT, 1)),
                ing(stack(ModBlocks.DECO_TUNGSTEN), 4));      // original: AnvilRecipes.java#655
        disassemble(writer, "iron", "deco_aluminium", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(stack(ModItems.INGOT_ALUMINIUM)),
                ing(stack(ModBlocks.DECO_ALUMINIUM), 4));     // original: AnvilRecipes.java#656
        disassemble(writer, "iron", "deco_steel", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1)),
                ing(stack(ModBlocks.DECO_STEEL), 4));         // original: AnvilRecipes.java#657
        disassemble(writer, "iron", "deco_rusty_steel", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1)),
                ing(stack(ModBlocks.DECO_RUSTY_STEEL), 8));   // original: AnvilRecipes.java#658
        disassemble(writer, "iron", "deco_lead", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(matStack(ModMaterials.LEAD, MaterialShape.INGOT, 1)),
                ing(stack(ModBlocks.DECO_LEAD), 4));          // original: AnvilRecipes.java#659
        disassemble(writer, "iron", "deco_beryllium", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(matStack(ModMaterials.BERYLLIUM, MaterialShape.INGOT, 1)),
                ing(stack(ModBlocks.DECO_BERYLLIUM), 4));     // original: AnvilRecipes.java#660

        // Нагреватели (ориг. #664-678, тир 2)
        disassemble(writer, "steel", "firebox", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 8));
                    b.addOutput(new ItemStack(Items.COPPER_INGOT, 6));
                },
                ing(stack(ModBlocks.FIREBOX))); // original: AnvilRecipes.java#664-670
        disassemble(writer, "steel", "heating_oven", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.FIREBRICK, 16));
                    b.addOutput(new ItemStack(Items.COPPER_INGOT, 8));
                },
                ing(stack(ModItems.HEATING_OVEN))); // original: AnvilRecipes.java#672-678 (heater_oven)

        // Двигатели Стирлинга (ориг. #680-735, тир 2); meta=1 (стёрлинг без шестерни,
        // ориг. выдаётся при снятии шестерни) — в порту такого предмета нет, SKIP;
        // также SKIP machine_stirling_steel meta=1 (#726-735) по той же причине.
        disassemble(writer, "steel", "stirling", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 6));
                    b.addOutput(new ItemStack(Items.COPPER_INGOT, 8));
                    b.addOutput(stack(ModItems.COIL_COPPER, 4));
                    b.addOutput(stack(ModItems.GEAR_LARGE, 1));
                },
                ing(stack(ModBlocks.STIRLING))); // original: AnvilRecipes.java#680-689 (meta 0)
        disassemble(writer, "steel", "stirling_steel", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 16));
                    b.addOutput(matStack(ModMaterials.BERYLLIUM, MaterialShape.INGOT, 6));
                    b.addOutput(new ItemStack(Items.COPPER_INGOT, 4));
                    b.addOutput(stack(ModItems.COIL_GOLD, 8));
                    b.addOutput(stack(ModItems.GEAR_LARGE, 1));
                },
                ing(stack(ModBlocks.STIRLING_STEEL))); // original: AnvilRecipes.java#700-709
        // Шестерни (ориг. #710-725, тир 2). meta 0/1 в порту единый предмет — оба рецепта
        // сохранены; в игре будет конфликт матчей (см. отчёт).
        disassemble(writer, "steel", "gear_steel", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 8));
                    b.addOutput(matStack(ModMaterials.TITANIUM, MaterialShape.INGOT, 1));
                },
                ing(stack(ModItems.GEAR_LARGE))); // original: AnvilRecipes.java#710-717 (gear meta 1)
        disassemble(writer, "steel", "gear_iron", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.IRON, MaterialShape.PLATE, 8));
                    b.addOutput(new ItemStack(Items.COPPER_INGOT, 1));
                },
                ing(stack(ModItems.GEAR_LARGE))); // original: AnvilRecipes.java#718-725 (gear meta 0)
        // machine_stirling_steel meta 1 (ориг. #726-735) — SKIP (вариант meta=1 не представлен).
        disassemble(writer, "oil", "bat9000", AnvilTier.OIL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED, 4));
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 16));
                },
                ing(stack(ModItems.BAT9000))); // original: AnvilRecipes.java#736-742

        // Деко-компьютер (ориг. #744-757, тир 2)
        disassemble(writer, "steel", "deco_computer", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.CRT_DISPLAY, 1));
                    b.addOutput(scrap(3));
                    b.addOutput(matStack(ModMaterials.COPPER, MaterialShape.WIRE, 4));
                    b.addOutput(stack(ModItems.PCB, 2));
                    b.addOutput(stack(ModItems.VACUUM_TUBE, 1), 0.5F);
                    b.addOutput(stack(ModItems.CAPACITOR, 1), 0.75F);
                    b.addOutput(stack(ModItems.CAPACITOR, 1), 0.5F);
                    b.addOutput(stack(ModItems.ANALOG_CIRCUIT, 1), 0.1F);
                },
                ing(stack(ModBlocks.PUTER))); // original: AnvilRecipes.java#744-757 (deco_computer -> порт puter)

        // ЭЛТ-декор (ориг. #758-768, wildcard meta -> 3 порт-блока как anyOf)
        disassemble(writer, "steel", "deco_crt", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.CRT_DISPLAY, 1));
                    b.addOutput(scrap(2));
                    b.addOutput(matStack(ModMaterials.COPPER, MaterialShape.WIRE, 2));
                    b.addOutput(matStack(ModMaterials.GOLD, MaterialShape.WIRE, 2), 0.25F);
                    b.addOutput(stack(ModItems.VACUUM_TUBE, 1), 0.25F);
                },
                anyOf(1, stack(ModBlocks.CRT_BROKEN), stack(ModBlocks.CRT_BSOD), stack(ModBlocks.CRT_CLEAN))); // original: AnvilRecipes.java#758-768 (deco_crt wildcard)

        // Тостеры (ориг. #769-805). Порт: один блок toaster — переносится только
        // железный тостер (meta 0); steel/wooden (meta 1/2) — SKIP.
        disassemble(writer, "steel", "toaster", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.IRON, MaterialShape.PLATE, 3));
                    b.addOutput(scrap(1));
                    b.addOutput(stack(ModItems.COIL_TUNGSTEN, 1));
                    b.addOutput(new ItemStack(Items.BREAD, 1), 0.5F);
                    b.addOutput(stack(ModItems.FUSION_CORE, 1), 0.01F);
                },
                ing(stack(ModBlocks.TOASTER))); // original: AnvilRecipes.java#769-779 (deco_toaster meta 0)

        // Приёмники (ориг. #806-839, тир 2)
        disassemble(writer, "steel", "radiorec", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 4));
                    b.addOutput(matStack(ModMaterials.COPPER, MaterialShape.WIRE, 1));
                    b.addOutput(stack(ModItems.VACUUM_TUBE, 1), 0.5F);
                    b.addOutput(matStack(ModMaterials.POLYMER, MaterialShape.INGOT, 1), 0.25F);
                },
                ing(stack(ModBlocks.RADIOREC))); // original: AnvilRecipes.java#806-814
        disassemble(writer, "steel", "tape_recorder", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 1));
                    b.addOutput(matStack(ModMaterials.TUNGSTEN, MaterialShape.INGOT, 1), 0.25F);
                },
                ing(stack(ModBlocks.TAPE_RECORDER))); // original: AnvilRecipes.java#815-821
        disassemble(writer, "steel", "pole_top", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.TUNGSTEN, MaterialShape.INGOT, 3));
                    b.addOutput(matStack(ModMaterials.RED_COPPER, MaterialShape.INGOT, 1));
                    b.addOutput(matStack(ModMaterials.BERYLLIUM, MaterialShape.INGOT, 2));
                    b.addOutput(matStack(ModMaterials.BERYLLIUM, MaterialShape.INGOT, 1), 0.5F);
                },
                ing(stack(ModBlocks.POLE_TOP))); // original: AnvilRecipes.java#822-830
        disassemble(writer, "steel", "pole_satellite_receiver", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 3));
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 2), 0.5F);
                    b.addOutput(stack(ModItems.VACUUM_TUBE, 1), 0.5F);
                    b.addOutput(matStack(ModMaterials.RED_COPPER, MaterialShape.WIRE_DENSE, 1));
                },
                ing(stack(ModBlocks.POLE_SATELLITE_RECEIVER))); // original: AnvilRecipes.java#831-839
        disassemble(writer, "iron", "file_cabinet", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 2));
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.PLATE, 2), 0.5F);
                    b.addOutput(matStack(ModMaterials.POLYMER, MaterialShape.PLATE, 2), 0.25F);
                    b.addOutput(scrap(1));
                },
                ing(stack(ModBlocks.FILE_CABINET))); // original: AnvilRecipes.java#840-849 (filing_cabinet)

        // Топливные стержни (ориг. #851-866, тир 2). RA226BE/PO210BE/ZR — SKIP
        // (в порте нет соответствующих pile_rod предметов).
        disassemble(writer, "steel", "pile_rod_uranium", AnvilTier.STEEL, AnvilRecipe.OverlayType.RECYCLING,
                b -> b.addOutput(stack(ModItems.PILE_ROD_URANIUM)),
                ingCount(matStack(ModMaterials.URANIUM, MaterialShape.BILLET, 1), 3)); // original: AnvilRecipes.java#863-866 (U.billet -> pile_rod NU)

        // РБМК (ориг. #868-925, тир 4)
        disassemble(writer, "nuclear", "rbmk_moderator", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_BLANK));
                    b.addOutput(stack(ModBlocks.BLOCK_GRAPHITE, 4));
                },
                ing(stack(ModBlocks.RBMK_MODERATOR))); // original: AnvilRecipes.java#869-873
        disassemble(writer, "nuclear", "rbmk_absorber", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_BLANK));
                    b.addOutput(matStack(ModMaterials.BORON, MaterialShape.INGOT, 8));
                },
                ing(stack(ModBlocks.RBMK_ABSORBER))); // original: AnvilRecipes.java#874-878
        disassemble(writer, "nuclear", "rbmk_reflector", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_BLANK));
                    b.addOutput(stack(ModItems.NEUTRON_REFLECTOR, 8));
                },
                ing(stack(ModBlocks.RBMK_REFLECTOR))); // original: AnvilRecipes.java#879-883
        disassemble(writer, "nuclear", "rbmk_control", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_ABSORBER));
                    b.addOutput(matStack(ModMaterials.GRAPHITE, MaterialShape.INGOT, 2));
                    b.addOutput(stack(ModItems.MOTOR, 2));
                },
                ing(stack(ModBlocks.RBMK_CONTROL))); // original: AnvilRecipes.java#884-889
        disassemble(writer, "nuclear", "rbmk_control_mod", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_CONTROL));
                    b.addOutput(stack(ModBlocks.BLOCK_GRAPHITE, 4));
                    b.addOutput(matStack(ModMaterials.BISMUTH, MaterialShape.NUGGET, 4));
                },
                ing(stack(ModBlocks.RBMK_CONTROL_MOD))); // original: AnvilRecipes.java#890-895
        disassemble(writer, "nuclear", "rbmk_control_auto", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_CONTROL));
                    b.addOutput(stack(ModItems.ADVANCED_CIRCUIT));
                    b.addOutput(stack(ModItems.CRT_DISPLAY));
                },
                ing(stack(ModBlocks.RBMK_CONTROL_AUTO))); // original: AnvilRecipes.java#896-901
        disassemble(writer, "nuclear", "rbmk_rod_reasim", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_BLANK));
                    b.addOutput(matStack(ModMaterials.ZIRCONIUM, MaterialShape.INGOT, 4));
                    b.addOutput(stack(ModItems.SHELL_STEEL, 2));
                },
                ing(stack(ModBlocks.RBMK_ROD_REASIM))); // original: AnvilRecipes.java#902-907
        disassemble(writer, "nuclear", "rbmk_rod_reasim_mod", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_ROD_REASIM));
                    b.addOutput(stack(ModBlocks.BLOCK_GRAPHITE, 4));
                    b.addOutput(matStack(ModMaterials.TCALLOY, MaterialShape.INGOT, 4));
                },
                ing(stack(ModBlocks.RBMK_ROD_REASIM_MOD))); // original: AnvilRecipes.java#908-913
        disassemble(writer, "nuclear", "rbmk_outgasser", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_BLANK));
                    b.addOutput(stack(ModBlocks.STEEL_GRATE, 6));
                    b.addOutput(stack(ModItems.TANK_STEEL));
                    b.addOutput(new ItemStack(Items.HOPPER));
                },
                ing(stack(ModBlocks.RBMK_OUTGASSER))); // original: AnvilRecipes.java#914-920
        disassemble(writer, "nuclear", "rbmk_storage", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_BLANK));
                    b.addOutput(stack(ModItems.CRATE_STEEL, 2));
                },
                ing(stack(ModBlocks.RBMK_STORAGE))); // original: AnvilRecipes.java#921-925

        // Ветка if(!GeneralConfig.enable528) — ВКЛЮЧЕНА (дефолт enable528=false, ориг. #927-964)
        disassemble(writer, "nuclear", "rbmk_rod", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_BLANK));
                    b.addOutput(stack(ModItems.SHELL_STEEL, 2));
                },
                ing(stack(ModBlocks.RBMK_ROD))); // original: AnvilRecipes.java#929-933
        disassemble(writer, "nuclear", "rbmk_rod_mod", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_ROD));
                    b.addOutput(stack(ModBlocks.BLOCK_GRAPHITE, 4));
                    b.addOutput(matStack(ModMaterials.BISMUTH, MaterialShape.NUGGET, 4));
                },
                ing(stack(ModBlocks.RBMK_ROD_MOD))); // original: AnvilRecipes.java#934-939
        disassemble(writer, "nuclear", "rbmk_boiler", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_BLANK));
                    b.addOutput(stack(ModItems.PIPE_COPPER, 6));
                    b.addOutput(stack(ModItems.SHELL_COPPER, 2));
                },
                ing(stack(ModBlocks.RBMK_BOILER))); // original: AnvilRecipes.java#940-945
        disassemble(writer, "nuclear", "rbmk_cooler", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModBlocks.RBMK_BLANK));
                    b.addOutput(stack(ModBlocks.STEEL_GRATE, 4));
                    b.addOutput(matStack(ModMaterials.POLYMER, MaterialShape.PLATE, 4));
                },
                ing(stack(ModBlocks.RBMK_COOLER))); // original: AnvilRecipes.java#946-951
        disassemble(writer, "nuclear", "reactor_research", AnvilTier.NUCLEAR, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 8));
                    b.addOutput(matStack(ModMaterials.TCALLOY, MaterialShape.INGOT, 4));
                    b.addOutput(stack(ModItems.MOTOR_DESH, 2));
                    b.addOutput(matStack(ModMaterials.BORON, MaterialShape.INGOT, 5));
                    b.addOutput(matStack(ModMaterials.LEAD, MaterialShape.PLATE, 8));
                    b.addOutput(stack(ModItems.CRT_DISPLAY, 3));
                    b.addOutput(stack(ModItems.CIRCUIT));
                    b.addOutput(stack(ModItems.CIRCUIT), 0.5F);
                },
                ing(stack(ModItems.REACTOR_RESEARCH))); // original: AnvilRecipes.java#952-962

        // Турбина, бочки, глифид, фьюжн (ориг. #966-1005)
        disassemble(writer, "oil", "turbine", AnvilTier.OIL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.TURBINE_TITANIUM));
                    b.addOutput(stack(ModItems.COIL_COPPER, 2));
                    b.addOutput(matStack(ModMaterials.STEEL, MaterialShape.INGOT, 4));
                },
                ing(stack(ModItems.TURBINE))); // original: AnvilRecipes.java#966-971 (machine_turbine)
        disassemble(writer, "oil", "yellow_barrel", AnvilTier.OIL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.TANK_STEEL));
                    b.addOutput(matStack(ModMaterials.LEAD, MaterialShape.PLATE, 2));
                    b.addOutput(stack(ModItems.NUCLEAR_WASTE, 10));
                },
                ing(stack(ModBlocks.BARREL_YELLOW))); // original: AnvilRecipes.java#973-978
        disassemble(writer, "oil", "vitrified_barrel", AnvilTier.OIL, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.TANK_STEEL));
                    b.addOutput(matStack(ModMaterials.LEAD, MaterialShape.PLATE, 2));
                    b.addOutput(stack(ModItems.NUCLEAR_WASTE_VITRIFIED, 10));
                },
                ing(stack(ModBlocks.BARREL_VITRIFIED))); // original: AnvilRecipes.java#979-984
        disassemble(writer, "iron", "egg_glyphid", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.GLYPHID_MEAT, 2));
                    b.addOutput(stack(ModItems.GLYPHID_MEAT, 1), 0.5F);
                    b.addOutput(new ItemStack(Items.BONE, 1), 0.75F);
                    b.addOutput(new ItemStack(Items.EXPERIENCE_BOTTLE, 1), 0.5F);
                },
                ing(stack(ModItems.EGG_GLYPHID))); // original: AnvilRecipes.java#986-992
        disassemble(writer, "iron", "fusion_heater", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.PIPE_STEEL, 4));
                    b.addOutput(stack(ModItems.PIPE_COPPER, 2));
                    b.addOutput(stack(ModItems.ANALOG_CIRCUIT, 1), 0.5F);
                },
                ing(stack(ModBlocks.FUSION_HEATER))); // original: AnvilRecipes.java#994-999
        disassemble(writer, "iron", "fusion_hatch", AnvilTier.IRON, AnvilRecipe.OverlayType.RECYCLING, b -> {
                    b.addOutput(stack(ModItems.PIPE_STEEL, 4));
                    b.addOutput(stack(ModItems.PIPE_COPPER, 4));
                    b.addOutput(stack(ModItems.ANALOG_CIRCUIT, 1), 0.75F);
                },
                ing(stack(ModBlocks.FUSION_HATCH))); // original: AnvilRecipes.java#1000-1005
    }

    // =====================================================================================
    //  Хелперы
    // =====================================================================================

    /** Инвентарный construction-рецепт (многие-к-одному или 1:1 из инвентаря). */
    private static void construction(Consumer<FinishedRecipe> writer, String tierFolder, String name, AnvilTier tier,
                                     AnvilRecipe.OverlayType overlay, ItemStack output, AnvilIngredient... requirements) {
        AnvilRecipeBuilder builder = AnvilRecipeBuilder.inventoryRecipe(output, tier)
                .withOrder(nextOrder())
                .withOverlay(overlay);
        for (AnvilIngredient requirement : requirements) {
            builder.addInventoryRequirement(requirement);
        }
        builder.save(writer, anvilId(tierFolder, "craft", name));
    }

    /** Инвентарный recycling/разборочный рецепт с несколькими выходами. */
    private static void disassemble(Consumer<FinishedRecipe> writer, String tierFolder, String name, AnvilTier tier,
                                    AnvilRecipe.OverlayType overlay, Consumer<AnvilRecipeBuilder> outputs, AnvilIngredient input) {
        AnvilRecipeBuilder builder = AnvilRecipeBuilder.inventoryRecipe(ItemStack.EMPTY, tier)
                .withOrder(nextOrder())
                .withOverlay(overlay)
                .addInventoryRequirement(input)
                .clearOutputs();
        outputs.accept(builder);
        builder.save(writer, anvilId(tierFolder, "disassemble", name));
    }

    private static int nextOrder() {
        return ++constructionOrder;
    }

    /** Ингредиент из одного предмета (количество 1). */
    private static AnvilIngredient ing(ItemStack stack) {
        return AnvilIngredient.of(stack);
    }

    /** Ингредиент из одного предмета с явным количеством. */
    private static AnvilIngredient ing(ItemStack stack, int count) {
        return AnvilIngredient.ofCount(stack, count);
    }

    /** Ингредиент из одного предмета с явным количеством. */
    private static AnvilIngredient ingCount(ItemStack stack, int count) {
        return AnvilIngredient.ofCount(stack, count);
    }

    /** Ore-dict-группа: несколько допустимых предметов, общее количество. */
    private static AnvilIngredient anyOf(int count, ItemStack... variants) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack variant : variants) {
            ItemStack single = variant.copy();
            single.setCount(1);
            list.add(single);
        }
        return new AnvilIngredient(list, count);
    }

    /** Алюминиевый слиток: исторический ingot_aluminium + автоген aluminum_ingot. */
    private static AnvilIngredient aluminiumIngot() {
        ItemStack legacy = stack(ModItems.INGOT_ALUMINIUM);
        ItemStack autogen = matStackOrNull(ModMaterials.ALUMINUM, MaterialShape.INGOT, 1);
        if (autogen != null) {
            return AnvilIngredient.of(legacy, autogen);
        }
        return AnvilIngredient.of(legacy);
    }

    /** Лом (ориг. ModItems.scrap) из таблицы материалов. */
    private static ItemStack scrap(int count) {
        Item scrap = ModMaterialItems.item(ModMaterials.SCRAP, MaterialShape.SCRAP);
        if (scrap == null) {
            throw new IllegalStateException("scrap item is not registered — cannot generate anvil recipes");
        }
        return new ItemStack(scrap, count);
    }

    /** Предмет материала в заданной форме; null, если форма не зарегистрирована (для SKIP в циклах). */
    private static ItemStack matStackOrNull(ModMaterials mat, MaterialShape shape, int count) {
        Item item = ModMaterialItems.item(mat, shape);
        return item == null ? null : new ItemStack(item, count);
    }

    /**
     * Предмет материала в заданной форме с жёсткой проверкой: для статических рецептов,
     * где наличие формы проверено по {@link ModMaterials} — отсутствие должно ронять
     * датаген, а не молча писать air в JSON.
     */
    private static ItemStack matStack(ModMaterials mat, MaterialShape shape, int count) {
        ItemStack stack = matStackOrNull(mat, shape, count);
        if (stack == null) {
            throw new IllegalStateException("Material item not registered: " + mat + " / " + shape
                    + " — the affected anvil recipe must be skipped explicitly");
        }
        return stack;
    }

    /** Сообщение об осознанном пропуске (циклы автогена). */
    private static void reportSkip(String reason) {
        System.out.println("[AnvilRecipeGenerator] SKIP: " + reason);
    }

    /**
     * Возвращает путь id для рецепта наковальни. {@link AnvilRecipeBuilder} унаследован
     * от {@link BaseRecipeBuilder}, поэтому {@code save(writer, String)} сам построит
     * кросс-версионный {@link net.minecraft.resources.ResourceLocation}.
     */
    private static String anvilId(String tierFolder, String category, String name) {
        return "anvil/" + tierFolder + "/" + category + "_" + name;
    }

    private static ItemStack stack(Object obj, int count) {
        if (obj instanceof RegistrySupplier<?>) {
            Object val = ((RegistrySupplier<?>) obj).get();
            if (val instanceof Item) {
                return new ItemStack((Item) val, count);
            } else if (val instanceof Block) {
                return new ItemStack(((Block) val).asItem(), count);
            }
        } else if (obj instanceof Item) {
            return new ItemStack((Item) obj, count);
        } else if (obj instanceof Block) {
            return new ItemStack(((Block) obj).asItem(), count);
        }
        throw new IllegalArgumentException("Unsupported object for stack: " + obj);
    }

    private static ItemStack stack(Object obj) {
        return stack(obj, 1);
    }
}
//?}
