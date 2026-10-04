package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderLaserMiner}: Sockel, um die Hochachse schwenkender Drehkranz, auf das Ziel geneigter Emitter und
 * der dreifach gewendelte rote Strahl.
 */
public class MiningLaserRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineMiningLaserBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/mining_laser.obj"));
    public static final ResourceLocation BASE_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/mining_laser_base.png");
    public static final ResourceLocation PIVOT_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/mining_laser_pivot.png");
    public static final ResourceLocation LASER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/mining_laser_laser.png");

    public MiningLaserRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineMiningLaserBlockEntity laser, float interpolation, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, -1D, 0.5D);

        double tx = (laser.targetX - laser.lastTargetX) * interpolation + laser.lastTargetX;
        double ty = (laser.targetY - laser.lastTargetY) * interpolation + laser.lastTargetY;
        double tz = (laser.targetZ - laser.lastTargetZ) * interpolation + laser.lastTargetZ;
        double vx = tx - laser.getBlockPos().getX();
        double vy = ty - laser.getBlockPos().getY() + 3;
        double vz = tz - laser.getBlockPos().getZ();

        Vec3 nVec = new Vec3(vx, vy, vz).normalize().scale(1.5D);

        Vec3 vec = new Vec3(vx - nVec.x, vy - nVec.y, vz - nVec.z);

        double length;
        double yaw = Math.toDegrees(Math.atan2(vec.x, vec.z));
        double sqrt = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        double pitch = Math.toDegrees(Math.atan2(vec.y, sqrt));

        MODEL.renderPart("Base", ps, buf.getBuffer(RenderType.entityCutoutNoCull(BASE_TEX)), light);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
        MODEL.renderPart("Pivot", ps, buf.getBuffer(RenderType.entityCutoutNoCull(PIVOT_TEX)), light);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
        ps.translate(0, -1, 0);
        ps.mulPose(Axis.XN.rotationDegrees((float) (pitch + 90)));
        ps.translate(0, 1, 0);
        MODEL.renderPart("Laser", ps, buf.getBuffer(RenderType.entityCutoutNoCull(LASER_TEX)), light);
        ps.popPose();

        if (laser.beam && laser.getLevel() != null) {
            length = vec.length();
            ps.translate(nVec.x, nVec.y - 1, nVec.z);
            int range = (int) Math.ceil(length * 0.5);
            int time = (int) laser.getLevel().getGameTime() * -25 % 360;
            BeamPronter.prontBeam(ps, buf, vec, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0xa00000, 0xa00000, time, range * 2, 0.075F, 3, 0.025F);
            BeamPronter.prontBeam(ps, buf, vec, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0xa00000, 0xa00000, time + 120, range * 2, 0.075F, 3, 0.025F);
            BeamPronter.prontBeam(ps, buf, vec, EnumWaveType.SPIRAL, EnumBeamType.SOLID, 0xa00000, 0xa00000, time + 240, range * 2, 0.075F, 3, 0.025F);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineMiningLaserBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
