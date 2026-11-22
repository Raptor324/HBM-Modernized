package com.hbm_m.test;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Regressionstest Creative-Tabs (Nutzer-Meldung 2026-10-13, Absturz beim Oeffnen des Inventars auf NeoForge:
 * "Itemstack 0 minecraft:air does not exist in tab's list").
 *
 * <p>Baut serverseitig <b>alle</b> Tabs so auf, wie es der Client beim Oeffnen des Inventars tut
 * ({@code CreativeModeTabs.tryRebuildTabContents}, mit und ohne OP-Rechte). Dabei laufen alle
 * {@code BuildCreativeModeTabContentsEvent}-Listener aller geladenen Mods (HBM allein bzw. mit Addons).
 * Prueft: kein Fehler, kein leerer Stapel, jeder {@code hbm_m}-Tab hat Inhalt.
 */
@GameTestHolder(RefStrings.MODID)
@PrefixGameTestTemplate(false)
public final class CreativeTabsGameTest {

    private CreativeTabsGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** Baut alle Tabs neu (mehrfach, damit beide Rechte-Varianten sicher einen vollen Aufbau ausloesen). */
    public static void rebuildAllTabs(ServerLevel level) {
        for (boolean ops : new boolean[] { true, false, true }) {
            try {
                CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), ops, level.registryAccess());
            } catch (RuntimeException e) {
                StringBuilder msg = new StringBuilder("creative tab build failed (ops=" + ops + "): ");
                for (Throwable t = e; t != null; t = t.getCause()) {
                    msg.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append(" | ");
                    if (t.getCause() == t) break;
                }
                com.mojang.logging.LogUtils.getLogger().error("[CreativeTabsGameTest] {}", msg, e);
                throw new GameTestAssertException(msg.toString());
            }
        }
    }

    @GameTest(template = "empty3x3x3", batch = "creative_tabs", timeoutTicks = 40)
    public static void allCreativeTabsBuildWithoutError(GameTestHelper helper) {
        rebuildAllTabs(helper.getLevel());
        List<String> empty = new ArrayList<>();
        int tabs = 0, stacks = 0;
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) continue;
            tabs++;
            int n = 0;
            for (ItemStack st : tab.getDisplayItems()) {
                n++;
                if (st.isEmpty()) empty.add(String.valueOf(id));
            }
            stacks += n;
            if (RefStrings.MODID.equals(id.getNamespace())) check(n > 0, "hbm_m tab empty: " + id);
        }
        check(empty.isEmpty(), "empty stacks in tabs: " + empty);
        com.mojang.logging.LogUtils.getLogger().info("[CreativeTabsGameTest] {} tabs, {} stacks built", tabs, stacks);
        helper.succeed();
    }
}
