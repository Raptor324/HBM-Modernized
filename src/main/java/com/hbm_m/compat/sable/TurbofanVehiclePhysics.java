package com.hbm_m.compat.sable;

import net.minecraft.core.Direction;

/**
 * Sable-независимые константы и формулы тяги турбовентилятора для опциональной
 * интеграции с движущимися конструкциями. Общий код — работает на всех версиях,
 * потребитель (compat-mixin) подключается только при наличии мода в рантайме.
 */
public final class TurbofanVehiclePhysics {

    /** Штатное сгорание одного миллибакета авиатоплива (HE). */
    public static final double BASE_OUTPUT = 3_850.0D;
    /** Один штатный турбовентилятор сбалансирован против максимального малого пропеллера. */
    public static final double BASE_THRUST = 256.0D;
    public static final double BASE_AIRFLOW = 25.6D;

    private TurbofanVehiclePhysics() { }

    /** Выхлоп уходит в сторону, противоположную оси забора воздуха машины. */
    public static Direction exhaustDirection(Direction placementFacing) {
        return placementFacing.getClockWise().getOpposite();
    }

    /** Тяга в pN, привязанная к выходу в HE — форсаж остаётся ощутимым. */
    public static double thrust(int output) {
        return output > 0 ? BASE_THRUST * output / BASE_OUTPUT : 0.0D;
    }

    /** Поток в м/с; корневая кривая поднимает максимальную скорость без взрыва значений. */
    public static double airflow(int output) {
        return output > 0 ? BASE_AIRFLOW * Math.sqrt(output / BASE_OUTPUT) : 0.0D;
    }

    public static boolean isActive(boolean wasOn, int output, int consumption) {
        return wasOn && output > 0 && consumption > 0;
    }
}
