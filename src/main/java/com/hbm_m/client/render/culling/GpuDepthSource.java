package com.hbm_m.client.render.culling;



import java.lang.reflect.Field;

import java.lang.reflect.Method;



import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;

import com.hbm_m.main.MainRegistry;



import net.minecraft.client.Minecraft;



//? if forge {

import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.api.distmarker.OnlyIn;

//?}

//? if fabric {

/*import net.fabricmc.api.EnvType;

import net.fabricmc.api.Environment;

*///?}



/**

 * Depth для {@link GpuCullingPipeline}: lag-1 снимок main depth после BER

 * (террейн + сущности + VBO машин прошлого кадра) или Iris {@code RenderTargets}.

 */

//? if forge {

@OnlyIn(Dist.CLIENT)

//?}

//? if fabric {

/*@Environment(EnvType.CLIENT)*///?}

public final class GpuDepthSource {



    public record Snapshot(int textureId, int width, int height, boolean valid) {}



    /**

     * Depth конца кадра N−1 (после BER): для dispatch в кадре N — окклюзия

     * между машинами без self-occlusion в том же BER-проходе.

     */

    private static volatile Snapshot lag1PostBerDepth = new Snapshot(0, 0, 0, false);



    private static volatile boolean irisReflectionTried = false;

    private static volatile boolean irisReflectionOk = false;

    private static Class<?> irisPipelineClass;

    private static Field irisRenderTargetsField;

    private static Method irisGetDepthTexture;



    private GpuDepthSource() {}



    /**

     * Снимок main depth после BER и MDI-flush. Вызывать в конце

     * {@code AFTER_BLOCK_ENTITIES} — станет входом dispatch следующего кадра.

     */

    public static void capturePostBerDepthForNextFrame() {

        lag1PostBerDepth = capture();

    }



    /**

     * Depth для compute dispatch: lag-1 post-BER, иначе текущий main (первые кадры).

     */

    public static Snapshot takeDepthForOcclusionDispatch() {

        Snapshot lag = lag1PostBerDepth;

        if (lag.valid()) {

            return lag;

        }

        return capture();

    }

    /**
     * Depth в {@code AFTER_BLOCK_ENTITIES} до MDI: текущий main (BER кадра уже записан в depth).
     */
    public static Snapshot takeDepthForSameFrameCull() {
        return capture();
    }

    public static void clearLag1Depth() {

        lag1PostBerDepth = new Snapshot(0, 0, 0, false);

    }



    public static Snapshot capture() {

        Minecraft mc = Minecraft.getInstance();

        var target = mc.getMainRenderTarget();

        int w = target.width;

        int h = target.height;

        if (w <= 0 || h <= 0) {

            return new Snapshot(0, 0, 0, false);

        }



        if (ShaderCompatibilityDetector.isExternalShaderActive()) {

            int irisDepth = tryGetIrisDepthTexture();

            if (irisDepth > 0) {

                return new Snapshot(irisDepth, w, h, true);

            }

            int mainDepth = target.getDepthTextureId();

            return new Snapshot(mainDepth, w, h, mainDepth > 0);

        }



        int depth = target.getDepthTextureId();

        return new Snapshot(depth, w, h, depth > 0);

    }



    private static int tryGetIrisDepthTexture() {

        ensureIrisReflection();

        if (!irisReflectionOk) {

            return 0;

        }

        try {

            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");

            Method getPipelineManager = irisClass.getMethod("getPipelineManager");

            Object pipelineManager = getPipelineManager.invoke(null);

            if (pipelineManager == null) {

                return 0;

            }

            Method getPipelineNullable = pipelineManager.getClass().getMethod("getPipelineNullable");

            Object pipeline = getPipelineNullable.invoke(pipelineManager);

            if (pipeline == null) {

                return 0;

            }

            Object renderTargets = null;

            if (irisPipelineClass.isInstance(pipeline)) {

                renderTargets = irisRenderTargetsField.get(pipeline);

            } else {

                renderTargets = tryGetRenderTargetsFromShaderPipeline(pipeline);

            }

            if (renderTargets == null) {

                return 0;

            }

            Object tex = irisGetDepthTexture.invoke(renderTargets);

            return tex instanceof Integer id ? id : 0;

        } catch (Throwable t) {

            return 0;

        }

    }



    private static Object tryGetRenderTargetsFromShaderPipeline(Object pipeline) {

        try {

            Class<?> shaderPipe = Class.forName("net.irisshaders.iris.pipeline.ShaderRenderingPipeline");

            if (!shaderPipe.isInstance(pipeline)) {

                return null;

            }

            for (Class<?> c = pipeline.getClass(); c != null; c = c.getSuperclass()) {

                try {

                    Field rt = c.getDeclaredField("renderTargets");

                    rt.setAccessible(true);

                    return rt.get(pipeline);

                } catch (NoSuchFieldException ignored) {

                }

            }

        } catch (Throwable ignored) {

        }

        return null;

    }



    private static void ensureIrisReflection() {

        if (irisReflectionTried) {

            return;

        }

        irisReflectionTried = true;

        try {

            irisPipelineClass = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");

            irisRenderTargetsField = irisPipelineClass.getDeclaredField("renderTargets");

            irisRenderTargetsField.setAccessible(true);

            Class<?> renderTargetsClass = Class.forName("net.irisshaders.iris.targets.RenderTargets");

            irisGetDepthTexture = renderTargetsClass.getMethod("getDepthTexture");

            irisReflectionOk = true;

            MainRegistry.LOGGER.info("[HBM-GpuCulling] Iris RenderTargets depth reflection enabled");

        } catch (Throwable t) {

            irisReflectionOk = false;

            MainRegistry.LOGGER.warn("[HBM-GpuCulling] Iris depth reflection unavailable; GPU occlusion uses vanilla depth only when shaders off");

        }

    }

}

