package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.satellite.RayScanEvents;
import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineSatLink}: die Schuessel ist verbunden, wenn sie unter freiem Himmel steht und auf
 * ihrer Frequenz ein Satellit im Orbit ist. Funkfackeln lesen "connected"/"freq"/"rx" (Antwort des Satelliten)
 * und rufen "setfreq" und "tx" (Befehl an den Satelliten, meldet dabei ein Funkereignis) auf.
 */
public class MachineSatLinkBlockEntity extends BaseHbmBlockEntity implements IRORValueProvider, IRORInteractive {

    public boolean connected;
    public int freq;

    public float rot = INACTIVE_ROT;
    public float prevRot = INACTIVE_ROT;
    public float lift = INACTIVE_LIFT;
    public float prevLift = INACTIVE_LIFT;

    public static final float SPEED = 0.25F;
    public static final float ACTIVE_ROT = -15F;
    public static final float ACTIVE_LIFT = -45F;
    public static final float INACTIVE_ROT = 0F;
    public static final float INACTIVE_LIFT = -85F;

    public MachineSatLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SATLINK_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSatLinkBlockEntity be) {
        be.updateEntity();
    }

    public void updateEntity() {
        if (level instanceof ServerLevel server) {
            this.connected = false;

            boolean canConnect = server.getHeight(Heightmap.Types.MOTION_BLOCKING, worldPosition.getX(), worldPosition.getZ()) <= worldPosition.getY();
            if (canConnect) {
                this.connected = SatelliteManager.get(server).isFreqTaken(freq);
            }

            this.updateInfo(server, canConnect);
            this.networkPackNT(server, 150);
        } else {
            this.prevRot = this.rot;
            this.prevLift = this.lift;

            float targetR = this.connected ? ACTIVE_ROT : INACTIVE_ROT;
            float targetL = this.connected ? ACTIVE_LIFT : INACTIVE_LIFT;

            if (Math.abs(rot - targetR) <= SPEED) rot = targetR;
            else if (rot < targetR) rot += SPEED;
            else if (rot > targetR) rot -= SPEED;

            if (Math.abs(lift - targetL) <= SPEED) lift = targetL;
            else if (lift < targetL) lift += SPEED;
            else if (lift > targetL) lift -= SPEED;
        }
    }

    /** Original {@code info}: Statuszeilen des verbundenen Satelliten (z.B. Abklingzeit des Weltraumlabors). */
    public java.util.List<net.minecraft.network.chat.Component> info = java.util.List.of();

    /** Original {@code updateInfo}. */
    protected void updateInfo(ServerLevel server, boolean canConnect) {
        if (!canConnect) {
            this.info = java.util.List.of();
            return;
        }
        Satellite sat = SatelliteManager.get(server).getSatFromFreq(freq);
        if (sat != null) this.info = sat.getInfo(server);
    }

    private void networkPackNT(ServerLevel server, int range) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("connected", connected);
        tag.putInt("freq", freq);
<<<<<<< HEAD
        net.minecraft.nbt.ListTag lines = new net.minecraft.nbt.ListTag();
        for (net.minecraft.network.chat.Component line : info) {
            lines.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(line)));
        }
        tag.put("info", lines);
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this, be -> tag);
=======
        ClientboundBlockEntityDataPacket packet = com.hbm_m.platform.BlockHooks.dataPacket(this, tag);
>>>>>>> 5cf60b6108d271636c3d48b21b60db45caf65f6b
        for (var player : server.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false)) {
            if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= (double) range * range)
                player.connection.send(packet);
        }
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        if (tag.contains("connected")) this.connected = tag.getBoolean("connected");
        if (tag.contains("freq")) this.freq = tag.getInt("freq");
        if (tag.contains("info")) {
            net.minecraft.nbt.ListTag lines = tag.getList("info", 8);
            java.util.List<net.minecraft.network.chat.Component> parsed = new java.util.ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                net.minecraft.network.chat.Component line = net.minecraft.network.chat.Component.Serializer.fromJson(lines.getString(i));
                if (line != null) parsed.add(line);
            }
            this.info = parsed;
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        this.freq = nbt.getInt("freq");
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putInt("freq", freq);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 10, worldPosition.getZ() + 3);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "connected",
                PREFIX_VALUE + "freq",
                PREFIX_VALUE + "rx",
                PREFIX_FUNCTION + "setfreq" + NAME_SEPARATOR + "freq",
                PREFIX_FUNCTION + "tx" + NAME_SEPARATOR + "payload"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // Original vergleicht hier mit PREFIX_VALUE + connected (dem Wahrheitswert), nicht mit "connected"
        if (name.equals(PREFIX_VALUE + connected)) {
            return this.connected ? "TRUE" : "FALSE";
        }
        if (name.equals(PREFIX_VALUE + "freq")) {
            return "" + this.freq;
        }
        if (name.equals(PREFIX_VALUE + "rx") && level instanceof ServerLevel server) {
            Satellite sat = SatelliteManager.get(server).getSatFromFreq(this.freq);
            if (sat != null) {
                return sat.tx;
            }
            return "";
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {

        if (name.equals(PREFIX_FUNCTION + "setfreq") && params.length == 1) {
            this.freq = IRORInteractive.parseInt(params[0], 0, 100_000);
            this.setChanged();
        }

        if (name.equals(PREFIX_FUNCTION + "tx") && level instanceof ServerLevel server) {
            Satellite sat = SatelliteManager.get(server).getSatFromFreq(this.freq);
            String[] cmd = String.join(IRORInteractive.PARAM_SEPARATOR, params).split(" ");
            if (sat != null) {
                sat.onCommand(server, cmd);
                SatelliteManager.get(server).setDirty();
            }
            RayScanEvents.reportEvent(server, worldPosition, RayScanEvents.INFO_RADIO, 300);
            this.setChanged();
        }

        return null;
    }
}
