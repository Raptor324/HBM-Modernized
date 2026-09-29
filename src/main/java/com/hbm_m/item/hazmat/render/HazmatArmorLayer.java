package com.hbm_m.item.hazmat.render;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.interfaces.IArmorLayerConfig;
import com.hbm_m.item.hazmat.HazmatArmorItem;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.layer.AbstractObjArmorLayer;
import com.hbm_m.powerarmor.render.ClientPowerArmorRender;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Слой рендера костюма химзащиты на теле. Один экземпляр на окрас
 * ({@link HazmatArmorItem.Variant}); геометрия общая, отличается только
 * текстура. Баллон фильтра ("Filter") рисуется на шлеме только при
 * установленном фильтре - как в оригинальном ModelM65.
 */
public class HazmatArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends AbstractObjArmorLayer<T, M> {

    private final HazmatArmorItem.Variant variant;

    private static final String[] PARTS = {
            "Helmet", "Filter", "Chest", "RightArm", "LeftArm", "RightLeg", "LeftLeg", "RightBoot", "LeftBoot"
    };

    public HazmatArmorLayer(RenderLayerParent<T, M> parent, HazmatArmorItem.Variant variant) {
        super(parent);
        this.variant = variant;
    }

    @Override
    protected IArmorLayerConfig createConfig() {
        // Вызывается из конструктора базового класса ещё до присвоения this.variant -
        // конфиг читает поле лениво, уже после завершения инициализации.
        return new HazmatConfig();
    }

    private class HazmatConfig implements IArmorLayerConfig {

        private Map<String, Material> materials;

        private Map<String, Material> materials() {
            Map<String, Material> m = materials;
            if (m == null) {
                m = new HashMap<>();
                Material mat = new Material(InventoryMenu.BLOCK_ATLAS,
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/armor/" + variant.baseId));
                for (String part : PARTS) {
                    m.put(part, mat);
                }
                materials = m;
            }
            return m;
        }

        @Override
        public String getArmorTypeId() {
            return "hazmat_" + variant.baseId;
        }

        @Override
        public ModelResourceLocation getBakedModelLocation() {
            // По модели на окрас: UV запечённых квадов привязаны к спрайту, которым модель
            // запекалась (JSON "default"), поэтому подмена текстуры в слое цвет не меняет.
            return switch (variant) {
                case YELLOW -> ClientPowerArmorRender.HAZMAT_MODEL_BAKED;
                case RED -> ClientPowerArmorRender.HAZMAT_RED_MODEL_BAKED;
                case GREY -> ClientPowerArmorRender.HAZMAT_GREY_MODEL_BAKED;
            };
        }

        @Override
        public Map<String, Material> getPartMaterials() {
            return materials();
        }

        @Override
        public boolean shouldRenderPart(@NotNull String partName, @NotNull ItemStack stack, EquipmentSlot slot) {
            // Баллон фильтра - только у масок red/grey (в оригинале их модель ModelM65 имеет
            // filter-часть; жёлтый шлем рисуется ванильным слоем без баллона).
            if ("Filter".equals(partName)) {
                return variant != HazmatArmorItem.Variant.YELLOW && IGasMask.hasFilter(stack);
            }
            return true;
        }

        @Override
        public boolean isItemValid(@NotNull ItemStack stack) {
            return stack.getItem() instanceof HazmatArmorItem item && item.variant == variant;
        }
    }
}
