package com.hbm_m.powerarmor.layer;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.T51Armor;
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
 * Render layer для T51 Power Armor.
 * Использует абстрактный базовый класс для общей логики рендеринга OBJ-брони.
 */
@OnlyIn(Dist.CLIENT)
public class T51PowerArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends AbstractObjArmorLayer<T, M> {

    /**
     * Атлас для текстур T51 брони.
     * Используется стандартный BLOCK_ATLAS.
     */
    private static final ResourceLocation T51_ATLAS_LOCATION = InventoryMenu.BLOCK_ATLAS;

    public T51PowerArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    protected IArmorLayerConfig createConfig() {
        return new T51Config();
    }

    /**
     * Конфигурация для T51 Power Armor.
     */
    private static class T51Config implements IArmorLayerConfig {
        private static final Map<String, Material> PART_MATERIALS = Map.of(
            "Helmet", withTex("block/armor/t51_helmet"),
            "Chest", withTex("block/armor/t51_chest"),
            "RightArm", withTex("block/armor/t51_arm"),
            "LeftArm", withTex("block/armor/t51_arm"),
            "RightLeg", withTex("block/armor/t51_leg"),
            "LeftLeg", withTex("block/armor/t51_leg"),
            "RightBoot", withTex("block/armor/t51_leg"),
            "LeftBoot", withTex("block/armor/t51_leg")
        );

        private static Material withTex(String path) {
            return new Material(T51_ATLAS_LOCATION, ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, path));
        }

        @Override
        public String getArmorTypeId() {
            return "t51";
        }

        @Override
        public ModelResourceLocation getBakedModelLocation() {
            return ClientPowerArmorRender.T51_MODEL_BAKED;
        }

        @Override
        public Map<String, Material> getPartMaterials() {
            return PART_MATERIALS;
        }

        @Override
        public boolean isItemValid(@NotNull ItemStack stack) {
            return stack.getItem() instanceof T51Armor;
        }
    }
}
