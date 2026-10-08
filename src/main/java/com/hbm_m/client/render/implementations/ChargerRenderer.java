package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.ChargerBlockEntity;
import com.hbm_m.client.ClientRenderHandler;
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

/**
 * 1:1 {@code RenderCharger}: Sockel, Gleitstueck faehrt mit {@code extend} ab und die Arme klappen mit
 * {@code swivel} um 30 Grad auf; das Licht leuchtet orange (1/0.75/0) ohne Textur, das Gleitstueck vollhell.
 */
public class ChargerRenderer implements com.hbm_m.client.render.HbmBerBounds<ChargerBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/blocks/charger.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/machine/charger.png");
    /** Original {@code delay = 20}. */
    private static final double DELAY = 20D;

    public ChargerRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(ChargerBlockEntity charger, float interp, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(90));
        // Original: Metadaten 4 -> 90, 3 -> 180, 5 -> 270, 2 -> 0 (FACING entspricht der Original-Metadate)
        switch (ObjBerHelper.facing(charger)) {
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            default -> { }
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Base", ps, vc, light);

        double time = (charger.getLastUsingTicks() + (charger.getUsingTicks() - charger.getLastUsingTicks()) * interp) / DELAY;
        double extend = Math.min(1, time * 2);
        double swivel = Math.max(0, (time - 0.5) * 2);

        ps.pushPose();
        ps.translate(-0.34375D, 0.25D, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(10));
        ps.translate(0.34375D, -0.25D, 0);
        ps.translate(0, -0.25 * extend, 0);

        ps.pushPose();
        ps.translate(0, 0.28D, 0);
        ps.mulPose(Axis.XP.rotationDegrees((float) (30 * swivel)));
        ps.translate(0, -0.28D, 0);
        MODEL.renderPart("Left", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 0.28D, 0);
        ps.mulPose(Axis.XP.rotationDegrees((float) (-30 * swivel)));
        ps.translate(0, -0.28D, 0);
        MODEL.renderPart("Right", ps, vc, light);
        ps.popPose();
        ps.popPose();

        ps.pushPose();
        // Licht: Textur aus, Beleuchtung aus, kein Culling, 240/240
        MODEL.renderPartColor("Light", ps, buf.getBuffer(ClientRenderHandler.CustomRenderTypes.SOLID_COLOR_NOCULL), 1F, 0.75F, 0F, 1F);
        ps.translate(-0.34375D, 0.25D, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(10));
        ps.translate(0.34375D, -0.25D, 0);
        ps.translate(0, -0.25 * extend, 0);
        MODEL.renderPart("Slide", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), LightTexture.FULL_BRIGHT);
        ps.popPose();

        ps.popPose();
    }
}
