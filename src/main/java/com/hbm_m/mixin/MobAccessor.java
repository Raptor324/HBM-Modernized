package com.hbm_m.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Zugriff auf die KI-Ziele (Original: {@code EntityLiving.tasks}), z.B. fuer {@code ItemModDefuser}. */
@Mixin(Mob.class)
public interface MobAccessor {

    @Accessor("goalSelector")
    GoalSelector hbm_m$getGoalSelector();
}
