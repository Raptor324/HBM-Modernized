package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineAshpitBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderAshpit}: Gehaeuse des Heizofens ({@code heater_oven}) mit Ascheschacht-Textur, Schiebeklappe
 * ({@code door * 0.75 / 135}) und {@code InnerBurning} statt {@code Inner}, sobald etwas in den Slots liegt.
 */
public class AshpitRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineAshpitBlockEntity> {

    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/ashpit.png");

    public AshpitRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(MachineAshpitBlockEntity oven, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        // Original: Metadaten 3/5/2/4 -> 0/90/180/270 Grad, danach -90 (wie RenderHeatingOven)
        switch (ObjBerHelper.facing(oven)) {
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }
        ps.mulPose(Axis.YP.rotationDegrees(-90));

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        HeatingOvenRenderer.MODEL.renderPart("Main", ps, vc, light);

        ps.pushPose();
        float door = oven.prevDoorAngle + (oven.doorAngle - oven.prevDoorAngle) * interp;
        ps.translate(0, 0, door * 0.75D / 135D);
        HeatingOvenRenderer.MODEL.renderPart("Door", ps, vc, light);
        ps.popPose();

        if (oven.isFull) {
            HeatingOvenRenderer.MODEL.renderPart("InnerBurning", ps, vc, light);
        } else {
            HeatingOvenRenderer.MODEL.renderPart("Inner", ps, vc, light);
        }

        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(MachineAshpitBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }
}
