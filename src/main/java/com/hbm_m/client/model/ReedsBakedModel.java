package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * 1:1 {@code RenderReeds}: das Schilf zeichnet sich von der Wasseroberflaeche bis zum Grund - oben {@code reeds_top},
 * unten {@code reeds_bottom}, dazwischen {@code reeds_mid}, je als gekreuzte Flaechen mit 75 % Helligkeit. Ist
 * {@code renderReeds} abgeschaltet, nur das oberste Stueck.
 */
public class ReedsBakedModel extends BakedModelWrapper<BakedModel> {

    private static final ModelProperty<Integer> DEPTH = new ModelProperty<>();

    public ReedsBakedModel(BakedModel original) {
        super(original);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        int depth = 0;
        if (!com.hbm_m.config.ModClothConfig.get().renderReeds) {
            depth = 1;
        } else {
            for (int i = pos.getY() - 1; i > level.getMinBuildHeight(); i--) {
                BlockState water = level.getBlockState(new BlockPos(pos.getX(), i, pos.getZ()));
                depth = pos.getY() - i;
                if (!water.is(Blocks.WATER)) break;
            }
        }
        return modelData.derive().with(DEPTH, depth).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        if (side != null) return Collections.emptyList();
        Integer d = data.get(DEPTH);
        if (d == null) return originalModel.getQuads(state, side, rand, data, renderType);
        List<BakedQuad> out = new ArrayList<>();
        for (int i = 0; i < d; i++) {
            TextureAtlasSprite icon = sprite(i == 0 ? "reeds_top" : i == d - 1 ? "reeds_bottom" : "reeds_mid");
            crossedSquares(out, icon, -i);
        }
        return out;
    }

    private static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/" + name));
    }

    /** RenderBlocks {@code drawCrossedSquares} (Hoehe 1): zwei Diagonalen, jede von beiden Seiten. */
    private static void crossedSquares(List<BakedQuad> out, TextureAtlasSprite icon, int y) {
        double a = 0.5 - 0.45, b = 0.5 + 0.45;
        double y0 = y, y1 = y + 1;
        quad(out, icon, new double[][] { { a, y1, a, 0, 0 }, { a, y0, a, 0, 16 }, { b, y0, b, 16, 16 }, { b, y1, b, 16, 0 } });
        quad(out, icon, new double[][] { { b, y1, b, 0, 0 }, { b, y0, b, 0, 16 }, { a, y0, a, 16, 16 }, { a, y1, a, 16, 0 } });
        quad(out, icon, new double[][] { { a, y1, b, 0, 0 }, { a, y0, b, 0, 16 }, { b, y0, a, 16, 16 }, { b, y1, a, 16, 0 } });
        quad(out, icon, new double[][] { { b, y1, a, 0, 0 }, { b, y0, a, 0, 16 }, { a, y0, b, 16, 16 }, { a, y1, b, 16, 0 } });
    }

    private static void quad(List<BakedQuad> out, TextureAtlasSprite icon, double[][] v) {
        int[] data = new int[32];
        int gray = 0xBF; // Original: Farbe * 0.75
        int color = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            data[o] = Float.floatToRawIntBits((float) v[i][0]);
            data[o + 1] = Float.floatToRawIntBits((float) v[i][1]);
            data[o + 2] = Float.floatToRawIntBits((float) v[i][2]);
            data[o + 3] = color;
            data[o + 4] = Float.floatToRawIntBits(icon.getU(v[i][3]));
            data[o + 5] = Float.floatToRawIntBits(icon.getV(v[i][4]));
            data[o + 6] = 0;
            data[o + 7] = 0x7F00; // Normal nach oben
        }
        out.add(new BakedQuad(data, -1, Direction.UP, icon, false));
    }
}
