//? if forge {
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Абстрактный базовый класс для рендеринга иконок брони в GUI.
 *
 * Forge-only: использует ModelData/RenderType overloads и Forge OBJ pipeline.
 * Для Fabric позже будет отдельная реализация.
 */
public abstract class AbstractArmorBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    protected final IArmorModelConfig config;

    protected AbstractArmorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, IArmorModelConfig config) {
        super(parts, transforms);
        this.config = config;
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
        // В vanilla ItemOverrides ctor private; на Forge используем EMPTY.
        return ItemOverrides.EMPTY;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                   RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        if (shouldSkipWorldRendering(state)) {
            return Collections.emptyList();
        }

        // ITEM RENDER: state == null
        ArmorItem.Type armorType = null;
        String[] partsToRender = config.getPartsForType(armorType);
        if (partsToRender == null || partsToRender.length == 0) {
            partsToRender = config.getPartOrder();
        }

        List<BakedQuad> all = new ArrayList<>();
        for (String partName : partsToRender) {
            BakedModel part = parts.get(partName);
            if (part == null) continue;

            if (side != null) {
                all.addAll(part.getQuads(null, side, rand, modelData, renderType));
            } else {
                for (Direction dir : Direction.values()) {
                    all.addAll(part.getQuads(null, dir, rand, modelData, renderType));
                }
                all.addAll(part.getQuads(null, null, rand, modelData, renderType));
            }
        }
        return all;
    }

    @Override
    @Deprecated
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }
}
//?}
