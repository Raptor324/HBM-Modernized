package com.hbm_m.client;

import com.hbm_m.main.MainRegistry;
import dev.architectury.platform.Platform;

/**
 * Detects OptiFine on the client and aborts startup with an explicit error message.
 */
public final class OptiFineDetector {
    private static final String[] OPTIFINE_CLASSES = {
            "net.optifine.Config",
            "optifine.OptiFineTransformationService",
            "optifine.OptiFineForgeTweaker",
            "net.optifine.shaders.Shaders"
    };

    private static final String[] OPTIFINE_MOD_IDS = {
            "optifine",
            "optiforge"
    };

    private static final String ERROR_MESSAGE =
            "HBM Modernized does not support OptiFine and never will. "
                    + "Please remove OptiFine and use Sodium/Embeddium instead, "
                    + "as well as Iris/Oculus for shaders.";

    private OptiFineDetector() {}

    /**
     * Checks whether OptiFine is present and aborts client startup if detected.
     */
    public static void abortIfOptiFinePresent() {
        if (!isOptiFinePresent()) {
            return;
        }
        MainRegistry.LOGGER.error(ERROR_MESSAGE);
        throw new IllegalStateException(ERROR_MESSAGE);
    }

    /**
     * Returns {@code true} if any OptiFine class or mod ID is detected in the runtime environment.
     */
    public static boolean isOptiFinePresent() {
        for (String modId : OPTIFINE_MOD_IDS) {
            if (Platform.isModLoaded(modId)) {
                return true;
            }
        }
        ClassLoader primaryLoader = OptiFineDetector.class.getClassLoader();
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        for (String className : OPTIFINE_CLASSES) {
            if (isClassPresent(className, primaryLoader)
                    || (contextLoader != null && contextLoader != primaryLoader && isClassPresent(className, contextLoader))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClassPresent(String className, ClassLoader loader) {
        try {
            Class.forName(className, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
