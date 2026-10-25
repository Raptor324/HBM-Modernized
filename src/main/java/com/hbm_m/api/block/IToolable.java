package com.hbm_m.api.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code api.hbm.block.IToolable}: Bloecke, die auf Handwerkzeuge reagieren. Die Werkzeuge rufen
 * {@link #onScrew} aus ihrem {@code useOn} auf (1.7.10 {@code onItemUse}), also erst, wenn der Block selbst
 * nicht reagiert hat; liefert es true, nutzt sich das Werkzeug ab bzw. verbraucht Brennstoff.
 */
public interface IToolable {

    boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool);

    enum ToolType {
        SCREWDRIVER,
        HAND_DRILL,
        DEFUSER,
        WRENCH,
        TORCH,
        BOLT;

        /** {@code stacksForDisplay}; im Port Lieferanten, weil die Items spaeter registriert werden. */
        public final List<Supplier<? extends Item>> stacksForDisplay = new ArrayList<>();

        public void register(Supplier<? extends Item> item) {
            stacksForDisplay.add(item);
        }

        public List<ItemStack> getStacksForDisplay() {
            List<ItemStack> list = new ArrayList<>();
            for (Supplier<? extends Item> s : stacksForDisplay) list.add(new ItemStack(s.get()));
            return list;
        }

        public boolean matches(ItemStack stack) {
            for (Supplier<? extends Item> s : stacksForDisplay) if (stack.is(s.get())) return true;
            return false;
        }

        static {
            // Original: Konstruktoren von ItemTooling/ItemBlowtorch/ItemBoltgun/ItemToolingWeapon rufen register auf
            SCREWDRIVER.register(com.hbm_m.item.ModItems.SCREWDRIVER);
            SCREWDRIVER.register(com.hbm_m.item.ModItems.SCREWDRIVER_DESH);
            HAND_DRILL.register(com.hbm_m.item.ModItems.HAND_DRILL);
            HAND_DRILL.register(com.hbm_m.item.ModItems.HAND_DRILL_DESH);
            WRENCH.register(com.hbm_m.item.ModItems.WRENCH_ARCHINEER);
            TORCH.register(com.hbm_m.item.ModItems.BLOWTORCH);
            TORCH.register(com.hbm_m.item.ModItems.ACETYLENE_TORCH);
            BOLT.register(com.hbm_m.item.ModItems.BOLTGUN);
            DEFUSER.register(com.hbm_m.item.ModItems.DEFUSER);
        }

        public static ToolType getType(ItemStack stack) {
            for (ToolType type : values()) if (type.matches(stack)) return type;
            return null;
        }
    }
}
