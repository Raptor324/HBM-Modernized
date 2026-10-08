package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.MachineMissileAssemblyBlock;
import com.hbm_m.blockentity.machines.MissileAssemblyBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.MissilePronter;
import com.hbm_m.item.missile.MissileStruct;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderMissileAssembly}: Das Grundgeruest kommt als Blockmodell, hier folgen die Stuetzstreben (je nach
 * Raketenlaenge jede bzw. jede zweite Einheit links und rechts) und die liegende, halbfertige Rakete auf 1.5 Bloecken.
 */
public class MissileAssemblyRenderer implements BlockEntityRenderer<MissileAssemblyBlockEntity> {

    private static final ResourceLocation STRUT_TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/strut.png");
    private static SimpleObjModel strut;

    public MissileAssemblyRenderer(BlockEntityRendererProvider.Context ctx) { }

    private static SimpleObjModel strut() {
        if (strut == null) strut = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/missile_parts/strut.obj"));
        return strut;
    }

    @Override
    public void render(MissileAssemblyBlockEntity te, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);

        switch (te.getBlockState().getValue(MachineMissileAssemblyBlock.FACING)) {
            case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            default -> { }
        }

        MissileStruct missile = te.getStruct();
        double height = MissilePronter.guiHeight(missile);

        int range = (int) (height / 2 - 1);
        int step = range >= 2 ? 2 : 1;

        for (int i = -range; i <= range; i += step) {
            if (i != 0) {
                ps.pushPose();
                ps.translate(i, 0, 0);
                strut().renderAll(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(STRUT_TEX)), light);
                ps.popPose();
            }
        }

        ps.translate(0, 1.5, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(180));
        ps.translate(-height / 2, 0, 0);
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        ps.mulPose(Axis.ZP.rotationDegrees(-90));

        MissilePronter.prontMissile(missile, ps, buffers, light);

        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MissileAssemblyBlockEntity te) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
