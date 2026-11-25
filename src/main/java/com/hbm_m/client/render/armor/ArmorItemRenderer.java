package com.hbm_m.client.render.armor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.client.render.armor.ArmorRenderSpec.PartBinding;
import com.hbm_m.client.render.armor.ArmorRenderSpec.RenderMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Unified {@link BlockEntityWithoutLevelRenderer} for ALL 3D power armor and 3D hazmat items
 * in inventory slots, ground drops, item frames, and first/third-person hands.
 *
 * <p>Normalizes each slot's sub-mesh bounding box automatically so helmets, chestplates, leggings,
 * and boots across all 18 armor sets render upright, centered, and cleanly proportioned without
 * hand-tuned per-item JSON transform matrices.</p>
 */
public class ArmorItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final ArmorItemRenderer INSTANCE = new ArmorItemRenderer();

    private static final int FULLBRIGHT = 0x00F000F0;

    private record ItemLayout(float centerX, float centerY, float centerZ, float fitScale) {}

    private static final Map<Item, ItemLayout> LAYOUT_CACHE = new ConcurrentHashMap<>();

    private ArmorItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void clearLayoutCache() {
        LAYOUT_CACHE.clear();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) return;

        ArmorRenderSpec spec = ArmorRenderRegistry.getSpec(armorItem);
        if (spec == null) return;

        EquipmentSlot slot = armorItem.getEquipmentSlot();
        List<PartBinding> bindings = spec.getParts(slot);
        if (bindings.isEmpty()) return;

        ArmorObjModel model = ArmorObjModel.get(spec.modelLocation);
        ItemLayout layout = LAYOUT_CACHE.computeIfAbsent(armorItem, k -> computeLayout(model, bindings, stack, slot));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyDisplayContextTransform(poseStack, displayContext, slot);

        // Entity pixel coordinates have +Y pointing DOWN and -Z facing FRONT.
        // Rotating 180 deg around X (det = +1) makes +Y point UP and +Z face the viewer while keeping winding intact.
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(layout.fitScale(), layout.fitScale(), layout.fitScale());
        poseStack.translate(-layout.centerX(), -layout.centerY(), -layout.centerZ());

        for (int i = 0; i < bindings.size(); i++) {
            PartBinding binding = bindings.get(i);
            if (!binding.shouldRender(stack)) continue;

            poseStack.pushPose();
            if (binding.offsetX() != 0.0F || binding.offsetY() != 0.0F || binding.offsetZ() != 0.0F) {
                poseStack.translate(binding.offsetX() * 16.0F, binding.offsetY() * 16.0F, binding.offsetZ() * 16.0F);
            }
            if (binding.pixelTransform() != null) {
                binding.pixelTransform().accept(poseStack, stack);
            }

            RenderType renderType = switch (binding.mode()) {
                case CUTOUT -> RenderType.entityCutoutNoCull(binding.texture());
                case TRANSLUCENT, GLOW -> RenderType.entityTranslucent(binding.texture());
            };
            int light = binding.mode() == RenderMode.GLOW ? FULLBRIGHT : packedLight;

            VertexConsumer vc = buffer.getBuffer(renderType);
            model.renderPart(binding.partName(), poseStack, vc, light,
                    binding.r(), binding.g(), binding.b(), 1.0F, spec.applyMtlTint);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void applyDisplayContextTransform(PoseStack poseStack, ItemDisplayContext ctx, EquipmentSlot slot) {
        switch (ctx) {
            case GUI -> {
                float guiScale = switch (slot) {
                    case CHEST -> 1.04F;
                    case LEGS -> 1.00F;
                    case FEET -> 0.96F;
                    default -> 0.98F;
                };
                poseStack.scale(guiScale, guiScale, guiScale);
                poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-32.0F));
            }
            case FIXED -> poseStack.scale(0.95F, 0.95F, 0.95F);
            case GROUND -> {
                poseStack.translate(0.0F, 0.05F, 0.0F);
                poseStack.scale(0.55F, 0.55F, 0.55F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                boolean left = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                poseStack.translate(0.0F, 0.08F, 0.05F);
                poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(left ? -140.0F : 140.0F));
                poseStack.scale(0.45F, 0.45F, 0.45F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                boolean left = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
                poseStack.translate(left ? -0.05F : 0.05F, 0.06F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(left ? -135.0F : 135.0F));
                poseStack.scale(0.52F, 0.52F, 0.52F);
            }
            case HEAD -> poseStack.scale(0.90F, 0.90F, 0.90F);
            default -> { }
        }
    }

    private static ItemLayout computeLayout(ArmorObjModel model, List<PartBinding> bindings,
                                            ItemStack stack, EquipmentSlot slot) {
        List<String> partNames = new ArrayList<>(bindings.size());
        for (PartBinding b : bindings) {
            if (b.includeInStableBounds() && b.shouldRender(stack)) {
                partNames.add(b.partName());
            }
        }
        float[] bounds = model.getBounds(partNames);
        if (bounds == null) {
            return new ItemLayout(0.0F, 0.0F, 0.0F, 1.0F / 16.0F);
        }
        float cx = 0.5F * (bounds[0] + bounds[3]);
        float cy = 0.5F * (bounds[1] + bounds[4]);
        float cz = 0.5F * (bounds[2] + bounds[5]);
        float dx = bounds[3] - bounds[0];
        float dy = bounds[4] - bounds[1];
        float dz = bounds[5] - bounds[2];
        float maxDim = Math.max(Math.max(dx, dy), Math.max(dz, 0.1F));
        float targetExtent = switch (slot) {
            case CHEST -> 0.86F;
            case HEAD -> 0.76F;
            case LEGS -> 0.80F;
            case FEET -> 0.72F;
            default -> 0.78F;
        };
        return new ItemLayout(cx, cy, cz, targetExtent / maxDim);
    }
}
