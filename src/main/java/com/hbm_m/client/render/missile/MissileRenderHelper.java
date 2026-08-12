package com.hbm_m.client.render.missile;

import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.client.model.MissileBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.SingleMeshVboRenderer;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.missile.track.MissileTrackPose;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;


public final class MissileRenderHelper {

    private static final boolean MODEL_DEBUG = Boolean.getBoolean("hbm_m.modelDebug");

    private MissileRenderHelper() {}

    /**
     * Model id for {@link net.minecraftforge.client.event.ModelEvent.RegisterAdditional} and
     * {@code models/item/<path>.json} (Forge uses {@code <namespace>:<path>}, not {@code item/<path>}).
     */
    public static ResourceLocation meshModelId(ResourceLocation itemId) {
        String path = itemId.getPath();
        if (path.startsWith("item/")) {
            return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), path.substring("item/".length()));
        }
        return itemId;
    }

    public static void renderInFlight(MissileBaseEntity entity, MissileFormFactorModels form, PoseStack poseStack,
                                      int packedLight, float yaw, float pitch, BlockPos lightPos) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YN.rotationDegrees(yaw - 90.0F));

        applyLaunchFacingRotation(poseStack, entity.getLaunchFacing());

        MissileRenderData data = resolveFlightData(entity);
        if (data != null) {
            data.render(poseStack, packedLight, lightPos);
        } else {
            debugMissile("renderInFlight: no MissileRenderData for entity {}", entity.getType());
        }

        poseStack.popPose();
    }

    @Nullable
    public static MissileRenderData resolveFromTrack(MissileTrackPose pose) {
        Item launchItem = BuiltInRegistries.ITEM.get(pose.launchItemId());
        if (launchItem != null && launchItem != Items.AIR) {
            MissileRenderData data = MissileRenderRegistry.get(launchItem);
            if (data != null) {
                return data;
            }
            ResourceLocation texture = MissileTextures.forItem(launchItem);
            MissileFormFactorModels form = launchItem instanceof MissileItem missileItem
                    ? MissileFormFactorModels.fromItem(missileItem)
                    : MissileFormFactorModels.OTHER;
            return MissileRenderData.withPadScale(pose.launchItemId(), texture, form.getPadScale());
        }
        var entityType = BuiltInRegistries.ENTITY_TYPE.get(pose.entityTypeId());
        if (entityType != null && MissileBaseEntity.class.isAssignableFrom(entityType.getBaseClass())) {
            @SuppressWarnings("unchecked")
            Class<? extends MissileBaseEntity> clazz = (Class<? extends MissileBaseEntity>) entityType.getBaseClass();
            MissileFormFactorModels form = MissileFormFactorModels.fromEntity(clazz);
            return MissileRenderData.withPadScale(pose.launchItemId(), MissileTextures.PLACEHOLDER, form.getPadScale());
        }
        return null;
    }

    @Nullable
    public static MissileRenderData resolveFlightData(MissileBaseEntity entity) {
        Item launchItem = LaunchPadBaseBlockEntity.getLaunchItemFor(entity.getType());
        if (launchItem != null) {
            MissileRenderData data = MissileRenderRegistry.get(launchItem);
            if (data != null) {
                return data;
            }
        }
        ResourceLocation texture = MissileTextures.forEntity(entity);
        MissileFormFactorModels form = MissileFormFactorModels.fromEntity(entity.getClass());
        ResourceLocation itemId = launchItem != null ? BuiltInRegistries.ITEM.getKey(launchItem) : null;
        if (itemId == null) {
            return null;
        }
        return MissileRenderData.withPadScale(itemId, texture, form.getPadScale());
    }

    public static void applyLaunchFacingRotation(PoseStack poseStack, net.minecraft.core.Direction facing) {
        switch (facing) {
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }
    }

    @Nullable
    public static MissileBakedModel resolveMissileModel(ResourceLocation itemId) {
        BakedModel model = resolveBakedModel(itemId);
        //? if forge {
        MissileBakedModel missileModel = com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.unwrapMissileDelegate(model);
        if (missileModel != null) {
            return missileModel;
        }
        //?} else {
        /*if (model instanceof MissileBakedModel missileModel) {
            return missileModel;
        }
        *///?}
        debugMissile("resolveMissileModel: {} is not MissileBakedModel", itemId);
        return null;
    }

    @Nullable
    public static BakedModel resolveBakedModel(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        //? if forge {
        BakedModel shaperModel = Minecraft.getInstance().getItemRenderer()
                .getItemModelShaper()
                .getItemModel(stack);
        if (shaperModel != null
                && shaperModel != Minecraft.getInstance().getModelManager().getMissingModel()) {
            return AbstractPartBasedRenderer.unwrapFabricForwardingModels(shaperModel);
        }
        //?}
        return resolveBakedModel(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Nullable
    public static BakedModel resolveBakedModel(@Nullable ResourceLocation itemId) {
        if (itemId == null) {
            return null;
        }
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel itemModel = modelManager.getModel(new ModelResourceLocation(itemId, "inventory"));
        itemModel = AbstractPartBasedRenderer.unwrapFabricForwardingModels(itemModel);
        //? if forge {
        if (com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.unwrapMissileDelegate(itemModel) != null) {
            return itemModel;
        }
        //?} else {
        /*if (itemModel instanceof MissileBakedModel) {
            return itemModel;
        }
        *///?}
        ResourceLocation meshId = meshModelId(itemId);
        if (!meshId.equals(itemId)) {
            BakedModel meshModel = modelManager.getModel(new ModelResourceLocation(meshId, "inventory"));
            meshModel = AbstractPartBasedRenderer.unwrapFabricForwardingModels(meshModel);
            //? if forge {
            if (com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat.unwrapMissileDelegate(meshModel) != null) {
                return meshModel;
            }
            //?} else {
            /*if (meshModel instanceof MissileBakedModel) {
                return meshModel;
            }
            *///?}
        }
        return itemModel;
    }

    /**
     * Vanilla item/GUI path: baked quads via {@link VertexConsumer#putBulkData} (no custom VBO shader).
     */
    public static void drawBakedQuads(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                      ItemStack stack) {
        BakedModel model = resolveBakedModel(stack);
        if (model == null) {
            return;
        }
        RandomSource random = RandomSource.create(42L);
        List<BakedQuad> quads;
        //? if forge {
        quads = model.getQuads(null, null, random, net.minecraftforge.client.model.data.ModelData.EMPTY,
                RenderType.solid());
        //?}
        //? if fabric {
        /*quads = model.getQuads(null, null, random);
        *///?}
        //? if neoforge {
        /*quads = model.getQuads(null, null, random);
        *///?}
        if (quads.isEmpty()) {
            debugMissile("drawBakedQuads: no quads for {}", BuiltInRegistries.ITEM.getKey(stack.getItem()));
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.solid());
        PoseStack.Pose pose = poseStack.last();
        int overlay = overlay();
        for (BakedQuad quad : quads) {
            //? if forge {
            consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, overlay, false);
            //?}
            //? if fabric {
            /*consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, packedLight, overlay);
            *///?}
        }
    }

    /**
     * World / BER path: VBO or {@link com.hbm_m.client.render.IrisCompanionMesh} per part
     * (see {@link SingleMeshVboRenderer#render}).
     */
    public static void drawMissileMesh(ItemStack stack, MissileBakedModel missileModel, PoseStack poseStack,
                                       int packedLight, BlockPos lightPos,
                                       @Nullable MultiBufferSource bufferSource) {
        drawMissileMesh(stack, missileModel, poseStack, packedLight, lightPos, bufferSource, null);
    }

    public static void drawMissileMesh(ItemStack stack, MissileBakedModel missileModel, PoseStack poseStack,
                                       int packedLight, BlockPos lightPos,
                                       @Nullable MultiBufferSource bufferSource,
                                       @Nullable BlockEntity blockEntity) {
        bindBlockAtlas();
        String cachePrefix = missileModel.getModelId().toString();
        boolean drewAny = false;
        for (String partName : missileModel.getRenderablePartNames()) {
            BakedModel part = missileModel.getPart(partName);
            if (part == null) {
                debugMissile("drawMissileMesh: missing part '{}' on {}", partName, cachePrefix);
                continue;
            }
            String cacheKey = cachePrefix + ":" + partName;
            SingleMeshVboRenderer renderer = MeshRenderCache.getOrCreateRenderer(cacheKey, part);
            if (renderer == null) {
                debugMissile("drawMissileMesh: MeshRenderCache returned null for {}", cacheKey);
                continue;
            }
            renderer.render(poseStack, packedLight, lightPos, blockEntity, bufferSource);
            drewAny = true;
        }
        if (!drewAny) {
            debugMissile("drawMissileMesh: no parts drawn for {}", cachePrefix);
        }
    }

    /** Draw all baked OBJ parts through the VBO / Iris companion cache. */
    public static void drawVboParts(MissileBakedModel missileModel, PoseStack poseStack,
                                    int packedLight, BlockPos lightPos) {
        drawVboParts(missileModel, poseStack, packedLight, lightPos, null, null);
    }

    public static void drawVboParts(MissileBakedModel missileModel, PoseStack poseStack,
                                    int packedLight, BlockPos lightPos,
                                    @Nullable MultiBufferSource bufferSource) {
        drawVboParts(missileModel, poseStack, packedLight, lightPos, bufferSource, null);
    }

    public static void drawVboParts(MissileBakedModel missileModel, PoseStack poseStack,
                                    int packedLight, BlockPos lightPos,
                                    @Nullable MultiBufferSource bufferSource,
                                    @Nullable BlockEntity blockEntity) {
        drawMissileMesh(ItemStack.EMPTY, missileModel, poseStack, packedLight, lightPos, bufferSource, blockEntity);
    }

    public static void bindBlockAtlas() {
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        Minecraft.getInstance().getTextureManager().bindForSetup(TextureAtlas.LOCATION_BLOCKS);
    }

    @Nullable
    public static MissileFormFactorModels resolveForm(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof MissileItem missileItem) {
            return MissileFormFactorModels.fromItem(missileItem);
        }
        if (item == Items.AIR) {
            return null;
        }
        return MissileFormFactorModels.MICRO;
    }

    private static void debugMissile(String message, Object... args) {
        if (MODEL_DEBUG) {
            MainRegistry.LOGGER.warn(message, args);
        }
    }

    public static int overlay() {
        return OverlayTexture.NO_OVERLAY;
    }

    public static float lerpRotation(float prev, float current, float partialTicks) {
        return Mth.lerp(partialTicks, prev, current);
    }
}
