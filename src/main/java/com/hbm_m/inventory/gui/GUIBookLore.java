package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.inventory.gui.GUIBookLore}: Doppelseite 272x182 (Textur 512x512, Einband in {@code cov_col}
 * eingefaerbt), je Seite ein 100 px breiter Textblock, {@code $} erzwingt einen Zeilenumbruch.
 */
public class GUIBookLore extends Screen {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/book/book_lore.png");

    protected int guiLeft;
    protected int guiTop;
    protected static int sizeX = 272;
    protected static int sizeY = 182;

    protected String key;
    protected CompoundTag tag;

    //judgement
    protected int color;

    protected int page;
    protected int maxPage;

    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new GUIBookLore(stack));
    }

    public GUIBookLore(ItemStack stack) {
        super(Component.empty());
        if (!stack.hasTag()) return;
        this.tag = stack.getTag();
        this.key = tag.getString("k");
        if (key.isEmpty()) return;

        this.color = tag.getInt("cov_col");
        if (color <= 0)
            color = 0x303030;
        this.maxPage = (int) Math.ceil(tag.getInt("p") / 2D) - 1;
    }

    @Override
    protected void init() {
        if (key == null || key.isEmpty()) {
            this.onClose();
            return;
        }
        this.guiLeft = (this.width - sizeX) / 2;
        this.guiTop = (this.height - sizeY) / 2;
    }

    @Override
    public void render(GuiGraphics g, int i, int j, float f) {
        this.renderBackground(g);
        this.drawGuiContainerBackgroundLayer(g, f, i, j);
        this.drawGuiContainerForegroundLayer(g, i, j);
    }

    protected void drawGuiContainerBackgroundLayer(GuiGraphics g, float f, int i, int j) {
        float r = (float) (color >> 16 & 255) / 255F;
        float gr = (float) (color >> 8 & 255) / 255F;
        float b = (float) (color & 255) / 255F;
        g.setColor(r, gr, b, 1.0F);
        g.blit(texture, guiLeft, guiTop, 0, 0, sizeX, sizeY, 512, 512);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(texture, guiLeft + 7, guiTop + 7, 0, 182, 258, 165, 512, 512);

        final boolean overY = j >= guiTop + 155 && j < guiTop + 165;
        if (page > 0) {
            if (overY && i >= guiLeft + 24 && i <= guiLeft + 42)
                g.blit(texture, guiLeft + 24, guiTop + 155, 295, 13, 18, 10, 512, 512);
            else
                g.blit(texture, guiLeft + 24, guiTop + 155, 272, 13, 18, 10, 512, 512);
        }

        if (page < maxPage) {
            if (overY && i >= guiLeft + 230 && i <= guiLeft + 248)
                g.blit(texture, guiLeft + 230, guiTop + 155, 295, 0, 18, 10, 512, 512);
            else
                g.blit(texture, guiLeft + 230, guiTop + 155, 272, 0, 18, 10, 512, 512);
        }
    }

    protected void drawGuiContainerForegroundLayer(GuiGraphics g, int x, int y) {
        String k = "book_lore." + key + ".page.";

        for (int i = 0; i < 2; i++) {
            int defacto = this.page * 2 + i;

            if (defacto < tag.getInt("p")) {
                String text;
                CompoundTag argTag = tag.getCompound("p" + defacto);

                if (argTag.isEmpty())
                    text = I18n.get(k + defacto);
                else {
                    List<String> args = new ArrayList<>();
                    int index = 1;
                    String arg = argTag.getString("a1");

                    while (!arg.isEmpty()) {
                        args.add(arg);
                        index++;
                        arg = argTag.getString("a" + index);
                    }

                    text = I18n.get(k + defacto, args.toArray());
                }

                float scale = 1;
                int width = 100;
                int widthScaled = (int) (width * scale);

                List<String> lines = new ArrayList<>();
                String[] words = text.split(" ");

                lines.add(words[0]);
                int indent = this.font.width(words[0]);

                for (int w = 1; w < words.length; w++) {
                    if (words[w].equals("$")) {
                        if (w + 1 < words.length && !words[w + 1].equals("$")) {
                            lines.add(words[++w]);
                            indent = this.font.width(words[w]);
                        } else
                            lines.add("");

                        continue;
                    }

                    indent += this.font.width(" " + words[w]);

                    if (indent <= widthScaled) {
                        String last = lines.get(lines.size() - 1);
                        lines.set(lines.size() - 1, last + (" " + words[w]));
                    } else {
                        lines.add(words[w]);
                        indent = this.font.width(words[w]);
                    }
                }

                for (int l = 0; l < lines.size(); l++) {
                    g.drawString(this.font, lines.get(l),
                            (int) ((guiLeft + 20 + i * 130) * scale),
                            (int) ((guiTop + 20) * scale + (9 * l)),
                            0x0F0F0F, false);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = (int) mx;
        int j = (int) my;
        if (j < guiTop + 155 || j >= guiTop + 165) return false;

        if (page > 0 && i >= guiLeft + 24 && i <= guiLeft + 42) {
            page--;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }

        if (page < maxPage && i >= guiLeft + 230 && i <= guiLeft + 248) {
            page++;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
