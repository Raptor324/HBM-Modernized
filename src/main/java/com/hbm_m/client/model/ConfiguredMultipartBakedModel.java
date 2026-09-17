package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.RenderHooks;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?}

/**
 * Единая multipart-модель станков (лоадер {@code hbm_m:machine_parts_loader}).
 * Части — ВСЕ группы OBJ (автодиско́вер, ручные списки в JSON не нужны).
 *
 * <p>По умолчанию ведёт себя как обычная блочная модель: рисует все части и в
 * мире, и в предмете. Фабричные станки ({@code MachineRenderers}) вливают в
 * инстанс свой конфиг в конце запекания моделей —
 * {@link #applyMachineConfig}: мир рисует BER (chunk mesh пуст), item-части —
 * по объявлению спеки. Никаких строк-ключей и внешних реестров.
 *
 * <p>Заменяет пер-станочные подклассы {@link AbstractMultipartBakedModel},
 * отличавшиеся только конфигом. Станкам с собственной логикой модели (Radar,
 * Press, BatterySocket) по-прежнему нужны отдельные классы.
 */
public class ConfiguredMultipartBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    /** Путь id модели без namespace; одинаков у ВСЕХ вариантов blockstate и у item-модели. */
    private final String modelKey;

    /** Мир — геометрия целиком в BER (выставляется спекой фабрики). */
    private boolean berWorld;
    /** Части item-рендера (из спеки станка); null = все части. */
    @Nullable
    private List<String> itemParts;
    //? if forge {
    /** Render types чанк-пасса; null = дефолт. */
    @Nullable
    private List<RenderType> chunkRenderTypes;
    //?}

    private final String[] cachedPartNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached;
    /** itemSideQuads[0] = null-side, [1..6] = Direction.ordinal()+1. Кэш отфильтрованных списков. */
    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[] itemSideQuads = new List[7];

    public ConfiguredMultipartBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, String modelKey) {
        super(parts, transforms);
        this.modelKey = modelKey;
        this.cachedPartNames = parts.keySet().stream().sorted().toArray(String[]::new);
    }

    public String getModelKey() {
        return modelKey;
    }

    /**
     * Вливание конфига станка (MachineRenderRegistry.bindBakedModels, конец бейка).
     *
     * @param itemParts части item-рендера; null = все части модели
     */
    public void applyMachineConfig(@Nullable List<String> itemParts,
                                   @Nullable List<RenderType> chunkRenderTypes) {
        this.berWorld = true;
        this.itemParts = itemParts;
        //? if forge {
        this.chunkRenderTypes = chunkRenderTypes;
        //?}
        clearCaches();
    }

    @Override
    public String[] getPartNames() {
        return cachedPartNames;
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Мир — геометрия в BER/VBO (конфиг фабричной спеки).
        return state != null && berWorld;
    }

    @Override
    protected List<String> getItemRenderPartNames() {
        List<String> itemPartList = itemParts;
        if (itemPartList == null) {
            return List.of(cachedPartNames);
        }
        List<String> result = new ArrayList<>(itemPartList.size());
        for (String name : itemPartList) {
            if (parts.containsKey(name)) {
                result.add(name);
            }
        }
        return result;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        // 3-arg контракт ванили: BER hot path + часть путей item-рендера.
        if (state == null) {
            return getItemQuads(side, rand);
        }
        return super.getQuads(state, side, rand);
    }

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand);
        }
        return super.getQuads(state, side, rand, modelData, renderType);
    }
    //?} elif neoforge {
    /*@Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, net.neoforged.neoforge.client.model.data.ModelData modelData,
                                    @Nullable RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand);
        }
        return super.getQuads(state, side, rand, modelData, renderType);
    }
    *///?}

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand) {
        int idx = side == null ? 0 : side.ordinal() + 1;
        List<BakedQuad> cached = itemSideQuads[idx];
        if (cached != null) {
            return cached;
        }
        if (!itemQuadsCached) {
            List<BakedQuad> allQuads = new ArrayList<>();
            for (String partName : getItemRenderPartNames()) {
                BakedModel part = parts.get(partName);
                if (part == null) continue;
                for (Direction dir : Direction.values()) {
                    allQuads.addAll(RenderHooks.getModelQuads(part, null, dir, rand, null));
                }
                allQuads.addAll(RenderHooks.getModelQuads(part, null, null, rand, null));
            }
            this.cachedItemQuads = allQuads.isEmpty() ? List.of() : List.copyOf(allQuads);
            this.itemQuadsCached = true;
        }
        List<BakedQuad> result = cachedItemQuads;
        if (side != null) {
            result = cachedItemQuads.stream()
                .filter(quad -> quad.getDirection() == side)
                .toList();
        }
        itemSideQuads[idx] = result;
        return result;
    }

    //? if forge {
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        if (chunkRenderTypes != null && !chunkRenderTypes.isEmpty()) {
            return ChunkRenderTypeSet.of(chunkRenderTypes.toArray(RenderType[]::new));
        }
        return super.getRenderTypes(state, rand, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return getParticleIcon(ModelData.EMPTY);
    }
    //?}

    @Override
    public void clearCaches() {
        super.clearCaches();
        this.itemQuadsCached = false;
        this.cachedItemQuads = null;
        java.util.Arrays.fill(itemSideQuads, null);
    }
}
