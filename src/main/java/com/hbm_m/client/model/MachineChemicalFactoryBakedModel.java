package com.hbm_m.client.model;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?}

/**
 * Chemical Factory — порт 1.7.10 {@code ResourceManager.chemical_factory} (части Base/Frame/Fan1/Fan2).
 * <p>
 * World render: пустой chunk mesh — вся геометрия в BER/VBO
 * ({@link com.hbm_m.client.render.implementations.MachineChemicalFactoryRenderer}).
 * Item render: все части как в оригинальном {@code ItemRenderBase} (Base + Frame + Fan1 + Fan2),
 * display-трансформы из JSON модели.
 */
public class MachineChemicalFactoryBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private static final String[] PRIORITY = { "Base", "Frame", "Fan1", "Fan2" };

    private final String[] cachedPartNames;

    public MachineChemicalFactoryBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);

        this.cachedPartNames = parts.keySet().stream()
            .sorted((a, b) -> {
                int aIndex = indexOf(PRIORITY, a);
                int bIndex = indexOf(PRIORITY, b);
                if (aIndex != -1 && bIndex != -1) return Integer.compare(aIndex, bIndex);
                if (aIndex != -1) return -1;
                if (bIndex != -1) return 1;
                return a.compareTo(b);
            })
            .toArray(String[]::new);
    }

    private static int indexOf(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) return i;
        }
        return -1;
    }

    @Override
    public String[] getPartNames() {
        return cachedPartNames;
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Мир — геометрия полностью в BER/VBO; предметы собираются из всех частей.
        return state != null;
    }

    //? if forge {
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }
    //?}

    @Override
    protected java.util.List<String> getItemRenderPartNames() {
        return java.util.List.of(PRIORITY);
    }
}
