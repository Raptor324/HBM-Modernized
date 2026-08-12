package com.hbm_m.client.render.implementations;

import com.hbm_m.client.missile.track.MissileTrackClient;
import com.hbm_m.client.missile.track.MissileTrackWorldRender;
import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderHelper;
import com.hbm_m.client.render.missile.MissileTextures;
import com.hbm_m.entity.missile.MissileBaseEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * OBJ renderer for all {@link MissileBaseEntity} subclasses (tier0/1, ABM, test).
 * When network track is active past launch / spawn-chunk, meshes are drawn from
 * {@link com.hbm_m.client.missile.track.MissileTrackWorldRender}; otherwise vanilla entity lerp here.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MissileEntityRenderer<T extends MissileBaseEntity> extends EntityRenderer<T> {

    public MissileEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double camX, double camY, double camZ) {
        if (MissileTrackClient.shouldUseTrackWorldRender(entity.getId())) {
            return false;
        }
        return !entity.isRemoved();
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        
                        float yaw = MissileRenderHelper.lerpRotation(entity.yRotO, entity.getYRot(), partialTicks);
        float pitch = MissileRenderHelper.lerpRotation(entity.xRotO, entity.getXRot(), partialTicks);

        double worldX = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        double worldY = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        double worldZ = Mth.lerp(partialTicks, entity.zOld, entity.getZ());

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        MissileTrackWorldRender.CameraRelativePose virtual =
                MissileTrackWorldRender.virtualizeWorld(worldX, worldY, worldZ, camera);

        BlockPos lightPos = BlockPos.containing(virtual.trueX(), virtual.trueY(), virtual.trueZ());
        int meshLight = LightTexture.pack(
                this.getBlockLightLevel(entity, lightPos),
                this.getSkyLightLevel(entity, lightPos));

        Direction launchFacing = entity.getLaunchFacing();
        poseStack.pushPose();
        poseStack.translate(
                virtual.relX() - (worldX - camera.x),
                virtual.relY() - (worldY - camera.y),
                virtual.relZ() - (worldZ - camera.z));

        if (virtual.screenScale() < 1.0F) {
            float s = virtual.screenScale();
            poseStack.scale(s, s, s);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YN.rotationDegrees(yaw - 90.0F));
        MissileRenderHelper.applyLaunchFacingRotation(poseStack, launchFacing);

        MissileRenderData data = MissileRenderHelper.resolveFlightData(entity);

        LightSampleCache.BASE_POSE.set(poseStack.last().pose());
        LightSampleCache.BASE_POSE_SET.set(true);
        float prevFogStart = RenderSystem.getShaderFogStart();
        float prevFogEnd = RenderSystem.getShaderFogEnd();
        double dist = camera.distanceTo(lightPos.getCenter());
        float fogEnd = Math.max(prevFogEnd > 0.0F ? prevFogEnd : 64.0F, (float) dist + 512.0F);
        RenderSystem.setShaderFogEnd(fogEnd);
        RenderSystem.setShaderFogStart(Math.min(prevFogStart, fogEnd * 0.85F));
        SingleMeshVboRenderer.setEntityMissileDepthBias(true);
        try {
            if (data != null) {
                data.render(poseStack, meshLight, lightPos, buffer);
            }
        } finally {
            SingleMeshVboRenderer.setEntityMissileDepthBias(false);
            RenderSystem.setShaderFogStart(prevFogStart);
            RenderSystem.setShaderFogEnd(prevFogEnd);
            LightSampleCache.BASE_POSE_SET.set(false);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return MissileTextures.forEntity(entity);
    }
}