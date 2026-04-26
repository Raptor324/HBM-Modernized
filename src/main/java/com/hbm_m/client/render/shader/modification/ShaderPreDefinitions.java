package com.hbm_m.client.render.shader.modification;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hbm_m.main.MainRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}

/**
 * Wraps a {@link ResourceProvider} so that resources matching a target {@link ResourceLocation}
 * are intercepted, their content is run through a {@link ShaderModification}, and the modified
 * bytes are exposed back to the caller as a synthetic {@link Resource}.
 * <p>
 * This is the entry point used by {@code ClientSetup.onRegisterShaders} to compile multiple
 * variants of the same shader source with different preprocessor defines (e.g. with and
 * without {@code #define USE_INSTANCING}).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class ShaderPreDefinitions {

    private ShaderPreDefinitions() {}

    /**
     * Returns a {@link ResourceProvider} that delegates to {@code delegate} for every resource
     * except {@code target}. When {@code target} is requested the original bytes are loaded,
     * passed through {@code modification.apply(...)}, and re-wrapped as a {@link Resource}.
     */
    public static ResourceProvider wrap(ResourceProvider delegate,
                                        ResourceLocation target,
                                        ShaderModification modification) {
        return wrapRedirect(delegate, target, target, modification);
    }

    /**
     * Same as {@link #wrap}, but when {@code virtualTarget} is requested the bytes are loaded
     * from {@code realSource} (which DOES exist in resource packs), then patched and returned as
     * if they were the {@code virtualTarget}'s content. This is essential when registering
     * multiple {@link net.minecraft.client.renderer.ShaderInstance}s that share the same source
     * but need DIFFERENT compiled programs &mdash; vanilla {@code Program.getOrCreate} caches by
     * the "vertex"/"fragment" name from the JSON, so two variants pointing at the same name will
     * collide and only the first compilation wins.
     * <p>
     * Example: {@code block_lit_simple.json} references {@code "vertex": "hbm_m:block_lit"},
     * {@code block_lit_instanced.json} references {@code "vertex": "hbm_m:block_lit_instanced"}.
     * Only the latter file does not exist on disk; this provider fabricates it from
     * {@code hbm_m:shaders/core/block_lit.vsh} + {@code #define USE_INSTANCING}.
     */
    public static ResourceProvider wrapRedirect(ResourceProvider delegate,
                                                ResourceLocation virtualTarget,
                                                ResourceLocation realSource,
                                                ShaderModification modification) {
        return location -> {
            if (!location.equals(virtualTarget)) {
                return delegate.getResource(location);
            }
            Optional<Resource> original = delegate.getResource(realSource);
            if (original.isEmpty()) {
                MainRegistry.LOGGER.error("ShaderPreDefinitions: real source '{}' not found for virtual '{}'", realSource, virtualTarget);
                return original;
            }
            Resource src = original.get();
            try (InputStream in = src.open()) {
                String text = readAll(in);
                String patched = modification.apply(text);
                byte[] bytes = patched.getBytes(StandardCharsets.UTF_8);
                return Optional.of(new Resource(src.source(), () -> new java.io.ByteArrayInputStream(bytes)));
            } catch (IOException e) {
                MainRegistry.LOGGER.error("ShaderPreDefinitions: failed to patch '{}' (from '{}')", virtualTarget, realSource, e);
                return Optional.empty();
            }
        };
    }

    private static String readAll(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n", "", "\n"));
        }
    }
}
