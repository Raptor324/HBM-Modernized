package com.hbm_m.armormod.item;

import java.util.EnumSet;
import java.util.List;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.item.gasmask.ArmorGasMaskItem;
import com.hbm_m.item.gasmask.GasMaskUtil;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.item.gasmask.ItemGasMaskFilter;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Съёмный противогаз-модификация: прицепляется к шлему (слот helmet_only, стол модификаций)
 * и даёт защиту лёгких через фильтр. ПКМ — вкрутить фильтр в прицепленную маску,
 * Шифт+ПКМ — выкрутить. Порт {@link com.hbm.items.armor.ItemModGasmask} (1.7.10).
 */
public class ItemModGasmask extends ItemArmorMod implements IGasMask {

    private final boolean mono;

    public ItemModGasmask(Properties properties, boolean mono) {
        super(properties.stacksTo(1), ArmorModificationHelper.helmet_only);
        this.mono = mono;
    }

    @Override
    public EnumSet<HazardClass> getBlacklist() {
        return mono
                ? EnumSet.of(HazardClass.GAS_BLISTERING, HazardClass.GAS_LUNG, HazardClass.BACTERIA)
                : EnumSet.of(HazardClass.GAS_BLISTERING);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Маска на голове, прицепленная к шлему или в слоте лица Curios.
        ItemStack mask = GasMaskUtil.resolveWornMask(player);

        if (mask.getItem() instanceof IGasMask) {
            // Маска уже прицеплена к шлему — обрабатываем как обычную маску.
            if (player.isShiftKeyDown()) {
                ItemStack filter = GasMaskUtil.takeFilter(mask);
                if (!filter.isEmpty()) {
                    if (!level.isClientSide()) {
                        if (!player.getInventory().add(filter)) {
                            player.drop(filter, false);
                        }
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                ModSounds.FILTER_SCREW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
                return InteractionResultHolder.pass(stack);
            }

            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof ItemGasMaskFilter && IGasMask.isFilterApplicable(mask, held)) {
                if (!level.isClientSide()) {
                    ItemStack old = GasMaskUtil.takeFilter(mask);
                    IGasMask.installFilter(mask, held.getItem());
                    if (old.isEmpty()) {
                        held.shrink(1);
                    } else {
                        player.setItemInHand(hand, old);
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.FILTER_SCREW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.attachment.gasProtection").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.hbm_m.attachment.slotHelmet").withStyle(ChatFormatting.GRAY));
        if (!IGasMask.hasFilter(stack)) {
            tooltip.add(Component.translatable("tooltip.hbm_m.mask.noFilter").withStyle(ChatFormatting.RED));
        } else {
            ItemStack filter = new ItemStack(IGasMask.getFilterItem(IGasMask.getFilterId(stack)));
            int dmg = IGasMask.getFilterDamage(stack);
            int max = filter.getItem() instanceof ItemGasMaskFilter f ? f.maxFilterDamage : ItemGasMaskFilter.DEFAULT_MAX_DAMAGE;
            tooltip.add(Component.literal("  ").append(filter.getHoverName())
                    .append(Component.literal(" (" + Math.max(0, (max - dmg) * 100 / max) + "%)"))
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    /** Текстура M65-модели для рендера прицепленной маски (см. клиентский GasMaskLayer). */
    public String getModelTexture() {
        return mono ? ArmorGasMaskItem.Variant.MONO.modelTexture : ArmorGasMaskItem.Variant.M65.modelTexture;
    }

    @Override
    public List<Component> getEffectTooltipLines() {
        return List.of(Component.translatable("tooltip.hbm_m.attachment.gasProtection").withStyle(ChatFormatting.GREEN));
    }
}
