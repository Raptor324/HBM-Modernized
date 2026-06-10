package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;

import dev.architectury.fluid.FluidStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

/**
 * Chemical Plant recipe registry - adapted from 1.7.10 HBM.
 * Supports item inputs/outputs and fluid inputs/outputs.
 */
public class ChemicalPlantRecipes {

    public static final ChemicalPlantRecipes INSTANCE = new ChemicalPlantRecipes();

    private static final Map<String, ChemicalRecipe> recipes = new HashMap<>();
    private static final List<ChemicalRecipe> recipeList = new ArrayList<>();

    /**
     * A complete chemical plant recipe with items and fluids.
     */
    public static class ChemicalRecipe {
        private final String id;
        private final String displayName;
        private final List<RecipeInput> itemInputs;
        private final List<FluidStack> fluidInputs;
        private final List<ItemStack> itemOutputs;
        private final List<FluidStack> fluidOutputs;
        private final int duration;
        private final int powerConsumption;

        public ChemicalRecipe(String id, String displayName, List<RecipeInput> itemInputs, List<FluidStack> fluidInputs,
                              List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, int duration, int powerConsumption) {
            this.id = id;
            this.displayName = displayName;
            this.itemInputs = itemInputs != null ? itemInputs : new ArrayList<>();
            this.fluidInputs = fluidInputs != null ? fluidInputs : new ArrayList<>();
            this.itemOutputs = itemOutputs != null ? itemOutputs : new ArrayList<>();
            this.fluidOutputs = fluidOutputs != null ? fluidOutputs : new ArrayList<>();
            this.duration = duration;
            this.powerConsumption = powerConsumption;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public List<RecipeInput> getItemInputs() { return itemInputs; }
        public List<FluidStack> getFluidInputs() { return fluidInputs; }
        public List<ItemStack> getItemOutputs() { return itemOutputs; }
        public List<FluidStack> getFluidOutputs() { return fluidOutputs; }
        public int getDuration() { return duration; }
        public int getPowerConsumption() { return powerConsumption; }
    }

    /**
     * Recipe input wrapper - can match by item, item with count, or tag.
     */
    public static abstract class RecipeInput {
        protected int count = 1;

        public abstract boolean matches(ItemStack stack);
        public abstract List<ItemStack> getDisplayStacks();

        public int getCount() { return count; }
    }

    public static class ItemInput extends RecipeInput {
        private final Item item;

        public ItemInput(Item item) {
            this.item = item;
            this.count = 1;
        }

        public ItemInput(Item item, int count) {
            this.item = item;
            this.count = count;
        }

        public ItemInput(ItemStack stack) {
            this.item = stack.getItem();
            this.count = stack.getCount();
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(item) && stack.getCount() >= count;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            return List.of(new ItemStack(item, count));
        }

        @Override
        public int hashCode() {
            return item.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ItemInput other) {
                return this.item == other.item && this.count == other.count;
            }
            return false;
        }
    }

    public static class TagInput extends RecipeInput {
        private final TagKey<Item> tag;

        public TagInput(String tagName) {
            this.tag = TagKey.create(Registries.ITEM, new ResourceLocation(tagName));
            this.count = 1;
        }

        public TagInput(String tagName, int count) {
            this.tag = TagKey.create(Registries.ITEM, new ResourceLocation(tagName));
            this.count = count;
        }

        public TagInput(TagKey<Item> tag) {
            this.tag = tag;
            this.count = 1;
        }

        public TagInput(TagKey<Item> tag, int count) {
            this.tag = tag;
            this.count = count;
        }

        @Override
        public boolean matches(ItemStack stack) {
            return stack.is(tag) && stack.getCount() >= count;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            List<ItemStack> stacks = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> {
                stacks.add(new ItemStack(holder.value(), count));
            });
            return stacks.isEmpty() ? List.of(ItemStack.EMPTY) : stacks;
        }

        @Override
        public int hashCode() {
            return tag.location().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TagInput other) {
                return this.tag.location().equals(other.tag.location()) && this.count == other.count;
            }
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Registration methods
    // -------------------------------------------------------------------------

    /**
     * Register a chemical recipe with full parameters.
     */
    private static void register(String id, String displayName, List<RecipeInput> itemInputs, List<FluidStack> fluidInputs,
                                  List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, int duration, int powerConsumption) {
        ChemicalRecipe recipe = new ChemicalRecipe(id, displayName, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration, powerConsumption);
        recipes.put(id, recipe);
        recipeList.add(recipe);
    }

    // -------------------------------------------------------------------------
    // Lookup methods
    // -------------------------------------------------------------------------

    /**
     * Get a recipe by its ID.
     */
    public static ChemicalRecipe getRecipe(String id) {
        return recipes.get(id);
    }

    /**
     * Get all registered recipes.
     */
    public static Map<String, ChemicalRecipe> getAllRecipes() {
        return new HashMap<>(recipes);
    }

    /**
     * Get all recipes in registration order.
     */
    public static List<ChemicalRecipe> getRecipeList() {
        return new ArrayList<>(recipeList);
    }

    /**
     * Check if inputs match a specific recipe.
     */
    public static boolean matchesRecipe(String recipeId, List<ItemStack> itemInputs, List<FluidStack> fluidInputs) {
        ChemicalRecipe recipe = recipes.get(recipeId);
        if (recipe == null) return false;

        // Check item inputs
        for (RecipeInput requiredInput : recipe.getItemInputs()) {
            boolean found = false;
            for (ItemStack provided : itemInputs) {
                if (requiredInput.matches(provided)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        // Check fluid inputs
        for (FluidStack requiredFluid : recipe.getFluidInputs()) {
            boolean found = false;
            for (FluidStack provided : fluidInputs) {
                if (provided.getFluid() == requiredFluid.getFluid()
                        && provided.getAmount() >= requiredFluid.getAmount()) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Recipe registration (called during mod initialization)
    // -------------------------------------------------------------------------

    public static void registerRecipes() {
        
        /// REGULAR FLUIDS ///
        register("chem.hydrogen", "Hydrogen Production",
                List.of(new ItemInput(ModItems.getPowders(ModPowders.COAL).get(), 1)),
                List.of(FluidStack.create(ModFluids.WATER.getSource(), 8000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.HYDROGEN.getSource(), 500L)),
                20, 400);
        
        register("chem.oxygen", "Oxygen Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.AIR.getSource(), 8000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.OXYGEN.getSource(), 500L)),
                20, 400);
        
        register("chem.xenon", "Xenon Extraction",
                List.of(),
                List.of(FluidStack.create(ModFluids.AIR.getSource(), 16000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.XENON.getSource(), 50L)),
                300, 1000);
        
        register("chem.co2", "Carbon Dioxide Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.GAS.getSource(), 1000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.CARBONDIOXIDE.getSource(), 1000L)),
                60, 100);

        register("chem.concrete", "Concrete Production",
                List.of(new ItemInput(Items.GRAVEL, 8),
                        new ItemInput(Items.SAND, 8),
                        new ItemInput(ModItems.getPowders(ModPowders.CEMENT).get(), 1)),
                List.of(FluidStack.create(ModFluids.WATER.getSource(), 2000L)),
                List.of(new ItemStack(ModBlocks.CONCRETE.get(), 16)),
                List.of(),
                80, 200);
        
        /// OILS ///
        register("chem.ethanol", "Ethanol Production",
                List.of(new ItemInput(Items.SUGAR, 10)),
                List.of(),
                List.of(),
                List.of(FluidStack.create(ModFluids.ETHANOL.getSource(), 1000L)),
                50, 100);
        
        register("chem.biofuel", "Biofuel Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.BIOGAS.getSource(), 1500L),
                        FluidStack.create(ModFluids.ETHANOL.getSource(), 250L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.BIOFUEL.getSource(), 1000L)),
                60, 100);
        
        register("chem.reoil", "Reclaimed Oil",
                List.of(),
                List.of(FluidStack.create(ModFluids.SLOP.getSource(), 1000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.RECLAIMED.getSource(), 800L)),
                40, 100);
        
        register("chem.gasoline", "Gasoline Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.NAPHTHA.getSource(), 1000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.GASOLINE.getSource(), 800L)),
                40, 100);
        
        register("chem.coallube", "Lubricant from Coal Creosote",
                List.of(),
                List.of(FluidStack.create(ModFluids.COALCREOSOTE.getSource(), 1000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.LUBRICANT.getSource(), 1000L)),
                40, 100);
        
        register("chem.heavylube", "Lubricant from Heavy Oil",
                List.of(),
                List.of(FluidStack.create(ModFluids.HEAVYOIL.getSource(), 2000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.LUBRICANT.getSource(), 1000L)),
                40, 100);
        
        /// ACIDS ///
        register("chem.peroxide", "Peroxide Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.WATER.getSource(), 1000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.PEROXIDE.getSource(), 1000L)),
                50, 100);
        
        register("chem.sulfuricacid", "Sulfuric Acid Production",
                List.of(new ItemInput(ModItems.SULFUR.get(), 1)),
                List.of(FluidStack.create(ModFluids.PEROXIDE.getSource(), 1000L),
                        FluidStack.create(ModFluids.WATER.getSource(), 1000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.SULFURIC_ACID.getSource(), 2000L)),
                50, 100);
        
        register("chem.nitricacid", "Nitric Acid Production",
                List.of(new ItemInput(ModItems.CRYSTAL_NITER.get(), 1)),
                List.of(FluidStack.create(ModFluids.SULFURIC_ACID.getSource(), 500L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.NITRIC_ACID.getSource(), 1000L)),
                50, 100);
        
        register("chem.birkeland", "Birkeland-Eyde Process",
                List.of(),
                List.of(FluidStack.create(ModFluids.AIR.getSource(), 8000L),
                        FluidStack.create(ModFluids.WATER.getSource(), 2000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.NITRIC_ACID.getSource(), 1000L)),
                200, 5000);
        
        /// COOLANTS ///
        register("chem.perfluoromethyl", "Perfluoromethyl Production",
                List.of(new ItemInput(ModItems.FLUORITE.get(), 1)),
                List.of(FluidStack.create(ModFluids.PETROLEUM.getSource(), 1000L),
                        FluidStack.create(ModFluids.UNSATURATEDS.getSource(), 500L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.PERFLUOROMETHYL.getSource(), 1000L)),
                20, 100);
        
        /// STEAM ///
        register("chem.steam", "Steam Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.WATER.getSource(), 1000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.STEAM.getSource(), 1000L)),
                10, 50);
        
        /// OXYHYDROGEN ///
        register("chem.oxyhydrogen", "Oxyhydrogen Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.HYDROGEN.getSource(), 500L),
                        FluidStack.create(ModFluids.OXYGEN.getSource(), 250L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.OXYHYDROGEN.getSource(), 500L)),
                20, 100);
        
        /// DEUTERIUM ///
        register("chem.deuterium", "Deuterium Extraction",
                List.of(),
                List.of(FluidStack.create(ModFluids.HEAVYWATER.getSource(), 2000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.DEUTERIUM.getSource(), 500L)),
                100, 1000);
        
        /// UF6 PROCESSING ///
        register("chem.uf6", "Uranium Hexafluoride Production",
                List.of(new ItemInput(ModItems.getPowder(ModIngots.URANIUM).get(), 1),
                        new ItemInput(ModItems.FLUORITE.get(), 4)),
                List.of(FluidStack.create(ModFluids.WATER.getSource(), 1000L)),
                List.of(new ItemStack(ModItems.SULFUR.get(), 2)),
                List.of(FluidStack.create(ModFluids.UF6.getSource(), 1200L)),
                100, 500);
        
        register("chem.puf6", "Plutonium Hexafluoride Production",
                List.of(new ItemInput(ModItems.getPowder(ModIngots.PLUTONIUM).get(), 1),
                        new ItemInput(ModItems.FLUORITE.get(), 3)),
                List.of(FluidStack.create(ModFluids.WATER.getSource(), 1000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.PUF6.getSource(), 900L)),
                200, 500);
        
        /// SCHRABIDIUM ///
        register("chem.sas3", "Schrabidium Sulfide Production",
                List.of(new ItemInput(ModItems.getPowder(ModIngots.SCHRABIDIUM).get(), 1),
                        new ItemInput(ModItems.SULFUR.get(), 2)),
                List.of(FluidStack.create(ModFluids.PEROXIDE.getSource(), 2000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.SAS3.getSource(), 1000L)),
                200, 5000);
        
        register("chem.schrabidic", "Schrabidic Acid Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.SAS3.getSource(), 2000L),
                        FluidStack.create(ModFluids.PEROXIDE.getSource(), 2000L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.SCHRABIDIC.getSource(), 2000L)),
                60, 5000);
        
        /// KEVLAR ///
        register("chem.kevlar", "Kevlar Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.AROMATICS.getSource(), 200L),
                        FluidStack.create(ModFluids.NITRIC_ACID.getSource(), 100L),
                        FluidStack.create(ModFluids.CHLORINE.getSource(), 100L)),
                List.of(new ItemStack(ModItems.PLATE_KEVLAR.get(), 4)),
                List.of(),
                60, 300);

        register("chem.assembler", "Machine Assembler Production",
                List.of(new ItemInput(ModItems.getIngot(ModIngots.STEEL).get(), 8),
                        new ItemInput(ModItems.PIPE_COPPER.get(), 2),
                        new ItemInput(ModItems.INSULATOR.get(), 16),
                        new ItemInput(ModItems.MOTOR.get(), 2),
                        new ItemInput(ModItems.COIL_TUNGSTEN.get(), 2),
                        new ItemInput(ModItems.ANALOG_CIRCUIT.get(), 1)),
                List.of(),
                List.of(new ItemStack(ModItems.MACHINE_ASSEMBLER.get(), 1)),
                List.of(),
                240, 1200);
        
        /// DHC ///
        register("chem.dhc", "Deuterohydrocarbon Production",
                List.of(),
                List.of(FluidStack.create(ModFluids.DEUTERIUM.getSource(), 500L),
                        FluidStack.create(ModFluids.REFORMGAS.getSource(), 250L),
                        FluidStack.create(ModFluids.SYNGAS.getSource(), 250L)),
                List.of(),
                List.of(FluidStack.create(ModFluids.HYDROGEN.getSource(), 500L)), // DHC not available, using hydrogen placeholder
                400, 500);
    }
}

