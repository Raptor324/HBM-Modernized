package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.machines.MachineFluidTankBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.client.model.ConfiguredMultipartBakedModel;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.util.DiamondPronter;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.main.MainRegistry;
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
 * Fluid Tank / BAT9000 на фабрике {@link MachineRenderers}: Frame — статика;
 * Tank — динамическая часть с ретекстурой под жидкость (VBO кешируется по
 * текстуре в MeshRenderCache: один VBO на уникальную жидкость); NFPA-алмазы —
 * immediate-хук. Вся геометрия в BER/VBO, chunk mesh пуст
 * ({@code ConfiguredMultipartBakedModel} world quads пусты).
 */
public final class MachineFluidTankRenderer {

    private static final RandomSource RANDOM = RandomSource.create(42);

    public static void register() {
        buildSpec("fluidtank", ModBlockEntities.FLUID_TANK_BE.get(), MachineFluidTankBlockEntity.class,
                MachineFluidTankRenderer::renderDiamonds);
        buildSpec("bat9000", ModBlockEntities.BAT9000_BE.get(), com.hbm_m.blockentity.machines.Bat9000BlockEntity.class,
                MachineFluidTankRenderer::renderDiamondsBat9000);
    }

    private MachineFluidTankRenderer() {}

    /** Одна спека на BE-тип; Bat9000 наследует MachineFluidTankBlockEntity — логика общая. */
    private static <T extends MachineFluidTankBlockEntity> void buildSpec(
            String id, net.minecraft.world.level.block.entity.BlockEntityType<T> type, Class<T> cls,
            MachineRenderHook<T> diamonds) {
        MachineRenderers.machine(id, type, cls)
            .part("Frame")
            .dynamicPart("Tank", MachineFluidTankRenderer::tankQuads,
                    be -> String.valueOf(be.getTankTextureLocation()))
            .blockTransform(MachineFluidTankRenderer::applyBlockTransform)
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
     * Легаси: setupBlockTransform (T(0.5,0,0.5)+rotateY) затем T(-0.5,0,-0.5)
     * вокруг всех VBO-частей (меши запечены в OBJ-координатах 0..1).
     */
    private static void applyBlockTransform(MachineFluidTankBlockEntity be, LegacyAnimator animator) {
        animator.setupBlockTransform(facing(be));
        animator.translate(-0.5f, 0.0f, -0.5f);
    }

    // ── Tank: ретекстурированные квады ─────────────────────────────────

    private static List<BakedQuad> tankQuads(MachineFluidTankBlockEntity be) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
        if (!(raw instanceof ConfiguredMultipartBakedModel model)) return List.of();
        BakedModel tankPart = model.getPart("Tank");
        if (tankPart == null) return List.of();

        ResourceLocation safeTex = defaultTankTexture();
        ResourceLocation fluidTex = be.getTankTextureLocation();
        if (fluidTex != null) safeTex = fluidTex;

        return buildRetexturedTankQuads(tankPart, safeTex);
    }

    private static ResourceLocation defaultTankTexture() {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/tank/tank_none");
    }

    /**
     * Квады Tank с подменой спрайта на {@code textureLoc}. Нормализация UV: UV
     * дефолтного спрайта → [0,1] → новый спрайт. VBO кешируется фабрикой по ключу
     * texture path — один VBO на уникальную текстуру жидкости.
     *
     * <p>Public: переиспользуется предметным рендером {@code FluidTankItemRenderer}.</p>
     */
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

    /** Сбор всех квадов части (все стороны + null side, neutral RandomSource). */
    public static List<BakedQuad> collectAllQuads(BakedModel part) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(RenderHooks.getModelQuads(part, null, dir, RANDOM, null));
        }
        quads.addAll(RenderHooks.getModelQuads(part, null, null, RANDOM, null));
        return quads;
    }


    /** Перенос UV квада со старого спрайта на новый (формат BLOCK: 8 int на вершину). */
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

    // ── NFPA-алмазы (hook) ─────────────────────────────────────────────

    /**
     * Компенсация фрейма: запечённый фрейм меша смещён на −3 блока по оси X
     * модели относительно фрейма, в котором 1.7.10 задавала легаси-оффсеты
     * алмазов (в оригинале TE стоял в другой точке структуры). Подобрано
     * вручную 2026-09-22, проверено в игре.
     */
    private static final float DIAMOND_FRAME_COMP_X = -3.0F;

    /**
     * Алмазы опасности на двух боковых гранях бака (1.7.10 RenderFluidTank):
     * translate(-0.25, 0.5, -1.501)/(0.25, 0.5, 1.501), rotateY ±90°, scale(1, 0.375, 0.375).
     * Стек хука = setupTransform·T(-0.5); возвращаемся в модельный фрейм T(0.5).
     * <p>
     * Root-трансформ модели B = T(-0.5,0,-2.5)·RotY(90) повторяется в хуке
     * (запечён в квады частей, см. ловушку obj-part-loader-root-transform),
     * затем идёт компенсация фрейма {@link #DIAMOND_FRAME_COMP_X}.
     */
    private static void renderDiamonds(MachineFluidTankBlockEntity be, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffer,
                                       int packedLight, int packedOverlay, MachineRenderApi api) {
        Fluid fluid = be.getFluidTank().getTankType();
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            return;
        }

        FluidType type = FluidType.forFluid(fluid);

        BlockPos pos = be.getBlockPos();
        int light = LevelRenderer.getLightColor(be.getLevel(), pos.above(2));

        RenderSystem.disableCull();

        poseStack.pushPose();
        poseStack.translate(-0.5F, 0.0F, -2.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(DIAMOND_FRAME_COMP_X, 0.0F, 0.0F);
        poseStack.translate(-0.25F, 0.5F, -1.501F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.scale(1.0F, 0.375F, 0.375F);
        DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity, type.symbol, light, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.5F, 0.0F, -2.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(DIAMOND_FRAME_COMP_X, 0.0F, 0.0F);
        poseStack.translate(0.25F, 0.5F, 1.501F);
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.scale(1.0F, 0.375F, 0.375F);
        DiamondPronter.pront(poseStack, buffer, type.poison, type.flammability, type.reactivity, type.symbol, light, packedOverlay);
        poseStack.popPose();

        RenderSystem.enableCull();
    }

    /**
     * NFPA-алмазы BAT9000 (1.7.10 RenderBAT9000): меш — вертикальный цилиндр
     * (радиус 2.625, высота 5, chunk mesh без фасинга), 4 алмаза по кругу:
     * T(center)·rotY(45), затем 4×[ T(2.5,2.25,0)·scale(1,0.75,0.75)·pront, rotY(90) ].
     * Фейсинг-поворот спеки раскручивается обратно до фрейма угла блока —
     * в 1.7.10 фасинг-ротации у BAT9000 не было вовсе.
     * <p>
     * Центр цилиндра измеряется по фактическим квадам запечённой модели
     * (raw центр = 0, с запечённым root-трансформом T(0.5,0,0.5) = 0.5) —
     * хук не расходится с мешем независимо от того, применён ли трансформ.
     */
    private static void renderDiamondsBat9000(MachineFluidTankBlockEntity be, float partialTick,
                                              PoseStack poseStack, MultiBufferSource buffer,
                                              int packedLight, int packedOverlay, MachineRenderApi api) {
        Fluid fluid = be.getFluidTank().getTankType();
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            return;
        }

        FluidType type = FluidType.forFluid(fluid);

        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above(2));

        RenderSystem.disableCull();

        // Центр запечённого меша в угловом фрейме блока (измеряется один раз)
        float[] center = bat9000Center;
        if (center == null) {
            center = measureBat9000Center(be);
            bat9000Center = center;
        }

        poseStack.pushPose();
        // Раскрутить blockTransform спеки (T(0.5)·R(90+f)·T(-0.5)) до фрейма угла блока:
        // обратный ход T(0.5)·R(-(90+f))·T(-0.5).
        float facingDeg = 90.0F + com.hbm_m.util.MultipartFacingTransforms
                .legacyFacingRotationYDegrees(facing(be));
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facingDeg));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        // Далее 1:1 RenderBAT9000 (T(x+0.5,y,z+0.5) и rotY(45)).
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

    /** Центр XZ запечённого меша BAT9000 в угловом фрейме блока (fallback — центр блока). */
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
