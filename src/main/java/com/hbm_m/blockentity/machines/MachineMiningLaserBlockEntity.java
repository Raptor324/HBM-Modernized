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
 *   <li><b>Acht Upgrade-Slots</b> wie im Original. Sie sind das, was den Laser vom Bohrer
 *   unterscheidet: EFFECT verbreitert den Schacht ({@code 1 + Stufe * 2}, hoechstens 25 - also bis
 *   zu 51 Bloecke breit), SPEED bricht schneller und frisst dafuer mehr, POWER senkt den Verbrauch,
 *   OVERDRIVE laesst mehrere Durchgaenge pro Tick laufen, FORTUNE erhoeht die Ausbeute. Die
 *   Verbrauchsformel ist 1:1: {@code Grundlast - Grundlast * POWER / 16 + Grundlast * SPEED / 16}.</li>
 *   <li>Der <b>An/Aus-Knopf</b> in der Oberflaeche ist wie im Original vorhanden - ein Laser, der
 *   von allein losgraebt, sobald Strom anliegt, waere kaum zu baendigen.</li>
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

    /** 1:1 aus dem Original: Slot 0 Batterie, 1-8 Upgrades, 9-29 Ausgabe. */
    public static final int SLOT_BATTERY = 0;
    public static final int UPGRADE_START = 1;
    public static final int UPGRADE_COUNT = 8;
    public static final int OUTPUT_START = 9;
    public static final int OUTPUT_COUNT = 21;
    private static final int SLOT_COUNT = OUTPUT_START + OUTPUT_COUNT;

    /** Original: die Stapelgrenzen der einzelnen Upgrade-Sorten. */
    private static final java.util.Map<com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType, Integer> UPGRADE_CAPS =
            java.util.Map.of(
                    com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.SPEED, 12,
                    com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.EFFECT, 12,
                    com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3,
                    com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.FORTUNE, 3);

    private static final long CAPACITY = 1_000_000L;
    private static final long MAX_RECEIVE = 2_000L;
    private static final long ENERGY_PER_TICK = 250L;

    /** Original: {@code range = 1} ohne Upgrades, also ein 3x3-Schacht. */
    private static final int BASE_RADIUS = 1;
    /** Original: {@code Math.min(range, 25)}. */
    private static final int MAX_RADIUS = 25;
    private static final double BASE_SPEED = 1.5D;
    private static final Set<Block> IGNORED_BLOCKS = Set.of(Blocks.BEDROCK, Blocks.BARRIER);

    private final com.hbm_m.inventory.UpgradeManager upgradeManager = new com.hbm_m.inventory.UpgradeManager();

    /** Original: {@code isOn} - der Knopf in der Oberflaeche. */
    private boolean isOn = false;

    /** Die aus den Upgrades errechneten Werte des laufenden Ticks. */
    private int curRadius = BASE_RADIUS;
    private int curSpeed = 1;
    private int curFortune = 0;
    private long curConsumption = ENERGY_PER_TICK;

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

        applyUpgrades();

        boolean wasOperational = operational;
        operational = canWork(level, pos);

        if (operational) {
            // Original: {@code cycles = 1 + OVERDRIVE} Durchgaenge in einem einzigen Tick.
            int cycles = 1 + upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.OVERDRIVE);

            for (int i = 0; i < cycles; i++) {
                if (getEnergyStored() < curConsumption) break;
                setEnergyStored(getEnergyStored() - curConsumption);

                if (tryMine(level, pos)) {
                    targetDepth++;
                    setChanged();
                    sendUpdateToClient();

                    if (targetDepth > maxDepth(level, pos)) {
                        operational = false;
                        break;
                    }
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

    /**
     * Liest die Upgrades und rechnet die Werte des Ticks aus - 1:1 die Formeln des Originals.
     */
    private void applyUpgrades() {
        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_START + UPGRADE_COUNT - 1, UPGRADE_CAPS);

        int effect = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.EFFECT);
        int speed = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.SPEED);
        int power = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.POWER);

        int newRadius = Math.min(MAX_RADIUS, BASE_RADIUS + effect * 2);
        curSpeed = 1 + speed;
        curFortune = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.FORTUNE);

        // Original: mehr Tempo kostet genauso viel, wie ein Sparmodul einspart.
        curConsumption = ENERGY_PER_TICK
                - (ENERGY_PER_TICK * power / 16L)
                + (ENERGY_PER_TICK * speed / 16L);

        if (newRadius != curRadius) {
            curRadius = newRadius;
            setChanged();
            sendUpdateToClient();
        }
    }

    private boolean canWork(ServerLevel level, BlockPos pos) {
        // 1:1: ohne Zuendung graebt er nicht, egal wieviel Strom anliegt.
        if (!isOn) return false;
        if (level.hasNeighborSignal(pos)) return false;
        if (getEnergyStored() < curConsumption) return false;
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

        for (int x = pos.getX() - curRadius; x <= pos.getX() + curRadius; x++) {
            for (int z = pos.getZ() - curRadius; z <= pos.getZ() + curRadius; z++) {
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
        // Original: {@code getBreakSpeed(speed)} - jede Tempostufe bricht entsprechend schneller.
        currentTicksToWork = Math.max(1, (int) Math.ceil(combinedHardness / (BASE_SPEED * curSpeed)));

        if (ticksWorked >= currentTicksToWork) {
            breakLayer(level, pos, y);
            ticksWorked = 0;
        }

        return false;
    }

    private void breakLayer(ServerLevel level, BlockPos pos, int y) {
        for (int x = pos.getX() - curRadius; x <= pos.getX() + curRadius; x++) {
            for (int z = pos.getZ() - curRadius; z <= pos.getZ() + curRadius; z++) {
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
        if (curFortune > 0) {
            com.hbm_m.platform.ItemHooks.setEnchantmentLevel(tool, level, "minecraft:fortune", curFortune);
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
                pos.getX() - curRadius, y, pos.getZ() - curRadius,
                pos.getX() + curRadius + 1, y + 1, pos.getZ() + curRadius + 1);
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
    public boolean isOn() { return isOn; }

    /** Original: {@code getWidth() = 1 + getRange() * 2} - die Kantenlaenge des Schachts. */
    public int getWidth() { return 1 + curRadius * 2; }

    /** Original: der {@code AuxButtonPacket}-Knopf in der Oberflaeche. */
    public void toggleOn() {
        isOn = !isOn;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("target_depth", targetDepth);
        tag.putBoolean("operational", operational);
        tag.putBoolean("isOn", isOn);
        tag.putInt("radius", curRadius);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        targetDepth = tag.getInt("target_depth");
        operational = tag.getBoolean("operational");
        isOn = tag.getBoolean("isOn");
        curRadius = tag.contains("radius") ? tag.getInt("radius") : BASE_RADIUS;
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
        if (slot >= UPGRADE_START && slot < UPGRADE_START + UPGRADE_COUNT) {
            return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
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
        return super.getRenderBoundingBox().inflate(curRadius, 0, curRadius).expandTowards(0, -depth, 0);
    }
}
