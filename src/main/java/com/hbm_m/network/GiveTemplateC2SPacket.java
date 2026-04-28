package com.hbm_m.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.network.C2SPacket;
import com.hbm_m.util.TemplateCraftingCosts;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class GiveTemplateC2SPacket implements C2SPacket {

    private final ItemStack recipeOutput;

    public GiveTemplateC2SPacket(ItemStack recipeOutput) {
        this.recipeOutput = recipeOutput;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static GiveTemplateC2SPacket decode(FriendlyByteBuf buf) {
        return new GiveTemplateC2SPacket(buf.readItem());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeItem(recipeOutput);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(GiveTemplateC2SPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            if (player.isCreative()) handleCreativeMode(player, msg.recipeOutput);
            else                     handleSurvivalMode(player, msg.recipeOutput);
        });
    }

    // ── Логика креатива ───────────────────────────────────────────────────────

    private static void handleCreativeMode(ServerPlayer player, ItemStack recipeOutput) {
        if (isStamp(recipeOutput)) {
            player.getInventory().add(recipeOutput.copy());
        } else {
            ItemStack newTemplate = new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get());
            ItemAssemblyTemplate.writeRecipeOutput(newTemplate, recipeOutput);
            player.getInventory().add(newTemplate);
        }
    }

    // ── Логика выживания ──────────────────────────────────────────────────────

    private static void handleSurvivalMode(ServerPlayer player, ItemStack recipeOutput) {
        if (isStamp(recipeOutput)) handleStampCrafting(player, recipeOutput);
        else                       handleTemplateCrafting(player, recipeOutput);
    }

    private static void handleStampCrafting(ServerPlayer player, ItemStack recipeOutput) {
        NonNullList<Ingredient> cost = TemplateCraftingCosts.getCostForStamp(recipeOutput);
        if (cost == null) return;

        if (tryConsume(player, cost)) {
            ItemStack resultStamp = recipeOutput.copy();
            resultStamp.setCount(1);
            player.getInventory().add(resultStamp);
        }
    }

    private static void handleTemplateCrafting(ServerPlayer player, ItemStack recipeOutput) {
        NonNullList<Ingredient> cost = TemplateCraftingCosts.getCostForTemplate(recipeOutput);
        if (cost == null) return;

        if (tryConsume(player, cost)) {
            ItemStack newTemplate = new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get());
            ItemAssemblyTemplate.writeRecipeOutput(newTemplate, recipeOutput);
            player.getInventory().add(newTemplate);
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private static boolean tryConsume(ServerPlayer player, NonNullList<Ingredient> cost) {
        List<ItemStack> simulatedInventory = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            simulatedInventory.add(stack.copy());
        }

        // Симуляция
        ingredientLoop:
        for (Ingredient ingredient : cost) {
            for (ItemStack stackInSlot : simulatedInventory) {
                if (ingredient.test(stackInSlot)) {
                    stackInSlot.shrink(1);
                    continue ingredientLoop;
                }
            }
            return false; // Не хватает ресурсов
        }

        // Реальное списание
        for (Ingredient ingredient : cost) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stackInSlot = player.getInventory().getItem(i);
                if (ingredient.test(stackInSlot)) {
                    stackInSlot.shrink(1);
                    break;
                }
            }
        }
        return true;
    }

    private static boolean isStamp(ItemStack stack) {
        return TemplateCraftingCosts.isStamp(stack);
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(ItemStack recipeOutput) {
        ModPacketHandler.sendToServer(ModPacketHandler.GIVE_TEMPLATE,
                new GiveTemplateC2SPacket(recipeOutput));
    }
}