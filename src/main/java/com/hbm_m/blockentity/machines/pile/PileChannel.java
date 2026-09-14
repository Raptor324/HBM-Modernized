package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.item.machine.ItemPileRodMK2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1-Port von {@code TileEntityPileCore.PileChannel}: eine gebohrte Roehre durch den Meiler.
 *
 * <p>Die Klasse deckt alle drei Kanalarten ab und traegt darum die Daten von allen dreien - im
 * Original ausdruecklich so gewollt, statt drei Unterklassen anzulegen. Ein Brennstoffkanal haelt
 * die Staebe, seine Hitze und seinen Neutronenfluss; ein Lueftungskanal nur seine Luft; ein
 * Steuerkanal nur die Auszugshoehe seines Stabs.</p>
 *
 * <p>{@link #entry} ist der erste Block des Kanals samt der Richtung, in die er in den Meiler
 * hineinlaeuft.</p>
 */
public class PileChannel {

    /** Original: {@code MAX_AIR = 1_000}. */
    public static final int MAX_AIR = 1_000;

    public final BlockPos entry;
    public final Direction dir;
    /** Laenge des Kanals, gleich der Ausdehnung des Meilers auf dieser Achse. */
    public final int length;
    public final PileChannelType type;

    /** Nur bei Brennstoffkanaelen belegt. */
    public final ItemStack[] rods;
    public double heat = 0D;
    public double outgoingNeutrons = 0D;
    public double incomingNeutrons = 0D;

    /** Nur bei Lueftungskanaelen. */
    public int air;

    /** Nur bei Steuerkanaelen. Original: {@code 1D}, der Stab ist also von Haus aus gezogen. */
    public double control = 1D;

    public PileChannel(BlockPos entry, Direction dir, int length, PileChannelType type) {
        this.entry = entry.immutable();
        this.dir = dir;
        this.length = Math.max(0, length);
        this.type = type;
        this.rods = new ItemStack[this.length];
        java.util.Arrays.fill(this.rods, ItemStack.EMPTY);
    }

    public boolean isAt(BlockPos pos) {
        return entry.equals(pos);
    }

    // ── Beladen und Auswerfen ───────────────────────────────────────────────

    /**
     * Original: {@code loadItem} - der neue Stab wird vorne hineingeschoben und schiebt die ganze
     * Reihe eine Stelle weiter. Faellt hinten einer heraus, landet er am Ausgang des Kanals.
     */
    public void loadItem(Level level, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        if (rods.length <= 0) {
            dropItem(level, stack, -1);
            return;
        }

        ItemStack carry = stack;
        for (int i = 0; i < rods.length; i++) {
            if (rods[i].isEmpty()) {
                rods[i] = carry;
                return;
            }
            ItemStack prev = rods[i];
            rods[i] = carry;
            carry = prev;
        }

        dropItem(level, carry, length);
    }

    /** Original: {@code ejectAll} - alles hinten hinauswerfen, etwa beim Abbau des Meilers. */
    public void ejectAll(Level level) {
        for (int i = 0; i < rods.length; i++) {
            dropItem(level, rods[i], length);
            rods[i] = ItemStack.EMPTY;
        }
    }

    /**
     * Original: {@code dropItem}. Der Abbrand wird dabei aus dem NBT entfernt, damit sich Staebe
     * im Lager wieder stapeln lassen.
     */
    public void dropItem(Level level, ItemStack stack, int depth) {
        if (level == null || stack == null || stack.isEmpty()) return;

        BlockPos at = entry.relative(dir, depth);

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ItemPileRodMK2.KEY_NBT_DEPLETION)) {
            tag.remove(ItemPileRodMK2.KEY_NBT_DEPLETION);
            if (tag.isEmpty()) stack.setTag(null);
        }

        level.addFreshEntity(new ItemEntity(level,
                at.getX() + 0.5D, at.getY() + 0.5D, at.getZ() + 0.5D, stack));
    }

    // ── Speichern ───────────────────────────────────────────────────────────

    /** Original: {@code writeChannelToNBT}, ein Kanal je Namenspraefix. */
    public void save(CompoundTag nbt, String name) {
        nbt.putInt(name + "_x", entry.getX());
        nbt.putInt(name + "_y", entry.getY());
        nbt.putInt(name + "_z", entry.getZ());
        nbt.putByte(name + "_d", (byte) dir.ordinal());

        if (type == PileChannelType.FUEL) {
            ListTag list = new ListTag();
            for (int i = 0; i < rods.length; i++) {
                if (rods[i] != null && !rods[i].isEmpty()) {
                    CompoundTag entryTag = new CompoundTag();
                    entryTag.putByte("slot", (byte) i);
                    rods[i].save(entryTag);
                    list.add(entryTag);
                }
            }
            nbt.put(name + "items", list);
            nbt.putDouble(name + "heat", heat);
            nbt.putDouble(name + "neutrons", incomingNeutrons);
        }

        if (type == PileChannelType.VENTILATION) {
            nbt.putInt(name + "air", air);
        }

        if (type == PileChannelType.CONTROL) {
            nbt.putDouble(name + "control", control);
        }
    }

    /** Original: {@code readChannelFromNBT}. Laenge und Art kommen aus der Geometrie des Kerns. */
    public static PileChannel load(CompoundTag nbt, String name, PileCoreBlockEntity core) {
        BlockPos pos = new BlockPos(nbt.getInt(name + "_x"), nbt.getInt(name + "_y"), nbt.getInt(name + "_z"));
        Direction dir = Direction.values()[nbt.getByte(name + "_d") & 7];

        PileChannelType type = PileChannelType.of(dir, core.getOrientation());
        PileChannel chan = new PileChannel(pos, dir, core.lengthForType(type), type);

        if (type == PileChannelType.FUEL) {
            ListTag list = nbt.getList(name + "items", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                int slot = entryTag.getByte("slot") & 0xFF;
                if (slot < chan.rods.length) {
                    chan.rods[slot] = ItemStack.of(entryTag);
                }
            }
            chan.heat = nbt.getDouble(name + "heat");
            chan.incomingNeutrons = nbt.getDouble(name + "neutrons");
        }

        if (type == PileChannelType.VENTILATION) {
            chan.air = nbt.getInt(name + "air");
        }

        if (type == PileChannelType.CONTROL) {
            chan.control = nbt.getDouble(name + "control");
        }

        return chan;
    }
}
