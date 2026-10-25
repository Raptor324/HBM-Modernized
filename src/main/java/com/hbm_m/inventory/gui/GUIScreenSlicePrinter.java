package com.hbm_m.inventory.gui;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachinePWRControllerBlock;
import com.hbm_m.block.machines.PWRBlock;
import com.hbm_m.blockentity.machines.PWRBlockEntity;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code GUIScreenSlicePrinter}: zeichnet Lage fuer Lage des Aufbaus isometrisch auf Magenta, speichert jedes Bild
 * (Magenta durchsichtig) als {@code .minecraft/printer/<Datum>/slice_<n>.png} und schliesst sich nach der obersten Lage.
 * Traeger ({@code pwr_block}) werden als das gemerkte Bauteil gezeichnet.
 */
public class GUIScreenSlicePrinter extends Screen {

    private static final int BACKGROUND = 0xFFFF00FF;

    private final int x1, y1, z1;
    private final int x2, y2, z2;
    private final int sizeX, sizeY, sizeZ;
    private final Direction dir;

    @Nullable private Set<Block> whitelist;

    private int yIndex;

    private final String dirname;
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    public GUIScreenSlicePrinter(int x1, int y1, int z1, int x2, int y2, int z2, Direction dir) {
        super(Component.empty());
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);

        this.dir = dir;

        this.sizeX = this.x2 - this.x1 + 1;
        this.sizeY = this.y2 - this.y1 + 1;
        this.sizeZ = this.z2 - this.z1 + 1;

        dirname = dateFormat.format(new Date());
    }

    public GUIScreenSlicePrinter(int x1, int y1, int z1, int x2, int y2, int z2, Direction dir, Set<Block> whitelist) {
        this(x1, y1, z1, x2, y2, z2, dir);
        this.whitelist = whitelist;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        g.fill(0, 0, width, height, BACKGROUND);
        g.flush();

        // Nach der obersten Lage schliessen
        if (yIndex >= sizeY) {
            mc.player.sendSystemMessage(Component.literal("Slices saved to: .minecraft/printer/" + dirname));
            this.onClose();
            return;
        }

        RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();

        PoseStack pose = g.pose();
        pose.pushPose();
        setupRotation(pose);

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                BlockPos pos = new BlockPos(x1 + x, y1 + yIndex, z1 + z);
                Block block = mc.level.getBlockState(pos).getBlock();
                if (whitelist != null && !whitelist.contains(block)) continue;

                BlockState state = block.defaultBlockState();

                // Traeger fuer den Schnitt wieder als ihr Bauteil zeichnen
                if (block instanceof PWRBlock && mc.level.getBlockEntity(pos) instanceof PWRBlockEntity pwr && pwr.block != null) {
                    state = pwr.block.defaultBlockState();
                }
                // Original: RenderBlocks liest die Metadaten an (dx, 0, dz) - der Controller zeigt damit nach Sueden
                if (state.hasProperty(MachinePWRControllerBlock.FACING)) state = state.setValue(MachinePWRControllerBlock.FACING, Direction.SOUTH);

                int dx = x;
                int dz = z;

                // tauschen statt drehen, damit der Controller richtig herum steht
                if (dir == Direction.WEST) {
                    dx = sizeZ - 1 - z;
                    dz = x;
                } else if (dir == Direction.SOUTH) {
                    dx = sizeX - 1 - x;
                    dz = sizeZ - 1 - z;
                } else if (dir == Direction.EAST) {
                    dx = z;
                    dz = sizeX - 1 - x;
                }

                pose.pushPose();
                pose.translate(dx, 0, dz);
                mc.getBlockRenderer().renderSingleBlock(state, pose, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }

        buffer.endBatch();
        pose.popPose();
        Lighting.setupForFlatItems();

        saveSlice(mc);
        yIndex++;
    }

    private void setupRotation(PoseStack pose) {
        float scale = -24;

        pose.translate(width / 2D, height / 2D - 36, 400);
        pose.scale(scale, scale, scale);
        pose.scale(1, 1, 0.5F); // unglaubliche Plaettkraft

        pose.mulPose(Axis.XP.rotationDegrees(-30));
        pose.mulPose(Axis.YP.rotationDegrees(225));

        if (dir == Direction.WEST || dir == Direction.EAST) {
            pose.translate(sizeX / -2D, -sizeY / 2D, sizeZ / -2D);
        } else {
            pose.translate(sizeZ / -2D, -sizeY / 2D, sizeX / -2D);
        }
    }

    /** Original: {@code GUIScreenWikiRender.saveScreenshot} ueber das ganze Fenster, Hintergrundfarbe wird durchsichtig. */
    private void saveSlice(Minecraft mc) {
        try {
            File printerDir = new File(mc.gameDirectory, "printer");
            printerDir.mkdir();
            File dir = new File(printerDir, dirname);
            dir.mkdir();

            NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            for (int iy = 0; iy < image.getHeight(); iy++) {
                for (int ix = 0; ix < image.getWidth(); ix++) {
                    int abgr = image.getPixelRGBA(ix, iy);
                    int r = abgr & 0xFF, gr = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
                    if (r == 0xFF && gr == 0x00 && b == 0xFF) image.setPixelRGBA(ix, iy, 0);
                }
            }
            image.writeToFile(new File(dir, "slice_" + yIndex + ".png"));
            image.close();
        } catch (Exception ignored) {
            // wie im Original: fehlgeschlagene Bilder werden uebersprungen
        }
    }
}
