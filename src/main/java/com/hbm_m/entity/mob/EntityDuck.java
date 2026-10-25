package com.hbm_m.entity.mob;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

/** 1:1 {@code EntityDuck}: ein Huhn mit Entenlauten, dessen Tod im Chat verkuendet wird. */
public class EntityDuck extends Chicken {

    public EntityDuck(EntityType<? extends Chicken> type, Level world) {
        super(type, world);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return HbmSoundsNT.get("hbm:entity.ducc");
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return HbmSoundsNT.get("hbm:entity.ducc");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HbmSoundsNT.get("hbm:entity.ducc");
    }

    @Override
    public Chicken getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob mate) {
        return ModEntities.DUCK.get().create(level);
    }

    @Override
    public void die(@NotNull DamageSource source) {
        if (!level().isClientSide && level().getServer() != null)
            level().getServer().getPlayerList().broadcastSystemMessage(this.getCombatTracker().getDeathMessage(), false);
        super.die(source);
    }
}
