package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineFireboxBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderFirebox}: Gehaeuse, Schwenkklappe um (1.375, 0.375), Glut voll leuchtend wenn an. */
public class FireboxRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFireboxBlockEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/machines/firebox.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/machines/firebox.png");

    public FireboxRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(MachineFireboxBlockEntity firebox, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        HeatingOvenRenderer.rotate(firebox, ps);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Main", ps, vc, light);

        ps.pushPose();
        float door = firebox.prevDoorAngle + (firebox.doorAngle - firebox.prevDoorAngle) * interp;
        ps.translate(1.375, 0, 0.375);
        ps.mulPose(Axis.YN.rotationDegrees(door));
        ps.translate(-1.375, 0, -0.375);
        MODEL.renderPart("Door", ps, vc, light);
        ps.popPose();

        if (firebox.wasOn) {
            MODEL.renderPart("InnerBurning", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), LightTexture.FULL_BRIGHT);
        } else {
            MODEL.renderPart("InnerEmpty", ps, vc, light);
        }

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MachineFireboxBlockEntity blockEntity) {
        return true;
    }

    @Override public int getViewDistance() { return 256; }
}
