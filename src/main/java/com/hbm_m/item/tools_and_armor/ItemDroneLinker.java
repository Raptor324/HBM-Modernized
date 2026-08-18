package com.hbm_m.item.tools_and_armor;

import com.hbm_m.blockentity.network.IDroneLinkable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port von {@code com.hbm.items.tool.ItemDroneLinker} (1.7.10 Original). Zustandsloses (bis auf
 * die im Item-Stack-NBT gespeicherte "vorherige Position") Werkzeug zum manuellen Verketten von
 * {@link IDroneLinkable}-Bloecken (Drone Crate / Drone Waypoint): erster Rechtsklick merkt sich die
 * Zielposition, zweiter Rechtsklick auf einen anderen verlinkbaren Block setzt dessen naechstes Ziel
 * und merkt sich wiederum die neue Position (Verkettung ohne erneutes Zurueckklicken moeglich).
 * Rechtsklick in die Luft loescht die gemerkte Position explizit.
 * <p>
 * SCOPE-Vereinfachung: Das Original zeigt waehrend des Haltens ein clientseitiges HUD-Overlay
 * ("Prev pos: x/y/z"). Hier stattdessen eine einfache Actionbar-Nachricht bei jedem Klick - kein
 * eigenes persistentes Overlay-Rendering noetig, funktional ausreichend.
 */
public class ItemDroneLinker extends Item {

    private static final String TAG_HAS_POS = "hasPos";
    private static final String TAG_POS = "pos";

    public ItemDroneLinker(Properties properties) {
        super(properties.stacksTo(1));
    }

   @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) return InteractionResult.sidedSuccess(level.isClientSide);

        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IDroneLinkable linkable)) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(stack);

        if (tag != null && tag.getBoolean(TAG_HAS_POS)) {
            BlockPos prevPos = BlockPos.of(tag.getLong(TAG_POS));
            BlockEntity prevBe = level.getBlockEntity(prevPos);
            if (prevBe instanceof IDroneLinkable prevLinkable) {
                prevLinkable.setNextTarget(linkable.getDronePoint());
                player.displayClientMessage(Component.literal("Linked " + prevPos.toShortString() + " -> " + pos.toShortString()), true);
            }
        } else {
            player.displayClientMessage(Component.literal("Set initial position: " + pos.toShortString()), true);
        }

        com.hbm_m.platform.PlatformHooks.editItemTag(stack, t -> {
            t.putLong(TAG_POS, pos.asLong());
            t.putBoolean(TAG_HAS_POS, true);
        });

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            com.hbm_m.platform.PlatformHooks.editItemTag(stack, t -> t.putBoolean(TAG_HAS_POS, false));
            player.displayClientMessage(Component.literal("Cleared stored position"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
