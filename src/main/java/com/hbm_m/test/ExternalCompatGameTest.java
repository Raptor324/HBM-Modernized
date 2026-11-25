package com.hbm_m.test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.hbm_m.api.energy.EnergyConversion;
import com.hbm_m.api.energy.ForeignEnergyBridge;
import com.hbm_m.api.weapon.ExternalGunCommonBridge;
import com.hbm_m.api.weapon.SednaWeaponApi;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineSteamCondenserBlockEntity;
import com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity;
import com.hbm_m.compat.ContraptionDoorState;
import com.hbm_m.compat.curios.CuriosCompat;
import com.hbm_m.compat.jade.HbmFluidStorageProvider;
import com.hbm_m.compat.sable.SableCompat;
import com.hbm_m.compat.tacz.TaczBridgeCompat;
import com.hbm_m.explosion.LithiumSectionCompat;
import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.util.Compat;
import com.hbm_m.util.CompatExternal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import snownee.jade.api.view.ViewGroup;

/**
 * Server-side GameTests for external mod integrations and fallback behavior:
 * Create/ContraptionDoorState, Sable, Curios, Jade, TACZ/ExternalGunCommonBridge/SednaWeaponApi,
 * ForeignEnergyBridge, LithiumSectionCompat, and Compat/CompatExternal.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class ExternalCompatGameTest {

    private ExternalCompatGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "external_compat", timeoutTicks = 100)
    public static void contraptionDoorState_roundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controller = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos part = helper.absolutePos(new BlockPos(1, 2, 1));
        VoxelShape shape = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);

        ContraptionDoorState.markContraptionWorld(level);
        check(ContraptionDoorState.isContraptionWorld(level), "Contraption world should be marked");

        ContraptionDoorState.setShape(level, controller, shape);
        check(ContraptionDoorState.getShape(level, controller) == shape, "Shape should round-trip");

        ContraptionDoorState.setOpen(level, controller, true);
        check(ContraptionDoorState.hasOpenEntry(level, controller), "Open entry should exist");
        check(ContraptionDoorState.getOpen(level, controller), "Open state should be true");
        ContraptionDoorState.setOpen(level, controller, false);
        check(!ContraptionDoorState.getOpen(level, controller), "Open state should update to false");

        ContraptionDoorState.setPartController(level, part, controller);
        check(controller.equals(ContraptionDoorState.getControllerForPart(level, part)),
                "Part-to-controller mapping should resolve");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "external_compat", timeoutTicks = 100)
    public static void sableCompat_absentFallbackIsIdentity(GameTestHelper helper) {
        if (SableCompat.isLoaded()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        Vec3 pos = new Vec3(12.5, 64.0, -3.25);
        Vec3 dir = new Vec3(0.0, 1.0, 0.0);
        BlockPos blockPos = new BlockPos(5, 70, 9);

        check(pos.equals(SableCompat.toWorld(level, pos)), "toWorld(Vec3) should be identity when Sable absent");
        check(pos.equals(SableCompat.toWorld(level, pos.x, pos.y, pos.z)),
                "toWorld(x,y,z) should be identity when Sable absent");
        check(Vec3.atCenterOf(blockPos).equals(SableCompat.blockCenterInWorld(level, blockPos)),
                "blockCenterInWorld should be block center when Sable absent");
        check(dir.equals(SableCompat.toWorldVector(level, pos, dir)),
                "toWorldVector should be identity when Sable absent");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "external_compat", timeoutTicks = 100)
    public static void curiosAndLithium_safeExecution(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 1));
        ItemStack mask = CuriosCompat.getFaceMask(stand);
        check(mask != null && mask.isEmpty(), "CuriosCompat.getFaceMask should return EMPTY on unequipped entity");
        check(CuriosCompat.getFaceMask(null).isEmpty(), "CuriosCompat.getFaceMask(null) should return EMPTY");

        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        LithiumSectionCompat.replaced(level, SectionPos.asLong(abs), null);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "external_compat", timeoutTicks = 100)
    public static void jadeFluidStorage_resolvesControllerAndPart(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos ctrlRel = new BlockPos(1, 1, 1);
        BlockPos partRel = new BlockPos(1, 2, 1);
        BlockPos chestRel = new BlockPos(2, 1, 1);

        helper.setBlock(ctrlRel, ModBlocks.STEAM_CONDENSER.get());
        BlockEntity ctrlBe = helper.getBlockEntity(ctrlRel);
        if (!(ctrlBe instanceof MachineSteamCondenserBlockEntity condenser)) {
            throw new GameTestAssertException("Expected MachineSteamCondenserBlockEntity");
        }
        condenser.getReceivingTanks()[0].fillMb(ModFluids.SPENTSTEAM.getSource(), 100);

        List<ViewGroup<CompoundTag>> ctrlGroups = HbmFluidStorageProvider.buildGroups(level, condenser);
        check(ctrlGroups != null && !ctrlGroups.isEmpty(),
                "Jade buildGroups should return fluid groups for IFluidUserMK2 controller");

        helper.setBlock(partRel, ModBlocks.UNIVERSAL_MACHINE_PART.get());
        BlockEntity partBe = helper.getBlockEntity(partRel);
        if (!(partBe instanceof UniversalMachinePartBlockEntity part)) {
            throw new GameTestAssertException("Expected UniversalMachinePartBlockEntity");
        }
        part.setControllerPos(helper.absolutePos(ctrlRel));

        List<ViewGroup<CompoundTag>> partGroups = HbmFluidStorageProvider.buildGroups(level, part);
        check(partGroups != null && !partGroups.isEmpty(),
                "Jade buildGroups should resolve UniversalMachinePartBlockEntity to controller tanks");

        helper.setBlock(chestRel, Blocks.CHEST);
        BlockEntity chestBe = helper.getBlockEntity(chestRel);
        check(HbmFluidStorageProvider.buildGroups(level, chestBe) == null,
                "Jade buildGroups should return null for non-HBM BlockEntity");

        // CompatExternal.getCoreFromPos should also resolve part -> controller
        check(CompatExternal.getCoreFromPos(level, helper.absolutePos(partRel)) == condenser,
                "CompatExternal.getCoreFromPos should resolve part to controller");
        check(Compat.isPositionLoaded(level, helper.absolutePos(ctrlRel)),
                "Compat.isPositionLoaded should be true in test arena");
        check(Compat.getTileStandard(level, helper.absolutePos(ctrlRel)) == condenser,
                "Compat.getTileStandard should return condenser BE");
        check(Compat.getTileStandard(level, helper.absolutePos(ctrlRel).atY(-1000)) == null,
                "Compat.getTileStandard out of build height should return null");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "external_compat", timeoutTicks = 100)
    public static void taczBridge_andSednaWeaponApi(GameTestHelper helper) {
        if (!TaczBridgeCompat.isLoaded()) {
            check(!TaczBridgeCompat.bridgeEnabled(), "bridgeEnabled should be false when hbm_tacz absent");
        }

        ItemStack nonGun = new ItemStack(Items.IRON_INGOT);
        check(!SednaWeaponApi.isSednaGun(nonGun), "Iron ingot is not a Sedna gun");
        check(!SednaWeaponApi.isSednaGun(ItemStack.EMPTY), "Empty stack is not a Sedna gun");

        ItemStack gunStack = new ItemStack(ModItems.GUN_FLIEGERFAUST.get());
        check(SednaWeaponApi.isSednaGun(gunStack), "GUN_FLIEGERFAUST should be a Sedna gun");
        int cap = SednaWeaponApi.getAmmoCapacity(gunStack);
        check(cap > 0, "Sedna gun capacity should be > 0");
        SednaWeaponApi.setRawAmmoCount(gunStack, 1);
        check(SednaWeaponApi.getRawAmmoCount(gunStack) == 1, "setRawAmmoCount/getRawAmmoCount should round-trip");
        SednaWeaponApi.setAiming(gunStack, true);
        check(SednaWeaponApi.isAiming(gunStack), "setAiming(true) should persist");
        SednaWeaponApi.setAiming(gunStack, false);
        check(!SednaWeaponApi.isAiming(gunStack), "setAiming(false) should persist");

        AtomicInteger keybindCalls = new AtomicInteger();
        ExternalGunCommonBridge mockBridge = new ExternalGunCommonBridge() {
            @Override
            public boolean isHandled(ItemStack stack) {
                return SednaWeaponApi.isSednaGun(stack);
            }

            @Override
            public void onKeybind(ItemGunBaseNT gun, LivingEntity entity, Container inventory,
                                  ItemStack stack, EnumKeybind keybind, boolean pressed) {
                keybindCalls.incrementAndGet();
            }
        };

        try {
            ExternalGunCommonBridge.register(mockBridge);
            check(ExternalGunCommonBridge.get() == mockBridge, "Custom ExternalGunCommonBridge should be registered");
            check(ExternalGunCommonBridge.get().isHandled(gunStack), "Mock bridge should handle Sedna gun");
            check(ExternalGunCommonBridge.get().shouldSuppressNativeSound(gunStack),
                    "shouldSuppressNativeSound defaults to isHandled");
            check(!ExternalGunCommonBridge.get().isHandled(nonGun), "Mock bridge should not handle non-gun");
            ExternalGunCommonBridge.get().onKeybind(
                    (ItemGunBaseNT) gunStack.getItem(), null, null, gunStack, EnumKeybind.RELOAD, true);
            check(keybindCalls.get() == 1, "onKeybind should invoke mock bridge");
        } finally {
            ExternalGunCommonBridge.register(null);
        }
        check(!ExternalGunCommonBridge.get().isHandled(gunStack), "Reset bridge should return false for isHandled");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "external_compat", timeoutTicks = 100)
    public static void foreignEnergyBridge_feHeConversionAndLatch(GameTestHelper helper) {
        ForeignEnergyBridge bridge = new ForeignEnergyBridge();
        long surplusHe = 1000L;
        long unmetHe = 500L;
        long gameTime = 100L;
        long neighbour = 42L;

        bridge.publish(surplusHe, unmetHe, gameTime);
        check(bridge.extractableFe() == EnergyConversion.feFromHe(surplusHe),
                "extractableFe should match published surplus");
        check(bridge.insertableFe() == EnergyConversion.feFromHe(unmetHe),
                "insertableFe should match published unmet demand");

        long toInsertFe = EnergyConversion.feFromHe(100L);
        long accepted = bridge.insertFe(toInsertFe, neighbour, gameTime);
        check(accepted == toInsertFe, "insertFe should accept within budget");
        check(bridge.injected() == 100L, "injected HE should be 100");
        check(bridge.latched(neighbour, gameTime + ForeignEnergyBridge.RECEIVE_LATCH_TICKS),
                "neighbour should remain latched within RECEIVE_LATCH_TICKS");
        check(!bridge.latched(neighbour, gameTime + ForeignEnergyBridge.RECEIVE_LATCH_TICKS + 1),
                "neighbour latch should expire after RECEIVE_LATCH_TICKS");

        long toExtractFe = EnergyConversion.feFromHe(200L);
        long extracted = bridge.extractFe(toExtractFe);
        check(extracted == toExtractFe, "extractFe should give within surplus budget");
        check(bridge.drainOwed() == 200L, "drainOwed should return 200 HE");
        check(bridge.drainOwed() == 0L, "drainOwed should clear owed balance");

        long pulled = bridge.injectedSource().extractEnergy(50L, false);
        check(pulled == 50L && bridge.injected() == 50L,
                "InjectedSource should expose and drain injected HE");
        helper.succeed();
    }
}
