package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.BlastDoorBlock;
import com.hbm_m.blockentity.machines.BlastDoorBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderBlastDoor}: Sockel, Kopfblock, Zahn und bis zu vier Schieber je nach Oeffnungsfortschritt (5 s). */
public class BlastDoorRenderer implements com.hbm_m.client.render.HbmBerBounds<BlastDoorBlockEntity> {

    private static SimpleObjModel obj(String n) { return new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/" + n + ".obj")); }
    private static ResourceLocation tex(String n) { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/" + n + ".png"); }

    public static final SimpleObjModel BASE = obj("blast_door_base"), BLOCK = obj("blast_door_block"), TOOTH = obj("blast_door_tooth"), SLIDER = obj("blast_door_slider");
    public static final ResourceLocation BASE_TEX = tex("blast_door_base"), BLOCK_TEX = tex("blast_door_block"), TOOTH_TEX = tex("blast_door_tooth"), SLIDER_TEX = tex("blast_door_slider");

    public BlastDoorRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static double anim(long time) {
        double duration = 5000D, extend = 5.0D;
        return Math.max(Math.min(time, duration) / duration * extend, 0.0D);
    }

    @Override
    public void render(BlastDoorBlockEntity te, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0, 0.5D);
        ps.mulPose(Axis.YP.rotationDegrees(180));
        if (te.getBlockState().getValue(BlastDoorBlock.FACING) == Direction.NORTH) ps.mulPose(Axis.YP.rotationDegrees(90));

        double timer;
        if (te.state == 0) timer = anim(5000);
        else if (te.state == 2) timer = 0;
        else if (te.isOpening) timer = anim(te.sysTime + 5000 - System.currentTimeMillis());
        else timer = anim(System.currentTimeMillis() - te.sysTime);

        BASE.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(BASE_TEX)), light);
        ps.translate(0, 3, 0);
        BLOCK.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(BLOCK_TEX)), light);
        ps.translate(0, -timer, 0);
        ps.translate(0, 2, 0);
        TOOTH.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TOOTH_TEX)), light);

        var sv = buf.getBuffer(RenderType.entityCutoutNoCull(SLIDER_TEX));
        if (timer > 1D) SLIDER.renderAll(ps, sv, light);
        if (timer > 2D) { ps.translate(0, 1, 0); SLIDER.renderAll(ps, sv, light); }
        if (timer > 3D) { ps.translate(0, 1, 0); SLIDER.renderAll(ps, sv, light); }
        if (timer > 4D) { ps.translate(0, 1, 0); SLIDER.renderAll(ps, sv, light); }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(BlastDoorBlockEntity te) { return true; }
    @Override public int getViewDistance() { return 256; }
}
