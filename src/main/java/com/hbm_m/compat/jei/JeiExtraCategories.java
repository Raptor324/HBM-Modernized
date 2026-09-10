package com.hbm_m.compat.jei;

//? if forge || neoforge {
import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.BreederRecipe;
import com.hbm_m.recipe.CatalyticReformerRecipe;
import com.hbm_m.recipe.CokerRecipe;
import com.hbm_m.recipe.CombinationOvenRecipe;
import com.hbm_m.recipe.FractionTowerRecipe;
import com.hbm_m.recipe.HydrotreaterRecipe;
import com.hbm_m.recipe.LiquefactorRecipe;
import com.hbm_m.recipe.MixerRecipe;
import com.hbm_m.recipe.MoldCastingRecipe;
import com.hbm_m.recipe.MoltenAlloyRecipe;
import com.hbm_m.recipe.PyroOvenRecipe;
import com.hbm_m.recipe.RadGenRecipe;
import com.hbm_m.recipe.SilexRecipe;
import com.hbm_m.recipe.SolidificationRecipe;
import com.hbm_m.recipe.VacuumDistillRecipe;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * Категории машин, у которых их не было: пятнадцать типов рецептов на 486 записей просто
 * не показывались в JEI. Все, кроме литья и сплавления, укладываются в
 * {@link JeiSimpleMachineCategory}, поэтому здесь только описание входов и выходов.
 */
public final class JeiExtraCategories {

    private JeiExtraCategories() {}

    public static final RecipeType<BreederRecipe> BREEDER =
            RecipeType.create(RefStrings.MODID, "breeder", BreederRecipe.class);
    public static final RecipeType<CatalyticReformerRecipe> CATALYTIC_REFORMER =
            RecipeType.create(RefStrings.MODID, "catalytic_reformer", CatalyticReformerRecipe.class);
    public static final RecipeType<CokerRecipe> COKER =
            RecipeType.create(RefStrings.MODID, "coker", CokerRecipe.class);
    public static final RecipeType<CombinationOvenRecipe> COMBINATION_OVEN =
            RecipeType.create(RefStrings.MODID, "combination_oven", CombinationOvenRecipe.class);
    public static final RecipeType<FractionTowerRecipe> FRACTION_TOWER =
            RecipeType.create(RefStrings.MODID, "fraction_tower", FractionTowerRecipe.class);
    public static final RecipeType<HydrotreaterRecipe> HYDROTREATER =
            RecipeType.create(RefStrings.MODID, "hydrotreater", HydrotreaterRecipe.class);
    public static final RecipeType<LiquefactorRecipe> LIQUEFACTOR =
            RecipeType.create(RefStrings.MODID, "liquefactor", LiquefactorRecipe.class);
    public static final RecipeType<MixerRecipe> MIXER =
            RecipeType.create(RefStrings.MODID, "mixer", MixerRecipe.class);
    public static final RecipeType<PyroOvenRecipe> PYRO_OVEN =
            RecipeType.create(RefStrings.MODID, "pyro_oven", PyroOvenRecipe.class);
    public static final RecipeType<RadGenRecipe> RADGEN =
            RecipeType.create(RefStrings.MODID, "radgen", RadGenRecipe.class);
    public static final RecipeType<SilexRecipe> SILEX =
            RecipeType.create(RefStrings.MODID, "silex", SilexRecipe.class);
    public static final RecipeType<SolidificationRecipe> SOLIDIFICATION =
            RecipeType.create(RefStrings.MODID, "solidification", SolidificationRecipe.class);
    public static final RecipeType<VacuumDistillRecipe> VACUUM_DISTILL =
            RecipeType.create(RefStrings.MODID, "vacuum_distill", VacuumDistillRecipe.class);

    private static ItemStack machine(net.minecraft.world.level.block.Block block) {
        return new ItemStack(block);
    }

    private static void addFluid(List<JeiMachineView.FluidAmount> list, Fluid fluid, int mb) {
        if (fluid != null && mb > 0) list.add(new JeiMachineView.FluidAmount(fluid, mb));
    }

    private static List<JeiMachineView.FluidAmount> fluids(Object... pairs) {
        List<JeiMachineView.FluidAmount> list = new ArrayList<>(pairs.length / 2);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            addFluid(list, (Fluid) pairs[i], (Integer) pairs[i + 1]);
        }
        return list;
    }

    private static List<ItemStack> items(ItemStack... stacks) {
        List<ItemStack> list = new ArrayList<>(stacks.length);
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) list.add(stack);
        }
        return list;
    }

    public static List<IRecipeCategory<?>> create(IGuiHelper helper) {
        List<IRecipeCategory<?>> out = new ArrayList<>();

        out.add(new JeiSimpleMachineCategory<>(helper, BREEDER, "block.hbm_m.breeder",
                new ItemStack(ModItems.BREEDER.get()), new JeiMachineView<BreederRecipe>() {
            @Override public List<ItemInput> itemInputs(BreederRecipe r) {
                return List.of(new ItemInput(r.getInput(), 1));
            }
            @Override public List<ItemStack> itemOutputs(BreederRecipe r) { return items(r.getOutput()); }
            @Override public List<Component> notes(BreederRecipe r) {
                return List.of(Component.literal(r.getEnergyPerTick() + " HE/t"));
            }
        }).withNotes(1));

        out.add(new JeiSimpleMachineCategory<>(helper, CATALYTIC_REFORMER, "block.hbm_m.catalytic_reformer",
                new ItemStack(ModItems.CATALYTIC_REFORMER.get()), new JeiMachineView<CatalyticReformerRecipe>() {
            @Override public List<FluidAmount> fluidInputs(CatalyticReformerRecipe r) {
                return fluids(r.getInputFluid(), r.getInputMb());
            }
            @Override public List<FluidAmount> fluidOutputs(CatalyticReformerRecipe r) {
                return fluids(r.getOutputA(), r.getOutputAMb(),
                              r.getOutputB(), r.getOutputBMb(),
                              r.getOutputC(), r.getOutputCMb());
            }
        }));

        out.add(new JeiSimpleMachineCategory<>(helper, COKER, "block.hbm_m.coker",
                machine(ModBlocks.COKER.get()), new JeiMachineView<CokerRecipe>() {
            @Override public List<FluidAmount> fluidInputs(CokerRecipe r) {
                return fluids(r.getInputFluid(), r.getInputMb());
            }
            @Override public List<ItemStack> itemOutputs(CokerRecipe r) { return items(r.getOutput()); }
            @Override public List<FluidAmount> fluidOutputs(CokerRecipe r) {
                return fluids(r.getByproductFluid(), r.getByproductMb());
            }
        }));

        out.add(new JeiSimpleMachineCategory<>(helper, COMBINATION_OVEN, "block.hbm_m.combination_oven",
                machine(ModBlocks.COMBINATION_OVEN.get()), new JeiMachineView<CombinationOvenRecipe>() {
            @Override public List<ItemInput> itemInputs(CombinationOvenRecipe r) {
                return List.of(new ItemInput(r.getInput(), Math.max(1, r.getInputCount())));
            }
            @Override public List<FluidAmount> fluidInputs(CombinationOvenRecipe r) {
                return fluids(r.getFluid(), r.getFluidAmount());
            }
            @Override public List<ItemStack> itemOutputs(CombinationOvenRecipe r) { return items(r.getOutput()); }
            @Override public List<Component> notes(CombinationOvenRecipe r) {
                return List.of(Component.literal(r.getDuration() + " ticks"));
            }
        }).withNotes(1));

        out.add(new JeiSimpleMachineCategory<>(helper, FRACTION_TOWER, "block.hbm_m.fraction_tower",
                new ItemStack(ModItems.FRACTION_TOWER.get()), new JeiMachineView<FractionTowerRecipe>() {
            @Override public List<FluidAmount> fluidInputs(FractionTowerRecipe r) {
                return fluids(r.getInputFluid(), r.getInputMb());
            }
            @Override public List<FluidAmount> fluidOutputs(FractionTowerRecipe r) {
                return fluids(r.getOutputA(), r.getOutputAMb(), r.getOutputB(), r.getOutputBMb());
            }
        }));

        out.add(new JeiSimpleMachineCategory<>(helper, HYDROTREATER, "block.hbm_m.hydrotreater",
                new ItemStack(ModItems.HYDROTREATER.get()), new JeiMachineView<HydrotreaterRecipe>() {
            @Override public List<FluidAmount> fluidInputs(HydrotreaterRecipe r) {
                return fluids(r.getInputFluid(), r.getInputMb(),
                              r.getHydrogen() == null ? null : r.getHydrogen().getFluid(), r.getHydrogenMb());
            }
            @Override public List<FluidAmount> fluidOutputs(HydrotreaterRecipe r) {
                return fluids(r.getOutput(), r.getOutputMb(), r.getSourGas(), r.getSourGasMb());
            }
        }));

        out.add(new JeiSimpleMachineCategory<>(helper, LIQUEFACTOR, "block.hbm_m.liquefactor",
                new ItemStack(ModItems.LIQUEFACTOR.get()), new JeiMachineView<LiquefactorRecipe>() {
            @Override public List<ItemInput> itemInputs(LiquefactorRecipe r) {
                return List.of(new ItemInput(r.getInput(), 1));
            }
            @Override public List<FluidAmount> fluidOutputs(LiquefactorRecipe r) {
                return r.getOutput() == null ? List.of()
                        : fluids(r.getOutput().getFluid(), r.getOutputAmountMb());
            }
        }));

        out.add(new JeiSimpleMachineCategory<>(helper, MIXER, "block.hbm_m.mixer",
                new ItemStack(ModItems.MIXER.get()), new JeiMachineView<MixerRecipe>() {
            @Override public List<FluidAmount> fluidInputs(MixerRecipe r) {
                List<FluidAmount> list = new ArrayList<>(2);
                if (r.getInputA() != null && !r.getInputA().isEmpty()) list.add(FluidAmount.of(r.getInputA()));
                if (r.getInputB() != null && !r.getInputB().isEmpty()) list.add(FluidAmount.of(r.getInputB()));
                return list;
            }
            @Override public List<FluidAmount> fluidOutputs(MixerRecipe r) {
                return r.getOutput() == null || r.getOutput().isEmpty()
                        ? List.of() : List.of(FluidAmount.of(r.getOutput()));
            }
            @Override public List<Component> notes(MixerRecipe r) {
                return List.of(Component.literal(r.getDuration() + " ticks, " + r.getEnergyPerTick() + " HE/t"));
            }
        }).withNotes(1));

        out.add(new JeiSimpleMachineCategory<>(helper, PYRO_OVEN, "block.hbm_m.pyrooven",
                machine(ModBlocks.PYROOVEN.get()), new JeiMachineView<PyroOvenRecipe>() {
            @Override public List<ItemInput> itemInputs(PyroOvenRecipe r) {
                return r.getInputItem() == null || r.getInputItem().isEmpty() ? List.of()
                        : List.of(new ItemInput(r.getInputItem(), Math.max(1, r.getInputItemCount())));
            }
            @Override public List<FluidAmount> fluidInputs(PyroOvenRecipe r) {
                return fluids(r.getInputFluid(), r.getInputFluidMb());
            }
            @Override public List<ItemStack> itemOutputs(PyroOvenRecipe r) { return items(r.getOutputItem()); }
            @Override public List<FluidAmount> fluidOutputs(PyroOvenRecipe r) {
                return fluids(r.getOutputFluid(), r.getOutputFluidMb());
            }
            @Override public List<Component> notes(PyroOvenRecipe r) {
                return List.of(Component.literal(r.getDuration() + " ticks"));
            }
        }).withNotes(1));

        out.add(new JeiSimpleMachineCategory<>(helper, RADGEN, "block.hbm_m.radgen",
                machine(ModBlocks.RADGEN.get()), new JeiMachineView<RadGenRecipe>() {
            @Override public List<ItemInput> itemInputs(RadGenRecipe r) {
                return List.of(new ItemInput(r.getInput(), 1));
            }
            @Override public List<ItemStack> itemOutputs(RadGenRecipe r) { return items(r.getOutput()); }
            @Override public List<Component> notes(RadGenRecipe r) {
                return List.of(Component.literal(r.getDuration() + " ticks, " + r.getPower() + " HE/t"));
            }
        }).withNotes(1));

        out.add(new JeiSimpleMachineCategory<>(helper, SILEX, "block.hbm_m.silex",
                new ItemStack(ModItems.SILEX.get()), new JeiMachineView<SilexRecipe>() {
            @Override public List<ItemInput> itemInputs(SilexRecipe r) {
                return List.of(new ItemInput(r.getInput(), 1));
            }
            @Override public List<FluidAmount> fluidInputs(SilexRecipe r) {
                return fluids(ModFluids.PEROXIDE.getSource(), r.getPeroxideMb());
            }
            @Override public List<ItemStack> itemOutputs(SilexRecipe r) {
                List<ItemStack> list = new ArrayList<>();
                for (SilexRecipe.WeightedOutput output : r.getOutputs()) {
                    if (!output.stack().isEmpty()) list.add(output.stack());
                }
                return list;
            }
            @Override public List<Component> notes(SilexRecipe r) {
                StringBuilder chances = new StringBuilder();
                int total = Math.max(1, r.getTotalWeight());
                for (SilexRecipe.WeightedOutput output : r.getOutputs()) {
                    if (output.stack().isEmpty()) continue;
                    if (chances.length() > 0) chances.append(", ");
                    chances.append(Math.round(output.weight() * 100.0 / total)).append('%');
                }
                return List.of(Component.literal(r.getDuration() + " ticks"),
                               Component.literal(chances.toString()));
            }
        }).withNotes(2));

        out.add(new JeiSimpleMachineCategory<>(helper, SOLIDIFICATION, "block.hbm_m.solidifier",
                machine(ModBlocks.SOLIDIFIER.get()), new JeiMachineView<SolidificationRecipe>() {
            @Override public List<FluidAmount> fluidInputs(SolidificationRecipe r) {
                return r.getInput() == null ? List.of()
                        : fluids(r.getInput().getFluid(), r.getFillMb());
            }
            @Override public List<ItemStack> itemOutputs(SolidificationRecipe r) { return items(r.getOutput()); }
        }));

        out.add(new JeiSimpleMachineCategory<>(helper, VACUUM_DISTILL, "block.hbm_m.vacuum_distill",
                new ItemStack(ModItems.VACUUM_DISTILL.get()), new JeiMachineView<VacuumDistillRecipe>() {
            @Override public List<FluidAmount> fluidInputs(VacuumDistillRecipe r) {
                return fluids(r.getInputFluid(), r.getInputMb());
            }
            @Override public List<FluidAmount> fluidOutputs(VacuumDistillRecipe r) {
                return fluids(r.getLight(), r.getLightMb(),
                              r.getHeavy(), r.getHeavyMb(),
                              r.getReformate(), r.getReformateMb(),
                              r.getSour(), r.getSourMb());
            }
        }));

        out.add(new MoldCastingJeiCategory(helper));
        out.add(new MoltenAlloyJeiCategory(helper));
        return out;
    }

    public static void registerRecipes(IRecipeRegistration registration, Level level) {
        registration.addRecipes(BREEDER, RecipeHooks.getAllRecipes(level, BreederRecipe.Type.INSTANCE));
        registration.addRecipes(CATALYTIC_REFORMER, RecipeHooks.getAllRecipes(level, CatalyticReformerRecipe.Type.INSTANCE));
        registration.addRecipes(COKER, RecipeHooks.getAllRecipes(level, CokerRecipe.Type.INSTANCE));
        registration.addRecipes(COMBINATION_OVEN, RecipeHooks.getAllRecipes(level, CombinationOvenRecipe.Type.INSTANCE));
        registration.addRecipes(FRACTION_TOWER, RecipeHooks.getAllRecipes(level, FractionTowerRecipe.Type.INSTANCE));
        registration.addRecipes(HYDROTREATER, RecipeHooks.getAllRecipes(level, HydrotreaterRecipe.Type.INSTANCE));
        registration.addRecipes(LIQUEFACTOR, RecipeHooks.getAllRecipes(level, LiquefactorRecipe.Type.INSTANCE));
        registration.addRecipes(MIXER, RecipeHooks.getAllRecipes(level, MixerRecipe.Type.INSTANCE));
        registration.addRecipes(PYRO_OVEN, RecipeHooks.getAllRecipes(level, PyroOvenRecipe.Type.INSTANCE));
        registration.addRecipes(RADGEN, RecipeHooks.getAllRecipes(level, RadGenRecipe.Type.INSTANCE));
        registration.addRecipes(SILEX, RecipeHooks.getAllRecipes(level, SilexRecipe.Type.INSTANCE));
        registration.addRecipes(SOLIDIFICATION, RecipeHooks.getAllRecipes(level, SolidificationRecipe.Type.INSTANCE));
        registration.addRecipes(VACUUM_DISTILL, RecipeHooks.getAllRecipes(level, VacuumDistillRecipe.Type.INSTANCE));
        registration.addRecipes(MoldCastingJeiCategory.RECIPE_TYPE,
                MoldCastingJeiCategory.filter(RecipeHooks.getAllRecipes(level, MoldCastingRecipe.Type.INSTANCE)));
        registration.addRecipes(MoltenAlloyJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, MoltenAlloyRecipe.Type.INSTANCE));
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.BREEDER.get()), BREEDER);
        registration.addRecipeCatalyst(new ItemStack(ModItems.CATALYTIC_REFORMER.get()), CATALYTIC_REFORMER);
        registration.addRecipeCatalyst(machine(ModBlocks.COKER.get()), COKER);
        registration.addRecipeCatalyst(machine(ModBlocks.COMBINATION_OVEN.get()), COMBINATION_OVEN);
        registration.addRecipeCatalyst(new ItemStack(ModItems.FRACTION_TOWER.get()), FRACTION_TOWER);
        registration.addRecipeCatalyst(new ItemStack(ModItems.HYDROTREATER.get()), HYDROTREATER);
        registration.addRecipeCatalyst(new ItemStack(ModItems.LIQUEFACTOR.get()), LIQUEFACTOR);
        registration.addRecipeCatalyst(new ItemStack(ModItems.MIXER.get()), MIXER);
        registration.addRecipeCatalyst(machine(ModBlocks.PYROOVEN.get()), PYRO_OVEN);
        registration.addRecipeCatalyst(machine(ModBlocks.RADGEN.get()), RADGEN);
        registration.addRecipeCatalyst(new ItemStack(ModItems.SILEX.get()), SILEX);
        registration.addRecipeCatalyst(machine(ModBlocks.SOLIDIFIER.get()), SOLIDIFICATION);
        registration.addRecipeCatalyst(new ItemStack(ModItems.VACUUM_DISTILL.get()), VACUUM_DISTILL);
        registration.addRecipeCatalyst(machine(ModBlocks.FOUNDRY_MOLD.get()), MoldCastingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(machine(ModBlocks.CRUCIBLE.get()), MoltenAlloyJeiCategory.RECIPE_TYPE);
    }
}
//?}
