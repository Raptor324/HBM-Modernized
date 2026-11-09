package com.hbm_m.client.stress;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.config.ModClothConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * {@code /nucleus stress} - client-side stress-test command (port of CrankShaft's
 * {@code /flywheel stress} for the Nucleus machine engine):
 * <pre>
 * /nucleus stress spawn &lt;count&gt; [&lt;model&gt;] [&lt;skin&gt;] [&lt;spacing&gt;]
 * /nucleus stress clear
 * /nucleus stress anims on|random|off
 * </pre>
 * Model suggestions: REAL block registry ids of every Nucleus-rendered machine
 * ("hbm_m:advanced_assembly_machine", ...) plus every door declaration
 * ("hbm_m:fire_door", ...).
 * The skin argument ("skin=clean") suggests the skins of the CONCRETELY selected
 * door. Spawn/clear are singleplayer only (integrated server places real
 * multiblock machines); the command tree itself works everywhere.
 */
@OnlyIn(Dist.CLIENT)
public final class NucleusStressCommand {

    private static final int STRESS_MAX = 100_000;
    private static final List<String> STRESS_COUNTS = List.of("100", "1000", "10000");
    private static final String DEFAULT_MODEL = "hbm_m:press";

    private NucleusStressCommand() {}

    private static final SuggestionProvider<CommandSourceStack> COUNT_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(STRESS_COUNTS, builder);

    private static final SuggestionProvider<CommandSourceStack> MODEL_SUGGESTIONS =
            (context, builder) -> suggestMatching(NucleusStressManager.machineModelIds(), builder);

    /** "skin=..." - only the skins of the door selected in the &lt;model&gt; argument. */
    private static final SuggestionProvider<CommandSourceStack> SKIN_SUGGESTIONS =
            (context, builder) -> suggestMatching(
                    NucleusStressManager.doorSkinSuggestions(context.getArgument("model", String.class)), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> stress = net.minecraft.commands.Commands.literal("stress");

        stress.then(net.minecraft.commands.Commands.literal("spawn")
                .then(net.minecraft.commands.Commands.argument("count", IntegerArgumentType.integer(1, STRESS_MAX))
                        .suggests(COUNT_SUGGESTIONS)
                        .executes(context -> executeSpawn(context, DEFAULT_MODEL, null, null))
                        .then(net.minecraft.commands.Commands.argument("model", NucleusStressTokenArgument.token())
                                .suggests(MODEL_SUGGESTIONS)
                                .executes(context -> executeSpawn(context,
                                        context.getArgument("model", String.class), null, null))
                                .then(net.minecraft.commands.Commands.argument("skin", NucleusStressTokenArgument.prefixed("skin="))
                                        .suggests(SKIN_SUGGESTIONS)
                                        .executes(context -> executeSpawn(context,
                                                context.getArgument("model", String.class),
                                                context.getArgument("skin", String.class), null))
                                        .then(net.minecraft.commands.Commands.argument("spacing", DoubleArgumentType.doubleArg(0.0D))
                                                .executes(context -> executeSpawn(context,
                                                        context.getArgument("model", String.class),
                                                        context.getArgument("skin", String.class),
                                                        DoubleArgumentType.getDouble(context, "spacing")))))
                                .then(net.minecraft.commands.Commands.argument("spacing", DoubleArgumentType.doubleArg(0.0D))
                                        .executes(context -> executeSpawn(context,
                                                context.getArgument("model", String.class), null,
                                                DoubleArgumentType.getDouble(context, "spacing")))))));

        stress.then(net.minecraft.commands.Commands.literal("clear")
                .executes(context -> executeClear(context)));

        LiteralArgumentBuilder<CommandSourceStack> anims = net.minecraft.commands.Commands.literal("anims");
        anims.then(net.minecraft.commands.Commands.literal("on")
                .executes(context -> executeAnims(context, NucleusStressManager.AnimMode.ON)));
        anims.then(net.minecraft.commands.Commands.literal("random")
                .executes(context -> executeAnims(context, NucleusStressManager.AnimMode.RANDOM)));
        anims.then(net.minecraft.commands.Commands.literal("off")
                .executes(context -> executeAnims(context, NucleusStressManager.AnimMode.OFF)));
        stress.then(anims);

        LiteralArgumentBuilder<CommandSourceStack> debug = net.minecraft.commands.Commands.literal("debug");
        debug.then(net.minecraft.commands.Commands.literal("cull_render")
                .executes(context -> executeToggleCullRender(context, null))
                .then(net.minecraft.commands.Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> executeToggleCullRender(context, BoolArgumentType.getBool(context, "enabled")))));

        // Operators only: the client-side source carries the permission level the
        // server synced to the player (singleplayer = cheats on / LAN with cheats).
        dispatcher.register(net.minecraft.commands.Commands.literal("nucleus")
                .requires(source -> source.hasPermission(2))
                .then(stress)
                .then(debug));
    }

    private static int executeSpawn(CommandContext<CommandSourceStack> context, String model, @Nullable String skin,
                                    @Nullable Double spacing) {
        int count = IntegerArgumentType.getInteger(context, "count");
        feedback(NucleusStressManager.spawn(Minecraft.getInstance(), count, model, skin, spacing));
        return 1;
    }

    private static int executeClear(CommandContext<CommandSourceStack> context) {
        feedback(NucleusStressManager.clear(Minecraft.getInstance()));
        return 1;
    }

    private static int executeToggleCullRender(CommandContext<CommandSourceStack> context, @Nullable Boolean enabled) {
        boolean newState = enabled != null ? enabled : !ModClothConfig.isDebugCullRender();
        ModClothConfig.get().nucleusDebugCullRender = newState;
        feedback(Component.literal("§e[HBM-M] Compute Cull Debug Render: " + (newState ? "§aENABLED (green overlay, depth-test disabled)" : "§cDISABLED")));
        return 1;
    }

    private static int executeAnims(CommandContext<CommandSourceStack> context, NucleusStressManager.AnimMode mode) {
        switch (mode) {
            case ON -> feedback(NucleusStressManager.animsOn(Minecraft.getInstance()));
            case RANDOM -> feedback(NucleusStressManager.animsRandom(Minecraft.getInstance()));
            case OFF -> feedback(NucleusStressManager.animsOff(Minecraft.getInstance()));
        }
        return 1;
    }

    private static void feedback(Component message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(message, false);
        }
    }

    private static CompletableFuture<Suggestions> suggestMatching(Iterable<String> candidates,
                                                                   SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (matchesCandidate(remaining, candidate.toLowerCase(Locale.ROOT))) {
                builder.suggest(candidate);
            }
        }
        return builder.buildFuture();
    }

    private static boolean matchesCandidate(String input, String candidate) {
        if (input.isEmpty() || matchesWordStart(input, candidate)) {
            return true;
        }
        int inputColon = input.indexOf(':');
        if (inputColon >= 0) {
            int candColon = candidate.indexOf(':');
            if (candColon >= 0) {
                String inputNs = input.substring(0, inputColon);
                String inputPath = input.substring(inputColon + 1);
                String candNs = candidate.substring(0, candColon);
                String candPath = candidate.substring(candColon + 1);
                if (candNs.startsWith(inputNs) && matchesWordStart(inputPath, candPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesWordStart(String input, String candidate) {
        if (input.isEmpty() || candidate.startsWith(input)) {
            return true;
        }
        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            if (c == '_' || c == ':' || c == '=' || c == '/') {
                if (candidate.startsWith(input, i + 1)) {
                    return true;
                }
            }
        }
        return false;
    }
}
