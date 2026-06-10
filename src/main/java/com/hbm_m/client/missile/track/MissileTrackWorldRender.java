package com.hbm_m.client.missile.track;

import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.missile.MissileRenderData;
import com.hbm_m.client.render.missile.MissileRenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Authoritative missile mesh draw for S2C tracks — independent of client entity sync and chunk gates.
 */
public final class MissileTrackWorldRender {

    private static final double FRUSTUM_SAFETY = 0.72D;
    private static final double MIN_VIRTUAL_DISTANCE_BLOCKS = 96.0D;

    private MissileTrackWorldRender() {}

    public static void render(float partialTick, PoseStack poseStack) {
        if (!MissileTrackClient.isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        try {
            for (MissileTrackClient.TrackEntry entry : MissileTrackClient.entries()) {
                if (!MissileTrackClient.shouldUseTrackWorldRender(entry.entityId)) {
                    continue;
                }
                MissileTrackClient.InterpolatedPose pose = entry.interpolate(partialTick);
                if (pose == null) {
                    continue;
                }
                renderOne(level, pose, poseStack, buffers, camera);
            }
        } finally {
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    private static void renderOne(ClientLevel level, MissileTrackClient.InterpolatedPose pose,
                                  PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vec3 camera) {
        MissileRenderData data = MissileRenderHelper.resolveFromTrack(pose.current());
        if (data == null) {
            return;
        }

        CameraRelativePose virtual = virtualizeWorld(pose.x(), pose.y(), pose.z(), camera);
        double drawX = camera.x + virtual.relX();
        double drawY = camera.y + virtual.relY();
        double drawZ = camera.z + virtual.relZ();

        BlockPos lightPos = BlockPos.containing(virtual.trueX(), virtual.trueY(), virtual.trueZ());
        int packedLight = LightTexture.pack(
                level.getBrightness(LightLayer.BLOCK, lightPos),
                level.getBrightness(LightLayer.SKY, lightPos));

        Direction launchFacing = pose.current().launchFacing();
        float yaw = pose.yaw();
        float pitch = pose.pitch();

        poseStack.pushPose();
        poseStack.translate(drawX, drawY, drawZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YN.rotationDegrees(yaw - 90.0F));
        MissileRenderHelper.applyLaunchFacingRotation(poseStack, launchFacing);

        if (virtual.screenScale() < 1.0F) {
            float s = virtual.screenScale();
            poseStack.scale(s, s, s);
        }

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
            data.render(poseStack, packedLight, lightPos, buffers);
        } finally {
            SingleMeshVboRenderer.setEntityMissileDepthBias(false);
            RenderSystem.setShaderFogStart(prevFogStart);
            RenderSystem.setShaderFogEnd(prevFogEnd);
            LightSampleCache.BASE_POSE_SET.set(false);
            poseStack.popPose();
        }
    }

    public static double maxSafeRenderDistanceBlocks() {
        Minecraft mc = Minecraft.getInstance();
        int chunks = mc.options.getEffectiveRenderDistance();
        return Math.max(MIN_VIRTUAL_DISTANCE_BLOCKS, chunks * 16.0D * FRUSTUM_SAFETY);
    }

    public static CameraRelativePose virtualizeWorld(double worldX, double worldY, double worldZ, Vec3 camera) {
        double relX = worldX - camera.x;
        double relY = worldY - camera.y;
        double relZ = worldZ - camera.z;
        double dist = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
        double max = maxSafeRenderDistanceBlocks();
        float scale = 1.0F;
        if (dist > max && dist >= 1.0E-4D) {
            scale = (float) (max / dist);
            relX *= scale;
            relY *= scale;
            relZ *= scale;
        }
        return new CameraRelativePose(relX, relY, relZ, scale, worldX, worldY, worldZ);
    }

    public record CameraRelativePose(
            double relX, double relY, double relZ,
            float screenScale,
            double trueX, double trueY, double trueZ
    ) {}
}
