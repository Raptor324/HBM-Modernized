package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.fluids.FluidStack;

/**
 * Реестр рецептов для рудного окислителя (Crystallizer).
 *
 * <p>Полный порт {@code com.hbm.inventory.recipes.CrystallizerRecipes#registerDefaults()}
 * из 1.7.10. В оригинале ~80 рецептов — здесь они все, с TODO-заглушками для тех мест,
 * где нужные предметы ещё не портированы в новую версию мода.</p>
 *
 * <p><strong>Принцип «зарегистрирован — работает»</strong>: фактически зарегистрированные
 * рецепты используют только существующие в проекте предметы / блоки / теги. Для
 * отсутствующих предметов (powder_calcium, ore_centrifuged, bedrock-руды, etc.) оставлены
 * закомментированные {@code TODO}-строки с указанием оригинальных параметров — раскомментировать
 * и поправить ссылки на ModItems когда соответствующий предмет будет добавлен.</p>
 *
 * <p><strong>Жидкость по умолчанию — перекись водорода 500 mB.</strong> В оригинале 1.7.10
 * перегрузка {@code registerRecipe(input, recipe)} без третьего параметра неявно подставляет
 * {@code FluidStack(Fluids.PEROXIDE, 500)}. Поэтому все «базовые» руды и утилитарные
 * рецепты на самом деле требуют перекись — это видно в игре: без неё крафт не запускается,
 * нужен идентификатор перекиси и заливка через бочку. Здесь воспроизводим то же поведение.</p>
 *
 * <p>Поиск рецепта идёт линейно по списку. На ~50–100 рецептах это норма; если
 * вырастет до 500+ — стоит закэшировать по {@code Item} входа.</p>
 */
public final class CrystallizerRecipes {

    private static final List<CrystallizerRecipe> RECIPES = new ArrayList<>();

    private static final int BASE_TIME = 600;     // 30 сек — большинство рудных рецептов
    private static final int UTILITY_TIME = 100;  //  5 сек — лёгкая обработка / превращения
    private static final int MIXING_TIME = 20;    //  1 сек — смешивания
    @SuppressWarnings("unused") private static final int ORE_TIME = 200;      // 10 сек — bedrock-цикл
    @SuppressWarnings("unused") private static final int BEDROCK_TIME = 200;
    @SuppressWarnings("unused") private static final int WASHING_TIME = 100;

    private CrystallizerRecipes() {}

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

    public static List<CrystallizerRecipe> getAll() {
        return List.copyOf(RECIPES);
    }

    public static void registerDefaults() {
        if (!RECIPES.isEmpty()) return; // защита от двойной регистрации

        // В оригинале (1.7.10) перегрузка registerRecipe(input, recipe) без третьего аргумента
        // подставляет PEROXIDE 500 mB по умолчанию. То есть «базовые» руды — это руды с перекисью,
        // а не «без жидкости вообще». Воспроизводим это поведение.
        FluidStack peroxide = fluid(ModFluids.PEROXIDE,       500);
        FluidStack sulfur   = fluid(ModFluids.SULFURIC_ACID,  500);
        // Жидкости, которые понадобятся когда раскомментируем TODO-рецепты:
        @SuppressWarnings("unused") FluidStack nitric  = fluid(ModFluids.NITRIC_ACID,    500);
        @SuppressWarnings("unused") FluidStack hiperf  = fluid(ModFluids.RADIOSOLVENT,   500);
        @SuppressWarnings("unused") FluidStack organic = fluid(ModFluids.SOLVENT,        500);

        // ═══════════════════════════════════════════════════════════════════
        // БАЗОВЫЕ РУДЫ (перекись водорода 500 mB, baseTime, prod 0.05)
        // ═══════════════════════════════════════════════════════════════════
        addOreTag("forge:ores/coal",       ModItems.CRYSTAL_COAL.get(),       0.05f, peroxide);
        addOreTag("forge:ores/iron",       ModItems.CRYSTAL_IRON.get(),       0.05f, peroxide);
        addOreTag("forge:ores/gold",       ModItems.CRYSTAL_GOLD.get(),       0.05f, peroxide);
        addOreTag("forge:ores/redstone",   ModItems.CRYSTAL_REDSTONE.get(),   0.05f, peroxide);
        addOreTag("forge:ores/lapis",      ModItems.CRYSTAL_LAPIS.get(),      0.05f, peroxide);
        addOreTag("forge:ores/diamond",    ModItems.CRYSTAL_DIAMOND.get(),    0.05f, peroxide);
        addOreTag("forge:ores/copper",     ModItems.CRYSTAL_COPPER.get(),     0.05f, peroxide);
        addOreTag("forge:ores/sulfur",     ModItems.CRYSTAL_SULFUR.get(),     0.05f, peroxide);
        addOreTag("forge:ores/niter",      ModItems.CRYSTAL_NITER.get(),      0.05f, peroxide);
        addOreTag("forge:ores/aluminum",   ModItems.CRYSTAL_ALUMINIUM.get(),  0.05f, peroxide);
        addOreTag("forge:ores/fluorite",   ModItems.CRYSTAL_FLUORITE.get(),   0.05f, peroxide);
        addOreTag("forge:ores/beryllium",  ModItems.CRYSTAL_BERYLLIUM.get(),  0.05f, peroxide);
        addOreTag("forge:ores/lead",       ModItems.CRYSTAL_LEAD.get(),       0.05f, peroxide);
        addOreTag("forge:ores/cinnabar",   ModItems.CRYSTAL_CINNEBAR.get(),   0.05f, peroxide);

        // ═══════════════════════════════════════════════════════════════════
        // РАДИОАКТИВНЫЕ / ТУГОПЛАВКИЕ РУДЫ (серная кислота, baseTime, prod 0.05)
        // ═══════════════════════════════════════════════════════════════════
        addOreTag("forge:ores/uranium",     ModItems.CRYSTAL_URANIUM.get(),     0.05f, sulfur);
        addOreTag("forge:ores/thorium",     ModItems.CRYSTAL_THORIUM.get(),     0.05f, sulfur);
        addOreTag("forge:ores/plutonium",   ModItems.CRYSTAL_PLUTONIUM.get(),   0.05f, sulfur);
        addOreTag("forge:ores/titanium",    ModItems.CRYSTAL_TITANIUM.get(),    0.05f, sulfur);
        addOreTag("forge:ores/tungsten",    ModItems.CRYSTAL_TUNGSTEN.get(),    0.05f, sulfur);
        addOreTag("forge:ores/lithium",     ModItems.CRYSTAL_LITHIUM.get(),     0.05f, sulfur);
        addOreTag("forge:ores/cobalt",      ModItems.CRYSTAL_COBALT.get(),      0.05f, sulfur);
        addOreTag("forge:ores/schrabidium", ModItems.CRYSTAL_SCHRABIDIUM.get(), 0.05f, sulfur);

        // Редкоземельные руды (forge:ores/rareground)
        addOreTag("forge:ores/rareground",  ModItems.CRYSTAL_RARE.get(),        0.05f, sulfur);

        // ═══════════════════════════════════════════════════════════════════
        // СПЕЦИАЛЬНЫЕ БЛОКИ-РУДЫ (фосфор, тикит, гравий с алмазами)
        // ═══════════════════════════════════════════════════════════════════
        // TODO: ModBlocks.ORE_NETHER_FIRE -> CRYSTAL_PHOSPHORUS, baseTime, prod 0.05, без кислоты.
        //       (Незер-руда фосфора — нужно добавить блок и тег.)
        // addItem(ModBlocks.ORE_NETHER_FIRE.get(), new ItemStack(ModItems.CRYSTAL_PHOSPHORUS.get()), 0.05f, BASE_TIME, null);

        // TODO: ModBlocks.ORE_TIKITE -> CRYSTAL_TRIXITE, baseTime, prod 0.05, серная.
        //       (Трикситовая руда — оригинал: ore_tikite block.)
        // addItem(ModBlocks.ORE_TIKITE.get(), new ItemStack(ModItems.CRYSTAL_TRIXITE.get()), 0.05f, BASE_TIME, sulfur);

        // TODO: ModBlocks.GRAVEL_DIAMOND -> CRYSTAL_DIAMOND, baseTime, prod 0.05, без кислоты.
        // addItem(ModBlocks.GRAVEL_DIAMOND.get(), new ItemStack(ModItems.CRYSTAL_DIAMOND.get()), 0.05f, BASE_TIME, null);

        // TODO: SRN.ingot() (шрараниевый ингот) -> CRYSTAL_SCHRARANIUM, baseTime, prod 0.05.
        //       (В оригинале это OreDictStack(SRN.ingot()) — нужен ингот шрараниума в ModIngots.)
        // addItem(ModItems.INGOT_SCHRARANIUM.get(), new ItemStack(ModItems.CRYSTAL_SCHRARANIUM.get()), 0.05f, BASE_TIME, null);

        // ═══════════════════════════════════════════════════════════════════
        // УТИЛИТАРНЫЕ ПРЕОБРАЗОВАНИЯ
        // ═══════════════════════════════════════════════════════════════════

        // Гнилое мясо → кожа (utilityTime, prod 0.25, перекись)
        addItem(Items.ROTTEN_FLESH, new ItemStack(Items.LEATHER), 0.25f, UTILITY_TIME, peroxide);

        // Булыжник → укреплённый камень (utilityTime, перекись)
        addItem(Items.COBBLESTONE, new ItemStack(ModBlocks.REINFORCED_STONE.get()), 0f, UTILITY_TIME, peroxide);

        // TODO: ModBlocks.GRAVEL_OBSIDIAN -> BRICK_OBSIDIAN, utilityTime.
        //       (Обсидиановый гравий — нужен блок ore_gravel_obsidian.)
        // addItem(ModBlocks.GRAVEL_OBSIDIAN.get(), new ItemStack(ModBlocks.BRICK_OBSIDIAN.get()), 0f, UTILITY_TIME, null);

        // Кость → 16 слизи (mixing, серная 1000 mB)
        addItem(Items.BONE, new ItemStack(Items.SLIME_BALL, 16), 0f, MIXING_TIME, fluid(ModFluids.SULFURIC_ACID, 1_000));

        // Чёрный краситель → 4 слизи (mixing, серная 250 mB)
        addItem(Items.BLACK_DYE, new ItemStack(Items.SLIME_BALL, 4), 0f, MIXING_TIME, fluid(ModFluids.SULFURIC_ACID, 250));

        // ═══════════════════════════════════════════════════════════════════
        // АНГЛОЯЗЫЧНЫЙ ПЕСОК / СТЕКЛО (через теги Forge)
        // ═══════════════════════════════════════════════════════════════════

        // TODO: песок -> ingot_fiberglass, utilityTime, prod 0.15.
        //       (Стекловолокно — нужен ModItems.INGOT_FIBERGLASS.)
        // addOreTag("forge:sand", ModItems.INGOT_FIBERGLASS.get(), 0.15f, null);

        // TODO: кремниевый ингот -> 2 кварца, utilityTime, prod 0.10, кислород 250 mB.
        //       (Нужен ингот кремния в ModIngots.SILICON или подобное.)
        // addItem(ModItems.INGOT_SILICON.get(), new ItemStack(Items.QUARTZ, 2), 0.10f, UTILITY_TIME, fluid(ModFluids.OXYGEN, 250));

        // ═══════════════════════════════════════════════════════════════════
        // РТУТЬ
        // ═══════════════════════════════════════════════════════════════════

        // Блок редстоуна → ингот ртути (baseTime, prod 0.25)
        // TODO: нужен ModItems.INGOT_MERCURY (в ModIngots пока MERCURY отсутствует).
        // addItem(Items.REDSTONE_BLOCK, new ItemStack(ModItems.INGOT_MERCURY.get()), 0.25f, BASE_TIME, null);

        // TODO: CINNABAR.crystal() -> 3 ингота ртути, baseTime, prod 0.25.
        //       (Циннабарит-кристалл существует в ModItems.CRYSTAL_CINNEBAR — а нужен ингот ртути.)
        // addItem(ModItems.CRYSTAL_CINNEBAR.get(), new ItemStack(ModItems.INGOT_MERCURY.get(), 3), 0.25f, BASE_TIME, null);

        // ═══════════════════════════════════════════════════════════════════
        // УГОЛЬ → ГРАФИТ
        // ═══════════════════════════════════════════════════════════════════

        // TODO: блок угля -> блок графита, baseTime.
        //       (Нужен ModBlocks.BLOCK_GRAPHITE. В ModIngots есть GRAPHITE — может быть только ингот.)
        // addItem(Items.COAL_BLOCK, new ItemStack(ModBlocks.BLOCK_GRAPHITE.get()), 0f, BASE_TIME, null);

        // ═══════════════════════════════════════════════════════════════════
        // АРМАТУРА → АРМБЕТОН
        // ═══════════════════════════════════════════════════════════════════

        // TODO: ModBlocks.REBAR -> CONCRETE_REBAR, 10 тиков, бетон 1000 mB.
        //       (REBAR блок отсутствует, есть только CONCRETE_REBAR.)
        // addItem(ModBlocks.REBAR.get(), new ItemStack(ModBlocks.CONCRETE_REBAR.get()), 0f, 10, fluid(ModFluids.CONCRETE, 1_000));

        // ═══════════════════════════════════════════════════════════════════
        // КАЛЬЦИЙ → ЦЕМЕНТ, МАЛАХИТ → СКРАП-МЕДЬ
        // ═══════════════════════════════════════════════════════════════════

        // TODO: powder_calcium -> 8 powder_cement, utilityTime, prod 0.10, REDMUD 75 mB.
        // addItem(ModItems.POWDER_CALCIUM.get(), new ItemStack(ModItems.POWDER_CEMENT.get(), 8), 0.10f, UTILITY_TIME, fluid(ModFluids.REDMUD, 75));

        // TODO: малахитовый ингот -> ItemScraps медный, 300 тиков, prod 0.10, серная 250 mB.
        // (MALACHITE ingot отсутствует. Также ItemScraps — целая система, требует портирования.)

        // ═══════════════════════════════════════════════════════════════════
        // BORAX (бура) → POWDER_BORON_TINY
        // ═══════════════════════════════════════════════════════════════════

        // TODO: BORAX.dust() -> 3 powder_boron_tiny, baseTime, prod 0.25, серная 500 mB.
        // addItem(ModItems.POWDER_BORAX.get(), new ItemStack(ModItems.POWDER_BORON_TINY.get(), 3), 0.25f, BASE_TIME, sulfur);

        // ═══════════════════════════════════════════════════════════════════
        // АДСКИЙ УГОЛЬ → ТВЁРДОЕ ТОПЛИВО
        // ═══════════════════════════════════════════════════════════════════

        // TODO: ModItems.COAL_INFERNAL -> SOLID_FUEL, utilityTime.
        //       (Нужны оба предмета.)
        // addItem(ModItems.COAL_INFERNAL.get(), new ItemStack(ModItems.SOLID_FUEL.get()), 0f, UTILITY_TIME, null);

        // ═══════════════════════════════════════════════════════════════════
        // ГНЕЙС (КАМЕНЬ) → ЛИТИЕВЫЙ ПОРОШОК
        // ═══════════════════════════════════════════════════════════════════

        // TODO: ModBlocks.STONE_GNEISS -> POWDER_LITHIUM, utilityTime, prod 0.25.
        // addItem(ModBlocks.STONE_GNEISS.get(), new ItemStack(ModItems.POWDER_LITHIUM.get()), 0.25f, UTILITY_TIME, null);

        // ═══════════════════════════════════════════════════════════════════
        // MUSTARDWILLOW → КАДМИЙ
        // ═══════════════════════════════════════════════════════════════════

        // TODO: plant_item:MUSTARDWILLOW -> POWDER_CADMIUM, 100 тиков, требует 10 шт. на крафт, радиосолвент 250 mB.
        // (Растение mustardwillow и ItemPlant система не портированы.)

        // ═══════════════════════════════════════════════════════════════════
        // НЕФТЯНОЙ СКРАП → НАГГЕТ МЫШЬЯКА
        // ═══════════════════════════════════════════════════════════════════

        // TODO: ModItems.SCRAP_OIL -> NUGGET_ARSENIC, 100 тиков, prod 0.30, требует 16 шт., радиосолвент 100 mB.
        // (Нефтяной скрап и наггет мышьяка не портированы.)

        // ═══════════════════════════════════════════════════════════════════
        // ФУЛЛЕРЕН → CFT ИНГОТ (углеродное волокно)
        // ═══════════════════════════════════════════════════════════════════

        // TODO: powder_ash:FULLERENE -> INGOT_CFT, baseTime, prod 0.10, требует 4 шт., ксилол 1000 mB.
        // (Не портированы.)

        // ═══════════════════════════════════════════════════════════════════
        // ДРАГОЦЕННЫЕ ПЫЛИ → ОБРАТНО В САМОЦВЕТЫ
        // ═══════════════════════════════════════════════════════════════════

        // TODO: powder_diamond -> diamond, utilityTime.
        // addItem(ModItems.POWDER_DIAMOND.get(), new ItemStack(Items.DIAMOND), 0f, UTILITY_TIME, null);

        // TODO: powder_emerald -> emerald, utilityTime.
        // addItem(ModItems.POWDER_EMERALD.get(), new ItemStack(Items.EMERALD), 0f, UTILITY_TIME, null);

        // TODO: powder_lapis -> blue dye, utilityTime.
        // addItem(ModItems.POWDER_LAPIS.get(), new ItemStack(Items.BLUE_DYE), 0f, UTILITY_TIME, null);

        // ═══════════════════════════════════════════════════════════════════
        // SEMTEX, DESH, METEORITE
        // ═══════════════════════════════════════════════════════════════════

        // TODO: powder_semtex_mix -> ingot_semtex, baseTime.
        // TODO: powder_desh_ready -> ingot_desh, baseTime.
        // TODO: powder_meteorite -> fragment_meteorite, utilityTime.
        // (Нужны соответствующие порошки и слитки.)

        // ═══════════════════════════════════════════════════════════════════
        // КАДМИЙ → РЕЗИНА
        // ═══════════════════════════════════════════════════════════════════

        // TODO: cadmium dust -> 16 ingot_rubber, utilityTime, FISHOIL 4000 mB.
        // (Cadmium dust + ingot_rubber отсутствуют как именованные предметы.)

        // TODO: latex ingot -> ingot_rubber, mixing, prod 0.15, SOURGAS 25 mB.
        // (INGOT_LATEX и INGOT_RUBBER отсутствуют.)

        // ═══════════════════════════════════════════════════════════════════
        // ОПИЛКИ → КОРДИТ (взрывчатка)
        // ═══════════════════════════════════════════════════════════════════

        // TODO: powder_sawdust -> CORDITE, mixing, prod 0.25, NITROGLYCERIN 250 mB.
        // (POWDER_SAWDUST и CORDITE не портированы.)

        // ═══════════════════════════════════════════════════════════════════
        // ОСМИРИДИЙ
        // ═══════════════════════════════════════════════════════════════════

        // TODO: powder_impure_osmiridium -> CRYSTAL_OSMIRIDIUM, baseTime, SCHRABIDIC 1000 mB.
        // (Грязный порошок осмиридия отсутствует — но CRYSTAL_OSMIRIDIUM уже есть.)

        // ═══════════════════════════════════════════════════════════════════
        // SCRAP_PLASTIC → CIRCUIT_STAR_PIECE (12 типов)
        // ═══════════════════════════════════════════════════════════════════

        // TODO: 12 рецептов scrap_plastic[i] -> circuit_star_piece[i], baseTime.
        // (Система ScrapType с метаданными — требует портирования.)

        // ═══════════════════════════════════════════════════════════════════
        // METEORITE SWORD: TREATED → ETCHED
        // ═══════════════════════════════════════════════════════════════════

        // TODO: meteorite_sword_treated -> meteorite_sword_etched, baseTime.
        // (Нужны 2 варианта меча — обработанный и протравленный.)

        // ═══════════════════════════════════════════════════════════════════
        // КРАСИТЕЛИ ИЗ ПЫЛЕЙ + 3 МАСЛА
        // ═══════════════════════════════════════════════════════════════════

        // В оригинале: для каждого из 3 масел (WOODOIL, FISHOIL, LIGHTOIL по 100 mB)
        // 6 рецептов: COAL.dust → BLACK, TI.dust → WHITE, IRON.dust → RED,
        //             W.dust → YELLOW, CU.dust → GREEN, CO.dust → BLUE.
        // TODO: нужен ModItems.CHEMICAL_DYE с подтипами (BLACK/WHITE/RED/YELLOW/GREEN/BLUE) + ModPowders для всех.

        // ═══════════════════════════════════════════════════════════════════
        // СМОЛА (oil_tar): CRUDE/CRACK/PARAFFIN → WAX → PELLET_CHARGED / PILL_RED
        // ═══════════════════════════════════════════════════════════════════

        // TODO: tar_crude -> tar_wax, 20 тиков, хлор 250 mB.
        // TODO: tar_crack -> tar_wax, 20 тиков, хлор 100 mB.
        // TODO: tar_paraffin -> tar_wax, 20 тиков, хлор 100 mB.
        // TODO: tar_wax -> pellet_charged, 200 тиков, IONGEL 500 mB.
        // TODO: tar_paraffin -> pill_red, 200 тиков, ESTRADIOL 250 mB.
        // (Вся система oil_tar и enum EnumTarType не портирована.)

        // ═══════════════════════════════════════════════════════════════════
        // ПЕСОК → ГЛИНА, ВЗРЫВЧАТКА ИЗ КВАРЦА
        // ═══════════════════════════════════════════════════════════════════

        // TODO: forge:sand -> CLAY, 20 тиков, COLLOID 1000 mB.
        // addOreTag("forge:sand", Items.CLAY, 0f, fluid(ModFluids.COLLOID, 1_000));   // если addOreTag поддержит ItemLike → доделать

        // TODO: SAND_MIX:QUARTZ -> 16 ball_dynamite, 20 тиков, NITROGLYCERIN 1000 mB.
        // TODO: NETHERQUARTZ.dust() -> 4 ball_dynamite, 20 тиков, NITROGLYCERIN 250 mB.
        // (BALL_DYNAMITE отсутствует, BALL_TNT — есть, но это другое.)

        // ═══════════════════════════════════════════════════════════════════
        // BEDROCK-РУДЫ — старая ветка (ore_centrifuged → ore_cleaned, и т.д.)
        // ═══════════════════════════════════════════════════════════════════

        // TODO: для каждого из EnumBedrockOre (метаданные 0..N):
        //   ore_centrifuged   -> ore_cleaned       (200t, без кислоты)
        //   ore_separated     -> ore_purified      (200t, серная 500)
        //   ore_separated     -> ore_nitrated      (200t, азотная 500)
        //   ore_nitrocrystalline -> ore_deepcleaned (200t, SOLVENT 500)
        //   ore_nitrocrystalline -> ore_seared      (200t, RADIOSOLVENT 500)
        // (Метапредметы ore_centrifuged / ore_separated / ore_nitrocrystalline / ore_cleaned / ore_purified /
        //  ore_nitrated / ore_deepcleaned / ore_seared отсутствуют.)
        // Использовать sulfur (sulfuric 500), nitric (azotic 500), organic (solvent 500), hiperf (radiosolvent 500).

        // ═══════════════════════════════════════════════════════════════════
        // BEDROCK-РУДЫ — новая ветка ItemBedrockOreNew (BedrockOreGrade × BedrockOreType)
        // ═══════════════════════════════════════════════════════════════════

        // TODO: огромный блок рецептов для каждой пары (Grade, Type):
        //   BASE / BASE_ROASTED -> BASE_WASHED                    (washing, вода 250)
        //   PRIMARY / PRIMARY_ROASTED -> PRIMARY_SULFURIC         (bedrock, серная 250)
        //   PRIMARY / PRIMARY_ROASTED / PRIMARY_NOSULFURIC -> PRIMARY_SOLVENT (bedrock, SOLVENT 250)
        //   PRIMARY / PRIMARY_ROASTED / PRIMARY_NOSULFURIC / PRIMARY_NOSOLVENT -> PRIMARY_RAD (bedrock, RADIOSOLVENT 250)
        //   SULFURIC_BYPRODUCT/ROASTED/ARC -> SULFURIC_WASHED     (washing, вода 250, требует 4)
        //   SOLVENT_BYPRODUCT/ROASTED/ARC -> SOLVENT_WASHED       (washing, вода 250, требует 4)
        //   RAD_BYPRODUCT/ROASTED/ARC -> RAD_WASHED               (washing, вода 250, требует 4)
        //   PRIMARY/PRIMARY_ROASTED/PRIMARY_SULFURIC/PRIMARY_NOSULFURIC/PRIMARY_SOLVENT/PRIMARY_NOSOLVENT/PRIMARY_RAD/PRIMARY_NORAD
        //     -> PRIMARY_FIRST   (bedrock, водород 250)
        //     -> PRIMARY_SECOND  (bedrock, хлор 250)
        //   CRUMBS -> BASE                                        (bedrock, SLOP 1000, требует 64)
        // (Класс ItemBedrockOreNew с enum BedrockOreGrade и BedrockOreType — НЕ портирован.
        //  Это самый большой блок рецептов из оригинала, ~80 строк per ore type.)

        // ═══════════════════════════════════════════════════════════════════
        // КОНЕЦ
        // ═══════════════════════════════════════════════════════════════════
    }

    // ───────────────────────────── helpers ─────────────────────────────

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return new FluidStack(entry.getSource(), amountMb);
    }

    private static void addOreTag(String tagId, Item output, float productivity, @Nullable FluidStack acid) {
        addOreTag(tagId, new ItemStack(output), productivity, BASE_TIME, acid, 1);
    }

    private static void addOreTag(String tagId, ItemStack output, float productivity, int duration,
                                  @Nullable FluidStack acid, int inputCount) {
        TagKey<Item> tag = ItemTags.create(ResourceLocation.tryParse(tagId));
        Ingredient ing = Ingredient.of(tag);
        RECIPES.add(new CrystallizerRecipe(ing, inputCount, acid, output, duration, productivity));
    }

    private static void addItem(ItemLike input, ItemStack output, float productivity, int duration,
                                @Nullable FluidStack acid) {
        addItem(input, output, productivity, duration, acid, 1);
    }

    private static void addItem(ItemLike input, ItemStack output, float productivity, int duration,
                                @Nullable FluidStack acid, int inputCount) {
        Ingredient ing = Ingredient.of(input);
        RECIPES.add(new CrystallizerRecipe(ing, inputCount, acid, output, duration, productivity));
    }

}