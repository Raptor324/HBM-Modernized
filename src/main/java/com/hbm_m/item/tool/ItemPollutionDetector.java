package com.hbm_m.item.tool;

import com.hbm_m.handler.pollution.PollutionData;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.network.InfoToastPacket;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.tool.ItemPollutionDetector}: zeigt alle 10 Ticks Russ/Gift/Schwermetall am Spieler an. */
public class ItemPollutionDetector extends Item {

    public ItemPollutionDetector(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof ServerPlayer player) || world.getGameTime() % 10 != 0) return;

        PollutionData data = PollutionHandler.getPollutionData(world, Mth.floor(entity.getX()), Mth.floor(entity.getY()), Mth.floor(entity.getZ()));
        if (data == null) data = new PollutionData();

        float soot = data.pollution[PollutionType.SOOT.ordinal()];
        float poison = data.pollution[PollutionType.POISON.ordinal()];
        float heavymetal = data.pollution[PollutionType.HEAVYMETAL.ordinal()];

        soot = ((int) (soot * 100)) / 100F;
        poison = ((int) (poison * 100)) / 100F;
        heavymetal = ((int) (heavymetal * 100)) / 100F;

        // PlayerInformPacket(..., id, 4000 ms)
        InfoToastPacket.sendTo(player, Component.translatable("pollution.soot").append(": " + soot).withStyle(ChatFormatting.YELLOW), 80, 100, 0xFFFFFF);
        InfoToastPacket.sendTo(player, Component.translatable("pollution.poison").append(": " + poison).withStyle(ChatFormatting.YELLOW), 80, 101, 0xFFFFFF);
        InfoToastPacket.sendTo(player, Component.translatable("pollution.heavymetal").append(": " + heavymetal).withStyle(ChatFormatting.YELLOW), 80, 102, 0xFFFFFF);
    }
}
