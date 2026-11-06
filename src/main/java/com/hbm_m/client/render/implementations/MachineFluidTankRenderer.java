// file: C:/Projects/HBM-Modernized-agents/src/main/java/com/hbm_m/client/render/implementations/MachineFluidTankRenderer.java

package com.hbm_m.client.render.implementations;

import java.util.ArrayList;

import java.util.List;

import java.util.Map;

import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineFluidTankBlock;

import com.hbm_m.blockentity.ModBlockEntities;

import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;

import com.hbm_m.client.model.ConfiguredMultipartBakedModel;

import com.hbm_m.client.render.LegacyAnimator;

import com.hbm_m.client.render.cache.RenderCacheManager;

import com.hbm_m.client.render.machine.MachineRenderApi;

import com.hbm_m.client.render.machine.MachineRenderHook;

import com.hbm_m.client.render.machine.MachineRenderers;

import com.hbm_m.client.render.util.DiamondPronter;

import com.hbm_m.inventory.fluid.FluidType;

import com.hbm_m.inventory.fluid.ModFluids;

import com.hbm_m.main.MainRegistry;

import com.hbm_m.platform.RenderHooks;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.LevelRenderer;

import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.block.model.BakedQuad;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.minecraft.client.resources.model.BakedModel;

import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.util.RandomSource;

import net.minecraft.world.inventory.InventoryMenu;

import net.minecraft.world.level.material.Fluid;

import net.minecraft.world.level.material.Fluids;

/**

 * Fluid Tank / BAT9000 on the {@link MachineRenderers} factory:

 * Both Frame and Tank are 100% STATIC VBO parts. Fluid texture switching on Tank is

 * performed on the GPU via per-instance {@code InstUvRect} (zero dynamic VBOs, zero

 * quad cloning on the CPU, one shared MDI draw call across all fluids).

 * NFPA diamonds are rendered via an immediate hook only when the tank contains a fluid.

 */

public final class MachineFluidTankRenderer {

    private static final RandomSource RANDOM = RandomSource.create(42);

    private static final ResourceLocation DEFAULT_TANK_TEX =

            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/tank/tank_none");

    /** Cached {offsetU, offsetV, scaleU, scaleV} per target fluid texture for each model class/key. */

    private static final Map<ResourceLocation, float[]> TANK_UV_RECT_CACHE = new ConcurrentHashMap<>();

    private static volatile TextureAtlasSprite cachedBaseTankSprite;

    static {

        RenderCacheManager.register(reason -> {

            TANK_UV_RECT_CACHE.clear();

            cachedBaseTankSprite = null;

            bat9000Center = null;

        });

    }

    public static void register() {

        buildSpec("fluidtank", ModBlockEntities.FLUID_TANK_BE.get(), MachineFluidTankBlockEntity.class,

                new MachineRenderHook<MachineFluidTankBlockEntity>() {

                    @Override

                    public void render(MachineFluidTankBlockEntity be, float pt, PoseStack ps,

                                       MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {

                        renderDiamonds(be, pt, ps, buf, light, overlay, api);

                    }

                    @Override

                    public boolean mayDrawContent(MachineFluidTankBlockEntity be) {

                        return hasFluidForDiamond(be);

                    }

                });

        buildSpec("bat9000", ModBlockEntities.BAT9000_BE.get(), com.hbm_m.blockentity.machines.Bat9000BlockEntity.class,

                new MachineRenderHook<com.hbm_m.blockentity.machines.Bat9000BlockEntity>() {

                    @Override

                    public void render(com.hbm_m.blockentity.machines.Bat9000BlockEntity be, float pt, PoseStack ps,

                                       MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {

                        renderDiamondsBat9000(be, pt, ps, buf, light, overlay, api);

                    }

                    @Override

                    public boolean mayDrawContent(com.hbm_m.blockentity.machines.Bat9000BlockEntity be) {

                        return hasFluidForDiamond(be);

                    }

                });

    }

    private MachineFluidTankRenderer() {}

    /** One spec per BE type; Bat9000 extends MachineFluidTankBlockEntity -- the logic is shared. */

    private static <T extends MachineFluidTankBlockEntity> void buildSpec(

            String id, net.minecraft.world.level.block.entity.BlockEntityType<T> type, Class<T> cls,

            MachineRenderHook<T> diamonds) {

        MachineRenderers.machine(id, type, cls)

            .part("Frame")

            .uvRect("Tank", MachineFluidTankRenderer::tankUvRect)

            .part("Tank")

            .blockTransform(MachineFluidTankRenderer::applyBlockTransform)

            .animationEpoch(MachineFluidTankRenderer::tankEpoch)

            .hook(diamonds)

            .facing(MachineFluidTankRenderer::facing)

            .chunkRenderTypes(net.minecraft.client.renderer.RenderType.cutoutMipped())

            .itemParts("Frame", "Tank")

            .register();

    }

    private static Direction facing(MachineFluidTankBlockEntity be) {

        return be.getBlockState().getValue(MachineFluidTankBlock.FACING);

    }

    /**
     * The tank OBJ is modeled in a natural coordinate system: the long axis (5 blocks) lies
     * along X and matches the multiblock structure 1:1 when FACING=NORTH. Therefore the base
     * +90 rotation from {@link LegacyAnimator#setupBlockTransform(Direction)} is not needed here.
     * We apply only the legacy facing table (structural rotation from
     * {@link com.hbm_m.multiblock.MultiblockStructureHelper#rotate}).
     * BAT9000 uses the old convention: its model and NFPA hazard diamonds
     * ({@link #renderDiamondsBat9000}) are designed around the base +90.
     */
    private static void applyBlockTransform(MachineFluidTankBlockEntity be, LegacyAnimator animator) {

        if (be instanceof com.hbm_m.blockentity.machines.Bat9000BlockEntity) {

            animator.setupBlockTransform(facing(be));

            return;

        }

        // +180 deg: tank OBJ is mirrored relative to the multiblock schema (front on the other side),
        // rotate model in world.
        animator.translate(0.5, 0.0, 0.5);

        animator.rotate(com.hbm_m.util.MultipartFacingTransforms.legacyFacingRotationYDegrees(facing(be)) + 180.0F, 0.0F, 1.0F, 0.0F);

    }

    /**

     * Epoch tracks the current fluid texture location: constant while the fluid type

     * is unchanged (100% O(1) frozen roster assert), flips on the exact tick the fluid

     * changes so the 16-byte InstUvRect slice is updated in the instance buffer.

     */

    private static long tankEpoch(MachineFluidTankBlockEntity be) {

        ResourceLocation tex = be.getTankTextureLocation();

        return tex != null ? tex.hashCode() : 0L;

    }

    private static boolean hasFluidForDiamond(MachineFluidTankBlockEntity be) {

        Fluid fluid = be.getFluidTank().getTankType();

        return fluid != null && fluid != Fluids.EMPTY && fluid != ModFluids.NONE.getSource();

    }

    // -- Tank: GPU InstUvRect affine atlas remap ------------------------

    /**

     * Computes {offsetU, offsetV, scaleU, scaleV} mapping the baked default Tank sprite UVs

     * directly to the active fluid's sprite UVs in the block atlas:

     *   u_out = offsetU + u_baked * scaleU

     * Returns null (identity {0, 0, 1, 1}) when the tank uses the default texture.

     */

    @Nullable

    private static float[] tankUvRect(MachineFluidTankBlockEntity be) {

        ResourceLocation targetLoc = be.getTankTextureLocation();

        if (targetLoc == null || DEFAULT_TANK_TEX.equals(targetLoc)) {

            return null;

        }

        TextureAtlasSprite baseSprite = resolveBaseTankSprite(be);

        if (baseSprite == null) {

            return null;

        }

        return TANK_UV_RECT_CACHE.computeIfAbsent(targetLoc, loc -> {

            TextureAtlasSprite newSprite = Minecraft.getInstance()

                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)

                    .apply(loc);

            if (newSprite == null || newSprite == baseSprite) {

                return new float[] { 0f, 0f, 1f, 1f };

            }

            float oldDu = baseSprite.getU1() - baseSprite.getU0();

            float oldDv = baseSprite.getV1() - baseSprite.getV0();

            if (Math.abs(oldDu) < 1e-7f || Math.abs(oldDv) < 1e-7f) {

                return new float[] { 0f, 0f, 1f, 1f };

            }

            float newDu = newSprite.getU1() - newSprite.getU0();

            float newDv = newSprite.getV1() - newSprite.getV0();

            float scaleU = newDu / oldDu;

            float scaleV = newDv / oldDv;

            float offsetU = newSprite.getU0() - baseSprite.getU0() * scaleU;

            float offsetV = newSprite.getV0() - baseSprite.getV0() * scaleV;

            return new float[] { offsetU, offsetV, scaleU, scaleV };

        });

    }

    @Nullable

    private static TextureAtlasSprite resolveBaseTankSprite(MachineFluidTankBlockEntity be) {

        TextureAtlasSprite s = cachedBaseTankSprite;

        if (s != null) {

            return s;

        }

        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());

        if (raw instanceof ConfiguredMultipartBakedModel model) {

            BakedModel tankPart = model.getPart("Tank");

            if (tankPart != null) {

                List<BakedQuad> quads = collectAllQuads(tankPart);

                if (!quads.isEmpty() && quads.get(0).getSprite() != null) {

                    s = quads.get(0).getSprite();

                    cachedBaseTankSprite = s;

                    return s;

                }

            }

        }

        s = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(DEFAULT_TANK_TEX);

        cachedBaseTankSprite = s;

        return s;

    }

    // -- Item renderer helper (used by FluidTankItemRenderer BEWLR) -----

    public static List<BakedQuad> buildRetexturedTankQuads(BakedModel tankPart, ResourceLocation textureLoc) {

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

    }

    /** Collects all quads of a part (all sides + null side, neutral RandomSource). */

    public static List<BakedQuad> collectAllQuads(BakedModel part) {

        List<BakedQuad> quads = new ArrayList<>();

        for (Direction dir : Direction.values()) {

            quads.addAll(RenderHooks.getModelQuads(part, null, dir, RANDOM, null));

        }

        quads.addAll(RenderHooks.getModelQuads(part, null, null, RANDOM, null));

        return quads;

    }

    private static BakedQuad retextureAndFixUV(BakedQuad original, TextureAtlasSprite newSprite) {

        int[] oldData = original.getVertices();

        int[] newData = new int[oldData.length];

        System.arraycopy(oldData, 0, newData, 0, oldData.length);

        var oldSprite = original.getSprite();

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

    // -- NFPA diamonds (hook) -------------------------------------------

    private static void renderDiamonds(MachineFluidTankBlockEntity be, float partialTick,

                                       PoseStack poseStack, MultiBufferSource buffer,

                                       int packedLight, int packedOverlay, MachineRenderApi api) {

        if (!hasFluidForDiamond(be)) {

            return;

        }

        FluidType type = FluidType.forFluid(be.getFluidTank().getTankType());

        BlockPos pos = be.getBlockPos();

        int light = LevelRenderer.getLightColor(be.getLevel(), pos.above(2));

        RenderSystem.disableCull();

        poseStack.pushPose();

        poseStack.translate(-0.25F, 0.5F, -1.501F);

        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        poseStack.scale(1.0F, 0.375F, 0.375F);

        DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity, type.symbol, light, packedOverlay);

        poseStack.popPose();

        poseStack.pushPose();

        poseStack.translate(0.25F, 0.5F, 1.501F);

        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));

        poseStack.scale(1.0F, 0.375F, 0.375F);

        DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity, type.symbol, light, packedOverlay);

        poseStack.popPose();

        RenderSystem.enableCull();

    }

    private static void renderDiamondsBat9000(MachineFluidTankBlockEntity be, float partialTick,

                                              PoseStack poseStack, MultiBufferSource buffer,

                                              int packedLight, int packedOverlay, MachineRenderApi api) {

        if (!hasFluidForDiamond(be)) {

            return;

        }

        FluidType type = FluidType.forFluid(be.getFluidTank().getTankType());

        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above(2));

        RenderSystem.disableCull();

        float[] center = bat9000Center;

        if (center == null) {

            center = measureBat9000Center(be);

            bat9000Center = center;

        }

        poseStack.pushPose();

        float facingDeg = 90.0F + com.hbm_m.util.MultipartFacingTransforms

                .legacyFacingRotationYDegrees(facing(be));

        poseStack.translate(0.5F, 0.0F, 0.5F);

        poseStack.mulPose(Axis.YP.rotationDegrees(-facingDeg));

        poseStack.translate(-0.5F, 0.0F, -0.5F);

        poseStack.translate(center[0], 0.0F, center[1]);

        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));

        for (int j = 0; j < 4; j++) {

            poseStack.pushPose();

            poseStack.translate(2.5F, 2.25F, 0.0F);

            poseStack.scale(1.0F, 0.75F, 0.75F);

            DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity, type.symbol, light, packedOverlay);

            poseStack.popPose();

            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        }

        poseStack.popPose();

        RenderSystem.enableCull();

    }

    private static volatile float[] bat9000Center;

    private static float[] measureBat9000Center(MachineFluidTankBlockEntity be) {

        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());

        List<BakedQuad> quads = collectAllQuads(model);

        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;

        float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        for (BakedQuad quad : quads) {

            int[] data = quad.getVertices();

            int vertexSize = data.length / 4;

            for (int i = 0; i < 4; i++) {

                float x = Float.intBitsToFloat(data[i * vertexSize]);

                float z = Float.intBitsToFloat(data[i * vertexSize + 2]);

                minX = Math.min(minX, x); maxX = Math.max(maxX, x);

                minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);

            }

        }

        if (minX <= maxX) {

            return new float[]{(minX + maxX) * 0.5F, (minZ + maxZ) * 0.5F};

        }

        return new float[]{0.5F, 0.5F};

    }

}
