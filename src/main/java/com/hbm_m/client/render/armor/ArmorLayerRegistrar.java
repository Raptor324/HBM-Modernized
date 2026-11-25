package com.hbm_m.client.render.armor;

import java.util.List;
import java.util.function.Function;
import java.util.function.Consumer;

import com.hbm_m.client.render.GasMaskLayer;
import com.hbm_m.entity.ModEntities;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;

/**
 * Shared registration of 3D armor, accessory, and gas-mask render layers across Forge and NeoForge.
 */
public final class ArmorLayerRegistrar {

    private ArmorLayerRegistrar() {}

    public static void addMobAndStandLayers(
            Function<EntityType<?>, EntityRenderer<?>> rendererLookup,
            Consumer<LivingEntityRenderer<?, ?>> layerInstaller) {
        for (EntityType<?> type : List.of(
                EntityType.ARMOR_STAND,
                EntityType.ZOMBIE,
                EntityType.ZOMBIE_VILLAGER,
                EntityType.SKELETON,
                EntityType.WITHER_SKELETON,
                EntityType.STRAY,
                EntityType.DROWNED,
                EntityType.HUSK,
                EntityType.PIGLIN,
                EntityType.PIGLIN_BRUTE,
                EntityType.ZOMBIFIED_PIGLIN,
                ModEntities.UNDEAD_SOLDIER.get())) {
            try {
                EntityRenderer<?> renderer = rendererLookup.apply(type);
                if (renderer instanceof LivingEntityRenderer<?, ?> living) {
                    layerInstaller.accept(living);
                }
            } catch (Exception ignored) {
                // some custom/optional entity types may not have a humanoid renderer
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void appendToLayerList(List<Object> rawLayers, LivingEntityRenderer renderer) {
        rawLayers.add(new UnifiedObjArmorLayer(renderer));
        rawLayers.add(new ArmorAccessoryLayer(renderer));
        rawLayers.add(new GasMaskLayer(renderer));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addViaPublicApi(LivingEntityRenderer renderer) {
        renderer.addLayer(new UnifiedObjArmorLayer(renderer));
        renderer.addLayer(new ArmorAccessoryLayer(renderer));
        renderer.addLayer(new GasMaskLayer(renderer));
    }
}
