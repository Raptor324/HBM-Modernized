package com.hbm_m.item.tool;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.item.IBatteryItem;
import com.hbm_m.item.ITooltipProvider;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.tool.ItemFusionCore}: laedt alle getragenen Akku-Ruestungsteile (volles Elektro-Set). */
public class ItemFusionCore extends Item implements ITooltipProvider {

    private final int charge;

    public ItemFusionCore(int charge, Properties properties) {
        super(properties);
        this.charge = charge;
    }

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
                    long newcharge = Math.min(current + this.charge, maxcharge);
                    battery.setCharge(st, newcharge);
                }
            }

            stack.shrink(1);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.battery"), SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Charges all worn armor pieces by " + BobMathUtil.getShortNumber(charge) + "HE").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("[Requires full electric set to be worn]"));
    }
}
