package com.hbm_m.particle.helper;

import java.util.HashMap;
import java.util.Map;

/** Registry of particle type string -> creator. Used by ParticleEffectClient. */
public final class ParticleCreators {

    private static final Map<String, IParticleCreator> CREATORS = new HashMap<>();

    static {
        CREATORS.put("nuke", new NukeTorexCreator());
        CREATORS.put("muke", new MukeParticleCreator());
        CREATORS.put("vomit", new VomitParticleCreator());
        CREATORS.put("skeleton", new SkeletonCreator());
        CREATORS.put("ashes", new AshesCreator());
        CREATORS.put("explosionLarge", new ExplosionCreator());
    }

    public static Map<String, IParticleCreator> particleCreators() {
        return CREATORS;
    }
}
