package com.hbm_m.item.bomb;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code ItemFleija}, {@code ItemSolinium} und {@code ItemN2}: Bombenteile mit "Verwendet in:"
 * und dem Namen der Bombe. fleija_propellant ist im Original selten (rare).
 */
public class ItemBombPart extends Item implements ITooltipProvider {

    private final Supplier<? extends Block> bomb;

    public ItemBombPart(Properties properties, Supplier<? extends Block> bomb) {
        super(properties);
        this.bomb = bomb;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable("item.bomb_part.used_in"));
        list.add(bomb.get().getName());
    }

    /** Original ItemFleija.getRarity: nur fleija_propellant ist rare. */
    public static Properties rare(Properties p) {
        return p.rarity(Rarity.RARE);
    }
}
