package com.hbm_m.client.render.armor;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import com.hbm_m.client.render.armor.ArmorRenderSpec.Bone;
import com.hbm_m.client.render.armor.ArmorRenderSpec.PartBinding;
import com.hbm_m.client.render.armor.ArmorRenderSpec.RenderMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Single unified 3D entity render layer for ALL OBJ/NMesh power armors and 3D hazmat suits.
 * Immune to {@link net.minecraft.client.resources.model.BakedModel} wrapping mods (such as
 * ItemTransformHelper or ModernFix) because it reads compiled {@code .nmesh} geometry directly
 * through {@link ArmorObjModel} and renders via vanilla {@link RenderType} entity passes (compatible
 * with Vanilla, Embeddium, Oculus, and Iris shader pipelines).
 */
public class UnifiedObjArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private static final float PX = 1.0F / 16.0F;
    private static final int FULLBRIGHT = 0x00F000F0;

    public UnifiedObjArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight,
                       @NotNull T entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.HEAD);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.CHEST);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.LEGS);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.FEET);
    }

    private void renderSlot(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) return;

        ArmorRenderSpec spec = ArmorRenderRegistry.getSpec(stack);
        if (spec == null) return;

        var bindings = spec.getParts(slot);
        if (bindings.isEmpty()) return;

        ArmorObjModel model = ArmorObjModel.get(spec.modelLocation);
        M parentModel = getParentModel();
        boolean young = parentModel.young;

        for (int i = 0; i < bindings.size(); i++) {
            PartBinding binding = bindings.get(i);
            if (!binding.shouldRender(stack)) continue;

            ModelPart bone = resolveBone(parentModel, binding.bone());
            if (!bone.visible) continue;

            renderPart(poseStack, buffer, packedLight, stack, spec, model, binding, bone, young);
        }
    }

    private static <T extends LivingEntity, M extends HumanoidModel<T>> ModelPart resolveBone(M model, Bone bone) {
        return switch (bone) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_LEG -> model.rightLeg;
        };
    }

    private static void renderPart(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                   ItemStack stack, ArmorRenderSpec spec, ArmorObjModel model,
                                   PartBinding binding, ModelPart bone, boolean young) {
        poseStack.pushPose();

        // Match AgeableListModel child proportions when worn by baby zombie/piglin mobs.
        if (young) {
            if (binding.bone() == Bone.HEAD) {
                poseStack.scale(0.75F, 0.75F, 0.75F);
                poseStack.translate(0.0F, 1.0F, 0.0F);
            } else {
                poseStack.scale(0.5F, 0.5F, 0.5F);
                poseStack.translate(0.0F, 1.5F, 0.0F);
            }
        }

        // Translate to current animated bone joint, rotate in Z-Y-X order, then shift back by rest pivot.
        poseStack.translate(bone.x * PX, bone.y * PX, bone.z * PX);
        if (bone.zRot != 0.0F || bone.yRot != 0.0F || bone.xRot != 0.0F) {
            poseStack.mulPose(new Quaternionf().rotationZYX(bone.zRot, bone.yRot, bone.xRot));
        }
        poseStack.translate(-binding.bone().restX * PX, -binding.bone().restY * PX, -binding.bone().restZ * PX);

        if (binding.offsetX() != 0.0F || binding.offsetY() != 0.0F || binding.offsetZ() != 0.0F) {
            poseStack.translate(binding.offsetX(), binding.offsetY(), binding.offsetZ());
        }

        poseStack.scale(PX, PX, PX);

        if (binding.pixelTransform() != null) {
            binding.pixelTransform().accept(poseStack, stack);
        }

        RenderType renderType = switch (binding.mode()) {
            case CUTOUT -> RenderType.armorCutoutNoCull(binding.texture());
            case TRANSLUCENT, GLOW -> RenderType.entityTranslucent(binding.texture());
        };
        int light = binding.mode() == RenderMode.GLOW ? FULLBRIGHT : packedLight;

        VertexConsumer vc = buffer.getBuffer(renderType);
        model.renderPart(binding.partName(), poseStack, vc, light,
                binding.r(), binding.g(), binding.b(), 1.0F, spec.applyMtlTint);

        poseStack.popPose();
    }
}
