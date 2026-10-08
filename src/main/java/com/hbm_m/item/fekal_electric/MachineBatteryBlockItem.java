package com.hbm_m.item.fekal_electric;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.platform.PlatformHooks;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class MachineBatteryBlockItem extends BlockItem implements ITooltipProvider {

    private final long maxPower; // Меняем int на long

    public MachineBatteryBlockItem(Block pBlock, Properties pProperties, long maxPower) {
        super(pBlock, pProperties);
        this.maxPower = maxPower;
    }

    @Override
    public void appendHbmTooltip(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
        // 1:1 Original MachineBattery.addInformation (IPersistentInfoProvider): alle vier Zeilen nur, wenn der
        // Gegenstand gespeicherte Blockdaten traegt (ItemBlockBase prueft NBT_PERSISTENT_KEY).
        // Читаем энергию из NBT
        if (PlatformHooks.hasItemTag(pStack)) {
            // BlockEntityTag на 1.20.1 — NBT-подтег (getTagElement); на 1.21.1 — тот же ключ
            // "BlockEntityTag" внутри CUSTOM_DATA (BlockItem пишет туда BE-данные при placement-копии).
            //? if < 1.21.1 {
            CompoundTag blockEntityTag = pStack.getTagElement("BlockEntityTag");
            //?} else {
            /*CompoundTag custom = PlatformHooks.getItemTag(pStack);
            CompoundTag blockEntityTag = custom != null && custom.contains("BlockEntityTag")
                    ? custom.getCompound("BlockEntityTag") : null;
            *///?}
            // Ladung liegt wie in der Blockentitaet unter "energy" (IPersistentNBT.writeNBT); "Energy" = alte Drops.
            if (blockEntityTag != null) {
                long energy = 0;
                if (blockEntityTag.contains("energy")) {
                    energy = blockEntityTag.getLong("energy");
                } else if (blockEntityTag.contains("Energy")) {
                    energy = blockEntityTag.getLong("Energy");
                }

                pTooltip.add(Component.literal("Stores up to " + com.hbm_m.util.BobMathUtil.getShortNumber(maxPower) + "HE").withStyle(ChatFormatting.GOLD));
                pTooltip.add(Component.literal("Charge speed: " + com.hbm_m.util.BobMathUtil.getShortNumber(maxPower / 200) + "HE").withStyle(ChatFormatting.GOLD));
                pTooltip.add(Component.literal("Discharge speed: " + com.hbm_m.util.BobMathUtil.getShortNumber(maxPower / 600) + "HE").withStyle(ChatFormatting.GOLD));
                pTooltip.add(Component.literal(com.hbm_m.util.BobMathUtil.getShortNumber(energy) + "/" + com.hbm_m.util.BobMathUtil.getShortNumber(maxPower) + "HE").withStyle(ChatFormatting.YELLOW));
            }
        }

    }
}