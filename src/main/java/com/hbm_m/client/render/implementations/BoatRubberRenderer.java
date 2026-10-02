package com.hbm_m.client.render.implementations;

import com.hbm_m.entity.item.EntityBoatRubber;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code com.hbm.render.entity.item.RenderBoatRubber}: das alte 1.7.10-{@code ModelBoat} (fuenf Quader,
 * 64x32-Textur) mit {@code textures/entity/boat_rubber.png}. Wie im Original dreht sich die Blickrichtung des
 * lokalen Fahrers mit dem Boot mit.
 */
public class BoatRubberRenderer extends EntityRenderer<EntityBoatRubber> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/boat_rubber.png");

    private final ModelPart model;

    public BoatRubberRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.model = createModel().bakeRoot();
    }

    /** net.minecraft.client.model.ModelBoat (1.7.10). */
    public static LayerDefinition createModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int b0 = 24;
        int b1 = 6;
        int b2 = 20;
        int b3 = 4;
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 8)
                        .addBox((float) (-b0 / 2), (float) (-b2 / 2 + 2), -3.0F, b0, b2 - 4, 4),
                PartPose.offsetAndRotation(0.0F, (float) b3, 0.0F, (float) Math.PI / 2F, 0.0F, 0.0F));
        root.addOrReplaceChild("side1", CubeListBuilder.create().texOffs(0, 0)
                        .addBox((float) (-b0 / 2 + 2), (float) (-b1 - 1), -1.0F, b0 - 4, b1, 2),
                PartPose.offsetAndRotation((float) (-b0 / 2 + 1), (float) b3, 0.0F, 0.0F, (float) Math.PI * 3F / 2F, 0.0F));
        root.addOrReplaceChild("side2", CubeListBuilder.create().texOffs(0, 0)
                        .addBox((float) (-b0 / 2 + 2), (float) (-b1 - 1), -1.0F, b0 - 4, b1, 2),
                PartPose.offsetAndRotation((float) (b0 / 2 - 1), (float) b3, 0.0F, 0.0F, (float) Math.PI / 2F, 0.0F));
        root.addOrReplaceChild("side3", CubeListBuilder.create().texOffs(0, 0)
                        .addBox((float) (-b0 / 2 + 2), (float) (-b1 - 1), -1.0F, b0 - 4, b1, 2),
                PartPose.offsetAndRotation(0.0F, (float) b3, (float) (-b2 / 2 + 1), 0.0F, (float) Math.PI, 0.0F));
        root.addOrReplaceChild("side4", CubeListBuilder.create().texOffs(0, 0)
                        .addBox((float) (-b0 / 2 + 2), (float) (-b1 - 1), -1.0F, b0 - 4, b1, 2),
                PartPose.offset(0.0F, (float) b3, (float) (b2 / 2 - 1)));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(EntityBoatRubber entity, float yaw, float interp, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // Original rendert auf posY = Mitte der 0.6 hohen Hitbox
        pose.translate(0.0D, 0.3D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float f2 = (float) entity.getTimeSinceHit() - interp;
        float f3 = entity.getDamageTaken() - interp;

        if (f3 < 0.0F) {
            f3 = 0.0F;
        }

        if (f2 > 0.0F) {
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(f2) * f2 * f3 / 10.0F * (float) entity.getForwardDirection()));
        }

        LocalPlayer me = Minecraft.getInstance().player;

        if (me != null && entity.hasPassenger(me)) {
            float diff = Mth.wrapDegrees(entity.getYRot() - entity.prevRenderYaw);
            me.setYRot(me.getYRot() + diff);
            me.yHeadRot += diff;
        }

        entity.prevRenderYaw = entity.getYRot();

        pose.scale(-1.0F, -1.0F, 1.0F);
        this.model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, interp, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityBoatRubber entity) {
        return TEXTURE;
    }
}
