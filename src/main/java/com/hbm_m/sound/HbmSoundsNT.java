package com.hbm_m.sound;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.RegistrySupplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Alle Soundereignisse des Originals unter ihrem Originalnamen (kleingeschrieben, 1.20 verlangt
 * das). Erzeugt von {@code tools/restport_sounds.py} - nicht von Hand editieren.
 * {@link #get(String)} nimmt den Originalschluessel ({@code "hbm:weapon.mukeExplosion"} oder
 * {@code "weapon.mukeExplosion"}) und findet auch Ereignisse, die {@link ModSounds} schon registriert.
 */
public final class HbmSoundsNT {

    private HbmSoundsNT() {}

    private static final Map<String, RegistrySupplier<SoundEvent>> EVENTS = new HashMap<>();

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("block.cratebreak", "block.cratebreak1"),
            Map.entry("block.assembleroperate", "block.assembler_operate"),
            Map.entry("block.turbofanoperate", "block.turbofan_operate"),
            Map.entry("block.pressoperate", "block.press_operate"),
            Map.entry("block.sonarping", "block.sonar_ping"),
            Map.entry("block.reactorstart", "reactor_start"),
            Map.entry("block.reactorstop", "reactor_stop"),
            Map.entry("block.vaultscrape", "block.vault_scrape"),
            Map.entry("block.vaultthud", "block.vault_thud"),
            Map.entry("block.lockopen", "block.lock_open"),
            Map.entry("block.lockhang", "block.lock_hang"),
            Map.entry("block.pipeplaced", "block.pipe_placed"),
            Map.entry("block.steamengineoperate", "block.steam_engine_operate"),
            Map.entry("block.chungusturbinerunning", "block.chungus_turbine"),
            Map.entry("block.largeturbinerunning", "block.large_turbine"),
            Map.entry("block.damage", "block.machine_damage"),
            Map.entry("block.electrichum", "block.electric_hum"),
            Map.entry("block.fusionreactorrunning", "block.fusion_reactor_spin"),
            Map.entry("block.chemicalplant", "block.chemical_plant"),
            Map.entry("block.assemblerstrike", "block.assembler_strike_random"),
            Map.entry("block.assemblerstart", "block.assembler_start"),
            Map.entry("block.assemblerstop", "block.assembler_stop"),
            Map.entry("block.assemblercut", "block.assembler_cut"),
            Map.entry("door.transitionsealopen", "block.transition_seal_open"),
            Map.entry("door.wghstart", "block.wgh_start"),
            Map.entry("door.wghstop", "block.wgh_stop"),
            Map.entry("door.alarm6", "block.alarm_6"),
            Map.entry("door.sliding_door_shut", "block.sliding_door_shut"),
            Map.entry("door.sliding_door_opened", "block.sliding_door_opened"),
            Map.entry("door.sliding_door_opening", "block.sliding_door_opening"),
            Map.entry("door.garage_move", "block.garage_move"),
            Map.entry("door.garage_stop", "block.garage_stop"),
            Map.entry("door.lever", "block.lever_1"),
            Map.entry("door.wgh_start", "block.wgh_start"),
            Map.entry("door.wgh_stop", "block.wgh_stop"),
            Map.entry("door.wgh_big_start", "block.door_wgh_big_start"),
            Map.entry("door.wgh_big_stop", "block.door_wgh_big_stop"),
            Map.entry("door.qe_sliding_shut", "block.sliding_door_shut"),
            Map.entry("door.qe_sliding_opened", "block.sliding_door_opened"),
            Map.entry("door.qe_sliding_opening", "block.sliding_door_opening"),
            Map.entry("item.techbleep", "techbleep"),
            Map.entry("item.techboop", "techboop"),
            Map.entry("item.pinunlock", "item.pin_unlock"),
            Map.entry("item.pinbreak", "item.pin_break"),
            Map.entry("item.gasmaskscrew", "tool.gasmaskscrew"),
            Map.entry("item.radaway", "radaway_use"),
            Map.entry("item.repair", "tool.repair_random"),
            Map.entry("item.upgradeplug", "tool.upgrade_plug"),
            Map.entry("weapon.flamethrowershoot", "weapon.flamethrower_shoot"),
            Map.entry("weapon.nuclearexplosion", "nuclear_explosion"),
            Map.entry("weapon.explosionlargenear", "explosionlargenear"),
            Map.entry("weapon.explosionlargefar", "explosionlargefar"),
            Map.entry("weapon.explosionsmallnear", "explosionsmallnear1"),
            Map.entry("weapon.explosionsmallfar", "explosionsmallfar1"),
            Map.entry("weapon.explosiontiny", "explosiontiny"),
            Map.entry("weapon.dflash", "weapon.d_flash"),
            Map.entry("weapon.fire.disintegration", "weapon.disintegration"),
            Map.entry("weapon.reload.tubefwoomp", "weapon.reload.tube_fwoomp"),
            Map.entry("entity.oldexplosion", "oldexplosion"),
            Map.entry("entity.bombdet", "bombdet1"),
            Map.entry("entity.bombwhistle", "bombwhistle"),
            Map.entry("entity.bomberloop", "bomber1"),
            Map.entry("entity.bombersmallloop", "bomber2"),
            Map.entry("alarm.classic", "alarm.classicsiren")
    );

    private static final String[] KEYS = {
            "misc.nullchopper", "misc.nullcrashing", "misc.nullmine",
            "block.shutdown", "block.mineroperate", "block.chemplantoperate",
            "block.dieseloperate", "block.igeneratoroperate",
            "block.vaultscrapenew", "block.vaultthudnew",
            "block.centrifugeoperate", "block.flesh",
            "block.missileassembly", "block.missileassembly2", "block.opendoor", "block.closedoor",
            "block.soyuzready", "block.screm", "block.chunguslever", "block.leverlarge",
            "block.bobble", "block.crateopen", "block.crateclose", "block.storageopen",
            "block.storageclose", "block.openc", "block.closec", "block.warnoverspeed",
            "block.boilergroan", "block.turbinegasstartup", "block.turbinegasrunning",
            "block.turbinegasshutdown",
            "block.fensuhum", "block.boiler", "block.hornnearsingle",
            "block.hornneardual", "block.hornfarsingle", "block.hornfardual", "block.reactorloop",
            "block.hephaestusrunning", "block.squeakytoy", "block.hunduns_magnificent_howl",
            "block.pyrooperate", "block.engine",
            "block.leverstart",
            "block.leverstop", "block.spark", "block.metalimpact",
            "item.jetpacktank",
            "item.syringe", "item.spray",
            "item.vice", "item.battery",
            "music.recordlambdacore", "music.recordsectorsweep", "music.recordvortalcombat", "music.transmission",
            "weapon.taushoot", "weapon.tauchargeloop", "weapon.tauchargeloop2", "weapon.revolvershoot",
            "weapon.revolvershootalt", "weapon.shotgunshoot", "weapon.schrabidiumshoot", "weapon.singflyby",
            "weapon.rifleshoot", "weapon.rpgshoot", "weapon.fatmanshoot", "weapon.fatmanreload",
            "weapon.flamethrowerignite", "weapon.immolatorignite", "weapon.immolatorshoot",
            "weapon.missiletakeoff", "weapon.sparkshoot", "weapon.bang", "weapon.slice",
            "weapon.kapeng", "weapon.leveractionreload", "weapon.b92reload", "weapon.stingerlockon",
            "weapon.sawshoot", "weapon.spinup", "weapon.spindown", "weapon.laserbang",
            "weapon.uzishoot", "weapon.silencershoot", "weapon.gbounce", "weapon.calshoot",
            "weapon.ricochet", "weapon.revolverreload", "weapon.shotgunreload", "weapon.magreload",
            "weapon.magreloadbolt", "weapon.rpgreload", "weapon.boat", "weapon.tesla",
            "weapon.teslashoot", "weapon.flamerreload", "weapon.stop", "weapon.bonk",
            "weapon.hksshoot", "weapon.bodysplat", "weapon.quadroreload", "weapon.fstbmbstart",
            "weapon.fstbmbping", "weapon.whack", "weapon.chainsaw", "weapon.rocketflame",
            "weapon.ballslaser", "weapon.dartshoot", "weapon.mukeexplosion", "weapon.cdeploy",
            "weapon.cswing", "weapon.extinguisher", "weapon.robin_explosion", "weapon.shotgunpump",
            "weapon.shotgunpumpalt", "weapon.explosionmedium", "weapon.hicalshot", "weapon.coilgunreload",
            "weapon.coilgunshoot", "weapon.glreload", "weapon.glshoot", "weapon.glopen",
            "weapon.glclose", "weapon.44shoot", "weapon.trainimpact",
            "weapon.hkshoot", "weapon.grenadebounce",
            "weapon.switchmode1", "weapon.switchmode2", "weapon.fire.blackpowder", "weapon.fire.flameloop",
            "weapon.fire.lockon", "weapon.fire.shreddercycle", "weapon.fire.tau", "weapon.fire.tauloop",
            "weapon.fire.taurelease", "weapon.fire.fatman", "weapon.fire.smack", "weapon.fire.vstar",
            "weapon.fire.loudestnoiseonearth", "weapon.fire.laser", "weapon.fire.laserpistol",
            "weapon.fire.lasergatling", "weapon.fire.silenced", "weapon.fire.assault", "weapon.fire.pistol",
            "weapon.fire.rifle", "weapon.fire.rifleheavy", "weapon.fire.shotgun", "weapon.fire.shotgunalt",
            "weapon.fire.shotgunauto", "weapon.fire.greasegun", "weapon.fire.uzi", "weapon.fire.tesla",
            "weapon.fire.aberrator", "weapon.fire.stab", "weapon.fire.grenade", "weapon.fire.amat",
            "weapon.fire.pistollight", "weapon.fire.mk108", "weapon.reload.boltclose", "weapon.reload.boltopen",
            "weapon.reload.closeclick", "weapon.reload.dryfireclick", "weapon.reload.insertcanister", "weapon.reload.insertgrenade",
            "weapon.reload.insertlarge", "weapon.reload.insertrocket", "weapon.reload.levercock", "weapon.reload.maginsert",
            "weapon.reload.magremove", "weapon.reload.magsmallinsert", "weapon.reload.magsmallremove", "weapon.reload.openlatch",
            "weapon.reload.pistolcock", "weapon.reload.pressurevalve", "weapon.reload.revolverclose", "weapon.reload.revolvercock",
            "weapon.reload.revolverspin", "weapon.reload.riflecock", "weapon.reload.shotguncock", "weapon.reload.shotguncockopen",
            "weapon.reload.shotguncockclose", "weapon.reload.shotgunreload", "weapon.reload.impact",
            "weapon.reload.fatmanfull", "weapon.reload.screw", "weapon.reload.grenadetech", "weapon.reload.grenadenuka",
            "weapon.foley.gunwhack", "weapon.casing.small", "weapon.casing.medium", "weapon.casing.large",
            "weapon.casing.shell", "turret.chekhov_fire", "turret.jeremy_fire", "turret.jeremy_reload",
            "turret.richard_fire", "turret.howard_fire", "turret.howard_reload", "turret.sentry_fire",
            "turret.sentry_lockon", "turret.mortarwhistle", "entity.chopperflyingloop", "entity.chopperdrop",
            "entity.choppercharge", "entity.chopperdamage", "entity.choppermineloop", "entity.choppercrashingloop",
            "entity.rockettakeoff", "entity.soyuztakeoff",
            "entity.planecrash",
            "entity.planeshotdown", "entity.cybercrab", "entity.ducc", "entity.slicer",
            "entity.megaquacc", "entity.ufobeam", "entity.ufoblast", "entity.siegeidle",
            "entity.siegehurt", "entity.siegedeath", "entity.meteoritefallingloop", "step.iron",
            "step.metalblock", "step.platemetal", "block.platemetalplace", "player.gulp",
            "player.groan", "potatos.random", "alarm.apcloop", "alarm.apcpass",
            "alarm.bankalarm", "alarm.autopilot", "alarm.containeralarm", "alarm.hatch",
            "alarm.razortrainhorn", "alarm.trainhorn", "alarm.blastdooralarm", "alarm.klaxon",
            "alarm.foklaxona", "alarm.foklaxonb", "alarm.easalarm", "alarm.airraid",
            "alarm.gambit", "alarm.soyuzed", "alarm.chime",
            "alarm.singer",
    };

    static {
        for (String key : KEYS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, key);
            EVENTS.put(key, ModSounds.SOUND_EVENTS.register(key, () -> SoundEvent.createVariableRangeEvent(id)));
        }
    }

    /** Laedt die Klasse vor {@code SOUND_EVENTS.register()}. */
    public static void init() { }

    public static SoundEvent get(String originalKey) {
        if (originalKey == null) return null;
        String key = originalKey.toLowerCase(Locale.ROOT);
        if (key.startsWith("hbm:")) key = key.substring(4);
        // fremder Namensraum (z.B. Kassetten-Klaenge des Space-Addons): direkt aus der Registry
        if (key.indexOf(':') >= 0) {
            ResourceLocation foreign = ResourceLocation.tryParse(key);
            return foreign == null ? null : BuiltInRegistries.SOUND_EVENT.get(foreign);
        }
        key = ALIASES.getOrDefault(key, key);
        RegistrySupplier<SoundEvent> sup = EVENTS.get(key);
        if (sup != null && sup.isPresent()) return sup.get();
        return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, key));
    }
}
