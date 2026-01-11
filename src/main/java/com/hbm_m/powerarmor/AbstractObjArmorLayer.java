package com.hbm_m.powerarmor;

import com.hbm_m.client.model.AbstractMultipartBakedModel;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Абстрактный базовый класс для рендеринга OBJ-брони.
 * Содержит всю общую логику рендеринга, оставляя подклассам только конфигурацию.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractObjArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {

    protected static final float PIXEL_SCALE = 1.0F / 16.0F;
    protected static final float DEFAULT_Z_FIGHTING_SCALE = 1.015F; 
    private static final int MAX_PIVOT_CACHE_SIZE = 100; 

    private static final Map<ModelResourceLocation, BakedModel> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BakedModel> PART_CACHE = new ConcurrentHashMap<>();

    protected final IArmorLayerConfig config;
    
    private final Map<String, Vec3> basePivots = Collections.synchronizedMap(
        new LinkedHashMap<String, Vec3>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Vec3> eldest) {
                return size() > MAX_PIVOT_CACHE_SIZE;
            }
        }
    );

    public AbstractObjArmorLayer(RenderLayerParent<T, M> parent) {
        super(parent);
        this.config = createConfig();
    }

    protected abstract IArmorLayerConfig createConfig();

    @Override
    public final void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight,
                           @NotNull T entity, float limbSwing, float limbSwingAmount, float partialTick,
                           float ageInTicks, float netHeadYaw, float headPitch) {
        
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.HEAD);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.CHEST);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.LEGS);
        renderSlot(poseStack, buffer, packedLight, entity, EquipmentSlot.FEET);
    }

    protected final void renderSlot(PoseStack poseStack, MultiBufferSource buffer, int light, T entity, EquipmentSlot slot) {
        if (!Minecraft.getInstance().isSameThread()) return;

        ItemStack stack = entity.getItemBySlot(slot);
        if (!config.isItemValid(stack)) return;

        ModelResourceLocation modelLocation = config.getBakedModelLocation();
        BakedModel baked = MODEL_CACHE.computeIfAbsent(modelLocation, loc -> 
            Minecraft.getInstance().getModelManager().getModel(loc)
        );
        
        if (baked == Minecraft.getInstance().getModelManager().getMissingModel()) return;
        if (!(baked instanceof AbstractMultipartBakedModel multipart)) return;

        M parentModel = this.getParentModel();
        final boolean crouching = entity.isCrouching();

        switch (slot) {
            case HEAD -> renderPart(poseStack, buffer, light, getCachedPart(multipart, modelLocation, "Helmet"), parentModel.head, "Helmet", crouching);
            case CHEST -> {
                renderPart(poseStack, buffer, light, getCachedPart(multipart, modelLocation, "Chest"), parentModel.body, "Chest", crouching);
                renderPart(poseStack, buffer, light, getCachedPart(multipart, modelLocation, "RightArm"), parentModel.rightArm, "RightArm", crouching);
                renderPart(poseStack, buffer, light, getCachedPart(multipart, modelLocation, "LeftArm"), parentModel.leftArm, "LeftArm", crouching);
            }
            case LEGS -> {
                renderPart(poseStack, buffer, light, getCachedPart(multipart, modelLocation, "RightLeg"), parentModel.rightLeg, "RightLeg", crouching);
                renderPart(poseStack, buffer, light, getCachedPart(multipart, modelLocation, "LeftLeg"), parentModel.leftLeg, "LeftLeg", crouching);
            }
            case FEET -> {
                renderPart(poseStack, buffer, light, getCachedPart(multipart, modelLocation, "RightBoot"), parentModel.rightLeg, "RightBoot", crouching);
                renderPart(poseStack, buffer, light, getCachedPart(multipart, modelLocation, "LeftBoot"), parentModel.leftLeg, "LeftBoot", crouching);
            }
            default -> {}
        }
    }

    private BakedModel getCachedPart(AbstractMultipartBakedModel multipart, ModelResourceLocation modelLocation, String partName) {
        String cacheKey = modelLocation.toString() + ":" + partName;
        return PART_CACHE.computeIfAbsent(cacheKey, key -> multipart.getPart(partName));
    }

    protected final void renderPart(PoseStack poseStack, MultiBufferSource buffer, int light, BakedModel partModel, ModelPart bone, String partName, boolean crouching) {
        if (partModel == null) return;
        Material mat = config.getPartMaterials().get(partName);
        if (mat == null) return;

        final VertexConsumer vc;
        try {
            // Возвращаем стандартный RenderType для брони
            // Т.к. оверлей больше не гадит в GL контекст, это должно работать идеально
            vc = buffer.getBuffer(RenderType.armorCutoutNoCull(mat.atlasLocation()));
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to get buffer", e);
            return;
        }

        Vec3 basePivot = getBasePivot(partName, bone, crouching);

        poseStack.pushPose();

        double pivotDeltaX = (bone.x - basePivot.x) * PIXEL_SCALE;
        double pivotDeltaY = (bone.y - basePivot.y) * PIXEL_SCALE;
        double pivotDeltaZ = (bone.z - basePivot.z) * PIXEL_SCALE;

        poseStack.translate(pivotDeltaX, pivotDeltaY, pivotDeltaZ);

        poseStack.translate(basePivot.x * PIXEL_SCALE, basePivot.y * PIXEL_SCALE, basePivot.z * PIXEL_SCALE);
        if (bone.xRot != 0.0F || bone.yRot != 0.0F || bone.zRot != 0.0F) {
            poseStack.mulPose(new org.joml.Quaternionf().rotationZYX(bone.zRot, bone.yRot, bone.xRot));
        }
        poseStack.translate(-basePivot.x * PIXEL_SCALE, -basePivot.y * PIXEL_SCALE, -basePivot.z * PIXEL_SCALE);

        Vec3 off = config.getPartOffsets().get(partName);
        if (off != null) {
            poseStack.translate(off.x, off.y, off.z);
        }

        poseStack.scale(PIXEL_SCALE, PIXEL_SCALE, PIXEL_SCALE);
        float zFightingScale = config.getZFightingScale();
        poseStack.scale(zFightingScale, zFightingScale, zFightingScale);

        RandomSource rand = RandomSource.create(42);
        
        emitAllQuads(poseStack, vc, partModel, rand, light);

        poseStack.popPose();
    }

    protected Vec3 getBasePivot(String partName, ModelPart bone, boolean crouching) {
        String cacheKey = config.getArmorTypeId() + ":" + partName;
        Vec3 basePivot = basePivots.computeIfAbsent(cacheKey, k -> {
            if ("Helmet".equals(partName)) return Vec3.ZERO;
            if (!crouching) return new Vec3(bone.x, bone.y, bone.z);
            return new Vec3(bone.x, bone.y, bone.z);
        });

        if (!crouching && !"Helmet".equals(partName)) {
            basePivots.put(cacheKey, new Vec3(bone.x, bone.y, bone.z));
            basePivot = new Vec3(bone.x, bone.y, bone.z);
        } else if (!crouching && "Helmet".equals(partName)) {
            basePivot = Vec3.ZERO;
        }
        return basePivot;
    }

    private static void emitAllQuads(PoseStack poseStack, VertexConsumer vc, BakedModel model, RandomSource rand, int light) {
        PoseStack.Pose pose = poseStack.last();
        
        for (var dir : net.minecraft.core.Direction.values()) {
            List<net.minecraft.client.renderer.block.model.BakedQuad> quads = model.getQuads(null, dir, rand, ModelData.EMPTY, null);
            if (quads != null && !quads.isEmpty()) {
                for (var q : quads) putQuadManual(vc, pose, q, light);
            }
        }

        List<net.minecraft.client.renderer.block.model.BakedQuad> general = model.getQuads(null, null, rand, ModelData.EMPTY, null);
        if (general != null && !general.isEmpty()) {
            for (var q : general) putQuadManual(vc, pose, q, light);
        }
    }

    private static void putQuadManual(VertexConsumer builder, PoseStack.Pose pose, net.minecraft.client.renderer.block.model.BakedQuad quad, int light) {
        int[] vertexData = quad.getVertices();
        net.minecraft.core.Vec3i faceNormal = quad.getDirection() != null ? quad.getDirection().getNormal() : new net.minecraft.core.Vec3i(0, 1, 0);
        
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        
        org.joml.Matrix4f posMatrix = pose.pose();
        org.joml.Matrix3f normalMatrix = pose.normal();

        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            
            float x = Float.intBitsToFloat(vertexData[offset + 0]);
            float y = Float.intBitsToFloat(vertexData[offset + 1]);
            float z = Float.intBitsToFloat(vertexData[offset + 2]);
            
            // На всякий случай оставляю проверку на NaN, она дешевая и полезная
            if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) { x=0; y=0; z=0; }
            
            int color = vertexData[offset + 3];
            float a = (float)(color >> 24 & 255) / 255.0F;
            float r = (float)(color >> 16 & 255) / 255.0F;
            float g = (float)(color >> 8 & 255) / 255.0F;
            float b = (float)(color & 255) / 255.0F;

            float u = Float.intBitsToFloat(vertexData[offset + 4]);
            float v = Float.intBitsToFloat(vertexData[offset + 5]);
            if (Float.isNaN(u)) u = 0; 
            if (Float.isNaN(v)) v = 0;
            
            float nx, ny, nz;
            int packedNormal = vertexData[offset + 7];
            
            if (packedNormal != 0) {
                nx = ((byte)(packedNormal & 255)) / 127.0F;
                ny = ((byte)(packedNormal >> 8 & 255)) / 127.0F;
                nz = ((byte)(packedNormal >> 16 & 255)) / 127.0F;
            } else {
                nx = faceNormal.getX();
                ny = faceNormal.getY();
                nz = faceNormal.getZ();
            }

            float lenSq = nx * nx + ny * ny + nz * nz;
            if (lenSq < 1.0E-5F || Float.isNaN(lenSq)) {
                nx = 0.0F; ny = 1.0F; nz = 0.0F;
            }

            builder.vertex(posMatrix, x, y, z)
                   .color(r, g, b, a)
                   .uv(u, v)
                   .overlayCoords(overlay)
                   .uv2(light)
                   .normal(normalMatrix, nx, ny, nz)
                   .endVertex();
        }
    }

    public void clearPivotCache() {
        synchronized (basePivots) {
            basePivots.clear();
        }
    }

    public static void clearAllCaches() {
        MODEL_CACHE.clear();
        PART_CACHE.clear();
    }
}