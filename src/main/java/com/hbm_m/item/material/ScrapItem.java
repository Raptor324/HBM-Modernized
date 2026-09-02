package com.hbm_m.item.material;

import java.util.List;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Литейные отходы (порт ItemScraps 1.7.10). Тултип 1:1 с оригиналом:
 * количество материала в форме {@code Mats.formatAmount} — при зажатом shift
 * в миллисведрибах (mB), иначе разбивка на блоки/слитки/самородки/кванты.
 * По умолчанию предмет из креатива равен 1 слитку (72 кванта = 144 mB);
 * системы, выдающие лом, могут записать нестандартное количество в тег
 * предмета под ключом {@code amount} (как в оригинале).
 */
public class ScrapItem extends Item implements ITooltipProvider {

    // MaterialShapes оригинала: NUGGET=8, INGOT=8*9=72, BLOCK=72*9=648
    public static final int QUANTA_PER_NUGGET = 8;
    public static final int QUANTA_PER_INGOT = QUANTA_PER_NUGGET * 9;
    public static final int QUANTA_PER_BLOCK = QUANTA_PER_INGOT * 9;

    public ScrapItem(Properties properties) {
        super(properties);
    }

    /** Количество в квантах: тег {@code amount} либо 1 слиток по умолчанию. */
    public static int getAmount(ItemStack stack) {
        return PlatformHooks.contains(stack, "amount")
                ? PlatformHooks.getInt(stack, "amount")
                : QUANTA_PER_INGOT;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(formatAmount(getAmount(stack), net.minecraft.client.gui.screens.Screen.hasShiftDown()));
    }

    /** Порт {@code Mats.formatAmount} 1.7.10. */
    public static MutableComponent formatAmount(int amount, boolean showInMb) {
        if (showInMb) {
            return Component.literal((amount * 2) + "mB").withStyle(ChatFormatting.GRAY);
        }

        MutableComponent out = Component.empty().withStyle(ChatFormatting.GRAY);

        int blocks = amount / QUANTA_PER_BLOCK;
        amount -= QUANTA_PER_BLOCK * blocks;
        int ingots = amount / QUANTA_PER_INGOT;
        amount -= QUANTA_PER_INGOT * ingots;
        int nuggets = amount / QUANTA_PER_NUGGET;
        amount -= QUANTA_PER_NUGGET * nuggets;
        int quanta = amount;

        if (blocks > 0) out.append(key(blocks, "block", "blocks")).append(" ");
        if (ingots > 0) out.append(key(ingots, "ingot", "ingots")).append(" ");
        if (nuggets > 0) out.append(key(nuggets, "nugget", "nuggets")).append(" ");
        if (quanta > 0) out.append(key(quanta, "quantum", "quanta")).append(" ");

        return out;
    }

    private static MutableComponent key(int count, String one, String many) {
        return Component.translatable(count == 1 ? "matshape." + one : "matshape." + many, count);
    }
}
