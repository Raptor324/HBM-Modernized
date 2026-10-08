package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.TurretBaseBlockEntity;
import com.hbm_m.blockentity.machines.TurretStats;
import com.hbm_m.inventory.menu.TurretMenu;
import com.hbm_m.network.TurretControlPacket;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1:1 {@code GUITurretBase} (+ {@code GUITurretFritz}-Tank und {@code GUITurretArty}-Modusknopf) fuer alle
 * Geschuetze: An/Aus (115/26), vier Zielkategorien (8/22/36/50, 30), KI-Chip-Namensliste mit Eingabefeld,
 * Strichliste der Abschuesse, Energiebalken und Munitionsanzeige ueber dem Munitionsraster.
 * Die Textur kommt aus {@link TurretStats}.
 */
public class GUITurret extends AbstractContainerScreen<TurretMenu> {

    private final ResourceLocation texture;
    private EditBox field;
    private int index;

    public GUITurret(TurretMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        // тайл может отсутствовать в реплее Flashback
        this.texture = pMenu.blockEntity != null ? pMenu.blockEntity.getGuiTexture()
                : ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/gui/weapon/gui_turret_base.png");
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;

        // Original GuiTextField (10/65, 50x14, 25 Zeichen); gezeichnet wird der Text selbst (halbe Groesse, gruen)
        this.field = new EditBox(this.font, this.leftPos + 10, this.topPos + 65, 50, 14, Component.empty());
        this.field.setBordered(false);
        this.field.setMaxLength(25);
        this.addWidget(this.field);
    }

    private TurretBaseBlockEntity turret() {
        return menu.blockEntity;
    }

    private boolean isArty() {
        // тайл может отсутствовать в реплее Flashback
        return turret() != null && turret().getStats() == TurretStats.ARTY;
    }

    private boolean isHimars() {
        return turret() != null && turret().getStats() == TurretStats.HIMARS;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, gui, mouseX, mouseY, delta);
        super.render(gui, mouseX, mouseY, delta);
        this.renderTooltip(gui, mouseX, mouseY);

        TurretBaseBlockEntity turret = turret();
        if (turret == null) return;

        // drawElectricityInfo(152, 45, 16, 52)
        if (isMouseOver(mouseX, mouseY, 152, 45, 16, 52)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(EnergyFormatter.format(menu.getEnergyLong()) + "/" + EnergyFormatter.format(menu.getMaxEnergyLong()) + "HE"));
            gui.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        Component on = Component.translatable("turret.on").withStyle(ChatFormatting.GREEN);
        Component off = Component.translatable("turret.off").withStyle(ChatFormatting.RED);
        String[] keys = { "turret.players", "turret.animals", "turret.mobs", "turret.machines" };
        boolean[] states = { turret.isTargetingPlayers(), turret.isTargetingAnimals(), turret.isTargetingMobs(), turret.isTargetingMachines() };
        for (int i = 0; i < 4; i++) {
            if (isMouseOver(mouseX, mouseY, 8 + i * 14, 30, 10, 10)) {
                gui.renderComponentTooltip(this.font, List.of(Component.translatable(keys[i], states[i] ? on : off)), mouseX, mouseY);
            }
        }

        // GUITurretFritz: Tankanzeige
        if (turret.getStats() == TurretStats.FRITZ) {
            turret.getTank().renderTankInfo(gui, this.font, mouseX, mouseY, this.leftPos + 134, this.topPos + 63, 7, 52);
        }

        // GUITurretArty: Modusanzeige
        if (isArty() && isMouseOver(mouseX, mouseY, 151, 16, 18, 18)) {
            int mode = turret.getFireMode();
            String m = mode == TurretBaseBlockEntity.ARTY_MODE_ARTILLERY ? "artillery" : mode == TurretBaseBlockEntity.ARTY_MODE_CANNON ? "cannon" : "manual";
            // I18nUtil.resolveKeyArray: "$" trennt die Zeilen
            List<Component> lines = new ArrayList<>();
            for (String line : Component.translatable("turret.arty." + m).getString().split("\\$")) lines.add(Component.literal(line));
            gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }

        // GUITurretHIMARS: Modusanzeige
        if (isHimars() && isMouseOver(mouseX, mouseY, 151, 16, 18, 18)) {
            String m = turret.getFireMode() == 0 ? "artillery_rocket" : "manual_rocket";
            List<Component> lines = new ArrayList<>();
            for (String line : Component.translatable("turret.arty." + m).getString().split("\\$")) lines.add(Component.literal(line));
            gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }

        // Munitionsanzeige ueber dem leeren Munitionsraster
        if (menu.getCarried().isEmpty() && this.leftPos + 79 <= mouseX && this.leftPos + 79 + 54 > mouseX && this.topPos + 62 < mouseY && this.topPos + 62 + 54 >= mouseY) {
            boolean draw = true;
            for (int i = 1; i < 10; i++) { // Menue-Slots 1-9 = Munition (wie Original)
                Slot slot = this.menu.slots.get(i);
                if (isMouseOver(mouseX, mouseY, slot.x, slot.y, 16, 16) && slot.hasItem()) {
                    draw = false;
                    break;
                }
            }
            if (draw) drawAmmoTypes(gui, turret, mouseX, mouseY);
        }
    }

    /** Original {@code drawStackText}: Zeilen mit Munitionssymbolen, das gerade gewaehlte darunter mit Namen. */
    private void drawAmmoTypes(GuiGraphics gui, TurretBaseBlockEntity turret, int mouseX, int mouseY) {
        List<ItemStack> list = new ArrayList<>(turret.getAmmoTypesForDisplay());
        if (list.isEmpty()) return;

        int selectedIndex = 0;
        if (list.size() > 1) {
            selectedIndex = (int) ((System.currentTimeMillis() % (1000L * list.size())) / 1000);
        }
        ItemStack selected = list.get(selectedIndex);

        List<List<ItemStack>> lines = new ArrayList<>();
        if (list.size() < 10) {
            lines.add(list);
        } else if (list.size() < 24) {
            lines.add(list.subList(0, list.size() / 2));
            lines.add(list.subList(list.size() / 2, list.size()));
        } else {
            int bound0 = (int) Math.ceil(list.size() / 3D);
            int bound1 = (int) Math.ceil(list.size() / 3D * 2D);
            lines.add(list.subList(0, bound0));
            lines.add(list.subList(bound0, bound1));
            lines.add(list.subList(bound1, list.size()));
        }

        Component name = selected.getHoverName();
        int width = this.font.width(name);
        for (List<ItemStack> line : lines) width = Math.max(width, line.size() * 18);
        int height = lines.size() * 18 + 10;

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + width + 4 > this.width) x = mouseX - 16 - width;
        if (y + height + 6 > this.height) y = this.height - height - 6;

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 400);
        gui.fill(x - 3, y - 4, x + width + 3, y + height + 3, 0xF0100010);
        gui.renderOutline(x - 3, y - 4, width + 6, height + 7, 0x505000FF);

        int row = 0;
        for (List<ItemStack> line : lines) {
            int col = 0;
            for (ItemStack stack : line) {
                int ix = x + col * 18;
                int iy = y + row * 18;
                if (stack == selected) gui.fill(ix - 1, iy - 1, ix + 17, iy + 17, 0x80FFFFFF);
                gui.renderItem(stack, ix, iy);
                col++;
            }
            row++;
        }
        gui.drawString(this.font, name, x, y + lines.size() * 18 + 1, 0xFFFFFF);
        gui.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean result = super.mouseClicked(x, y, button);

        TurretBaseBlockEntity turret = turret();
        // тайл может отсутствовать в реплее Flashback
        if (turret == null) return result;

        boolean flag = x >= this.field.getX() && x < this.field.getX() + this.field.getWidth() && y >= this.field.getY() && y < this.field.getY() + this.field.getHeight();
        this.field.setFocused(flag);

        if (inRect(x, y, 115, 26, 18, 18)) {
            click();
            TurretControlPacket.sendToServer(turret.getBlockPos(), TurretControlPacket.ACTION_TOGGLE_ON);
            return true;
        }

        int[] actions = { TurretControlPacket.ACTION_TOGGLE_PLAYERS, TurretControlPacket.ACTION_TOGGLE_ANIMALS, TurretControlPacket.ACTION_TOGGLE_MOBS, TurretControlPacket.ACTION_TOGGLE_MACHINES };
        for (int i = 0; i < 4; i++) {
            if (inRect(x, y, 8 + i * 14, 30, 10, 10)) {
                click();
                TurretControlPacket.sendToServer(turret.getBlockPos(), actions[i]);
                return true;
            }
        }

        int count = getCount();

        if (count > 0) {
            if (inRect(x, y, 7, 80, 18, 18)) {
                index--;
                if (index < 0) index = count - 1;
                click();
                return true;
            }
            if (inRect(x, y, 43, 80, 18, 18)) {
                index++;
                index %= count;
                click();
                return true;
            }
        }

        if (inRect(x, y, 7, 98, 18, 18)) {
            click();
            if (this.field.getValue().isEmpty()) return true;
            TurretControlPacket.sendToServer(turret.getBlockPos(), TurretControlPacket.ACTION_ADD_NAME, this.field.getValue(), 0);
            this.field.setValue("");
            return true;
        }

        if (inRect(x, y, 43, 98, 18, 18)) {
            click();
            TurretControlPacket.sendToServer(turret.getBlockPos(), TurretControlPacket.ACTION_DEL_NAME, "", this.index);
            return true;
        }

        // GUITurretArty: Modus umschalten
        if ((isArty() || isHimars()) && inRect(x, y, 151, 16, 18, 18)) { // GUITurretHIMARS ebenso
            click();
            TurretControlPacket.sendToServer(turret.getBlockPos(), TurretControlPacket.ACTION_CYCLE_FIRE_MODE);
            return true;
        }

        return result;
    }

    /** Original: {@code guiLeft + x <= mx && guiLeft + x + w > mx && guiTop + y < my && guiTop + y + h >= my}. */
    private boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return this.leftPos + x <= mx && this.leftPos + x + w > mx && this.topPos + y < my && this.topPos + y + h >= my;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.field.isFocused() && keyCode != 256) {
            this.field.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (this.field.isFocused()) return this.field.charTyped(c, modifiers);
        return super.charTyped(c, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        Component name = this.title;
        gui.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        gui.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        TurretBaseBlockEntity turret = turret();
        if (turret == null) return;

        List<String> names = turret.getWhitelist();

        Component n = Component.translatable("turret.none").withStyle(ChatFormatting.ITALIC);

        while (this.index >= this.getCount() && this.index > 0)
            this.index--;

        if (index < 0)
            index = 0;

        if (names != null && index < names.size()) {
            n = Component.literal(names.get(index));
        }

        String t = this.field.getValue();
        String cursor = System.currentTimeMillis() % 1000 < 500 ? " " : "||";

        if (this.field.isFocused()) {
            int c = Math.min(this.field.getCursorPosition(), t.length());
            t = t.substring(0, c) + cursor + t.substring(c);
        }

        double scale = 2;
        gui.pose().pushPose();
        gui.pose().scale((float) (1D / scale), (float) (1D / scale), 1F);
        gui.drawString(this.font, n, (int) (12 * scale), (int) (51 * scale), 0x00ff00, false);
        gui.drawString(this.font, t, (int) (12 * scale), (int) (69 * scale), 0x00ff00, false);
        gui.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mX, int mY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int guiLeft = this.leftPos;
        int guiTop = this.topPos;
        gui.blit(texture, guiLeft, guiTop, 0, 0, this.imageWidth, this.imageHeight);

        if (inRect(mX, mY, 7, 80, 18, 18)) gui.blit(texture, guiLeft + 7, guiTop + 80, 176, 58, 18, 18);
        if (inRect(mX, mY, 43, 80, 18, 18)) gui.blit(texture, guiLeft + 43, guiTop + 80, 194, 58, 18, 18);
        if (inRect(mX, mY, 7, 98, 18, 18)) gui.blit(texture, guiLeft + 7, guiTop + 98, 176, 76, 18, 18);
        if (inRect(mX, mY, 43, 98, 18, 18)) gui.blit(texture, guiLeft + 43, guiTop + 98, 194, 76, 18, 18);

        TurretBaseBlockEntity turret = turret();
        // тайл может отсутствовать в реплее Flashback
        if (turret == null) return;

        long max = menu.getMaxEnergyLong();
        int i = max > 0 ? (int) (menu.getEnergyLong() * 53 / max) : 0;
        gui.blit(texture, guiLeft + 152, guiTop + 97 - i, 194, 52 - i, 16, i);

        if (turret.isOn())
            gui.blit(texture, guiLeft + 115, guiTop + 26, 176, 40, 18, 18);

        if (turret.isTargetingPlayers())
            gui.blit(texture, guiLeft + 8, guiTop + 30, 176, 0, 10, 10);
        if (turret.isTargetingAnimals())
            gui.blit(texture, guiLeft + 22, guiTop + 30, 176, 10, 10, 10);
        if (turret.isTargetingMobs())
            gui.blit(texture, guiLeft + 36, guiTop + 30, 176, 20, 10, 10);
        if (turret.isTargetingMachines())
            gui.blit(texture, guiLeft + 50, guiTop + 30, 176, 30, 10, 10);

        int tallies = turret.getStattrak();

        if (tallies >= 36) {
            gui.blit(texture, guiLeft + 77, guiTop + 50, 176, 120, 63, 6);
        } else {
            int steps = (int) Math.ceil(tallies / 5D);

            for (int s = 0; s < steps; s++) {
                int m = tallies % 5;

                if (s < steps - 1 || m == 0) {
                    gui.blit(texture, guiLeft + 77 + 9 * s, guiTop + 50, 194, 94, 9, 6);
                } else {
                    gui.blit(texture, guiLeft + 77 + 9 * s, guiTop + 50, 176, 94, m * 2, 6);
                }
            }
        }

        // GUITurretFritz
        if (turret.getStats() == TurretStats.FRITZ) {
            turret.getTank().renderTank(gui, guiLeft + 134, guiTop + 63, 7, 52);
        }

        // GUITurretArty
        if (isArty()) {
            int mode = turret.getFireMode();
            if (mode == TurretBaseBlockEntity.ARTY_MODE_CANNON) gui.blit(texture, guiLeft + 151, guiTop + 16, 210, 0, 18, 18);
            if (mode == TurretBaseBlockEntity.ARTY_MODE_MANUAL) gui.blit(texture, guiLeft + 151, guiTop + 16, 210, 18, 18, 18);
        }

        // GUITurretHIMARS
        if (isHimars() && turret.getFireMode() == 1) gui.blit(texture, guiLeft + 151, guiTop + 16, 210, 0, 18, 18);
    }

    private int getCount() {
        // тайл может отсутствовать в реплее Flashback
        if (turret() == null) return 0;
        List<String> names = turret().getWhitelist();
        if (names == null) return 0;
        return names.size();
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return mouseX >= this.leftPos + x && mouseX <= this.leftPos + x + sizeX
                && mouseY >= this.topPos + y && mouseY <= this.topPos + y + sizeY;
    }
}
