package com.hbm_m.sound;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * World-space position of a machine's loop sound. A block entity on a Sable
 * (Create Aeronautics) ship lives in far-away plot-grid chunks of the same
 * level, so a sound anchored at the raw {@link BlockPos} is muted by vanilla
 * linear attenuation - and the ship MOVES, so the anchor has to be
 * re-projected every tick, not just at creation (see
 * {@code TurbofanLoopSoundFactory.updatePosition}).
 */
@OnlyIn(Dist.CLIENT)
public final class SableSoundPosition {

    private SableSoundPosition() {}

    /** Block center (+yOffset) projected into world space; identity without Sable/sub-levels. */
    public static Vec3 project(BlockPos pos, double yOffset) {
        Minecraft mc = Minecraft.getInstance();
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + yOffset;
        double z = pos.getZ() + 0.5D;
        if (mc.level == null) {
            return new Vec3(x, y, z);
        }
        return com.hbm_m.compat.sable.SableCompat.toWorld(mc.level, x, y, z);
    }
}
