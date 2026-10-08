package com.hbm_m.entity.cart;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.inventory.menu.CartCrateMenu;
import com.hbm_m.item.tool.ItemModMinecart;
import com.hbm_m.item.tool.ItemModMinecart.EnumCartBase;
import com.hbm_m.item.tool.ItemModMinecart.EnumMinecart;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityMinecartCrate}: Lore mit Stahlkiste (54 Plaetze, Stahlkisten-GUI). Zerstoert nimmt der Gegenstand
 * den Inhalt im NBT mit ({@code slot<i>}); ueber 6000 Bytes komprimiert gibt es eine kleine Explosion und eine leere
 * Kisten-Lore dazu.
 */
public class EntityMinecartCrate extends EntityMinecartContainerBase {

    public EntityMinecartCrate(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMinecartCrate(EntityType<?> type, Level world, double x, double y, double z, EnumCartBase base, ItemStack stack) {
        super(type, world, x, y, z, base);
        if (stack.hasTag()) {
            for (int i = 0; i < getContainerSize(); i++) {
                setItem(i, ItemStack.of(stack.getTag().getCompound("slot" + i)));
            }
        }
    }

    @Override
    public int getContainerSize() {
        return 9 * 6;
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory) {
        return new CartCrateMenu(id, inventory, this);
    }

    @Override
    public void destroy(@NotNull DamageSource source) {
        this.kill();
        ItemStack itemstack = ItemModMinecart.createCartItem(EnumCartBase.VANILLA, EnumMinecart.CRATE);

        CompoundTag nbt = new CompoundTag();

        for (int i = 0; i < getContainerSize(); i++) {

            ItemStack stack = getItem(i);
            if (stack.isEmpty())
                continue;

            CompoundTag slot = new CompoundTag();
            stack.save(slot);
            nbt.put("slot" + i, slot);
        }

        if (!nbt.isEmpty()) {
            itemstack.setTag(nbt);
        }

        if (this.hasCustomName()) {
            itemstack.setHoverName(this.getCustomName());
        }

        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            NbtIo.writeCompressed(nbt, bytes);

            if (bytes.size() > 6000) {
                level().explode(this, getX(), getY(), getZ(), 2F, true, Level.ExplosionInteraction.TNT);
                this.spawnAtLocation(ItemModMinecart.createCartItem(EnumCartBase.VANILLA, EnumMinecart.CRATE), 0.0F);
            }

        } catch (java.io.IOException e) { }

        this.spawnAtLocation(itemstack, 0.0F);
        this.clearContent();
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(EnumCartBase.VANILLA, EnumMinecart.CRATE);
    }
}
