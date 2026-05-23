package com.hbm_m.client.render.missile;

import java.util.List;
import java.util.Set;

import com.hbm_m.item.missile.MissileItem;
import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;

/**
 * OBJ paths and bake part names per missile hull (form factor).
 */
public enum MissileFormFactorModels {

    MICRO("models/missiles/missile_micro.obj", Set.of("Circle"), 1.0F, 1.0F),
    V2("models/missiles/missile_v2.obj", Set.of("Cylinder"), 1.0F, 1.0F),
    STEALTH("models/missiles/missile_stealth.obj", Set.of("Cylinder"), 1.0F, 1.0F),
    STRONG("models/missiles/missile_strong.obj", Set.of("Circle"), 1.0F, 1.5F),
    HUGE("models/missiles/missile_huge.obj", Set.of("Circle"), 1.0F, 1.0F),
    ATLAS("models/missiles/missile_atlas.obj", Set.of("Circle.002_Circle.003"), 1.0F, 1.0F),
    ABM("models/missiles/missile_abm.obj", Set.of("Circle"), 1.0F, 1.0F),
    SHUTTLE("models/missiles/missile_shuttle.obj",
            Set.of("Cube_Cube.001", "Cylinder", "Cylinder.001", "Cylinder.002", "Cylinder.003"),
            1.0F, 1.0F),
    ASSEMBLY("models/missiles/missile_assembly.obj", Set.of("Cube_Cube.001"), 1.0F, 1.0F),
    DOOMSDAY("models/missiles/missile_doomsday.obj", Set.of("Cylinder_Cylinder.002"), 1.0F, 1.0F),
    OTHER("models/missiles/missile_strong.obj", Set.of("Circle"), 1.0F, 1.0F);

    private final ResourceLocation objModel;
    private final Set<String> partNames;
    private final float padScale;
    private final float flightScaleMultiplier;

    MissileFormFactorModels(String objPath, Set<String> partNames, float padScale, float flightScaleMultiplier) {
        this.objModel = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, objPath);
        this.partNames = partNames;
        this.padScale = padScale;
        this.flightScaleMultiplier = flightScaleMultiplier;
    }

    public ResourceLocation getObjModel() {
        return objModel;
    }

    public Set<String> getPartNames() {
        return partNames;
    }

    public float getPadScale() {
        return padScale;
    }

    public float getFlightScale() {
        return padScale * flightScaleMultiplier;
    }

    public static MissileFormFactorModels fromItem(MissileItem item) {
        return fromFormFactor(item.formFactor);
    }

    public static MissileFormFactorModels fromFormFactor(MissileItem.MissileFormFactor formFactor) {
        return switch (formFactor) {
            case MICRO -> MICRO;
            case V2 -> V2;
            case STEALTH -> STEALTH;
            case STRONG -> STRONG;
            case HUGE -> HUGE;
            case ATLAS -> ATLAS;
            case ABM -> ABM;
            case OTHER -> SHUTTLE;
        };
    }

    public static MissileFormFactorModels fromEntity(Class<? extends com.hbm_m.entity.missile.MissileBaseEntity> entityClass) {
        if (com.hbm_m.entity.missile.MissileABMEntity.class.isAssignableFrom(entityClass)) {
            return ABM;
        }
        if (com.hbm_m.entity.missile.MissileTestEntity.class.isAssignableFrom(entityClass)) {
            return MICRO;
        }
        if (com.hbm_m.entity.missile.MissileTier0.class.isAssignableFrom(entityClass)) {
            return MICRO;
        }
        if (com.hbm_m.entity.missile.MissileTier1.class.isAssignableFrom(entityClass)) {
            return V2;
        }
        if (com.hbm_m.entity.missile.MissileStealthEntity.class.isAssignableFrom(entityClass)) {
            return STEALTH;
        }
        if (com.hbm_m.entity.missile.MissileTier2.class.isAssignableFrom(entityClass)) {
            return STRONG;
        }
        if (com.hbm_m.entity.missile.MissileShuttleEntity.class.isAssignableFrom(entityClass)) {
            return SHUTTLE;
        }
        if (com.hbm_m.entity.missile.MissileTier3.class.isAssignableFrom(entityClass)) {
            return HUGE;
        }
        if (com.hbm_m.entity.missile.MissileTier4.MissileDoomsday.class.isAssignableFrom(entityClass)
                || com.hbm_m.entity.missile.MissileTier4.MissileDoomsdayRusted.class.isAssignableFrom(entityClass)) {
            return DOOMSDAY;
        }
        if (com.hbm_m.entity.missile.MissileTier4.class.isAssignableFrom(entityClass)) {
            return ATLAS;
        }
        return MICRO;
    }

    public List<String> partNamesSorted() {
        return List.copyOf(partNames);
    }
}
