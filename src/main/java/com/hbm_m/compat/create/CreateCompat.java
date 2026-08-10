//? if forge || neoforge {
package com.hbm_m.compat.create;
//? if forge {
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//?} elif neoforge {
/*import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
*///?}

/**
 * Точка входа совместимости с Create. Forge-only: Create существует только под
 * Forge на 1.20.1.
 *
 * <p>Класс намеренно не содержит статических ссылок на типы Create — его
 * безопасно загружать (и вызывать {@link #isLoaded()}) даже когда Create нет в
 * сборке. Тело методов, ссылающихся на Create API, выполняется только когда
 * Create загружен.
 *
 * <p>Подключение: {@code modBus.addListener(CreateCompat::commonSetup)} в
 * {@code ForgeEntrypoint}. Method-reference создаёт Consumer для шины, класс
 * грузится безопасно, тело commonSetup проверяет {@link #isLoaded()}.
 */
public final class CreateCompat {

    public static final String MOD_ID = "create";

    private CreateCompat() {}

    /** Загружен ли Create в текущей сборке. */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** Слушатель FMLCommonSetupEvent. Безопасен без Create. */
    public static void commonSetup(FMLCommonSetupEvent event) {
        if (!isLoaded()) return;
        event.enqueueWork(CreateDoorRegistrar::register);
    }
}
//?}
