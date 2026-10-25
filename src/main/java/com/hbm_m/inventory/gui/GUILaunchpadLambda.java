package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.LaunchpadLambdaBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.LaunchpadLambdaMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUILaunchpadLambda}: Tanks fuer Benzin und Peroxid, Energiesaeule, drei Bereitschafts-LEDs, Schalter
 * Hand/Automatik, Startknopf und Statusanzeige (Leerlauf, Laden, Bereit oder Countdown).
 */
public class GUILaunchpadLambda extends GuiInfoScreen<LaunchpadLambdaMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_launchpad_lambda.png");

    private final LaunchpadLambdaBlockEntity launcher;

    public GUILaunchpadLambda(LaunchpadLambdaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.launcher = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 226;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (launcher != null) {
            launcher.tanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 107, topPos + 26, 16, 52);
            launcher.tanks[1].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 125, topPos + 26, 16, 52);
            drawElectricityInfo(g, mouseX, mouseY, 89, 26, 16, 52, launcher.getEnergyStored(), LaunchpadLambdaBlockEntity.MAX_POWER);

            drawCustomInfoStat(g, mouseX, mouseY, 34, 43, 18, 18, mouseX, mouseY, resolveKeyArray("desc.gui.lambda.manual"));
            drawCustomInfoStat(g, mouseX, mouseY, 52, 43, 18, 18, mouseX, mouseY, resolveKeyArray("desc.gui.lambda.auto"));
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (launcher != null) {
            CompoundTag data = null;
            int mx = (int) x, my = (int) y;

            if (isPointInRect(34, 43, 18, 18, mx, my)) {
                data = new CompoundTag();
                data.putBoolean("auto", false);
            }
            if (isPointInRect(52, 43, 18, 18, mx, my)) {
                data = new CompoundTag();
                data.putBoolean("auto", true);
            }
            if (isPointInRect(43, 70, 18, 18, mx, my) && launcher.erected) {
                data = new CompoundTag();
                data.putBoolean("launch", true);
            }

            if (data != null) {
                playClickSound();
                NBTControlPacket.sendToServer(launcher.getBlockPos(), data);
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
        if (launcher == null) return;

        if (launcher.erected && launcher.countdown > 0) {
            int countdown = launcher.countdown;
            String secs = "" + countdown / 20;
            String cents = "" + (countdown % 20) * 5;
            if (secs.length() == 1) secs = "0" + secs;
            if (cents.length() == 1) cents += "0";
            g.drawString(this.font, secs + ":" + cents, 40, 103, 0xff0000, false);
        } else if (!launcher.hasRocketLoaded()) {
            drawConstrainedLabel(g, Component.translatable("desc.gui.soyuz.idle").getString(), 53, 107, 0xff0000);
        } else if (!launcher.erected) {
            drawConstrainedLabel(g, Component.translatable("desc.gui.soyuz.loading").getString(), 53, 107, 0xff8000);
        } else {
            drawConstrainedLabel(g, Component.translatable("desc.gui.soyuz.ready").getString(), 53, 107, 0x00ff00);
        }
    }

    /** Original {@code drawConstrainedLabel}: auf hoechstens 22 px Breite verkleinert, um (x, y) zentriert. */
    private void drawConstrainedLabel(GuiGraphics g, String label, int x, int y, int color) {
        int width = Math.max(1, this.font.width(label));
        float scale = Math.min(1F, 22F / width);
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1F);
        g.drawString(this.font, label, (int) (x / scale - width / 2F), (int) (y / scale - this.font.lineHeight / 2F), color, false);
        g.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (launcher == null) return;

        int power = (int) (launcher.getEnergyStored() * 52 / LaunchpadLambdaBlockEntity.MAX_POWER);
        g.blit(TEXTURE, leftPos + 89, topPos + 78 - power, 194, 52 - power, 16, power);

        g.blit(TEXTURE, leftPos + 34 + (launcher.autolaunch ? 18 : 0), topPos + 43, 210 + (launcher.autolaunch ? 18 : 0), 26, 18, 18);

        g.blit(TEXTURE, leftPos + 112, topPos + 13, launcher.hasJetFuel() ? 210 : 216, 0, 6, 8);
        g.blit(TEXTURE, leftPos + 130, topPos + 13, launcher.hasOxidizer() ? 210 : 216, 0, 6, 8);
        g.blit(TEXTURE, leftPos + 94, topPos + 13, launcher.getEnergyStored() >= LaunchpadLambdaBlockEntity.CONSUMPTION ? 210 : 216, 0, 6, 8);

        if (launcher.countdown > 0)
            g.blit(TEXTURE, leftPos + 43, topPos + 70, 210, 44, 18, 18);

        g.blit(TEXTURE, leftPos + 34, topPos + 16, launcher.hasRocketLoaded() ? 228 : 210, 8, 18, 18);

        var sat = launcher.getInventory().getStackInSlot(LaunchpadLambdaBlockEntity.SLOT_SATELLITE);
        g.blit(TEXTURE, leftPos + 52, topPos + 16, sat.isEmpty() || LaunchpadLambdaBlockEntity.needsOrbiter(sat) ? 210 : 228, 8, 18, 18);

        launcher.tanks[0].renderTank(g, leftPos + 107, topPos + 78 - 52, 16, 52);
        launcher.tanks[1].renderTank(g, leftPos + 125, topPos + 78 - 52, 16, 52);
    }
}
