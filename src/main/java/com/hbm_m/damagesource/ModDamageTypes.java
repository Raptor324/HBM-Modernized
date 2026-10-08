package com.hbm_m.damagesource;

// Класс, содержащий ключи типов урона, используемых в моде.
// Эти ключи применяются при создании DamageSource в ModDamageSources.java.
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypes {

    // Helper-метод для краткости
    private static ResourceKey<DamageType> createKey(String name) {

        //? if fabric && < 1.21.1 {
        /*return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(RefStrings.MODID, name));
        *///?} else {
                return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name));
        //?}

    }
    
    /** SEDNA-Waffen: ein Typ je DamageClass ({@code sedna_physical} usw.), Meldungs-ID = Klassenname klein. */
    public static final ResourceKey<DamageType> SEDNA_PHYSICAL = createKey("sedna_physical");
    public static final ResourceKey<DamageType> SEDNA_FIRE = createKey("sedna_fire");
    public static final ResourceKey<DamageType> SEDNA_EXPLOSIVE = createKey("sedna_explosive");
    public static final ResourceKey<DamageType> SEDNA_ELECTRIC = createKey("sedna_electric");
    public static final ResourceKey<DamageType> SEDNA_PLASMA = createKey("sedna_plasma");
    public static final ResourceKey<DamageType> SEDNA_LASER = createKey("sedna_laser");
    public static final ResourceKey<DamageType> SEDNA_MICROWAVE = createKey("sedna_microwave");
    public static final ResourceKey<DamageType> SEDNA_SUBATOMIC = createKey("sedna_subatomic");
    public static final ResourceKey<DamageType> SEDNA_OTHER = createKey("sedna_other");

    public static ResourceKey<DamageType> sedna(String clazz) {
        return createKey("sedna_" + clazz);
    }

    /**
     * Meldungs-ID je Schadenstyp (Datagen): SEDNA-Typen ohne Praefix ({@code laser}, {@code microwave} ...), die alten
     * Typen {@link #LASER} / {@link #MICROWAVE} (Original {@code ModDamageSource.s_laser} / {@code microwave}) bekommen
     * eigene IDs, sonst teilen sie sich die Todesmeldungen mit den SEDNA-Klassen.
     */
    public static String msgIdFor(String path) {
        if (path.startsWith("sedna_")) return path.substring(6);
        if (path.equals("laser")) return LEGACY_LASER_MSG;
        if (path.equals("microwave")) return LEGACY_MICROWAVE_MSG;
        return path;
    }

    public static final String LEGACY_LASER_MSG = "legacy_laser";
    public static final String LEGACY_MICROWAVE_MSG = "legacy_microwave";

    public static final ResourceKey<DamageType> BLAST = createKey("blast");
    public static final ResourceKey<DamageType> NUCLEAR_BLAST = createKey("nuclear_blast");
    public static final ResourceKey<DamageType> MUD_POISONING = createKey("mud_poisoning");
    public static final ResourceKey<DamageType> ACID = createKey("acid");
    public static final ResourceKey<DamageType> EUTHANIZED_SELF = createKey("euthanized_self");
    public static final ResourceKey<DamageType> EUTHANIZED_SELF_2 = createKey("euthanized_self_2");
    public static final ResourceKey<DamageType> TAU_BLAST = createKey("tau_blast");
    public static final ResourceKey<DamageType> RADIATION = createKey("radiation");
    public static final ResourceKey<DamageType> DIGAMMA = createKey("digamma");
    public static final ResourceKey<DamageType> SUICIDE = createKey("suicide");
    public static final ResourceKey<DamageType> TELEPORTER = createKey("teleporter");
    public static final ResourceKey<DamageType> CHEATER = createKey("cheater");
    public static final ResourceKey<DamageType> RUBBLE = createKey("rubble");
    public static final ResourceKey<DamageType> SHRAPNEL = createKey("shrapnel");
    public static final ResourceKey<DamageType> BLACK_HOLE = createKey("black_hole");
    public static final ResourceKey<DamageType> BLENDER = createKey("blender");
    public static final ResourceKey<DamageType> METEORITE = createKey("meteorite");
    public static final ResourceKey<DamageType> BOXCAR = createKey("boxcar");
    public static final ResourceKey<DamageType> BOAT = createKey("boat");
    public static final ResourceKey<DamageType> BUILDING = createKey("building");
    public static final ResourceKey<DamageType> TAINT = createKey("taint");
    public static final ResourceKey<DamageType> AMS = createKey("ams");
    public static final ResourceKey<DamageType> AMS_CORE = createKey("ams_core");
    public static final ResourceKey<DamageType> BROADCAST = createKey("broadcast");
    public static final ResourceKey<DamageType> BANG = createKey("bang");
    public static final ResourceKey<DamageType> PC = createKey("pc");
    public static final ResourceKey<DamageType> CLOUD = createKey("cloud");
    public static final ResourceKey<DamageType> LEAD = createKey("lead");
    public static final ResourceKey<DamageType> ENERVATION = createKey("enervation");
    public static final ResourceKey<DamageType> ELECTRICITY = createKey("electricity");
    public static final ResourceKey<DamageType> EXHAUST = createKey("exhaust");
    public static final ResourceKey<DamageType> SPIKES = createKey("spikes");
    /** {@code ItemBoltgun}: a rivet, which the original explicitly makes bypass armour. */
    public static final ResourceKey<DamageType> BOLTGUN = createKey("boltgun");
    public static final ResourceKey<DamageType> LUNAR = createKey("lunar");
    public static final ResourceKey<DamageType> MONOXIDE = createKey("monoxide");
    public static final ResourceKey<DamageType> ASBESTOS = createKey("asbestos");
    public static final ResourceKey<DamageType> BLACKLUNG = createKey("blacklung");
    public static final ResourceKey<DamageType> MKU = createKey("mku");
    public static final ResourceKey<DamageType> VACUUM = createKey("vacuum");
    public static final ResourceKey<DamageType> OVERDOSE = createKey("overdose");
    public static final ResourceKey<DamageType> MICROWAVE = createKey("microwave");
    public static final ResourceKey<DamageType> NITAN = createKey("nitan");
    public static final ResourceKey<DamageType> REVOLVER_BULLET = createKey("revolver_bullet");
    public static final ResourceKey<DamageType> CHOPPER_BULLET = createKey("chopper_bullet");
    public static final ResourceKey<DamageType> TAU = createKey("tau");
    public static final ResourceKey<DamageType> CMB = createKey("cmb");
    public static final ResourceKey<DamageType> SUB_ATOMIC = createKey("sub_atomic");
    public static final ResourceKey<DamageType> EUTHANIZED = createKey("euthanized");
    public static final ResourceKey<DamageType> ELECTRIFIED = createKey("electrified");
    public static final ResourceKey<DamageType> FLAMETHROWER = createKey("flamethrower");
    public static final ResourceKey<DamageType> PLASMA = createKey("plasma");
    public static final ResourceKey<DamageType> ICE = createKey("ice");
    public static final ResourceKey<DamageType> LASER = createKey("laser");
    public static final ResourceKey<DamageType> BOIL = createKey("boil");
    public static final ResourceKey<DamageType> ACID_PLAYER = createKey("acid_player");
    public static final ResourceKey<DamageType> HARDLANDING_SMASH = createKey("hardlanding_smash");
}