package com.hbm_m.item.satellite;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.interfaces.IItemControlReceiver;
import com.hbm_m.item.ModItems;
import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.tool.ItemSatInterface} ({@code sat_coord}, im Port auch {@code sat_interface}): in der Hand
 * wird "connected" gesetzt, sobald ein Satellit auf der Frequenz im Orbit ist; {@code sat_coord} oeffnet die
 * Koordinateneingabe und schickt x/(y)/z als {@link Satellite#onCoordAction} (fehlendes y = -1).
 */
public class ItemSatInterface extends ItemSatChip implements IItemControlReceiver {

    public static final String KEY_NBT_CONNECTED = "connected";

    public ItemSatInterface(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide && this == ModItems.SAT_COORD.get())
            com.hbm_m.inventory.gui.GUIScreenSatCoord.open(stack);
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide || !(entity instanceof ServerPlayer player))
            return;

        if (player.getMainHandItem() != stack)
            return;

        Satellite sat = SatelliteManager.get((ServerLevel) world).getSatFromFreq(this.getFreq(stack));
        StackNbt.orCreate(stack).putBoolean(KEY_NBT_CONNECTED, sat != null);
    }

    @Override
    public void receiveControl(ItemStack stack, CompoundTag data) { }

    @Override
    public void receiveControl(Player player, ItemStack stack, CompoundTag data) {
        if (this != com.hbm_m.item.ModItems.SAT_COORD.get()) return;
        if (!(player.level() instanceof ServerLevel server)) return;
        int x = data.getInt("x");
        int z = data.getInt("z");
        if (!server.getWorldBorder().isWithinBounds(x, z)) return;
        int y = data.contains("y") ? net.minecraft.util.Mth.clamp(data.getInt("y"), server.getMinBuildHeight(), server.getMaxBuildHeight()) : -1;
        Satellite sat = SatelliteManager.get(server).getSatFromFreq(this.getFreq(stack));
        if (sat != null) sat.onCoordAction(server, player, x, y, z);
    }
}
