package com.hbm_m.client.render.implementations;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.decorations.SoyuzLauncherBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.client.model.SoyuzLauncherBakedModel;
import com.hbm_m.client.model.SoyuzRocketBakedModel;
import com.hbm_m.client.render.AbstractPartBasedRenderer;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Пусковая установка Союза на фабрике {@link MachineRenderers}: Table/TowerBase/
 * SupportBase/Legs — статика; Tower/Support — анимированные створы arms (поворот
 * на 45° при старте); ракета-превью — динамическая часть из injected-модели
 * (видима только при hasRocket).
 */
public final class SoyuzLauncherRenderer {

    private static final ResourceLocation ROCKET_MODEL_ID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/deco_soyuz_rocket");

    /** Original {@code SoyuzPronter.SoyuzSkin}: 0 Original, 1 Luna, 2 Post War (authentic). */
    public static ResourceLocation rocketModelId(int skin) {
        return switch (skin) {
            case 1 -> ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/soyuz_rocket_luna");
            case 2 -> ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "block/soyuz_rocket_authentic");
            default -> ROCKET_MODEL_ID;
        };
    }

    /** Ticks over which the arms fully open/close, matching legacy {@code timer = 20}. */
    private static final int ARM_ANIM_TICKS = 20;
    private static final double ARM_OPEN_DEGREES = 45.0D;

    public static void register() {
        MachineRenderers.machine("soyuzlauncher", ModBlockEntities.SOYUZ_LAUNCHER_BE.get(),
                SoyuzLauncherBlockEntity.class)
            .part(SoyuzLauncherBakedModel.TABLE)
            .part(SoyuzLauncherBakedModel.TOWER_BASE)
            .part(SoyuzLauncherBakedModel.SUPPORT_BASE)
            .part(SoyuzLauncherBakedModel.LEGS)
            .part(SoyuzLauncherBakedModel.TOWER, SoyuzLauncherRenderer::animateTower)
            .part(SoyuzLauncherBakedModel.SUPPORT, SoyuzLauncherRenderer::animateSupport)
            .dynamicPart("Rocket", SoyuzLauncherRenderer::animateRocket,
                    SoyuzLauncherRenderer::rocketQuads, be -> "rocket" + be.getRocketType())
            .facing(be -> be.getBlockState().getValue(SoyuzLauncherBlock.FACING))
            // Original RenderSoyuzLauncher: glTranslate(x + 0.5, y - 4, z + 0.5) - der Kern sitzt 4 Bloecke ueber dem Boden
            .blockTransform((be, anim) -> {
                anim.setupBlockTransform(be.getBlockState().getValue(SoyuzLauncherBlock.FACING));
                anim.translate(0.0, -SoyuzLauncherBlock.HEIGHT, 0.0);
            })
            .register();
    }

    private SoyuzLauncherRenderer() {}

    /** 0 = сомкнуты на ракете, 45 = полностью раскрыты. Вердикт легаси RenderSoyuzLauncher. */
    private static double armAngle(SoyuzLauncherBlockEntity be, float partialTick) {
        double rot = be.hasRocket() ? 0.0D : ARM_OPEN_DEGREES;
        if (be.isStarting() && be.getCountdown() < ARM_ANIM_TICKS) {
            rot = (ARM_ANIM_TICKS - be.getCountdown() + partialTick) * ARM_OPEN_DEGREES / ARM_ANIM_TICKS;
        }
        return rot;
    }

    private static boolean animateTower(SoyuzLauncherBlockEntity be, float partialTick,
                                        long gameTime, PoseStack pose) {
        double rot = armAngle(be, partialTick);
        if (rot == 0.0D) return true;
        applyPivot(pose, 0, 5.5, 5.5, rot, true);
        return true;
    }

    private static boolean animateSupport(SoyuzLauncherBlockEntity be, float partialTick,
                                          long gameTime, PoseStack pose) {
        double rot = armAngle(be, partialTick);
        if (rot == 0.0D) return true;
        applyPivot(pose, 0, 5.5, -6.5, rot, false);
        return true;
    }

    /** Legacy: glTranslate(px,py,pz) → glRotate(rot,axisX,0,0) → glTranslate(-px,-py,-pz). */
    private static void applyPivot(PoseStack pose, double px, double py, double pz, double rot, boolean positiveX) {
        pose.translate(px, py, pz);
        pose.mulPose((positiveX ? com.mojang.math.Axis.XP : com.mojang.math.Axis.XN).rotationDegrees((float) rot));
        pose.translate(-px, -py, -pz);
    }

    private static boolean animateRocket(SoyuzLauncherBlockEntity be, float partialTick,
                                         long gameTime, PoseStack pose) {
        if (!be.hasRocket()) return false;
        pose.translate(0.0, 5.0, 0.0);
        return true;
    }

    private static List<net.minecraft.client.renderer.block.model.BakedQuad> rocketQuads(
            SoyuzLauncherBlockEntity be) {
        int skin = Math.max(0, be.getRocketType());
        BakedModel rocketModel = getRocketModel(skin);
        if (!(rocketModel instanceof SoyuzRocketBakedModel model)) return List.of();
        BakedModel part = model.getPart(SoyuzRocketBakedModel.ROCKET);
        if (part == null) return List.of();
        return MeshRenderCache.getOrCompilePartGeometry("soyuzlauncher/rocket" + skin, part).solidQuads();
    }

    @Nullable
    private static BakedModel getRocketModel(int skin) {
        var modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = PlatformHooks.getModel(modelManager, rocketModelId(skin));
        return (model == null || model == modelManager.getMissingModel()) ? null : model;
    }
}
