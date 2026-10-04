package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineRadGenBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderRadGen}: Gehaeuse, bei Betrieb drehender Rotor, vollhelle gruene Betriebslampe (gedimmt im
 * Stillstand) und das blaeulich getoente Sichtglas.
 */
public class RadGenRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineRadGenBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/radgen.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/radgen.png");

    public RadGenRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineRadGenBlockEntity radgen, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        switch (radgen.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        MODEL.renderPart("Base", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        ps.pushPose();
        if (radgen.isOn()) {
            ps.translate(0, 1.5, 0);
            ps.mulPose(Axis.XP.rotationDegrees((System.currentTimeMillis() % 3600) * -0.1F));
            ps.translate(0, -1.5, 0);
        }
        MODEL.renderPart("Rotor", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);
        ps.popPose();

        if (radgen.isOn()) MODEL.renderPartColor("Light", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 0F, 1F, 0F, 1F);
        else MODEL.renderPartColor("Light", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 0F, 0.1F, 0F, 1F);

        MODEL.renderPartColor("Glass", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.TRANSLUCENT_COLOR_NOCULL), 0.5F, 0.75F, 1F, 0.3F);
        MODEL.renderPart("Glass", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineRadGenBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
