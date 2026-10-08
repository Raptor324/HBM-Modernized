package com.hbm_m.client.render.implementations;

import java.util.Random;

import com.hbm_m.block.bomb.CrashedBombBlock;
import com.hbm_m.blockentity.bomb.CrashedBombBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code RenderCrashedBomb}: der Blindgaenger steckt schraeg im Boden; Gier, Neigung (45-90), Rollen und Versatz
 * kommen aus einem Zufall mit der Blockposition als Saat ({@code BlockPos.getIdentity}).
 */
public class CrashedBombRenderer implements com.hbm_m.client.render.HbmBerBounds<CrashedBombBlockEntity> {

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p); }

    public static final SimpleObjModel[] MODELS = {
            new SimpleObjModel(rl("models/bombs/dud_balefire.obj")), new SimpleObjModel(rl("models/bombs/dud_conventional.obj")),
            new SimpleObjModel(rl("models/bombs/dud_nuke.obj")), new SimpleObjModel(rl("models/bombs/dud_salted.obj")) };
    public static final ResourceLocation[] TEX = {
            rl("textures/models/bombs/dud_balefire.png"), rl("textures/models/bombs/dud_conventional.png"),
            rl("textures/models/bombs/dud_nuke.png"), rl("textures/models/bombs/dud_salted.png") };

    private final Random rand = new Random();

    public CrashedBombRenderer(BlockEntityRendererProvider.Context ctx) { }

    /** {@code com.hbm.util.fauxpointtwelve.BlockPos.getIdentity}. */
    private static int identity(BlockPos p) {
        return (p.getY() + p.getZ() * 27644437) * 27644437 + p.getX();
    }

    @Override
    public void render(CrashedBombBlockEntity tile, float f, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(tile.getBlockState().getBlock() instanceof CrashedBombBlock block)) return;
        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);

        rand.setSeed(identity(tile.getBlockPos()));
        double yaw = rand.nextDouble() * 360;
        double pitch = rand.nextDouble() * 45 + 45;
        double roll = rand.nextDouble() * 360;
        double offset = rand.nextDouble() * 2 - 1;

        ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
        ps.mulPose(Axis.XP.rotationDegrees((float) pitch));
        ps.mulPose(Axis.ZP.rotationDegrees((float) roll));
        ps.translate(0, 0, -offset);

        int t = block.type.ordinal();
        if (block.type == CrashedBombBlock.DudType.NUKE) ps.translate(0, 0, 1.25);
        if (block.type == CrashedBombBlock.DudType.SALTED) ps.translate(0, 0, 0.5);
        MODELS[t].renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX[t])), light);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(CrashedBombBlockEntity tile) {
        return true;
    }
}
