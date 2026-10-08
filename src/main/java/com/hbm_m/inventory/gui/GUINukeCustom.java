package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.NukeCustomBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.explosion.CustomNukeExplosion;
import com.hbm_m.inventory.menu.NukeCustomMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUINukeCustom}: Schaltplan-Hintergrund, die gerade aktive Stufe leuchtet (Salz zusaetzlich bei
 * Atom/Wasserstoff), Tooltips mit "Stufe roh/angepasst", Bedingungen und Zitaten.
 */
public class GUINukeCustom extends GuiInfoScreen<NukeCustomMenu> {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/gun_bomb_schematic.png");
    private final NukeCustomBlockEntity testNuke;

    public GUINukeCustom(NukeCustomMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.testNuke = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static Component y(String s) {
        return Component.literal(s).withStyle(ChatFormatting.YELLOW);
    }

    private static Component t(String s) {
        return Component.literal(s);
    }

    private static Component i(String s) {
        return Component.literal(s).withStyle(ChatFormatting.ITALIC);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float f) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);
        this.renderTooltip(g, mouseX, mouseY);

        if (testNuke == null) return;
        testNuke.update();

        drawCustomInfoStat(g, mouseX, mouseY, 16, 89, 18, 18, mouseX, mouseY,
                y("Conventional Explosives (Level " + testNuke.tnt + "/" + Math.min(testNuke.tnt, CustomNukeExplosion.maxTnt) + ")"),
                t("Caps at " + CustomNukeExplosion.maxTnt),
                t("N²-like above level 75"),
                i("\"Goes boom\""));

        drawCustomInfoStat(g, mouseX, mouseY, 34, 89, 18, 18, mouseX, mouseY,
                y("Nuclear (Level " + testNuke.nuke + "/" + testNuke.getNukeAdj() + ")"),
                t("Requires TNT level 16"),
                t("Caps at " + CustomNukeExplosion.maxNuke),
                t("Has fallout"),
                i("\"Now I am become death, destroyer of worlds.\""));

        drawCustomInfoStat(g, mouseX, mouseY, 52, 89, 18, 18, mouseX, mouseY,
                y("Thermonuclear (Level " + testNuke.hydro + "/" + testNuke.getHydroAdj() + ")"),
                t("Requires nuclear level 100"),
                t("Caps at " + CustomNukeExplosion.maxHydro),
                t("Reduces added fallout by salted stage by 75%"),
                i("\"And for my next trick, I'll make"),
                i("the island of Elugelab disappear!\""));

        drawCustomInfoStat(g, mouseX, mouseY, 70, 89, 18, 18, mouseX, mouseY,
                y("Antimatter (Level " + testNuke.amat + "/" + testNuke.getAmatAdj() + ")"),
                t("Requires nuclear level 50"),
                t("Caps at " + CustomNukeExplosion.maxAmat),
                i("\"Antimatter, Balefire, whatever.\""));

        drawCustomInfoStat(g, mouseX, mouseY, 88, 89, 18, 18, mouseX, mouseY,
                y("Salted (Level " + testNuke.dirty + "/" + Math.min(testNuke.dirty, 100) + ")"),
                t("Extends fallout of nuclear and"),
                t("thermonuclear stages"),
                t("Caps at 100"),
                i("\"Not to be confused with tablesalt.\""));

        drawCustomInfoStat(g, mouseX, mouseY, 106, 89, 18, 18, mouseX, mouseY,
                y("Schrabidium (Level " + testNuke.schrab + "/" + testNuke.getSchrabAdj() + ")"),
                t("Requires nuclear level 50"),
                t("Caps at " + CustomNukeExplosion.maxSchrab),
                i("\"For the hundredth time,"),
                i("you can't bypass these caps!\""));

        drawCustomInfoStat(g, mouseX, mouseY, 142, 89, 18, 18, mouseX, mouseY,
                y("Ice cream (Level unknown)"),
                i("\"Probably not ice cream but the label came off.\""));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (testNuke == null) return;

        if (this.testNuke.euph > 0)
            g.blit(texture, leftPos + 142, topPos + 89, 176, 108, 18, 18);
        else if (this.testNuke.schrab > 0)
            g.blit(texture, leftPos + 106, topPos + 89, 176, 90, 18, 18);
        else if (this.testNuke.amat > 0)
            g.blit(texture, leftPos + 70, topPos + 89, 176, 54, 18, 18);
        else if (this.testNuke.hydro > 0)
            g.blit(texture, leftPos + 52, topPos + 89, 176, 36, 18, 18);
        else if (this.testNuke.nuke > 0)
            g.blit(texture, leftPos + 34, topPos + 89, 176, 18, 18, 18);
        else if (this.testNuke.tnt > 0)
            g.blit(texture, leftPos + 16, topPos + 89, 176, 0, 18, 18);

        if (this.testNuke.dirty > 0 &&
                this.testNuke.nuke > 0 &&
                this.testNuke.amat == 0 &&
                this.testNuke.schrab == 0 &&
                this.testNuke.euph == 0)
            g.blit(texture, leftPos + 88, topPos + 89, 176, 72, 18, 18);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
