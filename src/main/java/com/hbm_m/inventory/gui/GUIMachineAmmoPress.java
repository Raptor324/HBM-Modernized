package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity;
import com.hbm_m.inventory.menu.MachineAmmoPressMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AmmoPressRecipe;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Die Oberflaeche der Munitionspresse mit der <b>Rezeptliste</b> des Originals: links vier Spalten
 * zu drei Eintraegen, darunter ein Suchfeld, links und rechts davon die Blaetterpfeile.
 *
 * <p>Ein Klick waehlt das Zielrezept, ein zweiter Klick auf denselben Eintrag hebt die Auswahl
 * wieder auf. Solange etwas gewaehlt ist, zeigen die leeren Eingabefelder als blasse Schemen, was
 * dort hineingehoert - man sieht auf einen Blick, was noch fehlt.</p>
 */
public class GUIMachineAmmoPress extends AbstractContainerScreen<MachineAmmoPressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_ammo_press.png");

    /** Original: zwoelf sichtbare Eintraege, in vier Spalten zu je drei. */
    private static final int VISIBLE = 12;
    private static final int ROWS = 3;

    private final MachineAmmoPressBlockEntity blockEntity;

    private final List<Map.Entry<ResourceLocation, AmmoPressRecipe>> recipes = new ArrayList<>();
    private int index;
    private int maxIndex;
    private EditBox search;

    public GUIMachineAmmoPress(MachineAmmoPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();

        this.search = new EditBox(this.font, leftPos + 10, topPos + 75, 66, 12, Component.empty());
        this.search.setTextColor(-1);
        this.search.setTextColorUneditable(-1);
        this.search.setBordered(false);
        this.search.setMaxLength(25);
        this.search.setResponder(this::applySearch);
        this.addRenderableWidget(this.search);

        applySearch("");
    }

    // ── Liste ──

    private void applySearch(String text) {
        String needle = text.toLowerCase(Locale.ROOT);

        recipes.clear();

        if (minecraft != null && minecraft.level != null) {
            for (var entry : MachineAmmoPressBlockEntity.sortedRecipes(minecraft.level)) {
                if (needle.isEmpty() || entry.getValue().getOutput().getHoverName().getString()
                        .toLowerCase(Locale.ROOT).contains(needle)) {
                    recipes.add(entry);
                }
            }
        }

        // Original: {@code size = ceil((recipes - 12) / 3)} - je Blaetterschritt rueckt eine Spalte nach.
        index = 0;
        maxIndex = Math.max(0, (int) Math.ceil((recipes.size() - VISIBLE) / 3D));
    }

    /** Die Bildschirmecke des Eintrags mit dem Listenplatz {@code slot} (null bis elf). */
    private int entryX(int slot) { return 16 + 18 * (slot / ROWS); }
    private int entryY(int slot) { return 17 + 18 * (slot % ROWS); }

    // ── Zeichnen ──

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Original: die Blaetterpfeile leuchten nur, solange der Zeiger darauf steht.
        if (isHovering(7, 17, 9, 54, mouseX, mouseY))  guiGraphics.blit(TEXTURE, x + 7, y + 17, 176, 0, 9, 54);
        if (isHovering(88, 17, 9, 54, mouseX, mouseY)) guiGraphics.blit(TEXTURE, x + 88, y + 17, 185, 0, 9, 54);

        if (search != null && search.isFocused()) {
            guiGraphics.blit(TEXTURE, x + 8, y + 72, 176, 54, 70, 16);
        }

        ResourceLocation selected = blockEntity != null ? blockEntity.getSelectedRecipeId() : null;

        for (int i = index * 3; i < index * 3 + VISIBLE && i < recipes.size(); i++) {
            int slot = i - index * 3;
            var entry = recipes.get(i);

            int ex = x + entryX(slot);
            int ey = y + entryY(slot);

            // Original: ausgewaehlt aus (194,0), sonst aus (212,0).
            boolean isSelected = entry.getKey().equals(selected);
            guiGraphics.blit(TEXTURE, ex, ey, isSelected ? 194 : 212, 0, 18, 18);

            ItemStack out = entry.getValue().getOutput();
            guiGraphics.renderItem(out, ex + 1, ey + 1);
            guiGraphics.renderItemDecorations(font, out, ex + 1, ey + 1);
        }

        renderGhostInputs(guiGraphics, x, y);

        if (blockEntity != null && blockEntity.isPressing()) {
            guiGraphics.fill(x + 96, y + 20, x + 116, y + 52, 0xA0FF3020);
        }
    }

    /**
     * Original: fehlende Zutaten des gewaehlten Rezepts erscheinen blass im leeren Feld, und bei
     * Zutaten mit mehreren Moeglichkeiten wechselt die Anzeige im Sekundentakt durch.
     */
    private void renderGhostInputs(GuiGraphics guiGraphics, int x, int y) {
        if (blockEntity == null || minecraft == null || minecraft.level == null) return;

        AmmoPressRecipe recipe = blockEntity.getSelectedRecipe(minecraft.level);
        if (recipe == null) return;

        var inputs = recipe.getInputs();

        for (int i = 0; i < inputs.size() && i < 9; i++) {
            var ingredient = inputs.get(i);
            if (ingredient.isEmpty()) continue;
            if (!blockEntity.getInventory().getStackInSlot(i).isEmpty()) continue;

            ItemStack[] options = ingredient.getItems();
            if (options.length == 0) continue;

            ItemStack ghost = options[(int) (Math.abs(System.currentTimeMillis() / 1000) % options.length)];

            int gx = x + 116 + 18 * (i % 3);
            int gy = y + 18 + 18 * (i / 3);

            guiGraphics.renderItem(ghost, gx + 1, gy + 1);
            if (ghost.getCount() > 1) {
                guiGraphics.renderItemDecorations(font, ghost, gx + 1, gy + 1);
            }

            // Der halbtransparente Schleier aus der Hintergrundtextur legt sich darueber.
            guiGraphics.setColor(1F, 1F, 1F, 0.5F);
            guiGraphics.blit(TEXTURE, gx, gy, 116 + 18 * (i % 3), 18 + 18 * (i / 3), 18, 18);
            guiGraphics.setColor(1F, 1F, 1F, 1F);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFFFFFF, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Der Name des Eintrags unter dem Zeiger - die Liste hat sonst keine Beschriftung.
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            for (int i = index * 3; i < index * 3 + VISIBLE && i < recipes.size(); i++) {
                int slot = i - index * 3;
                if (isHovering(entryX(slot), entryY(slot), 18, 18, mouseX, mouseY)) {
                    guiGraphics.renderTooltip(font, recipes.get(i).getValue().getOutput(), mouseX, mouseY);
                    break;
                }
            }
        }
    }

    // ── Bedienung ──

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(7, 17, 9, 54, mouseX, mouseY)) {
            if (index > 0) index--;
            click();
            return true;
        }

        if (isHovering(88, 17, 9, 54, mouseX, mouseY)) {
            if (index < maxIndex) index++;
            click();
            return true;
        }

        for (int i = index * 3; i < index * 3 + VISIBLE && i < recipes.size(); i++) {
            int slot = i - index * 3;
            if (!isHovering(entryX(slot), entryY(slot), 18, 18, mouseX, mouseY)) continue;

            if (blockEntity != null) {
                // Der Server dreht die Auswahl selbst um, wenn dasselbe Rezept erneut kommt.
                com.hbm_m.network.AmmoPressSelectC2SPacket.send(blockEntity.getBlockPos(), recipes.get(i).getKey());
            }
            click();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    //? if < 1.21.1 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (scrollList(delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (scrollList(deltaY)) return true;
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
    *///?}

    private boolean scrollList(double delta) {
        if (delta > 0 && index > 0)        { index--; return true; }
        if (delta < 0 && index < maxIndex) { index++; return true; }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        // Damit sich das Inventar nicht schliesst, waehrend man einen Suchbegriff tippt.
        if (search != null && search.isFocused() && key != 256) {
            return search.keyPressed(key, scanCode, modifiers) || super.keyPressed(key, scanCode, modifiers);
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (search != null && search.isFocused()) return search.charTyped(c, modifiers);
        return super.charTyped(c, modifiers);
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
