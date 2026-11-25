package com.hbm_m.test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.gasmask.IGasMask;
import com.hbm_m.powerarmor.ArmorBJJetpack;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Server-safe GameTest verifying 3D power armor &amp; 3D hazmat assets, compiled .nmesh submeshes,
 * generated builtin/entity item JSONs, conditional item contracts, and removal of 2D hazmat PNGs.
 * (Client-only render classes like PoseStack/RenderLayer are avoided because gameTestServer runs
 * on DEDICATED_SERVER.)
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class Armor3dRenderGameTest {

    private Armor3dRenderGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) {
            throw new GameTestAssertException(msg);
        }
    }

    private static InputStream openResource(String classpath) {
        return Armor3dRenderGameTest.class.getResourceAsStream(classpath);
    }

    private static List<RegistrySupplier<Item>> all73ArmorSuppliers() {
        return List.of(
                ModItems.T51_HELMET, ModItems.T51_PLATE, ModItems.T51_LEGS, ModItems.T51_BOOTS,
                ModItems.AJR_HELMET, ModItems.AJR_PLATE, ModItems.AJR_LEGS, ModItems.AJR_BOOTS,
                ModItems.AJRO_HELMET, ModItems.AJRO_PLATE, ModItems.AJRO_LEGS, ModItems.AJRO_BOOTS,
                ModItems.BISMUTH_HELMET, ModItems.BISMUTH_PLATE, ModItems.BISMUTH_LEGS, ModItems.BISMUTH_BOOTS,
                ModItems.DNS_HELMET, ModItems.DNS_PLATE, ModItems.DNS_LEGS, ModItems.DNS_BOOTS,
                ModItems.BJ_HELMET, ModItems.BJ_PLATE, ModItems.BJ_PLATE_JETPACK, ModItems.BJ_LEGS, ModItems.BJ_BOOTS,
                ModItems.STEAMSUIT_HELMET, ModItems.STEAMSUIT_PLATE, ModItems.STEAMSUIT_LEGS, ModItems.STEAMSUIT_BOOTS,
                ModItems.DIESELSUIT_HELMET, ModItems.DIESELSUIT_PLATE, ModItems.DIESELSUIT_LEGS, ModItems.DIESELSUIT_BOOTS,
                ModItems.FAU_HELMET, ModItems.FAU_PLATE, ModItems.FAU_LEGS, ModItems.FAU_BOOTS,
                ModItems.ENVSUIT_HELMET, ModItems.ENVSUIT_PLATE, ModItems.ENVSUIT_LEGS, ModItems.ENVSUIT_BOOTS,
                ModItems.HEV_HELMET, ModItems.HEV_PLATE, ModItems.HEV_LEGS, ModItems.HEV_BOOTS,
                ModItems.NCRPA_HELMET, ModItems.NCRPA_PLATE, ModItems.NCRPA_LEGS, ModItems.NCRPA_BOOTS,
                ModItems.RPA_HELMET, ModItems.RPA_PLATE, ModItems.RPA_LEGS, ModItems.RPA_BOOTS,
                ModItems.TAURUN_HELMET, ModItems.TAURUN_PLATE, ModItems.TAURUN_LEGS, ModItems.TAURUN_BOOTS,
                ModItems.TRENCHMASTER_HELMET, ModItems.TRENCHMASTER_PLATE, ModItems.TRENCHMASTER_LEGS, ModItems.TRENCHMASTER_BOOTS,
                ModItems.HAZMAT_HELMET, ModItems.HAZMAT_PLATE, ModItems.HAZMAT_LEGS, ModItems.HAZMAT_BOOTS,
                ModItems.HAZMAT_HELMET_RED, ModItems.HAZMAT_PLATE_RED, ModItems.HAZMAT_LEGS_RED, ModItems.HAZMAT_BOOTS_RED,
                ModItems.HAZMAT_HELMET_GREY, ModItems.HAZMAT_PLATE_GREY, ModItems.HAZMAT_LEGS_GREY, ModItems.HAZMAT_BOOTS_GREY
        );
    }

    @GameTest(template = "empty3x3x3", batch = "armor_render", timeoutTicks = 100)
    public static void all73ArmorItemsHaveValidNMeshTexturesAndBuiltinEntityModels(GameTestHelper helper) {
        List<RegistrySupplier<Item>> suppliers = all73ArmorSuppliers();
        check(suppliers.size() == 73, "Expected 73 3D armor items across 18 sets, got " + suppliers.size());

        // 1. Check all 73 registered ArmorItems and their generated builtin/entity JSON models
        for (RegistrySupplier<Item> sup : suppliers) {
            Item item = sup.get();
            check(item instanceof ArmorItem, "Expected ArmorItem for " + sup.getId());
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String modelClasspath = "/assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json";
            try (InputStream jsonIn = openResource(modelClasspath)) {
                check(jsonIn != null, "Missing generated item model JSON: " + modelClasspath);
                String text = new String(jsonIn.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
                check(obj.has("parent") && "minecraft:builtin/entity".equals(obj.get("parent").getAsString()),
                        "Expected parent minecraft:builtin/entity in " + modelClasspath + ", got " + obj);
                check(!obj.has("display"),
                        "Generated 3D armor model must omit display block: " + modelClasspath);
            } catch (GameTestAssertException g) {
                throw g;
            } catch (Exception e) {
                throw new GameTestAssertException("Failed reading item model " + modelClasspath + ": " + e.getMessage());
            }
        }

        // 2. Check all 15 compiled .nmesh models & every expected submesh part
        Map<String, Set<String>> expectedParts = new HashMap<>();
        expectedParts.put("t51", Set.of("Helmet", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("ajr_a", Set.of("Helmet", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("bismuth", Set.of("Helmet", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("dnt", Set.of("Helmet", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("bj", Set.of("Head", "Body", "Jetpack", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftFoot", "RightFoot"));
        expectedParts.put("steamsuit", Set.of("Head", "Body", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("bnuuy", Set.of("Head", "Body", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("fau", Set.of("Head", "Body", "Cassette", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("envsuit", Set.of("Helmet", "Lamps", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftFoot", "RightFoot"));
        expectedParts.put("hev", Set.of("Head", "Body", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftFoot", "RightFoot"));
        expectedParts.put("ncrpa", Set.of("Helmet", "Eyes", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("remnant", Set.of("Head", "Body", "Glow", "Fan", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("taurun", Set.of("Helmet", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("trenchmaster", Set.of("Helmet", "Light", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));
        expectedParts.put("hazmat", Set.of("Helmet", "Filter", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"));

        for (var entry : expectedParts.entrySet()) {
            String modelName = entry.getKey();
            String nmeshPath = "/assets/hbm_m/models/armor/" + modelName + ".nmesh";
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("hbm_m", "models/armor/" + modelName + ".obj");
            try (InputStream in = openResource(nmeshPath)) {
                check(in != null, "Missing compiled .nmesh resource: " + nmeshPath);
                NMeshData data = NMeshData.read(loc, in);
                Map<String, Integer> partTris = new HashMap<>();
                for (NMeshData.SubMesh sm : data.subMeshes()) {
                    String key = (sm.objectName != null && !sm.objectName.isEmpty())
                            ? sm.objectName
                            : ((sm.groupName != null && !sm.groupName.isEmpty()) ? sm.groupName : "Default");
                    partTris.merge(key, sm.indexCount() / 3, Integer::sum);
                }
                for (String req : entry.getValue()) {
                    check(partTris.getOrDefault(req, 0) > 0,
                            "Submesh '" + req + "' missing or empty in " + nmeshPath + " (have=" + partTris.keySet() + ")");
                }
                if ("hazmat".equals(modelName)) {
                    int totalTris = partTris.values().stream().mapToInt(Integer::intValue).sum();
                    check(totalTris > 0, "Expected non-empty hazmat 3D mesh, got " + totalTris);
                }
            } catch (GameTestAssertException g) {
                throw g;
            } catch (Exception e) {
                throw new GameTestAssertException("Failed reading .nmesh " + nmeshPath + ": " + e.getMessage());
            }
        }

        // 3. Check all 59 armor textures + white.png exist on classpath
        List<String> texNames = new ArrayList<>(List.of("bismuth", "bj_eyepatch", "bj_chest", "bj_jetpack", "bj_arm", "bj_leg"));
        for (String p : List.of("t51", "ajr", "ajro", "dnt", "steamsuit", "bnuuy", "fau", "envsuit", "hev", "ncrpa", "rpa", "taurun", "trenchmaster")) {
            texNames.add(p + "_helmet");
            texNames.add(p + "_chest");
            texNames.add(p + "_arm");
            texNames.add(p + "_leg");
        }
        texNames.add("fau_cassette");
        for (String tex : texNames) {
            String cp = "/assets/hbm_m/textures/armor/" + tex + ".png";
            try (InputStream in = openResource(cp)) {
                check(in != null, "Missing armor texture: " + cp);
            } catch (GameTestAssertException g) {
                throw g;
            } catch (Exception e) {
                throw new GameTestAssertException("Error opening " + cp + ": " + e.getMessage());
            }
        }
        for (String hazTex : List.of("hazmat", "hazmat_red", "hazmat_grey")) {
            String cp = "/assets/hbm_m/textures/armor/" + hazTex + ".png";
            try (InputStream in = openResource(cp)) {
                check(in != null, "Missing hazmat armor texture: " + cp);
            } catch (GameTestAssertException g) {
                throw g;
            } catch (Exception e) {
                throw new GameTestAssertException("Error opening " + cp + ": " + e.getMessage());
            }
        }
        try (InputStream in = openResource("/assets/hbm_m/textures/misc/white.png")) {
            check(in != null, "Missing misc/white.png for Envsuit lamps");
        } catch (GameTestAssertException g) {
            throw g;
        } catch (Exception e) {
            throw new GameTestAssertException("Error checking white.png: " + e.getMessage());
        }

        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "armor_render", timeoutTicks = 100)
    public static void conditionalPartsAnd2dHazmatRemoval(GameTestHelper helper) {
        // 1. Legacy 2D hazmat layer PNGs must be gone
        for (String deadPath : List.of(
                "/assets/hbm_m/textures/models/armor/hazmat_layer_1.png",
                "/assets/hbm_m/textures/models/armor/hazmat_layer_2.png",
                "/assets/hbm_m/textures/models/armor/hazmat_red_layer_1.png",
                "/assets/hbm_m/textures/models/armor/hazmat_red_layer_2.png",
                "/assets/hbm_m/textures/models/armor/hazmat_grey_layer_1.png",
                "/assets/hbm_m/textures/models/armor/hazmat_grey_layer_2.png",
                "/assets/hbm_m/textures/armor/hazmat_1.png",
                "/assets/hbm_m/textures/armor/hazmat_2.png",
                "/assets/hbm_m/textures/armor/hazmat_1_red.png",
                "/assets/hbm_m/textures/armor/hazmat_2_red.png",
                "/assets/hbm_m/textures/armor/hazmat_1_grey.png",
                "/assets/hbm_m/textures/armor/hazmat_2_grey.png")) {
            try (InputStream in = openResource(deadPath)) {
                check(in == null, "Legacy 2D hazmat texture still packaged on classpath: " + deadPath);
            } catch (GameTestAssertException g) {
                throw g;
            } catch (Exception e) {
                throw new GameTestAssertException("Failed checking deleted path " + deadPath + ": " + e.getMessage());
            }
        }

        // 2. Hazmat helmet gas-mask Filter state toggles cleanly
        ItemStack helmet = new ItemStack(ModItems.HAZMAT_HELMET.get());
        check(!IGasMask.hasFilter(helmet), "New hazmat helmet should start without a filter");
        IGasMask.installFilter(helmet, new ItemStack(ModItems.GAS_MASK_FILTER.get()));
        check(IGasMask.hasFilter(helmet), "Hazmat helmet must report hasFilter=true after installFilter");
        IGasMask.removeFilter(helmet);
        check(!IGasMask.hasFilter(helmet), "Hazmat helmet must report hasFilter=false after removeFilter");

        // 3. BJ Plate vs BJ Plate Jetpack item identity check
        check(!(ModItems.BJ_PLATE.get() instanceof ArmorBJJetpack), "BJ_PLATE must not be ArmorBJJetpack");
        check(ModItems.BJ_PLATE_JETPACK.get() instanceof ArmorBJJetpack, "BJ_PLATE_JETPACK must be ArmorBJJetpack");

        // 4. Equip on ArmorStand and Zombie cleanly
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 1));
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HAZMAT_HELMET_RED.get()));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HAZMAT_PLATE_RED.get()));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.HAZMAT_LEGS_RED.get()));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.HAZMAT_BOOTS_RED.get()));
        check(stand.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.HAZMAT_HELMET_RED.get()), "ArmorStand head equip failed");

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 1));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.T51_HELMET.get()));
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.RPA_PLATE.get()));
        check(zombie.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.RPA_PLATE.get()), "Zombie chest equip failed");

        helper.succeed();
    }
}
