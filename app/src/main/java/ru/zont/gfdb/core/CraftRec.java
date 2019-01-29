package ru.zont.gfdb.core;

import android.annotation.SuppressLint;

public class CraftRec {
    public int mp;
    public int ammo;
    public int rat;
    public int parts;
    public String string;

    public CraftRec(String str) {
        if (!str.matches("\\d{1,4}/\\d{1,4}/\\d{1,4}/\\d{1,4}")) return;
        String[] d = str.split("/");
        mp = Integer.valueOf(d[0]);
        ammo = Integer.valueOf(d[1]);
        rat = Integer.valueOf(d[2]);
        parts = Integer.valueOf(d[3]);
        string = str;
    }

    @SuppressLint("DefaultLocale")
    public CraftRec(int mp, int ammo, int rat, int parts) {
        this.mp = mp;
        this.ammo = ammo;
        this.rat = rat;
        this.parts = parts;
        string = String.format("%d/%d/%d/%d", mp, ammo, rat, parts);
    }
}
