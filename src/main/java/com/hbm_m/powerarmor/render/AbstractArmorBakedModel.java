package com.hbm_m.powerarmor.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.AbstractMultipartBakedModel;
import com.hbm_m.interfaces.IArmorModelConfig;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?}

/**
 * Абстрактный базовый класс для рендеринга иконок брони в GUI.
 *
 * Forge 1.20.1 — ModelData/RenderType overload getQuads;
 * NeoForge 1.21.1 — ванильный 3-арговый getQuads.
 */
public abstract class AbstractArmorBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    protected final IArmorModelConfig config;
    /** Тип предмета брони, определяется при bake по имени item-модели (t51_helmet → HELMET). */
    @Nullable
    protected final ArmorItem.Type itemArmorType;

    protected AbstractArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms,
                                      IArmorModelConfig config, @Nullable ArmorItem.Type itemArmorType) {
        super(parts, transforms);
        this.config = config;
        this.itemArmorType = itemArmorType;
    }

    /**
     * Определяет тип брони по пути item-модели при bake.
     * Для общих моделей вроде {@code t51_armor} возвращает null (рендер всех частей — entity layer).
     */
    @Nullable
    protected static ArmorItem.Type resolveItemArmorType(@Nullable ResourceLocation modelName) {
        if (modelName == null) return null;
        String path = modelName.getPath();
        if (path.endsWith("_helmet")) return ArmorItem.Type.HELMET;
        if (path.endsWith("_chestplate")) return ArmorItem.Type.CHESTPLATE;
        if (path.endsWith("_leggings")) return ArmorItem.Type.LEGGINGS;
        if (path.endsWith("_boots")) return ArmorItem.Type.BOOTS;
        return null;
    }

    /** Создаёт новую модель с теми же частями, но другими трансформациями. */
    public abstract AbstractArmorBakedModel withTransforms(ItemTransforms newTransforms);

    @Override
    public String[] getPartNames() {
        return config.getPartOrder();
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return state != null;
    }

    @Override
    public ItemOverrides getOverrides() {
        // В vanilla ItemOverrides ctor private; используем EMPTY.
        return ItemOverrides.EMPTY;
    }

    //? if < 1.21.1 {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        return collectQuads(state, side, rand, modelData, renderType);
    }
    //?} else {
    /*@Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return collectQuads(state, side, rand, null, null);
    }
    *///?}

    private List<BakedQuad> collectQuads(@Nullable BlockState state, @Nullable Direction side,
                                         RandomSource rand, Object modelData, @Nullable RenderType renderType) {
        if (shouldSkipWorldRendering(state)) {
            return Collections.emptyList();
        }

        // ITEM RENDER: state == null — itemArmorType задаётся при bake из имени item-модели
        String[] partsToRender = config.getPartsForType(itemArmorType);
        if (partsToRender == null || partsToRender.length == 0) {
            partsToRender = config.getPartOrder();
        }

        List<BakedQuad> all = new ArrayList<>();
        for (String partName : partsToRender) {
            BakedModel part = parts.get(partName);
            if (part == null) continue;

            all.addAll(getPartQuads(part, side, rand, modelData, renderType));
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private static List<BakedQuad> getPartQuads(BakedModel part, @Nullable Direction side, RandomSource rand,
                                                Object modelData, @Nullable RenderType renderType) {
        //? if < 1.21.1 {
        if (side != null) {
            return part.getQuads(null, side, rand, (net.minecraftforge.client.model.data.ModelData) modelData, renderType);
        }
        List<BakedQuad> all = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            all.addAll(part.getQuads(null, dir, rand, (net.minecraftforge.client.model.data.ModelData) modelData, renderType));
        }
        all.addAll(part.getQuads(null, null, rand, (net.minecraftforge.client.model.data.ModelData) modelData, renderType));
        return all;
        //?} else {
        /*if (side != null) {
            return part.getQuads(null, side, rand);
        }
        List<BakedQuad> all = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            all.addAll(part.getQuads(null, dir, rand));
        }
        all.addAll(part.getQuads(null, null, rand));
        return all;
        *///?}
    }

    //? if forge {
    @Override
    @Deprecated
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }
    //?}
}
