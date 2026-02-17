package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.block.custom.decorations.DoorBlock;
import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.block.entity.custom.doors.DoorDecl;
import com.hbm_m.block.entity.custom.doors.DoorDeclRegistry;
import com.hbm_m.client.loader.ColladaAnimationData;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.config.ModClothConfig;

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
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

public class DoorBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {
    
    private final String[] cachedPartNames;
    private final ResourceLocation doorId;
    
    // Кэш квадов для item рендера
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;
    
    public DoorBakedModel(Map<String, BakedModel> parts, ItemTransforms transforms, ResourceLocation doorId) {
        super(parts, transforms);
        this.doorId = doorId;
        
        // Кешируем имена частей из JSON
        this.cachedPartNames = parts.keySet().toArray(new String[0]);
    }

    
    @Override
    public String[] getPartNames() {
        return cachedPartNames;
    }
    
    public Map<String, BakedModel> getParts() {
        return parts;
    }
    
    @Override
    protected boolean shouldSkipWorldRendering(@Nullable BlockState state) {
        // Пропускаем world рендер ТОЛЬКО когда нет шейдера (VBO путь)
        // При активном шейдере - рендерим через baked geometry
        return state != null && !ShaderCompatibilityDetector.isExternalShaderActive();
    }
    
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }
    
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData, 
                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // ITEM RENDER: Рендерим для GUI, руки, земли и тд
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }
        
        // WORLD RENDER - переключение по стороннему шейдеру (Iris/Oculus)
        // Нет шейдера → модель пустая, всё рендерит BER (VBO).
        // Шейдер активен → рендерим через baked geometry, кроме анимированных частей (движущаяся дверь)
        if (!ShaderCompatibilityDetector.isExternalShaderActive()) {
            return Collections.emptyList();
        }
        
        // Движется ли дверь: BlockState обновляется первым (при block update), ModelData — при packet BlockEntity
        boolean isMoving = state.hasProperty(DoorBlock.DOOR_MOVING) && state.getValue(DoorBlock.DOOR_MOVING);
        Boolean movingFromData = modelData.get(DoorBlockEntity.DOOR_MOVING_PROPERTY);
        if (movingFromData != null) isMoving = movingFromData;
        Boolean isOverlap = modelData.get(DoorBlockEntity.OVERLAP_PROPERTY);
        // Период overlap: дверь в open/closed, baked model и BER наслаиваются — устраняет моргание
        if (Boolean.TRUE.equals(isOverlap) || !isMoving) {
            return getAllPartQuads(state, side, rand, modelData, renderType);
        }
        // Дверь реально анимируется (state 2/3) — только frame, створки рисует BER
        return getStaticPartQuads(state, side, rand, modelData, renderType);
    }
    
    /**
     * Возвращает квады только статичных частей (frame) для Iris-пути при движущейся двери.
     * Подвижные части скрыты — их рендерит BER через putBulkData.
     */
    private List<BakedQuad> getStaticPartQuads(@Nullable BlockState state, @Nullable Direction side,
                                                RandomSource rand, ModelData modelData,
                                                @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> result = new ArrayList<>();
        int rotationY = getRotationYForFacing(state);
        
        for (String partName : STATIC_PART_NAMES) {
            BakedModel part = parts.get(partName);
            if (part == null) continue;
            List<BakedQuad> partQuads = new ArrayList<>();
            for (Direction d : Direction.values()) {
                partQuads.addAll(part.getQuads(state, d, rand, modelData, renderType));
            }
            partQuads.addAll(part.getQuads(state, null, rand, modelData, renderType));
            if (!partQuads.isEmpty()) {
                List<BakedQuad> translated = ModelHelper.translateQuads(partQuads, 0.5f, 0f, 0.5f);
                List<BakedQuad> rotated = ModelHelper.transformQuadsByFacing(translated, rotationY);
                if (side != null) {
                    for (BakedQuad q : rotated) {
                        if (q.getDirection() == side) result.add(q);
                    }
                } else {
                    result.addAll(rotated);
                }
            }
        }
        return result;
    }
    
    private static final String[] STATIC_PART_NAMES = {"frame", "Frame", "DoorFrame", "Base", "base"};

    /**
     * Возвращает квады всех частей для Iris-пути при статичной двери.
     * Створка (анимированные части) трансформируется в позицию open/closed.
     */
    private List<BakedQuad> getAllPartQuads(@Nullable BlockState state, @Nullable Direction side,
                                            RandomSource rand, ModelData modelData,
                                            @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) return Collections.emptyList();

        DoorDecl doorDecl = DoorDeclRegistry.getById(extractDoorTypeFromPath(doorId.getPath()));
        if (doorDecl == null) return Collections.emptyList();

        // OPEN: BlockState обновляется первым, ModelData — при packet BlockEntity
        boolean isOpen = state.hasProperty(DoorBlock.OPEN) && state.getValue(DoorBlock.OPEN);
        Boolean openFromData = modelData.get(DoorBlockEntity.OPEN_PROPERTY);
        if (openFromData != null) isOpen = openFromData;
        float openTicks = isOpen ? doorDecl.getOpenTime() : 0f;

        int rotationY = getRotationYForFacing(state);

        ColladaAnimationData animData = null;
        if (ModClothConfig.get().useColladaDoorAnimations && doorDecl.getColladaAnimationSource() != null) {
            var mc = Minecraft.getInstance();
            if (mc.getResourceManager() != null) {
                animData = ColladaAnimationData.getOrLoad(mc.getResourceManager(), doorDecl.getColladaAnimationSource());
            }
        }

        List<BakedQuad> allQuads = new ArrayList<>();
        java.util.Map<String, Matrix4f> transformCache = new java.util.HashMap<>();
        for (String partName : cachedPartNames) {
            BakedModel part = parts.get(partName);
            if (part == null) continue;

            List<BakedQuad> partQuads = new ArrayList<>();
            for (Direction d : Direction.values()) {
                partQuads.addAll(part.getQuads(state, d, rand, modelData, renderType));
            }
            partQuads.addAll(part.getQuads(state, null, rand, modelData, renderType));

            if (partQuads.isEmpty()) continue;

            if (isStaticPart(partName)) {
                List<BakedQuad> translated = ModelHelper.translateQuads(partQuads, 0.5f, 0f, 0.5f);
                allQuads.addAll(ModelHelper.transformQuadsByFacing(translated, rotationY));
            } else {
                Matrix4f transform = buildPartTransformWithParent(doorDecl, partName, openTicks, animData, cachedPartNames, transformCache);
                if (transform != null) {
                    partQuads = ModelHelper.transformQuadsByMatrix(partQuads, transform);
                }
                List<BakedQuad> translated = ModelHelper.translateQuads(partQuads, 0.5f, 0f, 0.5f);
                allQuads.addAll(ModelHelper.transformQuadsByFacing(translated, rotationY));
            }
        }
        return allQuads;
    }

    private static boolean isStaticPart(String partName) {
        for (String s : STATIC_PART_NAMES) {
            if (s.equals(partName)) return true;
        }
        return false;
    }

    private static String extractDoorTypeFromPath(String path) {
        String base;
        if (path.contains("block/doors/")) {
            base = path.substring(path.indexOf("block/doors/") + "block/doors/".length());
        } else {
            base = path.substring(Math.max(0, path.lastIndexOf('/') + 1));
        }
        // Убираем расширение .obj — модель загружается как models/block/doors/fire_door.obj
        int dot = base.lastIndexOf('.');
        return dot > 0 ? base.substring(0, dot) : base;
    }

    /**
     * Угол Y для выравнивания baked-модели с BER (VBO).
     * BER: translate(0.5,0,0.5) + rotate(90°) + facing + doOffsetTransform.
     * Должен совпадать с setupBlockTransform + doorDecl.doOffsetTransform.
     */
    private int getRotationYForFacing(BlockState state) {
        if (!state.hasProperty(DoorBlock.FACING)) return 90;
        int facingDeg = switch (state.getValue(DoorBlock.FACING)) {
            case SOUTH -> 0;
            case WEST -> 90;
            case EAST -> 270;
            default -> 180;
        };
        DoorDecl doorDecl = DoorDeclRegistry.getById(extractDoorTypeFromPath(doorId.getPath()));
        int offset = doorDecl != null ? doorDecl.getBakedModelRotationOffsetY() : 0;
        return (90 + facingDeg + offset + 360) % 360;
    }

    /**
     * Строит матрицу трансформации с учётом иерархии (water_door: spinny_upper/lower — дети door).
     * Дочерние части умножаются на трансформацию родителя, чтобы двигаться вместе со створкой.
     */
    @Nullable
    private static Matrix4f buildPartTransformWithParent(DoorDecl doorDecl, String partName,
            float openTicks, ColladaAnimationData animData, String[] allPartNames,
            java.util.Map<String, Matrix4f> transformCache) {
        String parentName = findParent(doorDecl, partName, allPartNames);
        Matrix4f parentMat = null;
        if (parentName != null) {
            parentMat = transformCache.get(parentName);
            if (parentMat == null) {
                parentMat = buildPartTransformWithParent(doorDecl, parentName, openTicks, animData, allPartNames, transformCache);
                if (parentMat != null) transformCache.put(parentName, parentMat);
            }
        }
        Matrix4f mat = buildPartTransformMatrix(doorDecl, partName, openTicks, parentName != null, animData);
        if (mat == null) return null;
        if (parentMat != null) {
            mat = new Matrix4f(parentMat).mul(mat);
        }
        transformCache.put(partName, mat);
        return mat;
    }

    @Nullable
    private static String findParent(DoorDecl doorDecl, String partName, String[] allPartNames) {
        for (String p : allPartNames) {
            for (String c : doorDecl.getChildren(p)) {
                if (c.equals(partName)) return p;
            }
        }
        return null;
    }

    /**
     * Строит матрицу трансформации для части двери (по аналогии с doPartTransform в DoorRenderer).
     */
    @Nullable
    private static Matrix4f buildPartTransformMatrix(DoorDecl doorDecl, String partName,
            float openTicks, boolean child, ColladaAnimationData animData) {
        float[] origin = new float[3];
        float[] rotation = new float[3];
        float[] translation = new float[3];
        doorDecl.getOrigin(partName, origin);
        doorDecl.getRotation(partName, openTicks, rotation);
        doorDecl.getTranslation(partName, openTicks, child, translation);

        Matrix4f mat = new Matrix4f();
        mat.translate(origin[0], origin[1], origin[2]);
        if (rotation[0] != 0) mat.rotateX((float) Math.toRadians(rotation[0]));
        if (rotation[1] != 0) mat.rotateY((float) Math.toRadians(rotation[1]));
        if (rotation[2] != 0) mat.rotateZ((float) Math.toRadians(rotation[2]));
        mat.translate(-origin[0] + translation[0], -origin[1] + translation[1], -origin[2] + translation[2]);

        if (animData != null && animData.getDurationSeconds() > 0) {
            String daeName = doorDecl.getDaeObjectName(partName);
            float normProgress = Math.min(1f, openTicks / Math.max(1, doorDecl.getOpenTime()));
            float timeSec = doorDecl.isColladaAnimationInverted()
                    ? (1f - normProgress) * animData.getDurationSeconds()
                    : normProgress * animData.getDurationSeconds();
            Matrix4f daeMatrix = animData.getTransformMatrix(daeName, timeSec);
            if (daeMatrix != null) {
                mat.mul(daeMatrix);
            }
        }
        return mat;
    }
    
    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand,
                                          ModelData modelData, 
                                          @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // Кэшируем квады для item рендера
        if (!itemQuadsCached) {
            buildItemQuads(rand, modelData, renderType);
            itemQuadsCached = true;
        }
        
        // Фильтруем по стороне если нужно
        if (side != null) {
            return cachedItemQuads.stream()
                .filter(quad -> quad.getDirection() == side)
                .toList();
        }
        
        return cachedItemQuads;
    }
    
    private void buildItemQuads(RandomSource rand, ModelData modelData, 
                                 @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> allQuads = new ArrayList<>();
        
        // Используем правильный порядок частей из базового класса
        List<String> itemRenderParts = getItemRenderPartNames();
        
        for (String partName : itemRenderParts) {
            BakedModel part = parts.get(partName);
            if (part != null) {
                // Добавляем все квады этой части
                for (Direction dir : Direction.values()) {
                    List<BakedQuad> partQuads = part.getQuads(null, dir, rand, modelData, renderType);
                    allQuads.addAll(partQuads);
                }
                
                // Добавляем квады без направления (cullface == null)
                List<BakedQuad> generalQuads = part.getQuads(null, null, rand, modelData, renderType);
                allQuads.addAll(generalQuads);
            }
        }
        
        this.cachedItemQuads = allQuads;
    }
    
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        // cutoutMipped для прозрачных текстур (стекло, решётки и т.д.)
        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
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
}
