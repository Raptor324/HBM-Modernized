package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityCharger} (1.7.10): eine Ladeplatte, die die Ausruestung der
 * darauf stehenden Spieler auflaedt.
 *
 * <p>Der Ablauf des Originals ist uebernommen: die Platte faehrt erst ueber {@link #DELAY} Ticks
 * aus, bevor sie Energie abgibt, und faehrt ebenso wieder ein, wenn niemand mehr etwas zu laden
 * hat. Geladen werden Haupthand und die vier Ruestungsteile - im Original sind das die
 * Ausruestungsplaetze 0 bis 4.</p>
 *
 * <p><b>Abweichung:</b> Das Original spricht Gegenstaende ueber {@code IBatteryItem} an; dieser
 * Port nutzt durchweg Energie-Faehigkeiten, darum laeuft das Laden ueber
 * {@link BaseMachineBlockEntity#chargeStack}. Wirkung und Reihenfolge bleiben gleich.</p>
 */
public class ChargerBlockEntity extends BaseMachineBlockEntity {

    private static final long MAX_POWER = 100_000L;
    /** Original: {@code delay = 20} - so lange braucht die Platte zum Ausfahren. */
    private static final int DELAY = 20;
    /** Original: {@code Math.max(power / 5, 1)} je Gegenstand und Tick. */
    private static final int SPLIT = 5;

    private int usingTicks = 0;
    private int lastUsingTicks = 0;
    /** Original: {@code lastOp} - haelt die Partikel/Klaenge noch ein paar Ticks am Laufen. */
    private int lastOp = 0;
    /** Original {@code charge > 0} / {@code particles}: synchronisiert, der Client faehrt die Platte selbst (RenderCharger). */
    private boolean charging = false;
    private boolean particlesSync = false;

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER_BE.get(), pos, state, 0, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChargerBlockEntity be) {
        if (level.isClientSide()) {
            be.clientTick(level, pos);
            return;
        }

        be.ensureNetworkInitialized();

        List<Player> players = level.getEntitiesOfClass(Player.class, chargeArea(pos));

        // Original: erst zaehlen, wieviel ueberhaupt gebraucht wird - daran haengt das Ausfahren.
        boolean anythingToCharge = false;
        for (Player player : players) {
            for (ItemStack stack : chargeables(player)) {
                if (!stack.isEmpty()) { anythingToCharge = true; break; }
            }
            if (anythingToCharge) break;
        }

        boolean particles = be.lastOp > 0;
        be.charging = anythingToCharge;
        be.particlesSync = particles;
        if (particles) {
            be.lastOp--;
            if (level.getGameTime() % 20 == 0) {
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);
            }
        }

        be.lastUsingTicks = be.usingTicks;

        // Original: Ausfahren, solange etwas zu laden ist; sonst wieder einfahren.
        if ((anythingToCharge || particles) && be.usingTicks < DELAY) {
            be.usingTicks++;
            if (be.usingTicks == 2) {
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
        if (!anythingToCharge && !particles && be.usingTicks > 0) {
            be.usingTicks--;
            if (be.usingTicks == 4) {
                level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }

        // Original: erst bei voll ausgefahrener Platte fliesst Strom.
        if (be.usingTicks >= DELAY && be.getEnergyStored() > 0) {
            be.doCharge(players);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Original updateEntity auf dem Client: Platte aus-/einfahren und magicCrit-Partikel ({@code -dir}). */
    private void clientTick(Level level, BlockPos pos) {
        lastUsingTicks = usingTicks;

        if ((charging || particlesSync) && usingTicks < DELAY) usingTicks++;
        if (!charging && !particlesSync && usingTicks > 0) usingTicks--;

        if (particlesSync) {
            BlockState state = getBlockState();
            // Original: ForgeDirection.getOrientation(meta).getOpposite()
            net.minecraft.core.Direction dir = state.hasProperty(com.hbm_m.block.machines.ChargerBlock.FACING)
                    ? state.getValue(com.hbm_m.block.machines.ChargerBlock.FACING).getOpposite() : net.minecraft.core.Direction.SOUTH;
            net.minecraft.util.RandomSource rand = level.random;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                    pos.getX() + 0.5 + rand.nextDouble() * 0.0625 + dir.getStepX() * 0.75,
                    pos.getY() + 0.1,
                    pos.getZ() + 0.5 + rand.nextDouble() * 0.0625 + dir.getStepZ() * 0.75,
                    -dir.getStepX() + rand.nextGaussian() * 0.1,
                    0,
                    -dir.getStepZ() + rand.nextGaussian() * 0.1);
        }
    }

    /** Original: {@code transferPower} - jeder Gegenstand bekommt hoechstens ein Fuenftel. */
    private void doCharge(List<Player> players) {
        for (Player player : players) {
            for (ItemStack stack : chargeables(player)) {
                if (stack.isEmpty()) continue;
                if (getEnergyStored() <= 0) return;

                long portion = Math.max(getEnergyStored() / SPLIT, 1L);
                if (chargeStack(stack, portion) > 0) {
                    lastOp = 4;
                }
            }
        }
    }

    /** Original: Ausruestungsplaetze 0 bis 4 - Haupthand und die vier Ruestungsteile. */
    private static Iterable<ItemStack> chargeables(Player player) {
        List<ItemStack> stacks = new java.util.ArrayList<>();
        stacks.add(player.getMainHandItem());
        player.getArmorSlots().forEach(stacks::add);
        return stacks;
    }

    /** Original: eine flache Box auf der Platte, waagerecht um einen halben Block erweitert. */
    private static AABB chargeArea(BlockPos pos) {
        return new AABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                .inflate(0.5D, 0.0D, 0.5D);
    }

    public int getUsingTicks()     { return usingTicks; }
    public int getLastUsingTicks() { return lastUsingTicks; }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false; // Original: kein Inventar.
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("usingTicks", usingTicks);
        tag.putBoolean("charging", charging);
        tag.putBoolean("particles", particlesSync);
    }

    /** Original synchronisiert nur charge/particles; usingTicks rechnet der Client selbst weiter. */
    @Override
    protected void applyClientUpdate(CompoundTag tag) {
        int keep = usingTicks;
        super.applyClientUpdate(tag);
        usingTicks = keep;
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        usingTicks = tag.getInt("usingTicks");
        charging = tag.getBoolean("charging");
        particlesSync = tag.getBoolean("particles");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.charger");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
            int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
        return null; // Original: kein GUI.
    }
}
