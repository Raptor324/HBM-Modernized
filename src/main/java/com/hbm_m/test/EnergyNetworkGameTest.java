package com.hbm_m.test;

import com.hbm_m.api.energy.ConverterBlockEntity;
import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.energy.PowerNet;
import com.hbm_m.api.energy.SwitchBlock;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineBatteryBlockEntity;
import com.hbm_m.blockentity.machines.MachineWoodBurnerBlockEntity;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Кроссплатформенные GameTests энергосети energymk2.
 * Группы:
 *   A. energy_algo — алгоритм распределения PowerNet на моках.
 *   B. energy_topo — топология UniNodespace на реальных блоках.
 *   C. energy_flow — сквозной поток энергии на реальных машинах.
 */
@GameTestHolder("hbm_m")
@PrefixGameTestTemplate(false)
public final class EnergyNetworkGameTest {

    private EnergyNetworkGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void checkEq(long expected, long actual, String msg) {
        if (expected != actual) {
            throw new GameTestAssertException(msg + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    // ─── Моки ───────────────────────────────────────────────────────

    private static final class MockReceiver implements IEnergyReceiver {
        long energy;
        final long capacity;
        final long speed;
        final Priority priority;

        MockReceiver(long capacity, long speed, Priority priority, long initial) {
            this.capacity = capacity;
            this.speed = speed;
            this.priority = priority;
            this.energy = Math.min(initial, capacity);
        }

        @Override public long getEnergyStored() { return energy; }
        @Override public long getMaxEnergyStored() { return capacity; }
        @Override public void setEnergyStored(long e) { energy = Math.max(0, Math.min(capacity, e)); }
        @Override public long getReceiveSpeed() { return speed; }
        @Override public Priority getPriority() { return priority; }
        @Override public boolean canReceive() { return energy < capacity; }
        @Override public long receiveEnergy(long maxReceive, boolean simulate) {
            long accepted = Math.min(maxReceive, capacity - energy);
            if (!simulate) energy += accepted;
            return accepted;
        }
        @Override public boolean canConnectEnergy(Direction side) { return true; }
    }

    private static final class MockProvider implements IEnergyProvider {
        long energy;
        final long speed;

        MockProvider(long stored, long speed) {
            this.energy = stored;
            this.speed = speed;
        }

        @Override public long getEnergyStored() { return energy; }
        @Override public long getMaxEnergyStored() { return energy; }
        @Override public void setEnergyStored(long e) { energy = Math.max(0, e); }
        @Override public long getProvideSpeed() { return speed; }
        @Override public long extractEnergy(long maxExtract, boolean simulate) {
            long taken = Math.min(energy, maxExtract);
            if (!simulate) energy -= taken;
            return taken;
        }
        @Override public boolean canExtract() { return energy > 0; }
        @Override public boolean canConnectEnergy(Direction side) { return true; }
    }

    /** Приемник-BlockEntity для проверки эвикции по isBadLink. */
    private static final class MockReceiverBE extends BlockEntity implements IEnergyReceiver {
        final MockReceiver inner = new MockReceiver(100_000L, 100_000L, IEnergyReceiver.Priority.NORMAL, 0);

        MockReceiverBE(BlockPos pos, BlockState state) {
            super(ModBlockEntities.WIRE_BE.get(), pos, state);
        }

        @Override public long getEnergyStored() { return inner.energy; }
        @Override public long getMaxEnergyStored() { return inner.capacity; }
        @Override public void setEnergyStored(long e) { inner.setEnergyStored(e); }
        @Override public long getReceiveSpeed() { return inner.speed; }
        @Override public Priority getPriority() { return inner.priority; }
        @Override public boolean canReceive() { return inner.canReceive(); }
        @Override public long receiveEnergy(long maxReceive, boolean simulate) { return inner.receiveEnergy(maxReceive, simulate); }
        @Override public boolean canConnectEnergy(Direction side) { return true; }
    }

    private static PowerNet makeNet(MockProvider[] providers, MockReceiver[] receivers) {
        PowerNet net = new PowerNet();
        for (MockProvider p : providers) net.addProvider(p);
        for (MockReceiver r : receivers) net.addReceiver(r);
        return net;
    }

    // ─── Группа A: алгоритм распределения ──────────────────────────

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoSingleProviderSingleReceiver(GameTestHelper helper) {
        MockProvider gen = new MockProvider(10_000L, 10_000L);
        MockReceiver rec = new MockReceiver(100_000L, 100_000L, IEnergyReceiver.Priority.NORMAL, 0);
        PowerNet net = makeNet(new MockProvider[]{gen}, new MockReceiver[]{rec});

        net.update();

        checkEq(10_000L, rec.energy, "Receiver got all available power");
        checkEq(0L, gen.energy, "Provider drained");
        checkEq(10_000L, net.energyTracker, "Tracker counts transferred energy");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoProportionalSplitSamePriority(GameTestHelper helper) {
        MockProvider gen = new MockProvider(1_000L, 1_000L);
        // Спрос A=900, спрос B=300 → веса 75%/25%
        MockReceiver a = new MockReceiver(1_000L, 1_000L, IEnergyReceiver.Priority.NORMAL, 100);
        MockReceiver b = new MockReceiver(400L, 1_000L, IEnergyReceiver.Priority.NORMAL, 100);
        PowerNet net = makeNet(new MockProvider[]{gen}, new MockReceiver[]{a, b});

        net.update();

        checkEq(750L, a.energy - 100, "Receiver A gets 75% of supply");
        checkEq(250L, b.energy - 100, "Receiver B gets 25% of supply");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoPriorityWaterfallHighFirst(GameTestHelper helper) {
        // Поставки 600, спрос HIGH=500, спрос NORMAL=500
        MockProvider gen = new MockProvider(600L, 600L);
        // Спрос приемника = min(емкость - запас, скорость) → задаем 500 через емкость/скорость
        MockReceiver high = new MockReceiver(500L, 500L, IEnergyReceiver.Priority.HIGH, 0);
        MockReceiver normal = new MockReceiver(500L, 500L, IEnergyReceiver.Priority.NORMAL, 0);
        PowerNet net = makeNet(new MockProvider[]{gen}, new MockReceiver[]{high, normal});

        net.update();

        checkEq(500L, high.energy, "HIGH priority served first in full");
        checkEq(100L, normal.energy, "NORMAL priority gets only the remainder");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoHighestToLowestOrdering(GameTestHelper helper) {
        // Спрос: HIGHEST=400 > HIGH=300 > NORMAL=200 > LOW=100 > LOWEST=50, поставки 700
        MockProvider gen = new MockProvider(700L, 700L);
        MockReceiver hxs = new MockReceiver(400L, 400L, IEnergyReceiver.Priority.HIGHEST, 0);
        MockReceiver hi  = new MockReceiver(300L, 300L, IEnergyReceiver.Priority.HIGH, 0);
        MockReceiver no  = new MockReceiver(200L, 200L, IEnergyReceiver.Priority.NORMAL, 0);
        MockReceiver lo  = new MockReceiver(100L, 100L, IEnergyReceiver.Priority.LOW, 0);
        MockReceiver lxs = new MockReceiver(50L, 50L, IEnergyReceiver.Priority.LOWEST, 0);
        PowerNet net = makeNet(new MockProvider[]{gen}, new MockReceiver[]{hxs, hi, no, lo, lxs});

        net.update();

        checkEq(400L, hxs.energy, "HIGHEST tier served fully");
        checkEq(300L, hi.energy, "HIGH tier served fully");
        checkEq(0L, no.energy, "NORMAL unserved - supply exhausted");
        checkEq(0L, lo.energy, "LOW unserved");
        checkEq(0L, lxs.energy, "LOWEST unserved");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoProviderSpeedCapRespected(GameTestHelper helper) {
        MockProvider gen = new MockProvider(10_000L, 2_000L);
        MockReceiver rec = new MockReceiver(100_000L, 100_000L, IEnergyReceiver.Priority.NORMAL, 0);
        PowerNet net = makeNet(new MockProvider[]{gen}, new MockReceiver[]{rec});

        net.update();

        checkEq(2_000L, rec.energy, "Only provideSpeed leaves the provider per tick");
        checkEq(8_000L, gen.energy, "Provider keeps the rest of its stock");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoReceiverSpeedCapRespected(GameTestHelper helper) {
        MockProvider gen = new MockProvider(10_000L, 10_000L);
        MockReceiver rec = new MockReceiver(100_000L, 1_500L, IEnergyReceiver.Priority.NORMAL, 0);
        PowerNet net = makeNet(new MockProvider[]{gen}, new MockReceiver[]{rec});

        net.update();

        checkEq(1_500L, rec.energy, "Receiver intake capped by its own speed");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoTransferPowerOvershootReturned(GameTestHelper helper) {
        MockReceiver rec = new MockReceiver(1_000L, 1_000L, IEnergyReceiver.Priority.NORMAL, 800);

        long overshoot = rec.transferPower(500);

        checkEq(1_000L, rec.getEnergyStored(), "Receiver filled to cap");
        checkEq(300L, overshoot, "Overshoot above capacity returned to sender");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoStaleSubscriptionTimedOut(GameTestHelper helper) {
        MockProvider gen = new MockProvider(5_000L, 5_000L);
        MockReceiver rec = new MockReceiver(100_000L, 100_000L, IEnergyReceiver.Priority.NORMAL, 0);
        PowerNet net = makeNet(new MockProvider[]{gen}, new MockReceiver[]{rec});

        // Устаревшая подписка (старше таймаута 3000 мс)
        net.receiverEntries.put(rec, System.currentTimeMillis() - 60_000L);
        net.update();

        check(!net.receiverEntries.containsKey(rec), "Stale subscription evicted by timeout");
        checkEq(0L, rec.energy, "Timed-out receiver got nothing");
        checkEq(5_000L, gen.energy, "Provider untouched when no valid receivers");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoRemovedReceiverEvictedByBadLink(GameTestHelper helper) {
        MockProvider gen = new MockProvider(5_000L, 5_000L);

        MockReceiverBE deadBe = new MockReceiverBE(
                helper.absolutePos(new BlockPos(1, 1, 1)),
                ModBlocks.WIRE_COATED.get().defaultBlockState());
        deadBe.setRemoved();

        PowerNet net = new PowerNet();
        net.addProvider(gen);
        net.receiverEntries.put(deadBe, System.currentTimeMillis());

        net.update();

        check(!net.receiverEntries.containsKey(deadBe), "Removed BE evicted by isBadLink");
        checkEq(5_000L, gen.energy, "No power sent to removed BE");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoEnergyConservedWithOddNumbers(GameTestHelper helper) {
        MockProvider g1 = new MockProvider(1_237L, 1_237L);
        MockProvider g2 = new MockProvider(9_001L, 777L);
        MockReceiver r1 = new MockReceiver(10_000L, 999L, IEnergyReceiver.Priority.NORMAL, 3);
        MockReceiver r2 = new MockReceiver(10_000L, 555L, IEnergyReceiver.Priority.LOW, 7);
        MockReceiver r3 = new MockReceiver(10_000L, 123_456L, IEnergyReceiver.Priority.HIGH, 11);

        long before = g1.energy + g2.energy + r1.energy + r2.energy + r3.energy;

        PowerNet net = makeNet(new MockProvider[]{g1, g2}, new MockReceiver[]{r1, r2, r3});
        net.update();

        long after = g1.energy + g2.energy + r1.energy + r2.energy + r3.energy;
        long drift = Math.abs(after - before);
        check(drift <= 5, "Total energy conserved within rounding tolerance (drift=" + drift + ")");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoSendPowerDiodeOneWayDistribution(GameTestHelper helper) {
        // Оригинальная семантика: вес внутри УРОВНЯ считается относительно спроса этого уровня,
        // поэтому для проверки пропорции оба приемника помещены в один уровень.
        MockReceiver hi = new MockReceiver(1_000L, 500L, IEnergyReceiver.Priority.HIGH, 0);
        MockReceiver lo = new MockReceiver(1_000L, 100L, IEnergyReceiver.Priority.HIGH, 0);
        PowerNet net = makeNet(new MockProvider[]{}, new MockReceiver[]{hi, lo});

        long leftover = net.sendPowerDiode(600L);

        checkEq(500L, hi.energy, "Diode: faster receiver takes 5/6 (its demand weight)");
        checkEq(100L, lo.energy, "Diode: slower receiver takes 1/6");
        checkEq(0L, leftover, "All power distributed");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_algo", timeoutTicks = 100)
    public static void algoEnumOrdinalContractPreserved(GameTestHelper helper) {
        // Регрессия миграции сохранений приоритетов батарей
        checkEq(0L, IEnergyReceiver.Priority.LOWEST.ordinal(), "LOWEST ordinal");
        checkEq(1L, IEnergyReceiver.Priority.LOW.ordinal(), "LOW ordinal (legacy 0)");
        checkEq(2L, IEnergyReceiver.Priority.NORMAL.ordinal(), "NORMAL ordinal (legacy 1)");
        checkEq(3L, IEnergyReceiver.Priority.HIGH.ordinal(), "HIGH ordinal (legacy 2)");
        checkEq(4L, IEnergyReceiver.Priority.HIGHEST.ordinal(), "HIGHEST ordinal");
        helper.succeed();
    }

    // ─── Группа B: топология узлов на реальных блоках ─────────────

    private static Nodespace.PowerNode nodeAt(GameTestHelper helper, BlockPos relativePos) {
        Level level = helper.getLevel();
        return Nodespace.getNode((ServerLevel) level, helper.absolutePos(relativePos));
    }

    private static void placeWire(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.WIRE_COATED.get());
    }

    @GameTest(template = "empty3x3x3", batch = "energy_topo", timeoutTicks = 120)
    public static void topoTwoAdjacentWiresMergeIntoOneNet(GameTestHelper helper) {
        placeWire(helper, new BlockPos(1, 1, 1));
        placeWire(helper, new BlockPos(2, 1, 1));

        helper.succeedWhen(() -> {
            Nodespace.PowerNode n1 = nodeAt(helper, new BlockPos(1, 1, 1));
            Nodespace.PowerNode n2 = nodeAt(helper, new BlockPos(2, 1, 1));
            check(n1 != null && n2 != null, "Both wires created nodes");
            check(n1.net == n2.net && n1.net != null, "Adjacent wires merged into a single network");
        });
    }

    @GameTest(template = "empty3x3x3", batch = "energy_topo", timeoutTicks = 160)
    public static void topoThreeWireChainSingleNet(GameTestHelper helper) {
        placeWire(helper, new BlockPos(0, 1, 1));
        placeWire(helper, new BlockPos(1, 1, 1));
        placeWire(helper, new BlockPos(2, 1, 1));

        helper.succeedWhen(() -> {
            Nodespace.PowerNode n0 = nodeAt(helper, new BlockPos(0, 1, 1));
            Nodespace.PowerNode n1 = nodeAt(helper, new BlockPos(1, 1, 1));
            Nodespace.PowerNode n2 = nodeAt(helper, new BlockPos(2, 1, 1));
            check(n0 != null && n1 != null && n2 != null, "All three nodes exist");
            check(n0.net != null && n0.net == n1.net && n1.net == n2.net, "Chain forms one network");
            checkEq(3L, n0.net.links.size(), "Network contains all three links");
        });
    }

    @GameTest(template = "empty3x3x3", batch = "energy_topo", timeoutTicks = 160)
    public static void topoDiagonalWiresStaySeparate(GameTestHelper helper) {
        placeWire(helper, new BlockPos(1, 1, 1));
        placeWire(helper, new BlockPos(2, 2, 2));

        helper.runAfterDelay(80, () -> {
            Nodespace.PowerNode a = nodeAt(helper, new BlockPos(1, 1, 1));
            Nodespace.PowerNode b = nodeAt(helper, new BlockPos(2, 2, 2));
            check(a != null && b != null, "Both nodes created");
            check(a.net == null || b.net == null || a.net != b.net,
                    "Diagonal wires must NOT merge (no face adjacency)");
            helper.succeed();
        });
    }

    @GameTest(template = "empty5x5x5", batch = "energy_topo", timeoutTicks = 240)
    public static void topoRemovingMiddleWireSplitsNet(GameTestHelper helper) {
        placeWire(helper, new BlockPos(1, 1, 1));
        placeWire(helper, new BlockPos(2, 1, 1));
        placeWire(helper, new BlockPos(3, 1, 1));

        helper.startSequence()
            .thenWaitUntil(() -> {
                Nodespace.PowerNode n1 = nodeAt(helper, new BlockPos(1, 1, 1));
                Nodespace.PowerNode n3 = nodeAt(helper, new BlockPos(3, 1, 1));
                check(n1 != null && n3 != null && n1.net != null && n1.net == n3.net,
                        "Precondition: single merged network");
            })
            .thenExecute(() -> helper.setBlock(new BlockPos(2, 1, 1), net.minecraft.world.level.block.Blocks.AIR))
            .thenWaitUntil(() -> {
                Nodespace.PowerNode left = nodeAt(helper, new BlockPos(1, 1, 1));
                Nodespace.PowerNode right = nodeAt(helper, new BlockPos(3, 1, 1));
                check(left != null && right != null, "End nodes survive the split");
                check(left.net != null && right.net != null && left.net != right.net,
                        "After removing the middle wire the ends form two separate networks");
            })
            .thenSucceed();
    }

    @GameTest(template = "empty5x5x5", batch = "energy_topo", timeoutTicks = 300)
    public static void topoSwitchGatesConnectivity(GameTestHelper helper) {
        placeWire(helper, new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(2, 1, 1),
                ModBlocks.SWITCH.get().defaultBlockState().setValue(SwitchBlock.POWERED, true));
        placeWire(helper, new BlockPos(3, 1, 1));

        helper.startSequence()
            .thenWaitUntil(() -> {
                Nodespace.PowerNode l = nodeAt(helper, new BlockPos(1, 1, 1));
                Nodespace.PowerNode r = nodeAt(helper, new BlockPos(3, 1, 1));
                check(l != null && r != null && l.net != null && l.net == r.net,
                        "Powered switch conducts: wires on both sides share one network");
            })
            .thenExecute(() -> helper.setBlock(new BlockPos(2, 1, 1),
                    ModBlocks.SWITCH.get().defaultBlockState().setValue(SwitchBlock.POWERED, false)))
            .thenWaitUntil(() -> {
                Nodespace.PowerNode l = nodeAt(helper, new BlockPos(1, 1, 1));
                Nodespace.PowerNode r = nodeAt(helper, new BlockPos(3, 1, 1));
                check(l == null || r == null || l.net == null || l.net != r.net,
                        "Switched-off switch destroys its node and splits the network");
            })
            .thenSucceed();
    }

    @GameTest(template = "empty5x5x5", batch = "energy_topo", timeoutTicks = 300)
    public static void topoBatteryBufferModeJunctionLifecycle(GameTestHelper helper) {
        BlockPos bat = new BlockPos(2, 1, 2);
        helper.setBlock(bat, ModBlocks.MACHINE_BATTERY.get());
        placeWire(helper, new BlockPos(2, 1, 1));

        helper.startSequence()
            .thenWaitUntil(() -> {
                MachineBatteryBlockEntity be =
                        (MachineBatteryBlockEntity) helper.getLevel()
                                .getBlockEntity(helper.absolutePos(bat));
                check(be != null, "Battery BE exists");
                check(be.getCurrentMode() == 0, "Battery starts in BOTH(buffer) mode by default");
                Nodespace.PowerNode jn = nodeAt(helper, bat);
                check(jn != null && jn.hasValidNet(),
                        "Buffer-mode battery maintains its own junction node with a valid net");
            })
            .thenExecute(() -> {
                MachineBatteryBlockEntity be =
                        (MachineBatteryBlockEntity) helper.getLevel()
                                .getBlockEntity(helper.absolutePos(bat));
                be.modeOnNoSignal = 1;
            })
            .thenWaitUntil(() -> {
                Nodespace.PowerNode jn = nodeAt(helper, bat);
                check(jn == null || jn.expired || !jn.hasValidNet(),
                        "Leaving buffer mode destroys the junction node");
            })
            .thenSucceed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_topo", timeoutTicks = 160)
    public static void topoWireRemovalDestroysItsNode(GameTestHelper helper) {
        BlockPos w = new BlockPos(1, 1, 1);
        placeWire(helper, w);

        helper.startSequence()
            .thenWaitUntil(() -> check(nodeAt(helper, w) != null, "Node exists while wire is placed"))
            .thenExecute(() -> helper.setBlock(w, net.minecraft.world.level.block.Blocks.AIR))
            .thenWaitUntil(() -> {
                Nodespace.PowerNode n = Nodespace.getNode(
                        (ServerLevel) helper.getLevel(), helper.absolutePos(w));
                check(n == null || n.expired || !n.hasValidNet(),
                        "Node gone / net destroyed after wire removal");
            })
            .thenSucceed();
    }

    // ─── Группа C: сквозной поток энергии ─────────────────────────

    private static MachineBatteryBlockEntity placeBattery(GameTestHelper helper, BlockPos pos, int mode, long energy) {
        helper.setBlock(pos, ModBlocks.MACHINE_BATTERY.get());
        MachineBatteryBlockEntity be =
                (MachineBatteryBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        check(be != null, "Battery BE created");
        be.modeOnNoSignal = mode;
        if (energy > 0) be.setEnergyStored(energy);
        return be;
    }

    private static long machineEnergy(GameTestHelper helper, BlockPos pos) {
        BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (be instanceof IEnergyReceiver rec) return rec.getEnergyStored();
        return Long.MIN_VALUE;
    }

    @GameTest(template = "empty5x5x5", batch = "energy_flow", timeoutTicks = 300)
    public static void flowGeneratorToReceiverThroughWire(GameTestHelper helper) {
        placeBattery(helper, new BlockPos(1, 1, 1), 2, 500_000L);
        placeWire(helper, new BlockPos(2, 1, 1));
        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.ELECTRIC_FURNACE.get());

        helper.succeedWhen(() -> {
            long fe = machineEnergy(helper, new BlockPos(3, 1, 1));
            check(fe > 0, "Furnace charged through the wire network (got " + fe + ")");
        });
    }

    @GameTest(template = "empty3x3x3", batch = "energy_flow", timeoutTicks = 200)
    public static void flowDirectProvisionAdjacentNoWire(GameTestHelper helper) {
        placeBattery(helper, new BlockPos(1, 1, 1), 2, 100_000L);
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.ELECTRIC_FURNACE.get());

        helper.succeedWhen(() -> {
            long fe = machineEnergy(helper, new BlockPos(2, 1, 1));
            check(fe > 0, "Adjacent receiver charged via direct provision (got " + fe + ")");
        });
    }

    @GameTest(template = "empty3x3x3", batch = "energy_flow", timeoutTicks = 220)
    public static void flowDepletedGeneratorTransfersNothing(GameTestHelper helper) {
        placeBattery(helper, new BlockPos(1, 1, 1), 2, 0L);
        placeWire(helper, new BlockPos(2, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 2), ModBlocks.ELECTRIC_FURNACE.get());

        helper.runAfterDelay(160, () -> {
            long fe = machineEnergy(helper, new BlockPos(1, 1, 2));
            checkEq(0L, fe, "Depleted generator transfers nothing even when subscribed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty3x3x3", batch = "energy_flow", timeoutTicks = 220)
    public static void flowInputModeBatteryDoesNotExport(GameTestHelper helper) {
        MachineBatteryBlockEntity bat = placeBattery(helper, new BlockPos(1, 1, 1), 1, 100_000L);
        placeWire(helper, new BlockPos(2, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 2), ModBlocks.ELECTRIC_FURNACE.get());

        helper.runAfterDelay(160, () -> {
            long fe = machineEnergy(helper, new BlockPos(1, 1, 2));
            checkEq(0L, fe, "INPUT-mode battery must not export energy");
            checkEq(100_000L, bat.getEnergyStored(), "INPUT-mode battery keeps its charge");
            helper.succeed();
        });
    }

    @GameTest(template = "empty5x5x5", batch = "energy_flow", timeoutTicks = 400)
    public static void flowSwitchCutsPowerMidRun(GameTestHelper helper) {
        BlockPos furnace = new BlockPos(2, 1, 4);
        placeBattery(helper, new BlockPos(2, 1, 0), 2, 500_000L);
        placeWire(helper, new BlockPos(2, 1, 1));
        helper.setBlock(new BlockPos(2, 1, 2),
                ModBlocks.SWITCH.get().defaultBlockState().setValue(SwitchBlock.POWERED, true));
        placeWire(helper, new BlockPos(2, 1, 3));
        helper.setBlock(furnace, ModBlocks.ELECTRIC_FURNACE.get());

        final long[] frozen = {-1};

        helper.startSequence()
            .thenWaitUntil(() -> {
                long fe = machineEnergy(helper, furnace);
                check(fe > 0, "Power flows through the closed switch (got " + fe + ")");
            })
            .thenExecute(() -> helper.setBlock(new BlockPos(2, 1, 2),
                    ModBlocks.SWITCH.get().defaultBlockState().setValue(SwitchBlock.POWERED, false)))
            .thenIdle(60)
            .thenExecute(() -> {
                frozen[0] = machineEnergy(helper, furnace);
                check(frozen[0] > 0, "Sanity: some energy arrived before the cut");
            })
            .thenIdle(40)
            .thenExecute(() -> {
                long nowFe = machineEnergy(helper, furnace);
                // Печь без рецепта не потребляет, сеть разомкнута → уровень замер
                checkEq(frozen[0], nowFe, "Open switch stops all further transfer");
            })
            .thenSucceed();
    }

    @GameTest(template = "empty3x3x3", batch = "energy_flow", timeoutTicks = 250)
    public static void flowConverterPullsFromHbmNet(GameTestHelper helper) {
        placeBattery(helper, new BlockPos(1, 1, 1), 2, 100_000L);
        placeWire(helper, new BlockPos(2, 1, 1));
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.CONVERTER_BLOCK.get());

        helper.succeedWhen(() -> {
            BlockEntity conv = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 2)));
            check(conv instanceof ConverterBlockEntity, "Converter BE exists");
            long buffered = ((ConverterBlockEntity) conv).getEnergyStored();
            check(buffered > 0, "Converter buffers energy pulled from the HBM network (got " + buffered + ")");
        });
    }

    /**
     * Репродукция реального сценария: мультиблок-генератор (буржуйка) → провод → приемник.
     * Буржуйка ставится целиком (onPlace формирует структуру), энергия задается напрямую
     * в буфер; провода окружают весь нижний ярус структуры — при корректных
     * getExtraEnergyPorts() хотя бы один провод прилегает к ENERGY_CONNECTOR части.
     */
    @GameTest(template = "empty5x5x5", batch = "energy_flow", timeoutTicks = 300)
    public static void flowWoodBurnerMultiblockProviderThroughWire(GameTestHelper helper) {
        BlockPos core = new BlockPos(2, 1, 2);
        helper.setBlock(core, ModBlocks.WOOD_BURNER.get());

        MachineWoodBurnerBlockEntity burner =
                (MachineWoodBurnerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(core));
        check(burner != null, "Wood burner core BE exists");
        burner.setEnergyStored(500_000L);

        // Кольцо проводов вокруг нижнего яруса структуры (y=1, footprint x2..3 z2..3)
        int[][] ring = {
                {1, 2}, {1, 3}, {4, 2}, {4, 3},   // стороны по X
                {2, 1}, {3, 1}, {2, 4}, {3, 4}    // стороны по Z
        };
        for (int[] p : ring) placeWire(helper, new BlockPos(p[0], 1, p[1]));

        // Цепочка от кольца к печи
        placeWire(helper, new BlockPos(1, 1, 1));
        placeWire(helper, new BlockPos(0, 1, 1));
        helper.setBlock(new BlockPos(0, 1, 0), ModBlocks.ELECTRIC_FURNACE.get());

        helper.succeedWhen(() -> {
            long fe = machineEnergy(helper, new BlockPos(0, 1, 0));
            check(fe > 0, "Furnace charged from multiblock generator through wire (got " + fe + ")");
        });
    }

    /**
     * Встречный сценарий: батарея(OUTPUT) → провод → advanced assembler (мультиблок).
     * Проверяет receiver-порты мультиблока: провода окружают структуру сборщика.
     */
    @GameTest(template = "empty5x5x5", batch = "energy_flow", timeoutTicks = 300)
    public static void flowChargesAdvancedAssemblerThroughWire(GameTestHelper helper) {
        placeBattery(helper, new BlockPos(0, 1, 0), 2, 500_000L);
        placeWire(helper, new BlockPos(0, 1, 1));
        placeWire(helper, new BlockPos(0, 1, 2));

        // Сборщик ставится контроллером, структура формируется в onPlace
        BlockPos asmCore = new BlockPos(2, 1, 2);
        helper.setBlock(asmCore, ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get());

        // Кольцо проводов вокруг всей структуры сборщика на уровне ядра и выше
        for (int y = 1; y <= 2; y++) {
            placeWire(helper, new BlockPos(1, y, 1));
            placeWire(helper, new BlockPos(1, y, 2));
            placeWire(helper, new BlockPos(1, y, 3));
            placeWire(helper, new BlockPos(2, y, 1));
            placeWire(helper, new BlockPos(2, y, 3));
            placeWire(helper, new BlockPos(3, y, 1));
            placeWire(helper, new BlockPos(3, y, 2));
            placeWire(helper, new BlockPos(3, y, 3));
        }
        // связываем внешнее кольцо с линией от батареи
        placeWire(helper, new BlockPos(1, 1, 0));

        helper.succeedWhen(() -> {
            BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(asmCore));
            check(be instanceof IEnergyReceiver, "Assembler core BE exists");
            long ae = ((IEnergyReceiver) be).getEnergyStored();
            check(ae > 0, "Advanced assembler charged through wire network (got " + ae + ")");
        });
    }
}





