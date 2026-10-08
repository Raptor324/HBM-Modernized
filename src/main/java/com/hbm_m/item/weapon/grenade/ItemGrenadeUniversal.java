package com.hbm_m.item.weapon.grenade;

import com.hbm_m.platform.StackNbt;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.grenade.EntityGrenadeUniversal;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.IAnimatedItem;
import com.hbm_m.item.IEquipReceiver;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.network.HbmAnimationPacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationKeyframe.IType;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemGrenadeUniversal}: Baukastengranate (Huelle + Fuellung + Zuender + optionales Extra) im NBT. */
public class ItemGrenadeUniversal extends Item implements IEquipReceiver, IAnimatedItem<GunAnimation> {

    /*
     *  __________
     * | ________ | ______ SHELL - determines what filling can be used, various bonuses like throw distance and fragmentation, max stack size
     * ||        ||
     * ||       __________ FILLING - the bang - high explosive, fragmentation, incendiary, etc
     * ||________||
     *  \   /\   /
     *   \_ || _/
     *    | || |
     *    | |_____________ FUZE - what triggers the explosive, timed, impact, or airburst
     *    | || |
     *    | || | _________ EXTRA - optional bonus like additional fuzes, special explosion effects, glue for sticky bombs, et cetera
     *    / || \
     *   |__||__|
     *     {__}
     */

    public static final String KEY_SHELL = "shell";
    public static final String KEY_FILLING = "filling";
    public static final String KEY_FUZE = "fuze";
    public static final String KEY_EXTRA = "extra";

    public ItemGrenadeUniversal(Properties props) {
        super(props.stacksTo(4));
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getShell(stack).getStackLimit();
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        HbmPlayerProps.getData(player).grenadeDeployment = 0;

        if (player instanceof ServerPlayer sp) {
            ModPacketHandler.sendToPlayer(sp, ModPacketHandler.HBM_ANIMATION, new HbmAnimationPacket(GunAnimation.EQUIP.ordinal(), 0, 0));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        EnumGrenadeShell shell = getShell(stack);
        if (HbmPlayerProps.getData(player).grenadeDeployment >= shell.getDrawDuration()) {
            if (!world.isClientSide) {
                EntityGrenadeUniversal grenade = new EntityGrenadeUniversal(world, player, stack);
                world.addFreshEntity(grenade);
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (stack.getCount() > 0) this.onEquip(player, stack);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isHeld) {

        if (!(entity instanceof LivingEntity)) return;
        Player player = entity instanceof Player p ? p : null;
        isHeld = isHeld && player != null && player.getMainHandItem() == stack;

        if (player != null) {
            boolean wasHeld = ItemGunBaseNT.getIsEquipped(stack);

            if (!wasHeld && isHeld) {
                this.onEquip(player, stack);
            } else if (isHeld) {
                HbmPlayerProps.getData(player).grenadeDeployment++;
                int deployment = HbmPlayerProps.getData(player).grenadeDeployment;
                EnumGrenadeShell shell = getShell(stack);
                if (shell == EnumGrenadeShell.FRAG && deployment == 18) {
                    playSoundAtEntity(world, player, "hbm:weapon.reload.revolverCock", 1F, 1F);
                }
                if (shell == EnumGrenadeShell.STICK) {
                    if (deployment == 16) playSoundAtEntity(world, player, "hbm:weapon.reload.boltOpen", 1F, 1.25F);
                    if (deployment == 25) playSoundAtEntity(world, player, "hbm:weapon.reload.boltOpen", 1F, 1.25F);
                }
                if (shell == EnumGrenadeShell.TECH && deployment == 18) {
                    playSoundAtEntity(world, player, "hbm:weapon.reload.grenadeTech", 1F, 1F);
                }
                if (shell == EnumGrenadeShell.NUKE && deployment == 26) {
                    playSoundAtEntity(world, player, "hbm:weapon.reload.grenadeNuka", 1F, 1F);
                }
            }
        }

        ItemGunBaseNT.setIsEquipped(stack, isHeld);
    }

    /** Original {@code world.playSoundAtEntity}: serverseitig fuer alle in der Naehe. */
    private static void playSoundAtEntity(Level world, Player player, String sound, float volume, float pitch) {
        if (!world.isClientSide) world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get(sound), SoundSource.PLAYERS, volume, pitch);
    }

    private static <E extends Enum<E>> E grab(Class<E> clazz, int i) {
        E[] values = clazz.getEnumConstants();
        return values[Math.abs(i) % values.length];
    }

    public static EnumGrenadeShell getShell(@Nullable ItemStack stack) {
        if (stack == null || !StackNbt.has(stack)) return EnumGrenadeShell.FRAG;
        return grab(EnumGrenadeShell.class, StackNbt.read(stack).getInt(KEY_SHELL));
    }

    public static EnumGrenadeFilling getFilling(@Nullable ItemStack stack) {
        if (stack == null || !StackNbt.has(stack)) return EnumGrenadeFilling.HE;
        return grab(EnumGrenadeFilling.class, StackNbt.read(stack).getInt(KEY_FILLING));
    }

    public static EnumGrenadeFuze getFuze(@Nullable ItemStack stack) {
        if (stack == null || !StackNbt.has(stack)) return EnumGrenadeFuze.S3;
        return grab(EnumGrenadeFuze.class, StackNbt.read(stack).getInt(KEY_FUZE));
    }

    @Nullable
    public static EnumGrenadeExtra getExtra(@Nullable ItemStack stack) {
        if (stack == null || !StackNbt.has(stack) || !StackNbt.read(stack).contains(KEY_EXTRA)) return null;
        return grab(EnumGrenadeExtra.class, StackNbt.read(stack).getInt(KEY_EXTRA));
    }

    public static ItemStack make(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze) { return make(shell, filling, fuze, null, 1); }
    public static ItemStack make(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze, @Nullable EnumGrenadeExtra extra) { return make(shell, filling, fuze, extra, 1); }

    public static ItemStack make(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze, @Nullable EnumGrenadeExtra extra, int amount) {
        ItemStack stack = new ItemStack(GrenadeItems.GRENADE_UNIVERSAL.get(), amount);
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_SHELL, shell.ordinal());
        tag.putInt(KEY_FILLING, filling.ordinal());
        tag.putInt(KEY_FUZE, fuze.ordinal());
        if (extra != null) tag.putInt(KEY_EXTRA, extra.ordinal());
        StackNbt.set(stack, tag);
        return stack;
    }

    /** Original {@code getSubItems}: alle gueltigen Kombinationen (Kreativ-Tab). */
    public static void addSubItems(List<ItemStack> list) {
        for (EnumGrenadeShell shell : EnumGrenadeShell.values()) for (EnumGrenadeFilling filling : EnumGrenadeFilling.values()) {
            if (filling.compatibleShells.contains(shell)) for (EnumGrenadeFuze fuze : EnumGrenadeFuze.values()) {
                list.add(make(shell, filling, fuze));
                for (EnumGrenadeExtra extra : EnumGrenadeExtra.values()) list.add(make(shell, filling, fuze, extra));
            }
        }
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.translatable("item.hbm_m.grenade_shell_" + getShell(stack).name().toLowerCase(Locale.US)).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("item.hbm_m.grenade_filling_" + getFilling(stack).name().toLowerCase(Locale.US)).withStyle(ChatFormatting.YELLOW));
        list.add(Component.translatable("item.hbm_m.grenade_fuze_" + getFuze(stack).name().toLowerCase(Locale.US)).withStyle(ChatFormatting.YELLOW));
        EnumGrenadeExtra extra = getExtra(stack);
        if (extra != null) list.add(Component.translatable("item.hbm_m.grenade_extra_" + extra.name().toLowerCase(Locale.US)).withStyle(ChatFormatting.RED));
    }

    @Override
    public BusAnimation getAnimation(GunAnimation type, ItemStack stack) {
        if (type != GunAnimation.EQUIP) return null;
        EnumGrenadeShell shell = getShell(stack);

        if (shell == EnumGrenadeShell.FRAG) {
            return new BusAnimation()
                    .addBus("BODYMOVE", new BusAnimationSequence().setPos(0, -5, 0).addPos(0, -3, 0, 350).addPos(0, 0, 0, 350, IType.SIN_DOWN))
                    .addBus("BODYTURN", new BusAnimationSequence().addPos(0, 0, 45, 350).addPos(0, 0, -15, 350, IType.SIN_DOWN).hold(200).addPos(0, 0, -20, 100, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("RINGMOVE", new BusAnimationSequence().hold(900).addPos(0, 0, 1, 150).addPos(0, -3, 3, 300))
                    .addBus("RINGTURN", new BusAnimationSequence().hold(900).addPos(0, 0, 45, 300))
                    .addBus("RENDERRING", new BusAnimationSequence().setPos(1, 1, 1).hold(1350).setPos(0, 0, 0));
        }

        if (shell == EnumGrenadeShell.STICK) {
            return new BusAnimation()
                    .addBus("BODYMOVE", new BusAnimationSequence().setPos(0, -7, 0).addPos(0, 3, 0, 750, IType.SIN_DOWN).holdUntil(1900).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BODYTURN", new BusAnimationSequence().setPos(0, 0, 90).addPos(0, 0, -45, 750, IType.SIN_DOWN).holdUntil(1900).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("RINGMOVE", new BusAnimationSequence().hold(800).addPos(0, -0.25, 0, 200, IType.SIN_FULL).hold(250).addPos(0, -0.5, 0, 200, IType.SIN_FULL).addPos(2, -5, 0, 350, IType.SIN_UP))
                    .addBus("RINGTURN", new BusAnimationSequence().hold(800).addPos(0, 360, 0, 200, IType.SIN_FULL).hold(250).addPos(0, 360 * 2, 0, 200, IType.SIN_FULL))
                    .addBus("RENDERRING", new BusAnimationSequence().setPos(1, 1, 1).hold(2100).setPos(0, 0, 0));
        }

        if (shell == EnumGrenadeShell.TECH) {
            return new BusAnimation()
                    .addBus("BODYMOVE", new BusAnimationSequence().setPos(0, -5, 0).addPos(0, -3, 0, 350).addPos(0, 0, 0, 350, IType.SIN_DOWN))
                    .addBus("BODYTURN", new BusAnimationSequence().addPos(0, 0, 45, 350).addPos(0, 0, -15, 350, IType.SIN_DOWN).hold(200).addPos(0, 0, -20, 100, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("RINGMOVE", new BusAnimationSequence().hold(900).addPos(0, 0, 1, 150).addPos(0, -3, 3, 300))
                    .addBus("RINGTURN", new BusAnimationSequence().hold(900).addPos(0, 0, 45, 300))
                    .addBus("RENDERRING", new BusAnimationSequence().setPos(1, 1, 1).hold(1350).setPos(0, 0, 0));
        }

        if (shell == EnumGrenadeShell.NUKE) {
            return new BusAnimation()
                    .addBus("BODYMOVE", new BusAnimationSequence().setPos(0, -5, 0).hold(250).addPos(0, 0, 0, 850, IType.SIN_DOWN))
                    .addBus("BODYTURN", new BusAnimationSequence().setPos(0, 0, 90).hold(250).addPos(0, 0, -25, 850, IType.SIN_DOWN).hold(200).addPos(0, 0, -30, 100, IType.SIN_DOWN).addPos(0, 0, 0, 750, IType.SIN_FULL))
                    .addBus("RINGMOVE", new BusAnimationSequence().hold(1300).addPos(0, 0, 1, 150).addPos(0, -3, 3, 300))
                    .addBus("RINGTURN", new BusAnimationSequence().hold(1300).addPos(0, 0, 720, 500)) // SPEEN
                    .addBus("RENDERRING", new BusAnimationSequence().setPos(1, 1, 1).hold(1750).setPos(0, 0, 0));
        }

        return null;
    }

    /** Original {@code ClientProxy}: {@code registerItemRenderer(grenade_universal, new ItemRenderGrenade())}. */
    //? if forge {
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }
    //?} elif neoforge {
    /*@Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.weapon.GunItemRenderer.INSTANCE;
            }
        });
    }
    *///?}

    @Override public boolean shouldPlayerModelAim(ItemStack stack) { return false; }
    @Override public Class<GunAnimation> getEnum() { return GunAnimation.class; }
}
