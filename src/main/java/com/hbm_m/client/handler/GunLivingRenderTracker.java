package com.hbm_m.client.handler;

import com.hbm_m.lib.RefStrings;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//? if forge {
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} elif neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
*///?}

/**
 * Merkt sich die LivingEntity, die aktuell gerendert wird. Das BEWLR ({@code GunItemRenderer}) bekommt
 * in 1.20/1.21 keinen Entity-Parameter; ueber Pre/Post von RenderLivingEvent kann der Drittperson-Halter
 * (z.B. andere Spieler) trotzdem aufgeloest werden (Muzzle-Flash pro Entity statt Spiegel des LocalPlayers).
 */
@OnlyIn(Dist.CLIENT)
//? if forge {
@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} elif neoforge {
/*@EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT)
*///?}
public class GunLivingRenderTracker {

    private static LivingEntity current;

    /** Entity im laufenden LivingEntityRenderer- Durchlauf, sonst null. */
    public static LivingEntity current() { return current; }

    //? if forge {
    @SubscribeEvent
    public static void onRenderLivingPre(net.minecraftforge.client.event.RenderLivingEvent.Pre<?, ?> event) { current = event.getEntity(); }
    @SubscribeEvent
    public static void onRenderLivingPost(net.minecraftforge.client.event.RenderLivingEvent.Post<?, ?> event) { current = null; }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onRenderLivingPre(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> event) { current = event.getEntity(); }
    @SubscribeEvent
    public static void onRenderLivingPost(net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?, ?> event) { current = null; }
    *///?}
}
