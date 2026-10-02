// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

/**
 * Запечённая модель большой батареи-пака: одна OBJ-часть (Battery или Capacitor),
 * ретекстуренная на тир ещё на этапе запекания (texture override в лоадере).
 * Используется только как item-модель — в мире сокет рисует MachineBatterySocketBakedModel.
 * Структура квади-доступа зеркалит {@link MissileBakedModel}.
 */
@OnlyIn(Dist.CLIENT)
public class BatteryPackBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private final String[] partNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached;

    public BatteryPackBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
        this.partNames = parts.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getPartNames() {
        return partNames;
    }

    @Override
    protected List<String> getItemRenderPartNames() {
        return List.of(partNames);
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return true;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        //? if forge {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
        //?}

        //? if neoforge {
        /*if (state == null) {
            return getItemQuads(side, rand);
        }
        return List.of();
        *///?}
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
            ModelData data, @Nullable RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand);
        }
        return List.of();
    }

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand) {
        if (!itemQuadsCached || cachedItemQuads == null) {
            // Кэш один раз для всех сторон + unsided (см. MissileBakedModel).
            List<BakedQuad> all = new ArrayList<>();
            for (String partName : getItemRenderPartNames()) {
                BakedModel part = parts.get(partName);
                if (part == null) {
                    continue;
                }
                for (Direction d : Direction.values()) {
                    all.addAll(com.hbm_m.platform.RenderHooks.getPartQuads(part, null, d, rand));
                }
                all.addAll(com.hbm_m.platform.RenderHooks.getPartQuads(part, null, null, rand));
            }
            // Исходные display-трансформы рассчитаны на модель, сдвинутую на
            // offset_y = -0.5 (центрирование вертикали: OBJ занимает y 0..2).
            all = shiftY(all, -0.5f);
            cachedItemQuads = all;
            itemQuadsCached = true;
        }
        if (side != null) {
            return cachedItemQuads.stream()
                    .filter(quad -> quad.getDirection() == side)
                    .toList();
        }
        return cachedItemQuads;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    /** Сдвиг позиций квадов по Y (форм-агностично: stride = длина/4). */
    private static List<BakedQuad> shiftY(List<BakedQuad> quads, float dy) {
        if (quads.isEmpty() || dy == 0f) return quads;
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] verts = quad.getVertices();
            int stride = verts.length / 4;
            if (stride < 3) { out.add(quad); continue; }
            int[] copy = verts.clone();
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                float y = Float.intBitsToFloat(copy[i + 1]);
                copy[i + 1] = Float.floatToIntBits(y + dy);
            }
            out.add(new BakedQuad(copy, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade()));
        }
        return out;
    }
}
