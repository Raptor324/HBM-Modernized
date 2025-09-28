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

        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name));
    }
    
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
}