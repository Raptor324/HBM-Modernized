package com.hbm_m.item.block;

import java.util.List;
import java.util.Set;

import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * 1:1-Port von {@code ItemBlockBlastInfo}: Baustoffe zeigen ihren Explosionswiderstand
 * ({@code "Blast Resistance: " + getExplosionResistance}) in Gold. Die Liste sind genau die Bloecke, die das
 * Original mit dieser Item-Klasse registriert (Meta-Varianten = eigene IDs im Port).
 */
public final class ItemBlockBlastInfo {

    private ItemBlockBlastInfo() {}

    private static final Set<String> BLOCKS = Set.of(
            "platemetal_base", "platemetal_black", "platemetal_white", "platemetal_red", "platemetal_green",
            "platemetal_light_gray", "platemetal_blue", "platemetal_purple", "platemetal_cyan", "platemetal_pink",
            "platemetal_lime", "platemetal_yellow", "platemetal_light_blue", "platemetal_magenta", "platemetal_orange",
            "gravel_obsidian",
            "asphalt", "asphalt_light",
            "reinforced_brick", "reinforced_glass", "reinforced_glass_pane", "reinforced_light", "reinforced_sand",
            "reinforced_lamp_off", "reinforced_lamp_on", "reinforced_laminate", "reinforced_laminate_pane",
            "reinforced_stone", "reinforced_ducrete",
            "concrete_smooth",
            "concrete_colored_ext_bronze", "concrete_colored_ext_hazard", "concrete_colored_ext_indigo",
            "concrete_colored_ext_machine", "concrete_colored_ext_machine_stripe", "concrete_colored_ext_pink",
            "concrete_colored_ext_purple", "concrete_colored_ext_sand",
            // Original "concrete" (Concrete Tile)
            "concrete_tile",
            "concrete_asbestos", "concrete_rebar", "concrete_super", "concrete_super_broken",
            "ducrete_smooth", "ducrete", "concrete_pillar",
            "brick_concrete", "brick_concrete_mossy", "brick_concrete_cracked", "brick_concrete_broken", "brick_concrete_marked",
            "brick_ducrete", "brick_obsidian", "brick_compound", "brick_light", "brick_fire",
            "cmb_brick", "cmb_brick_reinforced",
            "vinyl_tile", "vinyl_tile_small");

    public static boolean has(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && RefStrings.MODID.equals(id.getNamespace()) && BLOCKS.contains(id.getPath());
    }

    /** Original {@code addInformation}: nach den Block-Tooltips die Widerstandszeile. */
    public static void appendTooltip(Block block, List<Component> list) {
        if (!has(block)) return;
        list.add(Component.literal("Blast Resistance: " + block.getExplosionResistance()).withStyle(ChatFormatting.GOLD));
    }
}
