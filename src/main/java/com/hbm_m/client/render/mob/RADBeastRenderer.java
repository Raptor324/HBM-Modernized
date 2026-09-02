package com.hbm_m.client.render.mob;

import com.hbm_m.entity.mob.EntityRADBeast;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of {@code RenderRADBeast}. The original reuses the vanilla blaze model with its own
 * skin, so this does the same - the leader is drawn slightly larger to match its bigger health
 * bar, which is the only visual cue the original gives you before it starts hitting for sixteen.
 *
 * <p>The original also draws a beam between the beast and whoever it is currently irradiating,
 * using its {@code BeamPronter} helper. That helper is not ported, so the beam is missing; the
 * victim is still tracked (see {@link EntityRADBeast#getUnfortunateSoul()}) so it can be added
 * later without touching the entity.</p>
 */
public class RADBeastRenderer extends MobRenderer<EntityRADBeast, BlazeModel<EntityRADBeast>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/radbeast.png");

    public RADBeastRenderer(EntityRendererProvider.Context context) {
        super(context, new BlazeModel<>(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
    }

    @Override
    protected void scale(@NotNull EntityRADBeast beast, @NotNull PoseStack poseStack, float partialTick) {
        if (beast.isLeader()) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
        }
        super.scale(beast, poseStack, partialTick);
    }

    @Override
    protected int getBlockLightLevel(@NotNull EntityRADBeast beast, @NotNull net.minecraft.core.BlockPos pos) {
        return 15; // getBrightnessForRender: it lights itself
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityRADBeast beast) {
        return TEXTURE;
    }

    @Override
    public void render(@NotNull EntityRADBeast beast, float yaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int light) {
        super.render(beast, yaw, partialTick, poseStack, buffer, light);
    }
}
