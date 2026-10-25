package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.tool.ItemGuideBook.BookType;
import com.hbm_m.item.tool.ItemGuideBook.GuideImage;
import com.hbm_m.item.tool.ItemGuideBook.GuidePage;
import com.hbm_m.item.tool.ItemGuideBook.GuideText;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * 1:1 {@code com.hbm.inventory.gui.GUIScreenGuide}: Anleitungsbuch mit Deckblatt ({@code book_cover.png}) und
 * Doppelseiten ({@code book.png}, beide 512x512). Klick aufs Deckblatt schlaegt das Buch auf, die Pfeile unten
 * blaettern. Texte werden wie im Original wortweise umbrochen und um den Faktor {@code scale} verkleinert;
 * "$" trennt Zeilen im Deckblatttitel, {@code <titel>.scale} in der Sprachdatei ueberschreibt die Titelgroesse.
 */
public class GUIScreenGuide extends Screen {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/book/book.png");
    private static final ResourceLocation texture_cover = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/book/book_cover.png");

    protected int xSize;
    protected int ySize;
    protected int guiLeft;
    protected int guiTop;

    private final BookType type;

    int page;
    int maxPage;

    public GUIScreenGuide(BookType type) {
        super(Component.empty());
        this.type = type;

        page = -1;
        maxPage = (int) Math.ceil(type.pages.size() / 2D) - 1;

        this.xSize = 272;
        this.ySize = 182;
    }

    public static void open(BookType type) {
        Minecraft.getInstance().setScreen(new GUIScreenGuide(type));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        this.drawGuiContainerBackgroundLayer(g, f, mouseX, mouseY);
        this.drawGuiContainerForegroundLayer(g, mouseX, mouseY);
    }

    protected void drawGuiContainerBackgroundLayer(GuiGraphics g, float f, int i, int j) {

        if (page < 0) {
            g.blit(texture_cover, guiLeft, guiTop, 0, 0, xSize, ySize, 512, 512);
            return;
        }

        g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize, 512, 512);

        boolean overLeft = i >= guiLeft + 24 && i < guiLeft + 42 && j >= guiTop + 155 && j < guiTop + 165;
        boolean overRight = i >= guiLeft + 230 && i < guiLeft + 248 && j >= guiTop + 155 && j < guiTop + 165;

        if (this.page > 0) {

            if (!overLeft)
                g.blit(texture, guiLeft + 24, guiTop + 155, 3, 207, 18, 10, 512, 512);
            else
                g.blit(texture, guiLeft + 24, guiTop + 155, 26, 207, 18, 10, 512, 512);
        }

        if (this.page < this.maxPage) {

            if (!overRight)
                g.blit(texture, guiLeft + 230, guiTop + 155, 3, 194, 18, 10, 512, 512);
            else
                g.blit(texture, guiLeft + 230, guiTop + 155, 26, 194, 18, 10, 512, 512);
        }
    }

    /** Ganze Textur auf dimX x dimY gestreckt (Original: Tessellator-Quad mit UV 0..1). */
    public static void drawImage(GuiGraphics g, ResourceLocation image, int x, int y, int dimX, int dimY) {
        g.blit(image, x, y, 0.0F, 0.0F, dimX, dimY, dimX, dimY);
    }

    protected void drawGuiContainerForegroundLayer(GuiGraphics g, int x, int y) {

        if (this.page < 0) {

            float scale = this.type.titleScale;
            String[] coverLines = I18n.get(this.type.title).split("\\$");

            for (int i = 0; i < coverLines.length; i++) {

                String cover = coverLines[i];

                g.pose().pushPose();
                g.pose().scale(scale, scale, 1F);
                g.drawString(this.font, cover, (int) ((guiLeft + ((this.xSize / 2) - (this.font.width(cover) / 2 * scale))) / scale), (int) ((guiTop + 50 + i * 10 * scale) / scale), 0xfece00, false);
                g.pose().popPose();
            }

            return;
        }

        int sideOffset = 130;

        for (int i = 0; i < 2; i++) {

            int defacto = this.page * 2 + i;

            if (defacto < this.type.pages.size()) {

                GuidePage page = this.type.pages.get(defacto);

                for (GuideText textBox : page.texts) {
                    float scale = textBox.scale;
                    String text = I18n.get(textBox.text);
                    int width = textBox.width;

                    int widthScaled = (int) (width * scale);
                    List<String> lines = new ArrayList<>();
                    String[] words = text.split(" ");

                    lines.add(words[0]);
                    int indent = this.font.width(words[0]);

                    for (int w = 1; w < words.length; w++) {

                        indent += this.font.width(" " + words[w]);

                        if (indent <= widthScaled) {
                            String last = lines.get(lines.size() - 1);
                            lines.set(lines.size() - 1, last + (" " + words[w]));
                        } else {
                            lines.add(words[w]);
                            indent = this.font.width(words[w]);
                        }
                    }

                    float titleScale = getOverrideScale(page.titleScale, page.title + ".scale");

                    g.pose().pushPose();
                    g.pose().scale(1F / scale, 1F / scale, 1F);

                    float topOffset;

                    if (textBox.yOffset == -1) {
                        topOffset = page.title == null ? -10 : 6 / titleScale;
                    } else {
                        topOffset = textBox.yOffset;
                    }

                    for (int l = 0; l < lines.size(); l++) {
                        g.drawString(this.font, lines.get(l), (int) ((guiLeft + 20 + i * sideOffset + textBox.xOffset) * scale), (int) ((guiTop + 30 + topOffset) * scale + (12 * l)), 4210752, false);
                    }

                    g.pose().popPose();
                }

                if (page.title != null) {

                    float tScale = page.titleScale;
                    String titleLoc = I18n.get(page.title);

                    g.pose().pushPose();
                    g.pose().scale(1F / tScale, 1F / tScale, 1F);
                    g.drawString(this.font, titleLoc, (int) ((guiLeft + 20 + i * sideOffset + ((100 / 2) - (this.font.width(titleLoc) / 2 / tScale))) * tScale), (int) ((guiTop + 20) * tScale), page.titleColor, false);
                    g.pose().popPose();
                }

                if (!page.images.isEmpty()) {
                    for (GuideImage image : page.images) {

                        int ix = image.x;

                        if (ix == -1)
                            ix = 100 / 2 - image.sizeX / 2;

                        drawImage(g, image.image, guiLeft + 20 + ix + sideOffset * i, guiTop + image.y, image.sizeX, image.sizeY);
                    }
                }

                String pageLabel = (defacto + 1) + "/" + (this.type.pages.size());
                g.drawString(this.font, pageLabel, guiLeft + 44 + i * 185 - i * this.font.width(pageLabel), guiTop + 156, 4210752, false);
            }
        }
    }

    private float getOverrideScale(float def, String tag) {

        if (I18n.exists(tag)) {
            try {
                return 1F / Float.parseFloat(I18n.get(tag));
            } catch (NumberFormatException ignored) { }
        }

        return def;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = (int) mx;
        int j = (int) my;

        if (page < 0) {
            page = 0;
            click();
            return true;
        }

        boolean overLeft = i >= guiLeft + 24 && i < guiLeft + 42 && j >= guiTop + 155 && j < guiTop + 165;
        boolean overRight = i >= guiLeft + 230 && i < guiLeft + 248 && j >= guiTop + 155 && j < guiTop + 165;

        if (overLeft && page > 0) {
            page--;
            click();
        }

        if (overRight && page < maxPage) {
            page++;
            click();
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
