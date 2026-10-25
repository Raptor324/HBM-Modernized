package com.hbm_m.handler.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** 1:1 {@code com.hbm.handler.ability.IBaseAbility}. */
public interface IBaseAbility extends Comparable<IBaseAbility> {

    String getName();

    default String getExtension(int level) {
        return "";
    }

    /** Original {@code getFullName}: uebersetzter Name + Stufenzusatz. */
    default MutableComponent getFullName(int level) {
        return Component.translatable(getName()).append(getExtension(level));
    }

    default boolean isAllowed() {
        return true;
    }

    // 1 means no support for levels (i.e. the level is always 0).
    // The UI only supports levels() between 1 and 10 (inclusive).
    // All calls accepting an `int level` parameters must be done
    // with a level between 0 and levels()-1 (inclusive).
    default int levels() {
        return 1;
    }

    default int sortOrder() {
        return hashCode();
    }

    @Override
    default int compareTo(IBaseAbility o) {
        return sortOrder() - o.sortOrder();
    }
}
