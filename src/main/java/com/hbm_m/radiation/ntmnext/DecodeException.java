// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.util.DecodeException), Commit 3f9a261a.

package com.hbm_m.radiation.ntmnext;

/** Fehler beim Dekodieren gespeicherter Strahlungsdaten eines Chunks. */
final class DecodeException extends Exception {

    DecodeException(String message) {
        super(message);
    }
}
