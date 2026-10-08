package com.hbm_m.item.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemAmmoContainer}: gibt fuer bis zu drei Waffen im Inventar die Standardmunition.
 * Meta 1 (Behelfskiste, Textur {@code ammo_container_alt}) ist im Port {@code ammo_container_1}:
 * halbe Menge, keine teure Munition.
 */
public class ItemAmmoContainer extends Item implements ITooltipProvider {

    private final boolean makeshift;

    public ItemAmmoContainer(boolean makeshift, Properties properties) {
        super(properties.stacksTo(1));
        this.makeshift = makeshift;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Im Original laeuft das beidseitig; Inventar wird nur serverseitig veraendert (sonst Geisterstapel).
        if (world.isClientSide) return InteractionResultHolder.success(stack);

        List<ItemStack> stacks = new ArrayList<>();

        for (ItemStack inv : player.getInventory().items) {
            if (!inv.isEmpty() && inv.getItem() instanceof ItemGunBaseNT gun) {
                if (!gun.defaultAmmo.isEmpty() && !(makeshift && gun.isDefaultExpensive)) stacks.add(inv);
            }
        }

        if (stacks.size() <= 0) return InteractionResultHolder.pass(stack);

        Collections.shuffle(stacks);

        int maxGunCount = 3;

        for (int i = 0; i < maxGunCount && i < stacks.size(); i++) {
            ItemStack gunStack = stacks.get(i);
            ItemGunBaseNT gun = (ItemGunBaseNT) gunStack.getItem();
            ItemStack ammo = gun.defaultAmmo.copy();
            if (makeshift) ammo.setCount((int) Math.ceil(ammo.getCount() / 2D));
            player.getInventory().add(ammo);
            if (!ammo.isEmpty()) player.drop(ammo, false);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.unpack"), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.inventoryMenu.broadcastChanges();
        stack.shrink(1);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        // I18nUtil.resolveKeyArray(name + (meta == 1 ? ".1" : "") + ".desc")
        String key = "item.hbm_m.ammo_container" + (makeshift ? ".1" : "") + ".desc";
        for (String line : Component.translatable(key).getString().split("\\$")) list.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
    }
}
