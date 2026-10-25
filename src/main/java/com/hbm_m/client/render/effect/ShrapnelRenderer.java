package com.hbm_m.client.render.effect;

import com.hbm_m.entity.projectile.EntityShrapnel;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 1:1-Port von {@code RenderShrapnel} + {@code ModelShrapnel}: ein 4x4x4-Pixel-Wuerfel
 * (Textur 16x8, gespiegelt, Drehpunkt 1/-0.5/-0.5), auf den Kopf gestellt und schnell um die
 * Raumdiagonale kreiselnd; Vulkanbrocken (Typ >= 2) dreifach vergroessert.
 */
public class ShrapnelRenderer extends EntityRenderer<EntityShrapnel> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/shrapnel.png");
    private static final Vector3f DIAGONAL = new Vector3f(1, 1, 1).normalize();

    private final ModelPart bullet;

    public ShrapnelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("bullet",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0F, 0F, 0F, 4, 4, 4),
                PartPose.offset(1F, -0.5F, -0.5F));
        this.bullet = LayerDefinition.create(mesh, 16, 8).bakeRoot().getChild("bullet");
    }

    @Override
    public void render(EntityShrapnel entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians((entity.tickCount % 360) * 10 + partialTicks), DIAGONAL));
        if (entity.getShrapnelType() >= 2) pose.scale(3F, 3F, 3F);
        bullet.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityShrapnel entity) {
        return TEXTURE;
    }
}
