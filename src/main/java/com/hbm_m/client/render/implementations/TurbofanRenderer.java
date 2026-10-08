package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderTurbofan}: Gehaeuse, die mit dem Schwung drehenden Schaufeln und die Duese - kalt oder, sobald ein
 * Nachbrenner steckt, gluehend.
 */
public class TurbofanRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineTurbofanBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/turbofan.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/turbofan.png");
    public static final ResourceLocation BACK_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/turbofan_back.png");
    public static final ResourceLocation AFTERBURNER_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/turbofan_afterburner.png");

    public TurbofanRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineTurbofanBlockEntity turbo, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 2/4/3/5 -> 90/180/270/0 Grad
        switch (turbo.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        float spin = turbo.lastSpin + (turbo.spin - turbo.lastSpin) * interp;

        MODEL.renderPart("Body", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        ps.pushPose();
        ps.translate(0, 1.5, 0);
        ps.mulPose(Axis.ZN.rotationDegrees(spin));
        ps.translate(0, -1.5, 0);
        MODEL.renderPart("Blades", ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);
        ps.popPose();

        ResourceLocation back = turbo.afterburner == 0 ? BACK_TEX : AFTERBURNER_TEX;
        MODEL.renderPart("Afterburner", ps, buf.getBuffer(RenderType.entityCutout(back)), light);

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineTurbofanBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
