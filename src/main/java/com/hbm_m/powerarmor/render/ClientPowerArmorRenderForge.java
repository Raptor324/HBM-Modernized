//? if forge {
package com.hbm_m.powerarmor.render;

import java.util.List;

import com.hbm_m.client.render.armor.ArmorLayerRegistrar;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.mixin.client.LivingEntityRendererInvoker;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers unified 3D armor, accessories, and gas mask render layers on Forge (1.20.1).
 */
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientPowerArmorRenderForge {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        addPowerArmorLayers(event.getSkin("default"));
        addPowerArmorLayers(event.getSkin("slim"));
        ArmorLayerRegistrar.addMobAndStandLayers(type -> getRendererUnchecked(event, type), ClientPowerArmorRenderForge::addPowerArmorLayers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static net.minecraft.client.renderer.entity.EntityRenderer<?> getRendererUnchecked(
            EntityRenderersEvent.AddLayers event, EntityType type) {
        return event.getRenderer(type);
    }

    @SuppressWarnings("unchecked")
    private static void addPowerArmorLayers(LivingEntityRenderer<?, ?> renderer) {
        if (renderer == null) return;
        ArmorLayerRegistrar.appendToLayerList((List<Object>) (List<?>) ((LivingEntityRendererInvoker) renderer).hbm_m$getLayers(), renderer);
    }

    private ClientPowerArmorRenderForge() {}
}
//?}
