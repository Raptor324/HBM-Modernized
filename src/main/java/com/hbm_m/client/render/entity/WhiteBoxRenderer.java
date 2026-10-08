package com.hbm_m.client.render.entity;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Gegenstueck zu 1.7.10 {@code RenderEntity}: der Rueckfall-Renderer fuer Entities ohne eigenen Renderer zeichnet
 * die Trefferbox als weissen Quader ({@code renderOffsetAABB}). Genutzt fuer Entities, denen das Original keinen
 * Renderer zuweist (z.B. EntityWastePearl).
 */
public class WhiteBoxRenderer<T extends Entity> extends EntityRenderer<T> {

    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public WhiteBoxRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(T entity, float yaw, float partialTicks, PoseStack ps, MultiBufferSource buf, int light) {
        AABB bb = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
        VertexConsumer vc = buf.getBuffer(RenderType.entitySolid(WHITE));
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float x0 = (float) bb.minX, y0 = (float) bb.minY, z0 = (float) bb.minZ;
        float x1 = (float) bb.maxX, y1 = (float) bb.maxY, z1 = (float) bb.maxZ;

        face(vc, m, n, light, 0, 0, -1, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0);
        face(vc, m, n, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(vc, m, n, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(vc, m, n, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        face(vc, m, n, light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        face(vc, m, n, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);

        super.render(entity, yaw, partialTicks, ps, buf, light);
    }

    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        v(vc, m, n, light, nx, ny, nz, ax, ay, az, 0, 0);
        v(vc, m, n, light, nx, ny, nz, bx, by, bz, 1, 0);
        v(vc, m, n, light, nx, ny, nz, cx, cy, cz, 1, 1);
        v(vc, m, n, light, nx, ny, nz, dx, dy, dz, 0, 1);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, float nx, float ny, float nz,
                          float x, float y, float z, float u, float vv) {
        //? if < 1.21.1 {
        vc.vertex(m, x, y, z).color(1F, 1F, 1F, 1F).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        //?} else {
        /*com.hbm_m.platform.RenderHooks.normal(vc.addVertex(m, x, y, z).setColor(1F, 1F, 1F, 1F).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light), n, nx, ny, nz);
        *///?}
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return WHITE;
    }
}
