package com.hbm_m.item.crates;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.crates.BaseCrateBlockEntity;
import com.hbm_m.inventory.menu.BaseCrateMenu;
import com.hbm_m.item.ItemKeyPin;
import com.hbm_m.item.tool.ItemKey;
import com.hbm_m.platform.PlatformHooks;

import dev.architectury.registry.menu.MenuRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ItemBlockStorageCrate.onItemRightClick} + {@code InventoryCrate}: mit
 * {@code CRATE_OPEN_HELD} oeffnet sich eine Kiste direkt aus der Hand. Der Inhalt bleibt dabei im Item
 * ({@code BlockEntityTag} wie beim Abbauen) und wird bei jeder Aenderung zurueckgeschrieben; ein Schloss
 * oeffnet nur, wenn ein passender Schluessel im Inventar liegt, und Spinnen springen beim Spieler heraus.
 */
public final class HeldCrate {

    private HeldCrate() {}

    public static final Random rand = new Random();
    /** Original {@code TileEntityCrateBase.numSpiders}. */
    private static final int NUM_SPIDERS = 3;

    /** Original {@code onItemRightClick} (Server, Stapelgroesse 1). */
    public static void tryOpen(ServerPlayer player, ItemStack stack, Block block) {
        CompoundTag beTag = readBlockEntityTag(stack);

        if (beTag != null && beTag.getBoolean("isLocked")) {
            int lock = beTag.getInt("lock");
            for (ItemStack item : player.getInventory().items) {
                if (item.isEmpty()) continue; // kein Item
                if (!(item.getItem() instanceof ItemKey)) continue; // kein Schluessel
                if (!PlatformHooks.hasItemTag(item)) continue; // ohne NBT oeffnet er sowieso nichts
                if (ItemKeyPin.getPins(item) == lock) { // passende Stifte
                    spawnSpiders(player, stack);
                    open(player, stack, block);
                    break;
                }
            }
            return; // verschlossen: hier Schluss
        }

        spawnSpiders(player, stack);
        open(player, stack, block);
    }

    private static void open(ServerPlayer player, ItemStack stack, Block block) {
        if (!(block instanceof EntityBlock entityBlock)) return;
        // ausserhalb der Welt: der Client findet dort keinen Blockentity und baut sein leeres Gegenstueck
        BlockPos fake = new BlockPos(0, player.level().getMinBuildHeight() - 1, 0);
        BlockEntity be = entityBlock.newBlockEntity(fake, block.defaultBlockState());
        if (!(be instanceof BaseCrateBlockEntity crate)) return;

        CompoundTag beTag = readBlockEntityTag(stack);
        if (beTag != null) PlatformHooks.loadBlockEntityTag(crate, beTag, player.level().registryAccess());

        // InventoryCrate: ohne NBT ein Zufallsschluessel, damit sich die offene Kiste nicht stapelt
        if (!PlatformHooks.hasItemTag(stack) && beTag == null) {
            PlatformHooks.putLong(stack, "stacklock", rand.nextLong());
        }

        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                // InventoryCrate.getInventoryName: umbenannte Kiste behaelt ihren Namen
                return stack.hasCustomHoverName() ? stack.getHoverName() : crate.getDisplayName();
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return crate.createMenu(id, inv, p);
            }
        };

        MenuRegistry.openExtendedMenu(player, provider, buf -> buf.writeBlockPos(fake));
        if (player.containerMenu instanceof BaseCrateMenu menu && menu.getBlockEntity() == crate) {
            menu.bindHeldCrate(stack);
            // ItemInventory.openInventory: Oeffnen am Spieler, Tonhoehe 0.8
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.hbm_m.sound.ModSounds.CRATE_OPEN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    /** Original {@code spawnSpiders(player, world, ItemStack)}: drei Hoehlenspinnen um den Spieler. */
    private static void spawnSpiders(ServerPlayer player, ItemStack crate) {
        CompoundTag beTag = readBlockEntityTag(crate);
        if (beTag == null || !beTag.getBoolean("spiders")) return;
        Random random = new Random();

        for (int i = 0; i < NUM_SPIDERS; i++) {
            CaveSpider spider = EntityType.CAVE_SPIDER.create(player.level());
            if (spider == null) continue;
            spider.moveTo(player.getX() + random.nextGaussian() * 2, player.getY() + 1, player.getZ() + random.nextGaussian() * 2, random.nextFloat(), 0);
            spider.setTarget(player);
            player.level().addFreshEntity(spider);
        }

        beTag.remove("spiders");
        writeBlockEntityTag(crate, beTag);
    }

    /** Original {@code InventoryCrate.markDirty}: Inhalt sofort ins Item, Stapelsperre neu. */
    public static void markDirty(BaseCrateBlockEntity crate, ItemStack target) {
        clearBlockEntityTag(target);
        // wie beim Abbauen: leere, offene Kisten ohne Schloss/Spinnen tragen keinen Inhalt
        if (!crate.isEmpty() || crate.isLocked() || crate.hasSpiders) crate.saveToItem(target);
        // nie den ganzen Tag entfernen - ohne ihn wuerde die offene Kiste stapelbar
        removeRootKey(target, "stacklock");
        PlatformHooks.putLong(target, "stacklock", rand.nextLong());
    }

    /** Original {@code InventoryCrate.closeInventory}: 6-kB-Pruefung, dann Stapelsperre nur, wenn noch Daten da sind. */
    public static void closeInventory(BaseCrateBlockEntity crate, ItemStack target, Player player) {
        markDirty(crate, target);
        checkNBT(crate, target, player);

        removeRootKey(target, "stacklock");
        // keine Daten mehr -> Tag weg, Kiste wieder stapelbar; sonst neue Stapelsperre
        if (PlatformHooks.hasItemTag(target) || readBlockEntityTag(target) != null) {
            PlatformHooks.putLong(target, "stacklock", rand.nextLong());
        }
    }

    /** Original {@code ItemInventory.checkNBT}: ueber 6000 Byte komprimiert wird der Inhalt ausgeworfen. */
    private static void checkNBT(BaseCrateBlockEntity crate, ItemStack target, Player player) {
        //? if < 1.21.1 {
        CompoundTag nbt = target.getTag();
        //?} else {
        /*CompoundTag nbt = readBlockEntityTag(target);
        *///?}
        if (nbt == null || nbt.isEmpty()) return;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.writeCompressed(nbt, out);

            if (out.size() > 6000) {
                player.sendSystemMessage(Component.literal("Warning: Container NBT exceeds 6kB, contents will be ejected!").withStyle(ChatFormatting.RED));
                var handler = crate.getItemHandler();
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy());
                        handler.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
                // return null; // Reset.
                PlatformHooks.setItemTag(target, null);
                clearBlockEntityTag(target);
            }
        } catch (IOException ignored) { }
    }

    // ---- Versionshuellen fuer BlockEntityTag ----

    public static @Nullable CompoundTag readBlockEntityTag(ItemStack stack) {
        //? if < 1.21.1 {
        CompoundTag root = stack.getTag();
        return root != null && root.contains("BlockEntityTag") ? root.getCompound("BlockEntityTag") : null;
        //?} else {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? null : data.copyTag();
        *///?}
    }

    private static void writeBlockEntityTag(ItemStack stack, CompoundTag tag) {
        //? if < 1.21.1 {
        if (tag.isEmpty()) stack.removeTagKey("BlockEntityTag");
        else stack.addTagElement("BlockEntityTag", tag);
        //?} else {
        /*if (tag.isEmpty()) stack.remove(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        else stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        *///?}
    }

    private static void clearBlockEntityTag(ItemStack stack) {
        //? if < 1.21.1 {
        stack.removeTagKey("BlockEntityTag");
        //?} else {
        /*stack.remove(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        *///?}
    }

    private static void removeRootKey(ItemStack stack, String key) {
        //? if < 1.21.1 {
        stack.removeTagKey(key);
        //?} else {
        /*CompoundTag t = PlatformHooks.getItemTag(stack);
        if (t != null && t.contains(key)) { t.remove(key); PlatformHooks.setItemTag(stack, t); }
        *///?}
    }
}
