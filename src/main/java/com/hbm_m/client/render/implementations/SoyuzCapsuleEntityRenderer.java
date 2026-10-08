package com.hbm_m.client.render.implementations;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.missile.SoyuzCapsuleEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderSoyuzCapsule}: Kapsel unter dem Fallschirm, pendelt um einen Punkt 7 Bloecke ueber ihr. */
public class SoyuzCapsuleEntityRenderer extends EntityRenderer<SoyuzCapsuleEntity> {

    public static final SimpleObjModel SOYUZ_LANDER = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/soyuz_lander.obj"));
    public static final ResourceLocation LANDER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/soyuz_capsule/soyuz_lander.png");
    public static final ResourceLocation LANDER_RUST_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/soyuz_capsule/soyuz_lander_rust.png");
    public static final ResourceLocation CHUTE_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/soyuz_capsule/soyuz_chute.png");

    public SoyuzCapsuleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SoyuzCapsuleEntity entity, float entityYaw, float partialTick, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();

        double time = entity.level().getGameTime();
        double sine = Math.sin(time * 0.05) * 5;
        double sin3 = Math.sin(time * 0.05 + Math.PI * 0.5) * 5;
        int height = 7;
        ps.translate(0.0F, height, 0.0F);
        ps.mulPose(Axis.ZP.rotationDegrees((float) sine));
        ps.mulPose(Axis.XP.rotationDegrees((float) sin3));
        ps.translate(0.0F, -height, 0.0F);

        SOYUZ_LANDER.renderPart("Capsule", ps, buffers.getBuffer(RenderType.entityCutout(LANDER_TEX)), light);
        SOYUZ_LANDER.renderPart("Chute", ps, buffers.getBuffer(RenderType.entityCutout(CHUTE_TEX)), light);

        ps.popPose();
        super.render(entity, entityYaw, partialTick, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SoyuzCapsuleEntity entity) {
        return LANDER_TEX;
    }
}
