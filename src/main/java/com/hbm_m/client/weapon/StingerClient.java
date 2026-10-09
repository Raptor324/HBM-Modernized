package com.hbm_m.client.weapon;

//? if forge {
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.impl.ItemGunStinger;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Clientseite von {@link ItemGunStinger} (Original {@code ItemGunStinger.renderHUD} + {@code RenderScreenOverlay.renderStingerLockon}):
 * Das Fadenkreuz erscheint erst bei voll angelegter Waffe, darunter der Aufschaltbalken. Laeuft vor
 * {@link GunClientEventsForge} und bricht das Fadenkreuz-Overlay ab, damit dort nichts doppelt gezeichnet wird.
 * Die HUD-Bausteine (Hotbar) zeichnet weiterhin {@link GunClientHooks#renderHotbarExtras}.
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class StingerClient {

    private StingerClient() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        Player player = GunClientHooks.clientPlayer();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunStinger gun)) return;

        event.setCanceled(true);
        if (ItemGunBaseNT.aimingProgress < 1F) return;
        GuiGraphics g = event.getGuiGraphics();
        GunHud.renderCustomCrosshairs(g, gun.getConfig(stack, 0).getCrosshair(stack));
        renderStingerLockon(g);
    }

    /** Original {@code RenderScreenOverlay.renderStingerLockon}. */
    public static void renderStingerLockon(GuiGraphics g) {
        int progress = (int) (ItemGunStinger.lockon * 28);
        int w = g.guiWidth(), h = g.guiHeight();
        g.blit(GunHud.MISC, w / 2 - 15, h / 2 + 18, 146, 18, 30, 10);
        g.blit(GunHud.MISC, w / 2 - 14, h / 2 + 19, 147, 29, progress, 8);
    }
}
//?} elif neoforge {
/*import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.impl.ItemGunStinger;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/^* NeoForge-Gegenstueck: Stinger-Fadenkreuz und Aufschaltbalken (gleiches Verhalten wie der Forge-Zweig). ^/
@EventBusSubscriber(modid = RefStrings.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class StingerClient {

    private StingerClient() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOverlayPre(RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.CROSSHAIR.equals(event.getName())) return;
        Player player = GunClientHooks.clientPlayer();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunStinger gun)) return;

        event.setCanceled(true);
        if (ItemGunBaseNT.aimingProgress < 1F) return;
        GuiGraphics g = event.getGuiGraphics();
        GunHud.renderCustomCrosshairs(g, gun.getConfig(stack, 0).getCrosshair(stack));
        renderStingerLockon(g);
    }

    /^* Original {@code RenderScreenOverlay.renderStingerLockon}. ^/
    public static void renderStingerLockon(GuiGraphics g) {
        int progress = (int) (ItemGunStinger.lockon * 28);
        int w = g.guiWidth(), h = g.guiHeight();
        g.blit(GunHud.MISC, w / 2 - 15, h / 2 + 18, 146, 18, 30, 10);
        g.blit(GunHud.MISC, w / 2 - 14, h / 2 + 19, 147, 29, progress, 8);
    }
}
*///?}
