package com.hbm_m.item.tool;

import java.util.List;

import com.hbm_m.interfaces.ICopiable;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.network.CopyToolKeyState;
import com.hbm_m.network.InfoToastPacket;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;

/**
 * Порт com.hbm.items.tool.ItemSettingsTool (1.7.10) - "Устройство настройки".
 *
 * <p>Shift+ПКМ по машине - скопировать настройки ({@link ICopiable#getSettings}),
 * ПКМ - вставить ({@link ICopiable#pasteSettings}, индекс параметра выбирается
 * зажатой ALT). Пока предмет в руке, список вставляемых параметров рисуется на
 * экране через {@code OverlayInfoToast} (оригинальный RenderInfoSystem): выбранный
 * параметр голубым, остальные жёлтым.
 *
 * <p>Взаимодействие идёт через {@code onItemUseFirst} - как в оригинале, хук
 * срабатывает ДО активации блока, поэтому GUI машины не открывается. Хук есть
 * на обоих таргетах (IForgeItem / IItemExtension), код общий.
 */
public class ItemSettingsTool extends Item implements ITooltipProvider {

    /** TTL строк информера: оригинал - 4000 мс = 80 тиков. */
    private static final int INFO_TTL_TICKS = 80;
    /** Идентификаторы строк информера: оригинал использовал 897 + j. */
    private static final int INFO_BASE_ID = 897;
    /** Дебаунс листания индекса (оригинальный inputDelay > 4). */
    private static final int CYCLE_COOLDOWN_TICKS = 5;

    private static final int COLOR_SELECTED = 0x55FFFF; // AQUA
    private static final int COLOR_IDLE     = 0xFFFF55; // YELLOW

    public ItemSettingsTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ICopiable copiable)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            CompoundTag settings = copiable.getSettings(level, pos);
            if (settings != null) {
                settings.putString("tileName", copiable.getSettingsSourceID());
                settings.putInt("copyIndex", 0);
                settings.putInt("inputDelay", 0);
                String[] info = copiable.infoForDisplay(level, pos);
                if (info != null) {
                    ListTag displayInfo = new ListTag();
                    for (String str : info) {
                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("info", str);
                        displayInfo.add(nbt);
                    }
                    settings.put("displayInfo", displayInfo);
                }
                PlatformHooks.setItemTag(stack, settings);

                if (level.isClientSide) {
                    player.displayClientMessage(chatMessage(copiable, "chat.hbm_m.settings_tool.copied", ChatFormatting.AQUA), false);
                }
            } else {
                if (level.isClientSide) {
                    player.displayClientMessage(chatMessage(copiable, "chat.hbm_m.settings_tool.copy_failed", ChatFormatting.RED), false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());

        } else if (PlatformHooks.hasItemTag(stack)) {
            CompoundTag nbt = PlatformHooks.getItemTag(stack);
            int index = nbt.getInt("copyIndex");
            copiable.pasteSettings(nbt, index, level, player, pos);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Оригинал: на ICopiable-машине без тега сервер всё равно поглощал клик
        // (return !world.isRemote), GUI не открывался - поведение сохранено.
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static Component chatMessage(ICopiable copiable, String key, ChatFormatting color) {
        return Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.translatable("item.hbm_m.settings_tool").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.translatable(key, copiable.getSettingsSourceDisplay()).withStyle(color));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !selected || !(entity instanceof ServerPlayer player)) return;

        CompoundTag tag = PlatformHooks.getItemTag(stack);
        if (tag == null) return;
        ListTag displayInfo = tag.getList("displayInfo", Tag.TAG_COMPOUND);

        CopyToolKeyState.Keys keys = CopyToolKeyState.get(player.getUUID());
        if (keys.alt && !displayInfo.isEmpty()) {
            int cooldown = CopyToolKeyState.getCooldown(player.getUUID());
            if (cooldown > 0) {
                CopyToolKeyState.setCooldown(player.getUUID(), cooldown - 1);
            } else {
                int index = tag.getInt("copyIndex") + 1;
                if (index > displayInfo.size() - 1) index = 0;
                final int idx = index;
                PlatformHooks.editItemTag(stack, t -> t.putInt("copyIndex", idx));
                CopyToolKeyState.setCooldown(player.getUUID(), CYCLE_COOLDOWN_TICKS);
            }
        }

        if (displayInfo.isEmpty()) return;
        if (level.getGameTime() % 5 != 0) return;

        int selectedIndex = tag.getInt("copyIndex");
        for (int j = 0; j < displayInfo.size(); j++) {
            String key = displayInfo.getCompound(j).getString("info");
            if (key.isEmpty()) continue;
            int rgb = selectedIndex == j ? COLOR_SELECTED : COLOR_IDLE;
            InfoToastPacket.sendTo(player, key, INFO_TTL_TICKS, INFO_BASE_ID + j, rgb);
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hbm_m.settings_tool.1"));
        tooltip.add(Component.translatable("tooltip.hbm_m.settings_tool.2"));
        tooltip.add(Component.translatable("tooltip.hbm_m.settings_tool.3"));
        if (PlatformHooks.hasItemTag(stack)) {
            CompoundTag nbt = PlatformHooks.getItemTag(stack);
            if (nbt != null && nbt.contains("tileName")) {
                tooltip.add(Component.translatable(nbt.getString("tileName")).withStyle(ChatFormatting.BLUE));
            } else {
                tooltip.add(Component.translatable("tooltip.hbm_m.settings_tool.none").withStyle(ChatFormatting.RED));
            }
        }
    }
}
