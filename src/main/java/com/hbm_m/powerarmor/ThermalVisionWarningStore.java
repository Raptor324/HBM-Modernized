package com.hbm_m.powerarmor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

/**
 * Stores per-world thermal vision warning acknowledgement (client-side).
 * Prevents enabling thermal vision on first use in a given world.
 */
public final class ThermalVisionWarningStore {
    private static final String STORAGE_FILE = "hbm_m_thermal_warned.txt";
    private static final Set<String> WARNED_WORLDS = new HashSet<>();
    private static boolean loaded = false;

    private ThermalVisionWarningStore() {}

    public static boolean shouldBlockFirstActivation(Minecraft mc) {
        if (mc == null || mc.level == null || mc.player == null) {
            return false;
        }

        ensureLoaded(mc);
        String worldKey = getWorldKey(mc);
        if (WARNED_WORLDS.contains(worldKey)) {
            return false;
        }

        WARNED_WORLDS.add(worldKey);
        save(mc);

        mc.player.displayClientMessage(Component.translatable(
            "hud.hbm_m.thermal.warning"
        ), false);

        return true;
    }

    private static void ensureLoaded(Minecraft mc) {
        if (loaded) {
            return;
        }
        loaded = true;

        Path path = getStoragePath(mc);
        if (!Files.exists(path)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    WARNED_WORLDS.add(trimmed);
                }
            }
        } catch (IOException e) {
            MainRegistry.LOGGER.warn("[ThermalVision] Failed to read warning store: {}", e.getMessage());
        }
    }

    private static void save(Minecraft mc) {
        Path path = getStoragePath(mc);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, WARNED_WORLDS, StandardCharsets.UTF_8);
        } catch (IOException e) {
            MainRegistry.LOGGER.warn("[ThermalVision] Failed to save warning store: {}", e.getMessage());
        }
    }

    private static Path getStoragePath(Minecraft mc) {
        return mc.gameDirectory.toPath().resolve("config").resolve(STORAGE_FILE);
    }

    private static String getWorldKey(Minecraft mc) {
        if (mc.getSingleplayerServer() != null) {
            String levelName = mc.getSingleplayerServer().getWorldData().getLevelName();
            return "sp:" + levelName;
        }

        if (mc.getConnection() != null) {
            ServerData data = mc.getConnection().getServerData();
            if (data != null && data.ip != null) {
                return "mp:" + data.ip;
            }
        }

        return "unknown:" + mc.level.dimension().location();
    }
}
