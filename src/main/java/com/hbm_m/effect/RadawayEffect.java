package com.hbm_m.effect;

import com.hbm_m.platform.ClientEffectHooks;
import com.hbm_m.radiation.PlayerHandler;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

//? if forge {
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
 *///?}

/**
 * Антирадин (порт {@code com.hbm.potion.HbmPotion.radaway} 1.7.10): каждый тик
 * снижает накопленную дозу игрока на (amplifier + 1) × 140/120 RAD.
 *
 * <p>Вся версия-специфика (сигнатуры тика, клиентские иконки) — тонкие гейты,
 * логика едина для 1.20.1/1.21.1; иконки HUD/инвентаря рисует
 * {@link ClientEffectHooks} (на обеих версиях).
 */
public class RadawayEffect extends MobEffect {

    // Amplifier 0 → ~0.583 rad/tick, Amplifier 1 → ~1.167 rad/tick
    private static final float RADAWAY_POWER = 140.0F / 120.0F;

    public RadawayEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /** Единая логика тика — единственный источник поведения для обеих версий. */
    private void applyTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player && !entity.level().isClientSide()) {
            PlayerHandler.decrementPlayerRads(player, (amplifier + 1) * RADAWAY_POWER);
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

    //? if < 1.21.1 {
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
    //?} else {
    /*// 1.21.1: isDurationEffectTick переименован в shouldApplyEffectTickThisTick.
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
     *///?}

    // Клиентские иконки (HUD + инвентарь) — реализация в платформенном слое;
    // работает и на forge, и на neoforge (раньше на 1.21.1 иконок не было).
    @Override
    public void initializeClient(@NotNull Consumer<IClientMobEffectExtensions> consumer) {
        ClientEffectHooks.initializeClient(this, (Consumer<Object>) (Object) consumer);
    }
}
