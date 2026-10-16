package com.hbm_m.client.render.culling;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/** Monotonic id incremented after each {@link InstancedRenderFrame#presentAfterBlockEntities}. */
//? if forge || neoforge {
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
