package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.block.machines.MachineFluidTankBlock; // Добавлен импорт вашего блока
import com.hbm_m.util.MultipartFacingTransforms;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class MachineFluidTankBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private final Map<ResourceLocation, Map<Object, List<BakedQuad>>> quadCache = new ConcurrentHashMap<>();

    private static final Object NULL_SIDE_KEY = new Object();
    
    // Текстура по умолчанию (если пустой бак)
    private static final ResourceLocation DEFAULT_TEX = ResourceLocation.fromNamespaceAndPath("hbm_m", "block/tank/tank_none");

    public MachineFluidTankBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms) {
        super(parts, transforms);
    }

    @Override
    public String[] getPartNames() {
        return new String[]{"Frame", "Tank"};
    }

    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();

        // 1. Получаем угол поворота блока (если это предмет в инвентаре, state == null, вернется 0)
        int rotationY = getRotationYForFacing(state);

        // 2. Вычисляем исходную сторону (до поворота) для правильного Culling'а
        Direction querySide = getUnrotatedSide(side, rotationY);

        // 3. Рендер рамы
        BakedModel frame = getPart("Frame");
        if (frame != null) {
            List<BakedQuad> frameQuads = frame.getQuads(state, querySide, rand, modelData, renderType);
            
            if (rotationY != 0) {
                quads.addAll(ModelHelper.transformQuadsByFacing(frameQuads, rotationY));
            } else {
                quads.addAll(frameQuads);
            }
        }

        // 4. Рендер бака с учетом ModelData
        BakedModel tank = getPart("Tank");
        if (tank != null) {
            ResourceLocation fluidTex = DEFAULT_TEX;
            if (modelData != null && modelData.has(MachineFluidTankBlockEntity.FLUID_TEXTURE_PROPERTY)) {
                ResourceLocation propTex = modelData.get(MachineFluidTankBlockEntity.FLUID_TEXTURE_PROPERTY);
                if (propTex != null) {
                    fluidTex = propTex;
                }
            }

            // Запрашиваем кэшированные полигоны для неповёрнутой стороны
            List<BakedQuad> tankQuads = getCachedTankQuads(tank, fluidTex, querySide, rand);
            
            if (rotationY != 0) {
                quads.addAll(ModelHelper.transformQuadsByFacing(tankQuads, rotationY));
            } else {
                quads.addAll(tankQuads);
            }
        }

        return quads;
    }

    /**
     * Возвращает градус поворота по оси Y на основе свойства FACING.
     */
    private static int getRotationYForFacing(@Nullable BlockState state) {
        if (state == null || !state.hasProperty(MachineFluidTankBlock.FACING)) {
            return 0;
        }
        return MultipartFacingTransforms.vanillaChunkMeshRotationY(state.getValue(MachineFluidTankBlock.FACING));
    }

    /**
     * Инвертирует запрашиваемую сторону для правильного отсечения невидимых граней (culling) 
     * после того, как мы применим ModelHelper.transformQuadsByFacing.
     */
    private static Direction getUnrotatedSide(@Nullable Direction side, int rotationY) {
        if (side == null || side.getAxis() == Direction.Axis.Y || rotationY == 0) return side;
        
        // Считаем шаги против часовой стрелки для отмены поворота
        int steps = (4 - (rotationY / 90)) % 4;
        Direction r = side;
        for (int i = 0; i < steps; i++) {
            r = r.getClockWise(Direction.Axis.Y);
        }
        return r;
    }

    private List<BakedQuad> getCachedTankQuads(BakedModel originalTank, ResourceLocation textureLocation, @Nullable Direction side, RandomSource rand) {
        final ResourceLocation safeTexture = textureLocation == null ? DEFAULT_TEX : textureLocation;
    
        Map<Object, List<BakedQuad>> directionalCache = quadCache.computeIfAbsent(safeTexture, k -> new ConcurrentHashMap<>());
    
        Object cacheKey = side == null ? NULL_SIDE_KEY : side;
    
        return directionalCache.computeIfAbsent(cacheKey, k -> {
            List<BakedQuad> newQuads = new ArrayList<>();
            List<BakedQuad> originalQuads = originalTank.getQuads(null, side, rand, ModelData.EMPTY, null);
            
            TextureAtlasSprite newSprite = Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(safeTexture);
    
            for (BakedQuad quad : originalQuads) {
                newQuads.add(retextureAndFixUV(quad, newSprite));
            }
            return newQuads;
        });
    }

    private BakedQuad retextureAndFixUV(BakedQuad original, TextureAtlasSprite newSprite) {
        int[] oldData = original.getVertices();
        int[] newData = new int[oldData.length];
        System.arraycopy(oldData, 0, newData, 0, oldData.length);

        TextureAtlasSprite oldSprite = original.getSprite();
        if (oldSprite == null) return original;

        float oldUDiff = oldSprite.getU1() - oldSprite.getU0();
        float oldVDiff = oldSprite.getV1() - oldSprite.getV0();

        float newUDiff = newSprite.getU1() - newSprite.getU0();
        float newVDiff = newSprite.getV1() - newSprite.getV0();

        if (oldUDiff == 0 || oldVDiff == 0) return original;

        int vertexSize = oldData.length / 4; 

        for (int i = 0; i < 4; i++) {
            int offset = i * vertexSize;
            
            float oldU = Float.intBitsToFloat(oldData[offset + 4]);
            float oldV = Float.intBitsToFloat(oldData[offset + 5]);

            float normU = (oldU - oldSprite.getU0()) / oldUDiff;
            float normV = (oldV - oldSprite.getV0()) / oldVDiff;

            float newU = newSprite.getU0() + (normU * newUDiff);
            float newV = newSprite.getV0() + (normV * newVDiff);

            newData[offset + 4] = Float.floatToRawIntBits(newU);
            newData[offset + 5] = Float.floatToRawIntBits(newV);
        }

        return new BakedQuad(newData, original.getTintIndex(), original.getDirection(), newSprite, original.isShade());
    }
}