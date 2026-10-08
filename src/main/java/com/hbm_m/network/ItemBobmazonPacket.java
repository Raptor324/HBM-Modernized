package com.hbm_m.network;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.missile.EntityBobmazon;
import com.hbm_m.handler.BobmazonOfferFactory;
import com.hbm_m.handler.BobmazonOfferFactory.Offer;
import com.hbm_m.item.ModItems;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemBobmazonPacket}: Bestellung per Angebotsindex; der Server prueft Erfolg und Kronkorken und schickt die Lieferrakete. */
public class ItemBobmazonPacket implements C2SPacket {

    private final int offer;

    public ItemBobmazonPacket(int offer) {
        this.offer = offer;
    }

    public ItemBobmazonPacket(Player player, Offer offer) {
        int index = 0;
        ItemStack held = player.getMainHandItem();
        if (held.is(ModItems.BOBMAZON.get())) index = BobmazonOfferFactory.standard.indexOf(offer);
        if (held.is(ModItems.BOBMAZON_HIDDEN.get())) index = BobmazonOfferFactory.special.indexOf(offer);
        this.offer = index;
    }

    public static ItemBobmazonPacket decode(FriendlyByteBuf buf) {
        return new ItemBobmazonPacket(buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(offer);
    }

    public static void handle(ItemBobmazonPacket m, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer p)) return;
            Level world = p.level();

            ItemStack held = p.getMainHandItem();
            if (BobmazonOfferFactory.getOffers(held) == null) BobmazonOfferFactory.init();

            Offer offer = null;
            if (held.is(ModItems.BOBMAZON.get()) && m.offer >= 0 && m.offer < BobmazonOfferFactory.standard.size()) offer = BobmazonOfferFactory.standard.get(m.offer);
            if (held.is(ModItems.BOBMAZON_HIDDEN.get()) && m.offer >= 0 && m.offer < BobmazonOfferFactory.special.size()) offer = BobmazonOfferFactory.special.get(m.offer);

            if (offer == null) {
                p.sendSystemMessage(Component.literal("[BOBMAZON] There appears to be a mismatch between the offer you have requested and the offers that exist."));
                p.sendSystemMessage(Component.literal("[BOBMAZON] Engaging fail-safe..."));
                p.hurt(ModDamageSources.nuclearBlast(world), 1000);
                p.setDeltaMovement(p.getDeltaMovement().x, 2.0D, p.getDeltaMovement().z);
                p.hurtMarked = true;
                return;
            }

            ItemStack stack = offer.offer;

            if (offer.requirement.fullfills(p) || p.getAbilities().instabuild) {

                if (countCaps(p) >= offer.cost || p.getAbilities().instabuild) {

                    payCaps(p, offer.cost);
                    p.inventoryMenu.broadcastChanges();

                    RandomSource rand = world.random;
                    EntityBobmazon bob = new EntityBobmazon(world);
                    bob.setPos(p.getX() + rand.nextGaussian() * 10, 300, p.getZ() + rand.nextGaussian() * 10);
                    bob.payload = stack.copy();

                    world.addFreshEntity(bob);
                } else {
                    p.sendSystemMessage(Component.literal("[BOBMAZON] Not enough caps!"));
                }

            } else {
                p.sendSystemMessage(Component.literal("[BOBMAZON] Achievement requirement not met!"));
            }
        });
    }

    private static boolean isCap(Item item) {
        return item == ModItems.CAP_FRITZ.get() ||
                item == ModItems.CAP_KORL.get() ||
                item == ModItems.CAP_NUKA.get() ||
                item == ModItems.CAP_QUANTUM.get() ||
                item == ModItems.CAP_RAD.get() ||
                item == ModItems.CAP_SPARKLE.get();
    }

    private static int countCaps(Player player) {

        int count = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isCap(stack.getItem())) count += stack.getCount();
        }

        return count;
    }

    private static void payCaps(Player player, int price) {

        if (price == 0) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {

            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.isEmpty() && isCap(stack.getItem())) {

                int size = stack.getCount();
                for (int j = 0; j < size; j++) {

                    player.getInventory().removeItem(i, 1);
                    price--;

                    if (price == 0) return;
                }
            }
        }
    }

    public static void sendToServer(Player player, Offer offer) {
        ModPacketHandler.sendToServer(ModPacketHandler.ITEM_BOBMAZON, new ItemBobmazonPacket(player, offer));
    }
}
