package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderMixer}: Gehaeuse, Ruehrwerk, Fluessigkeit in der Farbe des Ausgangsfluids nach Gesamtfuellstand. */
public class MixerRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineMixerBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/mixer.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/mixer.png");

    public MixerRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineMixerBlockEntity mixer, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0, 0.5D);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        MODEL.renderPart("Main", ps, vc, light);

        ps.pushPose();
        ps.mulPose(Axis.YN.rotationDegrees(mixer.prevRotation + (mixer.rotation - mixer.prevRotation) * interp));
        MODEL.renderPart("Mixer", ps, vc, light);
        ps.popPose();

        int totalFill = 0;
        int totalMax = 0;

        for (FluidTank tank : mixer.tanks) {
            if (FluidTank.isFluidTypeExplicitlySet(tank.getTankType())) {
                totalFill += tank.getFill();
                totalMax += tank.getMaxFill();
            }
        }

        if (totalFill > 0) {
            int color = com.hbm_m.api.fluids.HbmFluidRegistry.getTintColor(mixer.tanks[2].getTankType());
            ps.translate(0, 1, 0);
            ps.scale(1, (float) ((double) totalFill / (double) totalMax * 0.99), 1);
            ps.translate(0, -1, 0);
            MODEL.renderPartColor("Fluid", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.TRANSLUCENT_COLOR_NOCULL),
                    ((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 0.75F);
        }

        ps.popPose();
    }

    @Override public int getViewDistance() { return 256; }
}
