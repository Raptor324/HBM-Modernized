//? if forge {
/*package com.hbm_m.powerarmor.render;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.mixin.LivingEntityRendererInvoker;
import com.hbm_m.powerarmor.layer.AJROPowerArmorLayer;
import com.hbm_m.powerarmor.layer.AJRPowerArmorLayer;
import com.hbm_m.powerarmor.layer.BismuthPowerArmorLayer;
import com.hbm_m.powerarmor.layer.DNTPowerArmorLayer;
import com.hbm_m.powerarmor.layer.T51PowerArmorLayer;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientPowerArmorRenderForge {

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("t51_armor_parts", new T51ArmorModelLoader());
        event.register("ajr_armor_parts", new AJRArmorModelLoader());
        event.register("bismuth_armor_parts", new BismuthArmorModelLoader());
        event.register("dnt_armor_parts", new DNTArmorModelLoader());
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ClientPowerArmorRender.T51_MODEL_BAKED);
        event.register(ClientPowerArmorRender.AJR_MODEL_BAKED);
        event.register(ClientPowerArmorRender.AJRO_MODEL_BAKED);
        event.register(ClientPowerArmorRender.BISMUTH_MODEL_BAKED);
        event.register(ClientPowerArmorRender.DNT_MODEL_BAKED);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // players
        addPowerArmorLayers(event.getSkin("default"));
        addPowerArmorLayers(event.getSkin("slim"));

        // armor stand
        var armorStandRenderer = event.getRenderer(EntityType.ARMOR_STAND);
        if (armorStandRenderer instanceof ArmorStandRenderer standRenderer) {
            addPowerArmorLayers(standRenderer);
        }

        registerForHumanoidMobs(event);
    }

    private static void registerForHumanoidMobs(EntityRenderersEvent.AddLayers event) {
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends net.minecraft.world.entity.monster.Monster> void registerForMobType(
        EntityRenderersEvent.AddLayers event, EntityType<T> entityType
    ) {
        try {
            var renderer = event.getRenderer(entityType);
            if (renderer instanceof HumanoidMobRenderer humanoidRenderer) {
                addPowerArmorLayers(humanoidRenderer);
            }
        } catch (Exception ignored) {
            // some mobs may not have a humanoid renderer; safe to ignore
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addPowerArmorLayers(LivingEntityRenderer<?, ?> renderer) {
        if (renderer == null) return;

        // addLayer(...) protected → добавляем напрямую в список layers через mixin accessor
        LivingEntityRendererInvoker accessor = (LivingEntityRendererInvoker) renderer;
        accessor.hbm_m$getLayers().add(new T51PowerArmorLayer((LivingEntityRenderer) renderer));
        accessor.hbm_m$getLayers().add(new AJRPowerArmorLayer((LivingEntityRenderer) renderer));
        accessor.hbm_m$getLayers().add(new AJROPowerArmorLayer((LivingEntityRenderer) renderer));
        accessor.hbm_m$getLayers().add(new BismuthPowerArmorLayer((LivingEntityRenderer) renderer));
        accessor.hbm_m$getLayers().add(new DNTPowerArmorLayer((LivingEntityRenderer) renderer));
    }

    private ClientPowerArmorRenderForge() {}
}
*///?}

