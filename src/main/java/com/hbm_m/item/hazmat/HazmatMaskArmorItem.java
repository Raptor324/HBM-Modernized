package com.hbm_m.item.hazmat;

import java.util.EnumSet;
import java.util.List;

import com.hbm_m.compat.curios.CuriosCompat;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.item.gasmask.GasMaskUtil;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.item.gasmask.ItemGasMaskFilter;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Шлем-противогаз красного и серого костюмов химзащиты. Фильтр хранится в NBT
 * шлема ({@link IGasMask}), ПКМ фильтром вкручивает его, Шифт+ПКМ по шлему в руке
 * выкручивает. Чёрный список пуст - маска защищает всем, что даёт фильтр (как в оригинале).
 *
 * <p>Порт {@link com.hbm.items.armor.ArmorHazmatMask} (1.7.10).</p>
 */
public class HazmatMaskArmorItem extends HazmatArmorItem implements IGasMask {

    public HazmatMaskArmorItem(ModArmorMaterials material, Properties properties, Variant variant) {
        super(material, Type.HELMET, properties, variant);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    // Взаимная блокировка со слотом лица Curios - как у ArmorGasMaskItem.
    //? if < 1.21.1 {
    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, net.minecraft.world.entity.Entity entity) {
        return super.canEquip(stack, slot, entity)
                && (slot != EquipmentSlot.HEAD || !(entity instanceof LivingEntity living) || CuriosCompat.getFaceMask(living).isEmpty());
    }
    //?} else {
    /*@Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
        return super.canEquip(stack, slot, entity) && (slot != EquipmentSlot.HEAD || CuriosCompat.getFaceMask(entity).isEmpty());
    }
    *///?}

    @Override
    public EnumSet<HazardClass> getBlacklist() {
        return EnumSet.noneOf(HazardClass.class);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Шифт+ПКМ по шлему в руке - выкрутить фильтр (как у ArmorGasMaskItem).
        ItemStack mask = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && IGasMask.hasFilter(mask)) {
            if (!level.isClientSide()) {
                ItemStack filter = GasMaskUtil.takeFilter(mask);
                if (!filter.isEmpty() && !player.getInventory().add(filter)) {
                    player.drop(filter, false);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.FILTER_SCREW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResultHolder.sidedSuccess(mask, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Состояние фильтра - как в оригинальном ArmorUtil.addGasMaskTooltip.
        if (!IGasMask.hasFilter(stack)) {
            tooltip.add(Component.translatable("tooltip.hbm_m.mask.noFilter").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.hbm_m.mask.filter").withStyle(ChatFormatting.GOLD));
            ItemStack filter = new ItemStack(IGasMask.getFilterItem(IGasMask.getFilterId(stack)));
            int dmg = IGasMask.getFilterDamage(stack);
            int max = filter.getItem() instanceof ItemGasMaskFilter f ? f.maxFilterDamage : ItemGasMaskFilter.DEFAULT_MAX_DAMAGE;
            tooltip.add(Component.literal("  ").append(filter.getHoverName())
                    .append(Component.literal(" (" + Math.max(0, (max - dmg) * 100 / max) + "%)"))
                    .withStyle(ChatFormatting.YELLOW));
        }
        super.appendHbmTooltip(stack, level, tooltip, flag);
    }
}
