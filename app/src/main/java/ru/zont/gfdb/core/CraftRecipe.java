package ru.zont.gfdb.core;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

public class CraftRecipe {
    public int mp;
    public int ammo;
    public int rat;
    public int parts;

    public CraftRecipe(String str) {
        if (!str.matches("\\d+/\\d+/\\d+/\\d+")) return;
        String[] d = str.split("/");
        mp = Integer.valueOf(d[0]);
        ammo = Integer.valueOf(d[1]);
        rat = Integer.valueOf(d[2]);
        parts = Integer.valueOf(d[3]);
    }

    public CraftRecipe(int mp, int ammo, int rat, int parts) {
        this(getFormat(mp, ammo, rat, parts));
    }

    public boolean isDollCraftable(TDoll doll, int craftType) {
    if (craftType < 0)
        if (doll.craftReqs == null && doll.heavyCraftReqs == null
                || doll.craftTime.equals("Unbuildable")) return false;
    if (craftType == 0 && doll.craftReqs == null) return false;
    if (craftType > 0 && doll.heavyCraftReqs == null) return false;
    if (mp+ammo+rat+parts > 920 && doll.type.equals("HG")) return false;

    if ((craftType <= 0) && (doll.craftReqs != null)
            && doll.craftReqs.matches("SUM:\\d+")) {
        CraftRecipe c = new CraftRecipe(doll.craftReqs);
        if (Integer.valueOf(doll.craftReqs.replaceAll("SUM:", ""))
                >= (c.mp + c.ammo + c.rat + c.parts)) return true;
    }
    if ((craftType < 0 || craftType > 0) && doll.heavyCraftReqs != null
            && doll.heavyCraftReqs.matches("SUM:\\d+")) {
        CraftRecipe c = new CraftRecipe(doll.heavyCraftReqs);
        if (Integer.valueOf(doll.heavyCraftReqs.replaceAll("SUM:", ""))
                >= (c.mp + c.ammo + c.rat + c.parts)) return true;
    }

    CraftRecipe dollCraft;
    if (craftType < 0)
        //noinspection ConstantConditions
        dollCraft = new CraftRecipe(doll.craftReqs != null ? doll.craftReqs : doll.heavyCraftReqs);
    else if (craftType == 0)
        dollCraft = new CraftRecipe(doll.craftReqs);
    else dollCraft = new CraftRecipe(doll.heavyCraftReqs);

        return mp >= dollCraft.mp
            && ammo >= dollCraft.ammo
            && rat >= dollCraft.rat
            && parts >= dollCraft.parts;
    }

    public TDolls getCraftbleDolls(TDolls list, int craftType) {
        TDolls result = new TDolls();
        for (TDoll doll : list)
            if (isDollCraftable(doll, craftType))
                result.add(doll);
        return result;
    }

    @SuppressLint("DefaultLocale")
    @NonNull
    private static String getFormat(int mp, int ammo, int rat, int parts) {
        return String.format("%d/%d/%d/%d", mp, ammo, rat, parts);
    }

    @NonNull
    @Override
    public String toString() {
        return getFormat(mp, ammo, rat, parts);
    }

    public static boolean isDollCraftable(TDoll doll, String craft, int craftType) {
        return new CraftRecipe(craft).isDollCraftable(doll, craftType);
    }

    public static TDolls getCraftable(TDolls list, String craft, int craftType) {
        return new CraftRecipe(craft).getCraftbleDolls(list, craftType);
    }
}
