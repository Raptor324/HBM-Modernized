package com.hbm_m.client.render.entity;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.train.TrainCargoTramTrailer;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code RenderTrainCargoTramTrailer}: Anhaengermodell plus je nach Zahl belegter Plaetze 1-9 Kisten auf der
 * Ladeflaeche (im Original Item-Entitaeten im Rahmenmodus, hier dieselbe Kistengroesse als Block gerendert).
 */
public class RenderTrainCargoTramTrailer extends EntityRenderer<TrainCargoTramTrailer> {

    public static final SimpleObjModel train_cargo_tram_trailer = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/vehicles/tram_trailer.obj"));
    public static final ResourceLocation tram_trailer = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/trains/tram_trailer.png");

    public RenderTrainCargoTramTrailer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(@NotNull TrainCargoTramTrailer train, float swing, float interp, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
        ps.pushPose();
        RenderTrainCargoTram.applyTrainTransform(train, interp, ps);

        train_cargo_tram_trailer.renderAll(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(tram_trailer)), light);

        int slots = train.getOccupiedSlots();

        if (slots > 0) {

            double scale = 2;
            ps.scale((float) scale, (float) scale, (float) scale);

            if (slots <= 5) {
                crate(ps, buffers, light, 0.0D, 0.375D, 0.0D);
            } else if (slots <= 10) {
                crate(ps, buffers, light, 0.1D, 0.375D, 0.25D);
                crate(ps, buffers, light, -0.1D, 0.375D, -0.25D);
            } else if (slots <= 15) {
                crate(ps, buffers, light, 0.1D, 0.375D, 0.0D);
                crate(ps, buffers, light, -0.1D, 0.375D, 0.375D);
                crate(ps, buffers, light, -0.1D, 0.375D, -0.375D);
            } else if (slots <= 20) {
                crate(ps, buffers, light, 0.2D, 0.375D, 0.3D);
                crate(ps, buffers, light, 0.2D, 0.375D, -0.2D);
                crate(ps, buffers, light, -0.2D, 0.375D, 0.2D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.3D);
            } else if (slots <= 25) {
                crate(ps, buffers, light, 0.2D, 0.375D, 0.6D);
                crate(ps, buffers, light, 0.2D, 0.375D, 0.0D);
                crate(ps, buffers, light, 0.2D, 0.375D, -0.5D);
                crate(ps, buffers, light, -0.2D, 0.375D, 0.2D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.3D);
            } else if (slots <= 30) {
                crate(ps, buffers, light, 0.2D, 0.375D, 0.6D);
                crate(ps, buffers, light, 0.2D, 0.375D, 0.0D);
                crate(ps, buffers, light, 0.2D, 0.375D, -0.5D);
                crate(ps, buffers, light, -0.2D, 0.375D, 0.5D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.1D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.6D);
            } else if (slots <= 35) {
                crate(ps, buffers, light, 0.2D, 0.375D, 0.4D);
                crate(ps, buffers, light, 0.2D, 0.375D, 0.0D);
                crate(ps, buffers, light, 0.2D, 0.375D, -0.4D);
                crate(ps, buffers, light, -0.2D, 0.375D, 0.3D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.1D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.5D);
                crate(ps, buffers, light, 0.0D, 0.6875D, -0.25D);
            } else if (slots <= 40) {
                crate(ps, buffers, light, 0.2D, 0.375D, 0.4D);
                crate(ps, buffers, light, 0.2D, 0.375D, 0.0D);
                crate(ps, buffers, light, 0.2D, 0.375D, -0.4D);
                crate(ps, buffers, light, -0.2D, 0.375D, 0.3D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.1D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.5D);
                crate(ps, buffers, light, 0.0D, 0.6875D, -0.25D);
                crate(ps, buffers, light, 0.0D, 0.6875D, 0.15D);
            } else {
                crate(ps, buffers, light, 0.2D, 0.375D, 0.4D);
                crate(ps, buffers, light, 0.2D, 0.375D, 0.0D);
                crate(ps, buffers, light, 0.2D, 0.375D, -0.4D);
                crate(ps, buffers, light, -0.2D, 0.375D, 0.3D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.1D);
                crate(ps, buffers, light, -0.2D, 0.375D, -0.5D);
                crate(ps, buffers, light, 0.0D, 0.6875D, -0.25D);
                crate(ps, buffers, light, 0.0D, 0.6875D, 0.15D);
                crate(ps, buffers, light, -0.1D, 0.375D, 0.8D);
            }
        }

        ps.popPose();
    }

    /**
     * Original {@code renderEntityWithPosYaw(EntityItem(crate))} mit {@code renderInFrame}: Schweben 0.1 (Alter 0),
     * Rahmen-Skalierung 1.25, Versatz 0.05, Drehung -90, Blockgroesse 0.25, Block um den Mittelpunkt.
     */
    private static void crate(PoseStack ps, MultiBufferSource buffers, int light, double x, double y, double z) {
        BlockState crate = ModBlocks.CRATE.get().defaultBlockState();
        ps.pushPose();
        ps.translate(x, y + 0.1D, z);
        ps.scale(1.25F, 1.25F, 1.25F);
        ps.translate(0.0F, 0.05F, 0.0F);
        ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
        ps.scale(0.25F, 0.25F, 0.25F);
        ps.translate(-0.5F, -0.5F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(crate, ps, buffers, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull TrainCargoTramTrailer entity) {
        return tram_trailer;
    }
}
