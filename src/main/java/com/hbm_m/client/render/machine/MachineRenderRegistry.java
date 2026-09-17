package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.PlatformHooks;

/**
 * Реестр всех фабричных спек станков. Заменяет пер-renderer бойлерплейт:
 * <ul>
 *   <li>{@link #flushAll(Matrix4f)} — единый instanced-flush (InstancedRenderFrame
 *       зовёт его в AFTER_BLOCK_ENTITIES вместо N хардкодов flushInstancedBatches);</li>
 *   <li>{@link #clearAll()} — единая инвалидация GPU-кешей (вызывается из
 *       {@code com.hbm_m.client.render.cache.RenderCacheManager} на reload/disconnect).</li>
 * </ul>
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
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

    /** Единый instanced-flush всех фабричных станков (render thread, AFTER_BLOCK_ENTITIES). */
    public static void flushAll(Matrix4f projection) {
        for (int i = 0; i < SPECS.size(); i++) {
            SPECS.get(i).flush(projection);
        }
    }

    /**
     * Фаза 2 (после MDI-диспетча): затухающие инстансы прямых путей —
     * GPU-bones chain-части (Ring/руки сборочных машин) и MDI-fallback.
     * Порядок «opaque всех путей → fading MDI → fading прямых путей» устраняет
     * depth-reject полупрозрачной геометрией непрозрачной базы.
     */
    public static void flushAllFading(Matrix4f projection) {
        for (int i = 0; i < SPECS.size(); i++) {
            SPECS.get(i).flushFading(projection);
        }
    }

    /**
     * Связывает спеки станков с их запечёнными multipart-моделями — вызывается из
     * ModelEvent.ModifyBakingResult (после бейка, до первого рендера).
     *
     * <p>Один проход по карте бейка: каждый {@link ConfiguredMultipartBakedModel}
     * резолвится в спеку по ключу записи. У направленных машин КАЖДЫЙ вариант
     * blockstate (facing с y-поворотом) и item-модель ("item/<id>") — ОТДЕЛЬНЫЕ
     * инстансы, и каждый получает конфиг спеки (berWorld + item-части + render types).
     */
    public static void bindBakedModels(java.util.Map<?, BakedModel> models) {
        // блок (по ns:path реестра) -> спека; предмет блока -> спека
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
                        + "мир будет двоиться, item без item-фильтра", spec.id());
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

    /** Единая инвалидация GPU-кешей всех фабричных станков (render thread). */
    public static void clearAll() {
        for (int i = 0; i < SPECS.size(); i++) {
            SPECS.get(i).clear();
        }
    }
}
