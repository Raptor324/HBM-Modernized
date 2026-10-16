
package com.hbm_m.client.render.shader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import com.hbm_m.main.MainRegistry;
import dev.architectury.platform.Platform;

/**
 * Try-with-resources scope helper that calls
 * {@code WorldRenderingPipeline.setPhase(WorldRenderingPhase.<phase>)} on entry
 * and restores {@code WorldRenderingPhase.NONE} on close.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class IrisPhaseGuard implements AutoCloseable {

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    /** Slow-path Method handles, kept around for the rare case MH binding fails. */
    private static Method getPipelineManager;
    private static Method getPipelineNullable;
    private static Method setPhase;
    private static Method getPhase;

    /**
     * Hot-path MethodHandles. {@code asType()}-adapted to {@code (Object)Object}
     * / {@code (Object, Object)void} so call sites can invokeExact with boxed
     * arguments - see {@link #currentPipeline()} and {@link #pushBlockEntities()}
     * for the call shapes.
     */
    private static MethodHandle getPipelineManagerMH;
    private static MethodHandle getPipelineNullableMH;
    private static MethodHandle setPhaseMH;
    private static MethodHandle getPhaseMH;

    private static Class<?> phaseEnumClass;

    private static volatile Object phaseBlockEntities;
    private static volatile Object phaseNone;

    private static final IrisPhaseGuard NOOP = new IrisPhaseGuard(false, null);

    private final boolean active;
    private final Object previousPhase;

    private IrisPhaseGuard(boolean active, Object previousPhase) {
        this.active = active;
        this.previousPhase = previousPhase;
    }

    /**
     * Pushes {@code BLOCK_ENTITIES} phase. Returns a guard whose {@link #close()} restores
     * {@code NONE}. When Iris is missing or the reflective call fails the guard is a no-op.
     */
    public static IrisPhaseGuard pushBlockEntities() {
        if (!initialized) {
            initReflection();
        }
        if (!available) return NOOP;

        try {
            Object pipeline = currentPipeline();
            if (pipeline == null) return NOOP;
            if (phaseBlockEntities == null) return NOOP;
            Object prevPhase = invokeGetPhase(pipeline);
            if (prevPhase == phaseBlockEntities) {
                return NOOP;
            }

            invokeSetPhase(pipeline, phaseBlockEntities);
            return new IrisPhaseGuard(true, prevPhase);
        } catch (Throwable t) {
            return NOOP;
        }
    }

    @Override
    public void close() {
        if (!active) return;
        try {
            Object pipeline = currentPipeline();
            if (pipeline == null) return;
            if (previousPhase != null) {
                invokeSetPhase(pipeline, previousPhase);
            } else if (phaseNone != null) {
                invokeSetPhase(pipeline, phaseNone);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Hot-path setPhase invocation. Falls back to {@link Method#invoke} only when
     * MH binding could not complete during {@link #initReflection}.
     */
    private static void invokeSetPhase(Object pipeline, Object phase) throws Throwable {
        if (setPhaseMH != null) {
            setPhaseMH.invokeExact(pipeline, phase);
            return;
        }
        if (setPhase != null) {
            setPhase.invoke(pipeline, phase);
        }
    }

    private static Object invokeGetPhase(Object pipeline) throws Throwable {
        if (getPhaseMH != null) {
            return getPhaseMH.invokeExact(pipeline);
        }
        if (getPhase != null) {
            return getPhase.invoke(pipeline);
        }
        return null;
    }

    private static Object currentPipeline() throws Throwable {
        // Static method: Iris.getPipelineManager() takes no args and returns
        // PipelineManager. Adapted to ()Object via asType() in init.
        Object pipelineManager;
        if (getPipelineManagerMH != null) {
            pipelineManager = (Object) getPipelineManagerMH.invokeExact();
        } else {
            pipelineManager = getPipelineManager.invoke(null);
        }
        if (pipelineManager == null) return null;
        if (getPipelineNullableMH != null) {
            return (Object) getPipelineNullableMH.invokeExact(pipelineManager);
        }
        return getPipelineNullable.invoke(pipelineManager);
    }

    private static synchronized void initReflection() {
        if (initialized) return;
        initialized = true;

        if (!Platform.isModLoaded("iris") && !Platform.isModLoaded("oculus")) {
            return;
        }

        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            getPipelineManager = irisClass.getMethod("getPipelineManager");

            Class<?> pipelineManagerClass = Class.forName("net.irisshaders.iris.pipeline.PipelineManager");
            getPipelineNullable = pipelineManagerClass.getMethod("getPipelineNullable");

            Class<?> worldRenderingPipelineClass = Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPipeline");
            phaseEnumClass = Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPhase");
            setPhase = worldRenderingPipelineClass.getMethod("setPhase", phaseEnumClass);

            // Находим метод getPhase
            try {
                getPhase = worldRenderingPipelineClass.getMethod("getPhase");
            } catch (NoSuchMethodException e) {
                getPhase = null;
            }

            phaseBlockEntities = enumValue("BLOCK_ENTITIES");
            phaseNone = enumValue("NONE");
            available = phaseBlockEntities != null && phaseNone != null;

            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                getPipelineManager.setAccessible(true);
                getPipelineNullable.setAccessible(true);
                setPhase.setAccessible(true);
                getPipelineManagerMH = lookup.unreflect(getPipelineManager)
                        .asType(MethodType.methodType(Object.class));
                getPipelineNullableMH = lookup.unreflect(getPipelineNullable)
                        .asType(MethodType.methodType(Object.class, Object.class));
                setPhaseMH = lookup.unreflect(setPhase)
                        .asType(MethodType.methodType(void.class, Object.class, Object.class));

                if (getPhase != null) {
                    getPhase.setAccessible(true);
                    getPhaseMH = lookup.unreflect(getPhase)
                            .asType(MethodType.methodType(Object.class, Object.class));
                }
            } catch (Throwable mhFail) {
                MainRegistry.LOGGER.warn("IrisPhaseGuard: MethodHandle binding failed ({}), using Method.invoke", mhFail.toString());
                getPipelineManagerMH = null;
                getPipelineNullableMH = null;
                setPhaseMH = null;
                getPhaseMH = null;
            }

            if (available) {
                MainRegistry.LOGGER.info("IrisPhaseGuard: reflection cached successfully (MH={})", setPhaseMH != null);
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("IrisPhaseGuard: reflection unavailable ({}), no phase will be set", t.getMessage());
        }
    }

    private static Object enumValue(String name) {
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object v = Enum.valueOf((Class<Enum>) phaseEnumClass.asSubclass(Enum.class), name);
            return v;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
