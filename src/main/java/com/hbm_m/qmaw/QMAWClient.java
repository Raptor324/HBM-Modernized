package com.hbm_m.qmaw;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.hbm_m.handler.HbmKeybinds;
import com.mojang.blaze3d.platform.InputConstants;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.hbm_m.main.MainRegistry;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.hbm_m.main.MainRegistry;
*///?}

/**
 * Die QMAW-Teile aus {@code ModEventHandlerClient} (1.7.10): Tooltipzeile "[ F1 fuer Hilfe ]" bei
 * Gegenstaenden mit Handbucheintrag und das Oeffnen von {@link GuiQMAW}, wenn die QMAW-Taste ohne
 * Shift in einem offenen Bildschirm gedrueckt wird. Dazu das Anmelden des {@link QMAWLoader}
 * (Original: {@code ClientProxy}, {@code registerReloadListener}).
 */
//? if forge {
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?} elif neoforge {
/*@EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?}
public final class QMAWClient {

    private QMAWClient() { }

    private static long qmawTimestamp;
    private static QuickManualAndWiki lastQMAW = null;

    //? if forge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        init();
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new QMAWLoader());
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        init();
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new QMAWLoader());
    }
    *///?}

    public static void init() {
        //? if < 1.21.1 {
        ClientTooltipEvent.ITEM.register(QMAWClient::onTooltip);
        //?} else {
        /*ClientTooltipEvent.ITEM.register((stack, tooltip, context, flag) -> onTooltip(stack, tooltip, flag));
        *///?}
        ClientTickEvent.CLIENT_POST.register(mc -> onClientTick());
    }

    /** Original {@code drawTooltip}, QMAW-Abschnitt. */
    private static void onTooltip(ItemStack stack, List<Component> list, TooltipFlag flag) {
        if (stack.isEmpty()) return;
        try {
            QuickManualAndWiki qmaw = QMAWLoader.triggers.get(stack.getItem());
            if (qmaw != null) {
                list.add(Component.translatable("qmaw.tab", HbmKeybinds.qmaw.getTranslatedKeyMessage().getString())
                        .withStyle(ChatFormatting.YELLOW));
                lastQMAW = qmaw;
                qmawTimestamp = System.currentTimeMillis();
            }
        } catch (Exception ex) {
            list.add(Component.literal("Error loading cannery: " + ex.getLocalizedMessage()).withStyle(ChatFormatting.RED));
        }
    }

    /** Original {@code clientTick}, QMAW-Abschnitt. */
    private static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null || mc.player == null) return;

        long window = mc.getWindow().getWindow();
        InputConstants.Key key = HbmKeybinds.qmaw.getKey();
        if (key.getType() != InputConstants.Type.KEYSYM || key.getValue() == InputConstants.UNKNOWN.getValue()) return;

        if (InputConstants.isKeyDown(window, key.getValue()) && !InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)) {

            QuickManualAndWiki qmaw = qmawTimestamp > System.currentTimeMillis() - 100 ? lastQMAW : null;

            if (qmaw != null) {
                mc.player.closeContainer();
                mc.setScreen(new GuiQMAW(qmaw));
            }
        }
    }
}
