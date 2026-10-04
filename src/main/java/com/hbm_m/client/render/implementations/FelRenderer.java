package com.hbm_m.client.render.implementations;

import java.awt.Color;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineFelBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.client.render.util.BeamPronter.EnumBeamType;
import com.hbm_m.client.render.util.BeamPronter.EnumWaveType;
import com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderFEL}: das Lasergehaeuse und - solange er feuert - der Strahl in der Farbe der Wellenlaenge
 * (sichtbares Licht als dunkler Regenbogen), gewendelt plus zufaellig zuckend, bis zum getroffenen Block.
 */
public class FelRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFelBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/fel.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/fel.png");

    public FelRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineFelBlockEntity fel, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 4/3/5/2 -> 90/180/270/0 Grad
        switch (fel.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        long time = fel.getLevel() != null ? fel.getLevel().getGameTime() : 0;
        int color;
        if (fel.mode.renderedBeamColor == 0) {
            color = Color.HSBtoRGB(time / 50.0F, 0.5F, 0.1F) & 16777215;
        } else {
            color = fel.mode.renderedBeamColor;
        }
        int length = fel.distance - 3;
        ps.translate(0, 1.5, -1.5);
        if (fel.getEnergyStored() > MachineFelBlockEntity.powerReq * Math.pow(2, fel.mode.ordinal()) && fel.isOn && fel.mode != EnumWavelengths.NULL && length > 0) {
            BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, -length - 1), EnumWaveType.SPIRAL, EnumBeamType.SOLID, color, color, 0, 1, 0F, 2, 0.0625F);
            BeamPronter.prontBeamwithDepth(ps, buf, new Vec3(0, 0, -length - 1), EnumWaveType.RANDOM, EnumBeamType.SOLID, color, color, (int) (time % 1000 / 2), (length / 2) + 1, 0.0625F, 2, 0.0625F);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineFelBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
