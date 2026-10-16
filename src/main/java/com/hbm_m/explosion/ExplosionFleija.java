package com.hbm_m.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Постепенное сферическое стирание блоков (F.L.E.I.J.A. / анти-шрабидиевый взрыв).
 * Порт {@code com.hbm.explosion.ExplosionFleija} (1.7.10).
 */
public class ExplosionFleija {

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
    public float explosionCoefficient = 1.0F;
    public float explosionCoefficient2 = 1.0F;

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
        tag.putFloat(name + "explosionCoefficient", explosionCoefficient);
        tag.putFloat(name + "explosionCoefficient2", explosionCoefficient2);
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
        explosionCoefficient = tag.getFloat(name + "explosionCoefficient");
        explosionCoefficient2 = tag.getFloat(name + "explosionCoefficient2");
    }

    public ExplosionFleija(int x, int y, int z, Level level, int rad, float coefficient, float coefficient2) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.level = level;
        this.radius = rad;
        this.radius2 = this.radius * this.radius;
        this.explosionCoefficient = coefficient;
        this.explosionCoefficient2 = coefficient2;
        this.nlimit = this.radius2 * 4;
    }

    /** @return true когда взрыв завершён */
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

    protected void breakColumn(int x, int z) {
        int dist = this.radius2 - (x * x + z * z);
        if (dist > 0) {
            dist = (int) Math.sqrt(dist);
            for (int y = (int) (dist / this.explosionCoefficient2); y > -dist / this.explosionCoefficient; y--) {
                if (this.posY + y > level.getMinBuildHeight()) {
                    level.setBlock(new BlockPos(this.posX + x, this.posY + y, this.posZ + z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }
}
