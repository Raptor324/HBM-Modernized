package com.hbm_m.client.stress;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.network.chat.Component;

/**
 * A string argument that reads one WHITESPACE-delimited token. Brigadier's
 * {@code word()}/{@code string()} both fall back to {@code readUnquotedString()}
 * for unquoted input, which rejects ':' and '=' - i.e. "hbm_m:bat9000" and
 * "skin=clean" cannot even be typed. Client commands are never serialized over
 * the network, so a custom ArgumentType without serialization support is fine.
 *
 * <p>The {@code prefixed} variant refuses tokens that do not start with the given
 * prefix ("skin="). This keeps Brigadier's branch selection unambiguous: a bare
 * number after the model falls through to the spacing argument instead of being
 * eaten by the skin argument.</p>
 */
public final class NucleusStressTokenArgument implements ArgumentType<String> {

    @javax.annotation.Nullable
    private final String requiredPrefix;

    private NucleusStressTokenArgument(@javax.annotation.Nullable String requiredPrefix) {
        this.requiredPrefix = requiredPrefix;
    }

    public static NucleusStressTokenArgument token() {
        return new NucleusStressTokenArgument(null);
    }

    /** Only accepts tokens starting with the prefix, e.g. "skin=clean". */
    public static NucleusStressTokenArgument prefixed(String prefix) {
        return new NucleusStressTokenArgument(prefix);
    }

    @Override
    public String parse(com.mojang.brigadier.StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        String token = reader.getString().substring(start, reader.getCursor());
        if (requiredPrefix != null && !token.regionMatches(true, 0, requiredPrefix, 0, requiredPrefix.length())) {
            throw new SimpleCommandExceptionType(Component.literal(
                    "Expected '" + requiredPrefix + "<value>', got '" + token + "'")).create();
        }
        return token;
    }
}
