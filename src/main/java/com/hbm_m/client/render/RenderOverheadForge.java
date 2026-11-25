package com.hbm_m.client.render;

import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//? if forge {
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.fml.common.EventBusSubscriber;
*///?}

/** Forge hook for {@link RenderOverhead} (Original: {@code RenderWorldLastEvent}). */
//? if forge {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
/*@EventBusSubscriber(value = Dist.CLIENT)
*///?}
public final class RenderOverheadForge {
    private RenderOverheadForge() {}

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        RenderOverhead.renderMarkers(event.getPoseStack(), event.getCamera());

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && ItemGunBaseNT.aimingProgress == 1.0F) {
            ItemStack held = mc.player.getMainHandItem();
            if (!(held.getItem() instanceof ItemGunBaseNT)) {
                held = mc.player.getOffhandItem();
            }
            if (held.getItem() instanceof ItemGunBaseNT gun) {
                boolean thermalSights = false;
                for (int i = 0; i < gun.getConfigCount(); i++) {
                    GunConfig cfg = gun.getConfig(held, i);
                    if (cfg != null && cfg.hasThermalSights(held)) {
                        thermalSights = true;
                        break;
                    }
                }
                if (thermalSights) {
                    RenderOverhead.renderThermalSight(event.getPoseStack(), event.getCamera(), com.hbm_m.platform.RenderHooks.getPartialTickRaw());
                }
            }
        }
    }
}
