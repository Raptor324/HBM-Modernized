package com.hbm_m.world.generator;

import com.hbm_m.world.generator.room.*;

/**
 * 1:1 {@code com.hbm.world.generator.CellularDungeonFactory}. Port: Das Original hielt ein statisches Verlies mit
 * Zustand; da 1.20 parallel baut, liefert {@link #jungle()} je Bau eine frische Instanz mit denselben Raeumen.
 */
public class CellularDungeonFactory {

    public static JungleDungeon jungle() {
        JungleDungeon jungle = new JungleDungeon(5, 5, 25, 25, 700, 6);
        for (int i = 0; i < 10; i++) jungle.rooms.add(new JungleDungeonRoom(jungle));
        jungle.rooms.add(new JungleDungeonRoomArrow(jungle));
        jungle.rooms.add(new JungleDungeonRoomArrowFire(jungle));
        jungle.rooms.add(new JungleDungeonRoomFire(jungle));
        jungle.rooms.add(new JungleDungeonRoomMagic(jungle));
        jungle.rooms.add(new JungleDungeonRoomMine(jungle));
        jungle.rooms.add(new JungleDungeonRoomPillar(jungle));
        jungle.rooms.add(new JungleDungeonRoomPoison(jungle));
        jungle.rooms.add(new JungleDungeonRoomRad(jungle));
        jungle.rooms.add(new JungleDungeonRoomRubble(jungle));
        jungle.rooms.add(new JungleDungeonRoomSlowness(jungle));
        jungle.rooms.add(new JungleDungeonRoomSpiders(jungle));
        jungle.rooms.add(new JungleDungeonRoomSpikes(jungle));
        jungle.rooms.add(new JungleDungeonRoomWeakness(jungle));
        jungle.rooms.add(new JungleDungeonRoomWeb(jungle));
        jungle.rooms.add(new JungleDungeonRoomZombie(jungle));
        return jungle;
    }
}
