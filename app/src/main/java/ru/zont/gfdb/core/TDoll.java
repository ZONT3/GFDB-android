package ru.zont.gfdb.core;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

public class TDoll implements Serializable {
    int parsingLevel = 0;
    int id;

    String name;
    int rarity;
    String type;
    String craftTime;

    int hp;
    int dmg;
    int acc;
    int eva;
    int rof;
    
    int hpBar;
    int dmgBar;
    int accBar;
    int evaBar;
    int rofBar;

    URL thumb;
    URL gamepress;
    URL fws;
    URL wiki;

    // ---------------------- LEVEL 2 PARSING
    URL cgMain;
    URL cgDamage;
    URL cgMainHQ;
    URL cgDamageHQ;
    ArrayList<String> costitles;
    ArrayList<URL[]> costumes;

    String role;
    String description;
    int[][] pattern;
    String affect;
    String buffs;
    String skills;

    @Nullable
    String craftReqs;
    @Nullable
    String heavyCraftReqs;

    @Nullable
    public String getCraftReqs() {
        return craftReqs;
    }

    @Nullable
    public String getHeavyCraftReqs() {
        return heavyCraftReqs;
    }

    TDoll(int id) { this.id = id; }

    public int getParsingLevel() { return parsingLevel; }

    public int getId() { return id; }

    @NonNull
    public String getName() { return name; }

    public int getRarity() { return rarity; }

    @NonNull
    public String getType() { return type; }

    @NonNull
    public String getCraftTime() { return craftTime; }

    public int getCraftTimeMins() {
        if (craftTime.equals("Unbuildable")) return Integer.MAX_VALUE;
        return Integer.valueOf(craftTime.split(":")[0]) * 60
                + Integer.valueOf(craftTime.split(":")[1]);
    }

    public int getHp() { return hp; }

    public int getDmg() { return dmg; }

    public int getAcc() { return acc; }

    public int getEva() { return eva; }

    public int getRof() { return rof; }

    public URL getThumb() { return thumb; }

    public URL getCgMain() { return cgMain; }

    public URL getCgDamage() { return cgDamage; }

    public URL getCgMainHQ() { return cgMainHQ; }

    public URL getCgDamageHQ() { return cgDamageHQ; }

    public ArrayList<URL[]> getCostumes() { return costumes; }

    public String getDescription() { return description; }

    public int[][] getPattern() { return pattern; }

    public String getSkills() { return skills; }

    public int getHpBar() { return hpBar; }

    public int getDmgBar() { return dmgBar; }

    public int getAccBar() { return accBar; }

    public int getEvaBar() { return evaBar; }

    public int getRofBar() { return rofBar; }

    public ArrayList<String> getCostitles() { return costitles; }

    public String getRole() { return role; }

    public String getAffect() { return affect; }

    public String getBuffs() { return buffs; }

    public URL getGamepress() { return gamepress; }

    public URL getFws() { return fws; }

    @Nullable
    public URL getWiki() { return wiki; }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TDoll) return ((TDoll) obj).id == id;
        return super.equals(obj);
    }

    @NonNull
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("[%d*] %s", rarity, name);
    }
}
