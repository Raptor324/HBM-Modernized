package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.model.MachineFluidTankBakedModel;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.main.MainRegistry;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?}

/**
 * VBO-рендер Fluid Tank (аналог {@link MachineAssemblerVboRenderer}).
 * <p>
 * Frame — статический меш через {@link MeshRenderCache#getOrCreateRenderer} (без смены текстуры).
 * Tank — меш с подменой текстуры жидкости: оригинальные квады Tank ретекстуризируются под
 * спрайт {@code block/tank/tank_<fluid>} и кешируются в {@link MeshRenderCache} по ключу
 * {@code "fluid_tank_Tank:" + texturePath}. Это даёт один VBO на уникальную текстуру жидкости,
 * переиспользуемый всеми баками с той же жидкостью (как chunk-bake кеш в старой baked-модели).
 * <p>
 * Логика смены текстуры: {@link com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity#getTankTextureLocation()}
 * — возвращает {@code block/tank/tank_none} для пустого бака и {@code block/tank/tank_<fluid>}
 * для заполненного. При смене жидкости BE дёргает {@code requestModelDataUpdate} (Forge) /
 * {@code scheduleChunkRebuild} (Fabric), но т.к. теперь chunk mesh пуст, перерисовку инициирует
 * BER на следующем кадре, выбирая свежий {@code fluidTex}.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MachineFluidTankVboRenderer {

    private static final String FRAME = "Frame";
    private static final String TANK = "Tank";

    /** Ключ VBO для статического Frame-меша (не зависит от жидкости). */
    private static final String FRAME_KEY = "fluid_tank_" + FRAME;
    /** Префикс ключа VBO для Tank-меша; суффикс = путь текстуры жидкости. */
    private static final String TANK_KEY_PREFIX = "fluid_tank_" + TANK + ":";

    private final MachineFluidTankBakedModel model;

    /**
     * Кеш ретекстурированных Tank-рендереров по текстуре жидкости.
     * VBO живёт в {@link MeshRenderCache}, здесь — только мягкие ссылки на ключи,
     * чтобы не пересобирать ретекстурированные квады каждый кадр.
     * Чистится в {@link #clearTankTextureCache()} при reload/disconnect.
     */
    private static final Map<ResourceLocation, String> TANK_TEXTURE_KEYS = new ConcurrentHashMap<>();

    public MachineFluidTankVboRenderer(MachineFluidTankBakedModel model) {
        this.model = model;
    }

    /** Рендер статического Frame (VBO из {@link MeshRenderCache}). */
    public void renderFrame(PoseStack poseStack, int packedLight, BlockPos blockPos,
                            @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(FRAME);
        if (part == null) return;
        var r = MeshRenderCache.getOrCreateRenderer(FRAME_KEY, part);
        if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
    }

    /**
     * Рендер Tank-меша с текстурой жидкости {@code fluidTex}.
     * VBO кешируется в {@link MeshRenderCache} по {@code "fluid_tank_Tank:" + fluidTex.getPath()}.
     */
    public void renderTank(PoseStack poseStack, int packedLight, BlockPos blockPos,
                           ResourceLocation fluidTex,
                           @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel tankPart = model.getPart(TANK);
        if (tankPart == null) return;

        String key = TANK_TEXTURE_KEYS.computeIfAbsent(fluidTex, t -> TANK_KEY_PREFIX + t.getPath());

        SingleMeshVboRenderer r = MeshRenderCache.getOrCreateRenderer(key, tankPart);
        if (r != null) {
            // VBO для этого ключа уже собран из оригинальных квадов Tank — но нам нужно
            // ретекстуризировать под fluidTex. MeshRenderCache.getOrCreateRenderer(part, model)
            // строит VBO из «сырых» квадов (дефолтная текстура). Для подмены текстуры мы
            // НЕ можем менять уже загруженный VBO — вместо этого используем отдельный кеш
            // ретекстуризированных мешей через getOrCreateRendererFromQuads.
            // См. renderTankRetextured ниже — этот метод оставлен как fallback/диагностика.
            r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    /**
     * Рендер Tank-меша с подменой текстуры жидкости. Квады Tank ретекстуризируются под
     * {@code fluidTex} и кешируются в {@link MeshRenderCache} через
     * {@link MeshRenderCache#getOrCreateRendererFromQuads}. Один VBO на уникальную текстуру.
     */
    public void renderTankRetextured(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                     ResourceLocation fluidTex,
                                     @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel tankPart = model.getPart(TANK);
        if (tankPart == null) return;

        ResourceLocation safeTex = fluidTex == null ? defaultTankTexture() : fluidTex;
        String key = TANK_TEXTURE_KEYS.computeIfAbsent(safeTex, t -> TANK_KEY_PREFIX + t.getPath());

        // Проверяем, есть ли уже VBO в кеше MeshRenderCache. getOrCreateRendererFromQuads
        // требует явный список квадов, но кеш по ключу переиспользует уже собранный VBO.
        SingleMeshVboRenderer existing = MeshRenderCache.peekRenderer(key);
        if (existing == null) {
            List<BakedQuad> retextured = buildRetexturedTankQuads(tankPart, safeTex);
            if (retextured.isEmpty()) return;
            existing = MeshRenderCache.getOrCreateRendererFromQuadList(key, retextured);
        }
        if (existing != null) {
            existing.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    /** Дефолтная текстура пустого бака. */
    private static ResourceLocation defaultTankTexture() {
        //? if fabric && < 1.21.1 {
        /*return new ResourceLocation(MainRegistry.MOD_ID, "block/tank/tank_none");
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/tank/tank_none");
        //?}
    }

    /**
     * Собирает квады Tank с подменой спрайта на {@code fluidTex}.
     * Нормализация UV: оригинальные UV (в координатах дефолтного спрайта) переносятся
     * в нормализованное [0,1] пространство, затем маппятся на новый спрайт.
     * Аналог {@code retextureAndFixUV} из старой {@link MachineFluidTankBakedModel}.
     */
    private static List<BakedQuad> buildRetexturedTankQuads(BakedModel tankPart, ResourceLocation textureLoc) {
        //? if forge {
        List<BakedQuad> original = collectAllQuads(tankPart);
        if (original.isEmpty()) return List.of();

        TextureAtlasSprite newSprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(textureLoc);

        List<BakedQuad> result = new ArrayList<>(original.size());
        for (BakedQuad quad : original) {
            result.add(retextureAndFixUV(quad, newSprite));
        }
        return result;
        //?}

        //? if fabric {
        /*List<BakedQuad> original = collectAllQuads(tankPart);
        if (original.isEmpty()) return List.of();

        TextureAtlasSprite newSprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(textureLoc);

        List<BakedQuad> result = new ArrayList<>(original.size());
        for (BakedQuad quad : original) {
            result.add(retextureAndFixUV(quad, newSprite));
        }
        return result;
        *///?}

        //? if neoforge {
        /*List<BakedQuad> original = collectAllQuads(tankPart);
        if (original.isEmpty()) return List.of();

        TextureAtlasSprite newSprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(textureLoc);

        List<BakedQuad> result = new ArrayList<>(original.size());
        for (BakedQuad quad : original) {
            result.add(retextureAndFixUV(quad, newSprite));
        }
        return result;
        *///?}
    }

    /** Сбор всех квадов части (все стороны + null side, neutral RandomSource). */
    //? if forge {
    private static List<BakedQuad> collectAllQuads(BakedModel part) {
        RandomSource rand = RandomSource.create(42);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(part.getQuads(null, dir, rand, ModelData.EMPTY, null));
        }
        quads.addAll(part.getQuads(null, null, rand, ModelData.EMPTY, null));
        return quads;
    }
    //?}

    //? if fabric {
    /*private static List<BakedQuad> collectAllQuads(BakedModel part) {
        RandomSource rand = RandomSource.create(42);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(part.getQuads(null, dir, rand));
        }
        quads.addAll(part.getQuads(null, null, rand));
        return quads;
    }
    *///?}

    //? if neoforge {
    /*// NeoForge 1.21.1: vanilla 3-arg getQuads (BlockState, Direction, RandomSource).
    private static List<BakedQuad> collectAllQuads(BakedModel part) {
        RandomSource rand = RandomSource.create(42);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(part.getQuads(null, dir, rand));
        }
        quads.addAll(part.getQuads(null, null, rand));
        return quads;
    }
    *///?}

    /**
     * Перенос UV квада со старого спрайта на новый.
     * Формат вершины BLOCK: 8 int (x,y,z,_,u,v,_,_). u,v — float bits в [4] и [5].
     */
    private static BakedQuad retextureAndFixUV(BakedQuad original, TextureAtlasSprite newSprite) {
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

    /**
     * Сброс кеша ключей текстур Tank. VBO чистится в {@link MeshRenderCache#clearAll()}.
     * Вызывается из {@link MachineFluidTankRenderer#clearCaches()}.
     */
    public static void clearTankTextureCache() {
        TANK_TEXTURE_KEYS.clear();
    }
}
