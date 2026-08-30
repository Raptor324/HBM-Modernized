//? if forge {
package com.hbm_m.compat.curios;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.item.gasmask.IGasMask;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * [Forge 1.20.1] Навешивает ICurio-обёртку на наши противогазы,
 * чтобы их можно было носить в слоте лица Curios. Класс активен только
 * при установленном Curios (см. CuriosCompat.isLoaded()).
 */
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID)
public final class CuriosForgeEvents {

    private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "gas_mask_curio");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        if (!CuriosCompat.isLoaded()) {
            return;
        }
        ItemStack stack = event.getObject();
        if (!(stack.getItem() instanceof IGasMask)) {
            return;
        }
        event.addCapability(KEY, CuriosApi.createCurioProvider(new GasMaskCurio(stack)));
    }

    private CuriosForgeEvents() {
    }
}
//?}
