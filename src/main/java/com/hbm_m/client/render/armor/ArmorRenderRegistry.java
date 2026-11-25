package com.hbm_m.client.render.armor;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.client.render.armor.ArmorRenderSpec.Bone;
import com.hbm_m.client.render.armor.ArmorRenderSpec.RenderMode;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.ArmorBJJetpack;
import com.mojang.math.Axis;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Central registry mapping every 3D OBJ/NMesh power armor and hazmat item to its {@link ArmorRenderSpec}.
 * Unifies the previous {@code AbstractObjArmorLayer} sets (T51, AJR, AJRO, Bismuth, DNS, Hazmat) and
 * {@code ObjArmorLayer} sets (BJ, Steamsuit, Dieselsuit, FAU, Envsuit, HEV, NCRPA, RPA, Taurun, Trenchmaster).
 */
public final class ArmorRenderRegistry {

    private static final Map<Item, ArmorRenderSpec> ITEM_SPECS = new IdentityHashMap<>();
    private static volatile boolean initialized = false;

    private ArmorRenderRegistry() {}

    @Nullable
    public static ArmorRenderSpec getSpec(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return getSpec(stack.getItem());
    }

    @Nullable
    public static ArmorRenderSpec getSpec(Item item) {
        ensureInitialized();
        return ITEM_SPECS.get(item);
    }

    public static boolean is3dArmorItem(Item item) {
        return getSpec(item) != null;
    }

    public static Collection<Item> getAll3dArmorItems() {
        ensureInitialized();
        return Collections.unmodifiableSet(ITEM_SPECS.keySet());
    }

    public static void reset() {
        synchronized (ArmorRenderRegistry.class) {
            ITEM_SPECS.clear();
            initialized = false;
        }
    }

    private static void ensureInitialized() {
        if (initialized) return;
        synchronized (ArmorRenderRegistry.class) {
            if (initialized) return;
            registerAll();
            initialized = true;
        }
    }

    private static ResourceLocation armorTex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/armor/" + name + ".png");
    }

    private static ResourceLocation whiteTex() {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/misc/white.png");
    }

    private static void registerSet(ArmorRenderSpec spec, Item helmet, Item chest, Item legs, Item boots) {
        ITEM_SPECS.put(helmet, spec);
        ITEM_SPECS.put(chest, spec);
        ITEM_SPECS.put(legs, spec);
        ITEM_SPECS.put(boots, spec);
    }

    /** Standard 4-texture HBM armor layout: Helmet, Chest, Left/RightArm, Left/RightLeg, Left/RightBoot. */
    private static ArmorRenderSpec standardSet(String objName, String texPrefix,
                                               String headPart, String chestPart,
                                               String leftFootPart, String rightFootPart) {
        ResourceLocation legTex = armorTex(texPrefix + "_leg");
        ResourceLocation armTex = armorTex(texPrefix + "_arm");
        return ArmorRenderSpec.builder("hbm_m:models/armor/" + objName + ".obj")
                .add(EquipmentSlot.HEAD, headPart, Bone.HEAD, armorTex(texPrefix + "_helmet"))
                .add(EquipmentSlot.CHEST, chestPart, Bone.BODY, armorTex(texPrefix + "_chest"))
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, armTex)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, armTex)
                .add(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, legTex)
                .add(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, legTex)
                .add(EquipmentSlot.FEET, leftFootPart, Bone.LEFT_LEG, legTex)
                .add(EquipmentSlot.FEET, rightFootPart, Bone.RIGHT_LEG, legTex)
                .build();
    }

    private static ResourceLocation hazmatTex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/block/armor/" + name + ".png");
    }

    private static ArmorRenderSpec hazmatVariant(String texName) {
        ResourceLocation tex = armorTex(texName);
        return ArmorRenderSpec.builder("hbm_m:models/armor/hazmat.obj")
                .add(EquipmentSlot.HEAD, "Helmet", Bone.HEAD, tex)
                // Filter canister attaches only when a replaceable filter is screwed in (not included in stable icon bounds).
                .addConditional(EquipmentSlot.HEAD, "Filter", Bone.HEAD, tex, IGasMask::hasFilter, false)
                .add(EquipmentSlot.CHEST, "Chest", Bone.BODY, tex)
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, tex)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, tex)
                .add(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, tex)
                .add(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, tex)
                .add(EquipmentSlot.FEET, "LeftBoot", Bone.LEFT_LEG, tex)
                .add(EquipmentSlot.FEET, "RightBoot", Bone.RIGHT_LEG, tex)
                .build();
    }

    private static void registerAll() {
        // 1. T-51 Power Armor
        registerSet(standardSet("t51", "t51", "Helmet", "Chest", "LeftBoot", "RightBoot"),
                ModItems.T51_HELMET.get(), ModItems.T51_PLATE.get(), ModItems.T51_LEGS.get(), ModItems.T51_BOOTS.get());

        // 2. T-45 AJR Power Armor
        registerSet(standardSet("ajr_a", "ajr", "Helmet", "Chest", "LeftBoot", "RightBoot"),
                ModItems.AJR_HELMET.get(), ModItems.AJR_PLATE.get(), ModItems.AJR_LEGS.get(), ModItems.AJR_BOOTS.get());

        // 3. AJRO Power Armor
        registerSet(standardSet("ajr_a", "ajro", "Helmet", "Chest", "LeftBoot", "RightBoot"),
                ModItems.AJRO_HELMET.get(), ModItems.AJRO_PLATE.get(), ModItems.AJRO_LEGS.get(), ModItems.AJRO_BOOTS.get());

        // 4. Bismuth Armor (uses single atlas texture + MTL Kd diffuse tint)
        ResourceLocation bismuthTex = armorTex("bismuth");
        ArmorRenderSpec bismuth = ArmorRenderSpec.builder("hbm_m:models/armor/bismuth.obj")
                .applyMtlTint(true)
                .add(EquipmentSlot.HEAD, "Helmet", Bone.HEAD, bismuthTex)
                .add(EquipmentSlot.CHEST, "Chest", Bone.BODY, bismuthTex)
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, bismuthTex)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, bismuthTex)
                .add(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, bismuthTex)
                .add(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, bismuthTex)
                .add(EquipmentSlot.FEET, "LeftBoot", Bone.LEFT_LEG, bismuthTex)
                .add(EquipmentSlot.FEET, "RightBoot", Bone.RIGHT_LEG, bismuthTex)
                .build();
        registerSet(bismuth, ModItems.BISMUTH_HELMET.get(), ModItems.BISMUTH_PLATE.get(),
                ModItems.BISMUTH_LEGS.get(), ModItems.BISMUTH_BOOTS.get());

        // 5. DNT Nano Suit (dns_*)
        registerSet(standardSet("dnt", "dnt", "Helmet", "Chest", "LeftBoot", "RightBoot"),
                ModItems.DNS_HELMET.get(), ModItems.DNS_PLATE.get(), ModItems.DNS_LEGS.get(), ModItems.DNS_BOOTS.get());

        // 6. BJ Lunar Armor (+ Jetpack variant)
        ResourceLocation bjArm = armorTex("bj_arm");
        ResourceLocation bjLeg = armorTex("bj_leg");
        ArmorRenderSpec bj = ArmorRenderSpec.builder("hbm_m:models/armor/bj.obj")
                .add(EquipmentSlot.HEAD, "Head", Bone.HEAD, armorTex("bj_eyepatch"))
                .add(EquipmentSlot.CHEST, "Body", Bone.BODY, armorTex("bj_chest"))
                .addConditional(EquipmentSlot.CHEST, "Jetpack", Bone.BODY, armorTex("bj_jetpack"),
                        stack -> stack.getItem() instanceof ArmorBJJetpack, true)
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, bjArm)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, bjArm)
                .add(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, bjLeg)
                .add(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, bjLeg)
                .add(EquipmentSlot.FEET, "LeftFoot", Bone.LEFT_LEG, bjLeg)
                .add(EquipmentSlot.FEET, "RightFoot", Bone.RIGHT_LEG, bjLeg)
                .build();
        registerSet(bj, ModItems.BJ_HELMET.get(), ModItems.BJ_PLATE.get(), ModItems.BJ_LEGS.get(), ModItems.BJ_BOOTS.get());
        ITEM_SPECS.put(ModItems.BJ_PLATE_JETPACK.get(), bj);

        // 7. Desh Steamsuit
        registerSet(standardSet("steamsuit", "steamsuit", "Head", "Body", "LeftBoot", "RightBoot"),
                ModItems.STEAMSUIT_HELMET.get(), ModItems.STEAMSUIT_PLATE.get(),
                ModItems.STEAMSUIT_LEGS.get(), ModItems.STEAMSUIT_BOOTS.get());

        // 8. Dieselsuit (bnuuy)
        registerSet(standardSet("bnuuy", "bnuuy", "Head", "Body", "LeftBoot", "RightBoot"),
                ModItems.DIESELSUIT_HELMET.get(), ModItems.DIESELSUIT_PLATE.get(),
                ModItems.DIESELSUIT_LEGS.get(), ModItems.DIESELSUIT_BOOTS.get());

        // 9. FAU / Digamma Armor (translucent Cassette)
        ResourceLocation fauArm = armorTex("fau_arm");
        ResourceLocation fauLeg = armorTex("fau_leg");
        ArmorRenderSpec fau = ArmorRenderSpec.builder("hbm_m:models/armor/fau.obj")
                .add(EquipmentSlot.HEAD, "Head", Bone.HEAD, armorTex("fau_helmet"))
                .add(EquipmentSlot.CHEST, "Body", Bone.BODY, armorTex("fau_chest"))
                .add(EquipmentSlot.CHEST, "Cassette", Bone.BODY, armorTex("fau_cassette"), RenderMode.TRANSLUCENT)
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, fauArm)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, fauArm)
                .add(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, fauLeg)
                .add(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, fauLeg)
                .add(EquipmentSlot.FEET, "LeftBoot", Bone.LEFT_LEG, fauLeg)
                .add(EquipmentSlot.FEET, "RightBoot", Bone.RIGHT_LEG, fauLeg)
                .build();
        registerSet(fau, ModItems.FAU_HELMET.get(), ModItems.FAU_PLATE.get(), ModItems.FAU_LEGS.get(), ModItems.FAU_BOOTS.get());

        // 10. Envsuit (translucent helmet visor + warm fullbright lamps)
        ResourceLocation envArm = armorTex("envsuit_arm");
        ResourceLocation envLeg = armorTex("envsuit_leg");
        ArmorRenderSpec envsuit = ArmorRenderSpec.builder("hbm_m:models/armor/envsuit.obj")
                .add(EquipmentSlot.HEAD, "Helmet", Bone.HEAD, armorTex("envsuit_helmet"), RenderMode.TRANSLUCENT)
                .add(EquipmentSlot.HEAD, "Lamps", Bone.HEAD, whiteTex(), RenderMode.GLOW, 1.0F, 1.0F, 0.8F)
                .add(EquipmentSlot.CHEST, "Chest", Bone.BODY, armorTex("envsuit_chest"))
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, envArm)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, envArm)
                .add(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, envLeg)
                .add(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, envLeg)
                .add(EquipmentSlot.FEET, "LeftFoot", Bone.LEFT_LEG, envLeg)
                .add(EquipmentSlot.FEET, "RightFoot", Bone.RIGHT_LEG, envLeg)
                .build();
        registerSet(envsuit, ModItems.ENVSUIT_HELMET.get(), ModItems.ENVSUIT_PLATE.get(),
                ModItems.ENVSUIT_LEGS.get(), ModItems.ENVSUIT_BOOTS.get());

        // 11. HEV Suit
        registerSet(standardSet("hev", "hev", "Head", "Body", "LeftFoot", "RightFoot"),
                ModItems.HEV_HELMET.get(), ModItems.HEV_PLATE.get(), ModItems.HEV_LEGS.get(), ModItems.HEV_BOOTS.get());

        // 12. NCR Ranger Power Armor (glowing visor eyes)
        ResourceLocation ncrpaHelmet = armorTex("ncrpa_helmet");
        ResourceLocation ncrpaArm = armorTex("ncrpa_arm");
        ResourceLocation ncrpaLeg = armorTex("ncrpa_leg");
        ArmorRenderSpec ncrpa = ArmorRenderSpec.builder("hbm_m:models/armor/ncrpa.obj")
                .add(EquipmentSlot.HEAD, "Helmet", Bone.HEAD, ncrpaHelmet)
                .add(EquipmentSlot.HEAD, "Eyes", Bone.HEAD, ncrpaHelmet, RenderMode.GLOW)
                .add(EquipmentSlot.CHEST, "Chest", Bone.BODY, armorTex("ncrpa_chest"))
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, ncrpaArm)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, ncrpaArm)
                .add(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, ncrpaLeg)
                .add(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, ncrpaLeg)
                .add(EquipmentSlot.FEET, "LeftBoot", Bone.LEFT_LEG, ncrpaLeg)
                .add(EquipmentSlot.FEET, "RightBoot", Bone.RIGHT_LEG, ncrpaLeg)
                .build();
        registerSet(ncrpa, ModItems.NCRPA_HELMET.get(), ModItems.NCRPA_PLATE.get(),
                ModItems.NCRPA_LEGS.get(), ModItems.NCRPA_BOOTS.get());

        // 13. Remnant Power Armor (glowing back core + spinning fan at y=4.875px)
        ResourceLocation rpaChest = armorTex("rpa_chest");
        ResourceLocation rpaArm = armorTex("rpa_arm");
        ResourceLocation rpaLeg = armorTex("rpa_leg");
        ArmorRenderSpec rpa = ArmorRenderSpec.builder("hbm_m:models/armor/remnant.obj")
                .add(EquipmentSlot.HEAD, "Head", Bone.HEAD, armorTex("rpa_helmet"))
                .add(EquipmentSlot.CHEST, "Body", Bone.BODY, rpaChest)
                .add(EquipmentSlot.CHEST, "Glow", Bone.BODY, rpaChest, RenderMode.GLOW)
                .addAnimated(EquipmentSlot.CHEST, "Fan", Bone.BODY, rpaChest, (pose, stack) -> {
                    pose.translate(0.0F, 4.875F, 0.0F);
                    pose.mulPose(Axis.ZP.rotationDegrees((float) (-System.currentTimeMillis() / 2.0 % 360.0)));
                    pose.translate(0.0F, -4.875F, 0.0F);
                })
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, rpaArm)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, rpaArm)
                .add(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, rpaLeg)
                .add(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, rpaLeg)
                .add(EquipmentSlot.FEET, "LeftBoot", Bone.LEFT_LEG, rpaLeg)
                .add(EquipmentSlot.FEET, "RightBoot", Bone.RIGHT_LEG, rpaLeg)
                .build();
        registerSet(rpa, ModItems.RPA_HELMET.get(), ModItems.RPA_PLATE.get(), ModItems.RPA_LEGS.get(), ModItems.RPA_BOOTS.get());

        // 14. Taurun Armor (slightly spaced legs)
        ResourceLocation taurunArm = armorTex("taurun_arm");
        ResourceLocation taurunLeg = armorTex("taurun_leg");
        ArmorRenderSpec taurun = ArmorRenderSpec.builder("hbm_m:models/armor/taurun.obj")
                .add(EquipmentSlot.HEAD, "Helmet", Bone.HEAD, armorTex("taurun_helmet"))
                .add(EquipmentSlot.CHEST, "Chest", Bone.BODY, armorTex("taurun_chest"))
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, taurunArm)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, taurunArm)
                .addOffset(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, taurunLeg, -0.01F, 0.0F, 0.0F)
                .addOffset(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, taurunLeg, 0.01F, 0.0F, 0.0F)
                .addOffset(EquipmentSlot.FEET, "LeftBoot", Bone.LEFT_LEG, taurunLeg, -0.01F, 0.0F, 0.0F)
                .addOffset(EquipmentSlot.FEET, "RightBoot", Bone.RIGHT_LEG, taurunLeg, 0.01F, 0.0F, 0.0F)
                .build();
        registerSet(taurun, ModItems.TAURUN_HELMET.get(), ModItems.TAURUN_PLATE.get(),
                ModItems.TAURUN_LEGS.get(), ModItems.TAURUN_BOOTS.get());

        // 15. Trenchmaster Armor (translucent helmet + glowing light + spaced legs)
        ResourceLocation trenchHelmet = armorTex("trenchmaster_helmet");
        ResourceLocation trenchArm = armorTex("trenchmaster_arm");
        ResourceLocation trenchLeg = armorTex("trenchmaster_leg");
        ArmorRenderSpec trenchmaster = ArmorRenderSpec.builder("hbm_m:models/armor/trenchmaster.obj")
                .add(EquipmentSlot.HEAD, "Helmet", Bone.HEAD, trenchHelmet, RenderMode.TRANSLUCENT)
                .add(EquipmentSlot.HEAD, "Light", Bone.HEAD, trenchHelmet, RenderMode.GLOW)
                .add(EquipmentSlot.CHEST, "Chest", Bone.BODY, armorTex("trenchmaster_chest"))
                .add(EquipmentSlot.CHEST, "LeftArm", Bone.LEFT_ARM, trenchArm)
                .add(EquipmentSlot.CHEST, "RightArm", Bone.RIGHT_ARM, trenchArm)
                .addOffset(EquipmentSlot.LEGS, "LeftLeg", Bone.LEFT_LEG, trenchLeg, -0.01F, 0.0F, 0.0F)
                .addOffset(EquipmentSlot.LEGS, "RightLeg", Bone.RIGHT_LEG, trenchLeg, 0.01F, 0.0F, 0.0F)
                .addOffset(EquipmentSlot.FEET, "LeftBoot", Bone.LEFT_LEG, trenchLeg, -0.01F, 0.0F, 0.0F)
                .addOffset(EquipmentSlot.FEET, "RightBoot", Bone.RIGHT_LEG, trenchLeg, 0.01F, 0.0F, 0.0F)
                .build();
        registerSet(trenchmaster, ModItems.TRENCHMASTER_HELMET.get(), ModItems.TRENCHMASTER_PLATE.get(),
                ModItems.TRENCHMASTER_LEGS.get(), ModItems.TRENCHMASTER_BOOTS.get());

        // 16-18. 3D Hazmat Suits (Yellow, Red, Grey)
        registerSet(hazmatVariant("hazmat"),
                ModItems.HAZMAT_HELMET.get(), ModItems.HAZMAT_PLATE.get(),
                ModItems.HAZMAT_LEGS.get(), ModItems.HAZMAT_BOOTS.get());
        registerSet(hazmatVariant("hazmat_red"),
                ModItems.HAZMAT_HELMET_RED.get(), ModItems.HAZMAT_PLATE_RED.get(),
                ModItems.HAZMAT_LEGS_RED.get(), ModItems.HAZMAT_BOOTS_RED.get());
        registerSet(hazmatVariant("hazmat_grey"),
                ModItems.HAZMAT_HELMET_GREY.get(), ModItems.HAZMAT_PLATE_GREY.get(),
                ModItems.HAZMAT_LEGS_GREY.get(), ModItems.HAZMAT_BOOTS_GREY.get());
    }
}
