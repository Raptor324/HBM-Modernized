package com.hbm_m.client.render.implementations;

import java.util.Random;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity;
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

/** 1:1 {@code RenderExposureChamber}: Kammer, rotierende Magnete, schwebender Kern und die Entladungsbloetze im Betrieb. */
public class ExposureChamberRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineExposureChamberBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/exposure_chamber.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/exposure_chamber.png");
    private static final int FULLBRIGHT = 0xF000F0;

    public ExposureChamberRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineExposureChamberBlockEntity chamber, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 4/3/5/2 -> 180/270/0/90 Grad
        switch (chamber.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }

        var vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        MODEL.renderPart("Chamber", ps, vc, light);

        double rotation = chamber.prevRotation + (chamber.rotation - chamber.prevRotation) * interp;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) rotation));
        MODEL.renderPart("Magnets", ps, vc, light);
        ps.popPose();

        long time = chamber.getLevel() == null ? 0 : chamber.getLevel().getGameTime();

        if (chamber.isOn) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees((float) (rotation / 2D)));
            ps.translate(0, Math.sin((time % (Math.PI * 16D) + interp) * 0.125) * 0.0625, 0);
            MODEL.renderPart("Core", ps, vc, FULLBRIGHT);
            ps.popPose();

            int duration = 8;
            Random rand = new Random(time / duration);
            int chance = 2;
            int color = time % duration >= duration / 2 ? 0x80d0ff : 0xffffff;
            rand.nextInt(chance); // Original: der erste Wurf verhaelt sich seltsam
            int start = (int) (System.currentTimeMillis() % 1000) / 50;
            if (rand.nextInt(chance) == 0) {
                ps.pushPose();
                ps.translate(0, 3.675, -7.5);
                BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 5), EnumWaveType.RANDOM, EnumBeamType.LINE, color, 0xffffff, start, 15, 0.125F, 1, 0);
                ps.popPose();
            }
            if (rand.nextInt(chance) == 0) {
                ps.pushPose();
                ps.translate(1.1875, 2.5, -7.5);
                BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 5), EnumWaveType.RANDOM, EnumBeamType.LINE, color, 0xffffff, start, 15, 0.125F, 1, 0);
                ps.popPose();
            }
            if (rand.nextInt(chance) == 0) {
                ps.pushPose();
                ps.translate(-1.1875, 2.5, -7.5);
                BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, 5), EnumWaveType.RANDOM, EnumBeamType.LINE, color, 0xffffff, start, 15, 0.125F, 1, 0);
                ps.popPose();
            }

            ps.pushPose();
            ps.translate(0, 1.75, 0);
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 1.5, 0), EnumWaveType.RANDOM, EnumBeamType.LINE, 0x80d0ff, 0xffffff, start, 10, 0.125F, 1, 0);
            // Original: (System.currentTimeMillis() + 5 % 1000) / 50 - Operatorrangfolge beibehalten
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 1.5, 0), EnumWaveType.RANDOM, EnumBeamType.LINE, 0x8080ff, 0xffffff, (int) (System.currentTimeMillis() + 5 % 1000) / 50, 10, 0.125F, 1, 0);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, 2.5, 0);
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, -1), EnumWaveType.SPIRAL, EnumBeamType.LINE, 0xffff80, 0xffffff, (int) (System.currentTimeMillis() % 360), 15, 0.125F, 1, 0);
            BeamPronter.prontBeam(ps, buf, new Vec3(0, 0, -1), EnumWaveType.SPIRAL, EnumBeamType.LINE, 0xff8080, 0xffffff, (int) (System.currentTimeMillis() % 360) + 180, 15, 0.125F, 1, 0);
            ps.popPose();
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineExposureChamberBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
