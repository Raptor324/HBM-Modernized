package com.hbm_m.inventory.menu;

/**
 * Thrown by the client-side (FriendlyByteBuf) menu constructors when the block entity the
 * server opened the menu on does not exist in the client level - a replay world (Flashback)
 * or a chunk that is not loaded yet. Kept as an IllegalStateException so nothing else changes;
 * {@code MixinClientPayloadHandlerMissingTile} turns it into a warning instead of a disconnect.
 */
public class MenuBlockEntityMissingException extends IllegalStateException {

    public MenuBlockEntityMissingException(String message) {
        super(message);
    }
}
