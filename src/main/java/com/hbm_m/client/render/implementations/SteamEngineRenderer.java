package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineSteamEngineBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderSteamEngine}: Sockel, Schwungrad, Welle, Pleuel und Kolben mit exakter Kurbelgeometrie. */
public class SteamEngineRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineSteamEngineBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/steam_engine.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/steam_engine.png");

    public SteamEngineRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineSteamEngineBlockEntity engine, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 3/5/2/4 -> 90/180/270/0 Grad
        switch (engine.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        float angle = engine.lastRotor + (engine.rotor - engine.lastRotor) * interp;
        ps.translate(2, 0, 0);
        renderCommon(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light, angle);

        ps.popPose();
    }

    public static void renderCommon(PoseStack ps, VertexConsumer vc, int light, double rot) {
        MODEL.renderPart("Base", ps, vc, light);

        ps.pushPose();
        ps.translate(2, 1.375, 0);
        ps.mulPose(Axis.ZN.rotationDegrees((float) rot));
        ps.translate(-2, -1.375, 0);
        MODEL.renderPart("Flywheel", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 1.375, -0.5);
        ps.mulPose(Axis.XP.rotationDegrees((float) (rot * 2D)));
        ps.translate(0, -1.375, 0.5);
        MODEL.renderPart("Shaft", ps, vc, light);
        ps.popPose();

        double sin = Math.sin(rot * Math.PI / 180D) * 0.25D - 0.25D;
        double cos = Math.cos(rot * Math.PI / 180D) * 0.25D;
        double ang = Math.acos(cos / 1.875D);

        ps.pushPose();
        ps.translate(sin, cos, 0);
        ps.translate(2.25, 1.375, 0);
        ps.mulPose(Axis.ZN.rotationDegrees((float) (ang * 180D / Math.PI - 90D)));
        ps.translate(-2.25, -1.375, 0);
        MODEL.renderPart("Transmission", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        double cath = Math.sqrt(3.515625D - (cos * cos) / 2);
        ps.translate(1.875 - cath + sin, 0, 0);
        MODEL.renderPart("Piston", ps, vc, light);
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineSteamEngineBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
