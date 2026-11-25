package com.hbm_m.client.dev;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm_m.handler.HbmKeybinds;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Dev helper (client only): copies display name + registry ID of items to the OS clipboard so
 * they can be pasted into bug reports, and dumps missing block/item translations on demand.
 * Actions in the {@code hbm.key} category, rebindable in the vanilla controls screen:
 * <ul>
 *   <li>{@code devCopyHotbar} — one line per non-empty hotbar slot: {@code Display Name | hbm_m:id}
 *       (works both in-world and inside container/inventory screens)</li>
 *   <li>{@code devCopyTarget} — the item under the cursor in container screens, or the block/entity
 *       currently under the crosshair in-world</li>
 *   <li>{@code devDumpMissingLang} — dumps blocks/items with no translation or only English fallback
 *       to {@code missing_translations_<lang>.txt} and the clipboard</li>
 * </ul>
 * Ticked from {@link HbmKeybinds#onClientTick()}.
 */
public final class ItemCopyDevHook {

    private static boolean prevHotbarDown;
    private static boolean prevTargetDown;
    private static boolean prevLangDumpDown;

    private ItemCopyDevHook() {}

    public static void onClientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            prevHotbarDown = false;
            prevTargetDown = false;
            prevLangDumpDown = false;
            return;
        }
        prevHotbarDown = handleClick(mc, HbmKeybinds.devCopyHotbar, prevHotbarDown, ItemCopyDevHook::copyHotbar);
        prevTargetDown = handleClick(mc, HbmKeybinds.devCopyTarget, prevTargetDown, ItemCopyDevHook::copyTarget);
        prevLangDumpDown = handleDumpClick(mc, HbmKeybinds.devDumpMissingLang, prevLangDumpDown);
    }

    private interface CopyAction {
        /** @return the copied text, or null if there was nothing to copy (already messaged). */
        String run(Minecraft mc);
    }

    private static boolean handleClick(Minecraft mc, KeyMapping key, boolean prevDown, CopyAction action) {
        boolean triggered;
        boolean nextDown;
        if (mc.screen == null) {
            triggered = key.consumeClick();
            nextDown = false;
        } else {
            while (key.consumeClick()) {}
            nextDown = isBoundKeyDown(mc, key);
            triggered = nextDown && !prevDown && !isTypingInTextBox(mc.screen);
        }
        if (!triggered) return nextDown;

        String text = action.run(mc);
        if (text != null) {
            mc.keyboardHandler.setClipboard(text);
            int lines = text.split("\n").length;
            mc.player.displayClientMessage(
                    Component.translatable("hbm.dev.copy_done", lines).withStyle(ChatFormatting.GREEN), false);
        }
        return nextDown;
    }

    private static boolean handleDumpClick(Minecraft mc, KeyMapping key, boolean prevDown) {
        boolean triggered;
        boolean nextDown;
        if (mc.screen == null) {
            triggered = key.consumeClick();
            nextDown = false;
        } else {
            while (key.consumeClick()) {}
            nextDown = isBoundKeyDown(mc, key);
            triggered = nextDown && !prevDown && !isTypingInTextBox(mc.screen);
        }
        if (!triggered) return nextDown;

        dumpMissingTranslations(mc);
        return nextDown;
    }

    private static void dumpMissingTranslations(Minecraft mc) {
        String langCode = mc.getLanguageManager().getSelected();
        Set<String> selectedLangKeys = collectLangKeys(mc, langCode);
        Language lang = Language.getInstance();

        Map<String, String> keyToId = new LinkedHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            keyToId.putIfAbsent(block.getDescriptionId(), id.toString());
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            keyToId.putIfAbsent(item.getDescriptionId(), id.toString());
        }

        List<String> noTranslation = new ArrayList<>();
        List<String> fallbackOnly = new ArrayList<>();
        boolean checkSelected = !"en_us".equals(langCode);

        for (Map.Entry<String, String> entry : keyToId.entrySet()) {
            String key = entry.getKey();
            String id = entry.getValue();
            if (!lang.has(key)) {
                noTranslation.add(key + " | " + id);
            } else if (checkSelected && !selectedLangKeys.contains(key)) {
                fallbackOnly.add(key + " | " + id + " | " + lang.getOrDefault(key));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== NO TRANSLATION AT ALL (").append(noTranslation.size()).append(") ===\n");
        for (String line : noTranslation) {
            sb.append(line).append('\n');
        }
        sb.append('\n');
        sb.append("=== MISSING IN ").append(langCode).append(" (FALLBACK TO ENGLISH) (")
                .append(fallbackOnly.size()).append(") ===\n");
        for (String line : fallbackOnly) {
            sb.append(line).append('\n');
        }

        String report = sb.toString();
        mc.keyboardHandler.setClipboard(report);

        Path out = mc.gameDirectory.toPath().resolve("missing_translations_" + langCode + ".txt");
        try {
            Files.writeString(out, report, StandardCharsets.UTF_8);
        } catch (Exception e) {
            MainRegistry.LOGGER.warn("[ItemCopyDevHook] Failed to write {}", out, e);
        }

        mc.player.displayClientMessage(
                Component.translatable("hbm.dev.lang_dump_done",
                        noTranslation.size(), fallbackOnly.size(), out.getFileName().toString())
                        .withStyle(ChatFormatting.GREEN),
                false);
    }

    private static Set<String> collectLangKeys(Minecraft mc, String langCode) {
        Set<String> keys = new HashSet<>();
        var rm = mc.getResourceManager();
        for (String ns : rm.getNamespaces()) {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(ns, "lang/" + langCode + ".json");
            for (Resource res : rm.getResourceStack(loc)) {
                try (var reader = new InputStreamReader(res.open(), StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed != null && parsed.isJsonObject()) {
                        JsonObject obj = parsed.getAsJsonObject();
                        keys.addAll(obj.keySet());
                    }
                } catch (Exception e) {
                    MainRegistry.LOGGER.warn("[ItemCopyDevHook] Failed to parse lang resource {}", loc, e);
                }
            }
        }
        return keys;
    }

    private static boolean isBoundKeyDown(Minecraft mc, KeyMapping key) {
        InputConstants.Key bound = key.getKey();
        if (bound.getValue() == InputConstants.UNKNOWN.getValue()) return false;
        long window = mc.getWindow().getWindow();
        return switch (bound.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, bound.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
            default -> false;
        };
    }

    private static boolean isTypingInTextBox(Screen screen) {
        return screen.getFocused() instanceof EditBox box && box.canConsumeInput();
    }

    private static String copyHotbar(Minecraft mc) {
        StringBuilder sb = new StringBuilder();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().items.get(slot);
            if (stack.isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(describeStack(stack));
        }
        if (sb.length() == 0) {
            mc.player.displayClientMessage(Component.translatable("hbm.dev.hotbar_empty"), false);
            return null;
        }
        return sb.toString();
    }

    private static String copyTarget(Minecraft mc) {
        if (mc.screen instanceof AbstractContainerScreen<?> container) {
            Slot slot = container.getSlotUnderMouse();
            if (slot == null || !slot.hasItem()) {
                mc.player.displayClientMessage(Component.translatable("hbm.dev.no_target"), false);
                return null;
            }
            return describeStack(slot.getItem());
        }
        if (mc.screen != null) return null;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            mc.player.displayClientMessage(Component.translatable("hbm.dev.no_target"), false);
            return null;
        }
        if (hit instanceof BlockHitResult blockHit) {
            var state = mc.level.getBlockState(blockHit.getBlockPos());
            return describe(state.getBlock().getName().getString(), BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        }
        if (hit instanceof EntityHitResult entityHit) {
            var entity = entityHit.getEntity();
            return describe(entity.getName().getString(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        }
        return null;
    }

    private static String describeStack(ItemStack stack) {
        String name = stack.getCount() > 1
                ? stack.getHoverName().getString() + " x" + stack.getCount()
                : stack.getHoverName().getString();
        return describe(name, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static String describe(String name, net.minecraft.resources.ResourceLocation id) {
        return name + " | " + id;
    }
}
