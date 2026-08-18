package com.hbm_m.hazard.modifier;

import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 port of the original's {@code HazardModifierRBMKRadiation}: a fresh rod starts at the
 * entry's base radiation and ramps toward {@code target} as it depletes (short-lived fission
 * products mean radioactivity rises faster than depletion under the quadratic curve), plus a
 * flat contribution from buffered Xenon-135 poison.
 *
 * <p>The original also had a pellet branch driven by the pellet's item-damage value (a
 * depletion/xenon state baked into the stack). Modernized {@link com.hbm_m.item.rbmk.RBMKPelletItem}
 * has no such state - pellets are static crafting ingredients, all depletion/xenon state lives
 * on the rod - so that branch has no structural counterpart here and is intentionally omitted.
 */
public class HazardModifierRBMKRadiation extends HazardModifier {

    float target;
    boolean linear = false;

    public HazardModifierRBMKRadiation(float target, boolean linear) {
        this.target = target;
        this.linear = linear;
    }

    @Override
    public float modify(ItemStack stack, LivingEntity holder, float level) {
        if (stack.getItem() instanceof RBMKRodItem) {
            double enrichment = RBMKRodItem.getEnrichment(stack);
            double depletion = linear ? 1D - enrichment : 1D - Math.pow(enrichment, 2);
            double xenon = RBMKRodItem.getPoisonLevel(stack);

            level = (float) (level + (this.target - level) * depletion);
            level += HazardRegistry.xe135 * xenon;
        }

        return level;
    }
}
