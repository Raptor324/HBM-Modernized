package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.client.model.MachineChemicalPlantBakedModel;
import com.hbm_m.client.model.ModelHelper;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.shader.IrisPhaseGuard;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.architectury.fluid.FluidStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Химкомбинат на фабрике {@link MachineRenderers}: Base — статика; Frame — динамическая
 * часть (по свойству FRAME); Slider/Spinner — анимация; жидкость — immediate-хук с
 * ОТЛОЖЕННОЙ отрисовкой (draw в AFTER_BLOCK_ENTITIES после instanced-flush, см.
 * комментарий у {@link #presentDeferredFluids()}).
 * <p>
 * Кастомный блочный трансформ: {@code T(0.5,0,0.5)·R(chemicalPlantPoseRotationY)·T(-0.5,0,-0.5)}
 * (сдвиг -0.5/-0.5 — baked-space частей, как в легаси renderChemicalPlantPartsInternal).
 */
public final class MachineChemicalPlantRenderer {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    /**
     * Пивот вращения спиннера в baked-координатах = OBJ-центр (0.5, 0.5) + JSON
     * root translation (0.5, 0.5). Если меняешь JSON translation — pivot = OBJ_CENTER + JSON_translation.
     */
    private static final float CHEMPLANT_BAKE_PIVOT_X = 1.0f;
    private static final float CHEMPLANT_BAKE_PIVOT_Z = 1.0f;

    public static void register() {
        MachineRenderers.machine("chemplant", ModBlockEntities.CHEMICAL_PLANT_BE.get(),
                MachineChemicalPlantBlockEntity.class)
            .part("Base")
            .dynamicPart("Frame", MachineChemicalPlantRenderer::frameQuads,
                    // Ключ по FRAME-состоянию: константный "frame" кешировал бы пустой
                    // рендерер по первому состоянию навсегда (см. advassembler).
                    be -> {
                        var st = be.getBlockState();
                        return String.valueOf(st.hasProperty(MachineChemicalPlantBlock.FRAME)
                                && st.getValue(MachineChemicalPlantBlock.FRAME));
                    })
            .part("Slider", MachineChemicalPlantRenderer::animateSlider)
            .part("Spinner", MachineChemicalPlantRenderer::animateSpinner)
            .blockTransform(MachineChemicalPlantRenderer::applyBlockTransform)
            .hook(MachineChemicalPlantRenderer::scheduleFluid)
            .register();
    }

    private MachineChemicalPlantRenderer() {}

    private static @Nullable MachineChemicalPlantBakedModel plantModel(MachineChemicalPlantBlockEntity be) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
        return raw instanceof MachineChemicalPlantBakedModel m ? m : null;
    }

    /** Кастомный блочный трансформ химзавода (+ baked-space сдвиг -0.5/-0.5 для всех частей). */
    private static void applyBlockTransform(MachineChemicalPlantBlockEntity be, LegacyAnimator animator) {
        var state = be.getBlockState();
        if (state.hasProperty(MachineChemicalPlantBlock.FACING)) {
            animator.translate(0.5, 0.0, 0.5);
            animator.rotate(com.hbm_m.util.MultipartFacingTransforms
                    .chemicalPlantPoseRotationY(state.getValue(MachineChemicalPlantBlock.FACING)), 0, 1, 0);
        } else {
            animator.translate(0.5, 0.0, 0.5);
        }
        animator.translate(-0.5f, 0.0f, -0.5f);
    }

    // ── Части ──────────────────────────────────────────────────────────

    private static List<BakedQuad> frameQuads(MachineChemicalPlantBlockEntity be) {
        var state = be.getBlockState();
        if (!state.hasProperty(MachineChemicalPlantBlock.FRAME)
                || !state.getValue(MachineChemicalPlantBlock.FRAME)) {
            return List.of();
        }
        BakedModel part = plantPart(be, "Frame");
        if (part == null) return List.of();
        return MeshRenderCache.getOrCompile("chemplant_Frame", part);
    }

    private static @Nullable BakedModel plantPart(MachineChemicalPlantBlockEntity be, String partName) {
        MachineChemicalPlantBakedModel model = plantModel(be);
        return model == null ? null : model.getPart(partName);
    }

    private static boolean animateSlider(MachineChemicalPlantBlockEntity be, float partialTick,
                                         long gameTime, PoseStack pose) {
        float anim = be.getAnim(partialTick);
        double sdx = chemicalSps(anim * 0.125) * 0.375;
        pose.last().pose().mul(new Matrix4f().translate((float) sdx, 0f, 0f));
        return true;
    }

    private static boolean animateSpinner(MachineChemicalPlantBlockEntity be, float partialTick,
                                          long gameTime, PoseStack pose) {
        float anim = be.getAnim(partialTick);
        float deg = (anim * 15f) % 360f;
        if (deg < 0f) deg += 360f;
        pose.last().pose().mul(new Matrix4f()
                .translate(CHEMPLANT_BAKE_PIVOT_X, 0f, CHEMPLANT_BAKE_PIVOT_Z)
                .rotateY(deg * DEG_TO_RAD)
                .translate(-CHEMPLANT_BAKE_PIVOT_X, 0f, -CHEMPLANT_BAKE_PIVOT_Z));
        return true;
    }

    /** Soft peak sine (BobMathUtil.sps). */
    private static double chemicalSps(double x) {
        return Math.sin(Math.PI / 2.0 * Math.cos(x));
    }

    // ==================== DEFERRED FLUID ====================
    // Копия легаси-прохода из MachineChemicalPlantVboRenderer (жидкость — не OBJ-статика,
    // UV-скролл per-frame; см. javadoc presentDeferredFluids про порядок отрисовки).

    /** Цвет/текстура для fallback; основной путь — mesh с {@code chemical_plant_fluid}. */
    public record FluidVisual(FluidStack textureFluid, float r, float g, float b) {}

    private record DeferredChemplantFluid(
            MachineChemicalPlantBakedModel model,
            BlockState state,
            Matrix4f pose,
            float anim,
            int packedLight,
            int packedOverlay,
            FluidVisual visual
    ) {}

    private static final List<DeferredChemplantFluid> DEFERRED_FLUIDS = new ArrayList<>();

    /** Сброс очереди жидкости в начале кадра (или на early-return). */
    public static void clearDeferredFluids() {
        DEFERRED_FLUIDS.clear();
    }

    /** Хук: только запись в очередь; draw — в {@link #presentDeferredFluids()}. */
    private static void scheduleFluid(MachineChemicalPlantBlockEntity be, float partialTick,
                                      PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay, MachineRenderApi api) {
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) return;
        MachineChemicalPlantBakedModel model = plantModel(be);
        if (model == null) return;
        FluidVisual visual = getRecipeVisual(be);
        if (visual == null) return;
        DEFERRED_FLUIDS.add(new DeferredChemplantFluid(
                model, be.getBlockState(), new Matrix4f(poseStack.last().pose()),
                be.getAnim(partialTick), packedLight, packedOverlay, visual));
    }

    /** Видимость жидкости: тот же критерий, что звук и anim. */
    private static boolean isChemplantProcessVisible(MachineChemicalPlantBlockEntity be) {
        return be.isChemplantEffectsActive();
    }

    @Nullable
    private static FluidVisual getRecipeVisual(MachineChemicalPlantBlockEntity be) {
        if (!isChemplantProcessVisible(be)) return null;
        Level level = be.getLevel();
        if (level == null) return null;
        ChemicalPlantRecipe recipe = be.getModule().peekRecipe(level);
        if (recipe == null) {
            return getTankFallbackVisual(be);
        }

        List<FluidStack> colorFluids = !recipe.getFluidOutputs().isEmpty()
            ? recipe.getFluidOutputs()
            : List.of();
        if (colorFluids.isEmpty() && !recipe.getFluidInputs().isEmpty()) {
            List<FluidStack> tmp = new ArrayList<>();
            for (var fin : recipe.getFluidInputs()) {
                var fluid = fin.getFluid();
                if (fluid == null) continue;
                tmp.add(FluidStack.create(fluid, fin.getAmount()));
            }
            colorFluids = tmp;
        }
        if (colorFluids.isEmpty()) return null;

        int colors = 0;
        float rr = 0, gg = 0, bb = 0;
        for (FluidStack fs : colorFluids) {
            if (fs.isEmpty()) continue;
            int tint = HbmFluidRegistry.getTintColor(fs.getFluid());
            rr += ((tint >> 16) & 0xFF) / 255.0F;
            gg += ((tint >> 8) & 0xFF) / 255.0F;
            bb += (tint & 0xFF) / 255.0F;
            colors++;
        }
        if (colors <= 0) return null;
        rr /= colors;
        gg /= colors;
        bb /= colors;

        FluidStack texFluid = null;
        for (FluidStack out : recipe.getFluidOutputs()) {
            if (!out.isEmpty()) { texFluid = out; break; }
        }
        if (texFluid == null) {
            for (var fin : recipe.getFluidInputs()) {
                var fluid = fin.getFluid();
                if (fluid == null) continue;
                texFluid = FluidStack.create(fluid, fin.getAmount());
                break;
            }
        }
        if (texFluid == null || texFluid.isEmpty()) return null;
        return new FluidVisual(texFluid, rr, gg, bb);
    }

    @Nullable
    private static FluidVisual getTankFallbackVisual(MachineChemicalPlantBlockEntity be) {
        // На клиенте выбранный рецепт часто не синхронизирован; при этом жидкости в баках видимы.
        var outputs = be.getOutputTanks();
        var inputs = be.getInputTanks();

        int colors = 0;
        float rr = 0, gg = 0, bb = 0;
        FluidStack firstNonEmpty = null;

        for (var t : outputs) {
            if (t == null || t.isEmpty()) continue;
            if (firstNonEmpty == null) {
                firstNonEmpty = FluidStack.create(t.getStoredFluid(), (long) t.getFluidAmountMb());
            }
            int tint = HbmFluidRegistry.getTintColor(t.getStoredFluid());
            rr += ((tint >> 16) & 0xFF) / 255.0F;
            gg += ((tint >> 8) & 0xFF) / 255.0F;
            bb += (tint & 0xFF) / 255.0F;
            colors++;
        }

        if (colors == 0) {
            for (var t : inputs) {
                if (t == null || t.isEmpty()) continue;
                if (firstNonEmpty == null) {
                    firstNonEmpty = FluidStack.create(t.getStoredFluid(), (long) t.getFluidAmountMb());
                }
                int tint = HbmFluidRegistry.getTintColor(t.getStoredFluid());
                rr += ((tint >> 16) & 0xFF) / 255.0F;
                gg += ((tint >> 8) & 0xFF) / 255.0F;
                bb += (tint & 0xFF) / 255.0F;
                colors++;
            }
        }

        if (colors <= 0 || firstNonEmpty == null || firstNonEmpty.isEmpty()) return null;
        rr /= colors;
        gg /= colors;
        bb /= colors;
        return new FluidVisual(firstNonEmpty, rr, gg, bb);
    }

    private static final ResourceLocation CHEMPLANT_FLUID_TEX =
        ResourceLocation.fromNamespaceAndPath("hbm_m", "block/machine/chemical_plant_fluid");

    /**
     * Изолированный immediate BufferSource для жидкости. Один endBatch() на кадр —
     * НЕ shared mc.renderBuffers().bufferSource(): endBatch(translucent) на shared
     * выкинул бы чужую pending translucent-геометрию раньше времени.
     */
    private static final MultiBufferSource.BufferSource FLUID_BUFFER_SOURCE =
            RenderHooks.immediateBufferSource(262144);

    /**
     * Отрисовка накопленной за кад жидкости. Вызывается из
     * {@code InstancedRenderFrame.presentAfterBlockEntities} после closePersistentIfActive
     * и instanced-flush. depthMask(false) — жидкость не пишет depth (как glDepthMask(false)
     * в 1.7.10): depth уже содержит opaque-части после instanced-flush.
     */
    public static void presentDeferredFluids() {
        if (DEFERRED_FLUIDS.isEmpty()) return;
        //? if forge || neoforge {
        try (var ignored = IrisPhaseGuard.pushBlockEntities()) {
            boolean depthMaskWas = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            PoseStack poseStack = new PoseStack();
            try {
                RenderSystem.depthMask(false);
                for (DeferredChemplantFluid e : DEFERRED_FLUIDS) {
                    poseStack.pushPose();
                    poseStack.last().pose().set(e.pose);
                    drawChemplantFluidBaked(e.model, e.state, e.anim, poseStack,
                            FLUID_BUFFER_SOURCE, e.packedLight, e.packedOverlay, e.visual);
                    poseStack.popPose();
                }
                FLUID_BUFFER_SOURCE.endBatch();
            } finally {
                RenderSystem.depthMask(depthMaskWas);
            }
        }
        //?}
        DEFERRED_FLUIDS.clear();
    }

    private static void drawChemplantFluidBaked(MachineChemicalPlantBakedModel model, BlockState state, float anim,
                                                PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, int packedOverlay, FluidVisual visual) {
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        BakedModel fluidPart = model.getPart("Fluid");
        if (fluidPart == null) return;

        List<BakedQuad> quads = collectChemplantFluidQuads(fluidPart, state);
        if (quads.isEmpty()) return;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(CHEMPLANT_FLUID_TEX);

        // Поворот блока уже применён caller'ом (блочный трансформ спеки).
        float du = -anim / 100f;
        float dv = (float) (chemicalSps(anim * 0.1) * 0.1 - 0.25);
        quads = ModelHelper.offsetQuadUvsWrapped(quads, du, dv,
            sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());

        VertexConsumer vc = bufferSource.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();
        float r = visual.r(), g = visual.g(), b = visual.b(), a = 0.5f;
        for (BakedQuad quad : quads) {
            //? if forge {
            vc.putBulkData(pose, quad, r, g, b, a, packedLight, packedOverlay, false);
            //?} else {
            /*vc.putBulkData(pose, quad, r, g, b, a, packedLight, packedOverlay);
            *///?}
        }
    }

    private static List<BakedQuad> collectChemplantFluidQuads(BakedModel fluidPart, @Nullable BlockState state) {
        RandomSource rand = RandomSource.create(42);
        for (RenderType layer : new RenderType[]{null, RenderType.cutout(), RenderType.solid(), RenderType.translucent()}) {
            List<BakedQuad> quads = new ArrayList<>();
            for (Direction dir : Direction.values()) {
                quads.addAll(fluidPart.getQuads(state, dir, rand, ModelDataHolder.DATA, layer));
            }
            quads.addAll(fluidPart.getQuads(state, null, rand, ModelDataHolder.DATA, layer));
            if (!quads.isEmpty()) {
                return quads;
            }
        }
        return List.of();
    }

    /** Кросс-версионный ModelData.EMPTY (forge/neoforge имена совпадают, импорт — нет). */
    private static final class ModelDataHolder {
        //? if forge {
        static final net.minecraftforge.client.model.data.ModelData DATA =
                net.minecraftforge.client.model.data.ModelData.EMPTY;
        //?} elif neoforge {
        /*static final net.neoforged.neoforge.client.model.data.ModelData DATA =
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
        *///?}
    }
}
