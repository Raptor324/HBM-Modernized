package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.AssemblerRecipe.AssemblerInputSlot;
import com.hbm_m.recipe.AssemblerRecipe.WeightedStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
/**
 * JEI port of {@code AssemblyMachineRecipeHandler} (extends {@code NEIGenericRecipeHandler}).
 */
@OnlyIn(Dist.CLIENT)
public class AssemblerJeiCategory extends JeiGenericRecipeCategory<AssemblerRecipe> {

    public static final RecipeType<AssemblerRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "assembler", AssemblerRecipe.class);

    /**
     * Original NEI hides recipes of secret blueprint pools by default
     * ({@code ClientConfig.NEI_HIDE_SECRETS == true} in {@code NEIGenericRecipeHandler}).
     */
    public static boolean isSecretPool(AssemblerRecipe recipe) {
        String pool = recipe.getBlueprintPool();
        return pool != null && pool.startsWith("secret.");
    }

    /**
     * Rezeptmenge wie im Original-NEI: {@code AssemblyMachineRecipeHandler} enumeriert ausschliesslich
     * {@code AssemblyMachineRecipes.INSTANCE} (AssemblyMachineRecipeHandler.java:13). Im Port ist das
     * 1:1-Set von {@code AssemblyMachineRecipeGenerator} (+ die dynamischen package_/unpackage_-Rezepte
     * aus {@code RestRecipeGenerator}) an den Rezept-IDs unter {@code hbm_m:assembler/} erkennbar; die
     * Port-Notbehelfe ({@code AssemblerRecipeGenerator} u.a.) liegen ausserhalb dieser Ebene und bleiben
     * außen vor — sie bleiben aber voll craftbar (Maschinen/GUI-Auswahl unveraendert).
     */
    public static boolean isVisible(AssemblerRecipe recipe) {
        if (!recipe.getRecipeId().getPath().startsWith("assembler/")) return false;
        if (isSecretPool(recipe)) return false;
        // Original NEIGenericRecipeHandler versteckt Rezepte mit ModItems.excludeNEI-Zutaten
        // (NEIGenericRecipeHandler.java:216-218/246-249; Liste: item_secret + Meteoritenschwerter).
        return !usesExcludedItem(recipe);
    }

    /**
     * Port von {@code ModItems.excludeNEI} (ModItems.registerItem): {@code item_secret} (im Port fuenf
     * Einzel-Items) und die elf Meteoritenschwerter der Schwertkette — das Basis-Schwert gehoert wie im
     * Original NICHT dazu.
     */
    private static volatile Set<Item> excludeNeiCache;

    private static Set<Item> excludeNei() {
        Set<Item> set = excludeNeiCache;
        if (set == null) {
            set = Set.of(
                    ModItems.ITEM_SECRET_CANISTER.get(),
                    ModItems.ITEM_SECRET_CONTROLLER.get(),
                    ModItems.ITEM_SECRET_SELENIUM_STEEL.get(),
                    ModItems.ITEM_SECRET_ABERRATOR.get(),
                    ModItems.ITEM_SECRET_FOLLY.get(),
                    ModItems.METEORITE_SWORD_SEARED.get(),
                    ModItems.METEORITE_SWORD_REFORGED.get(),
                    ModItems.METEORITE_SWORD_HARDENED.get(),
                    ModItems.METEORITE_SWORD_ALLOYED.get(),
                    ModItems.METEORITE_SWORD_MACHINED.get(),
                    ModItems.METEORITE_SWORD_TREATED.get(),
                    ModItems.METEORITE_SWORD_ETCHED.get(),
                    ModItems.METEORITE_SWORD_BRED.get(),
                    ModItems.METEORITE_SWORD_IRRADIATED.get(),
                    ModItems.METEORITE_SWORD_FUSED.get(),
                    ModItems.METEORITE_SWORD_BALEFUL.get());
            excludeNeiCache = set;
        }
        return set;
    }

    /** Original: inputItem- und outputItem-Stacks gegen {@code ModItems.excludeNEI} geprueft. */
    private static boolean usesExcludedItem(AssemblerRecipe recipe) {
        for (AssemblerInputSlot slot : getInputSlots(recipe)) {
            for (ItemStack stack : slot.ingredient().getItems()) {
                if (excludeNei().contains(stack.getItem())) return true;
            }
        }
        if (recipe.hasFixedOutput() && excludeNei().contains(recipe.getResultItemSafe().getItem())) return true;
        for (List<WeightedStack> group : recipe.getChanceOutputs()) {
            for (WeightedStack w : group) {
                if (excludeNei().contains(w.stack().getItem())) return true;
            }
        }
        return false;
    }

    public AssemblerJeiCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack[]{
                new ItemStack(ModItems.ADVANCED_ASSEMBLY_MACHINE.get())
        });
    }

    @Override
    public RecipeType<AssemblerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.advanced_assembly_machine");
    }

    private static List<AssemblerInputSlot> getInputSlots(AssemblerRecipe recipe) {
        List<AssemblerInputSlot> slots = recipe.getInputDisplaySlots();
        if (!slots.isEmpty()) {
            return slots;
        }
        return AssemblerJeiInputs.fallbackFromExpanded(recipe.getIngredients());
    }

    @Override
    protected int getInputCount(AssemblerRecipe recipe) {
        return getInputSlots(recipe).size() + recipe.getFluidInputs().size();
    }

    @Override
    protected int getOutputCount(AssemblerRecipe recipe) {
        // Wie im Original outputItem.length (1 Element — fest ODER ChanceOutputMulti) + outputFluid.length.
        int count = (recipe.hasFixedOutput() ? 1 : 0) + recipe.getChanceOutputs().size();
        return count + recipe.getFluidOutputs().size();
    }

    /** Fluessigkeit des Rezepts (inputFluids/outputFluids des Originals) als JEI-Slot. */
    private static void addFluidSlot(IRecipeSlotBuilder slot, dev.architectury.fluid.FluidStack fluid) {
        //? if forge {
        slot.setFluidRenderer(4_000, false, 16, 16)
                .setCustomRenderer(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount(), fluid.getTag()));
        //?} elif neoforge {
        /*slot.setFluidRenderer(4_000, false, 16, 16)
                .setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new HbmFluidJeiRenderer(16, 16))
                .addIngredient(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, new net.minecraftforge.fluids.FluidStack(fluid.getFluid(), (int) fluid.getAmount()));
        *///?}
    }

    @Override
    protected boolean hasBlueprintTemplate(AssemblerRecipe recipe) {
        return recipe.requiresBlueprint();
    }

    @Override
    protected int getInputXOffset(AssemblerRecipe recipe, int inputCount) {
        if (inputCount > 12) return -9;
        if (inputCount > 9) return 18;
        return 0;
    }

    @Override
    protected int getOutputXOffset(AssemblerRecipe recipe, int outputCount) {
        return getOffset(recipe);
    }

    @Override
    protected int getMachineXOffset(AssemblerRecipe recipe) {
        return getOffset(recipe);
    }

    private static int getOffset(AssemblerRecipe recipe) {
        int length = getInputSlots(recipe).size() + recipe.getFluidInputs().size();
        if (length > 12) return 27;
        if (length > 9) return 18;
        return 0;
    }

    @Override
    protected void addInputSlots(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, int inputXOffset) {
        List<AssemblerInputSlot> inputs = getInputSlots(recipe);
        int[][] positions = JeiNeiLayout.getGenericInputSlotPositions(getInputCount(recipe));

        for (int i = 0; i < inputs.size() && i < positions.length; i++) {
            AssemblerInputSlot slot = inputs.get(i);
            IRecipeSlotBuilder jeiSlot = addItemSlot(builder, RecipeIngredientRole.INPUT,
                    positions[i][0] + inputXOffset, positions[i][1]);
            JeiIngredientSlots.addCountedIngredient(jeiSlot, slot.ingredient(), slot.count());
        }
        int idx = inputs.size();
        for (dev.architectury.fluid.FluidStack fluid : recipe.getFluidInputs()) {
            if (idx >= positions.length) break;
            addFluidSlot(addItemSlot(builder, RecipeIngredientRole.INPUT, positions[idx][0] + inputXOffset, positions[idx][1]), fluid);
            idx++;
        }
    }

    @Override
    protected void addOutputSlots(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, int outputXOffset) {
        int[][] positions = JeiNeiLayout.getGenericOutputSlotPositions(getOutputCount(recipe));
        int idx = 0;
        if (recipe.hasFixedOutput()) {
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[idx][0] + outputXOffset, positions[idx][1])
                    .addItemStack(recipe.getResultItemSafe());
            idx++;
        }
        // Zufallsausgabe (Original ChanceOutputMulti.getAllPossibilities): alle Varianten zyklisch
        // in einem Slot — gleiche Darstellung wie in RockMillJeiCategory.
        for (List<WeightedStack> group : recipe.getChanceOutputs()) {
            List<ItemStack> variants = new ArrayList<>();
            for (WeightedStack w : group) {
                if (!w.stack().isEmpty()) variants.add(w.stack());
            }
            if (variants.isEmpty()) continue;
            if (idx >= positions.length) break;
            addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[idx][0] + outputXOffset, positions[idx][1])
                    .addItemStacks(variants);
            idx++;
        }
        for (dev.architectury.fluid.FluidStack fluid : recipe.getFluidOutputs()) {
            if (idx >= positions.length) break;
            addFluidSlot(addItemSlot(builder, RecipeIngredientRole.OUTPUT, positions[idx][0] + outputXOffset, positions[idx][1]), fluid);
            idx++;
        }
    }

    @Override
    protected void addBlueprintSlot(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, int machineXOffset) {
        if (!recipe.requiresBlueprint()) {
            return;
        }

        ItemStack folder = com.hbm_m.item.industrial.ItemBlueprints.make(recipe.getBlueprintPool());
        addUnframedSlot(builder, RecipeIngredientRole.RENDER_ONLY, 75 + machineXOffset, 10)
                .addItemStack(folder);
    }

    @Override
    protected void drawRecipeExtras(AssemblerRecipe recipe, GuiGraphics graphics) {
        JeiNeiRendering.drawGenericRecipeExtras(graphics, recipe.getDuration(), recipe.getPowerConsumption());
    }
}
