package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.handler.BobmazonOfferFactory.Offer;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ItemBobmazonPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

/** 1:1 {@code GUIScreenBobmazon}: drei Angebote pro Seite, Klick auf ein Angebot bestellt es. */
public class GUIScreenBobmazon extends Screen {

    protected static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_bobmazon.png");
    protected int xSize = 176 + 41;
    protected int ySize = 229;
    protected int guiLeft;
    protected int guiTop;
    int currentPage = 0;
    List<Offer> offers;
    List<FolderButton> buttons = new ArrayList<>();
    private final Player player;

    public GUIScreenBobmazon(Player player, List<Offer> offers) {
        super(Component.literal("Bobmazon"));
        this.player = player;
        this.offers = offers;
    }

    int getPageCount() {
        return (int) Math.ceil((offers.size() - 1) / 3);
    }

    @Override
    public void tick() {
        if (currentPage < 0)
            currentPage = 0;
        if (currentPage > getPageCount())
            currentPage = getPageCount();

        if (this.player.getMainHandItem().is(ModItems.BOBMAZON_HIDDEN.get()) && player.getName().getString().equals("SolsticeUnlimitd"))
            this.onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float f) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        this.drawGuiContainerBackgroundLayer(g, mouseX, mouseY);
        this.drawGuiContainerForegroundLayer(g, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        updateButtons();
    }

    protected void updateButtons() {

        if (!buttons.isEmpty())
            buttons.clear();

        for (int i = currentPage * 3; i < Math.min(currentPage * 3 + 3, offers.size()); i++) {
            buttons.add(new FolderButton(guiLeft + 34, guiTop + 35 + (54 * i) - currentPage * 3 * 54, offers.get(i)));
        }

        if (currentPage != 0)
            buttons.add(new FolderButton(guiLeft + 25 - 18, guiTop + 26 + (27 * 3), 1, "Previous"));
        if (currentPage != getPageCount())
            buttons.add(new FolderButton(guiLeft + 25 + (27 * 4) + 18 + 41, guiTop + 26 + (27 * 3), 2, "Next"));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int k) {
        int i = (int) mx, j = (int) my;
        try {
            for (FolderButton b : buttons)
                if (b.isMouseOnButton(i, j)) {
                    b.executeAction();
                    return true;
                }
        } catch (Exception ex) {
            updateButtons();
        }
        return super.mouseClicked(mx, my, k);
    }

    protected void drawGuiContainerForegroundLayer(GuiGraphics g, int i, int j) {

        String page = (currentPage + 1) + "/" + (getPageCount() + 1);
        g.drawString(this.font, page, guiLeft + this.xSize / 2 - this.font.width(page) / 2, guiTop + 205, 4210752, false);

        for (FolderButton b : buttons)
            if (b.isMouseOnButton(i, j))
                b.drawString(g, i, j);
    }

    protected void drawGuiContainerBackgroundLayer(GuiGraphics g, int i, int j) {
        g.blit(texture, guiLeft, guiTop, 0, 0, xSize, ySize);

        for (FolderButton b : buttons)
            b.drawButton(g, b.isMouseOnButton(i, j));
        for (FolderButton b : buttons)
            b.drawIcon(g, b.isMouseOnButton(i, j));

        for (int d = currentPage * 3; d < Math.min(currentPage * 3 + 3, offers.size()); d++) {
            drawRequirement(g, offers.get(d), guiLeft + 34, guiTop + 53 + (54 * d) - currentPage * 3 * 54);
        }
    }

    /** {@code Offer.drawRequirement}: Bewertungsbalken, Name (halbe Groesse), Preis, Kommentar und Erfolgssymbol. */
    private void drawRequirement(GuiGraphics g, Offer offer, int x, int y) {
        g.blit(texture, x + 19, y - 4, 176 + 41, 62, 39, 8);
        if (offer.rating > 0) g.blit(texture, x + 19, y - 4, 176 + 41, 54, offer.rating, 8);

        String count = "";
        if (offer.offer.getCount() > 1)
            count = " x" + offer.offer.getCount();

        g.pose().pushPose();
        float scale = 0.5F;
        g.pose().scale(scale, scale, scale);
        g.drawString(this.font, offer.offer.getHoverName().getString() + count, (int) ((x + 20) / scale), (int) ((y - 12) / scale), 4210752, false);
        g.pose().popPose();

        String price = offer.cost + " Cap";
        if (offer.cost != 1)
            price += "s";

        g.drawString(this.font, price, x + 62, y - 3, 4210752, false);

        g.pose().pushPose();
        g.pose().scale(0.5F, 0.5F, 0.5F);

        if (!offer.author.isEmpty())
            g.drawString(this.font, "- " + offer.author, (x + 20) * 2, (y + 18) * 2, 0x222222, false);
        g.drawString(this.font, offer.comment, (x + 20) * 2, (y + 8) * 2, 0x222222, false);

        g.pose().popPose();

        g.renderItem(offer.requirement.getIcon(), x + 1, y + 1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    class FolderButton {

        int xPos;
        int yPos;
        // 0: regular, 1: prev, 2: next
        int type;
        String info;
        Offer offer;

        public FolderButton(int x, int y, int t, String i) {
            xPos = x;
            yPos = y;
            type = t;
            info = i;
        }

        public FolderButton(int x, int y, Offer offer) {
            xPos = x;
            yPos = y;
            type = 0;
            this.offer = offer;
        }

        public boolean isMouseOnButton(int mouseX, int mouseY) {
            return xPos <= mouseX && xPos + 18 > mouseX && yPos < mouseY && yPos + 18 >= mouseY;
        }

        public void drawButton(GuiGraphics g, boolean b) {
            g.blit(texture, xPos, yPos, b ? 176 + 41 + 18 : 176 + 41, type == 1 ? 18 : (type == 2 ? 36 : 0), 18, 18);
        }

        public void drawIcon(GuiGraphics g, boolean b) {
            if (offer != null) {
                g.renderItem(offer.offer, xPos + 1, yPos + 1);
            }
        }

        public void drawString(GuiGraphics g, int x, int y) {
            if (info == null || info.isEmpty())
                return;

            g.renderTooltip(font, Component.literal(info), x, y);
        }

        public void executeAction() {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (type == 0) {
                ItemBobmazonPacket.sendToServer(player, offer);
            } else if (type == 1) {
                if (currentPage > 0)
                    currentPage--;
                updateButtons();
            } else if (type == 2) {
                if (currentPage < getPageCount())
                    currentPage++;
                updateButtons();
            }
        }
    }
}
