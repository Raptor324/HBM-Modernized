//? if forge || neoforge {
package com.hbm_m.client.render.item;

import java.util.List;

import com.hbm_m.blockentity.IPersistentNBT;
import com.hbm_m.client.model.ConfiguredMultipartBakedModel;
import com.hbm_m.client.render.implementations.MachineFluidTankRenderer;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * BEWLR предмета цистерны — порт 1.7.10 {@code RenderFluidTank.getRenderer()}
 * ({@code ItemRenderBase.renderCommonWithStack}): предмет читает {@code persistent}-тег
 * дропа и рендерит Frame + Tank, перетекстурированный под залитую жидкость
 * (тот же механизм, что и в мире — {@link MachineFluidTankRenderer}); взорванный
 * бак показывает wrecked-модель {@code fluid_tank_exploded}.
 *
 * <p>Display-трансформы приходят из item-модели {@code fluid_tank.json} (ваниль
 * применяет их до {@code renderByItem}), т.е. предмет стоит ровно там же, где
 * стояла статичная модель — меняется только текстура Tank.</p>
 */
public class FluidTankItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final FluidTankItemRenderer INSTANCE = new FluidTankItemRenderer();

    private static final RandomSource RANDOM = RandomSource.create(42);

    private FluidTankItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /**
     * Модель резолвится через blockstate (BlockModelShaper), а НЕ по голому RL через
     * {@code ModelManager.getModel}: в реестре запечённых моделей лежат только
     * ModelResourceLocation-ключи вариантов блокстейта/предметов, голый RL вернул бы
     * missing-модель и предмет рендерился бы пустым. Тот же путь, что и у мирового
     * {@code MachineFluidTankRenderer}.
     */
    private static BakedModel resolveTankModel(boolean exploded) {
        BlockState state = com.hbm_m.block.ModBlocks.FLUID_TANK.get().defaultBlockState();
        if (exploded) {
            state = state.setValue(com.hbm_m.block.machines.MachineFluidTankBlock.EXPLODED, true);
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var tag = PlatformHooks.getItemTag(stack);
        var persistent = tag != null ? tag.getCompound(IPersistentNBT.NBT_PERSISTENT_KEY) : null;

        FluidTank tank = new FluidTank(ModFluids.NONE.getSource(), 0);
        boolean exploded = false;
        if (persistent != null && persistent.contains("tank")) {
            tank.readFromNBT(persistent, "tank");
            exploded = persistent.getBoolean("hasExploded");
        }

        //? if forge {
        // Гвард HbmItemDisplayWrapper (BakingCompleted) глушит форджевское применение display
        // у всех isCustomRenderer-моделей — конвенция мода: BEWLR применяет JSON display сам
        // (паттерн ItemRenderDetonatorLaser). Восстанавливаем ванильную последовательность:
        // applyTransform(T), затем translate(-0.5).
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F); // отменить ванильный translate(-0.5) до renderByItem
        BakedModel displayModel = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        boolean leftHand = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        com.hbm_m.client.compat.itemtransformhelper.ItemTransformHelperCompat
                .resolveDisplayTransforms(displayModel, resolveTankModel(exploded))
                .getTransform(displayContext)
                .apply(leftHand, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        //?}

        // 1.7.10: glDisable(GL_CULL_FACE) вокруг рендера бака.
        RenderSystem.disableCull();
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutoutMipped());
        PoseStack.Pose pose = poseStack.last();

        if (!exploded) {
            BakedModel intact = resolveTankModel(false);
            BakedModel frame = part(intact, "Frame");
            if (frame != null) {
                drawQuads(consumer, pose, MachineFluidTankRenderer.collectAllQuads(frame), packedLight, packedOverlay);
            }
            BakedModel tankPart = part(intact, "Tank");
            if (tankPart != null) {
                drawQuads(consumer, pose,
                        MachineFluidTankRenderer.buildRetexturedTankQuads(tankPart, textureFromTank(tank)),
                        packedLight, packedOverlay);
            }
        } else {
            BakedModel wrecked = resolveTankModel(true);
            if (wrecked != null) {
                drawQuads(consumer, pose, MachineFluidTankRenderer.collectAllQuads(wrecked), packedLight, packedOverlay);
            }
        }

        RenderSystem.enableCull();

        //? if forge {
        poseStack.popPose();
        //?}
    }

    private static void drawQuads(VertexConsumer consumer, PoseStack.Pose pose, List<BakedQuad> quads,
                                  int packedLight, int packedOverlay) {
        for (BakedQuad quad : quads) {
            RenderHooks.putBulkData(consumer, pose, quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay, false);
        }
    }

    @Nullable
    private static BakedModel part(BakedModel model, String part) {
        if (model instanceof ConfiguredMultipartBakedModel multipart) {
            return multipart.getPart(part);
        }
        return null;
    }

    /** Разрешение текстуры бака по жидкости — зеркало {@code MachineFluidTankBlockEntity.getTankTextureLocation()}. */
    private static ResourceLocation textureFromTank(FluidTank tank) {
        Fluid fluid = tank.getTankType();
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/tank/tank_none");
        }
        ResourceLocation typeId = BuiltInRegistries.FLUID.getKey(fluid);
        String fluidName = typeId != null ? typeId.getPath() : "none";
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/tank/tank_" + fluidName);
    }
}
//?}
