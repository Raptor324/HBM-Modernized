package com.hbm_m.item.hazmat;

import java.util.List;

import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.handler.HazmatRegistry;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.tools_and_armor.ModArmorMaterialsAccess;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Костюм химзащиты (жёлтый / красный / серый). На теле рисуется OBJ-костюм
 * ({@code HazmatArmorLayer}); ванильный слой погашен прозрачными текстурами
 * материала. Защита от радиации — градиентная, через {@link HazmatRegistry};
 * от газов — по классам опасностей через {@link ArmorRegistry}.
 *
 * <p>Порт {@link com.hbm.items.armor.ArmorHazmat} (1.7.10).</p>
 */
public class HazmatArmorItem extends ArmorItem implements ITooltipProvider {

    public enum Variant {
        YELLOW("hazmat"),
        RED("hazmat_red"),
        GREY("hazmat_grey");

        /** База id модели сета (hbm_m:<baseId>_armor) и текстуры block/armor/<baseId>. */
        public final String baseId;

        Variant(String baseId) {
            this.baseId = baseId;
        }
    }

    public final Variant variant;

    public HazmatArmorItem(ModArmorMaterials material, Type type, Properties properties, Variant variant) {
        super(ModArmorMaterialsAccess.holder(material), type, properties);
        this.variant = variant;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Классы опасностей - как в оригинальном глобальном хуке: список по Shift.
        java.util.Set<HazardClass> protection = ArmorRegistry.getProtection(stack.getItem());
        if (!protection.isEmpty()) {
            if (Screen.hasShiftDown()) {
                tooltip.add(Component.translatable("hazard.prot").withStyle(ChatFormatting.GREEN));
                for (HazardClass clazz : protection) {
                    tooltip.add(Component.literal("  ")
                            .append(Component.translatable(clazz.translationKey))
                            .withStyle(ChatFormatting.YELLOW));
                }
            } else {
                tooltip.add(Component.translatable("tooltip.hbm_m.hold_shift_for_details")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }

        // Оригинальный глобальный хук печатает коэффициент сопротивления для любой вещи из HazmatRegistry.
        double resistance = HazmatRegistry.getResistance(stack);
        if (resistance > 0) {
            tooltip.add(Component.translatable("trait.hbm_m.rad_resistance", resistance)
                    .withStyle(ChatFormatting.YELLOW));
        }

        tooltip.add(Component.translatable("tooltip.hbm_m.hazmat.mudco")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
