package com.hbm_m.client.render.machine;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.PlatformHooks;

/**
 * Registry of all factory machine specs. Replaces per-renderer boilerplate:
 * <ul>
 *   <li>{@link #flushAll(Matrix4f)} - a single instanced flush (InstancedRenderFrame
 *       calls it in AFTER_BLOCK_ENTITIES instead of N hardcoded flushInstancedBatches);</li>
 *   <li>{@link #clearAll()} - a single GPU-cache invalidation (called from
 *       {@code com.hbm_m.client.render.cache.RenderCacheManager} on reload/disconnect).</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class MachineRenderRegistry {

    private static final List<MachineSpec<?>> SPECS = new ArrayList<>();

    private MachineRenderRegistry() {}

    static void register(MachineSpec<?> spec) {
        SPECS.add(spec);
        MainRegistry.LOGGER.info("[MachineRenderers] registered '{}' ({} parts)", spec.id(), spec.parts().size());
    }

    public static List<MachineSpec<?>> specs() {
        return Collections.unmodifiableList(SPECS);
    }

    // ==================== Stress command helpers (/nucleus stress) ====================

    /**
     * Registry ids of every block handled by a Nucleus machine spec, namespaced
     * and sorted ("hbm_m:advanced_assembly_machine", ...) - the Brigadier
     * suggestion source for stress spawn. Real block ids, not internal spec ids.
     */
    public static List<String> managedBlockIds() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < SPECS.size(); i++) {
            MachineSpec<?> spec = SPECS.get(i);
            for (net.minecraft.world.level.block.Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                if (!isValidFor(block, spec.type())) {
                    continue;
                }
                var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
                String id = key.getNamespace() + ":" + key.getPath();
                if (!out.contains(id)) {
                    out.add(id);
                }
            }
        }
        java.util.Collections.sort(out);
        return out;
    }

    /** Whether this block is rendered through a Nucleus machine spec. */
    public static boolean isManagedBlock(net.minecraft.world.level.block.Block block) {
        for (int i = 0; i < SPECS.size(); i++) {
            if (isValidFor(block, SPECS.get(i).type())) {
                return true;
            }
        }
        return false;
    }

    /** Single instanced flush of all factory machines (render thread, AFTER_BLOCK_ENTITIES). */
    public static void flushAll(Matrix4f projection) {
        for (int i = 0; i < SPECS.size(); i++) {
            SPECS.get(i).flush(projection);
        }
    }

    /**
     * Phase 2 (after the MDI dispatch): fading instances of the direct paths -
     * GPU-bones chain parts (Ring / assembly machine arms) and MDI-fallback.
     * The order "opaque of all paths -> fading MDI -> fading direct paths"
     * eliminates opaque geometry being depth-rejected by translucent geometry.
     */
    public static void flushAllFading(Matrix4f projection) {
        for (int i = 0; i < SPECS.size(); i++) {
            SPECS.get(i).flushFading(projection);
        }
    }

    /**
     * Binds machine specs to their baked multipart models - called from
     * ModelEvent.ModifyBakingResult (after baking, before the first render).
     *
     * <p>One pass over the bake map: every {@link ConfiguredMultipartBakedModel}
     * is resolved to a spec by the entry key. For facing machines EACH blockstate
     * variant (facing with y-rotation) and item model ("item/<id>") is a SEPARATE
     * instance, and each receives the spec's config (berWorld + item parts +
     * render types).
     */
    public static void bindBakedModels(java.util.Map<?, BakedModel> models) {
        // block (by ns:path registry key) -> spec; block item -> spec
        java.util.Map<String, MachineSpec<?>> byBlock = new java.util.HashMap<>();
        java.util.Map<String, MachineSpec<?>> byItem = new java.util.HashMap<>();
        for (int i = 0; i < SPECS.size(); i++) {
            MachineSpec<?> spec = SPECS.get(i);
            for (net.minecraft.world.level.block.Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
                if (!isValidFor(block, spec.type())) {
                    continue;
                }
                var blockKey = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
                byBlock.put(blockKey.getNamespace() + ":" + blockKey.getPath(), spec);
                net.minecraft.world.item.Item item = block.asItem();
                if (item != net.minecraft.world.item.Items.AIR) {
                    var itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
                    byItem.put(itemKey.getNamespace() + ":" + itemKey.getPath(), spec);
                }
            }
        }

        java.util.Map<MachineSpec<?>, Integer> boundCount = new java.util.IdentityHashMap<>();
        for (var entry : models.entrySet()) {
            if (!(entry.getValue() instanceof com.hbm_m.client.model.ConfiguredMultipartBakedModel model)) {
                continue;
            }
            net.minecraft.resources.ResourceLocation key = PlatformHooks.getModelId(entry.getKey());
            String path = key.getPath();
            MachineSpec<?> spec = path.startsWith("item/")
                    ? byItem.get(key.getNamespace() + ":" + path.substring("item/".length()))
                    : byBlock.get(key.getNamespace() + ":" + path);
            if (spec != null) {
                model.applyMachineConfig(spec.deriveItemParts(), spec.chunkRenderTypes());
                boundCount.merge(spec, 1, Integer::sum);
            }
        }
        for (int i = 0; i < SPECS.size(); i++) {
            MachineSpec<?> spec = SPECS.get(i);
            if (!boundCount.containsKey(spec)) {
                MainRegistry.LOGGER.warn("[MachineRenderers] spec '{}' bound 0 baked models - "
                        + "the world will render doubled, item without an item filter", spec.id());
            }
        }
    }

    private static boolean isValidFor(net.minecraft.world.level.block.Block block,
                                      net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            if (type.isValid(state)) {
                return true;
            }
        }
        return false;
    }

    /** Single GPU-cache invalidation of all factory machines (render thread). */
    public static void clearAll() {
        for (int i = 0; i < SPECS.size(); i++) {
            SPECS.get(i).clear();
        }
    }
}
