package com.hbm_m.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/**
 * Единый детект «время мира не идёт» для клиентских систем, тикающих вне мира:
 * NT-частицы ({@code EngineHandler}) и serverbound-треки ракет
 * ({@code MissileTrackClient}) раньше дублировали этот гейт по месту.
 *
 * Пауза реплея (Flashback) и ванильный /tick freeze останавливают поток
 * серверных обновлений, но клиентские тики продолжают крутиться — без гейта
 * клиентская симуляция улетает от записанного состояния.
 *
 * Детект по level.getGameTime() НЕ работает: клиентский тик при плейбеке сам
 * наращивает gameTime (+1 за тик, ClientLevel.tickTime не гейтится), а Flashback
 * пишет ClientboundSetTimePacket в КАЖДОМ снапшоте чанка записи (раз в 5 минут),
 * так что при перемотках время скачет в обе стороны — по нему паузу не отличить
 * от плейбека. Поэтому: ванильный тик-фриз (1.20.3+, клиент получает ClientboundTickingStatePacket)
 * плюс прямая детекция паузы Flashback через рефлексию (опциональная зависимость,
 * имена членов классов мода при рантайме не ремапятся — plain-имена безопасны и в dev,
 * и в production, в отличие от имён ванильных классов).
 */
@OnlyIn(Dist.CLIENT)
public final class ClientWorldFreeze {

    private static volatile boolean resolved;
    private static Method flashbackIsInReplay;
    private static Method flashbackIsExporting;
    private static Class<?> replayServerClass;
    private static Field replayPausedField;

    private ClientWorldFreeze() {}

    /**
     * Мир тикает: не пауза-меню и (1.21.1) не /tick freeze.
     * Семантика прежнего гейта NT-частиц в {@code EngineHandler}.
     */
    public static boolean worldRunsNormally() {
        Minecraft mc = Minecraft.getInstance();
        // Пауза-меню SP: клиентский тик жив, сервер стоит.
        if (mc.isPaused()) {
            return false;
        }
        //? if >= 1.21.1 {
        /*ClientLevel level = mc.level;
        if (level != null && !level.tickRateManager().runsNormally()) {
            return false;
        }
        *///?}
        return true;
    }

    /**
     * true, когда поток серверных обновлений поз остановлен и клиентская
     * экстраполяция должна застыть: пауза-меню, /tick freeze или пауза
     * реплея Flashback (фейковый ReplayServer не тикает — пакеты не идут).
     *
     * Экспорт Flashback — НЕ пауза, хотя replayPaused на весь его прогон
     * прибит в true (ExportJob.setServerTickAndWait пишет его на каждом
     * кадре и в setup, а снять может только кнопка паузы в редакторе,
     * недоступная во время экспорта): клиентские тики идут, сервер
     * переигрывает поток поз и пакетов на полном ходу. Если экспорт считать
     * паузой, треки ракет замирают на статичной позе без шлейфов на весь
     * ролик (баг «ракета не летит в экспорте»). Заморозка freeze-секций
     * экспорта и так ловится через worldRunsNormally().
     */
    public static boolean isWorldFrozen() {
        return !worldRunsNormally() || (isFlashbackReplayPaused() && !isFlashbackExporting());
    }

    /**
     * Идёт ли прямо сейчас экспорт видео во Flashback (рефлексия публичного
     * статического {@code Flashback.isExporting()}). true на всём прогоне
     * ExportJob — включая setup-отмотку. Flashback не установлен / сменил API
     * → false (гейты вырождаются в прежнее поведение до фикса).
     */
    public static boolean isFlashbackExporting() {
        try {
            resolve();
            if (flashbackIsExporting == null) {
                return false;
            }
            return (Boolean) flashbackIsExporting.invoke(null);
        } catch (Throwable t) {
            // Flashback не установлен / сменил API — считаем, что экспорта нет.
            return false;
        }
    }

    private static boolean isFlashbackReplayPaused() {
        try {
            resolve();
            if (flashbackIsInReplay == null || replayPausedField == null) {
                return false;
            }
            if (!(Boolean) flashbackIsInReplay.invoke(null)) {
                return false;
            }
            Object server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null || !replayServerClass.isInstance(server)) {
                return false;
            }
            return replayPausedField.getBoolean(server);
        } catch (Throwable t) {
            // Flashback не установлен / сменил API — считаем, что время идёт.
            return false;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> flashback = Class.forName("com.moulberry.flashback.Flashback");
            try {
                flashbackIsInReplay = flashback.getMethod("isInReplay");
            } catch (Throwable t) {
                flashbackIsInReplay = null;
            }
            try {
                flashbackIsExporting = flashback.getMethod("isExporting");
            } catch (Throwable t) {
                flashbackIsExporting = null;
            }
        } catch (Throwable t) {
            flashbackIsInReplay = null;
            flashbackIsExporting = null;
        }
        try {
            replayServerClass = Class.forName("com.moulberry.flashback.playback.ReplayServer");
            replayPausedField = replayServerClass.getField("replayPaused");
        } catch (Throwable t) {
            replayServerClass = null;
            replayPausedField = null;
        }
    }
}
