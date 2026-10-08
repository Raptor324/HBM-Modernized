package com.hbm_m.qmaw;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.qmaw.components.QComponentLink;
import com.hbm_m.qmaw.components.QComponentText;
import com.hbm_m.sound.HbmSoundsNT;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.qmaw.GuiQMAW}: das Schnellhandbuch (F1 ueber einem Gegenstand in einem
 * Inventar). Zerlegt den Eintragstext in Woerter, {@code [[Verweise|Ziel]]} und {@code <br>}.
 */
public class GuiQMAW extends Screen {

    //? if fabric && < 1.21.1 {
    /*protected static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID, "textures/gui/gui_wiki.png");
    protected static final ResourceLocation the_man = new ResourceLocation(RefStrings.MODID, "textures/gui/gui_wiki_flix.png");
    *///?} else {
    protected static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_wiki.png");
    protected static final ResourceLocation the_man = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_wiki_flix.png");
    //?}

    public String pageTitle; // Original: title (kollidiert mit Screen.title)
    public String qmawID;
    public ItemStack icon;
    public List<List<ManualElement>> lines = new ArrayList<>();
    /** History for returning via button */
    public List<String> back = new ArrayList<>();
    public List<String> forward = new ArrayList<>();

    protected int xSize = 340;
    protected int ySize = 224;
    protected int guiLeft;
    protected int guiTop;

    protected boolean isDragging = false;
    protected int scrollProgress = 0;
    protected int lastClickX = 0;
    protected int lastClickY = 0;

    public static final String EN_US = "en_US";

    public GuiQMAW(QuickManualAndWiki qmaw) {
        super(Component.empty());
        qmawID = qmaw.name;
        parseQMAW(qmaw);
    }

    /** 1.7.10-Sprachcode ({@code ru_RU}) aus dem 1.20-Code ({@code ru_ru}). */
    protected static String langCode() {
        String code = Minecraft.getInstance().getLanguageManager().getSelected();
        int us = code.indexOf('_');
        return us < 0 ? code : code.substring(0, us) + "_" + code.substring(us + 1).toUpperCase(Locale.ROOT);
    }

    protected void parseQMAW(QuickManualAndWiki qmaw) {
        String lang = langCode();

        this.pageTitle = qmaw.title.get(lang);
        if (pageTitle == null) this.pageTitle = qmaw.title.get(EN_US);
        if (pageTitle == null) this.pageTitle = "Missing Localization!";

        this.icon = qmaw.icon;

        String toParse = qmaw.contents.get(lang);
        if (toParse == null) toParse = qmaw.contents.get(EN_US);
        if (toParse == null) toParse = "Missing Localization!";

        int maxLineLength = xSize - 29;
        String prevToParse = toParse;
        int maxIterations = 1000;
        int currentLineWidth = 0;

        while (!toParse.isEmpty() && maxIterations > 0) {
            if (this.lines.isEmpty()) this.lines.add(new ArrayList<>());
            List<ManualElement> currentLine = this.lines.get(this.lines.size() - 1);

            toParse = toParse.trim();

            maxIterations--;

            if (toParse.startsWith("<br>")) {
                toParse = toParse.substring(4);
                currentLine = new ArrayList<>();
                this.lines.add(currentLine);
                currentLineWidth = 0;
                continue;
            }

            // handle links
            if (toParse.startsWith("[[")) {
                int end = toParse.indexOf("]]");
                if (end != -1) {
                    String link = toParse.substring(2, end);
                    toParse = toParse.substring(end + 2);

                    int pipe = link.indexOf("|");
                    QComponentLink linkComponent;

                    String suffix = toParse.startsWith(" ") ? " " : "";

                    if (pipe == -1) {
                        linkComponent = new QComponentLink(link, link + suffix);
                    } else {
                        linkComponent = new QComponentLink(link.substring(pipe + 1), link.substring(0, pipe) + suffix);
                    }

                    // append to current line
                    int width = linkComponent.getWidth();
                    if (width + currentLineWidth <= maxLineLength) {
                        currentLine.add(linkComponent);
                        currentLineWidth += width;
                    // new line
                    } else {
                        currentLine = new ArrayList<>();
                        this.lines.add(currentLine);
                        currentLine.add(linkComponent);
                        currentLineWidth = width;
                    }

                    prevToParse = toParse;
                    continue;
                }
            }

            // handle standard text
            int delimit = toParse.length();

            int spaceIndex = toParse.indexOf(" ");
            if (spaceIndex != -1) delimit = Math.min(delimit, spaceIndex);
            int linkIndex = toParse.indexOf("[[");
            if (linkIndex != -1) delimit = Math.min(delimit, linkIndex);
            int brIndex = toParse.indexOf("<br>");
            if (brIndex != -1) delimit = Math.min(delimit, brIndex);

            if (delimit > 0) {
                QComponentText textComponent = new QComponentText(toParse.substring(0, delimit) + (spaceIndex == delimit ? " " : ""));
                toParse = toParse.substring(delimit);

                // append to current line
                int width = textComponent.getWidth();
                if (width + currentLineWidth <= maxLineLength) {
                    currentLine.add(textComponent);
                    currentLineWidth += width;
                // new line
                } else {
                    currentLine = new ArrayList<>();
                    this.lines.add(currentLine);
                    currentLine.add(textComponent);
                    currentLineWidth = width;
                }

                prevToParse = toParse;
                continue;
            }

            if (toParse.equals(prevToParse)) break;
            prevToParse = toParse;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int key) {
        boolean ret = super.mouseClicked(mx, my, key);
        int x = (int) mx;
        int y = (int) my;

        if (key == 0) {
            this.lastClickX = x;
            this.lastClickY = y;
        }

        if (guiLeft + 3 <= x && guiLeft + 3 + 18 > x && guiTop + 3 < y && guiTop + 3 + 18 >= y) back();
        if (guiLeft + 21 <= x && guiLeft + 21 + 18 > x && guiTop + 3 < y && guiTop + 3 + 18 >= y) forward();

        if (lines.size() > 1 && scrollProgress == lines.size() - 1) {
            if (guiLeft + 60 <= x && guiLeft + 60 + 80 > x && guiTop + this.ySize - 84 < y && guiTop + this.ySize - 4 >= y) {
                SoundEvent singer = HbmSoundsNT.get("hbm:alarm.singer");
                if (singer != null) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(singer, 1.0F));
            }
        }
        return ret;
    }

    private static void playPress() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public void back() {
        if (this.back.isEmpty()) return;

        String prev = back.get(back.size() - 1);

        QuickManualAndWiki qmaw = QMAWLoader.qmaw.get(prev);
        if (qmaw != null) {
            playPress();
            GuiQMAW screen = new GuiQMAW(qmaw);
            screen.back.addAll(back);
            screen.back.remove(screen.back.size() - 1);
            screen.forward.addAll(forward);
            screen.forward.add(qmawID);
            Minecraft.getInstance().setScreen(screen);
        }
    }

    public void forward() {
        if (this.forward.isEmpty()) return;

        String next = forward.get(forward.size() - 1);

        QuickManualAndWiki qmaw = QMAWLoader.qmaw.get(next);
        if (qmaw != null) {
            playPress();
            GuiQMAW screen = new GuiQMAW(qmaw);
            screen.back.addAll(back);
            screen.back.add(qmawID);
            screen.forward.addAll(forward);
            screen.forward.remove(screen.forward.size() - 1);
            Minecraft.getInstance().setScreen(screen);
        }
    }

    public int getSliderPosition() {
        double progress = (double) scrollProgress / (double) (lines.size() - 1);
        return 25 + (int) (progress * 180);
    }

    private static boolean leftDown() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float f) {

        if (leftDown() && guiLeft + xSize - 15 <= mouseX && guiLeft + xSize - 15 + 12 > mouseX && guiTop + 25 < mouseY && guiTop + 25 + 191 >= mouseY) {
            isDragging = true;
        }

        if (!leftDown()) isDragging = false;

        if (isDragging) {
            int min = guiTop + 25 + 8;
            int max = guiTop + 25 + 191 - 8;
            int span = max - min;

            double progress = Mth.clamp((double) (mouseY - min) / span, 0D, 1D);
            this.scrollProgress = Mth.clamp((int) Math.round((lines.size() - 1) * progress), 0, lines.size() - 1);
        }

        gfx.fill(0, 0, this.width, this.height, 0xe0000000);

        this.drawGuiContainerBackgroundLayer(gfx, f, mouseX, mouseY);
        this.drawGuiContainerForegroundLayer(gfx, mouseX, mouseY);

        this.lastClickX = 0;
        this.lastClickY = 0;
    }

    /** Original {@code handleScroll}: ein Rasten des Mausrads = eine Zeile. */
    //? if < 1.21.1 {
    @Override
    public boolean mouseScrolled(double x, double y, double scroll) {
    //?} else {
    /*@Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scroll) {
    *///?}
        if (!leftDown()) {
            if (scroll > 0 && this.scrollProgress > 0) this.scrollProgress--;
            if (scroll < 0 && this.scrollProgress < this.lines.size() - 1) this.scrollProgress++;
        }
        return true;
    }

    private void drawGuiContainerForegroundLayer(GuiGraphics gfx, int mouseX, int mouseY) {

        int x = 43;
        int y = 4;

        if (this.icon != null) {
            gfx.renderItem(this.icon, guiLeft + x, guiTop + y);
            gfx.renderItemDecorations(this.font, this.icon, guiLeft + x, guiTop + y);

            x += 18;
            y += (16 - this.font.lineHeight) / 2;
        }

        y += 1;

        gfx.drawString(this.font, pageTitle, guiLeft + x, guiTop + y, 0xFFFFFF, false);
    }

    private void drawGuiContainerBackgroundLayer(GuiGraphics gfx, float f, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        gfx.blit(texture, guiLeft, guiTop, 0, 0, 170, ySize);
        gfx.blit(texture, guiLeft + 170, guiTop, 52, 0, 30, ySize);
        gfx.blit(texture, guiLeft + 200, guiTop, 52, 0, 140, ySize);

        if (!back.isEmpty()) gfx.blit(texture, guiLeft + 3, guiTop + 3, 204, 0, 18, 18);
        if (!forward.isEmpty()) gfx.blit(texture, guiLeft + 21, guiTop + 3, 222, 0, 18, 18);

        // scroll bar
        gfx.blit(texture, guiLeft + xSize - 15, guiTop + getSliderPosition(), 192, 0, 12, 16);

        int x = guiLeft + 7;
        int y = guiTop + 30;
        int lineNum = 0;

        if (lines.size() > 1 && scrollProgress == lines.size() - 1) {
            gfx.blit(the_man, guiLeft + 60, guiTop + this.ySize - 84, 0, 0, 80, 80);
            gfx.blit(the_man, guiLeft + 140, guiTop + this.ySize - 60, 0, 80, 77, 39);

        } else for (List<ManualElement> line : lines) {
            lineNum++;

            if (lineNum <= this.scrollProgress) continue;

            int maxHeight = 0;
            int inset = 0;

            for (ManualElement element : line) {
                maxHeight = Math.max(maxHeight, element.getHeight());
            }

            if (y + maxHeight > guiTop + 219) break;

            if (line.isEmpty()) y += this.font.lineHeight;

            for (ManualElement element : line) {
                int elementX = x + inset;
                int elementY = y + (maxHeight - element.getHeight()) / 2;
                boolean mouseOver = (elementX <= mouseX && elementX + element.getWidth() > mouseX && elementY < mouseY && elementY + element.getHeight() >= mouseY);
                element.render(gfx, mouseOver, elementX, elementY, mouseX, mouseY);
                if (elementX <= lastClickX && elementX + element.getWidth() > lastClickX && elementY < lastClickY && elementY + element.getHeight() >= lastClickY)
                    element.onClick(this);
                inset += element.getWidth();
            }

            y += maxHeight + 2;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (keyCode == GLFW.GLFW_KEY_LEFT) back();
        if (keyCode == GLFW.GLFW_KEY_RIGHT) forward();

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(null);
            return true;
        }
        return true;
    }
}
