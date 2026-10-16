package com.hbm_m.client.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.hbm_m.compat.curios.CuriosCompat;

/**
 * Клиентская точка входа интеграции с Curios: маска в слоте лица
 * для рендера на игроке ({@link com.hbm_m.client.render.GasMaskLayer})
 * и первого лица ({@link com.hbm_m.client.overlay.OverlayGasMask}).
 * Без Curios возвращает EMPTY — вызовы безопасны всегда.
 */
public final class CuriosClientCompat {

    private CuriosClientCompat() {
    }

    public static ItemStack getFaceMask(LivingEntity entity) {
        return CuriosCompat.getFaceMask(entity);
    }
}
