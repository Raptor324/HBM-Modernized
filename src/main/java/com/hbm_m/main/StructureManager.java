package com.hbm_m.main;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.world.gen.nbt.NBTStructure;

import net.minecraft.resources.ResourceLocation;

/**
 * 1:1 {@code com.hbm.main.StructureManager}: alle .nbt-Strukturen des Mods (umgesetzt nach
 * {@code assets/hbm_m/structures}). Geladen wird erst beim ersten Zugriff.
 */
public class StructureManager {

    // METEOR DUNGEON
    public static final NBTStructure meteor_spike = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/meteor-spike.nbt"));
    public static final NBTStructure meteor_core = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/meteor-core.nbt"));
    public static final NBTStructure meteor_corner = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/meteor-corner.nbt"));
    public static final NBTStructure meteor_t = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/meteor-t.nbt"));
    public static final NBTStructure meteor_stairs = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/meteor-stairs.nbt"));
    public static final NBTStructure meteor_fallback = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/meteor-fallback.nbt"));
    public static final NBTStructure meteor_3_bale = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-bale.nbt"));
    public static final NBTStructure meteor_3_blank = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-blank.nbt"));
    public static final NBTStructure meteor_3_block = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-block.nbt"));
    public static final NBTStructure meteor_3_crab = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-crab.nbt"));
    public static final NBTStructure meteor_3_crab_tesla = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-crab-tesla.nbt"));
    public static final NBTStructure meteor_3_crate = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-crate.nbt"));
    public static final NBTStructure meteor_3_dirt = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-dirt.nbt"));
    public static final NBTStructure meteor_3_lead = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-lead.nbt"));
    public static final NBTStructure meteor_3_ooze = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-ooze.nbt"));
    public static final NBTStructure meteor_3_pillar = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-pillar.nbt"));
    public static final NBTStructure meteor_3_star = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-star.nbt"));
    public static final NBTStructure meteor_3_tesla = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-tesla.nbt"));
    public static final NBTStructure meteor_3_book = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-book.nbt"));
    public static final NBTStructure meteor_3_mku = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-mku.nbt"));
    public static final NBTStructure meteor_3_statue = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-statue.nbt"));
    public static final NBTStructure meteor_3_glow = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/loot3x3/meteor-3-glow.nbt"));
    public static final NBTStructure meteor_room_base_end = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-base-end.nbt"));
    public static final NBTStructure meteor_room_base_thru = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-base-thru.nbt"));
    public static final NBTStructure meteor_room_balcony = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-balcony.nbt"));
    public static final NBTStructure meteor_room_basic = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-basic.nbt"));
    public static final NBTStructure meteor_room_dragon = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-dragon.nbt"));
    public static final NBTStructure meteor_room_ladder = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-ladder.nbt"));
    public static final NBTStructure meteor_room_ooze = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-ooze.nbt"));
    public static final NBTStructure meteor_room_split = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-split.nbt"));
    public static final NBTStructure meteor_room_stairs = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-stairs.nbt"));
    public static final NBTStructure meteor_room_triple = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-triple.nbt"));
    public static final NBTStructure meteor_room_fallback = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/room-fallback.nbt"));
    public static final NBTStructure meteor_dragon_chest = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/headloot/loot-chest.nbt"));
    public static final NBTStructure meteor_dragon_tesla = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/headloot/loot-tesla.nbt"));
    public static final NBTStructure meteor_dragon_trap = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/headloot/loot-trap.nbt"));
    public static final NBTStructure meteor_dragon_crate_crab = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/headloot/loot-crate-crab.nbt"));
    public static final NBTStructure meteor_dragon_fallback = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/meteor/room10/headloot/loot-fallback.nbt"));
    public static final NBTStructure vertibird = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/vertibird.nbt"));
    public static final NBTStructure crashed_vertibird = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/crashed-vertibird.nbt"));
    public static final NBTStructure aircraft_carrier = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/aircraft_carrier.nbt"));
    public static final NBTStructure oil_rig = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/oil_rig.nbt"));
    public static final NBTStructure beached_patrol = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/beached_patrol.nbt"));
    public static final NBTStructure lighthouse = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/lighthouse.nbt"));
    public static final NBTStructure dish = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/dish.nbt"));
    public static final NBTStructure water_pump = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/water_pump.nbt"));
    public static final NBTStructure dead_dish_small = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/dead_dish_small.nbt"));
    public static final NBTStructure desert_shack_1 = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/desert_shack_1.nbt"));
    public static final NBTStructure desert_shack_2 = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/desert_shack_2.nbt"));
    public static final NBTStructure desert_shack_3 = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/desert_shack_3.nbt"));
    public static final NBTStructure laboratory = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/laboratory.nbt"));
    public static final NBTStructure radio_house = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/radio_house.nbt"));
    public static final NBTStructure ntmruinsA = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinsa.nbt"));
    public static final NBTStructure ntmruinsB = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinsb.nbt"));
    public static final NBTStructure ntmruinsC = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinsc.nbt"));
    public static final NBTStructure ntmruinsD = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinsd.nbt"));
    public static final NBTStructure ntmruinsE = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinse.nbt"));
    public static final NBTStructure ntmruinsF = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinsf.nbt"));
    public static final NBTStructure ntmruinsG = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinsg.nbt"));
    public static final NBTStructure ntmruinsH = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinsh.nbt"));
    public static final NBTStructure ntmruinsI = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinsi.nbt"));
    public static final NBTStructure ntmruinsJ = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/ntmruinsj.nbt"));
    public static final NBTStructure forest_post = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/forest_post.nbt"));
    public static final NBTStructure forest_chem = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/forest_chem.nbt"));
    public static final NBTStructure plane1 = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/crashed_plane_1.nbt"));
    public static final NBTStructure plane2 = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/crashed_plane_2.nbt"));
    public static final NBTStructure tower_base = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/tower_base.nbt"));
    public static final NBTStructure factory = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/factory.nbt"));
    public static final NBTStructure crane = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/crane_mod.nbt"));
    public static final NBTStructure broadcasting_tower = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/broadcasting_tower.nbt"));
    // Original genauso: excavator.nbt liegt nicht im Original-Ressourcenordner und wird nirgends verwendet (toter Verweis, 1:1 belassen)
    public static final NBTStructure excavator = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/excavator.nbt"));
    public static final NBTStructure repeater_radio = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/repeater_radio.nbt"));
    public static final NBTStructure spire = new NBTStructure(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structures/spire.nbt"));
}
