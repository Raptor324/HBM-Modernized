package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.TeslaBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RenderTesla}: Spule (um 180 Grad gedreht) und je Ziel ein grauer Zufallsblitz ab der Spulenspitze (offset 1.75). */
public class TeslaRenderer implements com.hbm_m.client.render.HbmBerBounds<TeslaBlockEntity> {

    static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/tesla.obj"));
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/tesla.png");
    /** Original {@code TileEntityTesla.offset}. */
    private static final double OFFSET = 1.75D;

    public TeslaRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(TeslaBlockEntity tesla, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5D, 0D, 0.5D);
        ObjBerHelper.rotY(ps, 180);
        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);

        double sx = tesla.getBlockPos().getX() + 0.5D;
        double sy = tesla.getBlockPos().getY() + OFFSET;
        double sz = tesla.getBlockPos().getZ() + 0.5D;
        ps.translate(0.0D, OFFSET, 0.0D);

        long time = tesla.getLevel() != null ? tesla.getLevel().getGameTime() : 0L;
        for (Vec3 target : tesla.getTargets()) {
            double length = Math.sqrt(Math.pow(target.x - sx, 2) + Math.pow(target.y - sy, 2) + Math.pow(target.z - sz, 2));
            BeamPronter.prontBeam(ps, buf, new Vec3(-target.x + sx, target.y - sy, -target.z + sz),
                    BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x404040, 0x404040,
                    (int) time % 1000 + 1, (int) (length * 5), 0.125F, 2, 0.03125F);
        }
        ps.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TeslaBlockEntity be) { return true; }
}
