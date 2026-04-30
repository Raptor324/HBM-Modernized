package com.hbm_m.client.model.loading;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Matrix4f;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cross-platform model debug logging.
 *
 * Enable with JVM flags:
 * -Dhbm_m.modelDebug=true
 * -Dhbm_m.modelDebugFilter=hbm_m:block/machines/fluid_tank,hbm_m:block/machines/chemical_plant
 *
 * Filter matches by substring (case-insensitive) against model id string.
 */
public final class ModelDebugDumper {
    private static final String PROP_ENABLED = "hbm_m.modelDebug";
    private static final String PROP_FILTER = "hbm_m.modelDebugFilter";

    private static final Set<String> LOGGED_KEYS = ConcurrentHashMap.newKeySet();

    private ModelDebugDumper() {}

    public static boolean enabled() {
        return Boolean.getBoolean(PROP_ENABLED);
    }

    public static boolean matches(ResourceLocation id) {
        if (!enabled()) return false;
        if (id == null) return false;

        String filter = System.getProperty(PROP_FILTER, "").trim();
        if (filter.isEmpty()) {
            // Default: only our namespace to avoid log spam.
            return "hbm_m".equals(id.getNamespace());
        }

        String needle = id.toString().toLowerCase(Locale.ROOT);
        return Arrays.stream(filter.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(needle::contains);
    }

    public static BakedModel wrapIfEnabled(ResourceLocation id, BakedModel delegate) {
        if (delegate == null) return null;
        if (!matches(id)) return delegate;
        if (delegate instanceof DebugBakedModel) return delegate;
        return new DebugBakedModel(id, delegate);
    }

    public static void logMatricesOnce(ResourceLocation id, String label, Matrix4f rotation, Matrix4f root, Matrix4f combined) {
        if (!matches(id)) return;
        String key = id + "|matrices|" + label;
        if (!LOGGED_KEYS.add(key)) return;

        MainRegistry.LOGGER.info(
                "[ModelDebug] {} {} \n  rot={}\n  root={}\n  combined={}",
                id, label,
                fmt(rotation), fmt(root), fmt(combined)
        );
    }

    private static String fmt(Matrix4f m) {
        if (m == null) return "null";
        float[] v = new float[16];
        m.get(v);
        // row-major human readable
        return String.format(Locale.ROOT,
                "[[% .4f,% .4f,% .4f,% .4f],[% .4f,% .4f,% .4f,% .4f],[% .4f,% .4f,% .4f,% .4f],[% .4f,% .4f,% .4f,% .4f]]",
                v[0], v[4], v[8],  v[12],
                v[1], v[5], v[9],  v[13],
                v[2], v[6], v[10], v[14],
                v[3], v[7], v[11], v[15]
        );
    }

    private static final class DebugBakedModel implements BakedModel {
        private final ResourceLocation id;
        private final BakedModel delegate;

        private DebugBakedModel(ResourceLocation id, BakedModel delegate) {
            this.id = id;
            this.delegate = delegate;
        }

        @Override
        public java.util.List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
            java.util.List<BakedQuad> quads = delegate.getQuads(state, side, rand);
            logQuadsOnce(state, side, quads);
            return quads;
        }

        private void logQuadsOnce(BlockState state, Direction side, java.util.List<BakedQuad> quads) {
            if (!matches(id)) return;
            String stateKey = (state == null) ? "null" : state.toString();
            String key = id + "|quads|" + stateKey + "|" + side;
            if (!LOGGED_KEYS.add(key)) return;

            int count = quads == null ? 0 : quads.size();
            MainRegistry.LOGGER.info("[ModelDebug] {} state={} side={} quads={}", id, stateKey, side, count);
            if (quads == null || quads.isEmpty()) return;

            // Dump first 2 quads, first 4 vertices: position + UV.
            int dump = Math.min(2, quads.size());
            for (int qi = 0; qi < dump; qi++) {
                BakedQuad q = quads.get(qi);
                int[] v = q.getVertices();
                if (v == null || v.length < 32) continue;
                StringBuilder sb = new StringBuilder();
                sb.append("[ModelDebug]   quad[").append(qi).append("] dir=").append(q.getDirection()).append(" tint=").append(q.getTintIndex()).append('\n');
                for (int vi = 0; vi < 4; vi++) {
                    int base = vi * 8;
                    float px = Float.intBitsToFloat(v[base]);
                    float py = Float.intBitsToFloat(v[base + 1]);
                    float pz = Float.intBitsToFloat(v[base + 2]);
                    float u = Float.intBitsToFloat(v[base + 4]);
                    float vv = Float.intBitsToFloat(v[base + 5]);
                    sb.append(String.format(Locale.ROOT, "[ModelDebug]     v%d pos=(% .4f,% .4f,% .4f) uv=(% .4f,% .4f)%n", vi, px, py, pz, u, vv));
                }
                MainRegistry.LOGGER.info(sb.toString().trim());
            }
        }

        // Delegate everything else
        @Override public boolean useAmbientOcclusion() { return delegate.useAmbientOcclusion(); }
        @Override public boolean isGui3d() { return delegate.isGui3d(); }
        @Override public boolean usesBlockLight() { return delegate.usesBlockLight(); }
        @Override public boolean isCustomRenderer() { return delegate.isCustomRenderer(); }
        @Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return delegate.getParticleIcon(); }
        @Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return delegate.getTransforms(); }
        @Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return delegate.getOverrides(); }
    }
}

