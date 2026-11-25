package com.hbm_m.client.render.implementations;

import com.hbm_m.api.pneumatic.IPneumaticConnector;
import com.hbm_m.blockentity.network.pneumatic.PneumoTubeBlockEntity;
import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * 1:1 port of {@code RenderPneumoTube.renderCon} (1.7.10): draws the 4/16..12/16
 * endpoint collars on {@code insertionDir} ({@code pneumatic_tube_in}),
 * {@code ejectionDir} ({@code pneumatic_tube_out}), and pneumatic machine
 * connections ({@code pneumatic_tube_connector}).
 */
public class PneumoTubeRenderer implements HbmBerBounds<PneumoTubeBlockEntity> {

    private static final ResourceLocation TEX_IN = rl("block/pneumatic_tube_in");
    private static final ResourceLocation TEX_OUT = rl("block/pneumatic_tube_out");
    private static final ResourceLocation TEX_CONNECTOR = rl("block/pneumatic_tube_connector");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
    }

    public PneumoTubeRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(PneumoTubeBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light, int overlay) {
        Direction insertion = be.getInsertionDir();
        Direction ejection = be.getEjectionDir();
        var level = be.getLevel();
        var pos = be.getBlockPos();

        for (Direction dir : Direction.values()) {
            ResourceLocation tex = null;
            if (dir == insertion) {
                tex = TEX_IN;
            } else if (dir == ejection) {
                tex = TEX_OUT;
            } else if (level != null && level.getBlockEntity(pos.relative(dir)) instanceof IPneumaticConnector conn
                    && conn.canConnectPneumatic(dir.getOpposite())) {
                tex = TEX_CONNECTOR;
            }
            if (tex != null) {
                drawCollar(ps, buffer, sprite(tex), dir, light, overlay);
            }
        }
    }

    private static TextureAtlasSprite sprite(ResourceLocation tex) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
    }

    private static void drawCollar(PoseStack ps, MultiBufferSource buffer, TextureAtlasSprite sprite,
                                   Direction dir, int light, int overlay) {
        VertexConsumer vc = buffer.getBuffer(RenderType.cutoutMipped());
        PoseStack.Pose pose = ps.last();

        float nLo = 0.25F;
        float nHi = 0.75F;
        float cLo = 0.25F;
        float cHi = 0.75F;

        float x0 = nLo, x1 = nHi, y0 = nLo, y1 = nHi, z0 = nLo, z1 = nHi;
        switch (dir) {
            case EAST -> { x0 = cHi; x1 = 1F; }
            case WEST -> { x0 = 0F; x1 = cLo; }
            case UP -> { y0 = cHi; y1 = 1F; }
            case DOWN -> { y0 = 0F; y1 = cLo; }
            case SOUTH -> { z0 = cHi; z1 = 1F; }
            case NORTH -> { z0 = 0F; z1 = cLo; }
        }

        for (Direction face : Direction.values()) {
            float[][] v = switch (face) {
                case NORTH -> new float[][] {{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}};
                case SOUTH -> new float[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
                case EAST  -> new float[][] {{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}};
                case WEST  -> new float[][] {{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}};
                case UP    -> new float[][] {{x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}};
                case DOWN  -> new float[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}};
            };
            float nx = face.getStepX(), ny = face.getStepY(), nz = face.getStepZ();
            float pxU0, pxU1, pxV0, pxV1;
            switch (face) {
                case NORTH, SOUTH -> { pxU0 = x0 * 16F; pxU1 = x1 * 16F; pxV0 = (1F - y1) * 16F; pxV1 = (1F - y0) * 16F; }
                case EAST, WEST   -> { pxU0 = z0 * 16F; pxU1 = z1 * 16F; pxV0 = (1F - y1) * 16F; pxV1 = (1F - y0) * 16F; }
                default           -> { pxU0 = x0 * 16F; pxU1 = x1 * 16F; pxV0 = z0 * 16F; pxV1 = z1 * 16F; }
            }
            float u0 = sprite.getU(pxU0), u1 = sprite.getU(pxU1);
            float v0 = sprite.getV(pxV0), v1 = sprite.getV(pxV1);
            vcVertex(vc, pose, v[0][0], v[0][1], v[0][2], u0, v1, overlay, light, nx, ny, nz);
            vcVertex(vc, pose, v[1][0], v[1][1], v[1][2], u1, v1, overlay, light, nx, ny, nz);
            vcVertex(vc, pose, v[2][0], v[2][1], v[2][2], u1, v0, overlay, light, nx, ny, nz);
            vcVertex(vc, pose, v[3][0], v[3][1], v[3][2], u0, v0, overlay, light, nx, ny, nz);
        }
    }

    private static void vcVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                                 float u, float v, int overlay, int light, float nx, float ny, float nz) {
        RenderHooks.vertexFull(vc, pose, x, y, z, 255, 255, 255, 255, u, v, overlay, light, nx, ny, nz);
    }
}
