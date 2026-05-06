package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.block.machines.MachineFluidTankBlock;
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
//? if forge {
/*import net.minecraftforge.client.model.data.ModelData;
*///?}

public class MachineFluidTankBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {

    private final Map<ResourceLocation, Map<Object, List<BakedQuad>>> quadCache = new ConcurrentHashMap<>();

    private static final Object NULL_SIDE_KEY = new Object();
    
    // Текстура по умолчанию (если пустой бак)
    //? if fabric && < 1.21.1 {
    private static final ResourceLocation DEFAULT_TEX = new ResourceLocation("hbm_m", "block/tank/tank_none");
    //?} else {
        /*private static final ResourceLocation DEFAULT_TEX = ResourceLocation.fromNamespaceAndPath("hbm_m", "block/tank/tank_none");
    *///?}


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

    //? if forge {
    /*@Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData modelData, @Nullable RenderType renderType) {
        return getQuadsInternal(state, side, rand, modelData, renderType);
    }
    *///?}

    //? if fabric {
    /**
     * Текстура «стакана» берётся из {@link MachineFluidTankBlockEntity#getRenderAttachmentData()}
     * через {@link FabricRenderDataBridge} на этапе сборки чанка (см. mixins на ModelBlockRenderer / Sodium).
     * При смене типа жидкости клиентский {@code BlockEntity.load()} должен планировать пересборку чанка,
     * иначе кэш меша Sodium останется со старыми UV.
     */
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuadsInternal(state, side, rand, FabricRenderDataBridge.get(), null);
    }
    //?}

    // Общая внутренняя логика
    private List<BakedQuad> getQuadsInternal(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, @Nullable Object modelDataObj, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        int rotationY = getRotationYForFacing(state);
        Direction querySide = getUnrotatedSide(side, rotationY);

        // Рама
        BakedModel frame = getPart("Frame");
        if (frame != null) {
            //? if forge {
            /*quads.addAll(ModelHelper.transformQuadsByFacing(frame.getQuads(state, querySide, rand, (ModelData)modelDataObj, renderType), rotationY));
            *///?} else {
            quads.addAll(ModelHelper.transformQuadsByFacing(frame.getQuads(state, querySide, rand), rotationY));
             //?}
        }

        // Бак
        BakedModel tank = getPart("Tank");
        if (tank != null) {
            ResourceLocation fluidTex = DEFAULT_TEX;

            //? if forge {
            /*ModelData modelData = (ModelData) modelDataObj;
            if (modelData != null && modelData.has(MachineFluidTankBlockEntity.FLUID_TEXTURE_PROPERTY)) {
                fluidTex = modelData.get(MachineFluidTankBlockEntity.FLUID_TEXTURE_PROPERTY);
            }
            *///?}

            //? if fabric {
            if (modelDataObj instanceof ResourceLocation fabricTex) {
                fluidTex = fabricTex;
            }
            //?}

            List<BakedQuad> tankQuads = getCachedTankQuads(tank, fluidTex, querySide, rand);
            quads.addAll(ModelHelper.transformQuadsByFacing(tankQuads, rotationY));
        }

        return quads;
    }

    //? if forge {
    /*@Override
    protected List<BakedQuad> getQuadsForModelData(
        @Nullable BlockState state,
        @Nullable Direction side,
        RandomSource rand,
        ModelData modelData,
        @Nullable RenderType renderType
    ) {
        return getQuadsInternal(state, side, rand, modelData, renderType);
    }
    *///?}

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

    private List<BakedQuad> getCachedTankQuads(
            BakedModel originalTank,
            ResourceLocation textureLocation,
            @Nullable Direction side,
            RandomSource rand
    ) {
        final ResourceLocation safeTexture = textureLocation == null ? DEFAULT_TEX : textureLocation;

        //? if forge {

        /*List<BakedQuad> result = new ArrayList<>();
        List<BakedQuad> originalQuads = originalTank.getQuads(null, side, rand, ModelData.EMPTY, null);
        TextureAtlasSprite newSprite = Minecraft.getInstance()
            .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
            .apply(safeTexture);
        for (BakedQuad quad : originalQuads) {
            result.add(retextureAndFixUV(quad, newSprite));
        }
        return result;
    *///?}

        //? if fabric {
        Map<Object, List<BakedQuad>> directionalCache =
                quadCache.computeIfAbsent(safeTexture, k -> new ConcurrentHashMap<>());
        Object cacheKey = side == null ? NULL_SIDE_KEY : side;

        return directionalCache.computeIfAbsent(cacheKey, k -> {
            List<BakedQuad> newQuads = new ArrayList<>();
            List<BakedQuad> originalQuads = originalTank.getQuads(null, side, rand);
            TextureAtlasSprite newSprite = Minecraft.getInstance()
                    .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                    .apply(safeTexture);
            for (BakedQuad quad : originalQuads) {
                newQuads.add(retextureAndFixUV(quad, newSprite));
            }
            return newQuads;
        });
        //?}
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