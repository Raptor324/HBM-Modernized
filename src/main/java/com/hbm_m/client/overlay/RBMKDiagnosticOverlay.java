package com.hbm_m.client.overlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code TileEntityRBMKBase.diagnosticPrintHook} ("Dump of Ordered Data Diagnostic", DODD):
 * listet beim Anschauen einer RBMK-Saeule deren Diagnose-NBT ({@code getDiagData}) neben dem Fadenkreuz.
 * Gesteuert ueber {@code ClientConfig.doddRbmkDiagnostic} (Original DODD_RBMK_DIAGNOSTIC, sperrt dort den ganzen Look-Overlay-Hook).
 */
public final class RBMKDiagnosticOverlay {

    private RBMKDiagnosticOverlay() { }

    /** Original-Ausnahmen x/y/z/items/id/muffled, dazu die Inventar-/Zustandsschluessel, die der Port anders benennt. */
    private static final List<String> EXCEPTIONS = List.of(
            "x", "y", "z", "items", "id", "muffled",
            "inventory", "slots", "fuelSlot", "inputSlot", "outputSlot", "loadedItem", "lidState", "explodeOnBroken");

    /** @param corePos Fusspunkt der Saeule (Original {@code findCore}). */
    public static void print(GuiGraphics g, Level world, BlockPos corePos) {
        if (corePos == null) return;
        if (!(world.getBlockEntity(corePos) instanceof RBMKColumnBlockEntity te)) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        CompoundTag flush = new CompoundTag();
        te.getDiagData(flush);
        Set<String> keys = flush.getAllKeys();

        int pX = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int pZ = mc.getWindow().getGuiScaledHeight() / 2;

        List<String> exceptions = new ArrayList<>(EXCEPTIONS);

        //Keep the title unlocalized is cool.
        String title = "Dump of Ordered Data Diagnostic (DODD)";
        g.drawString(font, title, pX + 1, pZ - 19, 0x006000, false);
        g.drawString(font, title, pX, pZ - 20, 0x00FF00, false);

        String name = world.getBlockState(corePos).getBlock().getName().getString();
        g.drawString(font, name, pX + 1, pZ - 9, 0x606000, false);
        g.drawString(font, name, pX, pZ - 10, 0xffff00, false);

        String[] ents = new String[keys.size()];
        keys.toArray(ents);
        Arrays.sort(ents);

        for (String key : ents) {

            if (exceptions.contains(key))
                continue;
            String value = flush.get(key).toString();
            //No...'d' doesn't refer to "day" and 's' doesn't refer to "second". Meaningless.
            if (!value.isEmpty()) {
                char lastChar = value.charAt(value.length() - 1);
                if (lastChar == 'd' || lastChar == 's' || lastChar == 'b') {
                    value = value.substring(0, value.length() - 1);
                }
            }
            g.drawString(font, I18n.get("tile.rbmk.dodd." + key) + ": " + value, pX, pZ, 0xFFFFFF, false);
            pZ += 10;
        }
    }
}
