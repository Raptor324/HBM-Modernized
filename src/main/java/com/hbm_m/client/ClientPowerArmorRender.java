package com.hbm_m.client;

import com.hbm_m.client.loader.T51ArmorModelLoader;
import com.hbm_m.client.render.T51PowerArmorLayer;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientPowerArmorRender {

    public static final ResourceLocation T51_MODEL_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "t51_armor");
    public static final ModelResourceLocation T51_MODEL_BAKED = new ModelResourceLocation(T51_MODEL_ID, "inventory");

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("t51_armor_parts", new T51ArmorModelLoader());
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(T51_MODEL_BAKED);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        PlayerRenderer defaultRenderer = event.getSkin("default");
        if (defaultRenderer != null) {
            defaultRenderer.addLayer(new T51PowerArmorLayer<>(defaultRenderer));
        }

        PlayerRenderer slimRenderer = event.getSkin("slim");
        if (slimRenderer != null) {
            slimRenderer.addLayer(new T51PowerArmorLayer<>(slimRenderer));
        }

        var armorStandRenderer = event.getRenderer(EntityType.ARMOR_STAND);
        if (armorStandRenderer instanceof ArmorStandRenderer standRenderer) {
            standRenderer.addLayer(new T51PowerArmorLayer<>(standRenderer));
        }
    }

    private ClientPowerArmorRender() {}
}

