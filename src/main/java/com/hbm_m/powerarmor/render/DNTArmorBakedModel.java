package com.hbm_m.powerarmor.render;

import java.util.Map;

import com.hbm_m.powerarmor.DNTArmor;
import com.hbm_m.powerarmor.ModPowerArmorItem;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Baked model for rendering DNT power armor in GUI and hand.
 * Uses the same multipart baked model infrastructure as T51/AJR/Bismuth.
 */
@OnlyIn(Dist.CLIENT)
public class DNTArmorBakedModel extends AbstractArmorBakedModel {

    private static final String[] DNT_ORDER = {
            "Helmet", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };

    private static final DNTModelConfig CONFIG = new DNTModelConfig();

    public DNTArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms, CONFIG);
    }

    @Override
    public DNTArmorBakedModel withTransforms(ItemTransforms newTransforms) {
        return new DNTArmorBakedModel(this.parts, newTransforms);
    }

    private static class DNTModelConfig implements IArmorModelConfig {
        @Override
        public String getArmorSetId() {
            return "dnt";
        }

        @Override
        public String[] getPartOrder() {
            return DNT_ORDER;
        }

        @Override
        public String[] getPartsForType(ArmorItem.Type armorType) {
            if (armorType == null) {
                return DNT_ORDER;
            }

            return switch (armorType) {
                case HELMET -> new String[]{"Helmet"};
                case CHESTPLATE -> new String[]{"Chest", "RightArm", "LeftArm"};
                case LEGGINGS -> new String[]{"RightLeg", "LeftLeg"};
                case BOOTS -> new String[]{"RightBoot", "LeftBoot"};
            };
        }

        @Override
        public Class<? extends ModPowerArmorItem> getArmorItemClass() {
            return DNTArmor.class;
        }

        @Override
        public ModelResourceLocation getBaseModelLocation() {
            return ClientPowerArmorRender.DNT_MODEL_BAKED;
        }

        @Override
        public boolean isItemValid(net.minecraft.world.item.ItemStack stack) {
            return stack.getItem() instanceof DNTArmor;
        }
    }
}

