package com.hbm_m.client.sound;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Клиентский циклический звук пролётающей баллистической ракеты.
 *
 * Модель звука дистанционная: единственный looping ogg, pitch не двигаем (никакого
 * «вытягивания» дорожки) — вместо этого гейн модулируется скоростью сближения/удаления:
 *
 * <ul>
 *   <li><b>Запаздывающая позиция (retarded position)</b>: источник ставится туда, откуда
 *       звук долетает до игрока сейчас (скорость звука {@link #SPEED_OF_SOUND}). Ракета,
 *       взлетевшая за 300+ блоков, слышна с натуральной задержкой с того направления.</li>
 *   <li><b>Радиальный гейн</b>: каждый тик считаем radial-скорость источника вдоль линии
 *       к слушателю. Сближение (отрицательная радиальная) → гейн растёт до полной
 *       громкости; боковой/висевший полёт (radial ≈ 0) → равномерный средний гейн;
 *       удаление (положительная) → гейн медленно спадает к половине и оттуда тает по
 *       расстоянию за счёт LINEAR-аттенюации с attenuation_distance: 512.</li>
 * </ul>
 *
 * Источник данных — snapshot-провайдер ({@link MissileSoundEngine}), подающий позу и
 * скорость ракеты независимо от загрузки ванильной сущности (сетевой трек).
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)
*///?}
public class MissileFlybySoundInstance extends AbstractTickableSoundInstance {

    /** Скорость звука в воздухе: 343 м/с, один тик = 1/20 с → блоков(метров) за тик. */
    private static final double SPEED_OF_SOUND = 343.0D / 20.0D;

    /** Макс. «мах» для клампа радиальных скоростей — защита знаменателя допплера. */
    private static final double MAX_RADIAL_MACH = 0.9D;

    /** Коэффициент допплера: 0 = нет эффекта, 1 = физически точный. Мягкий 0.25. */
    private static final double DOPPLER_FACTOR = 0.25D;
    /** Жёсткие пределы питча — только допплер-окно ±20%, никаких аппаратных пределов. */
    private static final float MIN_PITCH = 0.80F;
    private static final float MAX_PITCH = 1.20F;

    /** Максимальный скачок pitch за тик (slew-rate limiter). */
    private static final float PITCH_SLEW_PER_TICK = 0.03F;
    /** EMA-коэффициент сглаживания pitch и радиального гейна. */
    private static final float SMOOTHING = 0.15F;

    /** Тиков на fade-in при создании канала. */
    private static final int FADE_IN_TICKS = 10;
    /** Тиков на fade-out после потери источника. */
    private static final int FADE_OUT_TICKS = 15;

    /** Базовая громкость канала: компенсирует увеличенный attenuation_distance (768),
     *  иначе на средних дистанциях ракета стала бы слишком тихой. */
    private static final float BASE_VOLUME = 1.9F;

    /** Гейн при радиальной скорости 0 (вис/боковой полёт). Понижен, чтобы удаляющаяся
     *  ракета с первых секунд ощутимо «проваливалась» вдаль. */
    private static final float RADIAL_GAIN_IDLE = 0.5F;
    /** Гейн при удалении (ракета летит от игрока). Не слишком низкий — линейная аттенюация сама тушит. */
    private static final float RADIAL_GAIN_RECEDE = 0.45F;
    /** Радиальная скорость (блоков/тик ≈ 200 м/с), на которой гейн достигает полного. */
    private static final double RADIAL_FULL_SPEED = 10.0D;

    /** Амплитудная аэро-турбулентность (чем дальше, тем более заметна — турбулентная атмосфера). */
    private static final float FLUTTER_NEAR = 0.025F;     // на нулевом расстоянии — ~1 дБ
    private static final float FLUTTER_FAR  = 0.18F;      // на дистанции приглушения
    /** Расстояние, на котором флаттеродостигает максимума. */
    private static final float FLUTTER_RANGE = 256.0F;
    /** Питч-вариация «пульса» двигателя (вздохи). */
    private static final float THROTTLE_PITCH_SWING = 0.025F; // ±2.5%
    /** Период общей пульсации в тиках (≈ 3.7 сек). */
    private static final float THROTTLE_PERIOD_TICKS = 74.0F;

    // --- «гул» на предельной дальности + растяжение спада гейна ---
    /** С этой дистанции звук начинает «оседать» в низкий гул. */
    private static final float RUMBLE_START = 200.0F;
    /** К этой дистанции (== attenuation_distance в sounds.json) ракета звучит одним гулом. */
    private static final float RUMBLE_END = 700.0F;
    /** Минимальный множитель питча на RUMBLE_END (чуть ниже октавы — совсем глухой рёв). */
    private static final float RUMBLE_MIN_PITCH = 0.45F;
    /** Максимальная компенсация хвоста спада: на RUMBLE_END гейн ×2, что сглаживает
     *  линейную аттенюацию OpenAL в плавно тающий шлейф вместо резкого обрыва. */
    private static final float TAIL_COMPENSATION = 1.0F;

    // --- высотное эхо: когда ракета уже высоко, слышен «второй» приглушённый ропот ---
    /** Вертикальный перевес (м) над игроком, с которого эхо начинает проступать. */
    private static final double ECHO_HEIGHT_START = 80.0D;
    /** Перевес, на котором эхо достигает полной громкости. */
    private static final double ECHO_HEIGHT_FULL = 200.0D;
    /** Громкость эха относительно основного канала. */
    private static final float ECHO_LEVEL = 0.25F;
    /** Питч эха (рассеяние в атмосфере «съедает» верха → эхо ощутимо ниже). */
    private static final float ECHO_PITCH = 0.65F;
    /** Минимальная задержка эха, тиков (≈140 мс), нарастает с высотой. */
    private static final float ECHO_MIN_DELAY_TICKS = 2.8F;
    /** Максимальная задержка эха, тиков (≈350 мс). */
    private static final float ECHO_MAX_DELAY_TICKS = 7.0F;
    /** Высота полной задержки. */
    private static final float ECHO_DELAY_MAX_HEIGHT = 400.0F;
    /** Дополнительное отставание эха при рендер-позиции (физическая задержка пути сигнала,
     *  домноженная с запасом, чтобы эхо ощутимо «тащилось» за основным рёвом). */
    private static final double ECHO_EXTRA_RETARD = 1.35D;
    /** Fade жизни эхо-канала через обычный конверт: гаснем подольше прямого звука. */
    private static final int ECHO_FADE_OUT_TICKS = 40;

    // --- входные данные, обновляемые MissileSoundEngine каждый тик ---
    private @Nullable Vec3 sourcePos;
    private @Nullable Vec3 sourceVel;

    /** Внутренний конверт fade-in/fade-out — НЕ путаем со стартовым полем volume. */
    private float envelope = 0.0F;
    private boolean alive = false;
    private float smoothedPitch = 1.0F;
    /** Сглаженный радиальный гейн (приближение/удаление) — множитель на громкость. */
    private float smoothedRadialGain = RADIAL_GAIN_IDLE;

    /** Фаза и собственный счётчик для аэро-турбулентности (убираем визуальную линейность). */
    private final float flutterSeed;
    private int flutterTick = 0;

    /** Второй канал — «высотное эхо». Спавнится лениво, звучит только пока ракета высоко. */
    private @Nullable MissileFlybySoundInstance echo;

    /** Признак эхо-канала: урезанная обработка (без допплера/радиального гейна, гул сразу,
     *  большее отставание позиции, свой fade — и, главное, эхо не спавнит эхо). */
    private final boolean echoChannel;

    public MissileFlybySoundInstance(Vec3 initialPos, Vec3 initialVel) {
        this(initialPos, initialVel, false);
    }

    /** @param echo true — инстанс является эхо-каналом переданной «материнской» ракеты. */
    private MissileFlybySoundInstance(Vec3 initialPos, Vec3 initialVel, boolean echo) {
        this(initialPos, initialVel, echo, 0.0F);
    }

    private MissileFlybySoundInstance(Vec3 initialPos, Vec3 initialVel, boolean echo, float flutterSeedAdd) {
        // BLOCKS (а не AMBIENT): звук должен звучать независимо от пользовательского
        // ползунка «Ambient/Environment», как звуки машин из AssemblerSoundInstance.
        super(ModSounds.MISSILE_FLYBY.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.echoChannel = echo;
        this.sourcePos = initialPos;
        this.sourceVel = initialVel;
        // Сид турбулентности из стартовой позы — у каждой ракеты свой характер;
        // у эха сид сдвинут, чтобы его дрожание не коррелировало с прямым звуком.
        this.flutterSeed = (float) (initialPos.x * 12.9898 + initialPos.y * 78.233
                + initialPos.z * 37.719) + flutterSeedAdd;
        this.looping = true;
        this.delay = 0;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        this.x = initialPos.x;
        this.y = initialPos.y;
        this.z = initialPos.z;
        // ВАЖНО: для looping-звука SoundEngine при volume <= 0 вычисляет delay > 0 и
        // убирает инстанс вялый в очередь ожидания — tick() не вызывается никогда.
        // Поэтому стартовое volume НЕ может быть нулём: конверт накладывается отдельно.
        this.volume = 0.0001F;
        this.pitch = 1.0F;
        this.alive = initialPos != null;
        if (echo) {
            // Эхо не участвует в радиальной динамике — работает на фиксированном гейне,
            // а его гул мы форсируем через echoLevelTarget=0 (задаёт родитель).
            this.smoothedRadialGain = 1.0F;
        }
    }

    /**
     * Подача актуальной позы/скорости ракеты из движка.
     * {@code null} — источник пропал (взрыв/конец трека): плавный fade-out.
     */
    void updateSource(@Nullable Vec3 pos, @Nullable Vec3 vel) {
        this.sourcePos = pos;
        this.sourceVel = vel;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) {
            if (player == null) {
                this.stop();
            }
            return;
        }

        boolean alive = this.sourcePos != null;
        double rawPitch = this.smoothedPitch;
        float rawRadialGain = this.smoothedRadialGain;
        float rumblePitch = 1.0F;
        float tailBoost = 1.0F;
        double outX = this.x;
        double outY = this.y;
        double outZ = this.z;

        if (alive) {
            Vec3 emitterNow = this.sourcePos;
            Vec3 sourceV = this.sourceVel != null ? this.sourceVel : Vec3.ZERO;
            Vec3 listenerPos = player.getEyePosition();
            Vec3 listenerV = player.getDeltaMovement();

            // --- запаздывающая позиция: 2 итерации фиксированной точки ---
            double distNow = emitterNow.distanceTo(listenerPos);
            double delay1 = distNow / SPEED_OF_SOUND;
            Vec3 emitterDelayed = emitterNow.subtract(sourceV.scale(delay1));
            double distDelayed = emitterDelayed.distanceTo(listenerPos);
            double delay2 = distDelayed / SPEED_OF_SOUND;
            if (this.echoChannel) {
                // Эхо «тащится» за прямым звуком: физическое отставание с запасом
                // плюс растущая с высотой добавка (~140→350 мс).
                double heightAbove = Math.max(0.0D, emitterNow.y - listenerPos.y);
                float extraTicks = Mth.lerp(
                        Mth.clamp((float) (heightAbove / ECHO_DELAY_MAX_HEIGHT), 0.0F, 1.0F),
                        ECHO_MIN_DELAY_TICKS, ECHO_MAX_DELAY_TICKS);
                delay2 = delay2 * ECHO_EXTRA_RETARD + extraTicks;
            }
            emitterDelayed = emitterNow.subtract(sourceV.scale(delay2));
            double range = emitterDelayed.distanceTo(listenerPos);

            if (range > 1.0E-4D && !this.echoChannel) {
                Vec3 los = listenerPos.subtract(emitterDelayed).normalize();

                // === Допплер (мягкий): свист при сближении, низ при удалении ===
                // Полная формула (C − v_listener)/(C − v_source), но ослабленная dopp-фактором.
                double sourceRadial = clampMach(sourceV.dot(los));
                double listenerRadial = clampMach(listenerV.dot(los));
                double full = (SPEED_OF_SOUND - listenerRadial) / (SPEED_OF_SOUND - sourceRadial);
                rawPitch = 1.0D + (full - 1.0D) * DOPPLER_FACTOR;

                // === Радиальный гейн (дельта дистанции → громкость) ===
                // Скорость сближения вдоль линии: положительная при сближении.
                double approachSpeed = -(sourceV.dot(los));
                if (approachSpeed > 0.0D) {
                    rawRadialGain = RADIAL_GAIN_IDLE
                            + (1.0F - RADIAL_GAIN_IDLE)
                            * Math.min(1.0F, (float) (approachSpeed / RADIAL_FULL_SPEED));
                } else {
                    rawRadialGain = Math.max(RADIAL_GAIN_RECEDE,
                            RADIAL_GAIN_IDLE + RADIAL_GAIN_IDLE
                                    * Math.max(-1.0F, (float) (approachSpeed / RADIAL_FULL_SPEED)));
                }
            }

            // === Край аттенюации: питч оседает в «гул», гейн растягивает спад ===
            // Питч просчитываем от «реального» расстояния (источник → ухо),
            // а не от допплер-сдвинутого, чтобы хвост не зависел от скорости трека.
            float rumbleT = (float) Mth.clamp((range - RUMBLE_START) / (RUMBLE_END - RUMBLE_START), 0.0D, 1.0D);
            rumbleT = rumbleT * rumbleT * (3.0F - 2.0F * rumbleT); // smoothstep
            rumblePitch = Mth.lerp(rumbleT, 1.0F, RUMBLE_MIN_PITCH);
            tailBoost = 1.0F + TAIL_COMPENSATION * rumbleT;

            outX = emitterDelayed.x;
            outY = emitterDelayed.y;
            outZ = emitterDelayed.z;
        }
        this.alive = alive;
        this.flutterTick++;

        // --- сглаживание: envelope → volume, EMA+slew → pitch ---
        float fadeStep = 1.0F / (alive ? FADE_IN_TICKS : FADE_OUT_TICKS);
        this.envelope = approach(this.envelope, alive ? 1.0F : 0.0F, fadeStep);
        float emaPitch = Mth.lerp(SMOOTHING, this.smoothedPitch,
                (float) Mth.clamp(rawPitch, MIN_PITCH, MAX_PITCH));

        // Аэро-турбулентность: амплитудный flutter растёт с расстоянием
        // (через текущее расстояние до запаздывающей позиции), питч-пульс — псевдо-
        // троттл-двигатель медленно «вздыхает», делая звук живым.
        double dist = Math.sqrt(
                (outX - player.getX()) * (outX - player.getX()) +
                (outY - player.getY()) * (outY - player.getY()) +
                (outZ - player.getZ()) * (outZ - player.getZ()));
        float distFactor = Mth.clamp((float) (dist / FLUTTER_RANGE), 0.0F, 1.0F);
        float flutterAmp = Mth.lerp(distFactor, FLUTTER_NEAR, FLUTTER_FAR);
        float t = this.flutterTick + this.flutterSeed;
        float flutter = (float) (
                Math.sin(t * 0.137F) * 0.5F +
                Math.sin(t * 0.431F + 1.7F) * 0.3F +
                Math.sin(t * 0.711F + 4.2F) * 0.2F);
        float throttleWobble = (float) Math.sin(t * Mth.TWO_PI / THROTTLE_PERIOD_TICKS);

        // «Гул» множит допплер-окно: на дальних дистанциях ракета звучит низким рёвом.
        float pitchWithPulse = emaPitch * rumblePitch + throttleWobble * THROTTLE_PITCH_SWING;
        this.smoothedPitch = approach(this.smoothedPitch, pitchWithPulse, PITCH_SLEW_PER_TICK);
        this.smoothedRadialGain = Mth.lerp(SMOOTHING, this.smoothedRadialGain, rawRadialGain);

        float flutteredGain = this.smoothedRadialGain * (1.0F + flutter * flutterAmp) * tailBoost;

        // Крошечная ненулевая нижняя граница — страховка от отложенного старта канала.
        this.volume = Math.max(0.0001F, BASE_VOLUME * this.envelope * Mth.clamp(flutteredGain, 0.0F, 1.0F));
        // Нижний предел питча опускаем до «гулового» окна (0.4), верх — как был.
        this.pitch = Mth.clamp(this.smoothedPitch, 0.4F, MAX_PITCH);
        this.x = outX;
        this.y = outY;
        this.z = outZ;

        if (!alive && this.envelope <= 0.0F) {
            this.stop();
        }
    }

    private static double clampMach(double radialVelocity) {
        double limit = SPEED_OF_SOUND * MAX_RADIAL_MACH;
        return Mth.clamp(radialVelocity, -limit, limit);
    }

    /** Локальный slew-rate limiter (Mth.approach появился позже целевой 1.20.1 — не полагаемся на него). */
    private static float approach(float current, float target, float maxDelta) {
        float delta = target - current;
        if (Math.abs(delta) <= maxDelta) {
            return target;
        }
        return current + Math.copySign(maxDelta, delta);
    }

}
