package com.hbm_m.item.weapon;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityCloudFleijaRainbow;
import com.hbm_m.entity.logic.EntityNukeExplosionMK3;
import com.hbm_m.entity.projectile.EntityB92Beam;
import com.hbm_m.main.Polaroid;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code GunB92} (Altwaffe, "Star Blaster"): Rechtsklick mit Ladung = Bogen-Spannen, Loslassen nach &ge; 10 Ticks
 * feuert je gespeicherter Ladung einen {@link EntityB92Beam} (Streuung waechst je Strahl um 0,2 bis 1). Schleichen +
 * Rechtsklick startet die 30-Tick-Ladeanimation; bei Tick 15 kommt eine Ladung dazu, mehr als 10 Ladungen zuenden eine
 * FLEIJA-Explosion (Radius 50). Port: NBT nur serverseitig (Client bekommt den Stapel synchronisiert).
 */
public class GunB92Item extends Item {

    private final RandomSource rand = RandomSource.create();

    public GunB92Item(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof Player player) || player.isShiftKeyDown()) return;
        int j = this.getUseDuration(stack) - timeLeft;
        // Original: ArrowLooseEvent (Forge) - hier ohne Ereignis, die Ladung ist die Haltedauer.
        if (j < 10.0D) return;

        if (!level.isClientSide) {
            for (int i = 0; i < getPower(stack); i++) {
                EntityB92Beam beam = EntityB92Beam.shoot(level, player, 3.0F);
                float divergence = i * 0.2F;
                if (divergence > 1F) divergence = 1F;
                if (i > 0) {
                    Vec3 m = beam.getDeltaMovement();
                    beam.setDeltaMovement(m.x + rand.nextGaussian() * divergence, m.y + rand.nextGaussian() * divergence, m.z + rand.nextGaussian() * divergence);
                }
                // damageItem(1): Gegenstand ohne Haltbarkeit - im Original wirkungslos
                level.addFreshEntity(beam);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:weapon.sparkShoot"), SoundSource.PLAYERS, 5.0F, 1.0F);
            setAnim(stack, 1);
            setPower(stack, 0);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        int j = getAnim(stack);

        if (j > 0) {
            if (j < 30) setAnim(stack, j + 1);
            else setAnim(stack, 0);

            if (j == 15) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), HbmSoundsNT.get("hbm:weapon.b92Reload"), SoundSource.PLAYERS, 2F, 0.9F);
                setPower(stack, getPower(stack) + 1);

                if (getPower(stack) > 10) {
                    setPower(stack, 0);
                    EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(level, entity.getX(), entity.getY(), entity.getZ(), 50);
                    if (!ex.isRemoved()) {
                        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 100.0F, level.random.nextFloat() * 0.1F + 0.9F);
                        level.addFreshEntity(ex);
                        EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(ModEntities.CLOUD_FLEIJA_RAINBOW.get(), level, 50);
                        cloud.setPos(entity.getX(), entity.getY(), entity.getZ());
                        level.addFreshEntity(cloud);
                    }
                }
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && getPower(stack) > 0) {
            if (getAnim(stack) == 0) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            }
        } else {
            if (getAnim(stack) == 0 && !level.isClientSide) setAnim(stack, 1);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        int id = Polaroid.id();
        if (id == 11) {
            list.add(Component.literal("A weapon that came from the stars."));
            list.add(Component.literal("It screams for murder."));
        } else if (id == 18) {
            list.add(Component.literal("One could turn the gun into a bomb"));
            list.add(Component.literal("by overloading the capacitors..."));
        } else {
            list.add(Component.literal("Stay away from me compootur!"));
        }
        list.add(Component.literal(""));
        list.add(Component.literal("Projectiles explode on impact."));
        list.add(Component.literal("Sneak while holding the right mouse button"));
        list.add(Component.literal("to charge additional energy."));
        list.add(Component.literal("The more energy is stored, the less accurate"));
        list.add(Component.literal("the beams become."));
        list.add(Component.literal("Only up to ten charges may be stored."));
        list.add(Component.literal(""));
        list.add(Component.literal("\"It's nerf or nothing!\""));
        list.add(Component.literal(""));
        list.add(Component.literal("[LEGENDARY WEAPON]"));
    }

    /** Original: getItemAttributeModifiers - Angriffsschaden +3,5. */
    @Override
    @SuppressWarnings("deprecation")
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        return ImmutableMultimap.of(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 3.5, AttributeModifier.Operation.ADDITION));
    }

    //? if forge {
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.GunB92ItemRenderer.instance();
            }
        });
    }
    //?} elif neoforge {
    /*@Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.hbm_m.client.render.item.GunB92ItemRenderer.instance();
            }
        });
    }
    *///?}

    // ---- NBT wie im Original: "animation" / "energy" ----

    public static int getAnim(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t == null ? 0 : t.getInt("animation");
    }

    private static void setAnim(ItemStack stack, int i) {
        stack.getOrCreateTag().putInt("animation", i);
    }

    public static int getPower(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t == null ? 0 : t.getInt("energy");
    }

    public static void setPower(ItemStack stack, int i) {
        stack.getOrCreateTag().putInt("energy", i);
    }

    public static float getRotationFromAnim(ItemStack stack) {
        float rad = 0.0174533F;
        rad *= 7.5F;
        int i = getAnim(stack);
        if (i < 10) return 0;
        i -= 10;
        if (i < 6) return rad * i;
        if (i > 14) return rad * (5 - (i - 15));
        return rad * 5;
    }

    public static float getTransFromAnim(ItemStack stack) {
        float i = getAnim(stack);
        if (i < 10) return 0;
        i -= 10;
        if (i > 4 && i < 10) return (i - 5) * 0.05F;
        if (i > 9 && i < 15) return (10 * 0.05F) - ((i - 5) * 0.05F);
        return 0;
    }
}
