package com.hbm_m.client.render;

import com.hbm_m.client.model.AbstractMultipartBakedModel;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.phys.Vec3;

/**
 * Абстрактный базовый класс для рендеринга OBJ-брони.
 * Содержит всю общую логику рендеринга, оставляя подклассам только конфигурацию.
 * 
 * @param <T> Тип сущности (LivingEntity)
 * @param <M> Тип модели (HumanoidModel)
 */
public abstract class AbstractObjArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    protected static final float PIXEL_SCALE = 1.0F / 16.0F;
    protected static final float DEFAULT_Z_FIGHTING_SCALE = 1.015F; // 1.5% increase

    protected final IArmorLayerConfig config;
    // Кэш BASE_PIVOTS с составным ключом: armorTypeId:partName
    private final Map<String, Vec3> basePivots = new ConcurrentHashMap<>();

    public AbstractObjArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
        this.config = createConfig();
    }

    /**
     * Создает конфигурацию для данного типа брони.
     * Должен быть реализован в подклассе.
     */
    protected abstract IArmorLayerConfig createConfig();

    @Override
    public final void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight,
                           @NotNull T entity, float limbSwing, float limbSwingAmount, float partialTick,
                           float ageInTicks, float netHeadYaw, float headPitch) {
        // Рендерим поверх игрока только если на слоте наш тип брони
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.HEAD);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.CHEST);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.LEGS);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.FEET);
    }

    protected final void renderSlot(PoseStack poseStack, MultiBufferSource buffer, int light, T entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (!config.isItemValid(stack)) return;

        BakedModel baked = Minecraft.getInstance().getModelManager().getModel(config.getBakedModelLocation());
        if (baked == Minecraft.getInstance().getModelManager().getMissingModel()) {
            if (entity.tickCount % 100 == 0) {
                MainRegistry.LOGGER.warn("{} Model is MISSING (returned missing model)", config.getArmorTypeId());
            }
            return;
        }

        if (!(baked instanceof AbstractMultipartBakedModel multipart)) {
            if (entity.tickCount % 100 == 0) {
                MainRegistry.LOGGER.warn("{} Model is NOT instance of AbstractMultipartBakedModel. It is: {}",
                        config.getArmorTypeId(), baked.getClass().getName());
            }
            return;
        }

        M parentModel = this.getParentModel();
        final boolean crouching = entity.isCrouching();

        switch (slot) {
            case HEAD -> {
                BakedModel part = multipart.getPart("Helmet");
                if (part == null && entity.tickCount % 100 == 0) {
                    MainRegistry.LOGGER.error("Part 'Helmet' is NULL for {}", config.getArmorTypeId());
                }
                renderPart(poseStack, buffer, light, part, parentModel.head, "Helmet", crouching);
            }
            case CHEST -> {
                renderPart(poseStack, buffer, light, multipart.getPart("Chest"), parentModel.body, "Chest", crouching);
                renderPart(poseStack, buffer, light, multipart.getPart("RightArm"), parentModel.rightArm, "RightArm", crouching);
                renderPart(poseStack, buffer, light, multipart.getPart("LeftArm"), parentModel.leftArm, "LeftArm", crouching);
            }
            case LEGS -> {
                renderPart(poseStack, buffer, light, multipart.getPart("RightLeg"), parentModel.rightLeg, "RightLeg", crouching);
                renderPart(poseStack, buffer, light, multipart.getPart("LeftLeg"), parentModel.leftLeg, "LeftLeg", crouching);
            }
            case FEET -> {
                renderPart(poseStack, buffer, light, multipart.getPart("RightBoot"), parentModel.rightLeg, "RightBoot", crouching);
                renderPart(poseStack, buffer, light, multipart.getPart("LeftBoot"), parentModel.leftLeg, "LeftBoot", crouching);
            }
            default -> {
                // MAINHAND/OFFHAND are irrelevant here.
            }
        }
    }

    protected final void renderPart(PoseStack poseStack, MultiBufferSource buffer, int light, BakedModel partModel, ModelPart bone, String partName, boolean crouching) {
        if (partModel == null) return;
        Material mat = config.getPartMaterials().get(partName);
        if (mat == null) return;

        final VertexConsumer vc;
        try {
            // IMPORTANT: Forge OBJ baking gives us atlas-space UVs already,
            // but Material.buffer() wraps the consumer with SpriteCoordinateExpander which would
            // re-map UVs again -> "one pixel" look / fully transparent legs/boots.
            // So we bind the atlas RenderType directly and DO NOT wrap the consumer.
            vc = buffer.getBuffer(RenderType.entityCutoutNoCull(mat.atlasLocation()));
        } catch (Exception e) {
            MainRegistry.LOGGER.error("{}: failed to get VertexConsumer for part={} atlas={} tex={}",
                    config.getArmorTypeId(), partName, mat.atlasLocation(), mat.texture(), e);
            return;
        }

        Vec3 basePivot = getBasePivot(partName, bone, crouching);

        poseStack.pushPose();

        // FIXED: OBJ geometry is in global pixel coords, but when crouching, Minecraft moves the pivot.
        // We need to apply the CURRENT pivot offset for positioning (to follow the limb),
        // but rotate around the BASE pivot (to maintain correct rotation center).
        //
        // Transform: translate to current pivot -> translate to base pivot -> rotate -> translate back to base -> translate back to current
        // Simplified: translate (current - base) -> translate to base -> rotate -> translate back
        double pivotDeltaX = (bone.x - basePivot.x) * PIXEL_SCALE;
        double pivotDeltaY = (bone.y - basePivot.y) * PIXEL_SCALE;
        double pivotDeltaZ = (bone.z - basePivot.z) * PIXEL_SCALE;

        // Apply current pivot offset for positioning
        poseStack.translate(pivotDeltaX, pivotDeltaY, pivotDeltaZ);

        // Rotate around base pivot
        poseStack.translate(basePivot.x * PIXEL_SCALE, basePivot.y * PIXEL_SCALE, basePivot.z * PIXEL_SCALE);
        if (bone.xRot != 0.0F || bone.yRot != 0.0F || bone.zRot != 0.0F) {
            poseStack.mulPose(new org.joml.Quaternionf().rotationZYX(bone.zRot, bone.yRot, bone.xRot));
        }
        poseStack.translate(-basePivot.x * PIXEL_SCALE, -basePivot.y * PIXEL_SCALE, -basePivot.z * PIXEL_SCALE);

        // Лёгкие поправки (в блок-юнитах) для центрирования частей, как rotationPoint сдвиги в 1.7.10
        Vec3 off = config.getPartOffsets().get(partName);
        if (off != null) {
            poseStack.translate(off.x, off.y, off.z);
        }

        // Масштабируем OBJ-пиксели к юнитам Minecraft
        poseStack.scale(PIXEL_SCALE, PIXEL_SCALE, PIXEL_SCALE);
        // Применяем небольшое увеличение масштаба для устранения z-fighting со скином игрока
        float zFightingScale = config.getZFightingScale();
        poseStack.scale(zFightingScale, zFightingScale, zFightingScale);

        RandomSource rand = RandomSource.create(42);
        emitAllQuads(poseStack, vc, partModel, rand, light);

        poseStack.popPose();
    }

    /**
     * Получает базовый (стоячий) pivot для части брони.
     * Кэширует результат с составным ключом armorTypeId:partName.
     * Для Helmet всегда возвращает Vec3.ZERO (фиксированный pivot).
     */
    protected Vec3 getBasePivot(String partName, ModelPart bone, boolean crouching) {
        String cacheKey = config.getArmorTypeId() + ":" + partName;

        // Cache base (standing) pivot when not crouching - this is the rotation center for OBJ geometry
        // For Helmet, use fixed (0,0,0) as base pivot since head animations can change the pivot even when standing
        Vec3 basePivot = basePivots.computeIfAbsent(cacheKey, k -> {
            if ("Helmet".equals(partName)) {
                return Vec3.ZERO; // Fixed base pivot for helmet
            }
            if (!crouching) {
                return new Vec3(bone.x, bone.y, bone.z);
            }
            // If we're crouching but haven't cached base yet, use current as fallback (will update on next stand)
            return new Vec3(bone.x, bone.y, bone.z);
        });

        // Update base pivot when standing (in case it changed), but NOT for Helmet (fixed at 0,0,0)
        if (!crouching && !"Helmet".equals(partName)) {
            basePivots.put(cacheKey, new Vec3(bone.x, bone.y, bone.z));
            basePivot = new Vec3(bone.x, bone.y, bone.z);
        } else if (!crouching && "Helmet".equals(partName)) {
            // For Helmet, always use (0,0,0) as base pivot
            basePivot = Vec3.ZERO;
        }

        return basePivot;
    }

    private static void emitAllQuads(PoseStack poseStack, VertexConsumer vc, BakedModel model, RandomSource rand, int light) {
        PoseStack.Pose pose = poseStack.last();

        for (var dir : net.minecraft.core.Direction.values()) {
            List<net.minecraft.client.renderer.block.model.BakedQuad> quads = model.getQuads(null, dir, rand, ModelData.EMPTY, null);
            for (var q : quads) {
                vc.putBulkData(pose, q, 1f, 1f, 1f, 1f, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, false);
            }
        }

        List<net.minecraft.client.renderer.block.model.BakedQuad> general = model.getQuads(null, null, rand, ModelData.EMPTY, null);
        for (var q : general) {
            vc.putBulkData(pose, q, 1f, 1f, 1f, 1f, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, false);
        }
    }
}

