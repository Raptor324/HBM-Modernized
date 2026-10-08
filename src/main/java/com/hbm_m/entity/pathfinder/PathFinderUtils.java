package com.hbm_m.entity.pathfinder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code PathFinderUtils}: Teilpfade fuer weite Ziele. Statt bis zum Ziel wird ein Punkt {@code maxDist} Bloecke
 * in dessen Richtung angesteuert, der vorher auf festen Boden gesetzt wird (zuerst bis zehn Bloecke abwaerts, sonst
 * zehn aufwaerts), damit der Pfad nicht im Leeren endet.
 */
public final class PathFinderUtils {

    private PathFinderUtils() { }

    @Nullable
    public static Path getPathEntityToEntityPartial(Level world, Mob fromEntity, Entity toEntity, float maxDist) {
        return partial(world, fromEntity, toEntity.getX() - fromEntity.getX(), toEntity.getY() - fromEntity.getY(), toEntity.getZ() - fromEntity.getZ(), maxDist);
    }

    @Nullable
    public static Path getPathEntityToCoordPartial(Level world, Mob fromEntity, int posX, int posY, int posZ, float maxDist) {
        return partial(world, fromEntity, posX - fromEntity.getX(), posY - fromEntity.getY(), posZ - fromEntity.getZ(), maxDist);
    }

    @Nullable
    private static Path partial(Level world, Mob fromEntity, double dx, double dy, double dz, float maxDist) {

        Vec3 vec = new Vec3(dx, dy, dz).normalize().scale(maxDist);

        int x = (int) Math.floor(fromEntity.getX() + vec.x);
        int y = (int) Math.floor(fromEntity.getY() + vec.y);
        int z = (int) Math.floor(fromEntity.getZ() + vec.z);

        //this part will adjust the end of the path so it's actually on the ground, it being unreachable causes mobs to slow down
        boolean solid = false;

        for (int i = y; i > y - 10; i--) {
            if (!world.getBlockState(new BlockPos(x, i, z)).blocksMotion() && world.getBlockState(new BlockPos(x, i - 1, z)).isRedstoneConductor(world, new BlockPos(x, i - 1, z))) {
                solid = true;
                y = i;
                break;
            }
        }

        if (!solid) for (int i = y + 10; i > y; i--) {
            if (!world.getBlockState(new BlockPos(x, i, z)).blocksMotion() && world.getBlockState(new BlockPos(x, i - 1, z)).isRedstoneConductor(world, new BlockPos(x, i - 1, z))) {
                y = i;
                break;
            }
        }

        return fromEntity.getNavigation().createPath(new BlockPos(x, y, z), 0);
    }
}
