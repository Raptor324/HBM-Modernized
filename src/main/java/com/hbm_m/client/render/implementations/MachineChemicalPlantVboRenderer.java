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
import com.hbm_m.client.render.GlobalMeshCache;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

//? if forge {
/*import net.minecraftforge.client.model.data.ModelData;
*///?}

import net.minecraft.world.level.block.entity.BlockEntity;

//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
*///?}
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//?}
//? if forge {
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
@Environment(EnvType.CLIENT)//?}
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
        if (recipe == null) return null;

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
        Direction facing = be.getBlockState().getValue(MachineChemicalPlantBlock.FACING);
        //? if forge {
        /*if (!tryRenderFluidBakedPart(model, facing, anim, poseStack, bufferSource, packedLight, packedOverlay, visual)) {
            renderSwirl(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay, visual);
        }
        *///?} else {
        renderSwirl(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay, visual);
        //?}
        poseStack.popPose();
    }

    private static final ResourceLocation CHEMPLANT_FLUID_TEX =
        new ResourceLocation("hbm_m", "block/machine/chemical_plant_fluid");

    /** Render-type для жидкости в BER: не deferred translucent (чтобы не зависеть от terrain batch). */
    private static final RenderType FLUID_RENDER_TYPE = RenderType.translucentMovingBlock();

    //? if forge {
    /*private static boolean tryRenderFluidBakedPart(MachineChemicalPlantBakedModel model, Direction facing, float anim,
                                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                                   int packedLight, int packedOverlay, FluidVisual visual) {
        BakedModel fluidPart = model.getPart("Fluid");
        if (fluidPart == null) return false;

        List<BakedQuad> quads = collectChemplantFluidQuads(fluidPart);
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

    private static List<BakedQuad> collectChemplantFluidQuads(BakedModel fluidPart) {
        RandomSource rand = RandomSource.create(42);
        for (RenderType layer : new RenderType[]{null, RenderType.cutout(), RenderType.solid(), RenderType.translucent()}) {
            List<BakedQuad> quads = new ArrayList<>();
            for (Direction dir : Direction.values()) {
                quads.addAll(fluidPart.getQuads(null, dir, rand, ModelData.EMPTY, layer));
            }
            quads.addAll(fluidPart.getQuads(null, null, rand, ModelData.EMPTY, layer));
            if (!quads.isEmpty()) {
                return quads;
            }
        }
        return List.of();
    }
    *///?}

    private static void renderSwirl(MachineChemicalPlantBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, int packedOverlay, FluidVisual visual) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(CHEMPLANT_FLUID_TEX);
        if (sprite == null) return;

        float red = visual.r();
        float green = visual.g();
        float blue = visual.b();
        float alpha = 0.5F;
        float fill = 1.0F;

        long time = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        float t = (time + partialTick) * 0.02F;
        float scroll = t - (float) Mth.floor(t);

        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();

        float du = (uMax - uMin) * scroll;
        float dv = (vMax - vMin) * scroll;

        float x0 = -0.35F;
        float x1 = 0.35F;
        float z0 = 0.65F;
        float z1 = 1.35F;
        float y0 = 1.02F;
        float y1 = y0 + (1.34F - y0) * fill;

        VertexConsumer vc = buffer.getBuffer(FLUID_RENDER_TYPE);

        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMat = pose.pose();
        Matrix3f normalMat = pose.normal();

        addVertex(vc, poseMat, normalMat, x0, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 0, 1, 0);
        addVertex(vc, poseMat, normalMat, x0, y1, z1, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 0, 1, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 0, 1, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z0, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 0, 1, 0);

        addVertex(vc, poseMat, normalMat, x0, y0, z0, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 0, 0, -1);
        addVertex(vc, poseMat, normalMat, x1, y0, z0, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 0, 0, -1);
        addVertex(vc, poseMat, normalMat, x1, y1, z0, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 0, 0, -1);
        addVertex(vc, poseMat, normalMat, x0, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 0, 0, -1);

        addVertex(vc, poseMat, normalMat, x0, y0, z1, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 0, 0, 1);
        addVertex(vc, poseMat, normalMat, x0, y1, z1, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 0, 0, 1);
        addVertex(vc, poseMat, normalMat, x1, y1, z1, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 0, 0, 1);
        addVertex(vc, poseMat, normalMat, x1, y0, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 0, 0, 1);

        addVertex(vc, poseMat, normalMat, x0, y0, z0, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, -1, 0, 0);
        addVertex(vc, poseMat, normalMat, x0, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, -1, 0, 0);
        addVertex(vc, poseMat, normalMat, x0, y1, z1, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, -1, 0, 0);
        addVertex(vc, poseMat, normalMat, x0, y0, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, -1, 0, 0);

        addVertex(vc, poseMat, normalMat, x1, y0, z0, red, green, blue, alpha, uMin + du, vMax + dv, packedOverlay, packedLight, 1, 0, 0);
        addVertex(vc, poseMat, normalMat, x1, y0, z1, red, green, blue, alpha, uMax + du, vMax + dv, packedOverlay, packedLight, 1, 0, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z1, red, green, blue, alpha, uMax + du, vMin + dv, packedOverlay, packedLight, 1, 0, 0);
        addVertex(vc, poseMat, normalMat, x1, y1, z0, red, green, blue, alpha, uMin + du, vMin + dv, packedOverlay, packedLight, 1, 0, 0);

        poseStack.popPose();
    }

    private static void addVertex(VertexConsumer vc, Matrix4f poseMat, Matrix3f normalMat,
                                  float x, float y, float z,
                                  float r, float g, float b, float a,
                                  float u, float v,
                                  int overlay, int light,
                                  float nx, float ny, float nz) {
        vc.vertex(poseMat, x, y, z)
            .color(r, g, b, a)
            .uv(u, v)
            .overlayCoords(overlay)
            .uv2(light)
            .normal(normalMat, nx, ny, nz)
            .endVertex();
    }

    public void renderStaticBase(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(BASE);
        if (part != null) {
            var r = GlobalMeshCache.getOrCreateRenderer("chemplant_" + BASE, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
        }
    }

    public void renderStaticFrame(PoseStack poseStack, int packedLight, BlockPos blockPos,
                                 @Nullable BlockEntity blockEntity, @Nullable MultiBufferSource bufferSource) {
        BakedModel part = model.getPart(FRAME);
        if (part != null) {
            var r = GlobalMeshCache.getOrCreateRenderer("chemplant_" + FRAME, part);
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
            var r = GlobalMeshCache.getOrCreateRenderer("chemplant_" + partName, part);
            if (r != null) r.render(poseStack, packedLight, blockPos, blockEntity, bufferSource);
            poseStack.popPose();
        }
    }
}
