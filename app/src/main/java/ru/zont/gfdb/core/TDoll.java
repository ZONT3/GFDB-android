package ru.zont.gfdb.core;

import android.annotation.SuppressLint;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
public class TDoll implements Serializable {
    protected int parsingLevel = 0;
    protected int id;

    protected String name;
    protected int rarity;
    protected String clss;
    protected String craft;

    protected int hp;
    protected int dmg;
    protected int acc;
    protected int eva;
    protected int rof;
    
    protected int hpBar;
    protected int dmgBar;
    protected int accBar;
    protected int evaBar;
    protected int rofBar;

    protected URL thumb;
    protected URL lvl2;
    protected URL lvl2fws;

    // ---------------------- LEVEL 2 PARSING
    protected URL cgMain;
    protected URL cgDamage;
    protected URL cgMainHQ;
    protected URL cgDamageHQ;
    protected ArrayList<String> costitles;
    protected ArrayList<URL[]> costumes;

    protected String role;
    protected String description;
    protected int[][] pattern;
    protected String affect;
    protected String buffs;
    protected String skills;

    public TDoll(int id) { this.id = id; }

    public int getId() { return id; }

    public String getName() { return name; }

    public int getRarity() { return rarity; }

    public String getClss() { return clss; }

    public String getCraft() { return craft; }

    public int getCraftMins() {
        if (craft.equals("Unbuildable")) return Integer.MAX_VALUE;
        return Integer.valueOf(craft.split(":")[0]) * 60
                + Integer.valueOf(craft.split(":")[1]);
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

    public URL getLvl2() { return lvl2; }

    public URL getLvl2fws() { return lvl2fws; }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TDoll) return ((TDoll) obj).id == id;
        return super.equals(obj);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("[%d*] %s", rarity, name);
    }
}
