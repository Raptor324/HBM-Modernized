package com.hbm_m.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Veraenderlicher Vektor mit exakt der API und Rechenweise von {@code net.minecraft.util.Vec3} (1.7.10):
 * {@code xCoord/yCoord/zCoord} als Felder, {@code rotateAroundX/Y/Z} veraendern den Vektor selbst,
 * {@code normalize/addVector/subtract/crossProduct} liefern neue Vektoren. Fuer 1:1-Portierungen von Code, der
 * Vektoren schrittweise dreht und verschiebt (Waffen, Partikel).
 */
public class Vec3NT {

    public double xCoord;
    public double yCoord;
    public double zCoord;

    public Vec3NT(double x, double y, double z) {
        if (x == -0.0D) x = 0.0D;
        if (y == -0.0D) y = 0.0D;
        if (z == -0.0D) z = 0.0D;
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    public Vec3NT(Vec3 v) {
        this(v.x, v.y, v.z);
    }

    public static Vec3NT createVectorHelper(double x, double y, double z) {
        return new Vec3NT(x, y, z);
    }

    public Vec3 toVec3() {
        return new Vec3(xCoord, yCoord, zCoord);
    }

    public Vec3NT setComponents(double x, double y, double z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        return this;
    }

    public Vec3NT subtract(Vec3NT v) {
        return new Vec3NT(v.xCoord - this.xCoord, v.yCoord - this.yCoord, v.zCoord - this.zCoord);
    }

    public Vec3NT normalize() {
        double d0 = Math.sqrt(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
        return d0 < 1.0E-4D ? new Vec3NT(0.0D, 0.0D, 0.0D) : new Vec3NT(this.xCoord / d0, this.yCoord / d0, this.zCoord / d0);
    }

    public double dotProduct(Vec3NT v) {
        return this.xCoord * v.xCoord + this.yCoord * v.yCoord + this.zCoord * v.zCoord;
    }

    public Vec3NT crossProduct(Vec3NT v) {
        return new Vec3NT(this.yCoord * v.zCoord - this.zCoord * v.yCoord, this.zCoord * v.xCoord - this.xCoord * v.zCoord, this.xCoord * v.yCoord - this.yCoord * v.xCoord);
    }

    public Vec3NT addVector(double x, double y, double z) {
        return new Vec3NT(this.xCoord + x, this.yCoord + y, this.zCoord + z);
    }

    public double distanceTo(Vec3NT v) {
        double d0 = v.xCoord - this.xCoord;
        double d1 = v.yCoord - this.yCoord;
        double d2 = v.zCoord - this.zCoord;
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public double squareDistanceTo(Vec3NT v) {
        double d0 = v.xCoord - this.xCoord;
        double d1 = v.yCoord - this.yCoord;
        double d2 = v.zCoord - this.zCoord;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public double lengthVector() {
        return Math.sqrt(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
    }

    /** Dreht um die X-Achse (Bogenmass), veraendert diesen Vektor. */
    public void rotateAroundX(float angle) {
        float f1 = Mth.cos(angle);
        float f2 = Mth.sin(angle);
        double d0 = this.xCoord;
        double d1 = this.yCoord * f1 + this.zCoord * f2;
        double d2 = this.zCoord * f1 - this.yCoord * f2;
        this.xCoord = d0;
        this.yCoord = d1;
        this.zCoord = d2;
    }

    /** Dreht um die Y-Achse (Bogenmass), veraendert diesen Vektor. */
    public void rotateAroundY(float angle) {
        float f1 = Mth.cos(angle);
        float f2 = Mth.sin(angle);
        double d0 = this.xCoord * f1 + this.zCoord * f2;
        double d1 = this.yCoord;
        double d2 = this.zCoord * f1 - this.xCoord * f2;
        this.xCoord = d0;
        this.yCoord = d1;
        this.zCoord = d2;
    }

    /** Dreht um die Z-Achse (Bogenmass), veraendert diesen Vektor. */
    public void rotateAroundZ(float angle) {
        float f1 = Mth.cos(angle);
        float f2 = Mth.sin(angle);
        double d0 = this.xCoord * f1 + this.yCoord * f2;
        double d1 = this.yCoord * f1 - this.xCoord * f2;
        double d2 = this.zCoord;
        this.xCoord = d0;
        this.yCoord = d1;
        this.zCoord = d2;
    }

    // ---- Erweiterungen des Original-com.hbm.util.Vec3NT (1:1) ----

    public Vec3NT() { this(0, 0, 0); }
    public Vec3NT(net.minecraft.world.entity.Entity e) { this(e.getX(), e.getY(), e.getZ()); }

    public Vec3NT eyeHeight(net.minecraft.world.entity.Entity e) { return this.add(0, e.getEyeHeight(), 0); }
    public Vec3NT halfHeight(net.minecraft.world.entity.Entity e) { return this.add(0, e.getBbHeight() / 2D, 0); }

    public Vec3NT normalizeSelf() {
        double len = Math.sqrt(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
        if (len < 1.0E-4D) {
            return multiply(0D);
        } else {
            return multiply(1D / len);
        }
    }

    public Vec3NT add(double x, double y, double z) {
        this.xCoord += x;
        this.yCoord += y;
        this.zCoord += z;
        return this;
    }

    public Vec3NT add(Vec3NT vec) {
        this.xCoord += vec.xCoord;
        this.yCoord += vec.yCoord;
        this.zCoord += vec.zCoord;
        return this;
    }

    public Vec3NT add(Vec3 vec) {
        this.xCoord += vec.x;
        this.yCoord += vec.y;
        this.zCoord += vec.z;
        return this;
    }

    public Vec3NT multiply(double m) {
        this.xCoord *= m;
        this.yCoord *= m;
        this.zCoord *= m;
        return this;
    }

    public Vec3NT multiply(double x, double y, double z) {
        this.xCoord *= x;
        this.yCoord *= y;
        this.zCoord *= z;
        return this;
    }

    public double distanceTo(double x, double y, double z) {
        double dX = x - this.xCoord;
        double dY = y - this.yCoord;
        double dZ = z - this.zCoord;
        return Math.sqrt(dX * dX + dY * dY + dZ * dZ);
    }

    public Vec3NT rotateAroundXRad(double alpha) {
        double cos = Math.cos(alpha);
        double sin = Math.sin(alpha);
        double x = this.xCoord;
        double y = this.yCoord * cos + this.zCoord * sin;
        double z = this.zCoord * cos - this.yCoord * sin;
        return this.setComponents(x, y, z);
    }

    public Vec3NT rotateAroundYRad(double alpha) {
        double cos = Math.cos(alpha);
        double sin = Math.sin(alpha);
        double x = this.xCoord * cos + this.zCoord * sin;
        double y = this.yCoord;
        double z = this.zCoord * cos - this.xCoord * sin;
        return this.setComponents(x, y, z);
    }

    public Vec3NT rotateAroundZRad(double alpha) {
        double cos = Math.cos(alpha);
        double sin = Math.sin(alpha);
        double x = this.xCoord * cos + this.yCoord * sin;
        double y = this.yCoord * cos - this.xCoord * sin;
        double z = this.zCoord;
        return this.setComponents(x, y, z);
    }

    public Vec3NT rotateAroundXDeg(double alpha) { return this.rotateAroundXRad(alpha / 180D * Math.PI); }
    public Vec3NT rotateAroundYDeg(double alpha) { return this.rotateAroundYRad(alpha / 180D * Math.PI); }
    public Vec3NT rotateAroundZDeg(double alpha) { return this.rotateAroundZRad(alpha / 180D * Math.PI); }

    public static double getMinX(Vec3NT... vecs) { double min = Double.POSITIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.xCoord < min) min = vec.xCoord; return min; }
    public static double getMinY(Vec3NT... vecs) { double min = Double.POSITIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.yCoord < min) min = vec.yCoord; return min; }
    public static double getMinZ(Vec3NT... vecs) { double min = Double.POSITIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.zCoord < min) min = vec.zCoord; return min; }
    public static double getMaxX(Vec3NT... vecs) { double max = Double.NEGATIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.xCoord > max) max = vec.xCoord; return max; }
    public static double getMaxY(Vec3NT... vecs) { double max = Double.NEGATIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.yCoord > max) max = vec.yCoord; return max; }
    public static double getMaxZ(Vec3NT... vecs) { double max = Double.NEGATIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.zCoord > max) max = vec.zCoord; return max; }

    @Override
    public String toString() {
        return "(" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + ")";
    }
}
