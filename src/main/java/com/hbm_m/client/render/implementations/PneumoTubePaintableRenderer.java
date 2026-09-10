package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.network.pneumatic.PneumoTubePaintableBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port des Zweipass-Renderns aus {@code PneumoTubePaintableBlock.getIcon}.
 *
 * <p>Erster Durchgang: der Tarnblock, oder - solange keiner gesetzt ist - der nackte Wuerfel des
 * Rohrs. Zweiter Durchgang: die Seitenmarkierung. Die Einzugsseite und die Ausgabeseite tragen ein
 * eigenes Zeichen, alle uebrigen ein neutrales; der Entschaerfer schaltet den ganzen zweiten
 * Durchgang ab.</p>
 *
 * <p><b>Anmerkung:</b> das Original vertauscht hier Ein- und Ausgabe ({@code overlayIn} liegt auf
 * {@code ejectionDir}). Das ist so uebernommen - sonst saehen bestehende Anlagen anders aus als
 * gewohnt.</p>
 */
public class PneumoTubePaintableRenderer implements BlockEntityRenderer<PneumoTubePaintableBlockEntity> {

    private static final ResourceLocation BASE = rl("block/pneumatic_tube_paintable");
    private static final ResourceLocation OVERLAY = rl("block/pneumatic_tube_paintable_overlay");
    private static final ResourceLocation OVERLAY_IN = rl("block/pneumatic_tube_paintable_overlay_in");
    private static final ResourceLocation OVERLAY_OUT = rl("block/pneumatic_tube_paintable_overlay_out");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, path);
    }

    private static final float EPS = 0.002F;

    public PneumoTubePaintableRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(PneumoTubePaintableBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light, int overlay) {

        BlockState camo = be.getCamo();

        if (camo != null) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(camo, ps, buffer, light, overlay);
        } else {
            drawCube(ps, buffer, sprite(BASE), 1F);
        }

        if (be.areMarkingsHidden()) return;

        Direction insertion = be.getInsertionDir();
        Direction ejection = be.getEjectionDir();

        for (Direction dir : Direction.values()) {
            // Original: overlayIn auf der Ausgabeseite, overlayOut auf der Einzugsseite.
            ResourceLocation tex = dir == ejection ? OVERLAY_IN
                    : dir == insertion ? OVERLAY_OUT : OVERLAY;
            drawFace(ps, buffer, sprite(tex), dir);
        }
    }

    private static TextureAtlasSprite sprite(ResourceLocation tex) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
    }

    /** Der nackte Wuerfel, wenn kein Anstrich gesetzt ist. */
    private static void drawCube(PoseStack ps, MultiBufferSource buffer, TextureAtlasSprite sprite, float alpha) {
        for (Direction dir : Direction.values()) drawFace(ps, buffer, sprite, dir, alpha, 0F);
    }

    private static void drawFace(PoseStack ps, MultiBufferSource buffer, TextureAtlasSprite sprite, Direction dir) {
        drawFace(ps, buffer, sprite, dir, 1F, EPS);
    }

    private static void drawFace(PoseStack ps, MultiBufferSource buffer, TextureAtlasSprite sprite,
                                 Direction dir, float alpha, float bulge) {
        VertexConsumer vc = buffer.getBuffer(ClientRenderHandler.CustomRenderTypes.PYLON_OVERLAY);
        PoseStack.Pose pose = ps.last();

        float a = -bulge;
        float b = 1F + bulge;

        float[][] corners = switch (dir) {
            case NORTH -> new float[][] {{a, a, a}, {b, a, a}, {b, b, a}, {a, b, a}};
            case SOUTH -> new float[][] {{b, a, b}, {a, a, b}, {a, b, b}, {b, b, b}};
            case EAST  -> new float[][] {{b, a, a}, {b, a, b}, {b, b, b}, {b, b, a}};
            case WEST  -> new float[][] {{a, a, b}, {a, a, a}, {a, b, a}, {a, b, b}};
            case UP    -> new float[][] {{a, b, a}, {a, b, b}, {b, b, b}, {b, b, a}};
            case DOWN  -> new float[][] {{a, a, a}, {a, a, b}, {b, a, b}, {b, a, a}};
        };

        float[][] uv = {{0F, 0F}, {0F, 1F}, {1F, 1F}, {1F, 0F}};

        for (int i = 0; i < 4; i++) {
            vert(vc, pose, sprite, corners[i][0], corners[i][1], corners[i][2], uv[i][0], uv[i][1], alpha);
        }
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose pose, TextureAtlasSprite sprite,
                             float x, float y, float z, float u, float v, float alpha) {
        float uu = sprite.getU(u);
        float vv = sprite.getV(v);

        //? if < 1.21.1 {
        vc.vertex(pose.pose(), x, y, z).color(1F, 1F, 1F, alpha).uv(uu, vv).endVertex();
        //?} else {
        /*vc.addVertex(pose, x, y, z).setColor(1F, 1F, 1F, alpha).setUv(uu, vv);
        *///?}
    }
}
