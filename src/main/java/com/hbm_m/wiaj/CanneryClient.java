package com.hbm_m.wiaj;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.handler.HbmKeybinds;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.wiaj.cannery.CanneryBase;
import com.hbm_m.wiaj.cannery.Jars;
import com.mojang.blaze3d.platform.InputConstants;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
//? if forge {
import net.minecraftforge.fml.common.Mod;
//?} elif neoforge {
/*import net.neoforged.fml.common.EventBusSubscriber;
*///?}

/**
 * The cannery parts of {@code ModEventHandlerClient} (1.7.10): the tooltip line
 * "[ LSHIFT + F1 for a presentation ]" on items with a demo script, and opening
 * {@link GuiWorldInAJar} when the qmaw key (F1) is held together with shift while
 * hovering a slot in any open container screen.
 * <p>
 * Mirrors {@code com.hbm_m.qmaw.QMAWClient} (the QMAW half of the same 1.7.10 handler);
 * the reflection-based {@code getMouseOverStack} is replaced by
 * {@link AbstractContainerScreen#getSlotUnderMouse()}.
 */
//? if forge {
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?} elif neoforge {
/*@EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT)
*///?}
public final class CanneryClient {

    private CanneryClient() { }

    //? if forge {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        init();
    }
    //?} elif neoforge {
    /*@net.neoforged.bus.api.SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        init();
    }
    *///?}

    public static void init() {
        Jars.initJars();

        //? if < 1.21.1 {
        ClientTooltipEvent.ITEM.register((stack, lines, flag) -> onTooltip(stack, lines));
        //?} else {
        /*ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> onTooltip(stack, lines));
        *///?}
        ClientTickEvent.CLIENT_POST.register(mc -> onClientTick());
    }

    /** Original tooltip hook, cannery section (original passed the pre-combined "LSHIFT + <key>" string). */
    private static void onTooltip(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty()) return;
        if (Jars.canneries.containsKey(stack.getItem())) {
            lines.add(Component.translatable("cannery.f1", "LSHIFT + " + HbmKeybinds.qmaw.getTranslatedKeyMessage().getString())
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    /** Original {@code clientTick}, cannery section. */
    private static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> container) || mc.player == null) return;

        long window = mc.getWindow().getWindow();
        InputConstants.Key key = HbmKeybinds.qmaw.getKey();
        if (key.getType() != InputConstants.Type.KEYSYM || key.getValue() == InputConstants.UNKNOWN.getValue()) return;

        if (InputConstants.isKeyDown(window, key.getValue()) && net.minecraft.client.gui.screens.Screen.hasShiftDown()) {

            Slot slot = container.getSlotUnderMouse();
            if (slot == null || !slot.hasItem()) return;

            CanneryBase cannery = Jars.canneries.get(slot.getItem().getItem());
            if (cannery != null) {
                mc.setScreen(new GuiWorldInAJar(cannery.createScript(), I18n.get(cannery.getName()), cannery.getIcon(), cannery.seeAlso()));
            }
        }
    }
}
