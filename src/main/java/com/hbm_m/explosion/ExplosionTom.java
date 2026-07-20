package com.hbm_m.explosion;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gerald/Horizons' authentic crater shape (port of legacy {@code com.hbm.explosion.ExplosionTom}):
 * unlike the plain sphere-clearing {@link ExplosionFleija}, this carves a proper crater bowl
 * with a raised "peak ring" rim made of {@code tektite} (the black glassy ring the meteor is
 * known for) and floods everything below the original terrain height with lava.
 */
public class ExplosionTom {

    public int posX;
    public int posY;
    public int posZ;
    public int lastposX = 0;
    public int lastposZ = 0;
    public int radius;
    public int radius2;
    public Level level;
    private int n = 1;
    private int nlimit;
    private int shell;
    private int leg;
    private int element;

    public ExplosionTom(int x, int y, int z, Level level, int rad) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.level = level;
        this.radius = rad;
        this.radius2 = this.radius * this.radius;
        this.nlimit = this.radius2 * 4;
    }

    public void saveToNbt(CompoundTag tag, String name) {
        tag.putInt(name + "posX", posX);
        tag.putInt(name + "posY", posY);
        tag.putInt(name + "posZ", posZ);
        tag.putInt(name + "lastposX", lastposX);
        tag.putInt(name + "lastposZ", lastposZ);
        tag.putInt(name + "radius", radius);
        tag.putInt(name + "radius2", radius2);
        tag.putInt(name + "n", n);
        tag.putInt(name + "nlimit", nlimit);
        tag.putInt(name + "shell", shell);
        tag.putInt(name + "leg", leg);
        tag.putInt(name + "element", element);
    }

    public void readFromNbt(CompoundTag tag, String name) {
        posX = tag.getInt(name + "posX");
        posY = tag.getInt(name + "posY");
        posZ = tag.getInt(name + "posZ");
        lastposX = tag.getInt(name + "lastposX");
        lastposZ = tag.getInt(name + "lastposZ");
        radius = tag.getInt(name + "radius");
        radius2 = tag.getInt(name + "radius2");
        n = tag.getInt(name + "n");
        nlimit = tag.getInt(name + "nlimit");
        shell = tag.getInt(name + "shell");
        leg = tag.getInt(name + "leg");
        element = tag.getInt(name + "element");
    }

    /** @return true when the explosion has finished. */
    public boolean update() {
        breakColumn(this.lastposX, this.lastposZ);
        this.shell = (int) Math.floor((Math.sqrt(n) + 1) / 2);
        int shell2 = this.shell * 2;
        if (shell2 == 0) {
            return true;
        }
        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / shell2);
        this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
        this.lastposX = this.leg == 0 ? this.shell : this.leg == 1 ? -this.element : this.leg == 2 ? -this.shell : this.element;
        this.lastposZ = this.leg == 0 ? this.element : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
        this.n++;
        return this.n > this.nlimit;
    }

    private static boolean isMeltable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.ICE) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
            return true;
        }
        if (state.getFluidState().is(FluidTags.WATER)) {
            return true;
        }
        return state.ignitedByLava();
    }

    private void breakColumn(int x, int z) {
        int dist = this.radius2 - (x * x + z * z);
        if (dist <= 0) {
            return;
        }

        int pX = posX + x;
        int pZ = posZ + z;
        double distance = Math.sqrt((double) x * x + (double) z * z);

        int y = level.getMaxBuildHeight();
        int terrain = 63;

        // Basic crater bowl shape, raised "peak ring" rim, then the outer crater rim.
        double cA = (terrain - Math.pow(Math.E, -Math.pow(distance, 2) / 40000) * 13) + level.random.nextInt(2);
        double cB = cA + Math.pow(Math.E, -Math.pow(distance - 200, 2) / 400) * 13;
        int craterFloor = (int) (cB + Math.pow(Math.E, -Math.pow(distance - 500, 2) / 2000) * 37);

        for (int i = level.getMaxBuildHeight(); i > level.getMinBuildHeight(); i--) {
            BlockPos check = new BlockPos(pX, i, pZ);
            if (i == craterFloor || !level.getBlockState(check).isAir()) {
                y = i;
                break;
            }
        }

        int height = terrain - 14;
        int offset = 20;
        int threshold = (int) ((float) distance * (float) (height + offset) / (float) this.radius)
                + level.random.nextInt(2) - offset;

        while (y > threshold) {
            if (y <= level.getMinBuildHeight() + 1) {
                break;
            }

            BlockPos pos = new BlockPos(pX, y, pZ);
            if (y <= craterFloor) {
                level.setBlock(pos, ModBlocks.TEKTITE.get().defaultBlockState(), 2);
            } else if (y > terrain + 1) {
                if (distance < 500) {
                    clearMeltableNeighbors(pX, y, pZ);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            } else {
                floodLavaNeighbors(pX, y, pZ);
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
            }
            y--;
        }
    }

    private void clearMeltableNeighbors(int x, int y, int z) {
        for (int i = -2; i < 3; i++) {
            for (int j = -2; j < 3; j++) {
                for (int k = -2; k < 3; k++) {
                    BlockPos pos = new BlockPos(x + i, y + j, z + k);
                    if (isMeltable(level, pos)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private void floodLavaNeighbors(int x, int y, int z) {
        for (int i = -2; i < 3; i++) {
            for (int k = -2; k < 3; k++) {
                BlockPos pos = new BlockPos(x + i, y, z + k);
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.ICE) || state.getFluidState().is(FluidTags.WATER) || state.isAir()) {
                    level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
                }
            }
        }
    }
}
