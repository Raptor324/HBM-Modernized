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

    private static final String[] KEYS = {
            "misc.nullchopper", "misc.nullcrashing", "misc.nullmine", "block.cratebreak",
            "block.shutdown", "block.mineroperate", "block.assembleroperate", "block.chemplantoperate",
            "block.dieseloperate", "block.igeneratoroperate", "block.turbofanoperate", "block.pressoperate",
            "block.sonarping", "block.reactorstart", "block.reactorstop", "block.vaultscrape",
            "block.vaultthud", "block.vaultscrapenew", "block.vaultthudnew", "block.lockopen",
            "block.lockhang", "block.centrifugeoperate", "block.pipeplaced", "block.flesh",
            "block.missileassembly", "block.missileassembly2", "block.opendoor", "block.closedoor",
            "block.soyuzready", "block.screm", "block.chunguslever", "block.leverlarge",
            "block.bobble", "block.crateopen", "block.crateclose", "block.storageopen",
            "block.storageclose", "block.openc", "block.closec", "block.warnoverspeed",
            "block.boilergroan", "block.steamengineoperate", "block.turbinegasstartup", "block.turbinegasrunning",
            "block.turbinegasshutdown", "block.chungusturbinerunning", "block.largeturbinerunning", "block.damage",
            "block.electrichum", "block.fensuhum", "block.boiler", "block.hornnearsingle",
            "block.hornneardual", "block.hornfarsingle", "block.hornfardual", "block.reactorloop",
            "block.fusionreactorrunning", "block.hephaestusrunning", "block.squeakytoy", "block.hunduns_magnificent_howl",
            "block.pyrooperate", "block.engine", "block.chemicalplant", "block.assemblerstrike",
            "block.assemblerstart", "block.assemblerstop", "block.assemblercut", "block.leverstart",
            "block.leverstop", "block.spark", "block.metalimpact", "door.transitionsealopen",
            "door.wghstart", "door.wghstop", "door.alarm6", "door.sliding_door_shut",
            "door.sliding_door_opened", "door.sliding_door_opening", "door.garage_move", "door.garage_stop",
            "door.lever", "door.wgh_start", "door.wgh_stop", "door.wgh_big_start",
            "door.wgh_big_stop", "door.qe_sliding_shut", "door.qe_sliding_opened", "door.qe_sliding_opening",
            "door.sliding_seal_open", "door.sliding_seal_stop", "item.techbleep", "item.techboop",
            "item.pinunlock", "item.pinbreak", "item.gasmaskscrew", "item.jetpacktank",
            "item.unpack", "item.syringe", "item.radaway", "item.spray",
            "item.repair", "item.vice", "item.upgradeplug", "item.battery",
            "music.recordlambdacore", "music.recordsectorsweep", "music.recordvortalcombat", "music.transmission",
            "weapon.taushoot", "weapon.tauchargeloop", "weapon.tauchargeloop2", "weapon.revolvershoot",
            "weapon.revolvershootalt", "weapon.shotgunshoot", "weapon.schrabidiumshoot", "weapon.singflyby",
            "weapon.rifleshoot", "weapon.rpgshoot", "weapon.fatmanshoot", "weapon.fatmanreload",
            "weapon.flamethrowerignite", "weapon.flamethrowershoot", "weapon.immolatorignite", "weapon.immolatorshoot",
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
            "weapon.glclose", "weapon.44shoot", "weapon.trainimpact", "weapon.nuclearexplosion",
            "weapon.explosionlargenear", "weapon.explosionlargefar", "weapon.explosionsmallnear", "weapon.explosionsmallfar",
            "weapon.explosiontiny", "weapon.hkshoot", "weapon.grenadebounce", "weapon.dflash",
            "weapon.switchmode1", "weapon.switchmode2", "weapon.fire.blackpowder", "weapon.fire.flameloop",
            "weapon.fire.lockon", "weapon.fire.shreddercycle", "weapon.fire.tau", "weapon.fire.tauloop",
            "weapon.fire.taurelease", "weapon.fire.fatman", "weapon.fire.smack", "weapon.fire.vstar",
            "weapon.fire.loudestnoiseonearth", "weapon.fire.disintegration", "weapon.fire.laser", "weapon.fire.laserpistol",
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
            "weapon.reload.shotguncockclose", "weapon.reload.shotgunreload", "weapon.reload.tubefwoomp", "weapon.reload.impact",
            "weapon.reload.fatmanfull", "weapon.reload.screw", "weapon.reload.grenadetech", "weapon.reload.grenadenuka",
            "weapon.foley.gunwhack", "weapon.casing.small", "weapon.casing.medium", "weapon.casing.large",
            "weapon.casing.shell", "turret.chekhov_fire", "turret.jeremy_fire", "turret.jeremy_reload",
            "turret.richard_fire", "turret.howard_fire", "turret.howard_reload", "turret.sentry_fire",
            "turret.sentry_lockon", "turret.mortarwhistle", "entity.chopperflyingloop", "entity.chopperdrop",
            "entity.choppercharge", "entity.chopperdamage", "entity.choppermineloop", "entity.choppercrashingloop",
            "entity.oldexplosion", "entity.rockettakeoff", "entity.soyuztakeoff", "entity.bombdet",
            "entity.bombwhistle", "entity.bomberloop", "entity.bombersmallloop", "entity.planecrash",
            "entity.planeshotdown", "entity.cybercrab", "entity.ducc", "entity.slicer",
            "entity.megaquacc", "entity.ufobeam", "entity.ufoblast", "entity.siegeidle",
            "entity.siegehurt", "entity.siegedeath", "entity.meteoritefallingloop", "step.iron",
            "step.metalblock", "step.platemetal", "block.platemetalplace", "player.gulp",
            "player.groan", "potatos.random", "alarm.apcloop", "alarm.apcpass",
            "alarm.bankalarm", "alarm.autopilot", "alarm.containeralarm", "alarm.hatch",
            "alarm.razortrainhorn", "alarm.trainhorn", "alarm.blastdooralarm", "alarm.klaxon",
            "alarm.foklaxona", "alarm.foklaxonb", "alarm.easalarm", "alarm.airraid",
            "alarm.classic", "alarm.gambit", "alarm.soyuzed", "alarm.chime",
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
        String key = originalKey.toLowerCase(Locale.ROOT);
        if (key.startsWith("hbm:")) key = key.substring(4);
        RegistrySupplier<SoundEvent> sup = EVENTS.get(key);
        if (sup != null && sup.isPresent()) return sup.get();
        return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, key));
    }
}
