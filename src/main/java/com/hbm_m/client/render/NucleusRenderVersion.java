package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/**
 * Global generational counter for Nucleus engine instance records.
 * <p>
 * Instance records (InstPos/InstRot/light/fade) in instance buffers are valid
 * only within the "world" of the current generation. Bumping the generation
 * declares ALL records stale: machines tracked by {@code RenderDirtyTracker}
 * are fully rebuilt at the next collect (the roster assert reports a worldGen
 * miss).
 * <p>
 * Bump sites:
 * <ul>
 *   <li>anchor drift ({@link InstancedStaticPartRenderer#onRenderOriginChanged}) -
 *       records are anchor-relative, so shifting the anchor invalidates them all;</li>
 *   <li>Iris state change ({@code ClientRenderFlags.onFrameStart}) - Iris paths
 *       fill {@code instanceLightUV}, which the fast path does not write.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class NucleusRenderVersion {

    private static volatile long worldGen = 1L;

    private NucleusRenderVersion() {}

    public static long worldGen() {
        return worldGen;
    }

    public static void bump() {
        worldGen++;
    }
}
