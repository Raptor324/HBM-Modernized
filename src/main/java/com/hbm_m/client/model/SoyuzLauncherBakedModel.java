package com.hbm_m.client.model;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?}

/**
 * Baked model for the decorative Soyuz launch tower (6 separate OBJ parts:
 * Table, TowerBase, Tower, SupportBase, Support, Legs).
 * <p>
 * The tower/support masts reach ~60 blocks tall - far beyond the 16-bit
 * chunk-local vertex encoding used by the normal baked-model/chunk-mesh
 * pipeline (and by fast renderers like Sodium/Embeddium), which silently
 * wraps/corrupts geometry that large ("inside-out" looking parts). So,
 * like {@link MachineHydraulicFrackiningTowerBakedModel}, world rendering
 * is skipped entirely here and handled instead by a BlockEntityRenderer
 * using direct float-precision VBOs (see SoyuzLauncherRenderer).
 */
public class SoyuzLauncherBakedModel extends AbstractMultipartBakedModel {

    public static final String TABLE = "Table";
    public static final String TOWER_BASE = "TowerBase";
    public static final String TOWER = "Tower";
    public static final String SUPPORT_BASE = "SupportBase";
    public static final String SUPPORT = "Support";
    public static final String LEGS = "Legs";

    public static final List<String> ALL_PARTS = List.of(TABLE, TOWER_BASE, TOWER, SUPPORT_BASE, SUPPORT, LEGS);

    public SoyuzLauncherBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Item icon (state == null) still needs baked quads; in-world rendering
        // goes entirely through the BER/VBO path.
        return state != null;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (shouldSkipWorldRendering(state)) {
            return List.of();
        }
        List<BakedQuad> result = new java.util.ArrayList<>();
        for (String partName : ALL_PARTS) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                result.addAll(part.getQuads(state, side, rand));
            }
        }
        return result;
    }

    //? if forge {
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (shouldSkipWorldRendering(state)) {
            return List.of();
        }
        List<BakedQuad> result = new java.util.ArrayList<>();
        for (String partName : ALL_PARTS) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                result.addAll(part.getQuads(state, side, rand, modelData, renderType));
            }
        }
        return result;
    }
    //?}

    @Override
    protected List<String> getItemRenderPartNames() {
        return ALL_PARTS;
    }
}
