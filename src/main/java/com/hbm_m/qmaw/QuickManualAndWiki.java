package com.hbm_m.qmaw;

import java.util.HashMap;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.qmaw.QuickManualAndWiki}: ein Handbucheintrag mit Titel und Text je Sprache. */
public class QuickManualAndWiki {

    public String name;
    public ItemStack icon;

    /** Removes this manual from the calculator search feature */
    public boolean noIndex = false;

    public HashMap<String, String> title = new HashMap<>();
    public HashMap<String, String> contents = new HashMap<>();

    public QuickManualAndWiki(String name) {
        this.name = name;
    }

    public QuickManualAndWiki setIcon(ItemStack stack) {
        this.icon = stack;
        return this;
    }

    public QuickManualAndWiki addTitle(String lang, String title) {
        this.title.put(lang, title);
        return this;
    }

    public QuickManualAndWiki addLang(String lang, String contents) {
        this.contents.put(lang, contents);
        return this;
    }

    public QuickManualAndWiki noIndex() {
        this.noIndex = true;
        return this;
    }
}
