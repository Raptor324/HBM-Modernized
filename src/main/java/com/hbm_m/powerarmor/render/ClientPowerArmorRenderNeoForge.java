//? if neoforge {
/*package com.hbm_m.powerarmor.render;

import com.hbm_m.client.render.armor.ArmorLayerRegistrar;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/^*
 * Registers unified 3D armor, accessories, and gas mask render layers on NeoForge (1.21.1).
 ^/
@EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public final class ClientPowerArmorRenderNeoForge {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        addSkinRenderer(event, PlayerSkin.Model.WIDE);
        addSkinRenderer(event, PlayerSkin.Model.SLIM);
        ArmorLayerRegistrar.addMobAndStandLayers(type -> getRendererUnchecked(event, type), ClientPowerArmorRenderNeoForge::addPowerArmorLayers);
    }

    private static void addSkinRenderer(EntityRenderersEvent.AddLayers event, PlayerSkin.Model skinModel) {
        if (event.getSkin(skinModel) instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            addPowerArmorLayers(livingRenderer);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static net.minecraft.client.renderer.entity.EntityRenderer<?> getRendererUnchecked(
            EntityRenderersEvent.AddLayers event, EntityType type) {
        return event.getRenderer(type);
    }

    private static void addPowerArmorLayers(LivingEntityRenderer<?, ?> renderer) {
        if (renderer != null) {
            ArmorLayerRegistrar.addViaPublicApi(renderer);
        }
    }

    private ClientPowerArmorRenderNeoForge() {}
}
*///?}
