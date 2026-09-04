package com.hbm_m.effect;

import com.hbm_m.block.bomb.BlockTaint;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.mob.EntityCreeperTainted;
import com.hbm_m.platform.ClientEffectHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

//? if forge {
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
 *///?}

/**
 * Эффект порчи — периодический урон и следы блока taint под сущностью.
 * Порт {@link com.hbm.potion.HbmPotion#taint} (1.7.10).
 *
 * <p>Вся версия-специфика — тонкие гейты, логика едина для 1.20.1/1.21.1;
 * иконки HUD/инвентаря рисует {@link ClientEffectHooks} (на обеих версиях).
 */
public class TaintEffect extends MobEffect {

    public TaintEffect() {
        super(MobEffectCategory.HARMFUL, 0x800080);
    }

    /** Единая логика тика — единственный источник поведения для обеих версий. */
    private void applyTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (level.isClientSide()) {
            return;
        }

        if (entity instanceof EntityCreeperTainted) {
            return;
        }

        if (level.random.nextInt(40) == 0) {
            entity.hurt(ModDamageSources.taint(level), amplifier + 1);
        }

        if (ModClothConfig.get().taintTrails) {
            BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 1.0, entity.getZ());
            if (below.getY() > level.getMinBuildHeight()) {
                BlockState ground = level.getBlockState(below);
                if (BlockTaint.canBeReplacedByTaint(level, below, ground)) {
                    level.setBlock(below, BlockTaint.stateWithAge(14), 2);
                }
            }
        }
    }

    //? if < 1.21.1 {
    @Override
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        applyTick(entity, amplifier);
    }
    //?} else {
    /*@Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        applyTick(entity, amplifier);
        return true;
    }
     *///?}

    /** Единая семантика интервала тика (1.7.10: через тик). */
    private boolean ticksThisTick(int duration) {
        return duration % 2 == 0;
    }

    //? if < 1.21.1 {
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return ticksThisTick(duration);
    }
    //?} else {
    /*// 1.21.1: isDurationEffectTick переименован в shouldApplyEffectTickThisTick.
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return ticksThisTick(duration);
    }
     *///?}

    // Клиентские иконки (HUD + инвентарь) — реализация в платформенном слое;
    // работает и на forge, и на neoforge (раньше на 1.21.1 иконок не было).
    @Override
    public void initializeClient(@NotNull Consumer<IClientMobEffectExtensions> consumer) {
        ClientEffectHooks.initializeClient(this, (Consumer<Object>) (Object) consumer);
    }
}
