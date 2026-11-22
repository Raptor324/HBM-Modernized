package com.hbm_m.creativetabs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Служебная вкладка «портировано за ночь»: сюда ночные портирующие агенты
 * добавляют по одной строке на каждый свежепортированный станок, чтобы было
 * видно, что появилось за ночь. Машина попадает сюда в дополнение к своей
 * обычной вкладке (machine/nuke/...), а не вместо неё.
 */
public class NightlyTab {
    private static final List<Supplier<? extends ItemLike>> PORTED = new ArrayList<>();

    static {
        add(ModBlocks.BARREL_STEEL);
        add(ModBlocks.BLAST_DOOR);
        add(ModBlocks.BLOCK_GRAPHITE_DETECTOR);
        add(ModBlocks.BLOCK_GRAPHITE_FUEL);
        add(ModBlocks.BLOCK_GRAPHITE_LITHIUM);
        add(ModBlocks.BLOCK_GRAPHITE_SOURCE);
        add(ModBlocks.BROADCASTER_PC);
        add(ModBlocks.CAPACITOR_COPPER);
    }

    /** Регистрирует станок во вкладке; вызывается из статики одного класса — ModBlocks/ModItems уже инициализированы. */
    public static void add(Supplier<? extends ItemLike> item) {
        PORTED.add(item);
    }

    public static void populate(BiConsumer<ItemStack, TabVisibility> acceptor) {
        Consumer<ItemStack> add = stack -> acceptor.accept(stack, TabVisibility.PARENT_AND_SEARCH_TABS);
        for (ItemStack stack : stacks()) {
            add.accept(stack);
        }
    }

    private static List<ItemStack> stacks() {
        return PORTED.stream()
                .map(s -> s.get().asItem().getDefaultInstance())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
