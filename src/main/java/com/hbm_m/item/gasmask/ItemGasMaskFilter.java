package com.hbm_m.item.gasmask;

import java.util.List;

import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Сменный фильтр противогаза. ПКМ — вкрутить в надетую маску (или в маску,
 * прицепленную к шлему как модификация), старый фильтр возвращается в руку.
 * Порт {@link com.hbm.items.tool.ItemFilter} (1.7.10).
 */
public class ItemGasMaskFilter extends Item implements ITooltipProvider {

    public static final int DEFAULT_MAX_DAMAGE = 20000;

    public final int maxFilterDamage;

    public ItemGasMaskFilter(Properties properties, int maxFilterDamage) {
        super(properties.durability(maxFilterDamage));
        this.maxFilterDamage = maxFilterDamage;
    }

    public ItemGasMaskFilter(Properties properties) {
        this(properties, DEFAULT_MAX_DAMAGE);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack filterStack = player.getItemInHand(hand);
        // Маска на голове, прицепленная к шлему или в слоте лица Curios.
        ItemStack mask = GasMaskUtil.resolveWornMask(player);

        if (!(mask.getItem() instanceof IGasMask) || !IGasMask.isFilterApplicable(mask, filterStack)) {
            return InteractionResultHolder.pass(filterStack);
        }

        if (!level.isClientSide()) {
            ItemStack old = GasMaskUtil.takeFilter(mask);
            IGasMask.installFilter(mask, this);

            if (old.isEmpty()) {
                filterStack.shrink(1);
            } else {
                // Старый фильтр возвращаем в руку (как в оригинале).
                player.setItemInHand(hand, old);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.FILTER_SCREW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        return InteractionResultHolder.sidedSuccess(filterStack, level.isClientSide());
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Классы защиты фильтра — как в оригинале выводится регистрация ArmorRegistry.
        tooltip.add(Component.translatable("hazard.prot").withStyle(ChatFormatting.GREEN));
        for (var clazz : ArmorRegistry.getProtection(stack.getItem())) {
            tooltip.add(Component.literal("  ").append(Component.translatable(clazz.translationKey))
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
