package com.hbm_m.item.custom.industrial;

// Предмет-папка для хранения и управления шаблонами мультиблоков.
// При использовании открывает GUI с возможностью получать шаблоны.

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hbm_m.client.overlay.GUITemplateFolder;

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
import net.minecraftforge.api.distmarker.OnlyIn; // OHIO

public class ItemTemplateFolder extends Item {
    public ItemTemplateFolder(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level pLevel, @Nonnull Player pPlayer, @Nonnull InteractionHand pUsedHand) {
        ItemStack itemStack = pPlayer.getItemInHand(pUsedHand);

        // GUI открывается только на клиенте
        if (pLevel.isClientSide()) {
            openScreen(pPlayer, itemStack);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, pLevel.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private void openScreen(Player player, ItemStack stack) {
        Minecraft.getInstance().setScreen(new GUITemplateFolder(stack));
    }
    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel, @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
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