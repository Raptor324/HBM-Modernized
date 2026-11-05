package com.hbm_m.interfaces;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/** Maschine, die sich merkt, wer sie aufgestellt hat. Gesetzt wird das beim Platzieren. */
public interface IOwnedMachine {

    void setOwner(UUID owner);

    @Nullable
    UUID getOwner();
}
