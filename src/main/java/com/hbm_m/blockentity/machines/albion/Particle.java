package com.hbm_m.blockentity.machines.albion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1-Port von {@code TileEntityPASource.Particle} (1.7.10): das Teilchen, das durch den Ring
 * wandert.
 *
 * <p>Es merkt sich Position und Richtung, seinen Impuls, wie stark der Strahl aufgefaechert ist
 * ({@link #defocus}) und wie weit es seit der letzten Ablenkung gekommen ist. Die beiden
 * Ausgangsstoffe reist es mit - der Detektor wertet sie am Ende gegen die Rezepte aus.</p>
 */
public class Particle {

    /** Original: {@code maxDefocus = 1000} - darueber zerfaellt der Strahl. */
    public static final int MAX_DEFOCUS = 1000;

    private final PAParticleHost source;

    public int x;
    public int y;
    public int z;
    public Direction dir;

    public int momentum;
    public int defocus;
    public int distanceTraveled;
    public boolean invalid = false;

    public final ItemStack input1;
    public final ItemStack input2;

    public Particle(PAParticleHost source, int x, int y, int z, Direction dir,
                    ItemStack input1, ItemStack input2) {
        this.source = source;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dir = dir;
        this.input1 = input1;
        this.input2 = input2;
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }

    /** Beendet den Lauf und meldet der Quelle, woran es lag. */
    public void crash(PAState state) {
        this.invalid = true;
        this.source.updateState(state);
    }

    public void move(BlockPos pos) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.source.setLastSpeed(this.momentum);
    }

    public void addDistance(int dist) {
        this.distanceTraveled += dist;
    }

    public void resetDistance() {
        this.distanceTraveled = 0;
    }

    /** Faechert den Strahl auf; ab {@link #MAX_DEFOCUS} ist er verloren. */
    public void defocus(int amount) {
        this.defocus += amount;
        if (this.defocus > MAX_DEFOCUS) crash(PAState.CRASH_DEFOCUS);
    }

    /** Buendelt den Strahl wieder - der Quadrupol tut das. */
    public void focus(int amount) {
        this.defocus -= amount;
        if (this.defocus < 0) this.defocus = 0;
    }
}
