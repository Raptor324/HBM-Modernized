package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.block.machines.MachineChemicalFactoryBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineChemicalFactoryBlockEntity;
import com.hbm_m.client.model.AbstractMultipartBakedModel;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.util.MultipartFacingTransforms;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;

/**
 * Chemical Factory на фабрике {@link MachineRenderers} — порт 1.7.10
 * {@code RenderChemicalFactory}:
 * <ul>
 *   <li>{@code Base} — статика; {@code Frame} — рендерится по blockstate-свойству
 *       FRAME (видима при блоке над любой клеткой верхнего пояса структуры, считает
 *       сервером {@code MultiblockFrameHelper} — та же система, что у advassembler;
 *       оригинал 1.7.10 проверял только блок строго над ядром);</li>
 *   <li>{@code Fan1}/{@code Fan2} — вращение вокруг собственных пивотов
 *       (±1, 0, 0) на {@code -anim*45°}/тик, пока работает хотя бы одна линия
 *       (anim инкрементится в BE только при didProcess, как в оригинале);</li>
 *   <li>поворот по FACING — как в оригинале: rotate(90) + таблица
 *       (N=0, W=90, S=180, E=270), т.е. {@code 90 + legacyFacingRotationYDegrees};</li>
 *   <li>финальный {@code translate(-0.5, 0, -0.5)} — компенсация baked JSON
 *       root translation (0.5, 0, 0.5), см. пивоты ниже.</li>
 * </ul>
 * <p>
 * Пивоты вентиляторов в baked-координатах = OBJ-пивот (±1, 0, 0) + JSON translation
 * (0.5, 0, 0.5). Если меняешь JSON translation — pivot = OBJ_PIVOT + JSON_translation.
 */
public final class MachineChemicalFactoryRenderer {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private static final float FAN1_PIVOT_X = 1.5f;
    private static final float FAN2_PIVOT_X = -0.5f;
    private static final float FAN_PIVOT_Z = 0.5f;

    public static void register() {
        MachineRenderers.machine("chemfactory", ModBlockEntities.CHEMICAL_FACTORY_BE.get(),
                MachineChemicalFactoryBlockEntity.class)
            .part("Base")
            .part("Fan1", MachineChemicalFactoryRenderer::animateFan1)
            .part("Fan2", MachineChemicalFactoryRenderer::animateFan2)
            .dynamicPart("Frame", MachineChemicalFactoryRenderer::frameQuads,
                    // Ключ обязан различать FRAME=false/true: константный ключ кешировал бы
                    // рендерер по первому встреченному состоянию навсегда (см. advassembler/chemplant).
                    MachineChemicalFactoryRenderer::frameCacheKey)
            .blockTransform(MachineChemicalFactoryRenderer::applyBlockTransform)
            .register();
    }

    private MachineChemicalFactoryRenderer() {}

    // ── Блочный трансформ ──────────────────────────────────────────────

    private static void applyBlockTransform(MachineChemicalFactoryBlockEntity be, LegacyAnimator animator) {
        var state = be.getBlockState();
        animator.translate(0.5, 0.0, 0.5);
        if (state.hasProperty(MachineChemicalFactoryBlock.FACING)) {
            float facingRot = MultipartFacingTransforms.legacyFacingRotationYDegrees(
                    state.getValue(MachineChemicalFactoryBlock.FACING));
            animator.rotate(90f + facingRot, 0, 1, 0);
        } else {
            animator.rotate(90, 0, 1, 0);
        }
        // baked-space сдвиг -0.5/-0.5: части выпечены с JSON root translation (0.5, 0, 0.5)
        animator.translate(-0.5f, 0.0f, -0.5f);
    }

    // ── Части ──────────────────────────────────────────────────────────

    private static boolean animateFan1(MachineChemicalFactoryBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        return animateFan(be, partialTick, pose, FAN1_PIVOT_X);
    }

    private static boolean animateFan2(MachineChemicalFactoryBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        return animateFan(be, partialTick, pose, FAN2_PIVOT_X);
    }

    /** Оригинал: translate(±1,0,0) → rotate(-anim*45 % 360) → translate(∓1,0,0). */
    private static boolean animateFan(MachineChemicalFactoryBlockEntity be, float partialTick,
                                      PoseStack pose, float pivotX) {
        float anim = be.getAnim(partialTick);
        float deg = (-anim * 45f) % 360f;
        if (deg < 0f) deg += 360f;
        pose.last().pose().mul(new Matrix4f()
                .translate(pivotX, 0f, FAN_PIVOT_Z)
                .rotateY(deg * DEG_TO_RAD)
                .translate(-pivotX, 0f, -FAN_PIVOT_Z));
        return true;
    }

    // ── Frame: видима только по свойству FRAME ─────────────────────────

    private static String frameCacheKey(MachineChemicalFactoryBlockEntity be) {
        var state = be.getBlockState();
        boolean frame = state.hasProperty(MachineChemicalFactoryBlock.FRAME)
                && state.getValue(MachineChemicalFactoryBlock.FRAME);
        return String.valueOf(frame);
    }

    private static List<BakedQuad> frameQuads(MachineChemicalFactoryBlockEntity be) {
        var state = be.getBlockState();
        if (!state.hasProperty(MachineChemicalFactoryBlock.FRAME)
                || !state.getValue(MachineChemicalFactoryBlock.FRAME)) {
            return List.of();
        }
        BakedModel part = factoryPart(be, "Frame");
        if (part == null) return List.of();
        return MeshRenderCache.getOrCompile("chemfactory_Frame", part);
    }

    private static @Nullable BakedModel factoryPart(MachineChemicalFactoryBlockEntity be, String partName) {
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
        return raw instanceof AbstractMultipartBakedModel mp ? mp.getPart(partName) : null;
    }
}
