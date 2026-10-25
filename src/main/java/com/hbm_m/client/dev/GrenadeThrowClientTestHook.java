// neo-pendant: keins - reines Entwickler-Werkzeug fuer den 1.20.1-Forge-Dev-Client
//? if forge {
package com.hbm_m.client.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.grenade.GrenadeItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Entwickler-Werkzeug (nur mit {@code -Dhbm.grenadeTest=true} bzw. {@code HBM_GRENADE_TEST=true}, sonst ohne Wirkung):
 * prueft den echten Client-Wurfpfad. Legt eine Superflat-Kreativwelt an, holt Granaten-Stacks aus den Kreativ-Tabs,
 * legt sie wie das Kreativ-Inventar ({@code handleCreativeModeItemAdd}) bzw. im Ueberleben serverseitig in Slot 0,
 * haelt die Benutzen-Taste ueber {@link KeyMapping} gedrueckt und laesst sie wieder los. Ergebnis je Fall im Log
 * ({@code [HBM GrenadeTest]}), danach beendet sich das Spiel.
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GrenadeThrowClientTestHook {

    private static final boolean ENABLED = Boolean.getBoolean("hbm.grenadeTest")
            || "true".equalsIgnoreCase(System.getenv("HBM_GRENADE_TEST"));

    private static final int HOLD = 60;

    private record Case(String name, ItemStack stack, GameType mode, float pitch) {}

    private static int phase;   // 0 Titel, 1 Welt laedt, 2 Faelle, 3 fertig
    private static int titleTicks;
    private static int worldTicks;
    private static List<Case> cases;
    private static int index;
    private static int step;
    private static int thrown;
    private static String thrownTypes = "";
    private static int pass;
    private static int fail;
    private static final Set<Integer> before = new HashSet<>();

    private GrenadeThrowClientTestHook() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || phase == 3) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            switch (phase) {
                case 0 -> {
                    if (mc.getOverlay() == null && mc.screen instanceof TitleScreen && ++titleTicks > 20) {
                        startWorld(mc);
                        phase = 1;
                    }
                }
                case 1 -> {
                    if (mc.level != null && mc.player != null && mc.getOverlay() == null) {
                        if (mc.screen != null && !(mc.screen instanceof PauseScreen)) {
                            worldTicks = 0;
                        } else if (++worldTicks > 100) {
                            if (mc.screen != null) mc.setScreen(null);
                            cases = collect(mc);
                            index = 0;
                            step = 0;
                            phase = 2;
                        }
                    }
                }
                case 2 -> runCase(mc);
                default -> { }
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM GrenadeTest] abgebrochen", t);
            phase = 3;
            mc.stop();
        }
    }

    private static void startWorld(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        Path saves = mc.gameDirectory.toPath().resolve("saves");
        if (Files.isDirectory(saves)) {
            try (Stream<Path> s = Files.list(saves)) {
                s.filter(p -> p.getFileName().toString().startsWith("hbm_grenadetest")).forEach(GrenadeThrowClientTestHook::deleteTree);
            } catch (IOException ignored) { }
        }
        String level = "hbm_grenadetest_" + System.currentTimeMillis();
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(level, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                WorldDataConfiguration.DEFAULT);
        MainRegistry.LOGGER.info("[HBM GrenadeTest] lege Testwelt {} an", level);
        mc.createWorldOpenFlows().createFreshLevel(level, settings, new WorldOptions(0L, false, false),
                ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    private static void deleteTree(Path p) {
        try (Stream<Path> s = Files.walk(p)) {
            s.sorted(Comparator.reverseOrder()).forEach(q -> { try { Files.delete(q); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }

    /** je Granaten-Item der erste Stack aus den Kreativ-Tabs (genau die Daten, die der Spieler dort herausnimmt) */
    private static List<Case> collect(Minecraft mc) {
        Set<Item> wanted = new HashSet<>(List.of(GrenadeItems.GRENADE_UNIVERSAL.get(), ModItems.GRENADE.get(), ModItems.GRENADEHE.get(),
                ModItems.GRENADE_IF.get(), ModItems.STICK_DYNAMITE.get(), ModItems.STICK_DYNAMITE_FISHING.get()));
        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.REGISTRY.allFlags(), true, mc.level.registryAccess());
        Map<Item, ItemStack> found = new LinkedHashMap<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            try { tab.buildContents(params); } catch (Throwable t) { continue; }
            for (ItemStack s : tab.getDisplayItems()) {
                if (wanted.contains(s.getItem()) && !found.containsKey(s.getItem())) found.put(s.getItem(), s.copy());
            }
        }
        for (Item i : wanted) if (!found.containsKey(i)) {
            MainRegistry.LOGGER.warn("[HBM GrenadeTest] {} in keinem Kreativ-Tab, nehme new ItemStack", BuiltInRegistries.ITEM.getKey(i));
            found.put(i, new ItemStack(i));
        }
        List<Case> list = new ArrayList<>();
        String only = System.getProperty("hbm.grenadeTestOnly");
        for (ItemStack s : found.values()) {
            if (only != null && !only.isEmpty() && !BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().matches(only)) continue;
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            list.add(new Case(id + "/creative/oben", s, GameType.CREATIVE, -30F));
            list.add(new Case(id + "/creative/boden", s, GameType.CREATIVE, 60F));
            list.add(new Case(id + "/survival/oben", s, GameType.SURVIVAL, -30F));
        }
        MainRegistry.LOGGER.info("[HBM GrenadeTest] {} Faelle", list.size());
        return list;
    }

    private static void runCase(Minecraft mc) {
        if (index >= cases.size()) {
            MainRegistry.LOGGER.info("[HBM GrenadeTest] FERTIG: {} ok, {} fehlgeschlagen", pass, fail);
            phase = 3;
            mc.stop();
            return;
        }
        Case c = cases.get(index);
        MinecraftServer server = mc.getSingleplayerServer();
        ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        KeyMapping use = mc.options.keyUse;
        if (mc.screen != null) mc.setScreen(null);
        step++;

        if (step == 1) {
            KeyMapping.set(use.getKey(), false);
            server.execute(() -> {
                sp.setGameMode(c.mode);
                sp.getInventory().clearContent();
                sp.setYRot(0F);
                sp.setXRot(c.pitch);
                sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), 0F, c.pitch);
            });
        } else if (step == 10) {
            mc.player.getInventory().selected = 0;
            ItemStack copy = c.stack.copy();
            if (c.mode == GameType.CREATIVE) {
                // wie CreativeModeInventoryScreen: Client-Slot setzen + ServerboundSetCreativeModeSlotPacket
                mc.player.getInventory().setItem(0, copy.copy());
                mc.gameMode.handleCreativeModeItemAdd(copy, 36);
            } else {
                ItemStack sv = copy.copy();
                server.execute(() -> { sp.getInventory().selected = 0; sp.getInventory().setItem(0, sv); });
            }
        } else if (step == 30) {
            before.clear();
            ServerLevel lvl = server.overworld();
            server.submit(() -> { for (Entity e : lvl.getAllEntities()) before.add(e.getId()); }).join();
            thrown = 0;
            thrownTypes = "";
            mc.player.setXRot(c.pitch);
            mc.player.setYRot(0F);
            KeyMapping.set(use.getKey(), true);
            KeyMapping.click(use.getKey());
        } else if (step > 30 && step <= 30 + HOLD + 30) {
            if (step == 30 + HOLD) KeyMapping.set(use.getKey(), false);
            ServerLevel lvl = server.overworld();
            server.submit(() -> {
                for (Entity e : lvl.getAllEntities()) {
                    if (before.contains(e.getId()) || e instanceof Player || e instanceof ItemEntity) continue;
                    before.add(e.getId());
                    thrown++;
                    thrownTypes += BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath() + " ";
                    e.discard();
                }
            }).join();
        } else if (step > 30 + HOLD + 30) {
            ItemStack held = sp.getMainHandItem();
            boolean ok = thrown > 0;
            if (ok) pass++; else fail++;
            MainRegistry.LOGGER.info("[HBM GrenadeTest] {} {}: geworfen={} [{}] modusClient={} modusServer={} hand(server)={}x{} nbt={} deployClient={} deployServer={} usingItem={}",
                    ok ? "OK  " : "FAIL", c.name, thrown, thrownTypes.trim(), mc.gameMode.getPlayerMode(), sp.gameMode.getGameModeForPlayer(),
                    held.getCount(), BuiltInRegistries.ITEM.getKey(held.getItem()).getPath(), held.getTag(),
                    HbmPlayerProps.getData(mc.player).grenadeDeployment, HbmPlayerProps.getData(sp).grenadeDeployment, mc.player.isUsingItem());
            index++;
            step = 0;
        }
    }
}
//?}
