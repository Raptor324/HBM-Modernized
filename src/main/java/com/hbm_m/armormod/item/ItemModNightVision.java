package com.hbm_m.armormod.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemModNightVision}: Nachtsicht bei voller Energie-Ruestung mit aktivem HUD. */
public class ItemModNightVision extends ItemArmorMod {

    private static final String NIGHT_VISION_ACTIVE_NBT_KEY = "ITEM_MOD_NV_ACTIVE";

    public ItemModNightVision() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.helmet_only, true, false, false, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(Component.translatable("item.night_vision.description.item").withStyle(ChatFormatting.AQUA));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(Component.translatable("item.night_vision.description.in_armor", stack.getHoverName()).withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide && entity instanceof Player player && armor.getItem() instanceof com.hbm_m.powerarmor.ModArmorFSBPowered && com.hbm_m.powerarmor.ModArmorFSB.hasFSBArmor(player)) {
            if (HbmPlayerProps.getData(player).enableHUD) {
                // 15 seconds to make less flickering if the client lags
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 15 * 20, 0));
                if (!armor.getOrCreateTag().contains(NIGHT_VISION_ACTIVE_NBT_KEY)) {
                    armor.getOrCreateTag().putBoolean(NIGHT_VISION_ACTIVE_NBT_KEY, true); // Value does not matter, it's just a flag
                }
                if (entity.getRandom().nextInt(200) == 0) {
                    armor.hurtAndBreak(1, entity, e -> {});
                }
            } else if (armor.hasTag() && armor.getTag().contains(NIGHT_VISION_ACTIVE_NBT_KEY)) { // Disable night vision if it was the armor mod that applied it to avoid removing other night vision sources.
                entity.removeEffect(MobEffects.NIGHT_VISION);
                armor.getTag().remove(NIGHT_VISION_ACTIVE_NBT_KEY);
            }
        }
    }
}
