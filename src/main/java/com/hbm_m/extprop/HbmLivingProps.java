package com.hbm_m.extprop;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.radiation.PlayerHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Данные живых сущностей (радиация). Порт {@link com.hbm.extprop.HbmLivingProps} (1.7.10), минимальный набор для радиации.
 */
public final class HbmLivingProps {

    public static final String KEY = "NTM_EXT_LIVING";
    private static final String NBT_RADIATION = "radiation";
    private static final String NBT_RAD_ENV = "radEnv";
    private static final String NBT_RAD_BUF = "radBuf";
    private static final String NBT_ASBESTOS = "asbestos";
    private static final String NBT_BLACK_LUNG = "blackLung";
    private static final String NBT_DIGAMMA = "digamma";

    private HbmLivingProps() {
    }

    private static CompoundTag livingTag(LivingEntity entity) {
        CompoundTag root = entity.getPersistentData();
        if (!root.contains(KEY)) {
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }

    public static float getRadiation(LivingEntity entity) {
        if (!ModClothConfig.get().enableRadiation) {
            return 0F;
        }
        if (entity instanceof Player player) {
            return PlayerHandler.getPlayerRads(player);
        }
        return livingTag(entity).getFloat(NBT_RADIATION);
    }

    public static void setRadiation(LivingEntity entity, float rad) {
        if (!ModClothConfig.get().enableRadiation) {
            return;
        }
        if (entity instanceof Player player) {
            PlayerHandler.setPlayerRads(player, rad);
            return;
        }
        livingTag(entity).putFloat(NBT_RADIATION, Math.max(0F, rad));
    }

    public static void incrementRadiation(LivingEntity entity, float rad) {
        if (!ModClothConfig.get().enableRadiation || rad == 0F) {
            return;
        }

        float radiation = getRadiation(entity) + rad;
        if (radiation > 2500F) {
            radiation = 2500F;
        }
        if (radiation < 0F) {
            radiation = 0F;
        }
        setRadiation(entity, radiation);
    }

    public static float getRadEnv(LivingEntity entity) {
        return livingTag(entity).getFloat(NBT_RAD_ENV);
    }

    public static void setRadEnv(LivingEntity entity, float rad) {
        livingTag(entity).putFloat(NBT_RAD_ENV, rad);
    }

    public static float getRadBuf(LivingEntity entity) {
        return livingTag(entity).getFloat(NBT_RAD_BUF);
    }

    public static void setRadBuf(LivingEntity entity, float rad) {
        livingTag(entity).putFloat(NBT_RAD_BUF, rad);
    }

    public static int getAsbestos(LivingEntity entity) {
        return livingTag(entity).getInt(NBT_ASBESTOS);
    }

    public static void incrementAsbestos(LivingEntity entity, int amount) {
        livingTag(entity).putInt(NBT_ASBESTOS, getAsbestos(entity) + amount);
    }

    public static int getBlackLung(LivingEntity entity) {
        return livingTag(entity).getInt(NBT_BLACK_LUNG);
    }

    public static void incrementBlackLung(LivingEntity entity, int amount) {
        livingTag(entity).putInt(NBT_BLACK_LUNG, getBlackLung(entity) + amount);
    }

    public static float getDigamma(LivingEntity entity) {
        return livingTag(entity).getFloat(NBT_DIGAMMA);
    }

    public static void incrementDigamma(LivingEntity entity, float amount) {
        livingTag(entity).putFloat(NBT_DIGAMMA, getDigamma(entity) + amount);
    }
}
