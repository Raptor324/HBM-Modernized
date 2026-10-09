package com.hbm_m.satellite;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code SatelliteScience} (Weltraumlabor): erzeugt alle 15 Minuten eine Flugdaten-Disk fuer das Bandlaufwerk.
 * Nachgelieferte Sensorrelais sammeln Messungen fuer Orbitdaten (100 Stunden je Relais), nachgelieferte
 * 0G-Fabriken arbeiten Orbital-Baugruppen der Reihe nach ab ({@link SpaceAssemblerRecipes}); die fertige Fracht holt
 * ein Sat-Dock per Landekapsel ab.
 */
public class SatelliteScience extends Satellite {

    public static final int COOLDOWN = 15 * 60 * 20;
    public long lastScience;

    public static final int SENSOR_DURATION = 100 * 60 * 60 * 20; // 100 fucking hour recipe
    public int sensorProgress;
    public int sensorCount;

    public int assemblerCount;
    public double assemblerProgress;

    /** FIFO der Rezeptnamen ({@link SpaceAssemblerRecipes.Recipe#name}). */
    public final List<String> assemblerTasks = new ArrayList<>();

    @Override
    public String getType() {
        return "SCIENCE_PROBE";
    }

    @Override
    public boolean hasData(ServerLevel world) {
        if (super.hasData(world)) return true;

        if (world.getGameTime() > this.lastScience + COOLDOWN) {
            this.produceData("drive_disk_empty", "drive_disk_flightdata");
            this.lastScience = world.getGameTime();
            this.markDirty();
        }

        return super.hasData(world);
    }

    @Override
    public void onPartDelivered(ServerLevel world, ItemStack part) {

        if (part.is(ModItems.SATELLITE_SCIENCE_SENSOR.get())) {
            this.sensorCount++;
            this.markDirty();
            return;
        }
        if (part.is(ModItems.SATELLITE_SCIENCE_ASSEMBLER.get())) {
            this.assemblerCount++;
            this.markDirty();
            return;
        }

        SpaceAssemblerRecipes.Recipe recipe = SpaceAssemblerRecipes.getRecipe(part);
        if (recipe != null) {
            this.assemblerTasks.add(recipe.name());
            this.markDirty();
        }
    }

    @Override
    public void onUpdateTick(ServerLevel world) {

        // Sensorrelais
        if (this.sensorProgress < SENSOR_DURATION) {
            if (this.sensorCount > 0) {
                this.sensorProgress += this.sensorCount;
                this.markDirty();
            }
        } else {
            this.sensorProgress = 0;
            this.produceData("drive_disk_empty", "drive_disk_orbitdata");
            this.markDirty();
        }

        // 0G-Fabrik
        if (this.assemblerCount > 0 && this.requestableSlots.length <= 0 && !this.assemblerTasks.isEmpty()) {

            SpaceAssemblerRecipes.Recipe task = SpaceAssemblerRecipes.byName(this.assemblerTasks.get(0));
            int duration = task != null ? task.duration() : 1;
            this.assemblerProgress += (double) this.assemblerCount / duration;

            if (this.assemblerProgress >= 1) {
                if (task != null) {
                    List<ItemStack> out = task.outputs().get();
                    this.requestableSlots = new ItemStack[out.size()];
                    for (int i = 0; i < out.size(); i++) this.requestableSlots[i] = out.get(i).copy();
                }
                this.assemblerProgress = 0;
                this.assemblerTasks.remove(0);
                this.markDirty();
            }
        }
    }

    @Override
    public List<Component> getInfo(ServerLevel world) {
        List<Component> info = new ArrayList<>();
        int cooldown = (int) ((lastScience + COOLDOWN) - world.getGameTime());
        int seconds = cooldown / 20;

        info.add(Component.translatable("item.hbm_m.satellite_science"));
        info.add(cooldown <= 0 ? Component.translatable("satellite.ready") : Component.translatable("satellite.cooldown", (seconds / 60) + "m" + (seconds % 60) + "s"));

        if (this.sensorCount > 0) {
            info.add(Component.translatable("satellite.sensors", this.sensorCount));
            info.add(Component.translatable("satellite.pending", String.format(java.util.Locale.ROOT, "%,d", SENSOR_DURATION - sensorProgress)));
        }
        if ("drive_disk_orbitdata".equals(this.driveOutput)) info.add(Component.translatable("satellite.data"));

        if (this.assemblerCount > 0) {
            info.add(Component.translatable("satellite.assemblers", this.assemblerCount));
            info.add(Component.translatable("satellite.progress", (int) Math.round(this.assemblerProgress * 100) + "%"));
            info.add(Component.translatable("satellite.queue", this.assemblerTasks.size()));
        }
        return info;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putLong("lastScience", lastScience);
        nbt.putInt("sensorProgress", sensorProgress);
        nbt.putInt("sensorCount", sensorCount);
        nbt.putInt("assemblerCount", assemblerCount);
        nbt.putDouble("assemblerProgress", assemblerProgress);

        nbt.putInt("taskCount", this.assemblerTasks.size());
        for (int i = 0; i < this.assemblerTasks.size(); i++) nbt.putString("task" + i, this.assemblerTasks.get(i));
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        lastScience = nbt.getLong("lastScience");
        sensorProgress = nbt.getInt("sensorProgress");
        sensorCount = nbt.getInt("sensorCount");
        assemblerCount = nbt.getInt("assemblerCount");
        assemblerProgress = nbt.getDouble("assemblerProgress");

        this.assemblerTasks.clear();
        int taskCount = nbt.getInt("taskCount");
        for (int i = 0; i < taskCount; i++) this.assemblerTasks.add(nbt.getString("task" + i));
    }
}
