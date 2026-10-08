package com.hbm_m.commands;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hbm_m.config.schema.ConfigSide;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Clientbefehle des Originals ({@code ClientCommandHandler}): {@code /ntmclient}
 * ({@code CommandReloadClient}) und {@code /dumpthreadsandcrashgame} ({@code SuicideThreadDump}).
 * Registriert aus dem Client-Befehlsereignis in {@code ClientSetup}.
 */
public final class HbmClientCommandsNT {

    private HbmClientCommandsNT() { }

    private static final Logger LOGGER = LogManager.getLogger("hbm_m");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(HbmCommandsNT.configCommand("ntmclient", ConfigSide.CLIENT, "client", "CLIENT VARIABLES:"));

        dispatcher.register(Commands.literal("dumpthreadsandcrashgame")
                .requires(src -> src.getEntity() instanceof Player)
                .executes(c -> { throw new SimpleCommandExceptionType(Component.literal("Requires argument \"dump\" or \"crash\"!")).create(); })
                .then(Commands.argument("mode", StringArgumentType.word()).executes(c -> {
                    String mode = StringArgumentType.getString(c, "mode");
                    if (!mode.equals("dump") && !mode.equals("crash")) {
                        throw new SimpleCommandExceptionType(Component.literal("Requires argument \"dump\" or \"crash\"!")).create();
                    }

                    ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
                    for (ThreadInfo thread : threads) dumpThread(thread);

                    // Original: FMLCommonHandler.exitJava(0, true) - harter Abbruch ohne Herunterfahren.
                    if (mode.equals("crash")) Runtime.getRuntime().halt(0);
                    return 1;
                })));
    }

    private static void dumpThread(ThreadInfo info) {

        LOGGER.log(Level.FATAL, "===========================================");
        LOGGER.log(Level.FATAL, "Thread: " + info.getThreadName() + " PID: " + info.getThreadId());
        LOGGER.log(Level.FATAL, "Suspended: " + info.isSuspended());
        LOGGER.log(Level.FATAL, "Blocked: " + info.getBlockedTime() + "ms, " + info.getBlockedCount() + "x");
        LOGGER.log(Level.FATAL, "Runs Native: " + info.isInNative());
        LOGGER.log(Level.FATAL, "State: " + info.getThreadState().name());
        LOGGER.log(Level.FATAL, "-------------------------------------------");

        if (info.getLockedMonitors().length != 0) {
            LOGGER.log(Level.FATAL, "Following locks found:");
            for (MonitorInfo monitor : info.getLockedMonitors()) {
                LOGGER.log(Level.FATAL, "- " + monitor.getLockedStackFrame());
            }
            LOGGER.log(Level.FATAL, "-------------------------------------------");
        }

        LOGGER.log(Level.FATAL, "Stacktrace:");
        for (StackTraceElement line : info.getStackTrace()) {
            LOGGER.log(Level.FATAL, "- " + line);
        }
        LOGGER.log(Level.FATAL, "===========================================");
    }
}
