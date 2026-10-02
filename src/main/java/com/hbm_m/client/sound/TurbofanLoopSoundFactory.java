package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/**
 * Клиентский цикл звука турбовентилятора (оригинал: TURBOFAN_LOOP, AudioWrapper):
 * volume = momentum/50, pitch = momentum/200 + 0.5 + afterburner*0.16.
 * Вынесен в client.sound, чтобы серверный класс BE не ссылался на клиентские типы.
 * <p>
 * Инстанс НЕ самоостанавливается по дистанции: ваниль сама глушит звук за радиусом
 * линейной аттенюации, а стоп-по-дистанции превращался в цикл create/stop
 * (ClientSoundManager тут же пересоздаёт инстанс) и выжигал пул OpenAL
 * ("Maximum sound pool size reached") - особенно на кораблях Sable, где плоские
 * plot-координаты дают гигантскую дистанцию. Жизненный цикл: BE удалён или
 * momentum = 0 -> stop; пересоздание - через ClientSoundManager по ключу позиции.
 */
@OnlyIn(Dist.CLIENT)
public final class TurbofanLoopSoundFactory {

    private TurbofanLoopSoundFactory() {}

    public static Object create(MachineTurbofanBlockEntity turbofan, SoundEvent sound) {
        BlockPos pos = turbofan.getBlockPos();
        return new AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.looping = true;
                this.delay = 0;
                this.attenuation = Attenuation.LINEAR;
                updatePosition();
                updateMix();
            }

            /**
             * Позиция звука - МИРОВАЯ: BE на корабле сидит в plot-чанках за 20 млн блоков, и
             * ваниль глушит звук в plot-координатах (tickable sound с нулевой громкостью
             * останавливается, менеджер пересоздаёт - churn, выжигающий пул OpenAL).
             */
            private void updatePosition() {
                Level level = Minecraft.getInstance().level;
                if (level == null) return;
                net.minecraft.world.phys.Vec3 world = com.hbm_m.compat.sable.SableCompat.toWorld(
                        level, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D);
                this.x = world.x;
                this.y = world.y;
                this.z = world.z;
            }

            private void updateMix() {
                // Каждый уровень форсажа (клэмп 3, как сам форсаж) немного поднимает громкость -
                // вместе с attenuation 64 это расширяет фактическую зону слышимости.
                float afterburnerBoost = 1.0F + 0.15F * Math.min(turbofan.getAfterburner(), 3);
                this.volume = turbofan.getMomentum() / 50.0F * afterburnerBoost;
                this.pitch = turbofan.getMomentum() / 200.0F + 0.5F + turbofan.getAfterburner() * 0.16F;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) {
                    this.stop();
                    return;
                }

                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof MachineTurbofanBlockEntity live)) {
                    this.stop();
                    return;
                }
                if (live.getMomentum() <= 0) {
                    this.stop();
                    return;
                }

                updatePosition();
                updateMix();
            }
        };
    }

    /** Перегрузка без заглушки: volume = momentum/50. */
    public static Object create(MachineTurbofanBlockEntity turbofan) {
        return create(turbofan, com.hbm_m.sound.ModSounds.TURBOFAN_OPERATE.get());
    }
}
