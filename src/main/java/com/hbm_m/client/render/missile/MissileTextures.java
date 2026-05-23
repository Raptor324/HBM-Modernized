package com.hbm_m.client.render.missile;

import com.hbm_m.entity.missile.MissileABMEntity;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.entity.missile.MissileTestEntity;
import com.hbm_m.entity.missile.MissileShuttleEntity;
import com.hbm_m.entity.missile.MissileStealthEntity;
import com.hbm_m.entity.missile.MissileTier0;
import com.hbm_m.entity.missile.MissileTier1;
import com.hbm_m.entity.missile.MissileTier2;
import com.hbm_m.entity.missile.MissileTier3;
import com.hbm_m.entity.missile.MissileTier4;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Atlas sprite IDs under {@code block/missile/} (PNG in {@code textures/block/missile/}).
 */
public final class MissileTextures {

    private static final String TEX_ROOT = "block/missile/";

    /** Shared placeholder until per-variant PNGs are added. */
    public static final ResourceLocation PLACEHOLDER = rl("missile_micro");

    public static final ResourceLocation MISSILE_MICRO = rl("missile_micro");
    public static final ResourceLocation MISSILE_MICRO_TAINT = rl("missile_micro_taint");
    public static final ResourceLocation MISSILE_MICRO_BHOLE = rl("missile_micro_bhole");
    public static final ResourceLocation MISSILE_MICRO_SCHRAB = rl("missile_micro_schrab");
    public static final ResourceLocation MISSILE_MICRO_EMP = rl("missile_micro_emp");

    public static final ResourceLocation MISSILE_V2 = rl("missile_v2");
    public static final ResourceLocation MISSILE_V2_INC = rl("missile_v2_inc");
    public static final ResourceLocation MISSILE_V2_CL = rl("missile_v2_cl");
    public static final ResourceLocation MISSILE_V2_BU = rl("missile_v2_bu");
    public static final ResourceLocation MISSILE_V2_DECOY = rl("missile_v2_decoy");

    public static final ResourceLocation MISSILE_ABM = rl("missile_abm");
    public static final ResourceLocation MISSILE_STEALTH = rl("missile_stealth");

    public static final ResourceLocation MISSILE_STRONG = rl("missile_strong");
    public static final ResourceLocation MISSILE_STRONG_EMP = rl("missile_strong_emp");
    public static final ResourceLocation MISSILE_STRONG_INC = rl("missile_strong_inc");
    public static final ResourceLocation MISSILE_STRONG_CL = rl("missile_strong_cl");
    public static final ResourceLocation MISSILE_STRONG_BU = rl("missile_strong_bu");

    public static final ResourceLocation MISSILE_HUGE = rl("missile_huge");
    public static final ResourceLocation MISSILE_HUGE_INC = rl("missile_huge_inc");
    public static final ResourceLocation MISSILE_HUGE_CL = rl("missile_huge_cl");
    public static final ResourceLocation MISSILE_HUGE_BU = rl("missile_huge_bu");

    public static final ResourceLocation MISSILE_ATLAS_NUCLEAR = rl("missile_atlas_nuclear");
    public static final ResourceLocation MISSILE_ATLAS_THERMO = rl("missile_atlas_thermo");
    public static final ResourceLocation MISSILE_ATLAS_TECTONIC = rl("missile_atlas_tectonic");
    public static final ResourceLocation MISSILE_ATLAS_DOOMSDAY = rl("missile_atlas_doomsday");
    public static final ResourceLocation MISSILE_ATLAS_DOOMSDAY_WEATHERED = rl("missile_atlas_doomsday_weathered");
    public static final ResourceLocation MISSILE_SHUTTLE = rl("missile_shuttle");

    private MissileTextures() {}

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, TEX_ROOT + path);
    }

    public static ResourceLocation forItem(Item item) {
        if (item == ModItems.MISSILE_MICRO.get()) return MISSILE_MICRO;
        if (item == ModItems.MISSILE_TAINT.get()) return MISSILE_MICRO_TAINT;
        if (item == ModItems.MISSILE_BHOLE.get()) return MISSILE_MICRO_BHOLE;
        if (item == ModItems.MISSILE_SCHRABIDIUM.get()) return MISSILE_MICRO_SCHRAB;
        if (item == ModItems.MISSILE_EMP.get()) return MISSILE_MICRO_EMP;

        if (item == ModItems.MISSILE_GENERIC.get()) return MISSILE_V2;
        if (item == ModItems.MISSILE_INCENDIARY.get()) return MISSILE_V2_INC;
        if (item == ModItems.MISSILE_CLUSTER.get()) return MISSILE_V2_CL;
        if (item == ModItems.MISSILE_BUSTER.get()) return MISSILE_V2_BU;
        if (item == ModItems.MISSILE_DECOY.get()) return MISSILE_V2_DECOY;

        if (item == ModItems.MISSILE_ABM.get()) return MISSILE_ABM;
        if (item == ModItems.MISSILE_TEST.get()) return MISSILE_MICRO;
        if (item == ModItems.MISSILE_STEALTH.get()) return MISSILE_STEALTH;

        if (item == ModItems.MISSILE_STRONG.get()) return MISSILE_STRONG;
        if (item == ModItems.MISSILE_INCENDIARY_STRONG.get()) return MISSILE_STRONG_INC;
        if (item == ModItems.MISSILE_CLUSTER_STRONG.get()) return MISSILE_STRONG_CL;
        if (item == ModItems.MISSILE_BUSTER_STRONG.get()) return MISSILE_STRONG_BU;
        if (item == ModItems.MISSILE_EMP_STRONG.get()) return MISSILE_STRONG_EMP;

        if (item == ModItems.MISSILE_BURST.get()) return MISSILE_HUGE;
        if (item == ModItems.MISSILE_INFERNO.get()) return MISSILE_HUGE_INC;
        if (item == ModItems.MISSILE_RAIN.get()) return MISSILE_HUGE_CL;
        if (item == ModItems.MISSILE_DRILL.get()) return MISSILE_HUGE_BU;
        if (item == ModItems.MISSILE_SHUTTLE.get()) return MISSILE_SHUTTLE;

        if (item == ModItems.MISSILE_NUCLEAR.get()) return MISSILE_ATLAS_NUCLEAR;
        if (item == ModItems.MISSILE_NUCLEAR_CLUSTER.get()) return MISSILE_ATLAS_THERMO;
        if (item == ModItems.MISSILE_VOLCANO.get()) return MISSILE_ATLAS_TECTONIC;
        if (item == ModItems.MISSILE_DOOMSDAY.get()) return MISSILE_ATLAS_DOOMSDAY;
        if (item == ModItems.MISSILE_DOOMSDAY_RUSTED.get()) return MISSILE_ATLAS_DOOMSDAY_WEATHERED;

        return PLACEHOLDER;
    }

    public static ResourceLocation forStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return PLACEHOLDER;
        }
        return forItem(stack.getItem());
    }

    public static ResourceLocation forEntity(MissileBaseEntity entity) {
        if (entity instanceof MissileTestEntity) return MISSILE_MICRO;
        if (entity instanceof MissileABMEntity) return MISSILE_ABM;

        if (entity instanceof MissileTier0.MissileMicro) return MISSILE_MICRO;
        if (entity instanceof MissileTier0.MissileTaint) return MISSILE_MICRO_TAINT;
        if (entity instanceof MissileTier0.MissileBHole) return MISSILE_MICRO_BHOLE;
        if (entity instanceof MissileTier0.MissileSchrabidium) return MISSILE_MICRO_SCHRAB;
        if (entity instanceof MissileTier0.MissileEmp) return MISSILE_MICRO_EMP;

        if (entity instanceof MissileTier1.MissileGeneric) return MISSILE_V2;
        if (entity instanceof MissileTier1.MissileIncendiary) return MISSILE_V2_INC;
        if (entity instanceof MissileTier1.MissileCluster) return MISSILE_V2_CL;
        if (entity instanceof MissileTier1.MissileBuster) return MISSILE_V2_BU;
        if (entity instanceof MissileTier1.MissileDecoy) return MISSILE_V2_DECOY;

        if (entity instanceof MissileStealthEntity) return MISSILE_STEALTH;

        if (entity instanceof MissileTier2.MissileStrong) return MISSILE_STRONG;
        if (entity instanceof MissileTier2.MissileIncendiaryStrong) return MISSILE_STRONG_INC;
        if (entity instanceof MissileTier2.MissileClusterStrong) return MISSILE_STRONG_CL;
        if (entity instanceof MissileTier2.MissileBusterStrong) return MISSILE_STRONG_BU;
        if (entity instanceof MissileTier2.MissileEmpStrong) return MISSILE_STRONG_EMP;

        if (entity instanceof MissileTier3.MissileBurst) return MISSILE_HUGE;
        if (entity instanceof MissileTier3.MissileInferno) return MISSILE_HUGE_INC;
        if (entity instanceof MissileTier3.MissileRain) return MISSILE_HUGE_CL;
        if (entity instanceof MissileTier3.MissileDrill) return MISSILE_HUGE_BU;
        if (entity instanceof MissileShuttleEntity) return MISSILE_SHUTTLE;

        if (entity instanceof MissileTier4.MissileNuclear) return MISSILE_ATLAS_NUCLEAR;
        if (entity instanceof MissileTier4.MissileNuclearCluster) return MISSILE_ATLAS_THERMO;
        if (entity instanceof MissileTier4.MissileVolcano) return MISSILE_ATLAS_TECTONIC;
        if (entity instanceof MissileTier4.MissileDoomsday) return MISSILE_ATLAS_DOOMSDAY;
        if (entity instanceof MissileTier4.MissileDoomsdayRusted) return MISSILE_ATLAS_DOOMSDAY_WEATHERED;

        return PLACEHOLDER;
    }
}
