package com.hbm_m.item.gasmask;

import java.util.EnumSet;
import java.util.List;

import com.hbm_m.compat.curios.CuriosCompat;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.item.tools_and_armor.ModArmorMaterialsAccess;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Шлем-противогаз. Защиту лёгких даёт только установленный фильтр ({@link ArmorRegistry}
 * читает классы фильтра минус чёрный список маски). Шифт+ПКМ по маске в руке — выкрутить фильтр.
 * Порт {@link com.hbm.items.armor.ArmorGasMask} (1.7.10).
 */
public class ArmorGasMaskItem extends ArmorItem implements IGasMask, ITooltipProvider {

    public enum Variant {
        GAS_MASK("gas_mask", "textures/models/gas_mask.png", "textures/misc/overlay_gasmask_%d.png"),
        M65("m65", "textures/models/m65.png", "textures/misc/overlay_goggles_%d.png"),
        OLDE("olde", "textures/armor/mask_olde.png", "textures/misc/overlay_goggles_%d.png"),
        MONO("mono", "textures/models/m65_mono.png", "textures/misc/overlay_goggles_%d.png");

        public final String id;
        public final String modelTexture;
        public final String overlayPattern;

        Variant(String id, String modelTexture, String overlayPattern) {
            this.id = id;
            this.modelTexture = modelTexture;
            this.overlayPattern = overlayPattern;
        }
    }

    public final Variant variant;

    public ArmorGasMaskItem(Variant variant, Properties properties) {
        super(ModArmorMaterialsAccess.holder(ModArmorMaterials.GAS_MASK), Type.HELMET, properties);
        this.variant = variant;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    // Взаимная блокировка: нельзя надеть противогаз на голову,
    // пока маска уже стоит в слоте лица Curios (и наоборот — см. GasMaskCurio).
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
        return switch (variant) {
            case MONO -> EnumSet.of(HazardClass.GAS_BLISTERING, HazardClass.GAS_LUNG, HazardClass.BACTERIA);
            default -> EnumSet.of(HazardClass.GAS_BLISTERING);
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Шифт+ПКМ по маске в руке — выкрутить фильтр (в оригинале то же самое).
        if (player.isShiftKeyDown()) {
            ItemStack mask = player.getItemInHand(hand);
            ItemStack filter = GasMaskUtil.takeFilter(mask);
            if (!filter.isEmpty()) {
                if (!level.isClientSide()) {
                    if (!player.getInventory().add(filter)) {
                        player.drop(filter, false);
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.FILTER_SCREW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                return InteractionResultHolder.sidedSuccess(mask, level.isClientSide());
            }
            return InteractionResultHolder.pass(mask);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
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
        tooltip.add(Component.translatable("hazard.neverProtects").withStyle(ChatFormatting.DARK_RED));
        for (HazardClass clazz : getBlacklist()) {
            tooltip.add(Component.literal("  ").append(Component.translatable(clazz.translationKey))
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }
}
