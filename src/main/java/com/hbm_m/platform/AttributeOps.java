package com.hbm_m.platform;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Versionsfassade fuer {@link AttributeModifier.Operation}: 1.21.1 hat die Konstanten umbenannt
 * (ADDITION -> ADD_VALUE, MULTIPLY_BASE -> ADD_MULTIPLIED_BASE, MULTIPLY_TOTAL -> ADD_MULTIPLIED_TOTAL).
 * {@code AttributeModifier.Operation.ADDITION} -> {@code AttributeOps.ADDITION}.
 */
public final class AttributeOps {
    private AttributeOps() {}

    //? if < 1.21.1 {
    public static final AttributeModifier.Operation ADDITION = AttributeModifier.Operation.ADDITION;
    public static final AttributeModifier.Operation MULTIPLY_BASE = AttributeModifier.Operation.MULTIPLY_BASE;
    public static final AttributeModifier.Operation MULTIPLY_TOTAL = AttributeModifier.Operation.MULTIPLY_TOTAL;
    //?} else {
    /*public static final AttributeModifier.Operation ADDITION = AttributeModifier.Operation.ADD_VALUE;
    public static final AttributeModifier.Operation MULTIPLY_BASE = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    public static final AttributeModifier.Operation MULTIPLY_TOTAL = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
    *///?}
}
