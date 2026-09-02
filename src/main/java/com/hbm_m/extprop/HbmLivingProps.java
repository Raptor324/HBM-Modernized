package com.hbm_m.extprop;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.config.RadiationConfig;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.network.InfoToastPacket;
import com.hbm_m.radiation.PlayerHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Данные живых сущностей (радиация). Порт {@link com.hbm.extprop.HbmLivingProps} (1.7.10), минимальный набор для радиации.
 */
public final class HbmLivingProps {

    public static final String KEY = "NTM_EXT_LIVING";

    // 1.7.10: maxAsbestos = 60 мин, maxBlacklung = 120 мин (в тиках)
    public static final int maxAsbestos = 60 * 60 * 20;
    public static final int maxBlackLung = 2 * 60 * 60 * 20;

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
        if (RadiationConfig.disableAsbestos) {
            return 0;
        }
        return livingTag(entity).getInt(NBT_ASBESTOS);
    }

    public static void setAsbestos(LivingEntity entity, int amount) {
        if (RadiationConfig.disableAsbestos) {
            return;
        }
        livingTag(entity).putInt(NBT_ASBESTOS, Math.max(0, amount));
    }

    public static void incrementAsbestos(LivingEntity entity, int amount) {
        if (RadiationConfig.disableAsbestos || amount == 0) {
            return;
        }
        // Креатив/спектатор не накапливают болезнь (иначе тост «Мои лёгкие горят» в креативе висит вечно).
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        int value = Math.min(getAsbestos(entity) + amount, maxAsbestos);
        livingTag(entity).putInt(NBT_ASBESTOS, value);

        Level level = entity.level();
        if (value >= maxAsbestos) {
            // Смерть от асбестоза: 1000 урона в обход брони, счётчик сбрасывается.
            livingTag(entity).putInt(NBT_ASBESTOS, 0);
            entity.hurt(ModDamageSources.asbestos(level), 1000F);
        } else if (entity instanceof ServerPlayer player
                && level.getGameTime() % 10 == 0) { // троттлинг: оригинал шлёт каждый тик, 3000 = миллисекунды (3 с)
            InfoToastPacket.sendTo(player, "info.asbestos", 60, InfoToastPacket.ID_GAS_HAZARD, 0xFF5555);
        }
    }

    public static int getBlackLung(LivingEntity entity) {
        if (RadiationConfig.disableCoal) {
            return 0;
        }
        return livingTag(entity).getInt(NBT_BLACK_LUNG);
    }

    public static void setBlackLung(LivingEntity entity, int amount) {
        if (RadiationConfig.disableCoal) {
            return;
        }
        livingTag(entity).putInt(NBT_BLACK_LUNG, Math.max(0, amount));
    }

    public static void incrementBlackLung(LivingEntity entity, int amount) {
        if (RadiationConfig.disableCoal || amount == 0) {
            return;
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        int value = Math.min(getBlackLung(entity) + amount, maxBlackLung);
        livingTag(entity).putInt(NBT_BLACK_LUNG, value);

        Level level = entity.level();        if (value >= maxBlackLung) {
            // Смерть от угольной болезни: 1000 урона в обход брони, счётчик сбрасывается.
            livingTag(entity).putInt(NBT_BLACK_LUNG, 0);
            entity.hurt(ModDamageSources.blacklung(level), 1000F);
        } else if (entity instanceof ServerPlayer player
                && level.getGameTime() % 10 == 0) {
            InfoToastPacket.sendTo(player, "info.coaldust", 60, InfoToastPacket.ID_GAS_HAZARD, 0xFF5555);
        }
    }

    public static float getDigamma(LivingEntity entity) {
        return livingTag(entity).getFloat(NBT_DIGAMMA);
    }

    public static void incrementDigamma(LivingEntity entity, float amount) {
        float total = getDigamma(entity) + amount;
        livingTag(entity).putFloat(NBT_DIGAMMA, total);

        // The original checks these three thresholds every time a player's digamma changes.
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            if (total > 0F)   com.hbm_m.advancement.ModAdvancements.grant(player, com.hbm_m.advancement.ModAdvancements.DIGAMMA_SEE);
            if (total >= 2F)  com.hbm_m.advancement.ModAdvancements.grant(player, com.hbm_m.advancement.ModAdvancements.DIGAMMA_FEEL);
            if (total >= 10F) com.hbm_m.advancement.ModAdvancements.grant(player, com.hbm_m.advancement.ModAdvancements.DIGAMMA_KNOW);
        }
    }
}
