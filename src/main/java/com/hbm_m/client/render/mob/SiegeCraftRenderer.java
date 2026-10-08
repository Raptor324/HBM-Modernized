package com.hbm_m.client.render.mob;

import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.render.RenderHelperA;
import com.hbm_m.entity.mob.siege.EntitySiegeCraft;
import com.hbm_m.entity.mob.siege.SiegeTier;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderSiegeCraft}: kreisende Scheibe ({@code siege_ufo.obj}, Textur je Stufe), leuchtende Spulen in
 * Lebensfarbe (rot -> gruen), bei aktivem Strahl ein Glanz ueber dem Rumpf, zufaellige Blitzboegen an den acht
 * Spulen und die drei Strahlen zum fixierten Zielpunkt.
 */
public class SiegeCraftRenderer extends EntityRenderer<EntitySiegeCraft> {

    public static final SimpleObjModel SIEGE_UFO = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/mobs/siege_ufo.obj"));

    public SiegeCraftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull EntitySiegeCraft ufo, float yaw, float f1, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {

        ps.pushPose();
        ps.translate(0, 0.5, 0);
        ps.pushPose();

        double rot = (ufo.tickCount + f1) * 5 % 360D;
        ps.mulPose(Axis.YP.rotationDegrees((float) rot));

        if (!ufo.isAlive()) {
            float tilt = ufo.deathTime + f1;
            Vector3f axis = new Vector3f(1, 0, 1).normalize();
            ps.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(tilt * 5), axis.x, axis.y, axis.z));
        }

        SIEGE_UFO.renderPartEntity("UFO", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(ufo))), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);

        // Spulen ohne Textur und Licht, Farbe nach Restleben
        float health = ufo.getHealth() / ufo.getMaxHealth();
        SIEGE_UFO.renderPartEntity("Coils", ps, buffers.getBuffer(RenderType.entityCutoutNoCull(GunGL.WHITE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1F - health, health, 0F, 1F);

        if (ufo.getBeam()) {
            // RenderMiscEffects.renderClassicGlint(world, f1, siege_ufo, "UFO", 0.5F, 1.0F, 1.0F, 5, 1F)
            float offset = (Minecraft.getInstance().player != null ? Minecraft.getInstance().player.tickCount : 0) + f1;
            float glintColor = 0.76F;
            GunGL.begin(ps, buffers, light);
            for (int k = 0; k < 2; ++k) {
                float movement = offset * (0.001F + (float) k * 0.003F) * 5F;
                RenderHelperA.renderGlintPart(SIEGE_UFO, "UFO", RenderHelperA.glintBF, 0.5F * glintColor, 1.0F * glintColor, 1.0F * glintColor, 1F, 30.0F - (float) k * 60.0F, movement);
            }
            GunGL.end();
        }

        Random rand = new Random(ufo.tickCount / 4);

        ps.pushPose();
        for (int i = 0; i < 8; i++) {
            ps.mulPose(Axis.YP.rotationDegrees(45F));
            if (rand.nextInt(5) == 0 || ufo.getBeam()) {
                ps.pushPose();
                ps.translate(4, 0, 0);
                BeamPronter.prontBeam(ps, buffers, new Vec3(-1.125, 0, 2.875), EnumWaveType.RANDOM, EnumBeamType.LINE, 0x80d0ff, 0xffffff, (int) (System.currentTimeMillis() % 1000) / 50, 15, 0.125F, 1, 0);
                ps.popPose();
            }
        }
        ps.popPose();
        ps.popPose();

        if (ufo.getBeam()) {
            ps.pushPose();
            Vec3 delta = ufo.getLockon().add(-ufo.getX(), -ufo.getY(), -ufo.getZ());
            double length = delta.length();
            double scale = 0.1D;
            BeamPronter.prontBeam(ps, buffers, delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x101020, 0x101020, ufo.tickCount / 6, (int) (length / 2 + 1), (float) scale * 1F, 4, 0.25F);
            BeamPronter.prontBeam(ps, buffers, delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, ufo.tickCount / 2, (int) (length / 2 + 1), (float) scale * 7F, 2, 0.0625F);
            BeamPronter.prontBeam(ps, buffers, delta, EnumWaveType.RANDOM, EnumBeamType.SOLID, 0x202060, 0x202060, ufo.tickCount / 4, (int) (length / 2 + 1), (float) scale * 7F, 2, 0.0625F);
            ps.popPose();
        }

        ps.popPose();
        super.render(ufo, yaw, f1, ps, buffers, light);
    }

    @Override
    public boolean shouldRender(@NotNull EntitySiegeCraft entity, @NotNull net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntitySiegeCraft entity) {
        SiegeTier tier = entity.getTier();
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/siege_craft_" + tier.name + ".png");
    }
}
