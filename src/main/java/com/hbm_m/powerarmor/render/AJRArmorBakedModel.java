package com.hbm_m.powerarmor.render;

import java.util.Map;

import com.hbm_m.powerarmor.AJRArmor;
import com.hbm_m.powerarmor.AJROArmor;
import com.hbm_m.powerarmor.ModPowerArmorItem;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Baked model for rendering AJR armor in GUI/hand.
 * Uses the shared multipart baked model infrastructure (same as T51).
 */
@OnlyIn(Dist.CLIENT)
public class AJRArmorBakedModel extends AbstractArmorBakedModel {

    private static final String[] AJR_ORDER = {
            "Helmet", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };

    private static final AJRModelConfig CONFIG = new AJRModelConfig();

    public AJRArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms, CONFIG);
    }

    @Override
    public AJRArmorBakedModel withTransforms(ItemTransforms newTransforms) {
        return new AJRArmorBakedModel(this.parts, newTransforms);
    }

    private static class AJRModelConfig implements IArmorModelConfig {
        @Override
        public String getArmorSetId() {
            return "ajr";
        }

        @Override
        public String[] getPartOrder() {
            return AJR_ORDER;
        }

        @Override
        public String[] getPartsForType(ArmorItem.Type armorType) {
            if (armorType == null) return AJR_ORDER;

            return switch (armorType) {
                case HELMET -> new String[]{"Helmet"};
                case CHESTPLATE -> new String[]{"Chest", "RightArm", "LeftArm"};
                case LEGGINGS -> new String[]{"RightLeg", "LeftLeg"};
                case BOOTS -> new String[]{"RightBoot", "LeftBoot"};
            };
        }

        @Override
        public Class<? extends ModPowerArmorItem> getArmorItemClass() {
            return AJRArmor.class;
        }

        @Override
        public ModelResourceLocation getBaseModelLocation() {
            return ClientPowerArmorRender.AJR_MODEL_BAKED;
        }

        @Override
        public boolean isItemValid(net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof AJRArmor
                || stack.getItem() instanceof AJROArmor;
        }
    }
}

