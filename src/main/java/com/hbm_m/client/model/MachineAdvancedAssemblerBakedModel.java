package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?}

public class MachineAdvancedAssemblerBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private final String[] cachedPartNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public MachineAdvancedAssemblerBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
        
        this.cachedPartNames = parts.keySet().stream()
            .sorted((a, b) -> {
                String[] priority = {"Base", "Frame", "Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1",
        "ArmLower2", "ArmUpper2", "Head2", "Spike2"};
                int aIndex = indexOf(priority, a);
                int bIndex = indexOf(priority, b);
                
                if (aIndex != -1 && bIndex != -1) return Integer.compare(aIndex, bIndex);
                else if (aIndex != -1) return -1;
                else if (bIndex != -1) return 1;
                else return a.compareTo(b);
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
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        //? if forge {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
        //?}

        //? if neoforge {
        /*// 1.21.1 neoforge: чанк-бэкер вызывает 5-arg overload (см. ниже). 3-arg — item/BER hot path.
        // WORLD: геометрия полностью в BER/VBO (как на forge). ITEM: приоритетные части (как на forge).
        if (state == null) {
            return buildItemQuadsFromRenderParts(side, rand);
        }
        return List.of();
        *///?}

        //? if fabric {
        /*if (state == null) {
            return getItemQuads(side, rand);
        }
        return List.of();
        *///?}
    }

    //? if neoforge {
    /*// NeoForge 1.21.1: 5-arg overload — вызывается чанк-бэкером. Зеркалируем forge-логику:
    // WORLD (state != null) — List.of() (геометрия в BER/VBO), ITEM (state == null) — приоритетные части.
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, net.neoforged.neoforge.client.model.data.ModelData modelData,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) {
            return buildItemQuadsFromRenderParts(side, rand);
        }
        return List.of();
    }
    *///?}

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }

        // WORLD: геометрия полностью в BER/VBO.
        return List.of();
    }

    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand,
                                          ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (!itemQuadsCached) {
            buildItemQuads(rand, modelData, renderType);
            itemQuadsCached = true;
        }
        if (side != null) {
            return cachedItemQuads.stream()
                .filter(quad -> quad.getDirection() == side)
                .toList();
        }
        return cachedItemQuads;
    }

    private void buildItemQuads(RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> allQuads = new ArrayList<>();
        List<String> itemRenderParts = getItemRenderPartNames();
        
        for (String partName : itemRenderParts) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                for (Direction dir : Direction.values()) {
                    List<BakedQuad> partQuads = part.getQuads(null, dir, rand, modelData, renderType);
                    allQuads.addAll(partQuads);
                }
                List<BakedQuad> generalQuads = part.getQuads(null, null, rand, modelData, renderType);
                allQuads.addAll(generalQuads);
            }
        }
        this.cachedItemQuads = allQuads;
    }
    //?}

    //? if fabric {
    /*private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand) {
        if (!itemQuadsCached) {
            buildItemQuads(rand);
            itemQuadsCached = true;
        }
        if (side != null) {
            return cachedItemQuads.stream()
                .filter(quad -> quad.getDirection() == side)
                .toList();
        }
        return cachedItemQuads;
    }

    private void buildItemQuads(RandomSource rand) {
        List<BakedQuad> allQuads = new ArrayList<>();
        List<String> itemRenderParts = getItemRenderPartNames();
        for (String partName : itemRenderParts) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                for (Direction dir : Direction.values()) {
                    allQuads.addAll(part.getQuads(null, dir, rand));
                }
                allQuads.addAll(part.getQuads(null, null, rand));
            }
        }
        this.cachedItemQuads = allQuads;
    }
    *///?}

    @Override
    protected List<String> getItemRenderPartNames() {
        List<String> result = new ArrayList<>();
        String[] priorityParts = {"Base", "Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1",
        "ArmLower2", "ArmUpper2", "Head2", "Spike2"};
        
        for (String priorityPart : priorityParts) {
            if (parts.containsKey(priorityPart)) {
                result.add(priorityPart);
            }
        }
        return result;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        //? if forge {
        return getParticleIcon(ModelData.EMPTY);
        //?}

        //? if fabric {
        /*return super.getParticleIcon();
        *///?}

        //? if neoforge {
        /*return super.getParticleIcon();
        *///?}
    }

    @Override
    public void clearCaches() {
        super.clearCaches();
        clearItemQuadCache();
    }

    public void clearItemQuadCache() {
        this.itemQuadsCached = false;
        this.cachedItemQuads = null;
    }
}