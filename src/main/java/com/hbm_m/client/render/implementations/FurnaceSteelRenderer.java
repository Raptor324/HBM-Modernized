package com.hbm_m.client.render.implementations;

import org.joml.Matrix4f;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineFurnaceSteelBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderFurnaceSteel}: Ofen und im Betrieb die gluehende, pulsierende Ofenoeffnung (additiv). */
public class FurnaceSteelRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFurnaceSteelBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/furnace_steel.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/furnace_steel.png");

    public FurnaceSteelRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineFurnaceSteelBlockEntity furnace, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);

        // Original: Metadaten 3/5/2/4 -> 0/90/180/270 Grad, danach -90
        switch (furnace.getBlockState().getValue(DummyableMachineBlock.FACING)) {
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
        ps.mulPose(Axis.YP.rotationDegrees(-90));

        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        if (furnace.wasOn) {
            float col = (float) Math.sin(System.currentTimeMillis() * 0.001);
            float r = 0.875F + col * 0.125F, g = 0.625F + col * 0.375F, b = 0F, a = 0.5F;
            VertexConsumer vc = buf.getBuffer(ClientRenderHandler.CustomRenderTypes.DETONATOR_LASER_GLOW);
            Matrix4f m = ps.last().pose();
            for (int i = 0; i < 4; i++) {
                float x = 1 + i * 0.0625F;
                //? if < 1.21.1 {
                vc.vertex(m, x, 1, -1).color(r, g, b, a).endVertex();
                vc.vertex(m, x, 1, 1).color(r, g, b, a).endVertex();
                vc.vertex(m, x, 0.5F, 1).color(r, g, b, a).endVertex();
                vc.vertex(m, x, 0.5F, -1).color(r, g, b, a).endVertex();
                //?} else {
                /*vc.addVertex(m, x, 1, -1).setColor(r, g, b, a);
                vc.addVertex(m, x, 1, 1).setColor(r, g, b, a);
                vc.addVertex(m, x, 0.5F, 1).setColor(r, g, b, a);
                vc.addVertex(m, x, 0.5F, -1).setColor(r, g, b, a);
                *///?}
            }
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineFurnaceSteelBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
