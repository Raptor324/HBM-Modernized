//? if neoforge {
/*package com.hbm_m.powerarmor.render;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.layer.AJROPowerArmorLayer;
import com.hbm_m.powerarmor.layer.AJRPowerArmorLayer;
import com.hbm_m.powerarmor.layer.BismuthPowerArmorLayer;
import com.hbm_m.powerarmor.layer.DNTPowerArmorLayer;
import com.hbm_m.client.render.GasMaskLayer;
import com.hbm_m.powerarmor.layer.T51PowerArmorLayer;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientPowerArmorRenderNeoForge {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // players
        addSkinRenderer(event, net.minecraft.client.resources.PlayerSkin.Model.WIDE);
        addSkinRenderer(event, net.minecraft.client.resources.PlayerSkin.Model.SLIM);

        // armor stand
        var armorStandRenderer = event.getRenderer(EntityType.ARMOR_STAND);
        if (armorStandRenderer instanceof ArmorStandRenderer standRenderer) {
            addPowerArmorLayers(standRenderer);
        }

        registerForHumanoidMobs(event);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addSkinRenderer(EntityRenderersEvent.AddLayers event, net.minecraft.client.resources.PlayerSkin.Model skinModel) {
        Object renderer = event.getSkin(skinModel);
        if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            addPowerArmorLayers(livingRenderer);
        }
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

        // На 1.21.1 LivingEntityRenderer#addLayer публичный — mixin-аксессор не нужен.
        LivingEntityRenderer raw = (LivingEntityRenderer) renderer;
        raw.addLayer(new T51PowerArmorLayer(raw));
        raw.addLayer(new AJRPowerArmorLayer(raw));
        raw.addLayer(new AJROPowerArmorLayer(raw));
        raw.addLayer(new BismuthPowerArmorLayer(raw));
        raw.addLayer(new DNTPowerArmorLayer(raw));
        raw.addLayer(new GasMaskLayer(raw));
    }

    private ClientPowerArmorRenderNeoForge() {}
}
*///?}
