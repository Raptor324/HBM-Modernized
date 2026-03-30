package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.util.MultipartFacingTransforms;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Iris/chunk mesh: при {@code render_active=false} — Base, Frame, Slider и Spinner (idle).
 * При {@code render_active=true} — только Base и Frame; подвижные части в BER.
 * <p>
 * Поворот: {@link MultipartFacingTransforms#chemicalPlantBakedRotationY} — blockstate для этого блока
 * <b>без</b> {@code rotationY}, чтобы совпадать с {@code LegacyAnimator.setupBlockTransform} (VBO).
 * <p>
 * <b>Прозрачность:</b> запечённый чанк-меш не поддерживает корректный {@link RenderType#translucent()} (порядок,
 * смешивание). Часть {@code Fluid} с альфой в baked не попадает — только в
 * {@link com.hbm_m.client.render.implementations.ChemicalPlantRenderer} через translucent pass.
 */
public class ChemicalPlantBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private static final String[] PRIORITY = { "Base", "Frame", "Slider", "Spinner", "Fluid" };

    private final String[] cachedPartNames;
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;

    public ChemicalPlantBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
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
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    /**
     * Только cutout: совпадает с {@code chemical_plant.json} и исключает попытки запекать translucent в terrain.
     */
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    RandomSource rand, ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }

        if (!ShaderCompatibilityDetector.isExternalShaderActive()) {
            return List.of();
        }

        List<BakedQuad> result = new ArrayList<>();
        int rotationY = getRotationYForFacing(state);
        boolean renderActive = state.hasProperty(MachineChemicalPlantBlock.RENDER_ACTIVE)
            && state.getValue(MachineChemicalPlantBlock.RENDER_ACTIVE);

        BakedModel basePart = parts.get("Base");
        if (basePart != null) {
            result.addAll(ModelHelper.transformQuadsByFacing(
                basePart.getQuads(state, side, rand, modelData, renderType), rotationY));
        }

        if (state.hasProperty(MachineChemicalPlantBlock.FRAME) && state.getValue(MachineChemicalPlantBlock.FRAME)) {
            BakedModel framePart = parts.get("Frame");
            if (framePart != null) {
                result.addAll(ModelHelper.transformQuadsByFacing(
                    framePart.getQuads(state, side, rand, modelData, renderType), rotationY));
            }
        }

        if (!renderActive) {
            addIdleSliderAndSpinner(state, side, rand, modelData, renderType, rotationY, result);
        }

        return result;
    }

    /** Soft peak sine (BobMathUtil.sps); при anim=0 даёт 1.0. */
    private static double chemicalSps(double x) {
        return Math.sin(Math.PI / 2.0 * Math.cos(x));
    }

    private void addIdleSliderAndSpinner(BlockState state, @Nullable Direction side, RandomSource rand,
                                        ModelData modelData, @Nullable net.minecraft.client.renderer.RenderType renderType,
                                        int rotationY, List<BakedQuad> result) {
        double sdx = chemicalSps(0) * 0.375;

        /*
         * Legacy GL (RenderChemicalPlant): после R_facing слайдер — только glTranslated(sdx, 0, 0) вдоль локальной X,
         * т.е. R * (v + (sdx,0,0)) = R*v + R*(sdx,0,0). Нельзя делать translate до R с (-0.5,0,-0.5) — иначе
         * «диагональный» оффсет на N/S и визуально «лишние» 90°.
         * Спиннер при static: T(0.5) R_spin T(-0.5) при R_spin=0 — единичный; лишний translate до R на квадах
         * давал сдвиг на полблока.
         */
        float slideRad = (float) Math.toRadians(rotationY);
        float slideTx = (float) (sdx * Math.cos(slideRad));
        float slideTz = (float) (sdx * Math.sin(slideRad));

        BakedModel sliderPart = parts.get("Slider");
        if (sliderPart != null) {
            List<BakedQuad> sq = sliderPart.getQuads(state, side, rand, modelData, renderType);
            sq = ModelHelper.transformQuadsByFacing(sq, rotationY);
            sq = ModelHelper.translateQuads(sq, slideTx, 0f, slideTz);
            result.addAll(sq);
        }

        BakedModel spinnerPart = parts.get("Spinner");
        if (spinnerPart != null) {
            List<BakedQuad> spq = spinnerPart.getQuads(state, side, rand, modelData, renderType);
            result.addAll(ModelHelper.transformQuadsByFacing(spq, rotationY));
        }
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
        for (String partName : new String[] { "Base", "Frame", "Slider", "Spinner" }) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                for (Direction dir : Direction.values()) {
                    allQuads.addAll(part.getQuads(null, dir, rand, modelData, renderType));
                }
                allQuads.addAll(part.getQuads(null, null, rand, modelData, renderType));
            }
        }
        this.cachedItemQuads = allQuads;
    }

    @Override
    protected List<String> getItemRenderPartNames() {
        return List.of("Base", "Frame", "Slider", "Spinner");
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return getParticleIcon(ModelData.EMPTY);
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

    private static int getRotationYForFacing(BlockState state) {
        if (!state.hasProperty(MachineChemicalPlantBlock.FACING)) {
            return 0;
        }
        return MultipartFacingTransforms.chemicalPlantBakedRotationY(state.getValue(MachineChemicalPlantBlock.FACING));
    }
}
