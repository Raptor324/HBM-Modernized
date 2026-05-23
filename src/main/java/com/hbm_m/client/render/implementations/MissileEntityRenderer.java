package com.hbm_m.client.render.implementations;

import com.hbm_m.client.render.missile.MissileFormFactorModels;
import com.hbm_m.client.render.missile.MissileRenderHelper;
import com.hbm_m.client.render.missile.MissileTextures;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
/**
 * OBJ renderer for all {@link MissileBaseEntity} subclasses (tier0/1, ABM, test).
 */
public class MissileEntityRenderer<T extends MissileBaseEntity> extends EntityRenderer<T> {

    public MissileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        MissileFormFactorModels form = MissileFormFactorModels.fromEntity(entity.getClass());
        float yaw = MissileRenderHelper.lerpRotation(entity.yRotO, entity.getYRot(), partialTicks);
        float pitch = MissileRenderHelper.lerpRotation(entity.xRotO, entity.getXRot(), partialTicks);
        BlockPos lightPos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());

        MissileRenderHelper.renderInFlight(entity, form, poseStack, packedLight, yaw, pitch, lightPos);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return MissileTextures.forEntity(entity);
    }
}
