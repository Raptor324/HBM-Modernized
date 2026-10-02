package com.hbm_m.api.tile;

import com.hbm_m.item.ItemKeyPin;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tool.ItemLock;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code TileEntityLockableBase} als Baustein: Stiftzahl, Sperre, Knackwert und ob sich ein Nachschluessel machen
 * laesst, samt {@link #canAccess}: passender Schluessel oder roter Schluessel oeffnen, sonst Knackversuch mit Stift und
 * Schraubenzieher (Chance {@code lockMod * 100} Prozent, mit Jacke x100).
 */
public class LockState {

    public int lock;
    public boolean isLocked = false;
    public double lockMod = 0.1D;
    public boolean cheesable = true;

    public void write(CompoundTag nbt) {
        nbt.putInt("lock", lock);
        nbt.putBoolean("cheesable", cheesable);
        nbt.putBoolean("isLocked", isLocked);
        nbt.putDouble("lockMod", lockMod);
    }

    public void read(CompoundTag nbt) {
        lock = nbt.getInt("lock");
        cheesable = nbt.getBoolean("cheesable");
        isLocked = nbt.getBoolean("isLocked");
        lockMod = nbt.getDouble("lockMod");
    }

    public boolean canAccess(Level world, Player player) {
        if (!isLocked) return true;
        if (player == null) return false;

        ItemStack stack = player.getMainHandItem();
        if (!stack.isEmpty() && stack.getItem() instanceof ItemKeyPin && !(stack.getItem() instanceof ItemLock) && ItemKeyPin.getCode(stack) == this.lock) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("block.lockOpen"), SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        if (!stack.isEmpty() && stack.is(ModItems.KEY_RED.get())) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("block.lockOpen"), SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        return tryPick(world, player);
    }

    private boolean tryPick(Level world, Player player) {
        boolean canPick = false;
        ItemStack stack = player.getMainHandItem();
        double chanceOfSuccess = this.lockMod * 100;
        var inv = player.getInventory();

        if (stack.is(ModItems.PIN.get()) && (inv.contains(new ItemStack(ModItems.SCREWDRIVER.get())) || inv.contains(new ItemStack(ModItems.SCREWDRIVER_DESH.get())))) {
            stack.shrink(1);
            canPick = true;
        }

        if ((stack.is(ModItems.SCREWDRIVER.get()) || stack.is(ModItems.SCREWDRIVER_DESH.get())) && inv.contains(new ItemStack(ModItems.PIN.get()))) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (inv.getItem(i).is(ModItems.PIN.get())) {
                    inv.getItem(i).shrink(1);
                    break;
                }
            }
            player.inventoryMenu.broadcastChanges();
            canPick = true;
        }

        if (canPick) {
            if (ArmorUtil.checkArmorPiece(player, ModItems.JACKT.get(), EquipmentSlot.CHEST) || ArmorUtil.checkArmorPiece(player, ModItems.JACKT2.get(), EquipmentSlot.CHEST))
                chanceOfSuccess *= 100D;

            double rand = world.random.nextDouble() * 100;
            if (chanceOfSuccess > rand) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("item.pinUnlock"), SoundSource.PLAYERS, 1.0F, 1.0F);
                return true;
            }
            world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("item.pinBreak"), SoundSource.PLAYERS, 1.0F, 0.8F + world.random.nextFloat() * 0.2F);
        }
        return false;
    }
}
