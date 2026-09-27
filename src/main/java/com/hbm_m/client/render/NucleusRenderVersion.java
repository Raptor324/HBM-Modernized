package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/**
 * Глобальный поколенческий счётчик инстанс-записей движка Nucleus.
 * <p>
 * Инстанс-записи (InstPos/InstRot/свет/fade) в instance-буферах валидны только
 * в рамках «мира» данного поколения. Смена поколения (bump) объявляет ВСЕ записи
 * протухшими: машины с {@code RenderDirtyTracker} пересобираются целиком при
 * ближайшей сборке (roster-assert выдаёт worldGen-мисс).
 * <p>
 * Bump-сайты:
 * <ul>
 *   <li>дрейф якоря ({@link InstancedStaticPartRenderer#onRenderOriginChanged}) —
 *       записи якорно-относительны, сдвиг якоря делает их все неверными;</li>
 *   <li>смена Iris-состояния ({@code ClientRenderFlags.onFrameStart}) — пути
 *       Iris заполняют {@code instanceLightUV}, которого fast-path не пишет.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class NucleusRenderVersion {

    private static volatile long worldGen = 1L;

    private NucleusRenderVersion() {}

    public static long worldGen() {
        return worldGen;
    }

    public static void bump() {
        worldGen++;
    }
}
