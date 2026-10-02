package com.hbm_m.satellite;

import com.hbm_m.item.ISatChip;
import com.hbm_m.item.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.commands.CommandSatellites} ({@code /ntmsatellites}): {@code orbit} schiesst den gehaltenen
 * Satelliten in den Orbit, {@code descend <freq>} loescht einen, {@code list} zeigt alle (nur fuer Spieler).
 */
public final class CommandSatellites {

    private CommandSatellites() { }

    private static boolean registered = false;

    public static void init() {
        if (registered) return;
        registered = true;
        CommandRegistrationEvent.EVENT.register((dispatcher, ctx, selection) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmsatellites")
                .requires(src -> src.hasPermission(4))
                .then(Commands.literal("orbit").executes(c -> orbit(c.getSource())))
                .then(Commands.literal("descend")
                        .then(Commands.argument("frequency", IntegerArgumentType.integer())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        SatelliteManager.get(c.getSource().getLevel()).all().keySet().stream().map(String::valueOf), b))
                                .executes(c -> descend(c.getSource(), IntegerArgumentType.getInteger(c, "frequency")))))
                .then(Commands.literal("list").executes(c -> list(c.getSource()))));
    }

    private static ServerPlayer player(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendSystemMessage(Component.translatable("commands.satellite.should_be_run_as_player").withStyle(ChatFormatting.RED));
            return null;
        }
        return player;
    }

    private static int orbit(CommandSourceStack src) {
        ServerPlayer player = player(src);
        if (player == null) return 0;

        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ISatChip && held.getItem() != ModItems.SAT_CHIP.get()) {
            SatelliteManager.get((ServerLevel) player.level()).orbit((ServerLevel) player.level(), held, ISatChip.getFreqS(held), player.getX(), player.getY(), player.getZ());
            held.shrink(1);
            src.sendSystemMessage(Component.translatable("commands.satellite.satellite_orbited").withStyle(ChatFormatting.GREEN));
        } else {
            src.sendSystemMessage(Component.translatable("commands.satellite.not_a_satellite").withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int descend(CommandSourceStack src, int freq) {
        if (player(src) == null) return 0;
        if (SatelliteManager.get(src.getLevel()).remove(freq) != null) {
            src.sendSystemMessage(Component.translatable("commands.satellite.satellite_descended").withStyle(ChatFormatting.GREEN));
        } else {
            src.sendSystemMessage(Component.translatable("commands.satellite.no_satellite").withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int list(CommandSourceStack src) {
        if (player(src) == null) return 0;
        var sats = SatelliteManager.get(src.getLevel()).all();

        if (sats.isEmpty()) {
            src.sendSystemMessage(Component.translatable("commands.satellite.no_active_satellites").withStyle(ChatFormatting.RED));
        } else {
            sats.forEach((listFreq, sat) -> src.sendSystemMessage(
                    Component.literal(listFreq + " - " + sat.getClass().getSimpleName()).withStyle(ChatFormatting.GREEN)));
        }
        return 1;
    }
}
