package com.hbm_m.client.render.implementations;



import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.block.entity.machines.MachineChemicalPlantBlockEntity;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.client.model.MachineChemicalPlantBakedModel;
import com.hbm_m.client.model.ModelHelper;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import dev.architectury.fluid.FluidStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.client.model.data.ModelData;
//?}

import net.minecraft.world.level.block.entity.BlockEntity;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
/**
 * VBO рендер Base/Frame/Slider/Spinner; жидкость — как 1.7.10 {@code RenderChemicalPlant}:
 * только геометрия {@code Fluid}, отдельная карта {@code chemical_plant_fluid}, tint + alpha 0.5,
 * UV-смещение {@code (-anim/100, sps(anim*0.1)*0.1 - 0.25)}.
 * <p>
 * Этот проход не уводим в {@link com.hbm_m.client.render.InstancedStaticPartRenderer}: у каждого блока
 * своё смещение UV от {@code anim}: для instancing нужен отдельный атрибут/uniform в instanced-шейдере
 * (см. {@code block_lit_instanced}). Рендер идёт через {@link RenderType#translucent()} вне Iris batch.
 */
public class MachineChemicalPlantVboRenderer {

    private static final String BASE = "Base";
    private static final String FRAME = "Frame";

    private final MachineChemicalPlantBakedModel model;

    /** Цвет/текстура для fallback; основной путь — mesh с {@code chemical_plant_fluid}. */
    public record FluidVisual(FluidStack textureFluid, float r, float g, float b) {}

    public MachineChemicalPlantVboRenderer(MachineChemicalPlantBakedModel model) {
        this.model = model;
    }

    /** Видимость жидкости: тот же критерий, что звук и {@code anim} (см. {@link MachineChemicalPlantBlockEntity#isChemplantEffectsActive}). */
    public static boolean isChemplantProcessVisible(MachineChemicalPlantBlockEntity be) {
        return be.isChemplantEffectsActive();
    }

    @Nullable
    public static FluidVisual getRecipeVisual(MachineChemicalPlantBlockEntity be) {
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
                var fluid = BuiltInRegistries.FLUID.get(fin.fluidId());
                if (fluid == null) continue;
                tmp.add(FluidStack.create(fluid, (long) fin.amount()));
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
                var fluid = BuiltInRegistries.FLUID.get(fin.fluidId());
                if (fluid == null) continue;
                texFluid = FluidStack.create(fluid, (long) fin.amount());
                break;
            }
        }
        if (texFluid == null || texFluid.isEmpty()) return null;
        return new FluidVisual(texFluid, rr, gg, bb);
    }

    @Nullable
    private static FluidVisual getTankFallbackVisual(MachineChemicalPlantBlockEntity be) {
        // На клиенте выбранный рецепт часто не синхронизирован; при этом жидкости в баках видимы и должны красить рендер.
        // Чтобы совпадать с оригиналом 1.7.10 (средний цвет по fluid-стекам рецепта), делаем "best effort":
        //  - если есть жидкости в выходных баках — усредняем по ним (аналог outputFluid),
        //  - иначе усредняем по входным бакам (аналог inputFluid).
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

    /** Как {@link com.hbm.util.BobMathUtil#sps(double)}. */
    private static double chemicalSps(double x) {
        return Math.sin(Math.PI / 2.0 * Math.cos(x));
    }

    /**
     * Рендер жидкости после позы блока (caller обычно делает {@code translate(-0.5, 0, -0.5)} в VBO-рамке).
     */
    public static void renderChemplantFluid(MachineChemicalPlantBlockEntity be, MachineChemicalPlantBakedModel model,
                                            float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay, FluidVisual visual) {
        poseStack.pushPose();
        poseStack.translate(-0.5f, 0f, -0.5f);
        float anim = be.getAnim(partialTick);
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(MachineChemicalPlantBlock.FACING);
        // Под шейдерами (Oculus/Iris) важно, чтобы tracked shader-texture слот 0 указывал на block atlas.
        // Иначе translucent RenderType может семплить "чужую" текстуру до первого toggle/reload шейдера.
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        // Только правильная геометрия части "Fluid". Без swirl-fallback, чтобы не было конфликтов с batched/VBO.
        tryRenderFluidBakedPart(model, state, facing, anim, poseStack, bufferSource, packedLight, packedOverlay, visual);
        poseStack.popPose();
    }

    private static final ResourceLocation CHEMPLANT_FLUID_TEX =
        new ResourceLocation("hbm_m", "block/machine/chemical_plant_fluid");

    /**
     * RenderType для жидкости.
     * - Forge: используем entityTranslucent с block atlas, чтобы рендер гарантированно попал в BE-пайплайн/буфер
     *   (terrain translucent на Forge может не флашиться на первом входе до ресурсного события).
     * - Fabric: используем block translucent (depth write OFF), иначе жидкость может писать depth и "вырезать" корпус
     *   при instanced/batched пути.
     */
    //? if forge {
    private static final RenderType FLUID_RENDER_TYPE = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
    //?} else {
    /*private static final RenderType FLUID_RENDER_TYPE = RenderType.translucent();
    *///?}

    //? if forge {
    private static boolean tryRenderFluidBakedPart(MachineChemicalPlantBakedModel model, BlockState state, Direction facing, float anim,
                                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                                   int packedLight, int packedOverlay, FluidVisual visual) {
        BakedModel fluidPart = model.getPart("Fluid");
        if (fluidPart == null) return false;

        // Forge BakedModel implementations (esp. OBJ/part models) may return no quads when state is null.
        List<BakedQuad> quads = collectChemplantFluidQuads(fluidPart, state);
        if (quads.isEmpty()) return false;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(CHEMPLANT_FLUID_TEX);

        // Поворот блока уже применён caller'ом через poseStack (setupBlockTransform).
        // Дублирующий transformQuadsByFacing убран — иначе fluid смещается при N/S/E/W.
        float du = -anim / 100f;
        float dv = (float) (chemicalSps(anim * 0.1) * 0.1 - 0.25);
        quads = ModelHelper.offsetQuadUvsWrapped(quads, du, dv,
            sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());

        VertexConsumer vc = bufferSource.getBuffer(FLUID_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        float r = visual.r(), g = visual.g(), b = visual.b(), a = 0.5f;
        for (BakedQuad quad : quads) {
            vc.putBulkData(pose, quad, r, g, b, a, packedLight, packedOverlay, false);
        }
        return true;
    }

    private static List<BakedQuad> collectChemplantFluidQuads(BakedModel fluidPart, @Nullable BlockState state) {
        RandomSource rand = RandomSource.create(42);
        for (RenderType layer : new RenderType[]{null, RenderType.cutout(), RenderType.solid(), RenderType.translucent()}) {
            List<BakedQuad> quads = new ArrayList<>();
            for (Direction dir : Direction.values()) {
                quads.addAll(fluidPart.getQuads(state, dir, rand, ModelData.EMPTY, layer));
            }
            quads.addAll(fluidPart.getQuads(state, null, rand, ModelData.EMPTY, layer));
            if (!quads.isEmpty()) {
                return quads;
            }
        }
        return List.of();
    }
    //?}

    //? if fabric {
    /*private static boolean tryRenderFluidBakedPart(MachineChemicalPlantBakedModel model, BlockState state, Direction facing, float anim,
                                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                                   int packedLight, int packedOverlay, FluidVisual visual) {
        BakedModel fluidPart = model.getPart("Fluid");
        if (fluidPart == null) return false;

        List<BakedQuad> quads = collectChemplantFluidQuads(fluidPart, state);
        if (quads.isEmpty()) return false;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(CHEMPLANT_FLUID_TEX);

        // Поворот блока уже применён caller'ом через poseStack (setupBlockTransform).
        float du = -anim / 100f;
        float dv = (float) (chemicalSps(anim * 0.1) * 0.1 - 0.25);
        quads = ModelHelper.offsetQuadUvsWrapped(quads, du, dv,
            sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());

        // В Fabric overload putBulkData(...) не принимает alpha → получается полностью непрозрачная жидкость.
        // Поэтому для baked-квадов отправляем вершины вручную с alpha=0.5 как в 1.7.10.
        VertexConsumer vc = bufferSource.getBuffer(FLUID_RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMat = pose.pose();
        Matrix3f normalMat = pose.normal();

        float r = visual.r(), g = visual.g(), b = visual.b(), a = 0.5f;
        for (BakedQuad quad : quads) {
            emitQuadWithAlpha(vc, poseMat, normalMat, quad, r, g, b, a, packedOverlay, packedLight);
        }
        return true;
    }

    private static void emitQuadWithAlpha(VertexConsumer vc, Matrix4f poseMat, Matrix3f normalMat,
                                          BakedQuad quad,
                                          float r, float g, float b, float a,
                                          int packedOverlay, int packedLight) {
        int[] v = quad.getVertices();
        // BakedQuad хранит 4 вершины; в 1.20.1 формат BLOCK = 8 int на вершину.
        // x,y,z,u,v лежат как float bits.
        Direction dir = quad.getDirection();
        float nx = dir.getStepX();
        float ny = dir.getStepY();
        float nz = dir.getStepZ();

        for (int i = 0; i < 4; i++) {
            int base = i * 8;
            float x = Float.intBitsToFloat(v[base]);
            float y = Float.intBitsToFloat(v[base + 1]);
            float z = Float.intBitsToFloat(v[base + 2]);
            float u = Float.intBitsToFloat(v[base + 4]);
            float vv = Float.intBitsToFloat(v[base + 5]);

            vc.vertex(poseMat, x, y, z)
                .color(r, g, b, a)
                .uv(u, vv)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normalMat, nx, ny, nz)
                .endVertex();
        }
    }

    private static List<BakedQuad> collectChemplantFluidQuads(BakedModel fluidPart, @Nullable BlockState state) {
        RandomSource rand = RandomSource.create(42);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(fluidPart.getQuads(state, dir, rand));
        }
        quads.addAll(fluidPart.getQuads(state, null, rand));
        return quads;
    }
    *///?}

    public void renderStaticBase(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(BASE);
        if (part != null) {
            var r = MeshRenderCache.getOrCreateRenderer("chemplant_" + BASE, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    public void renderStaticFrame(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                 @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(FRAME);
        if (part != null) {
            var r = MeshRenderCache.getOrCreateRenderer("chemplant_" + FRAME, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    public void renderAnimatedPart(PoseStack poseStack, int packedLight, String partName,
                                   Matrix4f transform, BlockPos blockPos,
                                   @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(partName);
        if (part != null) {
            poseStack.pushPose();
            if (transform != null) {
                poseStack.last().pose().mul(transform);
            }
            var r = MeshRenderCache.getOrCreateRenderer("chemplant_" + partName, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            poseStack.popPose();
        }
    }
}

