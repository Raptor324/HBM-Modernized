package com.hbm_m.client.render;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.armormod.item.ItemModGasmask;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.client.compat.curios.CuriosClientCompat;
import com.hbm_m.client.model.GasMaskModels;
import com.hbm_m.item.gasmask.ArmorGasMaskItem;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.powerarmor.layer.ModModelLayers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Рендер противогаза на голове персонажа — надетая маска или маска-модификация,
 * прицепленная к шлему. Порт getArmorModel/ItemModGasmask.modRender (1.7.10).
 */
public class GasMaskLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    private static ModelPart gasMaskRoot;
    private static ModelPart m65Mask;
    private static ModelPart m65Filter;

    public GasMaskLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    private static void ensureBaked() {
        if (gasMaskRoot != null) {
            return;
        }
        var models = Minecraft.getInstance().getEntityModels();
        ModelPart gasRoot = models.bakeLayer(ModModelLayers.GAS_MASK);
        gasMaskRoot = gasRoot.getChild("mask");
        ModelPart m65Root = models.bakeLayer(ModModelLayers.M65);
        m65Mask = m65Root.getChild("mask");
        m65Filter = m65Root.getChild("filter");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            T entity, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        ensureBaked();
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);

        // 1. Надетая маска-шлем.
        if (head.getItem() instanceof ArmorGasMaskItem mask) {
            renderVariant(entity, mask.variant, IGasMask.hasFilter(head), poseStack, buffer, packedLight);
        }

        // 2. Маска, прицепленная к шлему как модификация (attachment_mask).
        for (ItemStack mod : ArmorModificationHelper.pryMods(head)) {
            if (!mod.isEmpty() && mod.getItem() instanceof ItemModGasmask gasmask) {
                renderM65(entity, gasmask.getModelTexture(), IGasMask.hasFilter(mod),
                        poseStack, buffer, packedLight);
            }
        }

        // 3. Маска в слоте лица Curios (опционально) — поверх шлема.
        ItemStack faceMask = CuriosClientCompat.getFaceMask(entity);
        if (faceMask.getItem() instanceof ArmorGasMaskItem mask) {
            renderVariant(entity, mask.variant, IGasMask.hasFilter(faceMask), poseStack, buffer, packedLight);
        } else if (faceMask.getItem() instanceof ItemModGasmask gasmask) {
            renderM65(entity, gasmask.getModelTexture(), IGasMask.hasFilter(faceMask),
                    poseStack, buffer, packedLight);
        }
    }

    private void renderVariant(T entity, ArmorGasMaskItem.Variant variant, boolean withFilter,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (variant == ArmorGasMaskItem.Variant.GAS_MASK) {
            poseStack.pushPose();
            getParentModel().head.translateAndRotate(poseStack);
            poseStack.scale(1.15F, 1.15F, 1.15F);
            gasMaskRoot.render(poseStack, buffer.getBuffer(
                    RenderType.armorCutoutNoCull(texture(variant.modelTexture))), packedLight, 1);
            poseStack.popPose();
        } else {
            renderM65(entity, variant.modelTexture, withFilter, poseStack, buffer, packedLight);
        }
    }

    private void renderM65(T entity, String texture, boolean withFilter,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        // 1.7.10: scale 18/16, затем ×1.01
        float s = 1.125F * 1.01F;
        poseStack.scale(s, s, s);
        RenderType rt = RenderType.armorCutoutNoCull(texture(texture));
        m65Mask.render(poseStack, buffer.getBuffer(rt), packedLight, 1);
        if (withFilter) {
            m65Filter.render(poseStack, buffer.getBuffer(rt), packedLight, 1);
        }
        poseStack.popPose();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
    }
}
