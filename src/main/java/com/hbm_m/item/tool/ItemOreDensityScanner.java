package com.hbm_m.item.tool;

import com.hbm_m.worldgen.BedrockOreDensity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

/**
 * 1:1-Aequivalent zu {@code ItemOreDensityScanner} aus dem 1.7.10-Original: zeigt alle 5 Ticks
 * die Bedrock-Erz-Dichte jeder der 6 Kategorien an der Spielerposition als Actionbar-Text an,
 * plus den daraus resultierenden Gesamt-Tier und das benoetigte Bohr-Fluid (siehe
 * {@link BedrockOreDensity}).
 */
public class ItemOreDensityScanner extends Item {

    public ItemOreDensityScanner(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (level.getGameTime() % 5 != 0) return;

        int x = player.getBlockX();
        int z = player.getBlockZ();

        StringBuilder line = new StringBuilder();
        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            double density = BedrockOreDensity.getDensity(x, z, type);
            line.append(typeLabel(type)).append(": ").append(formatDensity(density)).append("  ");
        }

        double total = BedrockOreDensity.getTotalDensity(x, z);
        int tier = BedrockOreDensity.getTier(total);
        int fluidAmount = BedrockOreDensity.getBoreFluidAmountMb(total);
        String fluidName = fluidAmount <= 0 ? "none"
                : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(BedrockOreDensity.getBoreFluid(total)).getPath();

        line.append(ChatFormatting.YELLOW).append("Tier ").append(tier);
        if (fluidAmount > 0) {
            line.append(" - ").append(fluidAmount).append("mB ").append(fluidName);
        }

        player.displayClientMessage(Component.literal(line.toString()), true);
    }

    private static String typeLabel(BedrockOreDensity.Type type) {
        return switch (type) {
            case LIGHT -> "Light";
            case HEAVY -> "Heavy";
            case RARE -> "Rare";
            case ACTINIDE -> "Actinide";
            case NONMETAL -> "NonMetal";
            case CRYSTAL -> "Crystal";
        };
    }

    private static String formatDensity(double density) {
        ChatFormatting color;
        if (density <= 0.1) color = ChatFormatting.DARK_RED;
        else if (density <= 0.35) color = ChatFormatting.RED;
        else if (density <= 0.75) color = ChatFormatting.GOLD;
        else if (density >= 1.9) color = ChatFormatting.AQUA;
        else if (density >= 1.65) color = ChatFormatting.BLUE;
        else if (density >= 1.25) color = ChatFormatting.GREEN;
        else color = ChatFormatting.YELLOW;
        return color + String.valueOf((int) (density * 100) / 100D) + ChatFormatting.RESET;
    }
}
