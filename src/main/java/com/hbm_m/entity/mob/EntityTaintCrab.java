package com.hbm_m.entity.mob;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.machines.TeslaBlockEntity;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.item.ModItems;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityTaintCrab}: entsteht, wenn eine Tesla-Krabbe Taint betritt (25 HP, keine Panik). Zappt im
 * Radius 10, verstrahlt alles Nicht-Krabbige im Umkreis 5 (Strahlung 16, 10 Ticks) und platzt mit Staerke 3.
 *
 * <p>Waffenrunde: der Fernkampf feuert im Original ein SEDNA-Geschoss {@code EntityBulletBaseMK4(r762_fmj, 10)} samt
 * Flammenpartikel und {@code weapon.sawShoot} - das SEDNA-Geschosssystem fehlt im Port noch.</p>
 */
public class EntityTaintCrab extends EntityCyberCrab {

    public List<Vec3> targets = new ArrayList<>();

    public EntityTaintCrab(EntityType<? extends EntityTaintCrab> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    protected RangedAttackGoal arrowAI() {
        return new RangedAttackGoal(this, 0.5D, 5, 5, 50.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCyberCrab.createAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5F);
    }

    @Override
    public void aiStep() {
        targets = TeslaBlockEntity.zap(level(), getX(), getY() + 1.25, getZ(), 10, this);

        List<LivingEntity> near = level().getEntitiesOfClass(LivingEntity.class, new AABB(getX() - 5, getY() - 5, getZ() - 5, getX() + 5, getY() + 5, getZ() + 5));

        for (LivingEntity e : near) {
            if (!(e instanceof EntityCyberCrab)) e.addEffect(new MobEffectInstance(ModEffects.RADIATION.get(), 10, 15));
        }

        super.aiStep();
    }

    /** Original {@code getDropItem} coil_copper, 0-2 (+ Pluenderung). */
    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        int j = this.random.nextInt(3);
        if (looting > 0) j += this.random.nextInt(looting + 1);
        for (int k = 0; k < j; ++k) this.spawnAtLocation(ModItems.COIL_COPPER.get(), 1);
        super.dropCustomDeathLoot(source, looting, recentlyHit);
    }

    @Override
    protected void dropRareDrop() {
        this.spawnAtLocation(ModItems.COIL_MAGNETIZED_TUNGSTEN.get(), 1);
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity entity, float f) {
        // Waffenrunde: EntityBulletBaseMK4(this, XFactory762mm.r762_fmj, 10F, ...) + Flammenpartikel + weapon.sawShoot (0.5)
    }
}
