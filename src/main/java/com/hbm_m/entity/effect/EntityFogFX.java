package com.hbm_m.entity.effect;

import com.hbm_m.entity.ModEntities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.entity.particle.EntityFogFX} ("entity_nuclear_fog"): ruhender gelbgruener Nebel, lebt 400 Ticks
 * und wird vom {@code FogRenderer} als 25 Tafeln gezeichnet. Die Farb-/Groessenfelder des Originals (particleRed,
 * particleScale ...) setzt der Konstruktor dort zwar, gelesen werden sie aber nirgends - daher entfallen sie.
 * Die Bewegung wird wie im Original nur gedaempft, nie angewendet.
 */
public class EntityFogFX extends EntityModFX {

    public EntityFogFX(EntityType<? extends EntityFogFX> type, Level world) {
        super(type, world);
    }

    public EntityFogFX(Level world) {
        this(ModEntities.FOG_FX.get(), world);
    }

    public EntityFogFX(Level world, double x, double y, double z, double mx, double my, double mz) {
        this(world, x, y, z, mx, my, mz, 1.0F);
    }

    /** Der Faktor skaliert im Original nur das ungenutzte particleScale. */
    public EntityFogFX(Level world, double x, double y, double z, double mx, double my, double mz, float scale) {
        // EntityModFX(x, y, z, 0, 0, 0), danach *0.1 + uebergebene Bewegung (macht der Basiskonstruktor)
        super(ModEntities.FOG_FX.get(), world, x, y, z, mx, my, mz);
    }

    @Override
    public void tick() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        if (maxAge < 400) {
            maxAge = 400;
        }

        this.particleAge++;

        if (this.particleAge >= maxAge) {
            this.discard();
        }

        Vec3 m = getDeltaMovement().scale(0.9599999785423279D);

        if (this.onGround()) {
            m = new Vec3(m.x * 0.699999988079071D, m.y, m.z * 0.699999988079071D);
        }

        setDeltaMovement(m);
    }
}
