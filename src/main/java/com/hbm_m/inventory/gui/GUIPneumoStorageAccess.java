package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.PneumoStorageAccessMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.PneumoAccessStateC2SPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * 1:1-Port von {@code GUIPneumoStorageAccess} (1.7.10), 210x251.
 *
 * <p>Acht mal sechs Felder zeigen das Verzeichnis des Terminals. Ueber jedem Feld steht die
 * Gesamtmenge - abgekuerzt, weil sie ohne weiteres in die Millionen gehen kann. Darunter das
 * Suchfeld bei (79, 127), rechts der Bildlaufbalken: er laesst sich ziehen, und das Mausrad
 * blaettert zeilenweise.</p>
 *
 * <p>Links haengt eine schmale Leiste mit sechs Schaltern: vier Sortierweisen (Menge,
 * Gegenstandsnummer, angezeigter Name, interner Name), dazu ob das Suchfeld beim Oeffnen gleich
 * den Zeiger bekommt und ob die Suche auch die Kurzinfos durchsucht. Die letzten beiden merkt
 * sich der Bildschirm ueber die Sitzung hinweg - im Original sind das ebenfalls statische
 * Felder.</p>
 */
public class GUIPneumoStorageAccess extends AbstractContainerScreen<PneumoStorageAccessMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/storage/gui_pneumatic_access.png");

    /** Original: die Oberflaeche ist um 34 Bildpunkte breiter, der Rumpf beginnt entsprechend. */
    private static final int H_OFFSET = 34;

    private static final int SEARCH_X = 45 + H_OFFSET;
    private static final int SEARCH_Y = 127;
    private static final int SEARCH_W = 86;
    private static final int SEARCH_H = 12;

    private static final int BUTTON_X = 7;
    private static final int BUTTON_FIRST_Y = 7;
    private static final int BUTTON_FOCUS_Y = 79;
    private static final int BUTTON_DETAIL_Y = 97;

    private static final int SCROLL_X = 154 + H_OFFSET;
    /** Original: {@code 106 - 15} - Balkenhoehe minus Griffhoehe. */
    private static final int SCROLL_AREA = 106 - 15;

    /** Original: {@code protected static boolean startFocussed} - haelt ueber die Sitzung. */
    private static boolean startFocussed = false;
    /** Ebenfalls statisch im Original, damit die Wahl beim naechsten Oeffnen noch steht. */
    private static boolean detailedSearch = false;
    private static int sorting = PneumoStorageAccessMenu.SORT_BY_STACK_SIZE;

    private EditBox search;
    private int scrollIndex = 0;
    private int scrollBounds = 1;
    private boolean draggingScroll = false;

    public GUIPneumoStorageAccess(PneumoStorageAccessMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176 + H_OFFSET;
        this.imageHeight = 251;
        this.inventoryLabelX = 8 + H_OFFSET;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.titleLabelX = H_OFFSET + 88;
        this.titleLabelY = 5;
    }

    @Override
    protected void init() {
        super.init();

        String previous = search != null ? search.getValue() : "";

        search = new EditBox(this.font, leftPos + SEARCH_X, topPos + SEARCH_Y + 2, SEARCH_W, SEARCH_H,
                Component.translatable("gui.hbm_m.pneumo.access.search"));
        search.setTextColor(0xFFFFFF);
        search.setBordered(false);
        search.setMaxLength(50);
        search.setValue(previous);
        search.setResponder(value -> {
            scrollIndex = 0;
            sendState();
        });

        addRenderableWidget(search);

        if (startFocussed) setFocused(search);

        // Der Server kennt Sortierung und Suchart noch nicht, wenn die Oberflaeche neu aufgeht.
        sendState();
    }

    /** Alles, was der Server ueber die Anzeige wissen muss, geht in einem Rutsch hinueber. */
    private void sendState() {
        PneumoAccessStateC2SPacket.send(scrollIndex,
                search != null ? search.getValue() : "", sorting, detailedSearch);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Original: Rumpf um 34 nach rechts versetzt, davor die schmale Schalterleiste.
        guiGraphics.blit(TEXTURE, leftPos + H_OFFSET, topPos, 0, 0, 176, imageHeight);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 176, 15, 32, 122);

        guiGraphics.blit(TEXTURE, leftPos + BUTTON_X, topPos + BUTTON_FIRST_Y + sorting * 18, 208, 0, 18, 18);
        if (startFocussed) {
            guiGraphics.blit(TEXTURE, leftPos + BUTTON_X, topPos + BUTTON_FOCUS_Y, 208, 18, 18, 18);
        }
        if (detailedSearch) {
            guiGraphics.blit(TEXTURE, leftPos + BUTTON_X, topPos + BUTTON_DETAIL_Y, 208, 18, 18, 18);
        }

        guiGraphics.blit(TEXTURE, leftPos + SCROLL_X, topPos + scrollBarY(),
                draggingScroll ? 188 : 176, 0, 12, 15);
    }

    /** 1:1-Port von {@code getScrollBarYPos}. */
    private int scrollBarY() {
        double progress = scrollBounds > 0 ? (double) scrollIndex / (double) scrollBounds : 0D;
        if (progress > 1D) progress = 1D;
        return 17 + (int) (progress * SCROLL_AREA);
    }

    /** 1:1-Port von {@code scrollBounds = ceil(getStackCount() / 8 - 6)}, mindestens eins. */
    private void updateScrollBounds() {
        scrollBounds = (int) Math.ceil(menu.getListingSize() / 8D - 6D);
        if (scrollBounds < 1) scrollBounds = 1;
        setScroll(scrollIndex);
    }

    private void setScroll(int scroll) {
        int previous = scrollIndex;
        scrollIndex = Mth.clamp(scroll, 0, scrollBounds);
        if (previous != scrollIndex) sendState();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // Die Mengen stehen als Kurzzahl in der unteren rechten Ecke des Feldes.
        for (int i = 0; i < PneumoStorageAccessMenu.GRID_SIZE; i++) {
            long amount = menu.getAmount(i);
            if (amount <= 0) continue;

            Slot slot = menu.slots.get(i);
            String text = shortNumber(amount);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0F, 0F, 300F);
            guiGraphics.pose().scale(0.5F, 0.5F, 1F);
            guiGraphics.drawString(font, text,
                    (slot.x + 17) * 2 - font.width(text), (slot.y + 10) * 2, 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }
    }

    /** Grosse Mengen kuerzen, sonst passt die Zahl nicht ins Feld. */
    private static String shortNumber(long amount) {
        if (amount >= 1_000_000_000L) return (amount / 1_000_000_000L) + "G";
        if (amount >= 1_000_000L) return (amount / 1_000_000L) + "M";
        if (amount >= 10_000L) return (amount / 1_000L) + "k";
        return Long.toString(amount);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateScrollBounds();

        // Original: der Griff folgt der Maus, solange die Taste haengt.
        if (draggingScroll) {
            int sY = Mth.clamp(mouseY - topPos - 24, 0, 92);
            setScroll((int) Math.round(scrollBounds * (sY / 92D)));
        }

        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        renderButtonTooltips(guiGraphics, mouseX, mouseY);

        // Ueber einem belegten Feld die genaue Menge nennen - die Kurzzahl rundet ja.
        for (int i = 0; i < PneumoStorageAccessMenu.GRID_SIZE; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) continue;
            if (!isOverSlot(slot, mouseX, mouseY)) continue;

            guiGraphics.renderComponentTooltip(font, List.of(
                    slot.getItem().getHoverName(),
                    Component.translatable("gui.hbm_m.pneumo.access.count", menu.getAmount(i))),
                    mouseX, mouseY);
            break;
        }
    }

    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String[] sortKeys = {
                "gui.hbm_m.pneumo.access.sort.amount",
                "gui.hbm_m.pneumo.access.sort.id",
                "gui.hbm_m.pneumo.access.sort.name",
                "gui.hbm_m.pneumo.access.sort.internal"
        };

        for (int i = 0; i < sortKeys.length; i++) {
            if (isOverButton(BUTTON_FIRST_Y + i * 18, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(font,
                        List.of(Component.translatable(sortKeys[i])), mouseX, mouseY);
                return;
            }
        }

        if (isOverButton(BUTTON_FOCUS_Y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(Component.translatable(
                    startFocussed ? "gui.hbm_m.pneumo.access.focus.on"
                                  : "gui.hbm_m.pneumo.access.focus.off")), mouseX, mouseY);
            return;
        }

        if (isOverButton(BUTTON_DETAIL_Y, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(font, List.of(Component.translatable(
                    detailedSearch ? "gui.hbm_m.pneumo.access.detail.on"
                                   : "gui.hbm_m.pneumo.access.detail.off")), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        for (int i = 0; i <= PneumoStorageAccessMenu.SORT_BY_INTERNAL; i++) {
            if (isOverButton(BUTTON_FIRST_Y + i * 18, mx, my)) {
                playClick();
                sorting = i;
                scrollIndex = 0;
                sendState();
                return true;
            }
        }

        if (isOverButton(BUTTON_FOCUS_Y, mx, my)) {
            playClick();
            startFocussed = !startFocussed;
            return true;
        }

        if (isOverButton(BUTTON_DETAIL_Y, mx, my)) {
            playClick();
            detailedSearch = !detailedSearch;
            scrollIndex = 0;
            sendState();
            return true;
        }

        // Original: der Griff wird ueber den ganzen Balken gegriffen, nicht nur ueber dem Knopf.
        int localX = mx - leftPos;
        int localY = my - topPos;
        if (localX >= SCROLL_X - 1 && localX < SCROLL_X + 13 && localY > 16 && localY <= 16 + 108) {
            draggingScroll = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Original: gescrollt wird zeilenweise durch das Verzeichnis. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        setScroll(scrollIndex - (int) Math.signum(delta));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Solange das Suchfeld den Zeiger hat, darf "e" die Oberflaeche nicht schliessen.
        if (search != null && search.isFocused() && keyCode != 256) {
            return search.keyPressed(keyCode, scanCode, modifiers) || search.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean isOverButton(int y, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= BUTTON_X && localX < BUTTON_X + 18 && localY >= y && localY < y + 18;
    }

    private boolean isOverSlot(Slot slot, int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= slot.x && localX < slot.x + 16 && localY >= slot.y && localY < slot.y + 16;
    }
}
