package com.hbm_m.item.industrial;


import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.gui.GUITemplateFolder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
// Предмет-папка для хранения и управления шаблонами мультиблоков.
// При использовании открывает GUI с возможностью получать шаблоны.

public class ItemTemplateFolder extends Item {
    public ItemTemplateFolder(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level pLevel, @NotNull Player pPlayer, @NotNull InteractionHand pUsedHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pUsedHand);

        // GUI открывается только на клиенте
        if (pLevel.isClientSide()) {
            openScreen(pPlayer, itemStack);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, pLevel.isClientSide());
    }
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
    private void openScreen(Player player, ItemStack stack) {
        Minecraft.getInstance().setScreen(new GUITemplateFolder(stack));
    }
    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        // Получаем нашу строку из файла локализации
            String fullDesc = Component.translatable("item.hbm_m.template_folder.desc").getString();
            
            // Разделяем строку по символу '$'
            String[] lines = fullDesc.split("\\$");

            // Добавляем каждую строку как отдельный компонент
            for (String line : lines) {
                pTooltipComponents.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
    }
}