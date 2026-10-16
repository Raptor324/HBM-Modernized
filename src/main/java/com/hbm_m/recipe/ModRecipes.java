package com.hbm_m.recipe;

import com.hbm_m.lib.RefStrings;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;



public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(RefStrings.MODID, Registries.RECIPE_SERIALIZER);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(RefStrings.MODID, Registries.RECIPE_TYPE);




    public static final RegistrySupplier<RecipeSerializer<AssemblerRecipe>> ASSEMBLER_SERIALIZER =
            SERIALIZERS.register("assembler", () -> AssemblerRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<AssemblerRecipe>> ASSEMBLER_TYPE =
            RECIPE_TYPES.register("assembler", () -> AssemblerRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<ChemicalPlantRecipe>> CHEMICAL_PLANT_SERIALIZER =
            SERIALIZERS.register("chemical_plant", () -> ChemicalPlantRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ChemicalPlantRecipe>> CHEMICAL_PLANT_TYPE =
            RECIPE_TYPES.register("chemical_plant", () -> ChemicalPlantRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<PressRecipe>> PRESS_SERIALIZER =
            SERIALIZERS.register("press", () -> PressRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<PressRecipe>> PRESS_TYPE =
            RECIPE_TYPES.register("press", () -> PressRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<ArcFurnaceRecipe>> ARC_FURNACE_SERIALIZER =
            SERIALIZERS.register("arc_furnace", () -> ArcFurnaceRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ArcFurnaceRecipe>> ARC_FURNACE_TYPE =
            RECIPE_TYPES.register("arc_furnace", () -> ArcFurnaceRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<AmmoPressRecipe>> AMMO_PRESS_SERIALIZER =
            SERIALIZERS.register("ammo_press", () -> AmmoPressRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<AmmoPressRecipe>> AMMO_PRESS_TYPE =
            RECIPE_TYPES.register("ammo_press", () -> AmmoPressRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<PurexRecipe>> PUREX_SERIALIZER =
            SERIALIZERS.register("purex", () -> PurexRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<PurexRecipe>> PUREX_TYPE =
            RECIPE_TYPES.register("purex", () -> PurexRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER =
            SERIALIZERS.register("blast_furnace", () -> BlastFurnaceRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<BlastFurnaceRecipe>> BLAST_FURNACE_TYPE =
            RECIPE_TYPES.register("blast_furnace", () -> BlastFurnaceRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<ShredderRecipe>> SHREDDER_SERIALIZER =
            SERIALIZERS.register("shredding", () -> ShredderRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ShredderRecipe>> SHREDDER_TYPE =
            RECIPE_TYPES.register("shredding", () -> ShredderRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<CentrifugeRecipe>> CENTRIFUGE_SERIALIZER =
            SERIALIZERS.register("centrifuge", () -> CentrifugeRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CentrifugeRecipe>> CENTRIFUGE_TYPE =
            RECIPE_TYPES.register("centrifuge", () -> CentrifugeRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<AnvilRecipe>> ANVIL_SERIALIZER =
            SERIALIZERS.register("anvil", () -> AnvilRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<AnvilRecipe>> ANVIL_TYPE =
            RECIPE_TYPES.register("anvil", () -> AnvilRecipe.Type.INSTANCE);

    public static final RegistrySupplier<RecipeSerializer<CombinationOvenRecipe>> COMBINATION_OVEN_SERIALIZER =
            SERIALIZERS.register("combination_oven", () -> CombinationOvenRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CombinationOvenRecipe>> COMBINATION_OVEN_TYPE =
            RECIPE_TYPES.register("combination_oven", () -> CombinationOvenRecipe.Type.INSTANCE);

    // Crystallizer — рудный окислитель. ID сериализатора/типа = "crystallizer" (см. CrystallizerRecipe.Type.ID).
    public static final RegistrySupplier<RecipeSerializer<CrystallizerRecipe>> CRYSTALLIZER_SERIALIZER =
            SERIALIZERS.register("crystallizer", () -> CrystallizerRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CrystallizerRecipe>> CRYSTALLIZER_TYPE =
            RECIPE_TYPES.register("crystallizer", () -> CrystallizerRecipe.Type.INSTANCE);

    // Cyclotron — циклотрон. ID = "cyclotron" (см. CyclotronRecipe.Type.ID).
    public static final RegistrySupplier<RecipeSerializer<CyclotronRecipe>> CYCLOTRON_SERIALIZER =
            SERIALIZERS.register("cyclotron", () -> CyclotronRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CyclotronRecipe>> CYCLOTRON_TYPE =
            RECIPE_TYPES.register("cyclotron", () -> CyclotronRecipe.Type.INSTANCE);

    // Mixer — промышленный миксер. ID = "mixer" (см. MixerRecipe.Type.ID).
    public static final RegistrySupplier<RecipeSerializer<MixerRecipe>> MIXER_SERIALIZER =
            SERIALIZERS.register("mixer", () -> MixerRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<MixerRecipe>> MIXER_TYPE =
            RECIPE_TYPES.register("mixer", () -> MixerRecipe.Type.INSTANCE);

    // CrucibleSmelting — тигель-плавка предмета в расплавленный материал. ID = "crucible_smelting".
    public static final RegistrySupplier<RecipeSerializer<CrucibleSmeltingRecipe>> CRUCIBLE_SMELTING_SERIALIZER =
            SERIALIZERS.register("crucible_smelting", () -> CrucibleSmeltingRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CrucibleSmeltingRecipe>> CRUCIBLE_SMELTING_TYPE =
            RECIPE_TYPES.register("crucible_smelting", () -> CrucibleSmeltingRecipe.Type.INSTANCE);

    // MoltenAlloy — тигель-сплавление расплавленных материалов (MaterialStack → MaterialStack[]).
    // ID = "molten_alloy" (см. MoltenAlloyRecipe.Type.ID). Data-driven, material-based, не item/fluid.
    public static final RegistrySupplier<RecipeSerializer<MoltenAlloyRecipe>> MOLTEN_ALLOY_SERIALIZER =
            SERIALIZERS.register("molten_alloy", () -> MoltenAlloyRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<MoltenAlloyRecipe>> MOLTEN_ALLOY_TYPE =
            RECIPE_TYPES.register("molten_alloy", () -> MoltenAlloyRecipe.Type.INSTANCE);

    // MoldCasting — отливка в форме: пара (mold, material) → ItemStack. ID = "mold_casting".
    public static final RegistrySupplier<RecipeSerializer<MoldCastingRecipe>> MOLD_CASTING_SERIALIZER =
            SERIALIZERS.register("mold_casting", () -> MoldCastingRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<MoldCastingRecipe>> MOLD_CASTING_TYPE =
            RECIPE_TYPES.register("mold_casting", () -> MoldCastingRecipe.Type.INSTANCE);

    // ArcWelder — дуговая сварка. ID = "arc_welder" (см. ArcWelderRecipe.Type.ID).
    // Замена статике ArcWelderRecipes.recipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<ArcWelderRecipe>> ARC_WELDER_SERIALIZER =
            SERIALIZERS.register("arc_welder", () -> ArcWelderRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ArcWelderRecipe>> ARC_WELDER_TYPE =
            RECIPE_TYPES.register("arc_welder", () -> ArcWelderRecipe.Type.INSTANCE);

    // SolderingStation — паяльная станция. ID = "soldering_station" (см. SolderingRecipe.Type.ID).
    // Замена статике SolderingRecipes.recipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<SolderingRecipe>> SOLDERING_SERIALIZER =
            SERIALIZERS.register("soldering_station", () -> SolderingRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<SolderingRecipe>> SOLDERING_TYPE =
            RECIPE_TYPES.register("soldering_station", () -> SolderingRecipe.Type.INSTANCE);

    // GasCentrifuge — газовый центрифуг (JEI-only). ID = "gas_centrifuge" (см. GasCentrifugeRecipe.Type.ID).
    // Runtime использует cascade-enrichment через PseudoFluidType; этот рецепт — для статичного JEI-show.
    public static final RegistrySupplier<RecipeSerializer<GasCentrifugeRecipe>> GAS_CENTRIFUGE_SERIALIZER =
            SERIALIZERS.register("gas_centrifuge", () -> GasCentrifugeRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<GasCentrifugeRecipe>> GAS_CENTRIFUGE_TYPE =
            RECIPE_TYPES.register("gas_centrifuge", () -> GasCentrifugeRecipe.Type.INSTANCE);

    // Breeder — реактор-размножитель. ID = "breeder" (см. BreederRecipe.Type.ID).
    // Замена статике BreederRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<BreederRecipe>> BREEDER_SERIALIZER =
            SERIALIZERS.register("breeder", () -> BreederRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<BreederRecipe>> BREEDER_TYPE =
            RECIPE_TYPES.register("breeder", () -> BreederRecipe.Type.INSTANCE);

    // RadGen — радиационный генератор. ID = "radgen" (см. RadGenRecipe.Type.ID).
    public static final RegistrySupplier<RecipeSerializer<RadGenRecipe>> RADGEN_SERIALIZER =
            SERIALIZERS.register("radgen", () -> RadGenRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<RadGenRecipe>> RADGEN_TYPE =
            RECIPE_TYPES.register("radgen", () -> RadGenRecipe.Type.INSTANCE);

    // ExposureChamber — экспозиционная камера. ID = "exposure_chamber" (см. ExposureChamberRecipe.Type.ID).
    public static final RegistrySupplier<RecipeSerializer<ExposureChamberRecipe>> EXPOSURE_CHAMBER_SERIALIZER =
            SERIALIZERS.register("exposure_chamber", () -> ExposureChamberRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ExposureChamberRecipe>> EXPOSURE_CHAMBER_TYPE =
            RECIPE_TYPES.register("exposure_chamber", () -> ExposureChamberRecipe.Type.INSTANCE);

    // Silex — SILEX-обогащение. ID = "silex" (см. SilexRecipe.Type.ID).
    public static final RegistrySupplier<RecipeSerializer<SilexRecipe>> SILEX_SERIALIZER =
            SERIALIZERS.register("silex", () -> SilexRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<SilexRecipe>> SILEX_TYPE =
            RECIPE_TYPES.register("silex", () -> SilexRecipe.Type.INSTANCE);

    // Coker — кокер. ID = "coker" (см. CokerRecipe.Type.ID).
    // Замена статике CokerRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<CokerRecipe>> COKER_SERIALIZER =
            SERIALIZERS.register("coker", () -> CokerRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CokerRecipe>> COKER_TYPE =
            RECIPE_TYPES.register("coker", () -> CokerRecipe.Type.INSTANCE);

    // Compressor — компрессор. ID = "compressor" (см. CompressorRecipe.Type.ID).
    // Замена статике CompressorRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<CompressorRecipe>> COMPRESSOR_SERIALIZER =
            SERIALIZERS.register("compressor", () -> CompressorRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CompressorRecipe>> COMPRESSOR_TYPE =
            RECIPE_TYPES.register("compressor", () -> CompressorRecipe.Type.INSTANCE);

    // CrackingTower — крекинг-колонна. ID = "cracking_tower" (см. CrackingTowerRecipe.Type.ID).
    // Замена статике CrackingTowerRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<CrackingTowerRecipe>> CRACKING_TOWER_SERIALIZER =
            SERIALIZERS.register("cracking_tower", () -> CrackingTowerRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CrackingTowerRecipe>> CRACKING_TOWER_TYPE =
            RECIPE_TYPES.register("cracking_tower", () -> CrackingTowerRecipe.Type.INSTANCE);

    // FractionTower — фракционная колонна. ID = "fraction_tower" (см. FractionTowerRecipe.Type.ID).
    // Замена статике FractionTowerRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<FractionTowerRecipe>> FRACTION_TOWER_SERIALIZER =
            SERIALIZERS.register("fraction_tower", () -> FractionTowerRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<FractionTowerRecipe>> FRACTION_TOWER_TYPE =
            RECIPE_TYPES.register("fraction_tower", () -> FractionTowerRecipe.Type.INSTANCE);

    // Hydrotreater — гидроочиститель. ID = "hydrotreater" (см. HydrotreaterRecipe.Type.ID).
    // Замена статике HydrotreaterRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<HydrotreaterRecipe>> HYDROTREATER_SERIALIZER =
            SERIALIZERS.register("hydrotreater", () -> HydrotreaterRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<HydrotreaterRecipe>> HYDROTREATER_TYPE =
            RECIPE_TYPES.register("hydrotreater", () -> HydrotreaterRecipe.Type.INSTANCE);

    // CatalyticReformer — каталитический риформер. ID = "catalytic_reformer" (см. CatalyticReformerRecipe.Type.ID).
    // Замена статике CatalyticReformerRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<CatalyticReformerRecipe>> CATALYTIC_REFORMER_SERIALIZER =
            SERIALIZERS.register("catalytic_reformer", () -> CatalyticReformerRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<CatalyticReformerRecipe>> CATALYTIC_REFORMER_TYPE =
            RECIPE_TYPES.register("catalytic_reformer", () -> CatalyticReformerRecipe.Type.INSTANCE);

    // Liquefactor — ликвейфактор. ID = "liquefactor" (см. LiquefactorRecipe.Type.ID).
    // Замена статике LiquefactorRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<LiquefactorRecipe>> LIQUEFACTOR_SERIALIZER =
            SERIALIZERS.register("liquefactor", () -> LiquefactorRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<LiquefactorRecipe>> LIQUEFACTOR_TYPE =
            RECIPE_TYPES.register("liquefactor", () -> LiquefactorRecipe.Type.INSTANCE);

    // PyroOven — пиролизная печь. ID = "pyro_oven" (см. PyroOvenRecipe.Type.ID).
    // Замена статике PyroOvenRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<PyroOvenRecipe>> PYRO_OVEN_SERIALIZER =
            SERIALIZERS.register("pyro_oven", () -> PyroOvenRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<PyroOvenRecipe>> PYRO_OVEN_TYPE =
            RECIPE_TYPES.register("pyro_oven", () -> PyroOvenRecipe.Type.INSTANCE);

    // Radiolysis — радиолиз. ID = "radiolysis" (см. RadiolysisRecipe.Type.ID).
    // Замена статике RadiolysisRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<RadiolysisRecipe>> RADIOLYSIS_SERIALIZER =
            SERIALIZERS.register("radiolysis", () -> RadiolysisRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<RadiolysisRecipe>> RADIOLYSIS_TYPE =
            RECIPE_TYPES.register("radiolysis", () -> RadiolysisRecipe.Type.INSTANCE);

    // RotaryFurnace — вращающаяся печь. ID = "rotary_furnace" (см. RotaryFurnaceRecipe.Type.ID).
    // Замена статике RotaryFurnaceRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<RotaryFurnaceRecipe>> ROTARY_FURNACE_SERIALIZER =
            SERIALIZERS.register("rotary_furnace", () -> RotaryFurnaceRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<RotaryFurnaceRecipe>> ROTARY_FURNACE_TYPE =
            RECIPE_TYPES.register("rotary_furnace", () -> RotaryFurnaceRecipe.Type.INSTANCE);

    // Solidification — солидификатор. ID = "solidification" (см. SolidificationRecipe.Type.ID).
    // Замена статике SolidificationRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<SolidificationRecipe>> SOLIDIFICATION_SERIALIZER =
            SERIALIZERS.register("solidification", () -> SolidificationRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<SolidificationRecipe>> SOLIDIFICATION_TYPE =
            RECIPE_TYPES.register("solidification", () -> SolidificationRecipe.Type.INSTANCE);

    // VacuumDistill — вакуумная перегонка. ID = "vacuum_distill" (см. VacuumDistillRecipe.Type.ID).
    // Замена статике VacuumDistillRecipes — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<VacuumDistillRecipe>> VACUUM_DISTILL_SERIALIZER =
            SERIALIZERS.register("vacuum_distill", () -> VacuumDistillRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<VacuumDistillRecipe>> VACUUM_DISTILL_TYPE =
            RECIPE_TYPES.register("vacuum_distill", () -> VacuumDistillRecipe.Type.INSTANCE);

    // ElectrolyserFluid — электролизёр (жидкостный рецепт). ID = "electrolyser_fluid" (см. ElectrolyserFluidRecipe.Type.ID).
    // Замена статике ElectrolyserRecipes (fluid) — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<ElectrolyserFluidRecipe>> ELECTROLYSER_FLUID_SERIALIZER =
            SERIALIZERS.register("electrolyser_fluid", () -> ElectrolyserFluidRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ElectrolyserFluidRecipe>> ELECTROLYSER_FLUID_TYPE =
            RECIPE_TYPES.register("electrolyser_fluid", () -> ElectrolyserFluidRecipe.Type.INSTANCE);

    // ElectrolyserMetal — электролизёр (металлический рецепт). ID = "electrolyser_metal" (см. ElectrolyserMetalRecipe.Type.ID).
    // Замена статике ElectrolyserRecipes (metal) — теперь data-driven (JSON).
    public static final RegistrySupplier<RecipeSerializer<ElectrolyserMetalRecipe>> ELECTROLYSER_METAL_SERIALIZER =
            SERIALIZERS.register("electrolyser_metal", () -> ElectrolyserMetalRecipe.Serializer.INSTANCE);

    public static final RegistrySupplier<RecipeType<ElectrolyserMetalRecipe>> ELECTROLYSER_METAL_TYPE =
            RECIPE_TYPES.register("electrolyser_metal", () -> ElectrolyserMetalRecipe.Type.INSTANCE);

    public static void init() {
        SERIALIZERS.register();
        RECIPE_TYPES.register();
    }

    /**
     * Диагностика загрузки рецептов — вызывается из {@link MainRegistry#commonSetup()},
     * т.е. ПОСЛЕ RegisterEvent, когда Deferred-записи уже разрешены в реестрах.
     */
    //? if >= 1.21.1 {
    /*public static void debugRecipeSerializerRegistry() {
        try {
            var reg = net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER;
            System.err.println("[HBM DEBUG] commonSetup: ASSEMBLER_SERIALIZER present=" + ASSEMBLER_SERIALIZER.isPresent()
                    + " id=" + ASSEMBLER_SERIALIZER.getId());
            System.err.println("[HBM DEBUG] commonSetup: RECIPE_SERIALIZER registry contains hbm_m:assembler = "
                    + (reg.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", "assembler")) != null));

            // Manual codec test: parse a minimal assembler recipe JSON via Recipe.CODEC
            // to see the exact dispatch error.
            String testJson = "{"
                + "\"type\": \"hbm_m:assembler\","
                + "\"duration\": 100,"
                + "\"ingredients\": [ { \"item\": \"minecraft:iron_ingot\", \"count\": 1 } ],"
                + "\"output\": { \"item\": \"minecraft:iron_nugget\", \"count\": 9 },"
                + "\"power\": 50"
                + "}";
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(testJson).getAsJsonObject();
            com.mojang.serialization.DataResult<net.minecraft.world.item.crafting.Recipe<?>> result =
                    net.minecraft.world.item.crafting.Recipe.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, obj);
            result.resultOrPartial(err -> {
                System.err.println("[HBM DEBUG] Recipe.CODEC.parse FAILED: " + err);
            }).ifPresent(recipe -> {
                System.err.println("[HBM DEBUG] Recipe.CODEC.parse SUCCESS: " + recipe.getClass().getSimpleName()
                        + " type=" + recipe.getType());
            });

            // Test CONDITIONAL_CODEC — this is what RecipeManager.apply actually uses.
            com.mojang.serialization.DataResult<java.util.Optional<net.neoforged.neoforge.common.conditions.WithConditions<net.minecraft.world.item.crafting.Recipe<?>>>> condResult =
                    net.minecraft.world.item.crafting.Recipe.CONDITIONAL_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, obj);
            condResult.resultOrPartial(err -> {
                System.err.println("[HBM DEBUG] CONDITIONAL_CODEC.parse FAILED: " + err);
            }).ifPresent(opt -> {
                if (opt.isPresent()) {
                    System.err.println("[HBM DEBUG] CONDITIONAL_CODEC.parse SUCCESS: WithConditions present, recipe="
                            + opt.get().carrier().getClass().getSimpleName());
                } else {
                    System.err.println("[HBM DEBUG] CONDITIONAL_CODEC.parse returned Optional.empty() — conditions not met!");
                }
            });
        } catch (Throwable t) {
            System.err.println("[HBM DEBUG] commonSetup: RECIPE_SERIALIZER check failed: " + t);
            t.printStackTrace();
        }
    }
    *///?}
}
