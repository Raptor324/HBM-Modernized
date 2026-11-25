package com.hbm_m.hazard.modifier;

import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.item.rbmk.RBMKPelletItem;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 port of the original's {@code HazardModifierRBMKRadiation}: a fresh rod starts at the
 * entry's base radiation and ramps toward {@code target} as it depletes (short-lived fission
 * products mean radioactivity rises faster than depletion under the quadratic curve), plus a
 * flat contribution from buffered Xenon-135 poison.
 *
 * <p>The pellet branch is driven by the pellet's encoded state ({@code RBMKPelletItem.getState},
 * the {@code pellet_state} NBT the original carried as item damage): {@code state % 5} is the
 * depletion tier, and states 5-9 additionally flag xenon poison, which adds
 * {@code xe135 * nugget} on top.</p>
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

        } else if (stack.getItem() instanceof RBMKPelletItem) {

            int state = RBMKPelletItem.getState(stack);

            level = level + (this.target - level) * ((state % 5) / 4F);

            // CE HazardRegistry.nugget is the 0.1F form factor (the port keeps it local to its
            // registry table, so the value is spelled out here).
            if (RBMKPelletItem.isPoisoned(state))
                level += HazardRegistry.xe135 * 0.1F;
        }

        return level;
    }
}
