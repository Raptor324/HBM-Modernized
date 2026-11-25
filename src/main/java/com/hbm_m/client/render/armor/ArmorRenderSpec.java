package com.hbm_m.client.render.armor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Declarative specification of a 3D OBJ/NMesh armor set used by both {@link UnifiedObjArmorLayer}
 * (worn on players, armor stands, and humanoid mobs) and {@link ArmorItemRenderer} (inventory/hand/drops).
 */
public final class ArmorRenderSpec {

    /**
     * Humanoid bone pivot coordinates in Steve pixel units (16px = 1 block).
     * Rest-pose coordinates match 1.7.10 {@code ModelRendererObj} / Minecraft {@code HumanoidModel}.
     */
    public enum Bone {
        HEAD(0.0F, 0.0F, 0.0F),
        BODY(0.0F, 0.0F, 0.0F),
        LEFT_ARM(5.0F, 2.0F, 0.0F),
        RIGHT_ARM(-5.0F, 2.0F, 0.0F),
        LEFT_LEG(1.9F, 12.0F, 0.0F),
        RIGHT_LEG(-1.9F, 12.0F, 0.0F);

        public final float restX;
        public final float restY;
        public final float restZ;

        Bone(float restX, float restY, float restZ) {
            this.restX = restX;
            this.restY = restY;
            this.restZ = restZ;
        }
    }

    public enum RenderMode {
        CUTOUT,
        TRANSLUCENT,
        GLOW
    }

    public record PartBinding(
            String partName,
            Bone bone,
            ResourceLocation texture,
            RenderMode mode,
            float r,
            float g,
            float b,
            float offsetX,
            float offsetY,
            float offsetZ,
            @Nullable Predicate<ItemStack> condition,
            boolean includeInStableBounds,
            @Nullable BiConsumer<PoseStack, ItemStack> pixelTransform
    ) {
        public boolean shouldRender(ItemStack stack) {
            return condition == null || condition.test(stack);
        }
    }

    public final ResourceLocation modelLocation;
    public final boolean applyMtlTint;
    private final Map<EquipmentSlot, List<PartBinding>> slots;

    private ArmorRenderSpec(ResourceLocation modelLocation, boolean applyMtlTint,
                            Map<EquipmentSlot, List<PartBinding>> slots) {
        this.modelLocation = modelLocation;
        this.applyMtlTint = applyMtlTint;
        this.slots = slots;
    }

    public List<PartBinding> getParts(EquipmentSlot slot) {
        return slots.getOrDefault(slot, List.of());
    }

    public static Builder builder(String modelPath) {
        return new Builder(ResourceLocation.tryParse(modelPath));
    }

    public static final class Builder {
        private final ResourceLocation modelLocation;
        private boolean applyMtlTint = false;
        private final Map<EquipmentSlot, List<PartBinding>> slots = new EnumMap<>(EquipmentSlot.class);

        private Builder(ResourceLocation modelLocation) {
            this.modelLocation = modelLocation;
        }

        public Builder applyMtlTint(boolean apply) {
            this.applyMtlTint = apply;
            return this;
        }

        public Builder add(EquipmentSlot slot, String part, Bone bone, ResourceLocation texture) {
            return add(slot, part, bone, texture, RenderMode.CUTOUT, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, null, true, null);
        }

        public Builder add(EquipmentSlot slot, String part, Bone bone, ResourceLocation texture, RenderMode mode) {
            return add(slot, part, bone, texture, mode, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, null, true, null);
        }

        public Builder add(EquipmentSlot slot, String part, Bone bone, ResourceLocation texture, RenderMode mode,
                           float r, float g, float b) {
            return add(slot, part, bone, texture, mode, r, g, b, 0.0F, 0.0F, 0.0F, null, true, null);
        }

        public Builder addOffset(EquipmentSlot slot, String part, Bone bone, ResourceLocation texture,
                                 float offsetX, float offsetY, float offsetZ) {
            return add(slot, part, bone, texture, RenderMode.CUTOUT, 1.0F, 1.0F, 1.0F, offsetX, offsetY, offsetZ, null, true, null);
        }

        public Builder addConditional(EquipmentSlot slot, String part, Bone bone, ResourceLocation texture,
                                      Predicate<ItemStack> condition, boolean includeInStableBounds) {
            return add(slot, part, bone, texture, RenderMode.CUTOUT, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F,
                    condition, includeInStableBounds, null);
        }

        public Builder addAnimated(EquipmentSlot slot, String part, Bone bone, ResourceLocation texture,
                                   BiConsumer<PoseStack, ItemStack> pixelTransform) {
            return add(slot, part, bone, texture, RenderMode.CUTOUT, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F,
                    null, true, pixelTransform);
        }

        public Builder add(EquipmentSlot slot, String part, Bone bone, ResourceLocation texture, RenderMode mode,
                           float r, float g, float b, float offsetX, float offsetY, float offsetZ,
                           @Nullable Predicate<ItemStack> condition, boolean includeInStableBounds,
                           @Nullable BiConsumer<PoseStack, ItemStack> pixelTransform) {
            slots.computeIfAbsent(slot, s -> new ArrayList<>())
                    .add(new PartBinding(part, bone, texture, mode, r, g, b, offsetX, offsetY, offsetZ,
                            condition, includeInStableBounds, pixelTransform));
            return this;
        }

        public ArmorRenderSpec build() {
            Map<EquipmentSlot, List<PartBinding>> frozen = new EnumMap<>(EquipmentSlot.class);
            for (var e : slots.entrySet()) {
                frozen.put(e.getKey(), List.copyOf(e.getValue()));
            }
            return new ArmorRenderSpec(modelLocation, applyMtlTint, frozen);
        }
    }
}
