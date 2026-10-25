package com.hbm_m.commands;

import java.util.Map;
import java.util.Optional;

import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.config.schema.ConfigField;
import com.hbm_m.config.schema.ConfigSchema;
import com.hbm_m.config.schema.ConfigSide;
import com.hbm_m.network.ConfigSyncS2CPacket;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.world.gen.util.LogicBlockActions;
import com.hbm_m.world.gen.util.LogicBlockConditions;
import com.hbm_m.world.gen.util.LogicBlockInteractions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

/**
 * 1:1-Port der Serverbefehle aus {@code com.hbm.commands} (1.7.10), soweit der Port die
 * Unterbau-Systeme hat: {@code /ntmrad}, {@code /ntmreapnetworks}, {@code /ntmreload},
 * {@code /ntmserver}, {@code /ntmloadchunk}. {@code /ntmcustomize} und {@code /ntmsatellites}
 * stehen in eigenen Klassen.
 *
 * <p>Nicht portiert: {@code /ntmpackets} (der Port hat kein {@code PacketThreading}),
 * {@code /ntmlocate} (Strukturen laufen im Port ueber das Vanilla-Strukturgeruest, dafuer gibt es
 * {@code /locate structure}) und {@code /ntmwikirender} (Entwicklerwerkzeug fuer Wikibilder).</p>
 */
public final class HbmCommandsNT {

    private HbmCommandsNT() { }

    /** 1.7.10-{@code CommandBase}: ohne eigene Vorgabe Berechtigungsstufe 4. */
    private static final int DEFAULT_LEVEL = 4;

    private static boolean registered = false;

    public static void init() {
        if (registered) return;
        registered = true;
        CommandRegistrationEvent.EVENT.register((dispatcher, ctx, selection) -> register(dispatcher));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerRadiation(dispatcher);
        registerReapNetworks(dispatcher);
        registerReloadRecipes(dispatcher);
        dispatcher.register(configCommand("ntmserver", ConfigSide.SERVER, "server", "SERVER VARIABLES:"));
        registerDebugChunkLoad(dispatcher);
    }

    private static void msg(CommandSourceStack src, String text, ChatFormatting color) {
        src.sendSystemMessage(Component.literal(text).withStyle(color));
    }

    // ======================== /ntmrad ========================

    /** Original {@code CommandRadiation}: {@code /ntmrad <set/clear>}. */
    private static void registerRadiation(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmrad")
                .requires(src -> src.hasPermission(DEFAULT_LEVEL))
                .executes(c -> { msg(c.getSource(), "/ntmrad <set/clear>", ChatFormatting.RED); return 0; })
                .then(Commands.literal("clear").executes(c -> {
                    ChunkRadiationManager.getProxy().clearSystem(c.getSource().getLevel());
                    c.getSource().sendSystemMessage(Component.literal("Cleared radiation data!"));
                    return 1;
                }))
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0D, 100_000D)).executes(c -> {
                            float amount = (float) DoubleArgumentType.getDouble(c, "amount");
                            BlockPos pos = BlockPos.containing(c.getSource().getPosition());
                            ChunkRadiationManager.setRadiation(c.getSource().getLevel(), pos.getX(), pos.getY(), pos.getZ(), amount);
                            c.getSource().sendSystemMessage(Component.literal("Radiation set."));
                            return 1;
                        })))
                // NTM-Next-System (ADVANCED): Ausgabe/Eintrag je Strahlungsart.
                .then(Commands.literal("info").executes(c ->
                        com.hbm_m.radiation.ntmnext.NtmRadiationCommand.info(c.getSource())))
                .then(Commands.literal("add")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        new String[] {"gamma", "neutron", "beta"}, b))
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(-100_000D, 100_000D))
                                        .executes(c -> com.hbm_m.radiation.ntmnext.NtmRadiationCommand.add(
                                                c.getSource(),
                                                StringArgumentType.getString(c, "type"),
                                                DoubleArgumentType.getDouble(c, "amount"))))))
                .then(Commands.literal("wind").executes(c ->
                        com.hbm_m.radiation.ntmnext.NtmRadiationCommand.wind(c.getSource())))
                .then(Commands.literal("contaminate")
                        .then(Commands.argument("group", StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        new String[] {"short", "medium", "long", "exotic"}, b))
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0D, 100_000D))
                                        .executes(c -> com.hbm_m.radiation.ntmnext.NtmRadiationCommand.contaminate(
                                                c.getSource(),
                                                StringArgumentType.getString(c, "group"),
                                                DoubleArgumentType.getDouble(c, "amount")))))));
    }

    // ======================== /ntmreapnetworks ========================

    /** Original {@code CommandReapNetworks}. */
    private static void registerReapNetworks(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmreapnetworks")
                .requires(src -> src.hasPermission(DEFAULT_LEVEL))
                .executes(c -> {
                    try {
                        UniNodespace.reapAll();
                        msg(c.getSource(), "Nodespace cleared :)", ChatFormatting.YELLOW);
                    } catch (RuntimeException ex) {
                        printError(c.getSource(), "An error has occoured during network reap, consult the log for details.", ex);
                        throw ex;
                    }
                    return 1;
                }));
    }

    // ======================== /ntmreload ========================

    /**
     * Original {@code CommandReloadRecipes}. Die Rezepte und Fluessigkeiten liegen im Port als
     * Datenpaket vor, deshalb laeuft deren Neuladen ueber das Vanilla-Neuladen der Datenpakete;
     * Schadensresistenzen und Logikbloecke werden wie im Original direkt neu eingelesen.
     */
    private static void registerReloadRecipes(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmreload")
                .requires(src -> src.hasPermission(DEFAULT_LEVEL))
                .executes(c -> {
                    CommandSourceStack src = c.getSource();
                    try {
                        MinecraftServer server = src.getServer();
                        ReloadCommand.reloadPacks(server.getPackRepository().getSelectedIds(), src);
                        DamageResistanceHandler.init();

                        LogicBlockActions.initialize();
                        LogicBlockConditions.initialize();
                        LogicBlockInteractions.initialize();

                        msg(src, "Reload complete :)", ChatFormatting.YELLOW);
                    } catch (RuntimeException ex) {
                        printError(src, "An error has occoured during loading, consult the log for details.", ex);
                        throw ex;
                    }
                    return 1;
                }));
    }

    private static void printError(CommandSourceStack src, String head, RuntimeException ex) {
        msg(src, "----------------------------------", ChatFormatting.GRAY);
        msg(src, head, ChatFormatting.RED);
        msg(src, String.valueOf(ex.getLocalizedMessage()), ChatFormatting.RED);
        if (ex.getStackTrace().length > 0) msg(src, ex.getStackTrace()[0].toString(), ChatFormatting.RED);
        msg(src, "----------------------------------", ChatFormatting.GRAY);
    }

    // ======================== /ntmserver, /ntmclient ========================

    /**
     * Original {@code CommandReloadConfig} mit {@code CommandReloadServer}/{@code CommandReloadClient}:
     * {@code help/list/reload/get/set}. Die Variablen kommen aus dem Konfigurationsschema des Ports
     * ({@link ConfigSchema}, Seite SERVER bzw. CLIENT).
     *
     * <p>Abweichung: Das Original laesst jeden Spieler {@code /ntmserver set} ausfuehren; hier gilt
     * fuer die Serverseite dieselbe Operatorpruefung (Stufe 2) wie im Konfigurationsbildschirm.</p>
     */
    public static LiteralArgumentBuilder<CommandSourceStack> configCommand(String name, ConfigSide side, String sideName, String title) {
        boolean server = side == ConfigSide.SERVER;
        return Commands.literal(name)
                .requires(src -> src.getEntity() instanceof Player && (!server || src.hasPermission(2)))
                .executes(c -> { throw new SimpleCommandExceptionType(Component.literal("/" + name + " help")).create(); })
                .then(Commands.literal("help")
                        .executes(c -> { help(c.getSource(), name, sideName, null); return 1; })
                        .then(Commands.argument("command", StringArgumentType.word())
                                .executes(c -> { help(c.getSource(), name, sideName, StringArgumentType.getString(c, "command")); return 1; })))
                .then(Commands.literal("list").executes(c -> {
                    msg(c.getSource(), title, ChatFormatting.RED);
                    for (Map.Entry<String, String> line : ConfigSchema.snapshot(ModClothConfig.get(), side).entrySet()) {
                        c.getSource().sendSystemMessage(Component.literal("  ")
                                .append(Component.literal(line.getKey() + ": ").withStyle(ChatFormatting.GOLD))
                                .append(Component.literal(line.getValue()).withStyle(ChatFormatting.YELLOW)));
                    }
                    return 1;
                }))
                .then(Commands.literal("reload").executes(c -> {
                    if (server) ModClothConfig.reloadServer(); else ModClothConfig.reloadClient();
                    ConfigSchema.validate(ModClothConfig.get());
                    if (server) syncAll(c.getSource().getServer());
                    msg(c.getSource(), "Variables loaded from config file.", ChatFormatting.YELLOW);
                    return 1;
                }))
                .then(Commands.literal("get")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ConfigSchema.snapshot(ModClothConfig.get(), side).keySet(), b))
                                .executes(c -> {
                                    ConfigField f = field(side, StringArgumentType.getString(c, "name"));
                                    c.getSource().sendSystemMessage(Component.literal(f.getKey() + ": ").withStyle(ChatFormatting.GOLD)
                                            .append(Component.literal(f.getAsString(ModClothConfig.get())).withStyle(ChatFormatting.YELLOW)));
                                    return 1;
                                })))
                .then(Commands.literal("set")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ConfigSchema.snapshot(ModClothConfig.get(), side).keySet(), b))
                                .then(Commands.argument("value", StringArgumentType.greedyString()).executes(c -> {
                                    ConfigField f = field(side, StringArgumentType.getString(c, "name"));
                                    try {
                                        f.setFromString(ModClothConfig.get(), StringArgumentType.getString(c, "value"));
                                    } catch (RuntimeException ex) {
                                        throw new SimpleCommandExceptionType(Component.literal("Error parsing type for "
                                                + f.getKey() + ": " + ex.getLocalizedMessage())).create();
                                    }
                                    ConfigSchema.validate(ModClothConfig.get());
                                    if (server) {
                                        ModClothConfig.saveServer();
                                        syncAll(c.getSource().getServer());
                                    } else {
                                        ModClothConfig.saveClient();
                                    }
                                    msg(c.getSource(), "Value updated.", ChatFormatting.YELLOW);
                                    return 1;
                                }))));
    }

    private static ConfigField field(ConfigSide side, String key) throws CommandSyntaxException {
        ConfigField f = ConfigSchema.get(key);
        if (f == null || (side == ConfigSide.SERVER ? !f.isServer() : !f.isClient())) {
            throw new SimpleCommandExceptionType(Component.literal("Key does not exist.")).create();
        }
        return f;
    }

    private static void syncAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) ConfigSyncS2CPacket.sendTo(p);
    }

    private static void help(CommandSourceStack src, String name, String side, String command) {
        if (command != null) {
            if ("help".equals(command)) msg(src, "Shows usage for /" + name + " subcommands.", ChatFormatting.YELLOW);
            if ("list".equals(command)) msg(src, "Shows all " + side + " variable names and values.", ChatFormatting.YELLOW);
            if ("reload".equals(command)) msg(src, "Reads " + side + " variables from the config file.", ChatFormatting.YELLOW);
            if ("get".equals(command)) msg(src, "Shows value for the specified variable name.", ChatFormatting.YELLOW);
            if ("set".equals(command)) msg(src, "Sets a variable's value and saves it to the config file.", ChatFormatting.YELLOW);
        } else {
            usage(src, name, "help ", "<command>");
            usage(src, name, "list", null);
            usage(src, name, "reload", null);
            usage(src, name, "get ", "<name>");
            usage(src, name, "set ", "<name> <value>");
        }
    }

    private static void usage(CommandSourceStack src, String name, String sub, String args) {
        Component line = Component.literal("/" + name + " ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(sub).withStyle(ChatFormatting.GOLD));
        if (args != null) line = line.copy().append(Component.literal(args).withStyle(ChatFormatting.RED));
        src.sendSystemMessage(line);
    }

    // ======================== /ntmloadchunk ========================

    /**
     * Original {@code CommandDebugChunkLoad}: liest einen nicht geladenen Chunk von der Platte und
     * listet seine Blockentities; Eintraege ausserhalb des Chunks werden rot markiert.
     */
    private static void registerDebugChunkLoad(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmloadchunk")
                .requires(src -> src.hasPermission(DEFAULT_LEVEL))
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer()).executes(c -> {
                            debugChunk(c.getSource(), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "z"));
                            return 1;
                        }))));
    }

    private static void debugChunk(CommandSourceStack src, int x, int z) {
        ServerLevel level = src.getLevel();
        try {
            int cX = x >> 4;
            int cZ = z >> 4;

            if (level.hasChunk(cX, cZ)) {
                msg(src, "Chunk currently loaded.", ChatFormatting.RED);
                return;
            }

            Optional<CompoundTag> data = level.getChunkSource().chunkMap.read(new ChunkPos(cX, cZ)).join();
            if (data.isEmpty()) {
                msg(src, "Tag list null", ChatFormatting.RED);
                return;
            }

            ListTag tagList = data.get().getList("block_entities", Tag.TAG_COMPOUND);

            if (tagList.isEmpty()) {
                msg(src, "Tag list empty", ChatFormatting.RED);
            }

            for (int i1 = 0; i1 < tagList.size(); ++i1) {
                CompoundTag tileCompound = tagList.getCompound(i1);
                int tX = tileCompound.getInt("x");
                int tY = tileCompound.getInt("y");
                int tZ = tileCompound.getInt("z");
                String name = tileCompound.getString("id");

                int i = tX - cX * 16;
                int j = tY;
                int k = tZ - cZ * 16;

                boolean outside = i < 0 || i > 15 || j < level.getMinBuildHeight() || j >= level.getMaxBuildHeight() || k < 0 || k > 15;
                msg(src, name + " " + i + " " + j + " " + k, outside ? ChatFormatting.RED : ChatFormatting.GREEN);
            }
        } catch (Exception e) {
            msg(src, "" + e.getLocalizedMessage(), ChatFormatting.RED);
        }
    }
}
