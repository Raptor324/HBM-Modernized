package com.hbm_m.client.render.projectile;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.projectile.EntityBombletZeta;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code RenderBombletTheta}: das Bomblet-OBJ nach Flugrichtung gedreht (Gier - 90, dann Nicken um Z);
 * der Zeta-Bomblet wird halb so gross und mit eigener Textur gezeichnet.
 */
public class BombletThetaRenderer extends EntityRenderer<EntityBombletZeta> {

    private static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/projectiles/bomblet_theta.obj"));
    private static final ResourceLocation ZETA_TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity_obj/bomblet_zeta.png");

    public BombletThetaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull EntityBombletZeta e, float yaw, float interp, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(interp, e.yRotO, e.getYRot()) - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(interp, e.xRotO, e.getXRot())));

        // Original: instanceof EntityBombletZeta -> glScaled(0.5) + bombletZetaTexture
        ps.scale(0.5F, 0.5F, 0.5F);
        MODEL.renderAll(ps, buffers.getBuffer(RenderType.entityCutout(ZETA_TEXTURE)), light);

        ps.popPose();
        super.render(e, yaw, interp, ps, buffers, light);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityBombletZeta entity) {
        return ZETA_TEXTURE;
    }
}
