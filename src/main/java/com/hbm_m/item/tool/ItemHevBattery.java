package com.hbm_m.item.tool;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.item.IBatteryItem;
import com.hbm_m.powerarmor.ModArmorFSB;
import com.hbm_m.powerarmor.ModArmorFSBPowered;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Original {@code hev_battery} ({@code ItemFusionCore(150000)}, Stapel 4) und der gleichnamige Block {@code HEVBattery}.
 * Im Port teilen sich beide die ID: Rechtsklick in die Luft laedt wie {@code ItemFusionCore} die getragene
 * Strom-Ruestung, Rechtsklick auf einen Block setzt das Ladepad.
 */
public class ItemHevBattery extends BlockItem {

    private static final int CHARGE = 150000;

    public ItemHevBattery(Block block, Properties properties) {
        super(block, properties);
    }

    /** 1:1 {@code ItemFusionCore.onItemRightClick}. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (ModArmorFSB.hasFSBArmorIgnoreCharge(player) && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorFSBPowered) {

            for (ItemStack st : player.getInventory().armor) {
                if (st.isEmpty())
                    continue;

                if (st.getItem() instanceof IBatteryItem battery) {
                    long maxcharge = battery.getMaxCharge(st);
                    long current = battery.getCharge(st);
                    long newcharge = Math.min(current + CHARGE, maxcharge);
                    battery.setCharge(st, newcharge);
                }
            }

            stack.shrink(1);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.battery"), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Charges all worn armor pieces by " + BobMathUtil.getShortNumber(CHARGE) + "HE").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("[Requires full electric set to be worn]"));
    }
}
