package com.hbm_m.client.render.entity;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.cart.EntityMinecartTest;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code RenderMinecartTest}: Vanilla-Lore, statt des Blocks das Little-Boy-Modell; beim Zuenden wie die TNT-Lore
 * aufblaehend und weiss blinkend. Das Original verweist auf die nicht vorhandene Textur {@code LilBoy2.png}; der Port
 * nimmt die Little-Boy-Modelltextur.
 */
public class RenderMinecartTest extends MinecartRenderer<EntityMinecartTest> {

    private static final SimpleObjModel boyModel = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/projectiles/lilboy1.obj"));
    private static final ResourceLocation boyTexture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lilboy.png");

    public RenderMinecartTest(EntityRendererProvider.Context ctx) {
        super(ctx, ModelLayers.TNT_MINECART);
    }

    @Override
    protected void renderMinecartContents(@NotNull EntityMinecartTest cart, float interp, @NotNull BlockState state, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
        // Vanilla verschiebt um die Blockecke und dreht um 90 Grad; das Original rendert um den Mittelpunkt
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
        ps.translate(0.5F, 0.5F, -0.5F);

        int j = cart.getFuse();

        if (j > -1 && (float) j - interp + 1.0F < 10.0F) {
            float f1 = 1.0F - ((float) j - interp + 1.0F) / 10.0F;
            f1 = Math.max(0.0F, Math.min(1.0F, f1));
            f1 *= f1;
            f1 *= f1;
            float f2 = 1.0F + f1 * 0.3F;
            ps.scale(f2, f2, f2);
        }

        boolean flash = j > -1 && j / 5 % 2 == 0;
        int overlay = flash ? OverlayTexture.pack(OverlayTexture.u(1.0F), 10) : OverlayTexture.NO_OVERLAY;
        boyModel.renderAllEntity(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(boyTexture)), light, overlay, 1F, 1F, 1F, 1F);

        ps.popPose();
    }
}
