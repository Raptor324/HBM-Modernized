package com.hbm_m.client.render.projectile;

import java.util.Random;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.entity.projectile.EntityBulletBaseNT;
import com.hbm_m.handler.BulletConfiguration;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 1:1 {@code RenderBullet} (Altsystem) fuer {@link EntityBulletBaseNT}: Stil aus den synchronisierten Daten,
 * GL-Schritte ueber die {@link GunGL}-Fassade. {@code RenderSparks} (GL_LINE_STRIP, Breite 5/2) als einfache Linien.
 */
public class RenderBulletNT extends EntityRenderer<EntityBulletBaseNT> {

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/bullet.png");
    private static final ResourceLocation METEOR_MOLTEN = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/block_meteor_molten.png");
    private static final ResourceLocation OBSIDIAN = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/obsidian.png");

    public RenderBulletNT(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityBulletBaseNT bullet, float yaw, float f1, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        GunGL.begin(ps, buf, light, OverlayTexture.NO_OVERLAY);

        GunGL.rotate(Mth.lerp(f1, bullet.yRotO, bullet.getYRot()) - 90.0F, 0.0F, 1.0F, 0.0F);
        GunGL.rotate(Mth.lerp(f1, bullet.xRotO, bullet.getXRot()) + 180, 0.0F, 0.0F, 1.0F);
        GunGL.scale(1.5F, 1.5F, 1.5F);

        int style = bullet.getStyle();
        int trail = bullet.getTrail();

        if (style != BulletConfiguration.STYLE_BLADE)
            GunGL.rotate(new Random(bullet.getId()).nextInt(90) - 45, 1.0F, 0.0F, 0.0F);

        GunGL.enableCull();

        switch (style) {
            case BulletConfiguration.STYLE_NORMAL -> renderBullet(trail);
            case BulletConfiguration.STYLE_BOLT -> renderDart(trail, bullet.getId());
            case BulletConfiguration.STYLE_FLECHETTE -> renderFlechette();
            case BulletConfiguration.STYLE_ROCKET -> renderRocket(trail);
            case BulletConfiguration.STYLE_GRENADE -> renderGrenade(trail);
            case BulletConfiguration.STYLE_ORB -> renderOrb(trail, ps, buf);
            case BulletConfiguration.STYLE_METEOR -> renderMeteor(trail);
            case BulletConfiguration.STYLE_BLADE -> renderBlade(bullet, ps, buf, light);
            default -> renderBullet(trail);
        }

        GunGL.end();
        ps.popPose();
        super.render(bullet, yaw, f1, ps, buf, light);
    }

    private void renderBullet(int type) {
        GunGL.scale(0.5, 0.5, 0.5);
        GunGL.rotate(90, 0, 0, 1);
        GunGL.rotate(90, 0, 1, 0);
        GunGL.bindTexture(WeaponResources.bullet_rifle_tex);
        GunGL.renderPart(WeaponResources.projectiles, "BulletRifle");
    }

    private void renderRocket(int type) {
        GunGL.scale(0.5, 0.5, 0.5);
        GunGL.rotate(90, 0, 0, 1);
        GunGL.rotate(90, 0, 1, 0);
        GunGL.bindTexture(WeaponResources.rocket_tex);
        GunGL.renderPart(WeaponResources.projectiles, "Rocket");
    }

    private void renderGrenade(int type) {
        GunGL.scale(0.25F, 0.25F, 0.25F);
        GunGL.rotate(90, 0, 0, 1);
        GunGL.rotate(90, 0, 1, 0);
        GunGL.bindTexture(WeaponResources.grenade_tex);
        GunGL.renderPart(WeaponResources.projectiles, "Grenade");
    }

    private void renderOrb(int type, PoseStack ps, MultiBufferSource buf) {
        GunGL.enableCull();
        GunGL.disableLighting();
        GunGL.enableBlend();
        GunGL.blendFunc(GunGL.GL_SRC_ALPHA, GunGL.GL_ONE);
        GunGL.depthMask(false);

        switch (type) {
            case 0 -> {
                GunGL.bindTexture(WeaponResources.tom_flame_tex);
                GunGL.renderAll(WeaponResources.sphere_uv);
                GunGL.scale(0.3F, 0.3F, 0.3F);
                GunGL.renderAll(WeaponResources.sphere_uv);
                GunGL.scale(1F / 0.3F, 1F / 0.3F, 1F / 0.3F);
                for (int i = 0; i < 5; i++)
                    renderSpark(ps, buf, (int) (System.currentTimeMillis() / 100 + 100 * i), 0, 0, 0, 0.5F, 2, 2, 0x8080FF, 0xFFFFFF);
            }
            case 1 -> {
                GunGL.scale(0.5F, 0.5F, 0.5F);
                GunGL.disableTexture2D();
                GunGL.color(0.5F, 0.0F, 0.0F, 0.5F);
                GunGL.renderAll(WeaponResources.sphere_uv);
                GunGL.scale(0.75F, 0.75F, 0.75F);
                GunGL.renderAll(WeaponResources.sphere_uv);
                GunGL.scale(1F / 0.75F, 1F / 0.75F, 1F / 0.75F);
                GunGL.enableTexture2D();
                GunGL.color(1F, 1F, 1F, 1F);
                for (int i = 0; i < 3; i++)
                    renderSpark(ps, buf, (int) (System.currentTimeMillis() / 100 + 100 * i), 0, 0, 0, 1F, 2, 3, 0xFF0000, 0xFF8080);
            }
            default -> { }
        }

        GunGL.disableBlend();
        GunGL.enableLighting();
        GunGL.depthMask(true);
    }

    private void renderMeteor(int type) {
        GunGL.enableCull();
        GunGL.disableLighting();
        switch (type) {
            case 0 -> GunGL.bindTexture(METEOR_MOLTEN);
            case 1 -> GunGL.bindTexture(OBSIDIAN);
            default -> { }
        }
        GunGL.renderAll(WeaponResources.meteor);
        GunGL.enableLighting();
    }

    private void renderFlechette() {
        GunGL.scale(0.5, 0.5, 0.5);
        GunGL.rotate(90, 0, 0, 1);
        GunGL.rotate(90, 0, 1, 0);
        GunGL.bindTexture(WeaponResources.flechette_tex);
        GunGL.renderPart(WeaponResources.projectiles, "Flechette");
    }

    private void renderDart(int style, int eID) {
        float red = 1F, green = 1F, blue = 1F;
        switch (style) {
            case BulletConfiguration.BOLT_LASER -> { red = 1F; green = 0F; blue = 0F; }
            case BulletConfiguration.BOLT_NIGHTMARE -> { red = 1F; green = 1F; blue = 0F; }
            case BulletConfiguration.BOLT_LACUNAE -> { red = 0.25F; green = 0F; blue = 0.75F; }
            case BulletConfiguration.BOLT_WORM -> { red = 0F; green = 1F; blue = 0F; }
            default -> { }
        }

        GunGL.pushMatrix();
        GunGL.disableTexture2D();
        GunGL.disableCull();
        GunGL.disableLighting();
        GunGL.enableBlend();
        GunGL.blendFunc(GunGL.GL_SRC_ALPHA, GunGL.GL_ONE);
        GunGL.depthMask(false);

        GunGL.scale(1F / 4F, 1F / 8F, 1F / 8F);
        GunGL.scale(-1, 1, 1);
        GunGL.scale(2, 2, 2);

        GunGL.Tess tess = GunGL.tess();
        float r = red, g = green, b = blue;

        // front
        tri(tess, r, g, b, 1, 6, 0, 0, 0, 3, -1, -1, 0, 3, 1, -1);
        tri(tess, r, g, b, 0, 3, -1, 1, 1, 6, 0, 0, 0, 3, 1, 1);
        tri(tess, r, g, b, 0, 3, -1, -1, 1, 6, 0, 0, 0, 3, -1, 1);
        tri(tess, r, g, b, 1, 6, 0, 0, 0, 3, 1, -1, 0, 3, 1, 1);
        // mid
        tri(tess, r, g, b, 1, 6, 0, 0, 1, 4, -0.5, -0.5, 1, 4, 0.5, -0.5);
        tri(tess, r, g, b, 1, 4, -0.5, 0.5, 1, 6, 0, 0, 1, 4, 0.5, 0.5);
        tri(tess, r, g, b, 1, 4, -0.5, -0.5, 1, 6, 0, 0, 1, 4, -0.5, 0.5);
        tri(tess, r, g, b, 1, 6, 0, 0, 1, 4, 0.5, -0.5, 1, 4, 0.5, 0.5);
        // tail
        quad(tess, r, g, b, 4, 0.5, -0.5, 4, 0.5, 0.5, 0, 0.5, 0.5, 0, 0.5, -0.5);
        quad(tess, r, g, b, 4, -0.5, -0.5, 4, -0.5, 0.5, 0, -0.5, 0.5, 0, -0.5, -0.5);
        quad(tess, r, g, b, 4, -0.5, 0.5, 4, 0.5, 0.5, 0, 0.5, 0.5, 0, -0.5, 0.5);
        quad(tess, r, g, b, 4, -0.5, -0.5, 4, 0.5, -0.5, 0, 0.5, -0.5, 0, -0.5, -0.5);

        GunGL.enableTexture2D();
        GunGL.disableBlend();
        GunGL.enableLighting();
        GunGL.enableCull();
        GunGL.depthMask(true);
        GunGL.popMatrix();
    }

    /** Dreieck mit je eigenem Alpha (a = 1 oder 0) pro Ecke wie im Original. */
    private static void tri(GunGL.Tess t, float r, float g, float b,
                            float a0, double x0, double y0, double z0,
                            float a1, double x1, double y1, double z1,
                            float a2, double x2, double y2, double z2) {
        t.startDrawing(GunGL.GL_TRIANGLES);
        t.setColorRGBA_F(r, g, b, a0); t.addVertex(x0, y0, z0);
        t.setColorRGBA_F(r, g, b, a1); t.addVertex(x1, y1, z1);
        t.setColorRGBA_F(r, g, b, a2); t.addVertex(x2, y2, z2);
        t.draw();
    }

    /** Schweif-Quad: die ersten zwei Ecken voll, die letzten zwei durchsichtig. */
    private static void quad(GunGL.Tess t, float r, float g, float b,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             double x2, double y2, double z2, double x3, double y3, double z3) {
        t.startDrawingQuads();
        t.setColorRGBA_F(r, g, b, 1); t.addVertex(x0, y0, z0); t.addVertex(x1, y1, z1);
        t.setColorRGBA_F(r, g, b, 0); t.addVertex(x2, y2, z2); t.addVertex(x3, y3, z3);
        t.draw();
    }

    private void renderBlade(EntityBulletBaseNT bullet, PoseStack ps, MultiBufferSource buf, int light) {
        GunGL.pushMatrix();
        GunGL.rotate(90, 0, 0, 1);
        GunGL.translate(0, 0.5, 0);
        GunGL.rotate(System.currentTimeMillis() % 360, 1, 0, 0);
        GunGL.translate(0, -0.5, 0);
        GunGL.rotate(90, 0, 1, 0);
        GunGL.scale(1, 2, 1);
        // Original: EntityItem(blade_titanium) mit renderInFrame
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.BLADE_TITANIUM.get()), ItemDisplayContext.FIXED,
                light, OverlayTexture.NO_OVERLAY, ps, buf, bullet.level(), bullet.getId());
        GunGL.popMatrix();
    }

    /** 1:1 {@code RenderSparks.renderSpark}: Zickzack-Linie aus 'min + rand(max)' Segmenten, zwei Farben. */
    private static void renderSpark(PoseStack ps, MultiBufferSource buf, int seed, double x, double y, double z, float length, int min, int max, int color1, int color2) {
        VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.BEAM_LINES);
        Matrix4f m = ps.last().pose();
        Random rand = new Random(seed);
        Vec3 vec = new Vec3(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5).normalize();

        for (int i = 0; i < min + rand.nextInt(max); i++) {
            double prevX = x, prevY = y, prevZ = z;
            Vec3 dir = vec.normalize();
            double dx = dir.x * length * rand.nextFloat();
            double dy = dir.y * length * rand.nextFloat();
            double dz = dir.z * length * rand.nextFloat();
            x = prevX + dx;
            y = prevY + dy;
            z = prevZ + dz;
            for (int c : new int[] { color1, color2 }) {
                int cr = c >> 16 & 255, cg = c >> 8 & 255, cb = c & 255;
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, (float) prevX, (float) prevY, (float) prevZ, cr, cg, cb, 255);
                com.hbm_m.platform.RenderHooks.vertexColor(vc, m, (float) x, (float) y, (float) z, cr, cg, cb, 255);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBulletBaseNT e) {
        return TEX;
    }
}
