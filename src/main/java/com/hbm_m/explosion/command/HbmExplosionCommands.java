package com.hbm_m.explosion.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Brigadier-дерево для {@code /hbm_m explosion ...}.
 * Один аргумент {@code advanced} (greedy) после {@code type}: опциональные {@code key:value},
 * затем опционально ровно три координаты. Без координат - позиция источника команды.
 */
public final class HbmExplosionCommands {

    private static final SimpleCommandExceptionType NOT_SERVER_LEVEL =
            new SimpleCommandExceptionType(Component.translatable("commands.hbm_m.explosion.not_server_level"));
    private static final SimpleCommandExceptionType BAD_KEY_VALUE =
            new SimpleCommandExceptionType(Component.translatable("commands.hbm_m.explosion.bad_key_value"));
    private static final DynamicCommandExceptionType UNSUPPORTED_KEY =
            new DynamicCommandExceptionType(key ->
                    Component.translatable("commands.hbm_m.explosion.unsupported_key", key));
    private static final SimpleCommandExceptionType BAD_COORD_SUFFIX =
            new SimpleCommandExceptionType(Component.translatable("commands.hbm_m.explosion.bad_coord_suffix"));

    private HbmExplosionCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> buildExplosionBranch() {
        return Commands.literal("explosion")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(EXPLOSION_TYPE_SUGGESTIONS)
                        .executes(ctx -> runAtSource(ctx, ExplosionCommandOptions.DEFAULT))
                        .then(Commands.argument("advanced", StringArgumentType.greedyString())
                                .suggests(GREEDY_SUGGESTIONS)
                                .executes(HbmExplosionCommands::runGreedy)));
    }

    private static final SuggestionProvider<CommandSourceStack> EXPLOSION_TYPE_SUGGESTIONS = (ctx, builder) -> {
        String rem = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String id : ExplosionType.allIds()) {
            if (id.toLowerCase(Locale.ROOT).startsWith(rem)) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> GREEDY_SUGGESTIONS = (ctx, builder) -> {
        ExplosionType type;
        try {
            type = ExplosionType.parse(StringArgumentType.getString(ctx, "type"));
        } catch (CommandSyntaxException e) {
            return builder.buildFuture();
        }
        String adv = extractAdvancedPortion(ctx.getInput(), StringArgumentType.getString(ctx, "type"));
        return suggestForAdvanced(builder, type, adv);
    };

    /**
     * Текст после {@code type} в полной строке чата (без учёта курсора - для подсказок достаточно префикса).
     */
    private static String extractAdvancedPortion(String fullInput, String typeStr) {
        int exp = fullInput.toLowerCase(Locale.ROOT).indexOf("explosion ");
        if (exp < 0) {
            return "";
        }
        int afterExp = exp + "explosion ".length();
        if (afterExp + typeStr.length() > fullInput.length()) {
            return "";
        }
        if (!fullInput.regionMatches(true, afterExp, typeStr, 0, typeStr.length())) {
            return "";
        }
        int p = afterExp + typeStr.length();
        while (p < fullInput.length() && fullInput.charAt(p) == ' ') {
            p++;
        }
        return fullInput.substring(p);
    }

    private static CompletableFuture<Suggestions> suggestForAdvanced(SuggestionsBuilder builder, ExplosionType type, String advancedRaw) {
        String adv = advancedRaw.trim();
        if (adv.isEmpty()) {
            suggestUnusedKeys(builder, type, new HashSet<>(), "", "");
            builder.suggest("~ ~ ~");
            return builder.buildFuture();
        }

        List<String> parts = splitArgs(adv);
        if (parts.isEmpty()) {
            suggestUnusedKeys(builder, type, new HashSet<>(), "", "");
            builder.suggest("~ ~ ~");
            return builder.buildFuture();
        }

        int coordSuffix = countTrailingCoordLikeTokens(parts);
        List<String> kvParts = parts.subList(0, parts.size() - coordSuffix);

        // Режим ввода координат (1–3 токена в конце) - подсказки = вся строка advanced целиком
        if (coordSuffix > 0) {
            String coordTok = parts.get(parts.size() - 1);
            String lineBeforeLast = joinTokens(parts.subList(0, parts.size() - 1));
            suggestCoordToken(builder, lineBeforeLast, coordTok);
            return builder.buildFuture();
        }

        // Только key:value
        if (kvParts.isEmpty()) {
            suggestUnusedKeys(builder, type, new HashSet<>(), "", "");
            builder.suggest("~ ~ ~");
            return builder.buildFuture();
        }

        String last = kvParts.get(kvParts.size() - 1);
        List<String> beforeLast = kvParts.subList(0, kvParts.size() - 1);
        if (!allCompleteKeyValues(beforeLast, type)) {
            return builder.buildFuture();
        }
        Set<String> used = keysFromCompleteTokens(beforeLast, type);
        String joinedBeforeLast = joinTokens(beforeLast);

        if (last.contains(":")) {
            int c = last.indexOf(':');
            String key = last.substring(0, c).toLowerCase(Locale.ROOT);
            String valPrefix = last.substring(c + 1).toLowerCase(Locale.ROOT);
            if (!type.supportsOptionKey(key)) {
                return builder.buildFuture();
            }
            if (valPrefix.isEmpty()) {
                return suggestValue(builder, type, key, "", joinedBeforeLast);
            }
            if (isCompleteKeyValue(last, type)) {
                used.add(key);
                String fullSoFar = joinTokens(kvParts);
                suggestUnusedKeys(builder, type, used, "", fullSoFar);
                builder.suggest(fullSoFar + " ~ ~ ~");
                return builder.buildFuture();
            }
            return suggestValue(builder, type, key, valPrefix, joinedBeforeLast);
        }

        // Префикс нового ключа (ещё без ':')
        suggestUnusedKeys(builder, type, used, last.toLowerCase(Locale.ROOT), joinedBeforeLast);
        if (looksLikeCoordToken(last)) {
            suggestCoordToken(builder, joinedBeforeLast, last);
        }
        return builder.buildFuture();
    }

    private static String joinTokens(List<String> tokens) {
        return String.join(" ", tokens);
    }

    private static int countTrailingCoordLikeTokens(List<String> parts) {
        int k = 0;
        for (int j = parts.size() - 1; j >= 0 && k < 3; j--) {
            if (looksLikeCoordToken(parts.get(j))) {
                k++;
            } else {
                break;
            }
        }
        return k;
    }

    private static Set<String> keysFromCompleteTokens(List<String> tokens, ExplosionType type) {
        Set<String> used = new HashSet<>();
        for (String t : tokens) {
            if (isCompleteKeyValue(t, type)) {
                used.add(t.substring(0, t.indexOf(':')).toLowerCase(Locale.ROOT));
            }
        }
        return used;
    }

    /**
     * @param fullLineBeforeNewKey уже набранные полные токены слева (через пробел); greedy-аргумент заменяется целиком.
     */
    private static void suggestUnusedKeys(SuggestionsBuilder builder, ExplosionType type, Set<String> used,
                                          String keyNamePrefix, String fullLineBeforeNewKey) {
        String head = fullLineBeforeNewKey.isEmpty() ? "" : fullLineBeforeNewKey + " ";
        String p = keyNamePrefix.toLowerCase(Locale.ROOT);
        for (String key : new String[]{"crater", "damage", "biomes", "particles", "amplifier", "fallout", "sound"}) {
            if (type.supportsOptionKey(key) && !used.contains(key) && key.startsWith(p)) {
                builder.suggest(head + key + ":");
            }
        }
    }

    private static List<String> splitArgs(String s) {
        String[] a = s.trim().split("\\s+");
        List<String> out = new ArrayList<>();
        for (String x : a) {
            if (!x.isEmpty()) {
                out.add(x);
            }
        }
        return out;
    }

    private static boolean allCompleteKeyValues(List<String> tokens, ExplosionType type) {
        for (String t : tokens) {
            if (!isCompleteKeyValue(t, type)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCompleteKeyValue(String t, ExplosionType type) {
        if (!t.contains(":")) {
            return false;
        }
        int c = t.indexOf(':');
        if (c <= 0 || c == t.length() - 1) {
            return false;
        }
        String key = t.substring(0, c).toLowerCase(Locale.ROOT);
        String val = t.substring(c + 1);
        if (!type.supportsOptionKey(key) || val.isEmpty()) {
            return false;
        }
        try {
            applyKeyValueToken(t, type, ExplosionCommandOptions.DEFAULT);
            return true;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    /**
     * @param joinedBeforeLast полные токены до текущего (незавершённого) key:value
     */
    private static CompletableFuture<Suggestions> suggestValue(SuggestionsBuilder builder, ExplosionType type, String key,
                                                                 String valuePrefix, String joinedBeforeLast) {
        String head = joinedBeforeLast.isEmpty() ? "" : joinedBeforeLast + " ";
        String pre = valuePrefix.toLowerCase(Locale.ROOT);
        if ("amplifier".equals(key) && type.supportsAmplifier()) {
            for (String s : new String[]{"1", "2", "10", "100"}) {
                if (pre.isEmpty() || s.startsWith(pre)) {
                    builder.suggest(head + key + ":" + s);
                }
            }
        } else {
            if ("true".startsWith(pre)) {
                builder.suggest(head + key + ":true");
            }
            if ("false".startsWith(pre)) {
                builder.suggest(head + key + ":false");
            }
        }
        return builder.buildFuture();
    }

    private static void suggestCoordToken(SuggestionsBuilder builder, String lineBeforeLastToken, String partial) {
        String head = lineBeforeLastToken.isEmpty() ? "" : lineBeforeLastToken + " ";
        if (partial.isEmpty() || partial.equals("~")) {
            builder.suggest(head + "~");
        }
        if (partial.isEmpty() || !partial.contains("~")) {
            builder.suggest(head + "0");
        }
    }

    private static int runAtSource(CommandContext<CommandSourceStack> ctx, ExplosionCommandOptions opt) throws CommandSyntaxException {
        ExplosionType type = ExplosionType.parse(StringArgumentType.getString(ctx, "type"));
        CommandSourceStack src = ctx.getSource();
        Level level = src.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            throw NOT_SERVER_LEVEL.create();
        }
        Vec3 pos = src.getPosition();
        ExplosionCommandExecutor.execute(serverLevel, type, pos, filterOptionsForType(type, opt));
        src.sendSuccess(() -> Component.translatable("commands.hbm_m.explosion.success", type.id()), true);
        return 1;
    }

    private static int runGreedy(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ExplosionType type = ExplosionType.parse(StringArgumentType.getString(ctx, "type"));
        String greedy = StringArgumentType.getString(ctx, "advanced");
        if (greedy.trim().isEmpty()) {
            return runAtSource(ctx, ExplosionCommandOptions.DEFAULT);
        }
        ParsedAdvanced parsed = parseAdvanced(greedy, type);
        CommandSourceStack src = ctx.getSource();
        Level level = src.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            throw NOT_SERVER_LEVEL.create();
        }
        Vec3 pos = parsed.useSourcePosition() ? src.getPosition() : parsed.position(src);
        ExplosionCommandExecutor.execute(serverLevel, type, pos, filterOptionsForType(type, parsed.options()));
        src.sendSuccess(() -> Component.translatable("commands.hbm_m.explosion.success", type.id()), true);
        return 1;
    }

    private static ExplosionCommandOptions filterOptionsForType(ExplosionType type, ExplosionCommandOptions opt) {
        return new ExplosionCommandOptions(
                type.supportsCrater() && opt.crater(),
                type.supportsDamage() && opt.damage(),
                type.supportsBiomes() && opt.biomes(),
                type.supportsParticles() && opt.particles(),
                type.supportsAmplifier() ? opt.amplifier() : 1.0f,
                opt.sound(),
                type.supportsFallout() ? opt.fallout() : true
        );
    }

    private record ParsedAdvanced(ExplosionCommandOptions options, Double px, Double py, Double pz,
                                  boolean relativeX, boolean relativeY, boolean relativeZ, boolean useSourcePosition) {
        Vec3 position(CommandSourceStack src) {
            Vec3 o = src.getPosition();
            double x = relativeX ? o.x + px : px;
            double y = relativeY ? o.y + py : py;
            double z = relativeZ ? o.z + pz : pz;
            return new Vec3(x, y, z);
        }
    }

    /**
     * Разбор: с конца до трёх подряд токенов-координат; всё слева - key:value. Координаты можно опустить → позиция источника.
     */
    private static ParsedAdvanced parseAdvanced(String greedy, ExplosionType type) throws CommandSyntaxException {
        List<String> parts = splitArgs(greedy);
        if (parts.isEmpty()) {
            return new ParsedAdvanced(ExplosionCommandOptions.DEFAULT, 0.0, 0.0, 0.0, false, false, false, true);
        }

        int n = parts.size();
        int coordCount = 0;
        int i = n - 1;
        while (i >= 0 && coordCount < 3 && looksLikeCoordToken(parts.get(i))) {
            coordCount++;
            i--;
        }
        if (coordCount > 0 && coordCount < 3) {
            throw BAD_COORD_SUFFIX.create();
        }

        int kvEnd = n - coordCount;
        ExplosionCommandOptions opt = ExplosionCommandOptions.DEFAULT;
        for (int k = 0; k < kvEnd; k++) {
            opt = applyKeyValueToken(parts.get(k), type, opt);
        }

        if (coordCount == 0) {
            return new ParsedAdvanced(opt, 0.0, 0.0, 0.0, false, false, false, true);
        }

        CoordTriple xt = parseCoordToken(parts.get(n - 3));
        CoordTriple yt = parseCoordToken(parts.get(n - 2));
        CoordTriple zt = parseCoordToken(parts.get(n - 1));
        return new ParsedAdvanced(opt, xt.value(), yt.value(), zt.value(), xt.relative(), yt.relative(), zt.relative(), false);
    }

    private static boolean looksLikeCoordToken(String t) {
        if (t.contains(":")) {
            return false;
        }
        if (t.startsWith("~")) {
            return true;
        }
        try {
            Double.parseDouble(t);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private record CoordTriple(double value, boolean relative) {}

    private static CoordTriple parseCoordToken(String token) throws CommandSyntaxException {
        if (token.startsWith("^")) {
            throw BAD_KEY_VALUE.create();
        }
        if (token.startsWith("~")) {
            String rest = token.substring(1);
            if (rest.isEmpty()) {
                return new CoordTriple(0, true);
            }
            try {
                return new CoordTriple(Double.parseDouble(rest), true);
            } catch (NumberFormatException e) {
                throw BAD_KEY_VALUE.create();
            }
        }
        try {
            return new CoordTriple(Double.parseDouble(token), false);
        } catch (NumberFormatException e) {
            throw BAD_KEY_VALUE.create();
        }
    }

    private static ExplosionCommandOptions applyKeyValueToken(String raw, ExplosionType type, ExplosionCommandOptions opt) throws CommandSyntaxException {
        int c = raw.indexOf(':');
        if (c <= 0 || c == raw.length() - 1) {
            throw BAD_KEY_VALUE.create();
        }
        String key = raw.substring(0, c).toLowerCase(Locale.ROOT);
        String val = raw.substring(c + 1).toLowerCase(Locale.ROOT);
        if (!type.supportsOptionKey(key)) {
            throw UNSUPPORTED_KEY.create(key);
        }
        return switch (key) {
            case "crater" -> opt.withCrater(parseBool(val));
            case "damage" -> opt.withDamage(parseBool(val));
            case "biomes" -> opt.withBiomes(parseBool(val));
            case "particles" -> opt.withParticles(parseBool(val));
            case "amplifier" -> opt.withAmplifier(parseFloatStrict(val));
            case "sound" -> opt.withSound(parseBool(val));
            case "fallout" -> opt.withFallout(parseBool(val));
            default -> throw BAD_KEY_VALUE.create();
        };
    }

    private static boolean parseBool(String v) throws CommandSyntaxException {
        if ("true".equals(v) || "1".equals(v) || "yes".equals(v)) {
            return true;
        }
        if ("false".equals(v) || "0".equals(v) || "no".equals(v)) {
            return false;
        }
        throw BAD_KEY_VALUE.create();
    }

    private static float parseFloatStrict(String v) throws CommandSyntaxException {
        try {
            float f = Float.parseFloat(v);
            if (Float.isNaN(f) || f <= 0) {
                throw BAD_KEY_VALUE.create();
            }
            return f;
        } catch (NumberFormatException e) {
            throw BAD_KEY_VALUE.create();
        }
    }
}
