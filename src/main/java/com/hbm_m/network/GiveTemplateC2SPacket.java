package com.hbm_m.network;

import com.hbm_m.item.ItemAssemblyTemplate;
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

    /**
     * Конструктор для создания пакета ПЕРЕД отправкой.
     * @param recipeOutput Данные, которые мы хотим отправить.
     */
    public GiveTemplateC2SPacket(ItemStack recipeOutput) {
        this.recipeOutput = recipeOutput;
    }

    /**
     * Конструктор, который вызывается декодером на принимающей стороне.
     * Он читает данные из буфера и заполняет поля.
     * @param buf Буфер с входящими данными.
     */
    public GiveTemplateC2SPacket(FriendlyByteBuf buf) {
        this.recipeOutput = buf.readItem();
    }

    /**
     * Кодировщик: записывает данные из полей пакета в буфер.
     */
    public static void encode(GiveTemplateC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeItem(packet.recipeOutput);
    }

    /**
     * Декодер: создает новый экземпляр пакета, используя конструктор,
     * который принимает FriendlyByteBuf.
     */
    public static GiveTemplateC2SPacket decode(FriendlyByteBuf buf) {
        return new GiveTemplateC2SPacket(buf);
    }

    /**
     * Обработчик: выполняет логику пакета на сервере.
     */
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // --- НОВАЯ, РАЗДЕЛЕННАЯ ЛОГИКА ---

            if (player.isCreative()) {
                // --- ЛОГИКА ДЛЯ КРЕАТИВА ---
                // Просто создаем и выдаем шаблон, никаких проверок.
                ItemStack newTemplate = new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get());
                ItemAssemblyTemplate.writeRecipeOutput(newTemplate, this.recipeOutput);
                player.getInventory().add(newTemplate);

            } else {
                // --- ЛОГИКА ДЛЯ ВЫЖИВАНИЯ (ваш старый код) ---
                NonNullList<Ingredient> cost = TemplateCraftingCosts.getCostFor(this.recipeOutput);
                if (cost == null) return;

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
        });
        return true;
    }
}