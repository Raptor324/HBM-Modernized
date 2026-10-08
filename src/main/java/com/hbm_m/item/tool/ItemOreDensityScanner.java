package com.hbm_m.item.tool;

import com.hbm_m.network.InfoToastPacket;
import com.hbm_m.worldgen.BedrockOreDensity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code ItemOreDensityScanner} aus dem 1.7.10-Original: schickt alle 5 Ticks je Erzkategorie eine
 * Infozeile (Dichte + Bewertung, IDs 777-782) und eine Zeile mit Gesamt-Tier und Bohrfluessigkeit (ID 783),
 * jeweils 4 Sekunden sichtbar (siehe {@link BedrockOreDensity}).
 */
public class ItemOreDensityScanner extends Item {

    /** Original {@code BedrockOreType.suffix}. */
    private static final String[] SUFFIX = { "light", "heavy", "rare", "actinide", "nonmetal", "crystal" };

    public ItemOreDensityScanner(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {

        if (!(entity instanceof ServerPlayer player) || world.getGameTime() % 5 != 0) return;

        double totalLevel = 0D;

        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            double level = BedrockOreDensity.getDensity((int) Math.floor(player.getX()), (int) Math.floor(player.getZ()), type);
            MutableComponent msg = Component.translatable("item.hbm_m.bedrock_ore.type." + SUFFIX[type.ordinal()] + ".name")
                    .append(": " + ((int) (level * 100) / 100D) + " (")
                    .append(Component.translatable(translateDensity(level)).withStyle(getColor(level)))
                    .append(Component.literal(")").withStyle(ChatFormatting.RESET));
            InfoToastPacket.sendTo(player, msg, 80, 777 + type.ordinal(), 0xFFFFFF);
            totalLevel += level;
        }
        totalLevel /= BedrockOreDensity.Type.values().length;

        int tier = BedrockOreDensity.getTier(totalLevel);
        int fill = BedrockOreDensity.getBoreFluidAmountMb(totalLevel);

        MutableComponent builder = Component.literal("Tier " + tier).withStyle(ChatFormatting.YELLOW);
        if (fill > 0) {
            builder.append(Component.literal(" - " + fill + "mB ").withStyle(ChatFormatting.YELLOW))
                    .append(fluidName(BedrockOreDensity.getBoreFluid(totalLevel)).copy().withStyle(ChatFormatting.YELLOW));
        }

        InfoToastPacket.sendTo(player, builder, 80, 777 + BedrockOreDensity.Type.values().length, 0xFFFF55);
    }

    public static String translateDensity(double density) {
        if (density <= 0.1) return "item.hbm_m.ore_density_scanner.verypoor";
        if (density <= 0.35) return "item.hbm_m.ore_density_scanner.poor";
        if (density <= 0.75) return "item.hbm_m.ore_density_scanner.low";
        if (density >= 1.9) return "item.hbm_m.ore_density_scanner.excellent";
        if (density >= 1.65) return "item.hbm_m.ore_density_scanner.veryhigh";
        if (density >= 1.25) return "item.hbm_m.ore_density_scanner.high";
        return "item.hbm_m.ore_density_scanner.moderate";
    }

    public static ChatFormatting getColor(double density) {
        if (density <= 0.1) return ChatFormatting.DARK_RED;
        if (density <= 0.35) return ChatFormatting.RED;
        if (density <= 0.75) return ChatFormatting.GOLD;
        if (density > 2) return ChatFormatting.LIGHT_PURPLE; // only for BO items that got mined with fortune
        if (density >= 1.9) return ChatFormatting.AQUA;
        if (density >= 1.65) return ChatFormatting.BLUE;
        if (density >= 1.25) return ChatFormatting.GREEN;
        return ChatFormatting.YELLOW;
    }

    private static Component fluidName(Fluid fluid) {
        //? if forge {
        return Component.translatable(fluid.getFluidType().getDescriptionId());
        //?} else {
        /*var key = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
        return Component.translatable("fluid." + key.getNamespace() + "." + key.getPath());
        *///?}
    }
}
