package com.hbm_m.entity.cart;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.inventory.menu.CartDestroyerMenu;
import com.hbm_m.item.tool.ItemModMinecart;
import com.hbm_m.item.tool.ItemModMinecart.EnumCartBase;
import com.hbm_m.item.tool.ItemModMinecart.EnumMinecart;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code EntityMinecartDestroyer}: Schrott-Vernichtungslore. 9 exakte und 9 Platzhalter-Filter (Geisterplaetze);
 * alle 5 Ticks verschwinden passende Gegenstaende im Umkreis (2,5 / -1,5..2) mit Holzbruchgeraeusch, dazu Rauch.
 */
public class EntityMinecartDestroyer extends EntityMinecartContainerBase {

    public EntityMinecartDestroyer(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMinecartDestroyer(EntityType<?> type, Level world, double x, double y, double z, EnumCartBase base) {
        super(type, world, x, y, z, base);
    }

    @Override
    public int getContainerSize() {
        return 18;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return false;
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory) {
        return new CartDestroyerMenu(id, inventory, this);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && this.tickCount % 5 == 0) {

            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, new AABB(
                    getX() - 2.5,
                    getY() - 1.5,
                    getZ() - 2.5,
                    getX() + 2.5,
                    getY() + 2,
                    getZ() + 2.5));

            boolean sound = false;

            outer:
            for (ItemEntity item : items) {
                ItemStack stack = item.getItem();

                //Match meta (im Port: Gegenstand + NBT)
                for (int i = 0; i < 9; i++) {
                    ItemStack match = getItem(i);

                    if (!match.isEmpty() && StackNbt.sameItemSameTags(match, stack)) {
                        item.discard();
                        sound = true;
                        continue outer;
                    }
                }

                //Match wildcard
                for (int i = 9; i < 18; i++) {
                    ItemStack match = getItem(i);

                    if (!match.isEmpty() && match.getItem() == stack.getItem()) {
                        item.discard();
                        sound = true;
                        continue outer;
                    }
                }
            }

            if (sound)
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.NEUTRAL, 0.5F, 0.5F + level().random.nextFloat() * 0.2F);
        }

        if (level().isClientSide && this.tickCount % 5 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.75, getZ(), 0.0, 0.01, 0.0);
        }
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(this.getBase(), EnumMinecart.DESTROYER);
    }

    @Override
    public String specialContent() {
        return "destroyer";
    }
}
