package com.hbm_m.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Blockzustand am Gegenstand ({@code BlockStateTag}, Phase C, P3).
 * 1.20.1: Unter-Tag {@code BlockStateTag} im Item-NBT; Vanilla-BlockItem wendet ihn beim Setzen an.
 * 1.21.1: derselbe Unter-Tag bleibt in CUSTOM_DATA (eigene Leser, Rezepte), zusaetzlich die Komponente
 * {@code DataComponents.BLOCK_STATE}, die Vanilla-BlockItem beim Setzen anwendet.
 */
public final class BlockStateItemData {
    private BlockStateItemData() {}

    public static final String KEY = "BlockStateTag";

    /** Wie {@code StackNbt.orCreate(stack).put("BlockStateTag", bst)}. */
    public static void put(ItemStack stack, CompoundTag bst) {
        StackNbt.orCreate(stack).put(KEY, bst);
        //? if >= 1.21.1 {
        /*java.util.Map<String, String> props = new java.util.LinkedHashMap<>();
        for (String k : bst.getAllKeys()) props.put(k, bst.get(k).getAsString());
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE, new net.minecraft.world.item.component.BlockItemStateProperties(props));
        *///?}
    }
}
