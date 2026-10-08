package com.hbm_m.client.render.implementations;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.blockentity.machines.SlagBlockEntity;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Ersatz fuer das Blockrendering von {@code BlockDynamicSlag}: ein Quader mit der Hoehe {@code amount / maxAmount}.
 * Hat das Material verschiedene Hell-/Dunkelfarben, wird die Schlackentextur wie im Original per
 * {@code RGBMutatorInterpolatedComponentRemap(0xFFFFFF, 0x505050, light, dark)} umgefaerbt, sonst wird die
 * Grundtextur mit der Schmelzfarbe eingefaerbt ({@code colorMultiplier}).
 */
public class SlagRenderer implements com.hbm_m.client.render.HbmBerBounds<SlagBlockEntity> {

    public static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/slag_dynamic.png");
    private static final Map<NTMMaterial, ResourceLocation> ICONS = new HashMap<>();
    private static NativeImage baseImage;
    private static boolean baseFailed;

    public SlagRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(SlagBlockEntity tile, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (tile.mat == null || tile.amount <= 0) return;

        float h = Math.min(1F, (float) tile.amount / (float) SlagBlockEntity.maxAmount);
        NTMMaterial mat = tile.mat;

        ResourceLocation tex = BASE;
        float r = 1F, g = 1F, b = 1F;
        if (mat.solidColorLight != mat.solidColorDark) {
            ResourceLocation remapped = remapped(mat);
            if (remapped != null) tex = remapped;
        } else {
            int hex = mat.moltenColor;
            r = (hex >> 16 & 255) / 255F; g = (hex >> 8 & 255) / 255F; b = (hex & 255) / 255F;
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(tex));
        org.joml.Matrix4f m = ps.last().pose();
        org.joml.Matrix3f n = ps.last().normal();
        BlockPos pos = tile.getBlockPos();
        var level = tile.getLevel();

        // Oben
        if (h < 1F || level == null || !level.getBlockState(pos.above()).isSolidRender(level, pos.above())) {
            quad(vc, m, n, 0, h, 0, 0, h, 1, 1, h, 1, 1, h, 0, 0, 0, 0, 1, 1, 1, 1, 0, r, g, b, light, 0, 1, 0);
        }
        // Unten
        if (level == null || !level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            quad(vc, m, n, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, r, g, b, light, 0, -1, 0);
        }
        float v0 = 1F - h;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos np = pos.relative(dir);
            if (level != null && level.getBlockState(np).isSolidRender(level, np)) continue;
            switch (dir) {
                case NORTH -> quad(vc, m, n, 1, 0, 0, 0, 0, 0, 0, h, 0, 1, h, 0, 0, 1, 1, 1, 1, v0, 0, v0, r, g, b, light, 0, 0, -1);
                case SOUTH -> quad(vc, m, n, 0, 0, 1, 1, 0, 1, 1, h, 1, 0, h, 1, 0, 1, 1, 1, 1, v0, 0, v0, r, g, b, light, 0, 0, 1);
                case WEST -> quad(vc, m, n, 0, 0, 0, 0, 0, 1, 0, h, 1, 0, h, 0, 0, 1, 1, 1, 1, v0, 0, v0, r, g, b, light, -1, 0, 0);
                case EAST -> quad(vc, m, n, 1, 0, 1, 1, 0, 0, 1, h, 0, 1, h, 1, 0, 1, 1, 1, 1, v0, 0, v0, r, g, b, light, 1, 0, 0);
                default -> { }
            }
        }
    }

    private static void quad(VertexConsumer vc, org.joml.Matrix4f m, org.joml.Matrix3f n,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                             float r, float g, float b, int light, float nx, float ny, float nz) {
        vc.vertex(m, x0, y0, z0).color(r, g, b, 1F).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, 1F).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, 1F).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, 1F).uv(u3, v3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    /** Erzeugt einmalig je Material die umgefaerbte Textur (Original: {@code TextureAtlasSpriteMutatable}). */
    private static ResourceLocation remapped(NTMMaterial mat) {
        ResourceLocation cached = ICONS.get(mat);
        if (cached != null) return cached;
        if (baseFailed) return null;
        try {
            if (baseImage == null) {
                Resource res = Minecraft.getInstance().getResourceManager().getResourceOrThrow(BASE);
                try (var in = res.open()) { baseImage = NativeImage.read(in); }
            }
            NativeImage img = new NativeImage(baseImage.getWidth(), baseImage.getHeight(), true);
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    int abgr = baseImage.getPixelRGBA(x, y);
                    int a = abgr >>> 24, bb = abgr >> 16 & 255, gg = abgr >> 8 & 255, rr = abgr & 255;
                    int nr = shift(mat.solidColorLight >> 16 & 255, mat.solidColorDark >> 16 & 255, 0xFF, 0x50, rr);
                    int ng = shift(mat.solidColorLight >> 8 & 255, mat.solidColorDark >> 8 & 255, 0xFF, 0x50, gg);
                    int nb = shift(mat.solidColorLight & 255, mat.solidColorDark & 255, 0xFF, 0x50, bb);
                    img.setPixelRGBA(x, y, a << 24 | nb << 16 | ng << 8 | nr);
                }
            }
            ResourceLocation loc = Minecraft.getInstance().getTextureManager()
                    .register(RefStrings.MODID + "_slag_" + mat.id, new DynamicTexture(img));
            ICONS.put(mat, loc);
            return loc;
        } catch (Exception e) {
            baseFailed = true;
            return null;
        }
    }

    /** 1:1 {@code shiftComponent}: lineare Abbildung der Quellspanne auf die Zielspanne, Ergebnis &amp; 0xFF. */
    private static int shift(int lighter, int darker, int boundLighter, int boundDarker, int component) {
        double pos = (component - (double) boundLighter) / ((double) boundDarker - boundLighter);
        return ((int) (lighter + pos * (darker - lighter))) & 0xFF;
    }
}
