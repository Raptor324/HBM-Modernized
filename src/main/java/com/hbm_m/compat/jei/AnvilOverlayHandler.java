package com.hbm_m.compat.jei;

import java.util.Optional;

import com.hbm_m.inventory.gui.GUIAnvil;
import com.hbm_m.inventory.menu.AnvilMenu;
import com.hbm_m.inventory.menu.ModMenuTypes;
import com.hbm_m.recipe.AnvilRecipe;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * JEI-Port von {@code com.hbm.handler.nei.AnvilOverlayHandler} (NEI:
 * {@code API.registerGuiOverlayHandler(GUIAnvil.class, new AnvilOverlayHandler(), "ntmAnvil")}).
 * Waehrend der Amboss offen ist und der Spieler ein Construction-Rezept der Kategorie
 * {@link AnvilJeiCategory} auswaehlt, wird die Rezeptliste im {@link GUIAnvil} auf dieses
 * Rezept fokussiert ({@code GUIAnvil#focusRecipe}); es wird nichts uebertragen — wie im
 * Original („It will not craft the recepie. Only select it."), {@code maxTransfer} wird ignoriert.
 *
 * <p>NEI-Overlay-Handler existieren in JEI nicht; das naechste AEquivalent ist ein
 * universeller Transfer-Handler ({@code maxTransfer}/{@code doTransfer}-Signatur wie
 * {@code IOverlayHandler#overlayRecipe(gui, recipe, recipeIndex, maxTransfer)}). Fuer die
 * Smithing-Kategorie gibt es im Original keinen Overlay-Handler ({@code ntmSmithing} ist
 * nie registriert) — darum ist hier auch keiner angemeldet.</p>
 */
@OnlyIn(Dist.CLIENT)
public class AnvilOverlayHandler implements IRecipeTransferHandler<AnvilMenu, AnvilRecipe> {

    @Override
    public Class<? extends AnvilMenu> getContainerClass() {
        return AnvilMenu.class;
    }

    @Override
    public Optional<MenuType<AnvilMenu>> getMenuType() {
        return Optional.of(ModMenuTypes.ANVIL_MENU.get());
    }

    @Override
    public RecipeType<AnvilRecipe> getRecipeType() {
        return AnvilJeiCategory.RECIPE_TYPE;
    }

    @Override
    public IRecipeTransferError transferRecipe(AnvilMenu container, AnvilRecipe recipe, IRecipeSlotsView recipeSlots,
                                               Player player, boolean maxTransfer, boolean doTransfer) {
        // GUIAnvil liegt hinter der JEI-Rezeptansicht; nur auswaehlen, nichts verschieben.
        if (recipe != null && Minecraft.getInstance().screen instanceof GUIAnvil gui) {
            gui.focusRecipe(recipe);
        }
        return null; // Erfolg ohne Transfer (Custom-Handler uebernimmt die Logik selbst)
    }
}
