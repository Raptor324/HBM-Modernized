package com.hbm_m.client.render.entity;

import java.util.List;

import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.client.render.raptorpath.ObjAtlasMesh;
import com.hbm_m.entity.logic.EntityPantsir;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Zeichnet das Fahrzeug ueber Raptors VBO-Pfad. Die acht Raeder sind eigene Meshes und drehen sich mit der
 * Fahrt, die vier vorderen lenken mit. Darauf sitzen der Turm und auf ihm Radar und Werfer als Gelenke.
 * Das Modell schaut nach +Z.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class PantsirRenderer extends EntityRenderer<EntityPantsir> {

    private static final String OBJ = "models/machines/pantsir_s1.obj";
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/models/machines/pantsir_s1.png");
    private static final ResourceLocation SPRITE = ObjAtlasMesh.spriteIdOf(TEXTURE);

    private static final ObjAtlasMesh VEHICLE = ObjAtlasMesh.groups(OBJ, SPRITE, "hbm_m/pantsir_s1/vehicle",
            "chassis", "door1", "door2");
    private static final ObjAtlasMesh TURRET = mesh("turret");
    private static final ObjAtlasMesh RADAR = mesh("radar");
    private static final ObjAtlasMesh LAUNCHER = mesh("launcher");

    private static final double WHEEL_X = 1.064D;
    private static final double WHEEL_Y = 0.728D;
    private static final double[] AXLES = {4.1D, 1.9D, -2.31D, -3.82D};
    private static final ObjAtlasMesh[][] WHEELS = new ObjAtlasMesh[AXLES.length][2];

    static {
        for (int axle = 0; axle < AXLES.length; axle++) {
            WHEELS[axle][0] = mesh("wheel_" + axle + "_l");
            WHEELS[axle][1] = mesh("wheel_" + axle + "_r");
        }
    }

    private static ObjAtlasMesh mesh(String group) {
        return ObjAtlasMesh.groups(OBJ, SPRITE, "hbm_m/pantsir_s1/" + group, group);
    }

    public PantsirRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(EntityPantsir entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(EntityPantsir entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        BlockPos pos = entity.blockPosition();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(entity.getRoll()));

        draw(VEHICLE, pose, light, pos, buffer);

        float spin = entity.getWheelAngle(partialTick);
        for (int axle = 0; axle < AXLES.length; axle++) {
            float steer = axle < 2 ? entity.getSteer() * (axle == 0 ? 1.0F : 0.55F) : 0.0F;
            wheel(WHEELS[axle][0], WHEEL_X, AXLES[axle], steer, spin, pose, light, pos, buffer);
            wheel(WHEELS[axle][1], -WHEEL_X, AXLES[axle], steer, spin, pose, light, pos, buffer);
        }

        pose.pushPose();
        aroundY(pose, EntityPantsir.TURRET_AXIS_Z, entity.getTurretYaw(partialTick));
        draw(TURRET, pose, light, pos, buffer);

        pose.pushPose();
        aroundY(pose, EntityPantsir.RADAR_AXIS_Z, entity.getRadarYaw(partialTick));
        draw(RADAR, pose, light, pos, buffer);
        pose.popPose();

        pose.translate(0.0D, EntityPantsir.LAUNCHER_AXIS_Y, EntityPantsir.TURRET_AXIS_Z);
        pose.mulPose(Axis.XP.rotationDegrees(-entity.getLauncherPitch(partialTick)));
        pose.translate(0.0D, -EntityPantsir.LAUNCHER_AXIS_Y, -EntityPantsir.TURRET_AXIS_Z);
        draw(LAUNCHER, pose, light, pos, buffer);
        pose.popPose();

        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, light);
    }

    private static void aroundY(PoseStack pose, double axisZ, float degrees) {
        pose.translate(0.0D, 0.0D, axisZ);
        pose.mulPose(Axis.YP.rotationDegrees(degrees));
        pose.translate(0.0D, 0.0D, -axisZ);
    }

    private static void wheel(ObjAtlasMesh mesh, double x, double z, float steer, float spin, PoseStack pose,
                              int light, BlockPos pos, MultiBufferSource buffer) {
        pose.pushPose();
        pose.translate(x, WHEEL_Y, z);
        pose.mulPose(Axis.YP.rotationDegrees(steer));
        pose.mulPose(Axis.XP.rotationDegrees(spin));
        pose.translate(-x, -WHEEL_Y, -z);
        draw(mesh, pose, light, pos, buffer);
        pose.popPose();
    }

    private static void draw(ObjAtlasMesh mesh, PoseStack pose, int light, BlockPos pos, MultiBufferSource buffer) {
        List<BakedQuad> quads = mesh.quads();
        if (quads.isEmpty()) {
            return;
        }
        SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRendererFromQuadList(mesh.cacheKey(), quads);
        if (renderer != null) {
            renderer.render(pose, light, pos, null, buffer);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityPantsir entity) {
        return TEXTURE;
    }
}
