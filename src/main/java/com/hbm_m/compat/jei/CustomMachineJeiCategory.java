package com.hbm_m.compat.jei;
//? if forge || neoforge {

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm_m.block.machines.custom.ItemCustomMachine;
import com.hbm_m.compat.jei.NeiUniversalJeiCategory.Slot;
import com.hbm_m.config.CustomMachineConfigJSON;
import com.hbm_m.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm_m.inventory.recipes.CustomMachineRecipes;
import com.hbm_m.inventory.recipes.CustomMachineRecipes.CustomMachineRecipe;
import com.hbm_m.inventory.recipes.LegacyStacks.ChanceStack;
import com.hbm_m.inventory.recipes.LegacyStacks.Input;
import com.hbm_m.inventory.recipes.LegacyStacks.LegacyFluid;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code CustomMachineHandler}: eine Kategorie je Maschine aus {@code hbmCustomMachines.json}
 * ({@code NEIConfig}: {@code registerHandlerBypass} fuer jede Konfiguration). Bis zu drei
 * Fluessigkeiten oben, sechs Gegenstaende in zwei Reihen, Maschine bei (75, 42), dazu die Zeilen
 * fuer Strahlung, Verschmutzung, Fluss und Hitze aus {@code drawExtras}.
 */
public class CustomMachineJeiCategory implements IRecipeCategory<CustomMachineRecipe> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_custom.png");

    private final MachineConfiguration conf;
    private final RecipeType<CustomMachineRecipe> type;
    private final IDrawable background;
    private final IDrawable icon;
    private final ItemStack machine;

    public CustomMachineJeiCategory(IGuiHelper guiHelper, MachineConfiguration conf) {
        this.conf = conf;
        this.type = typeFor(conf);
        this.background = guiHelper.drawableBuilder(TEXTURE, 5, 11, 166, 65).setTextureSize(256, 256).build();
        this.machine = ItemCustomMachine.make(conf.unlocalizedName);
        this.icon = guiHelper.createDrawableItemStack(machine);
    }

    public static RecipeType<CustomMachineRecipe> typeFor(MachineConfiguration conf) {
        String id = ("custom_" + conf.unlocalizedName).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
        return RecipeType.create(RefStrings.MODID, id, CustomMachineRecipe.class);
    }

    public static List<MachineConfiguration> configurations() {
        return CustomMachineConfigJSON.niceList;
    }

    public static List<CustomMachineRecipe> recipes(MachineConfiguration conf) {
        List<CustomMachineRecipe> recipes = CustomMachineRecipes.recipes.get(conf.recipeKey);
        return recipes != null ? recipes : List.of();
    }

    public ItemStack getMachine() { return machine; }
    public MachineConfiguration getConfiguration() { return conf; }

    @Override public RecipeType<CustomMachineRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.literal(conf.displayName(CustomMachineConfigJSON.languageCode())); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 166; }
    @Override public int getHeight() { return 85; }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() { return background; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CustomMachineRecipe recipe, IFocusGroup focuses) {
        LegacyFluid[] fin = recipe.inputFluids != null ? recipe.inputFluids : new LegacyFluid[0];
        Input[] iin = recipe.inputItems != null ? recipe.inputItems : new Input[0];
        LegacyFluid[] fout = recipe.outputFluids != null ? recipe.outputFluids : new LegacyFluid[0];
        ChanceStack[] iout = recipe.outputItems != null ? recipe.outputItems : new ChanceStack[0];

        for (int i = 0; i < 3 && i < fin.length; i++) fluid(builder, RecipeIngredientRole.INPUT, 12 + i * 18, 6, fin[i]);
        for (int i = 0; i < 6 && i < iin.length; i++) {
            List<ItemStack> stacks = new ArrayList<>();
            for (ItemStack s : iin[i].ingredient().getItems()) {
                ItemStack c = s.copy();
                c.setCount(iin[i].stacksize());
                stacks.add(c);
            }
            builder.addSlot(RecipeIngredientRole.INPUT, 12 + (i % 3) * 18, i < 3 ? 24 : 42).addItemStacks(stacks);
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 42).addItemStack(machine);

        for (int i = 0; i < 3 && i < fout.length; i++) fluid(builder, RecipeIngredientRole.OUTPUT, 102 + i * 18, 6, fout[i]);
        for (int i = 0; i < 6 && i < iout.length; i++) {
            ChanceStack pair = iout[i];
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 102 + (i % 3) * 18, i < 3 ? 24 : 42)
                    .addItemStack(pair.stack().copy());
            if (pair.chance() != 1) {
                double percent = ((int) (pair.chance() * 1000)) / 10D;
                slot.addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.literal(percent + "%").withStyle(ChatFormatting.RED)));
            }
        }
    }

    private static void fluid(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, LegacyFluid f) {
        if (f == null || f.type() == null) return;
        NeiUniversalJeiCategory.addSlot(builder, role, x, y, Slot.fluid(f.type(), f.fill()), null);
    }

    @Override
    public void draw(CustomMachineRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics g, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        int side = 83;
        if (recipe.radiationAmount != 0) {
            String radiation = "Radiation:" + recipe.radiationAmount;
            g.drawString(font, radiation, 160 - font.width(radiation), 63, 0x08FF00, false);
        }
        if (recipe.pollutionAmount != 0) {
            String pollution = recipe.pollutionType + ":" + recipe.pollutionAmount;
            g.drawString(font, pollution, 160 - font.width(pollution), 75, 0x404040, false);
        }
        if (conf.fluxMode) {
            String flux = "Flux:" + recipe.flux;
            g.drawString(font, flux, side - font.width(flux) / 2, 16, 0x08FF00, false);
        }
        if (conf.maxHeat > 0 && recipe.heat > 0) {
            String heat = "Heat:" + recipe.heat;
            g.drawString(font, heat, side - font.width(heat) / 2, 8, 0xFF0000, false);
        }
    }
}
//?} else {
/*public final class CustomMachineJeiCategory {
    private CustomMachineJeiCategory() {}
}*///?}
