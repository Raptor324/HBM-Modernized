package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.hbm_m.blockentity.BaseHbmBlockEntity;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * Gekippte Maschine ({@code checkTilt}) fuer Baked-Modelle - Gegenstueck zu den Zeilen der Originalrenderer
 * {@code if(te.tilted) { glTranslated(0, -drop, 0); glRotated(10, 0, 0, 1); glRotated(5, 0, 1, 0); }}, die dort vor der
 * Ausrichtungsdrehung stehen und damit in Weltachsen um die Blockmitte am Boden wirken. Die fertig gebackenen Flaechen
 * sind bereits ausgerichtet, also wird die Kippung hier in Weltachsen auf die Eckpunkte gelegt.
 */
public class TiltedBakedModel extends BakedModelWrapper<BakedModel> {

    private static final ModelProperty<Boolean> TILTED = new ModelProperty<>();

    private final Matrix4f tilt;

    public TiltedBakedModel(BakedModel original, double drop) {
        super(original);
        this.tilt = new Matrix4f()
                .translate(0.5F, 0F, 0.5F)
                .translate(0F, (float) -drop, 0F)
                .rotateZ((float) Math.toRadians(10))
                .rotateY((float) Math.toRadians(5))
                .translate(-0.5F, 0F, -0.5F);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        ModelData data = super.getModelData(level, pos, state, modelData);
        boolean tilted = level.getBlockEntity(pos) instanceof BaseHbmBlockEntity be && be.tilted;
        return data.derive().with(TILTED, tilted).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        if (!Boolean.TRUE.equals(data.get(TILTED))) return super.getQuads(state, side, rand, data, renderType);
        // gekippt passt keine Flaeche mehr buendig an einen Nachbarn: alles ungecullt ausgeben
        if (side != null) return List.of();
        List<BakedQuad> out = new ArrayList<>();
        addTilted(out, super.getQuads(state, null, rand, data, renderType));
        for (Direction d : Direction.values()) addTilted(out, super.getQuads(state, d, rand, data, renderType));
        return out;
    }

    private void addTilted(List<BakedQuad> out, List<BakedQuad> quads) {
        for (BakedQuad q : quads) {
            int[] v = q.getVertices().clone();
            int stride = v.length / 4;
            for (int i = 0; i < 4; i++) {
                int o = i * stride;
                Vector4f p = new Vector4f(Float.intBitsToFloat(v[o]), Float.intBitsToFloat(v[o + 1]), Float.intBitsToFloat(v[o + 2]), 1F);
                tilt.transform(p);
                v[o] = Float.floatToRawIntBits(p.x());
                v[o + 1] = Float.floatToRawIntBits(p.y());
                v[o + 2] = Float.floatToRawIntBits(p.z());
                if (stride >= 8) {
                    int n = v[o + 7];
                    Vector3f nv = new Vector3f((byte) (n & 0xFF) / 127F, (byte) ((n >> 8) & 0xFF) / 127F, (byte) ((n >> 16) & 0xFF) / 127F);
                    if (nv.lengthSquared() > 0) {
                        tilt.transformDirection(nv);
                        v[o + 7] = (n & 0xFF000000) | ((((byte) (nv.z() * 127)) & 0xFF) << 16) | ((((byte) (nv.y() * 127)) & 0xFF) << 8) | (((byte) (nv.x() * 127)) & 0xFF);
                    }
                }
            }
            out.add(new BakedQuad(v, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade(), q.hasAmbientOcclusion()));
        }
    }
}
