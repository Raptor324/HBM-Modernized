package com.hbm_m.client.render.culling;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Monotonic id for one client render frame. {@code AFTER_BLOCK_ENTITIES} may run many
 * times per game tick; instanced present runs once per frame in {@link InstancedRenderFrame#present}.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class MdiRenderFrameGate {

    private static long frameSerial = 0L;

    private MdiRenderFrameGate() {}

    public static long currentSerial() {
        return frameSerial;
    }

    /** Call once per render frame after {@link InstancedRenderFrame#present}. */
    public static void advanceAfterPresent() {
        frameSerial++;
    }

    public static void reset() {
        frameSerial = 0L;
    }
}
