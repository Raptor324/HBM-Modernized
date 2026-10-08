package com.hbm_m.handler;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.hbm_m.handler.guncfg.GunNPCFactory;
import com.hbm_m.handler.guncfg.LegacyBulletConfigFactory;

/** 1:1 {@code BulletConfigSyncingUtil}: Schluessel -&gt; Konfiguration; der Schluessel wird zum Client synchronisiert. */
@Deprecated
public final class BulletConfigSyncingUtil {

    private static final Map<Integer, BulletConfiguration> configSet = new HashMap<>();

    static int i = 0;

    public static final int TEST_CONFIG = i++;

    public static final int TURBINE = i++;

    public static final int MASKMAN_ORB = i++;
    public static final int MASKMAN_BOLT = i++;
    public static final int MASKMAN_ROCKET = i++;
    public static final int MASKMAN_TRACER = i++;
    public static final int MASKMAN_METEOR = i++;

    public static final int WORM_BOLT = i++;
    public static final int WORM_LASER = i++;

    public static final int UFO_ROCKET = i++;

    private static boolean loaded = false;

    /** Original beim Start; hier beim ersten Zugriff (beide Seiten). */
    public static synchronized void loadConfigsForSync() {
        if (loaded) return;
        loaded = true;
        configSet.put(TURBINE, LegacyBulletConfigFactory.getTurbineConfig());

        configSet.put(MASKMAN_ORB, GunNPCFactory.getMaskmanOrb());
        configSet.put(MASKMAN_BOLT, GunNPCFactory.getMaskmanBolt());
        configSet.put(MASKMAN_ROCKET, GunNPCFactory.getMaskmanRocket());
        configSet.put(MASKMAN_TRACER, GunNPCFactory.getMaskmanTracer());
        configSet.put(MASKMAN_METEOR, GunNPCFactory.getMaskmanMeteor());
        configSet.put(WORM_BOLT, GunNPCFactory.getWormBolt());
        configSet.put(WORM_LASER, GunNPCFactory.getWormHeadBolt());

        configSet.put(UFO_ROCKET, GunNPCFactory.getRocketUFOConfig());
    }

    @Nullable
    public static BulletConfiguration pullConfig(int key) {
        loadConfigsForSync();
        return configSet.get(key);
    }

    public static int getKey(BulletConfiguration config) {
        loadConfigsForSync();
        for (Map.Entry<Integer, BulletConfiguration> e : configSet.entrySet()) {
            if (e.getValue() == config) return e.getKey();
        }
        return -1;
    }
}
