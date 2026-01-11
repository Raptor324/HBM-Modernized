package com.hbm_m.powerarmor;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
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
        // Регистрируем для игроков
        PlayerRenderer defaultRenderer = event.getSkin("default");
        if (defaultRenderer != null) {
            defaultRenderer.addLayer(new T51PowerArmorLayer<>(defaultRenderer));
        }

        PlayerRenderer slimRenderer = event.getSkin("slim");
        if (slimRenderer != null) {
            slimRenderer.addLayer(new T51PowerArmorLayer<>(slimRenderer));
        }

        // Регистрируем для стойки для брони
        var armorStandRenderer = event.getRenderer(EntityType.ARMOR_STAND);
        if (armorStandRenderer instanceof ArmorStandRenderer standRenderer) {
            standRenderer.addLayer(new T51PowerArmorLayer<>(standRenderer));
        }

        // Регистрируем для мобов, которые могут носить броню
        registerForHumanoidMobs(event);
    }

    /**
     * Регистрирует T51PowerArmorLayer для всех мобов с HumanoidModel, которые могут носить броню.
     */
    private static void registerForHumanoidMobs(EntityRenderersEvent.AddLayers event) {
        // Регистрируем для каждого типа моба отдельно
        registerForMobType(event, EntityType.ZOMBIE);
        registerForMobType(event, EntityType.SKELETON);
        registerForMobType(event, EntityType.WITHER_SKELETON);
        registerForMobType(event, EntityType.STRAY);
        registerForMobType(event, EntityType.DROWNED);
        registerForMobType(event, EntityType.HUSK);
        registerForMobType(event, EntityType.PIGLIN);
        registerForMobType(event, EntityType.PIGLIN_BRUTE);
        registerForMobType(event, EntityType.ZOMBIFIED_PIGLIN);
    }

    /**
     * Регистрирует T51PowerArmorLayer для конкретного типа моба.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends net.minecraft.world.entity.monster.Monster> void registerForMobType(
            EntityRenderersEvent.AddLayers event, EntityType<T> entityType) {
        try {
            var renderer = event.getRenderer(entityType);
            if (renderer instanceof HumanoidMobRenderer humanoidRenderer) {
                // Используем сырые типы для обхода проблем с дженериками
                humanoidRenderer.addLayer(new T51PowerArmorLayer(humanoidRenderer));
                if (MainRegistry.LOGGER.isDebugEnabled()) {
                    MainRegistry.LOGGER.debug("Registered T51PowerArmorLayer for {}", entityType.toShortString());
                }
            }
        } catch (Exception e) {
            // Некоторые мобы могут не иметь рендерера или иметь другой тип рендерера
            // Это нормально, просто пропускаем их
            if (MainRegistry.LOGGER.isDebugEnabled()) {
                MainRegistry.LOGGER.debug("Could not register T51PowerArmorLayer for {}: {}", 
                    entityType.toShortString(), e.getMessage());
            }
        }
    }

    private ClientPowerArmorRender() {}
}

