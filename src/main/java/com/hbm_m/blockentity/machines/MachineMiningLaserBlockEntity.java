package com.hbm_m.blockentity.machines;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.menu.MachineMiningLaserMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Mining Laser - Port von {@code TileEntityMachineMiningLaser} (1.7.10 Original). Trotz des Namens
 * keine Ziel-/Weltraumwaffe: eine automatisierte, senkrecht nach unten abteufende "Quarry", die
 * Schicht fuer Schicht direkt unterhalb der Maschine abbaut (mechanisch fast identisch zum bereits
 * portierten {@link MachineMiningDrillBlockEntity}, hier aber OHNE Bohrkopf-Item - die Original-
 * Maschine braucht keinen Drillbit, ihre Geschwindigkeit/Fortune kommt ausschliesslich aus dem
 * (in diesem Port bereits durchgaengig entfernten) Upgrade-System).
 * <p>
 * SCOPE-Entscheidungen:
 * <ul>
 *   <li>Kein Upgrade-System (SPEED/POWER/EFFECT/FORTUNE/OVERDRIVE/Nullifier/Exclusive-Prozess-
 *   Upgrades) - konsistent mit {@link MachineMiningDrillBlockEntity} (dort bereits mit RADIUS=1-
 *   Kommentar dokumentiert): feste Basiswerte statt Slot-gesteuerter Werte.</li>
 *   <li>Kein manueller On/Off-Knopf: die Maschine arbeitet automatisch, sobald Energie vorhanden
 *   und kein Redstone-Signal anliegt (Original: Knopf im GUI + Redstone-Sperre - hier nur Redstone-
 *   Sperre, wie bei vielen anderen automatisierten Maschinen dieses Ports).</li>
 *   <li>Kein Oel-Tank/Fluid-Sender: das Original saugt Oel-Erz-Drops in einen internen Tank. Dieser
 *   Port hat kein generisches "Oel-Erz-Block"-Konzept fuer normales Abbauen (Oel kommt in diesem
 *   Port ausschliesslich ueber das dedizierte {@code MachinePumpjackBlockEntity}-Lagerstaetten-
 *   system) - die Ziel-Infrastruktur existiert schlicht nicht, daher entfaellt dieser Teil
 *   vollstaendig (siehe gleiche Begruendung wie bei RTG->Batterie in MachineRadiolysisBlockEntity).</li>
 *   <li>Kein Fluessigkeits-Damm-Bau (Original: {@code buildDam()}) - Fluessigkeiten werden beim
 *   Antreffen einfach entfernt statt eingedaemmt.</li>
 *   <li>Mob-Entzuendung (Original: Nebeneffekt beim Abbauen) IST uebernommen: naheliegende
 *   {@link LivingEntity}s im Arbeitsbereich werden beim Abbauen kurz entzuendet.</li>
 * </ul>
 */
public class MachineMiningLaserBlockEntity extends BaseMachineBlockEntity {

    private static final int OUTPUT_START = 0;
    private static final int OUTPUT_COUNT = 9;
    public static final int SLOT_BATTERY = 9;
    private static final int SLOT_COUNT = 10;

    private static final long CAPACITY = 1_000_000L;
    private static final long MAX_RECEIVE = 2_000L;
    private static final long ENERGY_PER_TICK = 250L;

    private static final int RADIUS = 1; // kein Effect-Upgrade in diesem Port -> immer 3x3
    private static final double BASE_SPEED = 1.5D;
    private static final int BASE_FORTUNE = 0;
    private static final Set<Block> IGNORED_BLOCKS = Set.of(Blocks.BEDROCK, Blocks.BARRIER);

    private boolean operational = false;
    private int targetDepth = 0;
    private int ticksWorked = 0;
    private int currentTicksToWork = 100;

    public MachineMiningLaserBlockEntity(BlockPos pos, BlockState state) {
        super(com.hbm_m.blockentity.ModBlockEntities.MINING_LASER_BE.get(), pos, state, SLOT_COUNT, CAPACITY, MAX_RECEIVE, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMiningLaserBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        chargeFromBatterySlot(SLOT_BATTERY);

        boolean wasOperational = operational;
        operational = canWork(level, pos);

        if (operational) {
            setEnergyStored(getEnergyStored() - ENERGY_PER_TICK);

            if (tryMine(level, pos)) {
                targetDepth++;
                setChanged();
                sendUpdateToClient();

                if (targetDepth > maxDepth(level, pos)) {
                    operational = false;
                }
            }
        } else {
            ticksWorked = 0;
        }

        if (wasOperational != operational) {
            setChanged();
            sendUpdateToClient();
        }
    }

    private boolean canWork(ServerLevel level, BlockPos pos) {
        if (level.hasNeighborSignal(pos)) return false;
        if (getEnergyStored() < ENERGY_PER_TICK) return false;
        return targetDepth <= maxDepth(level, pos);
    }

    private int maxDepth(Level level, BlockPos pos) {
        return pos.getY() - level.getMinBuildHeight() - 2;
    }

    private int getTargetY(BlockPos pos) {
        return pos.getY() - 1 - targetDepth;
    }

    private boolean shouldIgnoreBlock(Level level, BlockState state, BlockPos pos) {
        if (state.isAir()) return true;
        if (IGNORED_BLOCKS.contains(state.getBlock())) return true;
        return state.getDestroySpeed(level, pos) < 0;
    }

    /** Bohrt eine 3x3-Ebene direkt unterhalb der Maschine, Schicht fuer Schicht (analog Mining Drill). */
    private boolean tryMine(ServerLevel level, BlockPos pos) {
        int y = getTargetY(pos);

        boolean ignoreAll = true;
        float combinedHardness = 0F;

        for (int x = pos.getX() - RADIUS; x <= pos.getX() + RADIUS; x++) {
            for (int z = pos.getZ() - RADIUS; z <= pos.getZ() + RADIUS; z++) {
                BlockPos target = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(target);
                if (state.getFluidState().isEmpty() && shouldIgnoreBlock(level, state, target)) continue;

                ignoreAll = false;
                combinedHardness += Math.max(0F, state.getDestroySpeed(level, target));
            }
        }

        if (ignoreAll) {
            ticksWorked = 0;
            return true;
        }

        ticksWorked++;
        currentTicksToWork = Math.max(1, (int) Math.ceil(combinedHardness / BASE_SPEED));

        if (ticksWorked >= currentTicksToWork) {
            breakLayer(level, pos, y);
            ticksWorked = 0;
        }

        return false;
    }

    private void breakLayer(ServerLevel level, BlockPos pos, int y) {
        for (int x = pos.getX() - RADIUS; x <= pos.getX() + RADIUS; x++) {
            for (int z = pos.getZ() - RADIUS; z <= pos.getZ() + RADIUS; z++) {
                BlockPos target = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(target);

                if (!state.getFluidState().isEmpty()) {
                    level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                    continue;
                }
                if (shouldIgnoreBlock(level, state, target)) continue;

                mineSingleBlock(level, target, state);
            }
        }

        igniteNearbyEntities(level, pos, y);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
    }

   private void mineSingleBlock(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack tool = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        if (BASE_FORTUNE > 0) {
            com.hbm_m.platform.ItemHooks.setEnchantmentLevel(tool, level, "minecraft:fortune", BASE_FORTUNE);
        }

        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(pos));

        List<ItemStack> drops = state.getDrops(builder);
        level.removeBlock(pos, false);

        for (ItemStack drop : drops) {
            insertOrDrop(pos, drop);
        }
    }

    /** Nebeneffekt aus dem Original: Kreaturen im Arbeitsbereich werden kurz entzuendet. */
    private void igniteNearbyEntities(ServerLevel level, BlockPos pos, int y) {
        AABB area = new AABB(
                pos.getX() - RADIUS, y, pos.getZ() - RADIUS,
                pos.getX() + RADIUS + 1, y + 1, pos.getZ() + RADIUS + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            com.hbm_m.platform.PlatformHooks.setSecondsOnFire(entity, 2);
        }
    }

    private void insertOrDrop(BlockPos minedAt, ItemStack toInsert) {
        if (toInsert.isEmpty()) return;

        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT && !toInsert.isEmpty(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (!slotStack.isEmpty() && com.hbm_m.platform.PlatformHooks.isSameItemSameTags(slotStack, toInsert)) {
                int room = slotStack.getMaxStackSize() - slotStack.getCount();
                int move = Math.min(room, toInsert.getCount());
                if (move > 0) {
                    slotStack.grow(move);
                    toInsert.shrink(move);
                }
            }
        }
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT && !toInsert.isEmpty(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, toInsert.copy());
                toInsert.setCount(0);
            }
        }

        if (!toInsert.isEmpty() && level != null) {
            Block.popResource(level, minedAt, toInsert);
        }
    }

    public int getProgressScaled(int scale) {
        if (currentTicksToWork <= 0) return 0;
        return Math.min(scale, ticksWorked * scale / currentTicksToWork);
    }

    public int getDrillDepth() { return targetDepth; }
    public boolean isActive() { return operational; }

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putInt("target_depth", targetDepth);
        tag.putBoolean("operational", operational);
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tag.putInt("target_depth", targetDepth);
    tag.putBoolean("operational", operational);
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        targetDepth = tag.getInt("target_depth");
        operational = tag.getBoolean("operational");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.mining_laser");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            return isEnergyProviderItem(stack);
        }
        return false; // Ausgabe-Slots: kein manuelles Einlegen.
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMiningLaserMenu(id, inventory, this);
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        double depth = targetDepth + 4.0D;
        return super.getRenderBoundingBox().expandTowards(0, -depth, 0);
    }
}
