package com.hbm_m.client.model.variant;

import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * Client-only holder for ModelProperty constants used by door rendering.
 * Kept separate from DoorBlockEntity so the server never loads ModelProperty
 * or other client classes, avoiding NoSuchFieldError on dedicated server.
 */
public final class DoorModelProperties {

    private DoorModelProperties() {}

    /** Property для передачи выбора модели через ModelData */
    public static final ModelProperty<DoorModelSelection> MODEL_SELECTION_PROPERTY = new ModelProperty<>();

    /** Property для передачи состояния движения двери через ModelData */
    public static final ModelProperty<Boolean> DOOR_MOVING_PROPERTY = new ModelProperty<>();

    /** Property для передачи состояния open/closed через ModelData */
    public static final ModelProperty<Boolean> OPEN_PROPERTY = new ModelProperty<>();

    /** Property: true когда дверь в open/closed, но BER ещё рисует створки (период overlap) */
    public static final ModelProperty<Boolean> OVERLAP_PROPERTY = new ModelProperty<>();
}
