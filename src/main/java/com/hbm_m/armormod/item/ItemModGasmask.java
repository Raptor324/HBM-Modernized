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
 * 1:1 {@code com.hbm.items.armor.ItemModGasmask}: Gasmaske als Helm-Mod. Shift+Rechtsklick nimmt den
 * Filter aus der gehaltenen Maske; Filter werden wie bei normalen Masken eingesetzt.
 */
public class ItemModGasmask extends ItemArmorMod implements IGasMask {

    private final boolean mono;

    public ItemModGasmask(Properties properties, boolean mono) {
        super(properties.stacksTo(1), ArmorModificationHelper.helmet_only, true, false, false, false);
        this.mono = mono;
    }

    @Override
    public EnumSet<HazardClass> getBlacklist() {
        return mono
                ? EnumSet.of(HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA)
                : EnumSet.of(HazardClass.GAS_BLISTERING);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            ItemStack filter = GasMaskUtil.takeFilter(stack);

            if (!filter.isEmpty()) {
                if (!player.getInventory().add(filter)) {
                    player.drop(filter, true);
                }
            }
        }

        return super.use(level, player, hand);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(Component.literal("Gas protection").withStyle(ChatFormatting.GREEN));
        list.add(Component.empty());
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHbmTooltip(stack, level, tooltip, flag);
        addGasMaskTooltip(stack, tooltip);

        EnumSet<HazardClass> haz = getBlacklist();
        if (!haz.isEmpty()) {
            tooltip.add(Component.literal("Will never protect against:").withStyle(ChatFormatting.RED));
            for (HazardClass clazz : haz) {
                tooltip.add(Component.literal(" -").append(Component.translatable(clazz.translationKey)).withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.GREEN, stack, " (gas protection)"));
        addGasMaskTooltip(stack, list);
    }

    /** Original {@code ArmorUtil.addGasMaskTooltip}. */
    public static void addGasMaskTooltip(ItemStack mask, List<Component> list) {
        if (!(mask.getItem() instanceof IGasMask)) return;

        if (!IGasMask.hasFilter(mask)) {
            list.add(Component.literal("No filter installed!").withStyle(ChatFormatting.RED));
            return;
        }

        list.add(Component.literal("Installed filter:").withStyle(ChatFormatting.GOLD));

        ItemStack filter = new ItemStack(IGasMask.getFilterItem(IGasMask.getFilterId(mask)));
        int meta = IGasMask.getFilterDamage(mask);
        int max = filter.getItem() instanceof ItemGasMaskFilter f ? f.maxFilterDamage : ItemGasMaskFilter.DEFAULT_MAX_DAMAGE;

        String append = "";
        if (max > 0) {
            append = " (" + ((max - meta) * 100 / max) + "%)";
        }

        list.add(Component.literal("  ").append(filter.getHoverName()).append(append));
    }

    /** Текстура M65-модели для рендера прицепленной маски (см. клиентский GasMaskLayer). */
    public String getModelTexture() {
        return mono ? ArmorGasMaskItem.Variant.MONO.modelTexture : ArmorGasMaskItem.Variant.M65.modelTexture;
    }
}
