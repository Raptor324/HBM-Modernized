package com.hbm_m.item.industrial;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.machine.ItemBlueprints} ({@code blueprints}): traegt den Rezept-Pool ("pool") fuer den
 * Blaupausen-Platz der Maschinen. Rechtsklick kopiert sie gegen ein Papier (geheime Pools nicht). Das Aussehen
 * (discover/secret/528) setzt die Modell-Eigenschaft {@code hbm_m:blueprint}.
 */
public class ItemBlueprints extends Item implements ITooltipProvider {

    public ItemBlueprints(Properties properties) {
        super(properties);
    }

    /** Modell-Eigenschaft: 0 blau, 1 discover (beige), 2 secret (schwarz), 3 528 (grau). */
    public static float iconIndex(ItemStack stack) {
        String poolName = grabPool(stack);
        if (poolName == null) return 0;
        if (poolName.startsWith(BlueprintPools.POOL_PREFIX_DISCOVER)) return 1;
        if (poolName.startsWith(BlueprintPools.POOL_PREFIX_SECRET)) return 2;
        if (poolName.startsWith(BlueprintPools.POOL_PREFIX_528)) return 3;
        return 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) return InteractionResultHolder.pass(stack);
        if (!StackNbt.has(stack)) return InteractionResultHolder.pass(stack);

        String poolName = StackNbt.read(stack).getString("pool");

        if (poolName.startsWith(BlueprintPools.POOL_PREFIX_SECRET)) return InteractionResultHolder.pass(stack);
        if (!player.getInventory().contains(new ItemStack(Items.PAPER))) return InteractionResultHolder.pass(stack);

        consume(player);
        player.swing(hand);

        ItemStack copy = stack.copyWithCount(1);

        if (!player.getAbilities().instabuild) {
            if (stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                return InteractionResultHolder.success(stack);
            }

            if (!player.getInventory().add(copy)) {
                player.drop(stack.copyWithCount(1), false);
            }
            player.inventoryMenu.broadcastChanges();
        } else {
            player.drop(copy, false);
        }

        return InteractionResultHolder.success(stack);
    }

    private static void consume(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(Items.PAPER)) {
                s.shrink(1);
                return;
            }
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (!StackNbt.has(stack) || level == null) return;

        String poolName = StackNbt.read(stack).getString("pool");
        List<Component> pool = BlueprintPools.getPools(level).get(poolName);

        if (pool == null || pool.isEmpty()) return;

        if (poolName.startsWith(BlueprintPools.POOL_PREFIX_SECRET)) {
            list.add(Component.literal("Cannot be copied!").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("Right-click to copy (requires paper)").withStyle(ChatFormatting.YELLOW));
        }

        list.addAll(pool);
    }

    @Nullable
    public static String grabPool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.getItem() != ModItems.BLUEPRINTS.get()) return null;
        if (!StackNbt.has(stack)) return null;
        if (!StackNbt.read(stack).contains("pool")) return null;
        return StackNbt.read(stack).getString("pool");
    }

    /** Wie {@link #grabPool}, aber "" statt null (fuer die Pool-Vergleiche der Maschinen). */
    public static String getBlueprintPool(ItemStack stack) {
        String pool = grabPool(stack);
        return pool == null ? "" : pool;
    }

    public static ItemStack make(String pool) {
        ItemStack stack = new ItemStack(ModItems.BLUEPRINTS.get());
        StackNbt.set(stack, new CompoundTag());
        StackNbt.tag(stack).putString("pool", pool);
        return stack;
    }
}
