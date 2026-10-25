package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.rail.RailDummyableBlock;
import com.hbm_m.block.rail.RailSwitchBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Weichenschild der Weichen ({@code rail_large_switch(_flipped)}). Im Original Teil des statischen Blockrenderers
 * ({@code renderWorld}: Teil {@code SignTurn}/{@code SignStraight} je nach Stellung); die Schiene selbst ist hier ein
 * statisches OBJ-Blockmodell, das stellungsabhaengige Schild rendert dieser BER mit denselben Verschiebungen/Drehungen.
 */
public class RailSwitchRenderer implements com.hbm_m.client.render.HbmBerBounds<RailSwitchBlockEntity> {

    private static final SimpleObjModel SWITCH = new SimpleObjModel(rl("models/block/rail/rail_standard_switch.obj"));
    private static final SimpleObjModel SWITCH_FLIPPED = new SimpleObjModel(rl("models/block/rail/rail_standard_switch_flipped.obj"));
    private static final ResourceLocation SIGN = rl("textures/block/rail_switch_sign.png");
    private static final ResourceLocation SIGN_FLIPPED = rl("textures/block/rail_switch_sign_flipped.png");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
    }

    public RailSwitchRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(@NotNull RailSwitchBlockEntity sw, float interp, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light, int overlay) {
        BlockState state = sw.getBlockState();
        if (!state.hasProperty(RailDummyableBlock.META)) return;
        int meta = state.getValue(RailDummyableBlock.META);
        if (meta < 12) return;

        boolean flipped = state.is(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get());

        float rotation = 0;
        if (meta == 15) rotation = 90F / 180F * (float) Math.PI;
        if (meta == 12) rotation = 180F / 180F * (float) Math.PI;
        if (meta == 14) rotation = 270F / 180F * (float) Math.PI;

        ps.pushPose();
        if (meta == 12) ps.translate(0.5F, 0F, 0F);
        if (meta == 13) ps.translate(-0.5F, 0F, 0F);
        if (meta == 14) ps.translate(0F, 0F, -0.5F);
        if (meta == 15) ps.translate(0F, 0F, 0.5F);
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotation(rotation));

        (flipped ? SWITCH_FLIPPED : SWITCH).renderPart(sw.isSwitched ? "SignTurn" : "SignStraight", ps,
                buffers.getBuffer(RenderType.entityCutout(flipped ? SIGN_FLIPPED : SIGN)), light);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull RailSwitchBlockEntity sw) {
        return true;
    }
}
