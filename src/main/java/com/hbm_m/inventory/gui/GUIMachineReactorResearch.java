package com.hbm_m.inventory.gui;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity;
import com.hbm_m.inventory.menu.MachineReactorResearchMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIReactorResearch}: drei Siebensegmentanzeigen (Fluss, Temperatur, Sollstellung), ein unsichtbares
 * dreistelliges Eingabefeld (8,99) fuer die Stabstellung in Prozent, der Knopf (44,97) schickt sie an den Reaktor.
 * Ist die Stellung hoechstens 50 %, leuchten die Stabmarken im Kerndiagramm.
 */
public class GUIMachineReactorResearch extends GuiInfoScreen<MachineReactorResearchMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_research_reactor.png");

    private final MachineReactorResearchBlockEntity reactor;
    private final NumberDisplay[] displays = new NumberDisplay[3];
    private byte timer;

    /** Original {@code GuiTextField} (8,99, 33x16, ohne Hintergrund, max. 3 Zeichen) - wird nie gezeichnet. */
    private String field = "0";
    private boolean fieldFocused = false;

    public GUIMachineReactorResearch(MachineReactorResearchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.reactor = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 222;
        displays[0] = new NumberDisplay(14, 25, 0x08FF00).setDigitLength(4);
        displays[1] = new NumberDisplay(12, 63, 0x08FF00).setDigitLength(3);
        displays[2] = new NumberDisplay(5, 101, 0x08FF00).setDigitLength(3);
    }

    @Override
    protected void init() {
        super.init();
        if (reactor != null) this.field = String.valueOf((int) (reactor.level * 100));
    }

    private static Component[] lines(String... text) {
        Component[] out = new Component[text.length];
        for (int i = 0; i < text.length; i++) out[i] = Component.literal(text[i]);
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        drawCustomInfoStat(g, mouseX, mouseY, -14, 23, 16, 16, leftPos - 6, topPos + 23 + 16, lines(
                "The reactor has to be submerged",
                "in water on its sides to cool.",
                "The neutron flux is provided to",
                "adjacent breeding reactors."));

        drawCustomInfoStat(g, mouseX, mouseY, -14, 61, 16, 16, leftPos - 6, topPos + 61 + 16, lines(
                "This reactor is fueled with plate fuel.",
                "The reaction needs a neutron source to start."));

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        String[] labels = { "Flux", "Heat", "Control" };

        g.drawString(this.font, this.title, 121 - this.font.width(this.title) / 2, 6, 15066597, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
        g.drawString(this.font, labels[0], 6, 13, 15066597, false);
        g.drawString(this.font, labels[1], 6, 51, 15066597, false);
        g.drawString(this.font, labels[2], 6, 89, 15066597, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean handled = super.mouseClicked(mx, my, button);
        int mouseX = (int) mx;
        int mouseY = (int) my;

        fieldFocused = leftPos + 8 <= mouseX && leftPos + 8 + 33 > mouseX && topPos + 99 <= mouseY && topPos + 99 + 16 > mouseY;

        if (leftPos + 8 <= mouseX && leftPos + 8 + 33 > mouseX && topPos + 99 < mouseY && topPos + 99 + 16 >= mouseY)
            displays[2].setBlinks(true);
        else
            displays[2].setBlinks(false);

        if (reactor != null && leftPos + 44 <= mouseX && leftPos + 44 + 11 > mouseX && topPos + 97 < mouseY && topPos + 97 + 20 >= mouseY) {

            double level;

            try {
                int j = (int) Mth.clamp(Double.parseDouble(field), 0, 100);
                field = j + "";
                level = j * 0.01D;
            } catch (NumberFormatException ex) {
                return true;
            }

            CompoundTag control = new CompoundTag();
            control.putDouble("level", level);
            timer = 15;

            NBTControlPacket.sendToServer(reactor.getBlockPos(), control);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    com.hbm_m.sound.HbmSoundsNT.get("hbm:block.rbmk_az5_cover"), 0.5F));
            return true;
        }

        return handled;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (fieldFocused) {
            if (c >= ' ' && c != 127 && field.length() < 3) field = field + c;
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (fieldFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!field.isEmpty()) field = field.substring(0, field.length() - 1);
                return true;
            }
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean isDigits(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (reactor == null) return;

        if (reactor.level <= 0.5D) {
            for (int x = 0; x < 3; x++)
                for (int y = 0; y < 3; y++)
                    g.blit(TEXTURE, leftPos + 81 + 36 * x, topPos + 26 + 36 * y, 176, 0, 8, 8);
            for (int x = 0; x < 2; x++)
                for (int y = 0; y < 2; y++)
                    g.blit(TEXTURE, leftPos + 99 + 36 * x, topPos + 44 + 36 * y, 176, 0, 8, 8);
        }

        if (timer > 0) {
            g.blit(TEXTURE, leftPos + 44, topPos + 97, 176, 8, 11, 20);
            timer--;
        }

        for (byte i = 0; i < 2; i++)
            displays[i].drawNumber(g, leftPos, topPos, reactor.getDisplayData()[i]);

        if (isDigits(field)) {
            int level = (int) Mth.clamp(Double.parseDouble(field), 0, 100);
            field = level + "";
            displays[2].drawNumber(g, leftPos, topPos, level);
        } else {
            field = "0";
            displays[2].drawNumber(g, leftPos, topPos, 0);
        }

        drawInfoPanel(g, -14, 23, PanelType.LARGE_GREEN_INFO);
        drawInfoPanel(g, -14, 61, PanelType.LARGE_BLUE_INFO);
    }
}
