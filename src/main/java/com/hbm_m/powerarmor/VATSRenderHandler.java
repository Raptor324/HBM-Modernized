package com.hbm_m.powerarmor;

import org.joml.Matrix4f;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VATSRenderHandler {

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || entity == player) return;

        // VATS должен быть активен
        if (!ModEventHandlerClient.isVATSActive()) return;

        // Игрок должен носить FSB броню с VATS (как у тебя в логике)
        if (!ModPowerArmorItem.hasFSBArmor(player)) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ModPowerArmorItem armorItem) || !armorItem.getSpecs().hasVats) return;

        // В радиусе
        int chunks = ModClothConfig.get().vatsRenderDistanceChunks;

        double maxDistBlocks = chunks * 16.0D;
        double maxDistSqr = maxDistBlocks * maxDistBlocks;

        if (player.distanceToSqr(entity) > maxDistSqr) return;


        // Считаем заполнение (1 полоска = 1 HP)
        float maxHp = entity.getMaxHealth();
        if (maxHp <= 0.0F) return;

        int segments = Math.max(1, Mth.ceil(maxHp)); // сколько хп, столько и полосок (округление вверх)
        float hp = Mth.clamp(entity.getHealth(), 0.0F, maxHp);

        // сколько "полных" HP осталось -> столько красных полосок
        int filled = Mth.clamp(Mth.floor(hp + 1.0e-4F), 0, segments);

        Component bar = Component.literal(repeat('|', filled)).withStyle(ChatFormatting.RED)
            .append(Component.literal(repeat('|', segments - filled)).withStyle(ChatFormatting.DARK_GRAY));

        // Рисуем над головой как nameplate
        PoseStack ps = event.getPoseStack();
        ps.pushPose();

        ps.translate(0.0D, entity.getBbHeight() + 0.6D, 0.0D);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.025F, -0.025F, 0.025F);

        Font font = mc.font;
        float x = -font.width(bar) / 2.0F;

        Matrix4f mat = ps.last().pose();
        font.drawInBatch(
                bar,
                x, 0.0F,
                0xFFFFFF,
                false,
                mat,
                event.getMultiBufferSource(),
                Font.DisplayMode.NORMAL,
                0,
                event.getPackedLight()
        );

        ps.popPose();
    }

    private static String repeat(char c, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }
}
