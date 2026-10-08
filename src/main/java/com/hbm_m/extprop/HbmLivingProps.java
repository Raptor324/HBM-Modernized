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
        if (!ModClothConfig.get().enableRadiation || !RadiationConfig.enableContamination) {
            return 0F;
        }
        if (entity instanceof Player player) {
            return PlayerHandler.getPlayerRads(player);
        }
        return livingTag(entity).getFloat(NBT_RADIATION);
    }

    public static void setRadiation(LivingEntity entity, float rad) {
        if (!ModClothConfig.get().enableRadiation || !RadiationConfig.enableContamination) {
            return;
        }
        if (entity instanceof Player player) {
            PlayerHandler.setPlayerRads(player, rad);
            return;
        }
        livingTag(entity).putFloat(NBT_RADIATION, Math.max(0F, rad));
    }

    public static void incrementRadiation(LivingEntity entity, float rad) {
        if (!ModClothConfig.get().enableRadiation || !RadiationConfig.enableContamination || rad == 0F) {
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


    // ------------------------------------------------------------------ Restport: fehlende Felder

    private static final String NBT_BOMB = "hfr_bomb";
    private static final String NBT_CONTAGION = "hfr_contagion";
    private static final String NBT_OIL = "hfr_oil";
    private static final String NBT_FIRE = "hfr_fire";
    private static final String NBT_PHOSPHORUS = "hfr_phosphorus";
    private static final String NBT_BALEFIRE = "hfr_balefire";
    private static final String NBT_BLACKFIRE = "hfr_blackfire";
    private static final String NBT_CONT = "hfr_cont";

    /** Original {@code bombTimer} (Zeitbombe, z.B. {@code ItemModAuto}/Pflock). */
    public static int getTimer(LivingEntity entity) { return livingTag(entity).getInt(NBT_BOMB); }
    public static void setTimer(LivingEntity entity, int bombTimer) { livingTag(entity).putInt(NBT_BOMB, bombTimer); }

    /** Original {@code contagion} (MKU); nur aktiv, wenn MKU erlaubt ist. */
    public static int getContagion(LivingEntity entity) {
        if (!ModClothConfig.get().enableMKU) return 0;
        return livingTag(entity).getInt(NBT_CONTAGION);
    }
    public static void setContagion(LivingEntity entity, int contagion) { livingTag(entity).putInt(NBT_CONTAGION, contagion); }

    public static int getOil(LivingEntity entity) { return livingTag(entity).getInt(NBT_OIL); }
    public static void setOil(LivingEntity entity, int oil) { livingTag(entity).putInt(NBT_OIL, oil); }

    /** Brennzustaende des Originals ({@code fire}, {@code phosphorus}, {@code balefire}, {@code blackFire}). */
    public static int getFire(LivingEntity entity) { return livingTag(entity).getInt(NBT_FIRE); }
    public static void setFire(LivingEntity entity, int v) { livingTag(entity).putInt(NBT_FIRE, v); }
    public static int getPhosphorus(LivingEntity entity) { return livingTag(entity).getInt(NBT_PHOSPHORUS); }
    public static void setPhosphorus(LivingEntity entity, int v) { livingTag(entity).putInt(NBT_PHOSPHORUS, v); }
    public static int getBalefire(LivingEntity entity) { return livingTag(entity).getInt(NBT_BALEFIRE); }
    public static void setBalefire(LivingEntity entity, int v) { livingTag(entity).putInt(NBT_BALEFIRE, v); }
    public static int getBlackFire(LivingEntity entity) { return livingTag(entity).getInt(NBT_BLACKFIRE); }
    public static void setBlackFire(LivingEntity entity, int v) { livingTag(entity).putInt(NBT_BLACKFIRE, v); }

    /** 1:1 {@code ContaminationEffect}: linear abklingende Strahlungsquelle am Koerper. */
    public static class ContaminationEffect {
        public float maxRad;
        public int maxTime;
        public int time;
        public boolean ignoreArmor;

        public ContaminationEffect(float rad, int time, boolean ignoreArmor) {
            this.maxRad = rad;
            this.maxTime = this.time = time;
            this.ignoreArmor = ignoreArmor;
        }

        public float getRad() {
            return maxRad * ((float) time / (float) maxTime);
        }

        CompoundTag save() {
            CompoundTag me = new CompoundTag();
            me.putFloat("maxRad", this.maxRad);
            me.putInt("maxTime", this.maxTime);
            me.putInt("time", this.time);
            me.putBoolean("ignoreArmor", ignoreArmor);
            return me;
        }

        static ContaminationEffect load(CompoundTag me) {
            ContaminationEffect effect = new ContaminationEffect(me.getFloat("maxRad"), me.getInt("maxTime"), me.getBoolean("ignoreArmor"));
            effect.time = me.getInt("time");
            return effect;
        }
    }

    /** Liefert eine Kopie der Liste; Aenderungen ueber {@link #setCont} zurueckschreiben. */
    public static java.util.List<ContaminationEffect> getCont(LivingEntity entity) {
        java.util.List<ContaminationEffect> list = new java.util.ArrayList<>();
        net.minecraft.nbt.ListTag tag = livingTag(entity).getList(NBT_CONT, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < tag.size(); i++) list.add(ContaminationEffect.load(tag.getCompound(i)));
        return list;
    }

    public static void setCont(LivingEntity entity, java.util.List<ContaminationEffect> list) {
        net.minecraft.nbt.ListTag tag = new net.minecraft.nbt.ListTag();
        for (ContaminationEffect e : list) tag.add(e.save());
        livingTag(entity).put(NBT_CONT, tag);
    }

    public static void addCont(LivingEntity entity, ContaminationEffect cont) {
        java.util.List<ContaminationEffect> list = getCont(entity);
        list.add(cont);
        setCont(entity, list);
    }

    public static final java.util.UUID DIGAMMA_UUID = java.util.UUID.fromString("2a3d8aec-5ab9-4218-9b8b-ca812bdf378b");

    /**
     * 1:1-Port von {@code setDigamma}: speichert den Wert, halbiert die Maximalgesundheit pro Punkt
     * ({@code 0.5^digamma - 1} als MULTIPLY_TOTAL-Modifikator), und ab 10 Punkten bzw. Maximalgesundheit
     * 0 stirbt das Wesen mit Seelensand-Schweiss. Enten (Quackos) sind immun.
     */
    public static void setDigamma(LivingEntity entity, float digamma) {
        if (entity.level().isClientSide) return;
        if (isDuck(entity)) digamma = 0.0F;

        livingTag(entity).putFloat(NBT_DIGAMMA, digamma);

        float healthMod = (float) Math.pow(0.5, digamma) - 1F;
        var attr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(DIGAMMA_UUID);
            attr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(DIGAMMA_UUID, "digamma", healthMod,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        if (entity.getHealth() > entity.getMaxHealth() && entity.getMaxHealth() > 0) {
            entity.setHealth(entity.getMaxHealth());
        }

        if ((entity.getMaxHealth() <= 0 || digamma >= 10.0F) && entity.isAlive()) {
            entity.setAbsorptionAmount(0);
            var src = com.hbm_m.damagesource.ModDamageSources.create(entity.level(), com.hbm_m.damagesource.ModDamageTypes.DIGAMMA);
            entity.hurt(src, 500F);
            entity.setHealth(0);
            entity.die(src);

            if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
                data.putString("type", "sweat");
                data.putInt("count", 50);
                data.putInt("block", net.minecraft.world.level.block.Block.getId(net.minecraft.world.level.block.Blocks.SOUL_SAND.defaultBlockState()));
                data.putInt("entity", entity.getId());
                com.hbm_m.particle.helper.IParticleCreator.sendPacket(sl, entity.getX(), entity.getY(), entity.getZ(), 50, data);
            }
        }

        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            float di = getDigamma(entity);
            if (di > 0F)   com.hbm_m.advancement.ModAdvancements.grant(player, com.hbm_m.advancement.ModAdvancements.DIGAMMA_SEE);
            if (di >= 2F)  com.hbm_m.advancement.ModAdvancements.grant(player, com.hbm_m.advancement.ModAdvancements.DIGAMMA_FEEL);
            if (di >= 10F) com.hbm_m.advancement.ModAdvancements.grant(player, com.hbm_m.advancement.ModAdvancements.DIGAMMA_KNOW);
        }
    }

    /** 1:1-Port von {@code incrementDigamma}: auf 0..10 begrenzt, dann {@link #setDigamma}. */
    public static void incrementDigamma(LivingEntity entity, float amount) {
        if (isDuck(entity)) amount = 0.0F;
        float dRad = getDigamma(entity) + amount;
        if (dRad > 10) dRad = 10;
        if (dRad < 0) dRad = 0;
        setDigamma(entity, dRad);
    }

    /** {@code EntityDuck} des Originals ({@code entity_fucc_a_ducc}); EntityQuackos erbt davon. */
    private static boolean isDuck(LivingEntity entity) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && (key.getPath().equals("entity_fucc_a_ducc") || key.getPath().equals("entity_elder_one"));
    }
}
