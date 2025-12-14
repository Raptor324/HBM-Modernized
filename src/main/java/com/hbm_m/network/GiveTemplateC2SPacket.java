package com.hbm_m.network;

import com.hbm_m.item.custom.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.TemplateCraftingCosts;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GiveTemplateC2SPacket {

    private final ItemStack recipeOutput;

    public GiveTemplateC2SPacket(ItemStack recipeOutput) {
        this.recipeOutput = recipeOutput;
    }

    public GiveTemplateC2SPacket(FriendlyByteBuf buf) {
        this.recipeOutput = buf.readItem();
    }

    public static void encode(GiveTemplateC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeItem(packet.recipeOutput);
    }

    public static GiveTemplateC2SPacket decode(FriendlyByteBuf buf) {
        return new GiveTemplateC2SPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (player.isCreative()) {
                // ЛОГИКА ДЛЯ КРЕАТИВА
                handleCreativeMode(player);
            } else {
                // ЛОГИКА ДЛЯ ВЫЖИВАНИЯ
                handleSurvivalMode(player);
            }
        });
        return true;
    }

    private void handleCreativeMode(ServerPlayer player) {
        // Проверяем, является ли это штампом
        if (isStamp(this.recipeOutput)) {
            // В креативе просто выдаем штамп
            player.getInventory().add(this.recipeOutput.copy());
        } else {
            // Обычная логика для шаблонов
            ItemStack newTemplate = new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get());
            ItemAssemblyTemplate.writeRecipeOutput(newTemplate, this.recipeOutput);
            player.getInventory().add(newTemplate);
        }
    }

    private void handleSurvivalMode(ServerPlayer player) {
        // Проверяем, является ли это штампом
        if (isStamp(this.recipeOutput)) {
            // Логика для штампов - требуется плоский штамп
            handleStampCrafting(player);
        } else {
            // Обычная логика для шаблонов с проверкой ресурсов
            handleTemplateCrafting(player);
        }
    }

    private void handleStampCrafting(ServerPlayer player) {
        // Получаем стоимость штампа из TemplateCraftingCosts
        NonNullList<Ingredient> cost = TemplateCraftingCosts.getCostForStamp(this.recipeOutput);
        if (cost == null) return; // Если стоимость не определена - ничего не делаем

        // Этап 1: Проверка наличия ресурсов (плоского штампа)
        List<ItemStack> simulatedInventory = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            simulatedInventory.add(stack.copy());
        }

        boolean canCraft = true;
        ingredientLoop:
        for (Ingredient ingredient : cost) {
            for (ItemStack stackInSlot : simulatedInventory) {
                if (ingredient.test(stackInSlot)) {
                    stackInSlot.shrink(1);
                    continue ingredientLoop;
                }
            }
            canCraft = false;
            break;
        }

        // Этап 2: Списание плоского штампа и выдача нужного штампа
        if (canCraft) {
            for (Ingredient ingredient : cost) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stackInSlot = player.getInventory().getItem(i);
                    if (ingredient.test(stackInSlot)) {
                        stackInSlot.shrink(1);
                        break;
                    }
                }
            }

            // Выдаем нужный штамп
            ItemStack resultStamp = this.recipeOutput.copy();
            resultStamp.setCount(1);
            player.getInventory().add(resultStamp);
        }
    }

    private void handleTemplateCrafting(ServerPlayer player) {
        NonNullList<Ingredient> cost = TemplateCraftingCosts.getCostForTemplate(this.recipeOutput);
        if (cost == null) return; // Если стоимость не найдена - ничего не делаем

        // Этап 1: Проверка наличия ресурсов
        List<ItemStack> simulatedInventory = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            simulatedInventory.add(stack.copy());
        }

        boolean canCraft = true;
        ingredientLoop:
        for (Ingredient ingredient : cost) {
            for (ItemStack stackInSlot : simulatedInventory) {
                if (ingredient.test(stackInSlot)) {
                    stackInSlot.shrink(1);
                    continue ingredientLoop;
                }
            }
            canCraft = false;
            break;
        }

        // Этап 2: Списание и выдача
        if (canCraft) {
            for (Ingredient ingredient : cost) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stackInSlot = player.getInventory().getItem(i);
                    if (ingredient.test(stackInSlot)) {
                        stackInSlot.shrink(1);
                        break;
                    }
                }
            }

            ItemStack newTemplate = new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get());
            ItemAssemblyTemplate.writeRecipeOutput(newTemplate, this.recipeOutput);
            player.getInventory().add(newTemplate);
        }
    }

    /**
     * Проверяет, является ли предмет штампом пресса
     */
    private boolean isStamp(ItemStack stack) {
        return TemplateCraftingCosts.isStamp(stack);
    }
}