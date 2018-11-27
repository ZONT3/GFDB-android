package ru.zont.gfdb.core;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

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

    // ---------------------- LEVEL 2 PARSING
    protected URL cgMain;
    protected URL cgDamage;
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

    public int getHp() { return hp; }

    public int getDmg() { return dmg; }

    public int getAcc() { return acc; }

    public int getEva() { return eva; }

    public int getRof() { return rof; }

    public URL getThumb() { return thumb; }

    public URL getCgMain() { return cgMain; }

    public URL getCgDamage() { return cgDamage; }

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
}
