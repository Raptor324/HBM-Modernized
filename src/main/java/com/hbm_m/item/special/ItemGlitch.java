package com.hbm_m.item.special;

import com.hbm_m.platform.ItemHooks;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.item.IBatteryItem;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.VortexEntity;
import com.hbm_m.entity.projectile.EntityBoxcar;
import com.hbm_m.entity.projectile.EntityMeteor;
import com.hbm_m.explosion.ExplosionChaos;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.main.Polaroid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemGlitch} ("It's a gamble!"): Rechtsklick verbraucht das Teil (Haltbarkeit 1,
 * 5 Schaden) und wuerfelt einen von 31 Effekten. Das Bild folgt wie beim Polaroid der Polaroid-Zahl.
 * Die Waffen aus Fall 13 und 18 (lilmac, maresleg, G12-Munition) werden ueber ihre Registry-ID gesucht und
 * fehlen, solange das Waffensystem noch nicht portiert ist.
 */
public class ItemGlitch extends Item implements IBatteryItem, ITooltipProvider {

    public ItemGlitch(Properties properties) {
        super(properties.stacksTo(1).durability(1));
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty()) player.getInventory().add(stack);
    }

    private static ItemStack byId(String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static void chat(Player player, String text) {
        player.sendSystemMessage(Component.literal(text));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemHooks.hurtAndBreak(stack, 5, player, hand);

        if (!world.isClientSide)
            switch (player.getRandom().nextInt(31)) {
            case 0:
                chat(player, "Sorry nothing.");
                break;
            case 1:
                chat(player, "Prometheus was punished by the gods by giving the gift of knowledge to man. He was cast into the bowels of the earth and pecked by birds.");
                break;
            case 2:
                player.hurt(ModDamageSources.radiation(world), 1000);
                break;
            case 3:
                player.hurt(ModDamageSources.create(world, ModDamageTypes.BOXCAR), 1000);
                break;
            case 4:
                player.hurt(ModDamageSources.blackHole(world), 1000);
                break;
            // BlockMeteoriteTreasure.getItemDropped ist nicht ueberschrieben: der Block selbst
            case 5:
                give(player, new ItemStack(ModBlocks.BLOCK_METEOR_TREASURE.get()));
                break;
            case 6:
                for (int i = 0; i < 3; i++)
                    give(player, new ItemStack(ModBlocks.BLOCK_METEOR_TREASURE.get()));
                break;
            case 7:
                for (int i = 0; i < 10; i++)
                    give(player, new ItemStack(ModBlocks.BLOCK_METEOR_TREASURE.get()));
                break;
            case 8:
                give(player, new ItemStack(ModItems.AMMO_CONTAINER.get(), 10));
                chat(player, "Oh, and by the way: The polaroid shifts reality. Things can be different if the polaroid is broken.");
                break;
            case 9:
                give(player, new ItemStack(ModItems.NUKE_ADVANCED_KIT.get(), 1));
                break;
            case 10:
                give(player, new ItemStack(ModItems.NUKE_STARTER_KIT.get(), 1));
                break;
            case 11: {
                EntityBoxcar pip = new EntityBoxcar(world);
                pip.setPos(player.getX(), player.getY() + 50, player.getZ());
                world.addFreshEntity(pip);
                break;
            }
            case 12:
                for (int i = 0; i < 10; i++) {
                    EntityBoxcar pippo = new EntityBoxcar(world);
                    pippo.setPos(player.getX() + player.getRandom().nextGaussian() * 25, player.getY() + 50, player.getZ() + player.getRandom().nextGaussian() * 25);
                    world.addFreshEntity(pippo);
                }
                break;
            case 13:
                give(player, byId("gun_heavy_revolver_lilmac", 1));
                give(player, new ItemStack(ModItems.BOTTLE_SPARKLE.get()));
                give(player, new ItemStack(ModItems.GEIGER_COUNTER.get()));
                chat(player, "Have some free stuff. You'll need it for that one cryptic achievement.");
                break;
            case 14:
                player.getInventory().dropAll();
                ExplosionChaos.igniteAllBlocks(world, (int) player.getX(), (int) player.getY(), (int) player.getZ(), 5);
                break;
            case 15:
                for (int i = 0; i < 36; i++)
                    give(player, new ItemStack(Items.DIRT, 64));
                break;
            case 16:
                chat(player, "v yvxr lbhe nggvghqr!");
                break;
            case 17:
                chat(player, "89% of magic tricks are not magic. Technically, they are sorcery.");
                break;
            case 18:
                give(player, byId("gun_maresleg", 1));
                give(player, byId("ammo_standard_g12", 12));
                chat(player, "Here ya go.");
                break;
            case 19:
                chat(player, "Ë");
                break;
            case 20:
                chat(player, "Good day, I am text");
                break;
            case 21:
                give(player, new ItemStack(ModItems.MISSILE_NUCLEAR.get()));
                chat(player, "73616d706c652074657874!");
                break;
            case 22:
                chat(player, "Budget cuts, no effect for you.");
                break;
            case 23:
                chat(player, "oof");
                break;
            case 24:
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 9));
                chat(player, "Tank!");
                break;
            case 25:
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 9));
                chat(player, "More devastating than a falling boxcar!");
                break;
            case 26:
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 * 20, 9));
                chat(player, "Ha!");
                break;
            case 27: {
                VortexEntity vortex = new VortexEntity(ModEntities.VORTEX.get(), world);
                vortex.setSize(2.5F);
                vortex.setPos(player.getX(), player.getY() - 15, player.getZ());
                world.addFreshEntity(vortex);
                break;
            }
            case 28: {
                EntityMeteor mirv = new EntityMeteor(world);
                mirv.setPos(player.getX(), player.getY() + 100, player.getZ());
                world.addFreshEntity(mirv);
                chat(player, "Watch your head!");
                break;
            }
            case 29:
                ExplosionLarge.spawnBurst(world, player.getX(), player.getY(), player.getZ(), 27, 3);
                chat(player, "Bam!");
                break;
            case 30:
                give(player, new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE)));
                chat(player, "It's dangerous to go alone, take this!");
                break;
            }

        player.inventoryMenu.broadcastChanges();
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHbmTooltip(ItemStack itemstack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("It's a gamble!"));
        list.add(Component.literal(""));

        switch (Polaroid.id()) {
            case 1 -> list.add(Component.literal("Click-click-click!"));
            case 2 -> list.add(Component.literal("Creek!"));
            case 3 -> list.add(Component.literal("Bzzzt!"));
            case 4 -> list.add(Component.literal("TS staring off into space."));
            case 5 -> list.add(Component.literal("BANG!!"));
            case 6 -> list.add(Component.literal("Woop!"));
            case 7 -> list.add(Component.literal("Poow!"));
            case 8 -> list.add(Component.literal("Pft!"));
            case 9 -> list.add(Component.literal("GF fgnevat bss vagb fcnpr."));
            case 10 -> list.add(Component.literal("Backup memory #8 on 1.44 million bytes."));
            case 11 -> list.add(Component.literal("PTANG!"));
            case 12 -> list.add(Component.literal("Bzzt-zrrt!"));
            case 13 -> list.add(Component.literal("Clang, click-brrthththrtrtrtrtrtr!"));
            case 14 -> list.add(Component.literal("KABLAM!"));
            case 15 -> list.add(Component.literal("PLENG!"));
            case 16 -> list.add(Component.literal("Wheeeeeeee-"));
            case 17 -> list.add(Component.literal("Thump."));
            case 18 -> list.add(Component.literal("BANG! Choo-chooo! B A N G ! ! !"));
            default -> { }
        }
    }

    @Override public void chargeBattery(ItemStack stack, long i) { }
    @Override public void setCharge(ItemStack stack, long i) { }
    @Override public void dischargeBattery(ItemStack stack, long i) { }
    @Override public long getCharge(ItemStack stack) { return 200; }
    @Override public long getMaxCharge(ItemStack stack) { return 200; }
    @Override public long getChargeRate(ItemStack stack) { return 0; }
    @Override public long getDischargeRate(ItemStack stack) { return 200; }
}
