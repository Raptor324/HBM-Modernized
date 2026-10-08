package com.hbm_m.client.render.projectile;

import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.legacy.ModelBullet;
import com.hbm_m.entity.projectile.EntityBullet;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code RenderRocket} (Zweig EntityBullet): kleiner Quader, 1,5-fach, um die Flugachse zufaellig je Entity-ID
 * gedreht; Textur emplacer (Hubschrauber), tau (kritisch) oder bullet. Tau/Hubschrauber leuchten voll.
 */
public class BulletRenderer extends EntityRenderer<EntityBullet> {

    private static final ResourceLocation EMPLACER = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/emplacer.png");
    private static final ResourceLocation TAU = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/tau.png");
    private static final ResourceLocation BULLET = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/bullet.png");
    private final ModelBullet model = new ModelBullet();

    public BulletRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull EntityBullet rocket, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, rocket.yRotO, rocket.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, rocket.xRotO, rocket.getXRot()) + 180));
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.XP.rotationDegrees(new Random(rocket.getId()).nextInt(360)));

        int l = rocket.isFullbright() ? 0xF000F0 : light;
        model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(rocket))), l, OverlayTexture.NO_OVERLAY);

        pose.popPose();
        super.render(rocket, yaw, partialTicks, pose, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityBullet bullet) {
        if (bullet.getIsChopper()) return EMPLACER;
        if (bullet.getIsCritical()) return TAU;
        return BULLET;
    }
}
