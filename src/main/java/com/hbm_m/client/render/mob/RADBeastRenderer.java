package com.hbm_m.client.render.mob;

import com.hbm_m.entity.mob.EntityRADBeast;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of {@code RenderRADBeast}. The original reuses the vanilla blaze model with its own
 * skin, so this does the same.
 *
 * <p>Dazu wie im Original der gruene Strahl ({@code BeamPronter}, RANDOM/SOLID) zum aktuellen Opfer und die
 * M65-Gasmaske als zweiter Durchgang ({@code shouldRenderPass} Pass 0 mit {@code ModelM65Blaze}).</p>
 */
public class RADBeastRenderer extends MobRenderer<EntityRADBeast, BlazeModel<EntityRADBeast>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/radbeast.png");
    private static final ResourceLocation MASK =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/m65_blaze.png");

    public RADBeastRenderer(EntityRendererProvider.Context context) {
        super(context, new BlazeModel<>(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
        this.addLayer(new M65MaskLayer(this));
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
        net.minecraft.world.entity.Entity victim = beast.getUnfortunateSoul();

        if (victim != null && beast.getY() > 0.1) {
            poseStack.pushPose();
            poseStack.translate(0, 1.25, 0);

            double sx = beast.getX();
            double sy = beast.getY() + 1.25;
            double sz = beast.getZ();

            // Original: posY + height/2 (beim lokalen Spieler war posY die Augenhoehe, daher dort -1.5)
            double tX = victim.getX();
            double tY = victim.getY() + victim.getBbHeight() / 2;
            double tZ = victim.getZ();

            double length = Math.sqrt(Math.pow(tX - sx, 2) + Math.pow(tY - sy, 2) + Math.pow(tZ - sz, 2));
            if (length < 200) com.hbm_m.client.render.util.BeamPronter.prontBeam(poseStack, buffer, new net.minecraft.world.phys.Vec3(tX - sx, tY - sy, tZ - sz),
                    com.hbm_m.client.render.util.BeamPronter.EnumWaveType.RANDOM, com.hbm_m.client.render.util.BeamPronter.EnumBeamType.SOLID,
                    0x004000, 0x004000, (int) (beast.level().getGameTime() % 1000 + 1), (int) (length * 5), 0.125F, 2, 0.03125F);

            poseStack.popPose();
        }

        super.render(beast, yaw, partialTick, poseStack, buffer, light);
    }

    /**
     * 1:1 {@code ModelM65Blaze}: Maske (Texturgroesse 32x32) als Kind an Kopfposition und -winkeln, gezeichnet mit
     * {@code glScaled(18/16)} und {@code glScaled(1.01)}.
     */
    private static class M65MaskLayer extends RenderLayer<EntityRADBeast, BlazeModel<EntityRADBeast>> {

        private final ModelPart mask;

        M65MaskLayer(RenderLayerParent<EntityRADBeast, BlazeModel<EntityRADBeast>> parent) {
            super(parent);
            this.mask = createMask().bakeRoot().getChild("mask");
        }

        private static LayerDefinition createMask() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition mask = mesh.getRoot().addOrReplaceChild("mask", CubeListBuilder.create(), PartPose.ZERO);
            float yOffset = 4F;
            // convertToChild: Gelenkpunkte relativ zur Maske (deren Gelenkpunkt 0,0,0 ist)
            mask.addOrReplaceChild("shape1", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 8, 8, 8),
                    PartPose.offset(-4F, -8F + yOffset, -4F));
            mask.addOrReplaceChild("shape2", CubeListBuilder.create().texOffs(0, 16).addBox(0F, 0F, 0F, 3, 3, 1),
                    PartPose.offset(-1.5F, -3.5F + yOffset, -5F));
            mask.addOrReplaceChild("shape3", CubeListBuilder.create().texOffs(0, 20).addBox(0F, -2F, 0F, 2, 2, 1),
                    PartPose.offsetAndRotation(-1F, -3.5F + yOffset, -5F, -0.4799655F, 0F, 0F));
            mask.addOrReplaceChild("shape4", CubeListBuilder.create().texOffs(8, 16).addBox(0F, 0F, -2F, 3, 2, 2),
                    PartPose.offsetAndRotation(-1.5F, -2F + yOffset, -4F, 0.6108652F, 0F, 0F));
            mask.addOrReplaceChild("shape5", CubeListBuilder.create().texOffs(0, 23).addBox(0F, 0F, 0F, 3, 3, 0),
                    PartPose.offset(-3.5F, -6F + yOffset, -4.2F));
            mask.addOrReplaceChild("shape6", CubeListBuilder.create().texOffs(0, 26).addBox(0F, 0F, 0F, 3, 3, 0),
                    PartPose.offset(0.5F, -6F + yOffset, -4.2F));
            mask.addOrReplaceChild("shape7", CubeListBuilder.create().texOffs(6, 20).addBox(0F, 0F, 0F, 2, 2, 1),
                    PartPose.offset(-1F, -3.2F + yOffset, -6F));
            mask.addOrReplaceChild("shape8", CubeListBuilder.create().texOffs(6, 23).addBox(0F, 0F, -3F, 2, 2, 1),
                    PartPose.offsetAndRotation(-1F, -2F + yOffset, -4F, 0.6108652F, 0F, 0F));
            mask.addOrReplaceChild("shape9", CubeListBuilder.create().texOffs(18, 21).addBox(0F, -1F, -5F, 3, 4, 2),
                    PartPose.offsetAndRotation(-1.5F, -2F + yOffset, -4F, 0.6108652F, 0F, 0F));
            mask.addOrReplaceChild("shape10", CubeListBuilder.create().texOffs(18, 16).addBox(0F, -0.5F, -5F, 4, 3, 2),
                    PartPose.offsetAndRotation(-2F, -2F + yOffset, -4F, 0.6108652F, 0F, 0F));
            return LayerDefinition.create(mesh, 32, 32);
        }

        @Override
        public void render(@NotNull PoseStack pose, @NotNull MultiBufferSource buffer, int light, @NotNull EntityRADBeast beast,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            // setRotationAngles: Gelenkpunkt und Y/X-Winkel vom Bipedkopf (Gelenkpunkt 0,0,0)
            mask.setPos(0F, 0F, 0F);
            mask.yRot = netHeadYaw * ((float) Math.PI / 180F);
            mask.xRot = headPitch * ((float) Math.PI / 180F);

            pose.pushPose();
            float d = 1F / 16F * 18F;
            pose.scale(d, d, d);
            pose.scale(1.01F, 1.01F, 1.01F);
            mask.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(MASK)), light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
