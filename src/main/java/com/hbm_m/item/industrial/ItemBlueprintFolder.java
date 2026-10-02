package com.hbm_m.item.industrial;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.machine.ItemBlueprintFolder}: Rechtsklick verbraucht den Ordner und gibt eine zufaellige
 * {@link ItemBlueprints Blaupause} aus seiner Gruppe (Meta 0 = "alt.", 1 = "discover.", 2 = "secret."; im Port
 * {@code blueprint_folder}, {@code blueprint_folder_discover}, {@code blueprint_folder_secret}).
 */
public class ItemBlueprintFolder extends Item {

    private final String prefix;

    public ItemBlueprintFolder(Properties properties, String prefix) {
        super(properties.stacksTo(1));
        this.prefix = prefix;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) return InteractionResultHolder.pass(stack);

        List<String> pools = new ArrayList<>();

        for (String pool : BlueprintPools.getPools(world).keySet()) {
            if (pool.startsWith(prefix)) pools.add(pool);
        }

        if (!pools.isEmpty()) {
            stack.shrink(1);
            String chosen = pools.get(player.getRandom().nextInt(pools.size()));
            ItemStack blueprint = ItemBlueprints.make(chosen);
            if (!player.getInventory().add(blueprint))
                player.drop(blueprint, false);
        }

        return InteractionResultHolder.success(stack);
    }
}
