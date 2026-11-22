package com.hbm_m.client.render.projectile;

import com.hbm_m.entity.projectile.ShrapnelEntity;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Port of {@code RenderShrapnel} + {@code ModelShrapnel} (1.7.10): a 4x4x4 cube
 * (16x8 texture) spinning on all three axes at 10 deg/tick; volcanic clumps
 * (modes >= 2) are scaled x3.
 */
public class ShrapnelEntityRenderer extends EntityRenderer<ShrapnelEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/entity/shrapnel.png");

    private final ModelPart model;

    public ShrapnelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bullet",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 4, 4, 4, new CubeDeformation(0.0F)),
                PartPose.offset(1.0F, -0.5F, -0.5F));
        this.model = LayerDefinition.create(mesh, 16, 8).bakeRoot();
    }

    @Override
    public void render(ShrapnelEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        poseStack.translate(-x, -y, -z);

        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        float spin = (entity.tickCount % 360) * 10 + partialTick * 10;
        poseStack.mulPose(Axis.XP.rotationDegrees(spin));
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

        // 1.7.10: lava clumps (mode byte >= 2) are scaled x3
        if (entity.getShrapnelMode() >= 2) {
            poseStack.scale(3.0F, 3.0F, 3.0F);
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        this.model.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShrapnelEntity entity) {
        return TEXTURE;
    }
}
