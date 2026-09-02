package com.hbm_m.client;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

import com.hbm_m.inventory.gui.GUIPWRPrinter;

import net.minecraft.client.Minecraft;

/**
 * Клиентские хуки PWR Printer: открытие экрана скана структуры.
 *
 * <p>Вынесено из {@link com.hbm_m.network.PWRPrinterScanPacket}: класс пакета регистрируется
 * S2C-приёмником на ОБОИХ сторонах (в т.ч. на выделенном сервере — NeoForge 1.21+ требует
 * объявления пейлоада для negotiation). Прямая ссылка на {@link GUIPWRPrinter} в теле
 * обработчика линковала иерархию GUIPWRPrinter → AbstractContainerScreen → Screen
 * (клиентские классы) уже при создании method reference во время регистрации мода,
 * что роняло загрузку на DEDICATED_SERVER (BootstrapMethodError: invalid dist DEDICATED_SERVER).
 * Делегирование через FQN внутри лямбды резолвится лениво — только при исполнении на клиенте.
 */
//? if forge || neoforge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class PwrPrinterClientHooks {

    private PwrPrinterClientHooks() {
    }

    public static void openScanScreen(int sizeX, int sizeY, int sizeZ, byte[] grid) {
        Minecraft.getInstance().setScreen(new GUIPWRPrinter(sizeX, sizeY, sizeZ, grid));
    }
}
