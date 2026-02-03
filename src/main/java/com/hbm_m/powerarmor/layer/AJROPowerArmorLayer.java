package com.hbm_m.powerarmor.layer;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.AJROArmor;
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

/**
 * Render layer for AJRO Power Armor (entity rendering).
 * Reuses the AJR OBJ model but with distinct AJRO textures.
 */
@OnlyIn(Dist.CLIENT)
public class AJROPowerArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends AbstractObjArmorLayer<T, M> {

    private static final ResourceLocation AJRO_ATLAS_LOCATION = InventoryMenu.BLOCK_ATLAS;

    public AJROPowerArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    protected IArmorLayerConfig createConfig() {
        return new AJROConfig();
    }

    private static class AJROConfig implements IArmorLayerConfig {
        private static final Map<String, Material> PART_MATERIALS = Map.of(
                "Helmet", withTex("block/armor/ajro_helmet"),
                "Chest", withTex("block/armor/ajro_chest"),
                "RightArm", withTex("block/armor/ajro_arm"),
                "LeftArm", withTex("block/armor/ajro_arm"),
                "RightLeg", withTex("block/armor/ajro_leg"),
                "LeftLeg", withTex("block/armor/ajro_leg"),
                "RightBoot", withTex("block/armor/ajro_leg"),
                "LeftBoot", withTex("block/armor/ajro_leg")
        );

        private static Material withTex(String path) {
            return new Material(AJRO_ATLAS_LOCATION, ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, path));
        }

        @Override
        public String getArmorTypeId() {
            return "ajro";
        }

        @Override
        public ModelResourceLocation getBakedModelLocation() {
            return ClientPowerArmorRender.AJRO_MODEL_BAKED;
        }

        @Override
        public Map<String, Material> getPartMaterials() {
            return PART_MATERIALS;
        }

        @Override
        public boolean isItemValid(@NotNull ItemStack stack) {
            return stack.getItem() instanceof AJROArmor;
        }
    }
}

