package com.hbm_m.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityFBIDrone}: Quadcopter der Razzia (35 HP). Sucht Spieler in 100 Bloecken, fliegt 7-10 Bloecke ueber
 * ihnen und wirft alle 60 Ticks eine Granate, wenn das Ziel fast senkrecht (5 Bloecke) mehr als 3 Bloecke darunter ist.
 * Die Granate ist eine Baukastengranate {@code FRAG/HE/S7}.
 */
public class EntityFBIDrone extends EntityUFOBase {

    private int attackCooldown;

    public EntityFBIDrone(EntityType<? extends EntityFBIDrone> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 35.0D);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.isRemoved()) return;
        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;

        if (attackCooldown > 0) attackCooldown--;

        if (this.target != null && attackCooldown <= 0) {

            Vec3 vec = new Vec3(getX() - target.getX(), getY() - target.getY(), getZ() - target.getZ());
            if (Math.abs(vec.x) < 5 && Math.abs(vec.z) < 5 && vec.y > 3) {
                attackCooldown = 60;
                com.hbm_m.entity.grenade.EntityGrenadeUniversal grenade = new com.hbm_m.entity.grenade.EntityGrenadeUniversal(level(),
                        com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal.make(com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell.FRAG,
                                com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling.HE, com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze.S7));
                grenade.setThrower(this);
                grenade.setPosition(getX(), getY(), getZ());
                level().addFreshEntity(grenade);
            }
        }

        if (this.courseChangeCooldown > 0) {
            approachPosition(this.target == null ? 0.25D : 0.5D);
        }
    }

    @Override
    protected int getScanRange() {
        return 100;
    }

    @Override
    protected int targetHeightOffset() {
        return 7 + random.nextInt(4);
    }

    @Override
    protected int wanderHeightOffset() {
        return 7 + random.nextInt(4);
    }
}
