package com.hbm_m.client.render.weapon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.render.NucleusJointSpecs;
import com.hbm_m.client.render.cache.RenderCacheManager;
import com.hbm_m.client.weapon.WeaponResources;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Central registry and manager for weapon kinematic rigs backed by {@link NucleusJointSpecs}.
 */
@OnlyIn(Dist.CLIENT)
public final class WeaponJointSpecs {

    private static final Map<ResourceLocation, WeaponJointRig> RIGS = new ConcurrentHashMap<>();

    static {
        RenderCacheManager.register(reason -> clearAll());
        initDefaultRigs();
    }

    private WeaponJointSpecs() { }

    public static void registerRig(ResourceLocation modelLocation, WeaponJointRig rig) {
        if (modelLocation != null && rig != null) {
            RIGS.put(modelLocation, rig);
        }
    }

    public static void registerRig(WeaponJointRig rig) {
        if (rig != null && rig.getModelLocation() != null) {
            RIGS.put(rig.getModelLocation(), rig);
        }
    }

    @Nullable
    public static WeaponJointRig getRig(ResourceLocation modelLocation) {
        if (modelLocation == null) return null;
        WeaponJointRig rig = RIGS.get(modelLocation);
        if (rig == null) {
            // Ensure default rigs are initialized
            initDefaultRigs();
            rig = RIGS.get(modelLocation);
        }
        return rig;
    }

    public static boolean applyPartTransform(ResourceLocation modelLocation, String partName, PoseStack pose, float value) {
        WeaponJointRig rig = getRig(modelLocation);
        if (rig == null) return false;
        WeaponJointRig.JointDef joint = rig.getJoint(partName);
        if (joint == null) return false;
        WeaponKinematics.applyJoint(pose, joint, value);
        return true;
    }

    public static void clearAll() {
        // Weapon rigs are immutable static kinematic definitions backed by NucleusJointSpecs.
        // Preserving RIGS ensures registration is strictly idempotent so the global 512-joint texture is never leaked or exhausted.
        if (RIGS.isEmpty()) {
            initDefaultRigs();
        }
    }

    /**
     * Initializes declarative kinematic rigs for all core animated firearms and handheld weapons.
     */
    public static synchronized void initDefaultRigs() {
        if (!RIGS.isEmpty()) return;

        // Minigun
        registerRig(new WeaponJointRig(WeaponResources.minigun)
                .registerRotate("Barrels", 0f, 0f, 1f, 0f, 0f, 0f)
                .setLeftPartName("GunDual")
                .registerRestParts("Barrels"));

        // Chainsaw
        registerRig(new WeaponJointRig(WeaponResources.chainsaw_loc)
                .registerRotate("Tooth", 1f, 0f, 0f, 0f, 0.375f, 0.5625f));

        // Maresleg
        registerRig(new WeaponJointRig(WeaponResources.maresleg)
                .registerRotate("Lever", 1f, 0f, 0f, 0f, 0.125f, -2.875f)
                .registerTranslate("Shell", 0f, 1f, 0f)
                .registerRotate("BodyLift", 1f, 0f, 0f, 0f, 0f, -4f)
                .registerRotate("BodyFlip", -1f, 0f, 0f, 0f, 0f, -2f));

        // Uzi
        registerRig(new WeaponJointRig(WeaponResources.uzi)
                .registerTranslateAxis("Slide", 0f, 0f, 1f)
                .registerTranslate("Magazine", 0f, 1f, 0f)
                .registerRotate("StockFront", 1f, 0f, 0f, 0f, 0.3125f, -5.75f)
                .registerRotate("StockBack", 1f, 0f, 0f, 0f, -0.3125f, -3f)
                .registerChain("StockChain",
                        new int[] { NucleusJointSpecs.KIND_ROTATE, NucleusJointSpecs.KIND_ROTATE },
                        new float[] { 1f, 0f, 0f, 1f, 0f, 0f },
                        new float[] { 0f, 0.3125f, -5.75f, 0f, -0.3125f, -3f })
                .setLeftPartName("GunMirror")
                .registerRestParts("StockBack", "StockFront", "Slide", "Magazine"));

        // Star F
        registerRig(new WeaponJointRig(WeaponResources.star_f)
                .registerTranslateAxis("Slide", 0f, 0f, 1f)
                .registerRotate("Hammer", 1f, 0f, 0f, 0f, 1.75f, -4.25f)
                .registerTranslate("Mag", 0f, 1f, 0f));

        // Boltgun
        registerRig(new WeaponJointRig(WeaponResources.boltgun_loc)
                .registerTranslateAxis("Barrel", 0f, 0f, -1f));

        // MAS-36
        registerRig(new WeaponJointRig(WeaponResources.mas36)
                .registerRotate("Stock", 1f, 0f, 0f, 0f, 0.3125f, -2.125f)
                .registerRotate("BoltTurn", 0f, 0f, 1f, 0f, 1.15625f, 0f)
                .registerTranslateAxis("BoltPull", 0f, 0f, 1f)
                .registerChain("Bolt",
                        new int[] { NucleusJointSpecs.KIND_ROTATE, NucleusJointSpecs.KIND_TRANSLATE_AXIS },
                        new float[] { 0f, 0f, 1f, 0f, 0f, 1f },
                        new float[] { 0f, 1.15625f, 0f, 0f, 0f, 0f }));

        // Heavy Revolver (Lil Mac)
        registerRig(new WeaponJointRig(WeaponResources.lilmac)
                .registerRotate("Pivot", 1f, 0f, 0f, 0f, 0f, 0f)
                .registerRotate("Cylinder", 1f, 0f, 0f, 0f, 1.75f, 0f)
                .registerRotate("Hammer", 0f, 0f, 1f, 4.0f, 1.25f, 0f)
                .registerTranslate("Bullets", 0f, 0f, 1f));

        // Crucible
        registerRig(new WeaponJointRig(WeaponResources.crucible_loc)
                .registerRotate("GuardLeft", -1f, 0f, 0f, 0f, 3f, 0.5f)
                .registerRotate("GuardRight", 1f, 0f, 0f, 0f, 3f, -0.5f));

        // Mining Drill
        registerRig(new WeaponJointRig(WeaponResources.drill)
                .registerTranslate("Piston1", 0f, 1f, 0f)
                .registerTranslate("Piston2", 0f, 1f, 0f)
                .registerTranslate("Piston3", 0f, 1f, 0f)
                .registerRotate("DrillBack", 0f, 0f, -1f, 0f, 0f, 0f)
                .registerRotate("DrillFront", 0f, 0f, 1f, 0f, 0f, 0f));

        // SPAS-12
        registerRig(new WeaponJointRig(WeaponResources.spas_12)
                .registerTranslateAxis("PumpGrip", 0f, 0f, 1f));

        // Henry
        registerRig(new WeaponJointRig(WeaponResources.henry)
                .registerRotate("Lever", 1f, 0f, 0f, 0f, 0.25f, -2.3125f)
                .registerRotate("Hammer", 1f, 0f, 0f, 0f, 0.625f, -3f)
                .registerRotate("Sight", 1f, 0f, 0f, 0f, 1.25f, -0.1875f)
                .registerRotate("Front", 0f, 0f, 1f, 0f, 1f, 0f));
    }
}
