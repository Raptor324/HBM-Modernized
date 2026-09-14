package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.ForceFieldMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityForceField} (1.7.10): eine Kugel, die alles draussen haelt, was
 * nicht schon drin war - und alles drin, was schon drin war.
 *
 * <p>Das Feld merkt sich jeden Tick, welche Kreaturen innerhalb und welche ausserhalb des Radius
 * sind. Wechselt jemand die Seite, wird er zurueckgeworfen und das Feld nimmt Schaden - und zwar
 * nach dem Schwung des Aufpralls ({@code Masse * Geschwindigkeit * 50}, wobei die Masse aus Hoehe
 * und Breite der Kreatur kommt). Faellt die Schildstaerke auf null, geht das Feld fuer
 * {@code 100 + Radius} Ticks in die Abkuehlung.</p>
 *
 * <p>Spieler laesst das Feld wie im Original ungehindert durch - es haelt nur Kreaturen und
 * Geschosse auf.</p>
 *
 * <p>Ein- und ausgeschaltet wird es ueber den Schalter rechts in der Oberflaeche, nicht ueber
 * Redstone - der Zustand haelt sich ueber das Speichern hinweg.</p>
 *
 * <p>Bei einem Treffer, der mindestens ein Zweihundertfuenfzigstel der Schildstaerke kostet,
 * blinkt das Feld fuenf Ticks lang rot - {@link #getColor()} liefert dafuer die Farbe, die der
 * Renderer und die Teilchen des Blocks verwenden.</p>
 */
public class ForceFieldBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_RADIUS = 1;
    public static final int SLOT_UPGRADE_HEALTH = 2;
    public static final int INVENTORY_SIZE = 3;

    /** Original: {@code maxPower = 1000000}. */
    private static final long MAX_POWER = 1_000_000L;
    /** Original: {@code baseCon = 1000}. */
    private static final int BASE_CONSUMPTION = 1_000;
    /** Original: {@code radCon = 500} je Radiusaufwertung. */
    private static final int RADIUS_CONSUMPTION = 500;
    /** Original: {@code shCon = 250} je Schildaufwertung. */
    private static final int SHIELD_CONSUMPTION = 250;
    /** Original: {@code baseRadius = 16}. */
    private static final int BASE_RADIUS = 16;
    /** Original: {@code radUpgrade = 16} Bloecke je Aufwertung. */
    private static final int RADIUS_PER_UPGRADE = 16;
    /** Original: {@code shUpgrade = 50} Schild je Aufwertung. */
    private static final int HEALTH_PER_UPGRADE = 50;
    /** Original: Grundschild 100. */
    private static final int BASE_HEALTH = 100;

    private int health = BASE_HEALTH;
    private int maxHealth = BASE_HEALTH;
    private int powerCons = BASE_CONSUMPTION;
    private int cooldown = 0;
    /** Original: {@code blink} - faellt der Treffer schwer aus, leuchtet das Feld kurz rot. */
    private int blink = 0;
    /** Original: {@code color} - 0x00FF00 im Normalbetrieb, 0xFF0000 waehrend des Blinkens. */
    private int color = 0x00FF00;
    /** Original: {@code muffled} aus {@code TileEntityMachineBase} - unterdrueckt den Aufprallklang. */
    private boolean muffled = false;
    private float radius = BASE_RADIUS;
    /** Original: {@code isOn = false} - der Schalter startet aus. */
    private boolean isOn = false;

    /** Wer beim letzten Tick draussen beziehungsweise drinnen war. */
    private final List<Entity> outside = new ArrayList<>();
    private final List<Entity> inside = new ArrayList<>();

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> health;
                case 1 -> maxHealth;
                case 2 -> (int) radius;
                case 3 -> cooldown;
                case 4 -> powerCons;
                case 5 -> isOn ? 1 : 0;
                case 6 -> color;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public ForceFieldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORCE_FIELD_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ForceFieldBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();
        be.chargeFromBatterySlot(SLOT_BATTERY);
        be.applyUpgrades();

        // Original: das Blinken laeuft ab und schaltet die Farbe zurueck.
        if (be.blink > 0) {
            be.blink--;
            be.color = 0xFF0000;
        } else {
            be.color = 0x00FF00;
        }

        if (be.cooldown > 0) {
            be.cooldown--;
        } else if (be.health < be.maxHealth) {
            // Original: Regeneration um ein Hundertstel des Maximums je Tick (ganzzahlig).
            be.health = Math.min(be.maxHealth, be.health + be.maxHealth / 100);
        }

        if (be.isOn && be.cooldown == 0 && be.health > 0 && be.getEnergyStored() >= be.powerCons) {
            be.doField(level, pos, be.radius);
            be.setEnergyStored(be.getEnergyStored() - be.powerCons);
        } else {
            be.outside.clear();
            be.inside.clear();
        }

        // Original: reicht die Energie nicht fuer einen Tick, verfaellt der ganze Rest.
        if (be.getEnergyStored() < be.powerCons) be.setEnergyStored(0L);

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Original: die Aufwertungen werden nach Stapelgroesse gewertet. */
    private void applyUpgrades() {
        ItemStack radiusStack = getInventory().getStackInSlot(SLOT_UPGRADE_RADIUS);
        ItemStack healthStack = getInventory().getStackInSlot(SLOT_UPGRADE_HEALTH);

        int radiusCount = radiusStack.is(ModItems.UPGRADE_RADIUS.get()) ? radiusStack.getCount() : 0;
        int healthCount = healthStack.is(ModItems.UPGRADE_HEALTH.get()) ? healthStack.getCount() : 0;

        radius = BASE_RADIUS + radiusCount * RADIUS_PER_UPGRADE;
        maxHealth = BASE_HEALTH + healthCount * HEALTH_PER_UPGRADE;
        if (health > maxHealth) health = maxHealth;

        powerCons = BASE_CONSUMPTION + radiusCount * RADIUS_CONSUMPTION + healthCount * SHIELD_CONSUMPTION;
    }

    /**
     * 1:1-Port von {@code doField}: wer die Kugelschale durchquert, wird zurueckgesetzt und das
     * Feld nimmt Schaden.
     */
    private void doField(Level level, BlockPos pos, float rad) {
        List<Entity> wasOutside = new ArrayList<>(outside);
        List<Entity> wasInside = new ArrayList<>(inside);

        outside.clear();
        inside.clear();

        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;

        // Original: der Suchbereich reicht 25 Bloecke ueber den Radius hinaus.
        AABB box = new AABB(cx - (rad + 25), cy - (rad + 25), cz - (rad + 25),
                            cx + (rad + 25), cy + (rad + 25), cz + (rad + 25));

        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            // Original: Spieler gehen ungehindert durch.
            if (entity instanceof Player) continue;

            double dist = Math.sqrt(Math.pow(cx - entity.getX(), 2)
                    + Math.pow(cy - entity.getY(), 2)
                    + Math.pow(cz - entity.getZ(), 2));
            boolean out = dist > rad;

            boolean knownOutside = wasOutside.contains(entity);
            boolean knownInside = wasInside.contains(entity);

            if (knownOutside && !out) {
                // Von aussen hereingekommen - wieder hinausschieben.
                bounce(level, entity, cx, cy, cz, rad + 1, false);
                outside.add(entity);
                damage(impact(entity));
            } else if (knownInside && out) {
                // Von innen hinausgekommen - wieder hereinholen.
                bounce(level, entity, cx, cy, cz, rad - 1, true);
                inside.add(entity);
                damage(impact(entity));
            } else if (out) {
                outside.add(entity);
            } else {
                inside.add(entity);
            }
        }
    }

    /** Setzt die Kreatur auf die Schale zurueck und dreht ihren Schwung um. */
    private void bounce(Level level, Entity entity, double cx, double cy, double cz,
                        double targetDist, boolean inward) {
        Vec3 toCenter = new Vec3(cx - entity.getX(), cy - entity.getY(), cz - entity.getZ()).normalize();

        double speed = entity.getDeltaMovement().length();
        double sign = inward ? 1D : -1D;
        Vec3 newMotion = toCenter.scale(sign * speed);

        // Original: erst auf die Schale setzen, dann den Schwung wieder abziehen - sonst schiebt
        // die naechste Bewegung die Kreatur gleich wieder hindurch.
        entity.teleportTo(cx - toCenter.x * targetDist - newMotion.x,
                          cy - toCenter.y * targetDist - newMotion.y,
                          cz - toCenter.z * targetDist - newMotion.z);

        entity.setDeltaMovement(newMotion);
        entity.hurtMarked = true;

        // Original: playSoundAtEntity(entity, "hbm:weapon.sparkShoot", 2.5F, 1.0F) - beim
        // hereinkommenden Aufprall nur, wenn das Feld nicht gedaempft ist.
        if (!inward || !isMuffled()) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    com.hbm_m.sound.ModSounds.SPARK_SHOOT.get(), SoundSource.BLOCKS, 2.5F, 1.0F);
        }
    }

    /** Original: {@code impact} - Masse mal Geschwindigkeit mal 50. */
    private static int impact(Entity e) {
        double mass = e.getBbHeight() * e.getBbWidth() * e.getBbWidth();
        double speed = e.getDeltaMovement().length();
        return (int) (mass * speed * 50);
    }

    /** Original: {@code damage} - bei null Schild geht das Feld in die Abkuehlung. */
    private void damage(int amount) {
        health -= amount;

        // Original: {@code if(ouch >= maxHealth / 250) blink = 5;}
        if (amount >= maxHealth / 250) blink = 5;

        if (health <= 0) {
            health = 0;
            cooldown = (int) (100 + radius);
        }
    }

    // ── Anzeige ─────────────────────────────────────────────────────────────

    /** Die Feldkugel reicht weit ueber den Block hinaus - sonst verschwindet sie am Bildrand. */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(radius + 2D);
    }

    public int getHealth()      { return health; }
    public int getMaxHealth()   { return maxHealth; }
    public float getRadius()    { return radius; }
    public int getCooldown()    { return cooldown; }
    public int getPowerCons()   { return powerCons; }
    public int getColor()       { return color; }
    public boolean isMuffled()  { return muffled; }
    public void setMuffled(boolean muffled) { this.muffled = muffled; setChanged(); }
    public boolean isOn()       { return isOn; }
    public boolean isFieldOn()  { return isOn && cooldown == 0 && health > 0; }

    /** Original: {@code AuxButtonPacket} mit Schluessel 0 - der Schalter in der Oberflaeche. */
    public void toggle() {
        isOn = !isOn;
        setChanged();
    }

    public ContainerData getContainerData() {
        return data;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyReceiverItem(stack);
        if (slot == SLOT_UPGRADE_RADIUS) return stack.is(ModItems.UPGRADE_RADIUS.get());
        if (slot == SLOT_UPGRADE_HEALTH) return stack.is(ModItems.UPGRADE_HEALTH.get());
        return false;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("health", health);
        tag.putInt("cooldown", cooldown);
        tag.putBoolean("isOn", isOn);
        tag.putInt("blink", blink);
        tag.putBoolean("muffled", muffled);
        tag.putInt("color", color);
        // Der Renderer braucht die Reichweite auch auf der Gegenseite - er zeichnet die Kugel.
        tag.putFloat("radius", radius);
        tag.putInt("maxHealth", maxHealth);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        // Ohne Tag bleibt es beim Grundwert - sonst stuende ein frisch gesetztes Feld auf null.
        if (tag.contains("health")) health = tag.getInt("health");
        cooldown = tag.getInt("cooldown");
        isOn = tag.getBoolean("isOn");
        blink = tag.getInt("blink");
        muffled = tag.getBoolean("muffled");
        color = tag.contains("color") ? tag.getInt("color") : 0x00FF00;
        if (tag.contains("radius")) radius = tag.getFloat("radius");
        if (tag.contains("maxHealth")) maxHealth = tag.getInt("maxHealth");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.machine_forcefield");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ForceFieldMenu(id, inv, this);
    }
}
