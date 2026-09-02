package com.hbm_m.client.render.implementations;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAssemblerBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Сборщик на фабрике {@link MachineRenderers}: Body — статика; Slider/Arm — анимация;
 * 4 шестерни — одна часть модели "Cog" с четырьмя ключами (CogA..CogD);
 * иконка рецепта — immediate-хук.
 * <p>
 * Отличие от легаси: idle-combined меш (слияние Body+Slider+Arm+4×Cog в один VBO
 * для простаивающих машин) не переносится — при автоматическом MDI все части
 * всех машин одного типа и так собираются в один мульти-draw, отдельная
 * склейка больше не даёт выигрыша.
 */
public final class MachineAssemblerRenderer {

    /** Degrees → radians multiplier. */
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    // Root transform from machine_assembler.json shifts model by (1,0,2); cog center is there, not at origin.
    private static final float ROOT_TX = 1f, ROOT_TZ = 2f;
    private static final float VBO_COG_OFFSET_X = 1f, VBO_COG_OFFSET_Z = 2f;

    private static final float[][] COG_POSITIONS = {
            {-0.6f, 0.75f, 1.0625f},
            {0.6f, 0.75f, 1.0625f},
            {-0.6f, 0.75f, -1.0625f},
            {0.6f, 0.75f, -1.0625f},
    };

    public static void register() {
        MachineRenderers.machine("assembler", ModBlockEntities.MACHINE_ASSEMBLER_BE.get(),
                MachineAssemblerBlockEntity.class)
            .part("Body", MachineAssemblerRenderer::animateBody)
            .part("Slider", MachineAssemblerRenderer::animateSlider)
            .part("Arm", MachineAssemblerRenderer::animateArm)
            .part("Cog", "CogA", MachineAssemblerRenderer::animateCogA)
            .part("Cog", "CogB", MachineAssemblerRenderer::animateCogB)
            .part("Cog", "CogC", MachineAssemblerRenderer::animateCogC)
            .part("Cog", "CogD", MachineAssemblerRenderer::animateCogD)
            .hook(MachineAssemblerRenderer::renderRecipeIcon)
            .register();
    }

    private MachineAssemblerRenderer() {}

    // ==================== ANIMATION ====================
    // Легаси-ориентация модели: полный поворот на -90° вокруг центра блока
    // (поверх блочного трансформа, который уже даёт setupBlockTransform).

    private static void applyLegacyYaw(PoseStack pose) {
        pose.translate(0.5f, 0f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
        pose.translate(-0.5f, 0f, -0.5f);
    }

    /**
     * Body в легаси рисовался внутри той же -90°-группы, что и анимированные части,
     * с компенсацией T(-0.5,0,-0.5) (root-перенос JSON (1,0,2) в baked-координатах
     * multipart-модели). Статическая часть фабрики этого офсета не имеет — применяем
     * трансформом «аниматора» (движок сам снимает матрицу).
     */
    private static boolean animateBody(MachineAssemblerBlockEntity be, float partialTick,
                                       long gameTime, PoseStack pose) {
        applyLegacyYaw(pose);
        pose.translate(-0.5f, 0f, -0.5f);
        return true;
    }

    // Анимации легаси тикают от wallclock-миллисекунд (System.currentTimeMillis),
    // НЕ от игрового времени (тики в 50 раз медленнее).

    private static boolean animateSlider(MachineAssemblerBlockEntity be, float partialTick,
                                             long gameTime, PoseStack pose) {
        // Slider: ping-pong 0..500 за 5000ms
        float sliderX = sliderX(be, System.currentTimeMillis());
        applyLegacyYaw(pose);
        pose.last().pose().mul(new Matrix4f().translate(sliderX, 0, 0).translate(-0.5f, 0f, -0.5f));
        return true;
    }

    private static boolean animateArm(MachineAssemblerBlockEntity be, float partialTick,
                                      long gameTime, PoseStack pose) {
        long time = System.currentTimeMillis();
        // Arm sway
        float armZ = 0;
        if (be.isCrafting()) {
            double swayRaw = (time % 2000) / 2.0;
            float sway = (float) Math.sin(swayRaw / Math.PI / 50);
            armZ = sway * 0.3f;
        }
        // Arm ездит ВМЕСТЕ со Slider (общий sliderX) + добавляет своё качание armZ.
        applyLegacyYaw(pose);
        pose.last().pose().mul(new Matrix4f().translate(sliderX(be, time), 0, armZ).translate(-0.5f, 0f, -0.5f));
        return true;
    }

    /** Slider: ping-pong 0..500 за 5000ms (общий для Slider и Arm). */
    private static float sliderX(MachineAssemblerBlockEntity be, long time) {
        if (!be.isCrafting()) return 0;
        long t = (time % 5000) / 5;
        int offset = (int) (t > 500 ? 500 - (t - 500) : t);
        return offset * 0.003f - 0.75f;
    }

    private static float cogRotation(MachineAssemblerBlockEntity be, long time) {
        // Cog rotation
        return be.isCrafting() ? (float) ((time % (360L * 5)) / 5.0) : 0f;
    }

    private static boolean animateCogA(MachineAssemblerBlockEntity be, float pt, long gameTime, PoseStack pose) {
        return animateCog(be, System.currentTimeMillis(), pose, COG_POSITIONS[0][0], COG_POSITIONS[0][1], COG_POSITIONS[0][2], -cogRotation(be, System.currentTimeMillis()));
    }
    private static boolean animateCogB(MachineAssemblerBlockEntity be, float pt, long gameTime, PoseStack pose) {
        return animateCog(be, System.currentTimeMillis(), pose, COG_POSITIONS[1][0], COG_POSITIONS[1][1], COG_POSITIONS[1][2], cogRotation(be, System.currentTimeMillis()));
    }
    private static boolean animateCogC(MachineAssemblerBlockEntity be, float pt, long gameTime, PoseStack pose) {
        return animateCog(be, System.currentTimeMillis(), pose, COG_POSITIONS[2][0], COG_POSITIONS[2][1], COG_POSITIONS[2][2], -cogRotation(be, System.currentTimeMillis()));
    }
    private static boolean animateCogD(MachineAssemblerBlockEntity be, float pt, long gameTime, PoseStack pose) {
        return animateCog(be, System.currentTimeMillis(), pose, COG_POSITIONS[3][0], COG_POSITIONS[3][1], COG_POSITIONS[3][2], cogRotation(be, System.currentTimeMillis()));
    }

    private static boolean animateCog(MachineAssemblerBlockEntity be, long time, PoseStack pose,
                                      float cx, float cy, float cz, float rotationDeg) {
        applyLegacyYaw(pose);
        pose.last().pose().mul(buildCogMatrix(cx, cy, cz, rotationDeg));
        return true;
    }

    private static Matrix4f buildCogMatrix(float cx, float cy, float cz, float rotationDeg) {
        return new Matrix4f()
                .translate(cx - 0.5f + VBO_COG_OFFSET_X, cy, cz - 0.5f + VBO_COG_OFFSET_Z)
                .rotateZ(rotationDeg * DEG_TO_RAD)
                .translate(-ROOT_TX, 0f, -ROOT_TZ);
    }

    // ==================== RECIPE ICON (hook) ====================

    /**
     * Иконка рецепта поверх машины. Стек хука уже несёт блочный трансформ
     * (T(0.5,0,0.5)·R(90)·R(legacy facing)); старый путь из «сырого» стека делал
     * T(0.5)·R(legacy)·T(-0.5)·R(90)·T(0,1.0625,0)·items, что в текущем фрейме
     * эквивалентно R(-90)·T(-0.5,0,-0.5)·R(90)·T(0,1.0625,0)·items.
     * Видимость уже отфильтрована куллингом и анимационной дистанцией движка.
     */
    private static void renderRecipeIcon(MachineAssemblerBlockEntity be, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, MachineRenderApi api) {
        ItemStack icon = be.getClientRecipeIcon();
        if (icon.isEmpty()) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90));
        poseStack.translate(-0.5, 0, -0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        poseStack.translate(0, 1.0625, 0);

        if (icon.getItem() instanceof BlockItem bi) {
            var blockModel = mc.getBlockRenderer().getBlockModel(bi.getBlock().defaultBlockState());
            if (blockModel.isGui3d()) {
                poseStack.translate(-1, -0.2625, 1);
            } else {
                poseStack.translate(-1, -0.125, 1);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
        } else {
            poseStack.translate(-1, -0.2, 1);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        }

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        mc.getItemRenderer().renderStatic(
                icon,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                be.getLevel(),
                0
        );

        poseStack.popPose();
    }
}
