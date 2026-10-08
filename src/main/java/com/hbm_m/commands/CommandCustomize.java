package com.hbm_m.commands;

import com.hbm_m.item.ICustomizable;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.commands.CommandCustomize} ({@code /ntmcustomize [args...]}): ruft
 * {@link ICustomizable#customize} des gehaltenen Gegenstands mit den leerzeichengetrennten Argumenten auf.
 * Fuer alle Spieler (Berechtigungsstufe 0).
 */
public final class CommandCustomize {

    private CommandCustomize() { }

    private static boolean registered = false;

    public static void init() {
        if (registered) return;
        registered = true;
        CommandRegistrationEvent.EVENT.register((dispatcher, ctx, selection) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmcustomize")
                .requires(src -> src.hasPermission(0))
                .executes(c -> process(c.getSource(), new String[0]))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(c -> {
                            String raw = StringArgumentType.getString(c, "args").trim();
                            return process(c.getSource(), raw.isEmpty() ? new String[0] : raw.split("\\s+"));
                        })));
    }

    private static int process(CommandSourceStack sender, String[] args) {
        if (!(sender.getEntity() instanceof Player player)) {
            sender.sendSystemMessage(Component.literal("Customization is only available to players!").withStyle(ChatFormatting.RED));
            return 0;
        }

        ItemStack held = player.getMainHandItem();

        if (held.isEmpty() || !(held.getItem() instanceof ICustomizable item)) {
            sender.sendSystemMessage(Component.literal("You have to hold a customizable item to use this command!").withStyle(ChatFormatting.RED));
            return 0;
        }

        item.customize(player, held, args);
        return 1;
    }
}
