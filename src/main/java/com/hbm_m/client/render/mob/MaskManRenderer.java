package com.hbm_m.client.render.mob;

import com.hbm_m.client.render.implementations.RBMKColumnRenderer;
import com.hbm_m.entity.mob.EntityMaskMan;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * 1:1 port of {@code RenderMaskMan} + {@code ModelMaskMan}.
 *
 * <p>{@code maskman.obj} is a part-per-limb export: Torso, LArm, RArm, LLeg, RLeg, Head, and a
 * Skull + IOU pair that replace the head once the boss drops below half health. Limbs swing off
 * the vanilla walk cycle, at the reduced amplitudes the original picks per part - a quarter for
 * the arms, full for the legs.</p>
 */
public class MaskManRenderer extends EntityRenderer<EntityMaskMan> {

    private static final String MODEL = "models/mobs/maskman.obj";

    public MaskManRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F; // shadowOpaque = 0
    }

    @Override
    public void render(@NotNull EntityMaskMan man, float yaw, float partialTick,
                       @NotNull PoseStack ps, @NotNull MultiBufferSource buffer, int light) {
        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(MODEL);
        if (obj.isEmpty()) return;

        TextureAtlasSprite body = RBMKColumnRenderer.sprite(RefStrings.MODID, "entity_obj/maskman");
        TextureAtlasSprite iou  = RBMKColumnRenderer.sprite(RefStrings.MODID, "entity_obj/iou");

        // The walk cycle the original derives by hand from limbSwing/limbSwingAmount.
        float swingPhase  = man.walkAnimation.position(partialTick);
        float swingAmount = man.walkAnimation.speed(partialTick) * 0.5F;
        float swing = (float) Math.toDegrees(Mth.cos(swingPhase / 2F + Mth.PI) * 1.4F * swingAmount);

        float bodyYaw = Mth.rotLerp(partialTick, man.yBodyRotO, man.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, man.yHeadRotO, man.yHeadRot) - bodyYaw;

        ps.pushPose();
        // The original's glRotatef(180, 1, 0, 0) is not a model flip - it cancels the
        // scale(-1, -1, 1) that 1.7.10's RendererLivingEntity applies before calling the model.
        // Net effect of that pair is a 180 degree turn about Y. A modern EntityRenderer has no
        // such pre-flip, so copying the X rotation literally stood the boss on his head.
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.translate(0, -1.5F, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-90));
        ps.mulPose(Axis.XP.rotationDegrees(swing * -0.1F));

        part(obj, body, ps, buffer, light, "Torso");

        limb(obj, body, ps, buffer, light, "LLeg", -0.5F, 1.75F, -0.5F, swing);
        limb(obj, body, ps, buffer, light, "RLeg", -0.5F, 1.75F, 0.5F, -swing);
        limb(obj, body, ps, buffer, light, "LArm", -0.5F, 3.75F, -1.5F, swing * 0.25F);
        limb(obj, body, ps, buffer, light, "RArm", -0.5F, 3.75F, 1.5F, -swing * 0.25F);

        ps.pushPose();
        ps.translate(0.5F, 4F, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-headYaw));
        if (!man.isUnmasked()) {
            part(obj, body, ps, buffer, light, "Head");
        } else {
            // Below half health the mask comes off and the note takes its place.
            part(obj, body, ps, buffer, light, "Skull");
            part(obj, iou, ps, buffer, light, "IOU");
        }
        ps.popPose();

        ps.popPose();
        super.render(man, yaw, partialTick, ps, buffer, light);
    }

    private static void limb(Map<String, List<float[]>> obj, TextureAtlasSprite sprite, PoseStack ps,
                             MultiBufferSource buffer, int light,
                             String name, float x, float y, float z, float rotation) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.ZP.rotationDegrees(rotation));
        part(obj, sprite, ps, buffer, light, name);
        ps.popPose();
    }

    private static void part(Map<String, List<float[]>> obj, TextureAtlasSprite sprite, PoseStack ps,
                             MultiBufferSource buffer, int light, String name) {
        List<float[]> mesh = obj.get(name);
        if (mesh == null) return;
        RBMKColumnRenderer.renderObjGroup(buffer.getBuffer(RenderType.solid()), ps.last().pose(),
                mesh, sprite, 1F, 1F, 1F, light, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityMaskMan man) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
