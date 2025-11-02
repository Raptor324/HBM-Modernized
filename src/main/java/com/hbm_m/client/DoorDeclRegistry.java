package com.hbm_m.client;

import com.hbm_m.block.entity.DoorDecl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class DoorDeclRegistry {
    private static final Map<String, DoorDecl> REGISTRY = new HashMap<>();
    
    public static void register(String id, DoorDecl decl) {
        REGISTRY.put(id, decl);
    }
    
    public static DoorDecl getById(String id) {
        return REGISTRY.getOrDefault(id, DoorDecl.LARGE_VEHICLE_DOOR);
    }
    
    // Вызывается из ClientSetup
    public static void init() {
        register("large_vehicle_door", DoorDecl.LARGE_VEHICLE_DOOR);
        register("round_airlock_door", DoorDecl.ROUND_AIRLOCK_DOOR);
        // Добавь другие двери здесь
    }
}
