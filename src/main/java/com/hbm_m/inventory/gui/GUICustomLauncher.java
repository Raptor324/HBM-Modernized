package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.CustomLauncherBlockEntity;
import com.hbm_m.blockentity.machines.LaunchTableBlockEntity;
import com.hbm_m.client.render.util.MissilePronter;
import com.hbm_m.inventory.menu.CustomLauncherMenu;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartSize;
import com.hbm_m.item.missile.MissileStruct;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIMachineCompactLauncher} / {@code GUIMachineLaunchTable}: Tanks, Feststoff, Strom, Statuslampen fuer
 * Rakete, Zielgeber und Treibstoffe, beim Starttisch die drei Rampengroessen, dazu die eingelegte Rakete liegend.
 */
public class GUICustomLauncher extends GuiInfoScreen<CustomLauncherMenu> {

    private static final ResourceLocation TEX_TABLE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/gui_launch_table.png");
    private static final ResourceLocation TEX_SMALL = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/gui_launch_table_small.png");

    public GUICustomLauncher(CustomLauncherMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    private boolean isTable() {
        return menu.blockEntity instanceof LaunchTableBlockEntity;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        CustomLauncherBlockEntity launcher = menu.blockEntity;
        if (launcher != null) {
            launcher.tanks[0].renderTankInfo(g, font, mouseX, mouseY, leftPos + 116, topPos + 36, 16, 34);
            launcher.tanks[1].renderTankInfo(g, font, mouseX, mouseY, leftPos + 134, topPos + 36, 16, 34);
            drawCustomInfoStat(g, mouseX, mouseY, 152, 88 - 52, 16, 52, mouseX, mouseY, Component.literal("Solid Fuel: " + launcher.solid + "l"));
            drawElectricityInfo(g, mouseX, mouseY, 134, 113, 34, 6, launcher.getEnergyStored(), CustomLauncherBlockEntity.maxPower);

            if (isTable()) {
                drawCustomInfoStat(g, mouseX, mouseY, 7, 98, 18, 18, mouseX, mouseY, Component.literal("Size 10 & 10/15"));
                drawCustomInfoStat(g, mouseX, mouseY, 25, 98, 18, 18, mouseX, mouseY, Component.literal("Size 15 & 15/20"));
                drawCustomInfoStat(g, mouseX, mouseY, 43, 98, 18, 18, mouseX, mouseY, Component.literal("Size 20"));
                drawCustomInfoStat(g, mouseX, mouseY, -16, 36, 16, 16, leftPos - 8, topPos + 36 + 16,
                        Component.literal("Accepts custom missiles"), Component.literal("of all sizes, as long as the"), Component.literal("correct size setting is selected."));
            } else {
                drawCustomInfoStat(g, mouseX, mouseY, -16, 36, 16, 16, leftPos - 8, topPos + 36 + 16,
                        Component.literal("Only accepts custom missiles"), Component.literal("of size 10 and 10/15."));
            }
            drawCustomInfoStat(g, mouseX, mouseY, -16, 36 + 16, 16, 16, leftPos - 8, topPos + 36 + 16,
                    Component.literal("Detonator can only trigger center block."));
        }

        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isTable() && menu.blockEntity != null) {
            PartSize[] sizes = { PartSize.SIZE_10, PartSize.SIZE_15, PartSize.SIZE_20 };
            for (int i = 0; i < 3; i++) {
                int bx = leftPos + 7 + i * 18;
                if (bx <= mouseX && bx + 18 > mouseX && topPos + 98 < mouseY && topPos + 98 + 18 >= mouseY) {
                    playClickSound();
                    CompoundTag data = new CompoundTag();
                    data.putInt("padSize", sizes[i].ordinal());
                    com.hbm_m.network.NBTControlPacket.sendToServer(menu.blockEntity.getBlockPos(), data);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = isTable() ? TEX_TABLE : TEX_SMALL;
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        CustomLauncherBlockEntity launcher = menu.blockEntity;
        if (launcher == null) return;

        int i = (int) launcher.getPowerScaled(34);
        g.blit(texture, leftPos + 134, topPos + 113, 176, 96, i, 6);

        int j = launcher.getSolidScaled(52);
        g.blit(texture, leftPos + 152, topPos + 88 - j, 176, 96 - j, 16, j);

        if (launcher.isMissileValid()) g.blit(texture, leftPos + 25, topPos + 35, 176, 26, 18, 18);
        if (launcher.hasDesignator()) g.blit(texture, leftPos + 25, topPos + 71, 176, 26, 18, 18);

        led(g, texture, 121, launcher.liquidState());
        led(g, texture, 139, launcher.oxidizerState());
        led(g, texture, 157, launcher.solidState());

        if (launcher instanceof LaunchTableBlockEntity table) {
            switch (table.padSize) {
                case SIZE_10 -> g.blit(texture, leftPos + 7, topPos + 98, 176, 8, 18, 18);
                case SIZE_15 -> g.blit(texture, leftPos + 25, topPos + 98, 194, 8, 18, 18);
                case SIZE_20 -> g.blit(texture, leftPos + 43, topPos + 98, 212, 8, 18, 18);
                default -> { }
            }
        }

        drawInfoPanel(g, -16, 36, PanelType.LARGE_BLUE_INFO);
        drawInfoPanel(g, -16, 36 + 16, PanelType.LARGE_GRAY_STAR);

        launcher.tanks[0].renderTank(g, leftPos + 116, topPos + 70 - 34, 16, 34); // Original: Unterkante 70, Port-renderTank erwartet die Oberkante
        launcher.tanks[1].renderTank(g, leftPos + 134, topPos + 70 - 34, 16, 34);

        // DRAW MISSILE: liegend ueber dem Inventar, auf 5 Slots skaliert
        if (launcher.isMissileValid()) {
            MissileStruct missile = launcher.getLoad();
            double height = MissilePronter.guiHeight(missile);

            var ps = g.pose();
            ps.pushPose();
            ps.translate(leftPos + 88, topPos + 115, 100);

            double size = 5 * 18;
            float scale = (float) (size / Math.max(height, 6));

            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.translate(height / 2D * scale, 0, 0);
            ps.scale(scale, scale, scale);
            ps.scale(-1, -1, -1);

            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            RenderSystem.disableCull();
            MissilePronter.prontMissile(missile, ps, g.bufferSource(), LightTexture.FULL_BRIGHT);
            g.flush();
            RenderSystem.enableCull();
            com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
            ps.popPose();
        }
    }

    private void led(GuiGraphics g, ResourceLocation tex, int x, int state) {
        if (state == 1) g.blit(tex, leftPos + x, topPos + 23, 176, 0, 6, 8);
        if (state == 0) g.blit(tex, leftPos + x, topPos + 23, 182, 0, 6, 8);
    }
}
