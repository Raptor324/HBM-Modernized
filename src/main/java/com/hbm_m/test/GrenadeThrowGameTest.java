package com.hbm_m.test;

import java.util.List;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
 *///?}

/**
 * Wurf-Items muessen im Kreativ- wie im Ueberlebensmodus werfen (Original: Kreativ wirft ohne Verbrauch).
 * Simuliert den Serverpfad: Spieler-Tick (Ziehdauer) + {@code ServerPlayerGameMode.useItem}.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class GrenadeThrowGameTest {

    private GrenadeThrowGameTest() { }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty5x5x5", batch = "grenadethrow", timeoutTicks = 200)
    public static void universalCreative(GameTestHelper helper) { run(helper, GameType.CREATIVE, ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3)); }

    @GameTest(template = "empty5x5x5", batch = "grenadethrow", timeoutTicks = 200)
    public static void universalSurvival(GameTestHelper helper) { run(helper, GameType.SURVIVAL, ItemGrenadeUniversal.make(EnumGrenadeShell.FRAG, EnumGrenadeFilling.HE, EnumGrenadeFuze.S3)); }

    @GameTest(template = "empty5x5x5", batch = "grenadethrow", timeoutTicks = 200)
    public static void dynamiteCreative(GameTestHelper helper) { run(helper, GameType.CREATIVE, new ItemStack(ModItems.STICK_DYNAMITE.get())); }

    @GameTest(template = "empty5x5x5", batch = "grenadethrow", timeoutTicks = 200)
    public static void grenadeCreative(GameTestHelper helper) { run(helper, GameType.CREATIVE, new ItemStack(ModItems.GRENADE.get())); }

    private static void run(GameTestHelper helper, GameType mode, ItemStack stack) {
        //? if < 1.21.1 {
        // FakePlayer: Mock-Spieler von makeMockServerPlayerInLevel hat keinen Netzkanal (Mod-Pakete beim Login scheitern)
        net.minecraft.server.level.ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "grenade_test"));
        net.minecraft.core.BlockPos p = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0F, 0F);
        player.setGameMode(mode);
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, stack);
        int before = stack.getCount();
        helper.onEachTick(player::doTick);
        helper.runAfterDelay(60, () -> {
            ItemStack held = player.getMainHandItem();
            player.gameMode.useItem(player, helper.getLevel(), held, InteractionHand.MAIN_HAND);
            List<Entity> thrown = helper.getLevel().getEntities((Entity) null, player.getBoundingBox().inflate(8), e -> e != player && !(e instanceof net.minecraft.world.entity.item.ItemEntity));
            check(!thrown.isEmpty(), "nichts geworfen (" + mode + ", " + stack.getItem() + ", deploy=" + com.hbm_m.extprop.HbmPlayerProps.getData(player).grenadeDeployment + ")");
            int after = player.getMainHandItem().getCount();
            if (mode == GameType.CREATIVE) check(after == before, "Kreativ verbraucht (" + after + ")");
            thrown.forEach(Entity::discard);
            helper.succeed();
        });
        //?} else {
        /*helper.succeed();
        *///?}
    }
}
