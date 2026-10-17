// neo-pendant: keins - reines Entwickler-Werkzeug fuer den 1.20.1-Forge-Dev-Client
//? if forge {
package com.hbm_m.client.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.ItemFluidTank;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.FluidIdentifierControlPacket;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Entwickler-Werkzeug (nur mit {@code -Dhbm.barrelTest=true} bzw. {@code HBM_BARREL_TEST=true}, sonst ohne Wirkung):
 * Fass/Tank fuellen ueber den echten Spielerweg. Superflat-Kreativwelt; je Fall wird der Block per Rechtsklick
 * gesetzt, der Fluid-Identifier wie im Identifier-GUI per {@link FluidIdentifierControlPacket} auf Wasser gestellt,
 * die Fass-GUI per Rechtsklick geoeffnet und Identifier, zwei volle Fluessigkeitstanks (Kreativ-Tab-Stack) und ein
 * Wassereimer per Slot-Klick-Paket (PICKUP / QUICK_MOVE) eingelegt. Variante "schleichen": Typ per Schleich-Rechtsklick
 * mit dem Identifier. Ergebnis je Fall im Log ({@code [HBM BarrelTest]}), danach beendet sich das Spiel.
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BarrelFillClientTestHook {

    private static final boolean ENABLED = Boolean.getBoolean("hbm.barrelTest")
            || "true".equalsIgnoreCase(System.getenv("HBM_BARREL_TEST"));

    /** Menue-Slotindex des Hotbar-Slots n (6 Maschinenslots + 27 Inventar). */
    private static int hotbar(int n) { return 6 + 27 + n; }

    private record Case(String name, Supplier<Block> block, int distance, boolean sneakIdentifier) {}

    private static final List<Case> CASES = List.of(
            new Case("barrel_iron", ModBlocks.BARREL_IRON, 3, false),
            new Case("barrel_steel", ModBlocks.BARREL_STEEL, 3, false),
            new Case("barrel_plastic", ModBlocks.BARREL_PLASTIC, 3, false),
            new Case("barrel_tcalloy", ModBlocks.BARREL_TCALLOY, 3, false),
            new Case("barrel_antimatter", ModBlocks.BARREL_ANTIMATTER, 3, false),
            new Case("fluid_tank", ModBlocks.FLUID_TANK, 5, false),
            new Case("barrel_steel/schleichen", ModBlocks.BARREL_STEEL, 3, true),
            new Case("barrel_plastic/schleichen", ModBlocks.BARREL_PLASTIC, 3, true));

    private static int phase;   // 0 Titel, 1 Welt laedt, 2 Faelle, 3 fertig
    private static int titleTicks;
    private static int worldTicks;
    private static int index;
    private static int step;
    private static int pass;
    private static int fail;
    private static BlockPos origin;
    private static BlockPos target;
    private static BlockPos core;
    /** Block, den der Spieler zum Oeffnen anklickt: der vorderste Block der Maschine in Blickrichtung (bei Mehrblock ein Teilblock). */
    private static BlockPos front;
    private static String note = "";

    private BarrelFillClientTestHook() { }

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
            MainRegistry.LOGGER.error("[HBM BarrelTest] abgebrochen", t);
            phase = 3;
            mc.stop();
        }
    }

    private static void startWorld(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        Path saves = mc.gameDirectory.toPath().resolve("saves");
        if (Files.isDirectory(saves)) {
            try (Stream<Path> s = Files.list(saves)) {
                s.filter(p -> p.getFileName().toString().startsWith("hbm_barreltest")).forEach(BarrelFillClientTestHook::deleteTree);
            } catch (IOException ignored) { }
        }
        String level = "hbm_barreltest_" + System.currentTimeMillis();
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(level, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                WorldDataConfiguration.DEFAULT);
        MainRegistry.LOGGER.info("[HBM BarrelTest] lege Testwelt {} an", level);
        mc.createWorldOpenFlows().createFreshLevel(level, settings, new WorldOptions(0L, false, false),
                ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    private static void deleteTree(Path p) {
        try (Stream<Path> s = Files.walk(p)) {
            s.sorted(Comparator.reverseOrder()).forEach(q -> { try { Files.delete(q); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }

    /** Erster Stack aus den Kreativ-Tabs, der {@code match} erfuellt (genau das, was der Spieler dort herausnimmt). */
    private static ItemStack fromCreative(Minecraft mc, java.util.function.Predicate<ItemStack> match, ItemStack fallback) {
        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.REGISTRY.allFlags(), true, mc.level.registryAccess());
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            try { tab.buildContents(params); } catch (Throwable t) { continue; }
            for (ItemStack s : tab.getDisplayItems()) if (match.test(s)) return s.copy();
        }
        MainRegistry.LOGGER.warn("[HBM BarrelTest] {} in keinem Kreativ-Tab, nehme Ersatz", fallback);
        return fallback.copy();
    }

    private static Fluid water() { return ModFluids.WATER.getSource(); }

    private static void giveCreative(Minecraft mc, int hotbarSlot, ItemStack stack) {
        // wie CreativeModeInventoryScreen: Client-Slot setzen + ServerboundSetCreativeModeSlotPacket
        mc.player.getInventory().setItem(hotbarSlot, stack.copy());
        mc.gameMode.handleCreativeModeItemAdd(stack.copy(), 36 + hotbarSlot);
    }

    private static MachineFluidTankBlockEntity serverTank(MinecraftServer server) {
        if (core == null) return null;
        return server.submit(() -> server.overworld().getBlockEntity(core) instanceof MachineFluidTankBlockEntity t ? t : null).join();
    }

    private static void click(Minecraft mc, int slot, ClickType type) {
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 0, type, mc.player);
    }

    private static void runCase(Minecraft mc) {
        if (index >= CASES.size()) {
            MainRegistry.LOGGER.info("[HBM BarrelTest] FERTIG: {} ok, {} fehlgeschlagen", pass, fail);
            phase = 3;
            mc.stop();
            return;
        }
        Case c = CASES.get(index);
        MinecraftServer server = mc.getSingleplayerServer();
        ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        ServerLevel lvl = server.overworld();
        step++;

        switch (step) {
            case 1 -> {
                if (mc.screen != null) mc.player.closeContainer();
                note = "";
                core = null;
                BlockPos feet = server.submit(sp::blockPosition).join();
                origin = new BlockPos(index * 12, feet.getY(), 0);
                target = origin.relative(Direction.SOUTH, c.distance());
                server.submit(() -> {
                    sp.setGameMode(GameType.CREATIVE);
                    sp.getInventory().clearContent();
                    for (BlockPos p : BlockPos.betweenClosed(origin.offset(-5, 0, -2), origin.offset(5, 6, 12))) {
                        lvl.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    for (ItemEntity e : lvl.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(16))) e.discard();
                    sp.connection.teleport(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F, 35F);
                }).join();
            }
            case 15 -> {
                mc.player.getInventory().selected = 0;
                giveCreative(mc, 0, fromCreative(mc, s -> s.is(c.block().get().asItem()), new ItemStack(c.block().get().asItem())));
                ItemStack id = fromCreative(mc, s -> s.is(ModItems.FLUID_IDENTIFIER.get()), new ItemStack(ModItems.FLUID_IDENTIFIER.get()));
                giveCreative(mc, 1, id);
                ItemStack tanks = fromCreative(mc, s -> s.is(ModItems.FLUID_TANK_FULL.get()) && ItemFluidTank.getFluid(s) == water(),
                        ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), water(), 1));
                tanks.setCount(2);
                giveCreative(mc, 2, tanks);
                giveCreative(mc, 3, new ItemStack(Items.WATER_BUCKET));
            }
            case 25 -> {
                // Block setzen: Rechtsklick auf die Oberseite des Bodens
                BlockPos ground = target.below();
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            }
            case 40 -> {
                core = server.submit(() -> {
                    for (BlockPos p : BlockPos.betweenClosed(origin.offset(-5, 0, -2), origin.offset(5, 6, 12))) {
                        if (lvl.getBlockEntity(p) instanceof MachineFluidTankBlockEntity) return p.immutable();
                    }
                    return null;
                }).join();
                if (core == null) { note = "Block nicht gesetzt (" + lvl.getBlockState(target) + ")"; step = 1000; return; }
                front = server.submit(() -> {
                    for (int dz = 1; dz <= 12; dz++) {
                        BlockPos p = origin.relative(Direction.SOUTH, dz);
                        if (!lvl.getBlockState(p).isAir()) return p;
                    }
                    return core;
                }).join();
                note += "kern=" + core.subtract(origin).toShortString() + " klick=" + front.subtract(origin).toShortString() + " ";
                mc.player.getInventory().selected = 1; // Identifier in die Hand (MultiPlayerGameMode.tick sendet den Slotwechsel)
            }
            case 45 -> {
                // Identifier-GUI: Linksklick auf "Wasser" schickt genau dieses Paket
                FluidIdentifierControlPacket.send(HbmFluidRegistry.getFluidName(water()), null);
            }
            case 55 -> {
                ItemStack held = sp.getMainHandItem();
                if (!(held.getItem() instanceof FluidIdentifierItem) || FluidIdentifierItem.getType(held, true) != water()) {
                    note = "Identifier serverseitig nicht Wasser: " + held + " " + held.getTag();
                    step = 1000; return;
                }
                if (c.sneakIdentifier()) KeyMapping.set(mc.options.keyShift.getKey(), true);
            }
            case 60 -> {
                if (c.sneakIdentifier()) {
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(front).add(0, 0, -0.5), Direction.NORTH, front, false);
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                }
            }
            case 66 -> {
                KeyMapping.set(mc.options.keyShift.getKey(), false);
                if (c.sneakIdentifier()) {
                    MachineFluidTankBlockEntity t = serverTank(server);
                    note += "nachSchleichklick typ=" + (t == null ? "?" : key(t.getFluidTank().getTankType())) + " ";
                }
            }
            case 72 -> {
                // GUI oeffnen: normaler Rechtsklick (Identifier in der Hand, nicht geschlichen)
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(front).add(0, 0, -0.5), Direction.NORTH, front, false);
                note += "oeffnen=" + mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit) + " ";
            }
            case 85 -> {
                if (mc.player.containerMenu == mc.player.inventoryMenu) { note += "GUI nicht offen (screen=" + mc.screen + ")"; step = 1000; return; }
                note += "menu=" + mc.player.containerMenu.getClass().getSimpleName() + " ";
                if (!c.sneakIdentifier()) {
                    click(mc, hotbar(1), ClickType.PICKUP);           // Identifier aufnehmen
                    click(mc, MachineFluidTankBlockEntity.SLOT_ID_IN, ClickType.PICKUP); // ablegen
                }
            }
            case 90 -> {
                click(mc, hotbar(2), ClickType.PICKUP);               // 2 volle Wassertanks aufnehmen
                click(mc, MachineFluidTankBlockEntity.SLOT_LOAD_IN, ClickType.PICKUP); // in den Ladeslot
                if (!mc.player.containerMenu.getCarried().isEmpty()) note += "Ladeslot verweigert(" + mc.player.containerMenu.getCarried() + ") ";
            }
            case 100 -> {
                MachineFluidTankBlockEntity t = serverTank(server);
                note += "nachTanks=" + (t == null ? "?" : t.getFluidTank().getFill()) + " ";
                // leere Tanks aus dem Ausgabeslot nehmen (sonst passt der leere Eimer nicht dazu - wie im Original)
                click(mc, MachineFluidTankBlockEntity.SLOT_LOAD_OUT, ClickType.QUICK_MOVE);
            }
            case 103 -> click(mc, hotbar(3), ClickType.QUICK_MOVE); // Wassereimer per Shift-Klick
            case 115 -> step = 999;
            default -> { }
        }

        if (step >= 999) {
            MachineFluidTankBlockEntity t = serverTank(server);
            int fill = t == null ? -1 : t.getFluidTank().getFill();
            Fluid type = t == null ? null : t.getFluidTank().getTankType();
            boolean ok = step == 999 && t != null && type == water() && fill == 3000;
            if (ok) pass++; else fail++;
            String slots = t == null ? "-" : java.util.stream.IntStream.range(0, 6)
                    .mapToObj(i -> t.getItemHandler().getStackInSlot(i).toString()).reduce((a, b) -> a + "|" + b).orElse("");
            MainRegistry.LOGGER.info("[HBM BarrelTest] {} {}: typ={} fuellung={} (soll 3000) slots=[{}] {}",
                    ok ? "OK  " : "FAIL", c.name(), type == null ? "-" : key(type), fill, slots, note);
            if (mc.player.containerMenu != mc.player.inventoryMenu) mc.player.closeContainer();
            index++;
            step = 0;
        }
    }

    private static String key(Fluid f) {
        return String.valueOf(BuiltInRegistries.FLUID.getKey(f));
    }
}
//?}
