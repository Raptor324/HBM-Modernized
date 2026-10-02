package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineBoilerBlock;
import com.hbm_m.blockentity.machines.MachineBoilerBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderBoiler}: ueber 90 % Fuellung des Ausgangstanks pumpt der Kessel sichtbar (Sinus-Skalierung um 1 %),
 * geplatzt zeigt er das Wrackmodell.
 */
public class BoilerRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineBoilerBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/boiler.obj"));
    public static final SimpleObjModel MODEL_BURST = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/boiler_burst.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/boiler.png");

    public BoilerRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineBoilerBlockEntity boiler, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 3/5/2/4 -> 0/90/180/270 Grad; Port-FACING entspricht dieser Richtung
        switch (boiler.getBlockState().getValue(MachineBoilerBlock.FACING)) {
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        if (!boiler.hasExploded) {

            if (boiler.tanks[1].getFill() > boiler.tanks[1].getMaxFill() * 0.9) {
                double sine = Math.sin(System.currentTimeMillis() / 50D % (Math.PI * 2));
                sine *= 0.01D;
                ps.scale((float) (1 - sine), (float) (1 + sine), (float) (1 - sine));
            }

            MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);
        } else {
            MODEL_BURST.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineBoilerBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
