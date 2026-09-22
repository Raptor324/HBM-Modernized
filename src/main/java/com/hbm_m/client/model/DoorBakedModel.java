package com.hbm_m.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.hbm_m.client.model.variant.DoorModelProperties;
import com.hbm_m.client.model.variant.DoorModelRegistry;
import com.hbm_m.client.model.variant.DoorModelType;
import com.hbm_m.client.model.variant.DoorSkin;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.block.decorations.DoorBlock;
import com.hbm_m.block.entity.doors.DoorDecl;
import com.hbm_m.block.entity.doors.DoorDeclRegistry;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.util.MultipartFacingTransforms;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

public class DoorBakedModel extends AbstractMultipartBakedModel implements AbstractMultipartBakedModel.PartNamesProvider {
    
    private final String[] cachedPartNames;
    private final ResourceLocation doorId;

    // Должно быть доступно и на Fabric (не внутри forge-only блоков).
    private static final String[] STATIC_PART_NAMES = {"frame", "Frame", "DoorFrame", "Base", "base"};
    
    // Кэш квадов для item рендера
    private List<BakedQuad> cachedItemQuads;
    private boolean itemQuadsCached = false;
    // Индекс скина, для которого собран кэш (автопрокрутка скинов раз в секунду)
    private int cachedItemSkinIndex = -1;
    
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
        // Геометрия всегда предоставляется BER/VBO системой.
        return state != null;
    }
    
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }
    
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                     RandomSource rand, ModelData modelData, 
                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
        // ITEM RENDER: Рендерим для GUI, руки, земли (state == null)
        if (state == null) {
            return getItemQuads(side, rand, modelData, renderType);
        }
        
        // WORLD RENDER: блокируем запекание в чанк, чтобы не было 2 моделей (рендер идет строго через BER/VBO)
        return Collections.emptyList();
    }
    
    private List<BakedQuad> getItemQuads(@Nullable Direction side, RandomSource rand,
                                          ModelData modelData,
                                          @Nullable net.minecraft.client.renderer.RenderType renderType) {
        int skinIndex = getCyclingSkinIndex();
        if (!itemQuadsCached || cachedItemSkinIndex != skinIndex) {
            buildItemQuads(rand, modelData, renderType, skinIndex);
            cachedItemSkinIndex = skinIndex;
            itemQuadsCached = true;
        }
        
        if (side != null) {
            return cachedItemQuads.stream()
                .filter(quad -> quad.getDirection() == side)
                .toList();
        }
        
        return cachedItemQuads;
    }
    
    private void buildItemQuads(RandomSource rand, ModelData modelData,
                                 @Nullable net.minecraft.client.renderer.RenderType renderType, int skinIndex) {
        List<BakedQuad> allQuads = new ArrayList<>();

        // DAE-скины (и любые не-DoorBakedModel варианты): их квадов нет в parts.
        // Берём запечённую модель из реестра напрямую — как это делает
        // DoorModelFakeItemRenderer в GUI выбора скина.
        BakedModel direct = resolveDirectItemModel(skinIndex);
        if (direct != null) {
            for (Direction dir : Direction.values()) {
                allQuads.addAll(direct.getQuads(null, dir, rand, modelData, renderType));
            }
            allQuads.addAll(direct.getQuads(null, null, rand, modelData, renderType));
            // Квады из getQuads НЕ несут display-трансформ. Обычный item-пайплайн
            // применит трансформ САМОЙ двери (this), а в GUI выбора скина эталонный
            // вид даёт трансформ DAE-модели скина. Подменяем один на другой:
            // DoorGUI · T(-0.5) · B = SkinGUI · T(-0.5)
            // => B = T(0.5) · DoorGUI⁻¹ · SkinGUI · T(-0.5)
            java.util.List<BakedQuad> transformed = applyGuiTransform(allQuads, direct);
            this.cachedItemQuads = transformed;
            return;
        }

        Map<String, BakedModel> partsToUse = getPartsForSkinIndex(skinIndex);
        List<String> itemRenderParts = getItemRenderPartNames();

        for (String partName : itemRenderParts) {
            BakedModel part = partsToUse.get(partName);
            if (part != null) {
                for (Direction dir : Direction.values()) {
                    allQuads.addAll(part.getQuads(null, dir, rand, modelData, renderType));
                }
                allQuads.addAll(part.getQuads(null, null, rand, modelData, renderType));
            }
        }

        this.cachedItemQuads = allQuads;
    }

    /**
     * Для варианта (0=LEGACY, 1..N=скины) возвращает запечённую модель напрямую,
     * если она НЕ DoorBakedModel (DAE и т.п.) — у таких вариантов нет частей.
     * DoorBakedModel-варианты рендерятся обычным путём через getPartsForSkinIndex.
     */
    @Nullable
    private BakedModel resolveDirectItemModel(int skinIndex) {
        String doorType = extractDoorTypeFromPath(doorId.getPath());
        DoorModelRegistry registry = DoorModelRegistry.getInstance();
        if (!registry.isRegistered(doorType)) return null;

        DoorModelSelection selection;
        if (skinIndex == 0) {
            selection = DoorModelSelection.legacy();
        } else {
            List<DoorSkin> skins = registry.getSkins(doorType);
            int skinIdx = skinIndex - 1;
            if (skinIdx >= skins.size()) return null;
            selection = new DoorModelSelection(DoorModelType.MODERN, skins.get(skinIdx));
        }

        ResourceLocation modelPath = registry.getModelPath(doorType, selection);
        if (modelPath == null) return null;

        //? if < 1.21.1 {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelPath);
        //?} else {
        /*BakedModel model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(modelPath));
        *///?}
        if (model == null || model == Minecraft.getInstance().getModelManager().getMissingModel()) return null;
        return (model instanceof DoorBakedModel) ? null : model;
    }

    /**
     * Подмена display-трансформа: квады DAE-скина будут отрендерены item-пайплайном
     * с трансформом этой двери ({@code DoorGUI}), а эталонный вид (GUI выбора скина,
     * {@link com.hbm_m.client.overlay.DoorModelFakeItemRenderer}) даёт трансформ
     * самой скиновой модели ({@code SkinGUI}). Запекаем разницу в квады:
     * <pre>DoorGUI · T(-0.5) · B = SkinGUI · T(-0.5)  =>  B = T(0.5) · DoorGUI⁻¹ · SkinGUI · T(-0.5)</pre>
     * Порядок операций внутри ItemTransform — 1:1 как в ItemTransform.apply(false, pose):
     * translate(/16) → rotationXYZ → scale.
     */
    private java.util.List<BakedQuad> applyGuiTransform(java.util.List<BakedQuad> quads, BakedModel direct) {
        net.minecraft.client.renderer.block.model.ItemTransform doorGui =
                this.getTransforms().getTransform(net.minecraft.world.item.ItemDisplayContext.GUI);
        net.minecraft.client.renderer.block.model.ItemTransform skinGui =
                direct.getTransforms().getTransform(net.minecraft.world.item.ItemDisplayContext.GUI);
        boolean doorIdentity = doorGui == null || doorGui == net.minecraft.client.renderer.block.model.ItemTransform.NO_TRANSFORM;
        boolean skinIdentity = skinGui == null || skinGui == net.minecraft.client.renderer.block.model.ItemTransform.NO_TRANSFORM;
        if (doorIdentity && skinIdentity) return quads;

        org.joml.Matrix4f m = new org.joml.Matrix4f().translation(0.5f, 0.5f, 0.5f);
        if (!doorIdentity) {
            m.mul(itemTransformMatrix(doorGui).invert());
        }
        if (!skinIdentity) {
            m.mul(itemTransformMatrix(skinGui));
        }
        m.translate(-0.5f, -0.5f, -0.5f);
        return ModelHelper.transformQuadsByMatrix(quads, m);
    }

    /** Матрица ItemTransform — повтор ItemTransform.apply(false, pose). */
    private static org.joml.Matrix4f itemTransformMatrix(net.minecraft.client.renderer.block.model.ItemTransform t) {
        org.joml.Matrix4f m = new org.joml.Matrix4f();
        m.translate(t.translation.x / 16f, t.translation.y / 16f, t.translation.z / 16f);
        m.rotateXYZ((float) Math.toRadians(t.rotation.x),
                    (float) Math.toRadians(t.rotation.y),
                    (float) Math.toRadians(t.rotation.z));
        m.scale(t.scale.x, t.scale.y, t.scale.z);
        return m;
    }

    /**
     * Автопрокрутка вариантов в item-рендере — по мотивам 1.7.10 DoorDecl.getCyclingSkins():
     * индекс = (мс % (кол-во вариантов * 1000)) / 1000, каждый вариант держится одну секунду, по кругу.
     * Индекс 0 - LEGACY модель, далее default и скины MODERN.
     */
    private int getCyclingSkinIndex() {
        String doorType = extractDoorTypeFromPath(doorId.getPath());
        int count = DoorModelRegistry.getInstance().getSkins(doorType).size() + 1; // +1 за LEGACY
        if (count <= 1) return 0;
        return (int) ((System.currentTimeMillis() % (count * 1000L)) / 1000L);
    }

    /**
     * Части модели для варианта с указанным индексом: 0 - LEGACY, 1..N - default и скины MODERN.
     */
    private Map<String, BakedModel> getPartsForSkinIndex(int index) {
        String doorType = extractDoorTypeFromPath(doorId.getPath());
        DoorModelRegistry registry = DoorModelRegistry.getInstance();
        if (!registry.isRegistered(doorType)) return parts;

        if (index == 0) {
            ResourceLocation legacyPath = registry.getModelPath(doorType, DoorModelSelection.legacy());
            if (legacyPath == null) return parts;
            //? if < 1.21.1 {
            BakedModel legacyModel = Minecraft.getInstance().getModelManager().getModel(legacyPath);
             //?} else {
            /*BakedModel legacyModel = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(legacyPath));
            *///?}
            if (legacyModel instanceof DoorBakedModel doorModel) {
                return doorModel.getParts();
            }
            return parts;
        }

        List<DoorSkin> skins = registry.getSkins(doorType);
        int skinIndex = index - 1;
        if (skinIndex >= skins.size()) return parts;

        ResourceLocation modelPath = registry.getModelPath(doorType,
                new DoorModelSelection(DoorModelType.MODERN, skins.get(skinIndex)));
        if (modelPath == null) return parts;

        //? if < 1.21.1 {
        BakedModel selectionModel = Minecraft.getInstance().getModelManager().getModel(modelPath);
         //?} else {
        /*BakedModel selectionModel = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(modelPath));
        *///?}

        if (selectionModel == null || selectionModel == Minecraft.getInstance().getModelManager().getMissingModel()) {
            return parts;
        }
        if (selectionModel instanceof DoorBakedModel doorModel) {
            return doorModel.getParts();
        }
        return parts;
    }
    
    /*
     * Получает части модели с учётом выбора (legacy/modern/skin).
     * Если в ModelData есть выбор и реестр имеет конфиг - используем модель из реестра.
     */
    private Map<String, BakedModel> getPartsForModelData(ModelData modelData) {
        var selection = modelData.get(DoorModelProperties.MODEL_SELECTION_PROPERTY);
        if (selection == null) return parts;

        String doorType = extractDoorTypeFromPath(doorId.getPath());
        DoorModelRegistry registry = DoorModelRegistry.getInstance();
        if (!registry.isRegistered(doorType)) return parts;

        ResourceLocation modelPath = registry.getModelPath(doorType, selection);
        if (modelPath == null) return parts;

        //? if < 1.21.1 {
        BakedModel selectionModel = Minecraft.getInstance().getModelManager().getModel(modelPath);
         //?} else {
        /*BakedModel selectionModel = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(modelPath));
        *///?}

        if (selectionModel == null || selectionModel == Minecraft.getInstance().getModelManager().getMissingModel()) {
            return parts;
        }
        if (selectionModel instanceof DoorBakedModel doorModel) {
            return doorModel.getParts();
        }
        return parts;
    }

    /*
     * Возвращает квады только статичных частей (frame) для Iris-пути при движущейся двери.
     * Подвижные части скрыты - их рендерит BER через putBulkData.
     */
    private List<BakedQuad> getStaticPartQuads(@Nullable BlockState state, @Nullable Direction side,
                                                RandomSource rand, ModelData modelData,
                                                @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> result = new ArrayList<>();
        int rotationY = getRotationYForFacing(state);
        Map<String, BakedModel> partsToUse = getPartsForModelData(modelData);
        
        for (String partName : STATIC_PART_NAMES) {
            BakedModel part = partsToUse.get(partName);
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
    
    /*
     * Возвращает квады всех частей для Iris-пути при статичной двери.
     * Створка (анимированные части) трансформируется в позицию open/closed.
     */
    private List<BakedQuad> getAllPartQuads(@Nullable BlockState state, @Nullable Direction side,
                                            RandomSource rand, ModelData modelData,
                                            @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (state == null) return Collections.emptyList();

        DoorDecl doorDecl = DoorDeclRegistry.getById(extractDoorTypeFromPath(doorId.getPath()));
        if (doorDecl == null) return Collections.emptyList();

        // OPEN: BlockState обновляется первым, ModelData - при packet BlockEntity
        boolean isOpen = state.hasProperty(DoorBlock.OPEN) && state.getValue(DoorBlock.OPEN);
        Boolean openFromData = modelData.get(DoorModelProperties.OPEN_PROPERTY);
        if (openFromData != null) isOpen = openFromData;
        float openTicks = isOpen ? doorDecl.getOpenTime() : 0f;

        int rotationY = getRotationYForFacing(state);

        Map<String, BakedModel> partsToUse = getPartsForModelData(modelData);
        String[] partNamesToUse = partsToUse.keySet().toArray(new String[0]);

        List<BakedQuad> allQuads = new ArrayList<>();
        java.util.Map<String, Matrix4f> transformCache = new java.util.HashMap<>();
        for (String partName : partNamesToUse) {
            BakedModel part = partsToUse.get(partName);
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
                DoorModelSelection selection = modelData.get(DoorModelProperties.MODEL_SELECTION_PROPERTY);
                Matrix4f transform = buildPartTransformWithParent(doorDecl, partName, openTicks, partNamesToUse, transformCache, selection);
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
        // Убираем расширение .obj - модель загружается как models/block/doors/fire_door.obj
        int dot = base.lastIndexOf('.');
        return dot > 0 ? base.substring(0, dot) : base;
    }

    /**
     * Угол Y для chunk/Iris квадов двери (отдельная таблица углов + offset из {@link DoorDecl}).
     */
    private int getRotationYForFacing(BlockState state) {
        if (!state.hasProperty(DoorBlock.FACING)) {
            return 90;
        }
        DoorDecl doorDecl = DoorDeclRegistry.getById(extractDoorTypeFromPath(doorId.getPath()));
        int offset = doorDecl != null ? doorDecl.getBakedModelRotationOffsetY() : 0;
        return MultipartFacingTransforms.doorChunkMeshRotationY(
            state.getValue(DoorBlock.FACING), offset);
    }

    /**
     * Строит матрицу трансформации с учётом иерархии (water_door: spinny_upper/lower - дети door).
     * Дочерние части умножаются на трансформацию родителя, чтобы двигаться вместе со створкой.
     */
    @Nullable
    private static Matrix4f buildPartTransformWithParent(DoorDecl doorDecl, String partName,
            float openTicks, String[] allPartNames,
            java.util.Map<String, Matrix4f> transformCache, DoorModelSelection selection) {
        String parentName = findParent(doorDecl, partName, allPartNames, selection);
        Matrix4f parentMat = null;
        if (parentName != null) {
            parentMat = transformCache.get(parentName);
            if (parentMat == null) {
                parentMat = buildPartTransformWithParent(doorDecl, parentName, openTicks, allPartNames, transformCache, selection);
                if (parentMat != null) transformCache.put(parentName, parentMat);
            }
        }
        Matrix4f mat = buildPartTransformMatrix(doorDecl, partName, openTicks, parentName != null, selection);
        if (mat == null) return null;
        if (parentMat != null) {
            mat = new Matrix4f(parentMat).mul(mat);
        }
        transformCache.put(partName, mat);
        return mat;
    }

    @Nullable
    private static String findParent(DoorDecl doorDecl, String partName, String[] allPartNames, DoorModelSelection selection) {
        for (String p : allPartNames) {
            for (String c : doorDecl.getChildren(p, selection)) {
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
            float openTicks, boolean child, DoorModelSelection selection) {
        float[] origin = new float[3];
        float[] rotation = new float[3];
        float[] translation = new float[3];
        doorDecl.getOrigin(partName, origin, selection);
        doorDecl.getRotation(partName, openTicks, rotation, selection);
        doorDecl.getTranslation(partName, openTicks, child, translation, selection);

        Matrix4f mat = new Matrix4f();
        mat.translate(origin[0], origin[1], origin[2]);
        if (rotation[0] != 0) mat.rotateX((float) Math.toRadians(rotation[0]));
        if (rotation[1] != 0) mat.rotateY((float) Math.toRadians(rotation[1]));
        if (rotation[2] != 0) mat.rotateZ((float) Math.toRadians(rotation[2]));
        mat.translate(-origin[0] + translation[0], -origin[1] + translation[1], -origin[2] + translation[2]);
        return mat;
    }
    
    
    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    // В 1.20+ ItemOverrides имеет приватный конструктор, поэтому кастомные overrides недоступны.

    // Без ветки neoforge слой падал в solid, и прозрачные участки рисовались непрозрачными.
    //? if forge || neoforge {
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        // cutoutMipped для прозрачных текстур (стекло, решётки и т.д.)
        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }
    //?}

    @Override
    public TextureAtlasSprite getParticleIcon() {
        //? if forge {
        return getParticleIcon(ModelData.EMPTY);
        //?}


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
