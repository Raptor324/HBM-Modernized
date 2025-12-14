package com.hbm_m.block.entity.custom.doors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DoorDeclRegistry {
    private static final Map<String, DoorDecl> REGISTRY = new HashMap<>();
    
    public static void register(String id, DoorDecl decl) {
        REGISTRY.put(id, decl);
    }

    public static Map<String, DoorDecl> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public static boolean contains(String id) {
        return REGISTRY.containsKey(id);
    }
    
    public static DoorDecl getById(String id) {
        return REGISTRY.getOrDefault(id, DoorDecl.LARGE_VEHICLE_DOOR);
    }
    
    // Вызывается из ClientSetup
    public static void init() {
        register("large_vehicle_door", DoorDecl.LARGE_VEHICLE_DOOR);
        register("round_airlock_door", DoorDecl.ROUND_AIRLOCK_DOOR);
        register("transition_seal", DoorDecl.TRANSITION_SEAL);
        register("fire_door", DoorDecl.FIRE_DOOR);
        register("sliding_blast_door", DoorDecl.SLIDE_DOOR);
        register("sliding_seal_door", DoorDecl.SLIDING_SEAL_DOOR);
        register("secure_access_door", DoorDecl.SECURE_ACCESS_DOOR);
        register("qe_sliding_door", DoorDecl.QE_SLIDING);
        register("qe_containment_door", DoorDecl.QE_CONTAINMENT);
        register("water_door", DoorDecl.WATER_DOOR);
        register("silo_hatch", DoorDecl.SILO_HATCH);
        register("silo_hatch_large", DoorDecl.SILO_HATCH_LARGE);
        // Добавь другие двери здесь
    }
}
