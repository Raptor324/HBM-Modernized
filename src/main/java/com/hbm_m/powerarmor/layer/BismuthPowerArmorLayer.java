package com.hbm_m.powerarmor.layer;


import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.interfaces.IArmorLayerConfig;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.BismuthArmor;
import com.hbm_m.powerarmor.render.ClientPowerArmorRender;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

/**
 * Render layer для Bismuth Power Armor.
 * Полностью повторяет рабочий пайплайн T51/AJR: entity-рендер идёт через OBJ multipart baked model.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public class BismuthPowerArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends AbstractObjArmorLayer<T, M> {

    private static final ResourceLocation BISMUTH_ATLAS_LOCATION = InventoryMenu.BLOCK_ATLAS;

    public BismuthPowerArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    protected IArmorLayerConfig createConfig() {
        return new BismuthConfig();
    }

    private static class BismuthConfig implements IArmorLayerConfig {
        private static final Material BISMUTH_SINGLE_ATLAS = withTex("block/armor/bismuth");

        private static final Map<String, Material> PART_MATERIALS = Map.of(
                "Helmet", BISMUTH_SINGLE_ATLAS,
                "Chest", BISMUTH_SINGLE_ATLAS,
                "RightArm", BISMUTH_SINGLE_ATLAS,
                "LeftArm", BISMUTH_SINGLE_ATLAS,
                "RightLeg", BISMUTH_SINGLE_ATLAS,
                "LeftLeg", BISMUTH_SINGLE_ATLAS,
                "RightBoot", BISMUTH_SINGLE_ATLAS,
                "LeftBoot", BISMUTH_SINGLE_ATLAS
        );

        private static Material withTex(String path) {
            //? if fabric && < 1.21.1 {
            /*return new Material(BISMUTH_ATLAS_LOCATION, new ResourceLocation(MainRegistry.MOD_ID, path));
            *///?} else {
                        return new Material(BISMUTH_ATLAS_LOCATION, ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, path));
            //?}

        }

        @Override
        public String getArmorTypeId() {
            return "bismuth";
        }

        @Override
        public ModelResourceLocation getBakedModelLocation() {
            return ClientPowerArmorRender.BISMUTH_MODEL_BAKED;
        }

        @Override
        public Map<String, Material> getPartMaterials() {
            return PART_MATERIALS;
        }

        @Override
        public boolean isItemValid(@NotNull ItemStack stack) {
            return stack.getItem() instanceof BismuthArmor;
        }
    }
}

