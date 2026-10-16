//? if neoforge {
/*package com.hbm_m.compat.curios;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.item.ModItems;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;

/^*
 * [NeoForge 1.21.1] Регистрирует ICurio для наших противогазов
 * (слот лица Curios).
 *
 * ВАЖНО: БЕЗ @EventBusSubscriber — авто-подписчик грузит класс (и ICurio)
 * на construction даже без Curios. Регистрируем вручную из
 * {@link CuriosCompat#init()} только при наличии Curios.
 ^/
public final class CuriosNeoForgeEvents {

    /^* Вызывать только после проверки CuriosCompat.isLoaded(). ^/
    public static void init() {
        var container = net.neoforged.fml.ModList.get()
                .getModContainerById(MainRegistry.MOD_ID).orElseThrow();
        container.getEventBus().register(CuriosNeoForgeEvents.class);
    }


    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        if (!CuriosCompat.isLoaded()) {
            return;
        }
        event.registerItem(CuriosCapability.ITEM, (stack, ctx) -> new GasMaskCurio(stack),
                ModItems.GAS_MASK.get(), ModItems.GAS_MASK_M65.get(),
                ModItems.GAS_MASK_MONO.get(), ModItems.GAS_MASK_OLDE.get(),
                ModItems.ATTACHMENT_MASK.get(), ModItems.ATTACHMENT_MASK_MONO.get());
    }

    private CuriosNeoForgeEvents() {
    }
}
*///?}
