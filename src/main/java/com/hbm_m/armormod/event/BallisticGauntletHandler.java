package com.hbm_m.armormod.event;

//? if forge {
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.inventory.ComparableStack;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.factory.Lego;
import com.hbm_m.item.weapon.sedna.factory.XFactory12ga;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.particle.helper.BlackPowderCreator;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1:1 {@code ModEventHandler.onPlayerPunch}: Mit dem ballistischen Handschuh als Servo-Mod im Brustteil feuert ein
 * Faustschlag (ohne Waffe in der Hand) eine Ladung 12-Gauge-Munition aus dem Inventar ab.
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BallisticGauntletHandler {

    @SubscribeEvent
    public static void onPlayerPunch(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty() || !ArmorModificationHelper.hasMods(chestplate)) return;

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)) return;

        ItemStack servo = ArmorModificationHelper.pryMods(chestplate)[ArmorModificationHelper.servos];
        if (servo == null || servo.isEmpty() || servo.getItem() != ModItems.BALLISTIC_GAUNTLET.get()) return;

        BulletConfig[] configs = {XFactory12ga.g12_bp, XFactory12ga.g12_bp_magnum, XFactory12ga.g12_bp_slug, XFactory12ga.g12,
                XFactory12ga.g12_slug, XFactory12ga.g12_flechette, XFactory12ga.g12_magnum, XFactory12ga.g12_explosive, XFactory12ga.g12_phosphorus};

        BulletConfig fired = null;
        for (BulletConfig config : configs) {
            if (config != null && consumeAmmo(player, config.ammo)) {
                fired = config;
                break;
            }
        }
        if (fired == null) return;

        int bullets = fired.projectilesMin;
        if (fired.projectilesMax > fired.projectilesMin) bullets += player.getRandom().nextInt(fired.projectilesMax - fired.projectilesMin);

        for (int i = 0; i < bullets; i++) {
            EntityBulletBaseMK4 mk4 = new EntityBulletBaseMK4(player, fired, 15F, 0F, -0.1875, -0.0625, 0.5);
            level.addFreshEntity(mk4);
            if (i == 0 && fired.blackPowder) {
                Vec3 m = mk4.getDeltaMovement();
                BlackPowderCreator.composeEffect(level, mk4.getX(), mk4.getY(), mk4.getZ(), m.x, m.y, m.z, 10, 0.25F, 0.5F, 10, 0.25F);
            }
        }
        Lego.playSound(player, "hbm:weapon.shotgunShoot", 1.0F, 1.0F);
    }

    /** {@code InventoryUtil.doesPlayerHaveAStack(player, ammo, true, true)}: eine Patrone aus dem Inventar nehmen. */
    private static boolean consumeAmmo(Player player, ComparableStack ammo) {
        if (ammo == null) return false;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && ammo.matchesRecipe(stack, true)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
//?}
