package ru.zont.gfdb.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Parser {
    public static final int FILES_COUNT = 6;
    @SuppressWarnings("WeakerAccess")
    public static final int LIST_GP = 0;
    @SuppressWarnings("WeakerAccess")
    public static final int LIST_FWS = 1;
    @SuppressWarnings("WeakerAccess")
    public static final int STATS_GP = 2;
    @SuppressWarnings("WeakerAccess")
    public static final int STATS_FWS = 3;
    @SuppressWarnings("WeakerAccess")
    public static final int LIST_WIKI = 4;
    @SuppressWarnings("WeakerAccess")
    public static final int CRAFTLIST_WIKI = 5;

    private String gameServer;

    private File gpListFile;
    private File gpStatFile;
    private File fwsListFile;
    private File fwsStatFile;
    private File wikiListFile;
    private File wikiCraftlistFile;

    private boolean preparsed = false;
    private ArrayList<HashMap<String, String>> listGP;
    private ArrayList<HashMap<String, String>> statsGP;
    private Elements listFWS;
    private Elements statsFWS;
    private Elements listWiki;
    private Elements craftlistWiki;

    public Parser(File cacheDir, String server, boolean clearCache) {
        gameServer = server;

        gpListFile = new File(cacheDir, "list_gamepress.json");
        gpStatFile = new File(cacheDir, "stats_gamepress.json");
        fwsListFile = new File(cacheDir, "list_gffws.html");
        fwsStatFile = new File(cacheDir, "stats_gffws.html");
        wikiListFile = new File(cacheDir, "list_wiki.html");
        wikiCraftlistFile = new File(cacheDir, "caftlist_wiki.html");

        if (clearCache) {
            gpListFile.delete();
            gpStatFile.delete();
            fwsListFile.delete();
            fwsStatFile.delete();
            wikiListFile.delete();
            wikiCraftlistFile.delete();
        }
    }

    public Parser(File cacheDir, String server) {
        this(cacheDir, server, false);
    }

    public void download(int file) {
        File f; URL url;
        try {
            switch (file) {
                case LIST_GP:
                    f = gpListFile;
                    url = new URL("https://gamepress.gg/sites/default/files/aggregatedjson/t-dolls-list.json");
                    break;
                case LIST_FWS:
                    f = fwsListFile;
                    url = new URL("https://gf.fws.tw/db/guns/alist");
                    break;
                case STATS_GP:
                    f = gpStatFile;
                    url = new URL("https://gamepress.gg/sites/default/files/aggregatedjson/GFLStatRankings.json");
                    break;
                case STATS_FWS:
                    f = fwsStatFile;
                    url = new URL("https://gf.fws.tw/db/guns/table_list");
                    break;
                case LIST_WIKI:
                    f = wikiListFile;
                    url = new URL("https://en.gfwiki.com/wiki/T-Doll_Index");
                    break;
                case CRAFTLIST_WIKI:
                    f = wikiCraftlistFile;
                    url = new URL("https://en.gfwiki.com/wiki/T-Doll_Production");
                    break;
                default:
                    return;
            }
        } catch (MalformedURLException e) { e.printStackTrace(); return; }

        if (f.exists()) f.delete();
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(f)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1)
                fileOutputStream.write(dataBuffer, 0, bytesRead);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void preparse() throws IOException {
        Type type = new TypeToken<ArrayList<HashMap<String, String>>>() {}.getType();
        listGP = new Gson().fromJson(new JsonReader(new FileReader(gpListFile)), type);
        statsGP = new Gson().fromJson(new JsonReader(new FileReader(gpStatFile)), type);
        listFWS = Jsoup.parse(fwsListFile, "UTF-8").body()
                .getElementsByClass("gun_card_list");
        statsFWS = Jsoup.parse(fwsStatFile, "UTF-8").body()
                .getElementsByAttributeValue("id", "datatable").first()
                .getElementsByTag("tbody").first()
                .getElementsByTag("tr");
        listWiki = Jsoup.parse(wikiListFile, "UTF-8").body()
                .getElementsByClass("card-bg-small");
        craftlistWiki = Jsoup.parse(wikiCraftlistFile, "UTF-8").body()
                .getElementsByClass("gf-table");
        preparsed = true;
    }

    private void check() throws IOException {
        if (!gpListFile.exists() || !fwsListFile.exists() || !gpStatFile.exists()
                || !fwsStatFile.exists() || !wikiListFile.exists() || !wikiCraftlistFile.exists())
            throw new ListNotPreparedException("File has not found");
    }

    public TDolls getList() throws IOException {
        check();
        if (!preparsed) preparse();

        TDolls list = new TDolls();
        switch (gameServer) {
            case "EN":
                for (HashMap<String, String> entry : listGP) {
                    String id = entry.get("id");
                    if (id != null) list.add(new TDoll(Integer.valueOf(id)));
                }
                return list;
            case "TW":
                for (Element card : listFWS) {
                    String id = null;
                    try {
                        id = card.getElementsByTag("a").first().attr("href")
                                .replace("/db/guns/info/", "");
                    } catch (Exception e) { new ParserException("Failed to get ID",e).printStackTrace(); }
                    if (id != null) list.add(new TDoll(Integer.valueOf(id)));
                }
                return list;
            default: return list;
        }
    }

    public void baseParse(TDoll doll) throws IOException {
        check();
        if (!preparsed) preparse();

        switch (gameServer) {
            case "EN":
                baseParseEN(doll);
                doll.parsingLevel = 1;
                return;
            case "TW":
                baseParseTW(doll);
                doll.parsingLevel = 1;
        }
    }

    public ArrayList<ParserException> fullParse(TDoll doll) throws ParserException {
        if (doll.parsingLevel < 1)
            throw new ParserException(doll.toString() + "'s parsing level is < 1");

        ArrayList<ParserException> exceptions = new ArrayList<>();
        switch (gameServer) {
            case "EN":
                fullParseEN(doll, exceptions);
                doll.parsingLevel = 2;
                return exceptions;
            case "TW":
                fullParseTW(doll, exceptions);
                doll.parsingLevel = 2;
                return exceptions;
            default: throw new ParserException("Unknown server");
        }
    }

    @SuppressLint("DefaultLocale")
    @SuppressWarnings("ConstantConditions")
    private void baseParseEN(TDoll doll) throws ParserException {
        HashMap<String, String> entry = null;
        for (HashMap<String, String> e : listGP)
            if (Objects.equals(e.get("id"), doll.id + ""))
                entry = e;
        if (entry == null) throw new ParserException("ID not found in the list");

        Element fwsEntry = null;
        for (Element e : listFWS) {
            try {
                if (e.getElementsByAttributeValueStarting("href", "/db/guns/info/").first()
                        .attr("href").replace("/db/guns/info/", "")
                        .equals(doll.id + "")) {
                    fwsEntry = e;
                    break;
                }
            } catch (Exception ignored) {}
        }
        if (fwsEntry == null) Log.w("PARSER", String.format("%d FWS ENTRY HAS NOT FOUND!", doll.getId()));

        for (Element e : listWiki) {
            try {
                if (Integer.parseInt(e.getElementsByTag("span").get(1)
                        .text().split("&")[0]) == doll.id) {
                    doll.wiki = new URL("https://en.gfwiki.com" +
                            e.getElementsByTag("a").first().attr("href"));
                }
            } catch (Throwable ignored) { }
        }
        if (doll.wiki == null) Log.w("PARSER", String.format("%d WIKI ENTRY HAS NOT FOUND!", doll.getId()));

        try {
            if (fwsEntry != null) doll.thumb = new URL("http://gf.fws.tw"
                    + fwsEntry.getElementsByAttributeValueContaining("style", "background-image: url(")
                    .first().attr("style").replaceAll("background-image: url\\(", "").replaceAll("\\);", ""));
            else doll.thumb = new URL("https://gamepress.gg" + entry.get("icon").replaceAll("\\\\", ""));
            doll.name = entry.get("title");
            doll.type = entry.get("tdoll_class");
            doll.rarity = Integer.valueOf(entry.get("rarity").replace("rarity-", "").replace("ex", "6"));
            doll.craftTime = entry.get("field_t_doll_craft_time"); if (doll.craftTime.isEmpty()) doll.craftTime = "Unbuildable";
            doll.acc = Integer.parseInt(entry.get("acc"));
            doll.dmg = Integer.parseInt(entry.get("dmg"));
            doll.eva = Integer.parseInt(entry.get("eva"));
            doll.hp = Integer.parseInt(entry.get("hp"));
            doll.rof = Integer.parseInt(entry.get("rof"));
            try {
                doll.hpBar = (int) ((float) doll.hp / (float) getHighest("hp", statsGP) * 100);
                doll.dmgBar = (int) ((float) doll.dmg / (float) getHighest("dmg", statsGP) * 100);
                doll.accBar = (int) ((float) doll.acc / (float) getHighest("acc", statsGP) * 100);
                doll.evaBar = (int) ((float) doll.eva / (float) getHighest("eva", statsGP) * 100);
                doll.rofBar = (int) ((float) doll.rof / (float) getHighest("rof", statsGP) * 100);
            } catch (Exception pe) {
                new ParserException("Failed to parse statBars", pe).printStackTrace();
                doll.hpBar = 0; doll.dmgBar = 0; doll.accBar = 0;
                doll.evaBar = 0; doll.rofBar = 0;
            }

            doll.craftReqs = null;
            doll.heavyCraftReqs = null;
            Elements list = craftlistWiki.first().getElementsByTag("a");
            if (!doll.craftTime.equals("Unbuildable")) {
                for (Element e : list) {
                    if (e.attr("href").equals(doll.wiki.toString()
                            .replace("https://en.gfwiki.com", ""))) {
                        Element td = e.parent().parent();
                        if (td.nextElementSibling() != null && !td.nextElementSibling().hasAttr("colspan")) {

                            int man = Integer.valueOf(td.nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int ammo = Integer.valueOf(td.nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int ration = Integer.valueOf(td.nextElementSibling().nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int parts = Integer.valueOf(td.nextElementSibling().nextElementSibling().nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            doll.craftReqs = String.format("%s/%s/%s/%s", man, ammo, ration, parts);
                        } else if (td.nextElementSibling() != null) {
                            int val = Integer.valueOf(td.nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            doll.craftReqs = "SUM:" + val;
                        }
                    }
                }
                list = craftlistWiki.get(1).getElementsByTag("a");
                for (Element e : list) {
                    if (e.attr("href").equals(doll.wiki.toString()
                            .replace("https://en.gfwiki.com", ""))) {
                        Element td = e.parent().parent();
                        if (td.nextElementSibling() != null && !td.nextElementSibling().hasAttr("colspan")) {

                            int man = Integer.valueOf(td.nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int ammo = Integer.valueOf(td.nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int ration = Integer.valueOf(td.nextElementSibling().nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int parts = Integer.valueOf(td.nextElementSibling().nextElementSibling().nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            doll.heavyCraftReqs = String.format("%s/%s/%s/%s", man, ammo, ration, parts);
                        } else if (td.nextElementSibling() != null) {
                            int val = Integer.valueOf(td.nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            doll.heavyCraftReqs = "SUM:" + val;
                        } else doll.heavyCraftReqs = "1000/1000/1000/1000";
                    }
                }
            }

            doll.gamepress = new URL("https://girlsfrontline.gamepress.gg" + entry.get("path").replaceAll("\\\\", ""));
            doll.fws = new URL("http://gf.fws.tw/db/guns/info/" + doll.id);
        } catch (Exception e) {
            throw new ParserException(e);
        }
    }

    @SuppressLint("DefaultLocale")
    private void baseParseTW(TDoll doll) throws ParserException {
        Element entry = null;
        for (Element e : listFWS) {
            try {
                if (e.getElementsByAttributeValueStarting("href", "/db/guns/info/").first()
                        .attr("href").replace("/db/guns/info/", "")
                        .equals(doll.id + "")) {
                    entry = e;
                    break;
                }
            } catch (Exception ignored) { }
        }
        if (entry == null) throw new ParserException("Entry in alist has not found");

        Element tableEntry = null;
        for (Element e : statsFWS) {
            try {
                if (e.getElementsByAttributeValueStarting("href", "/db/guns/info/").first()
                        .attr("href").replace("/db/guns/info/", "")
                        .equals(doll.id + "")) {
                    tableEntry = e;
                    break;
                }
            } catch (Exception ignored) { }
        }
        if (tableEntry == null) throw new ParserException("Entry in table has not found");

        for (Element e : listWiki) {
            try {
                if (Integer.parseInt(e.getElementsByTag("span").get(1)
                        .text().split("&")[0]) == doll.id) {
                    doll.wiki = new URL("https://en.gfwiki.com" +
                            e.getElementsByTag("a").first().attr("href"));
                }
            } catch (Throwable ignored) { }
        }
        if (doll.wiki == null) Log.w("PARSER", String.format("%d WIKI ENTRY HAS NOT FOUND!", doll.getId()));

        try {
            Elements td1 = tableEntry.getElementsByTag("td");
            doll.name = entry.getElementsByTag("h4").first().getElementsByTag("a").text();
            doll.rarity = td1.get(1).text().equals("EXTRA") ? 6 : Integer.valueOf(td1.get(3).text());
            doll.type = entry.getElementsByTag("u").text();
            doll.craftTime = td1.first().text(); if (!doll.craftTime.matches("\\d{2}:\\d{2}")) doll.craftTime = "Unbuildable";
            doll.dmg = Integer.parseInt(td1.get(5).getElementsByClass("hidden").text());
            doll.acc = Integer.parseInt(td1.get(6).getElementsByClass("hidden").text());
            doll.eva = Integer.parseInt(td1.get(7).getElementsByClass("hidden").text());
            doll.rof = Integer.parseInt(td1.get(8).getElementsByClass("hidden").text());
            doll.hp = Integer.parseInt(td1.get(9).getElementsByClass("hidden").text());
            try {
                doll.dmgBar = (int) ((float) doll.dmg / (float) getHighest("dmg", statsFWS) * 100);
                doll.accBar = (int) ((float) doll.acc / (float) getHighest("acc", statsFWS) * 100);
                doll.evaBar = (int) ((float) doll.eva / (float) getHighest("eva", statsFWS) * 100);
                doll.rofBar = (int) ((float) doll.rof / (float) getHighest("rof", statsFWS) * 100);
                doll.hpBar = (int) ((float) doll.hp / (float) getHighest("hp", statsFWS) * 100);
            } catch (Exception pe) {
                new ParserException("Failed to pars statBars", pe).printStackTrace();
                doll.hpBar = 0; doll.dmgBar = 0; doll.accBar = 0;
                doll.evaBar = 0; doll.rofBar = 0;
            }

            doll.craftReqs = null;
            doll.heavyCraftReqs = null;
            Elements list = craftlistWiki.first().getElementsByTag("a");
            if (!doll.craftTime.equals("Unbuildable")) {
                for (Element e : list) {
                    if (e.attr("href").equals(doll.wiki.toString()
                            .replace("https://en.gfwiki.com", ""))) {
                        Element td = e.parent().parent();
                        if (!td.nextElementSibling().hasAttr("colspan")) {

                            int man = Integer.valueOf(td.nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int ammo = Integer.valueOf(td.nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int ration = Integer.valueOf(td.nextElementSibling().nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int parts = Integer.valueOf(td.nextElementSibling().nextElementSibling().nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            doll.craftReqs = String.format("%s/%s/%s/%s", man, ammo, ration, parts);
                        } else {
                            int val = Integer.valueOf(td.nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            doll.craftReqs = String.format("SUM:%d", val);
                        }
                    }
                }
                list = craftlistWiki.get(1).getElementsByTag("a");
                for (Element e : list) {
                    if (e.attr("href").equals(doll.wiki.toString()
                            .replace("https://en.gfwiki.com", ""))) {
                        Element td = e.parent().parent();
                        if (!td.nextElementSibling().hasAttr("colspan")) {

                            int man = Integer.valueOf(td.nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int ammo = Integer.valueOf(td.nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int ration = Integer.valueOf(td.nextElementSibling().nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            int parts = Integer.valueOf(td.nextElementSibling().nextElementSibling().nextElementSibling().nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            doll.heavyCraftReqs = String.format("%s/%s/%s/%s", man, ammo, ration, parts);
                        } else if (td.nextElementSibling() != null) {
                            int val = Integer.valueOf(td.nextElementSibling().text()
                                    .replaceAll("\\D+", ""));
                            doll.heavyCraftReqs = "SUM:" + val;
                        } else doll.heavyCraftReqs = "1000/1000/1000/1000";
                    }
                }
            }

            doll.thumb = new URL("http://gf.fws.tw"
                    + entry.getElementsByAttributeValueContaining("style", "background-image: url(")
                    .first().attr("style").replaceAll("background-image: url\\(", "").replaceAll("\\);", ""));
            doll.fws = new URL("https://gf.fws.tw/db/guns/info/"+doll.id);
        } catch (Exception e) {
            throw new ParserException(e);
        }
    }

    @SuppressLint("DefaultLocale")
    private void fullParseEN(TDoll doll, ArrayList<ParserException> exceptions) throws ParserException {
        Element root;
        try { root = Jsoup.connect(doll.gamepress.toString()).get().body(); }
        catch (IOException e) { throw new ParserException("Gamepress connection error", e); }
        Element gftwRoot = null;
        try { gftwRoot = Jsoup.connect(doll.fws.toString()).get().body(); }
        catch (IOException e) { e.printStackTrace(); }

        try {
            doll.cgMain = new URL("http://gamepress.gg"
                    + root.getElementsByAttributeValue("id", "tab-1-img").first()
                    .getElementsByTag("img").first().attr("src"));
            doll.cgDamage = new URL("http://gamepress.gg"
                    + root.getElementsByAttributeValue("id", "tab-2-img").first()
                    .getElementsByTag("img").first().attr("src"));
        } catch (Exception pe) { exceptions.add(new ParserException(doll, "Main CG", pe)); }

        if (gftwRoot != null) {
            try {
                doll.cgMainHQ = new URL("http://gf.fws.tw"
                        + gftwRoot.getElementsByClass("img-thumbnail").first().attr("src"));
                doll.cgDamageHQ = new URL("http://gf.fws.tw"
                        + gftwRoot.getElementsByClass("img-thumbnail").get(1).attr("src"));
            } catch (Exception pe) {
                exceptions.add(new ParserException(doll, "Main CG HQ", pe));
            }
        }

        doll.costitles = new ArrayList<>();
        doll.costumes = new ArrayList<>();
        try {
            Elements costumes = root.getElementsByClass("costume-row-item");
            if (costumes.size() % 2 > 0) {
                Log.e("PARSER_ERROR", "T-Doll ID" + doll.id + "; Invalid costumes count");
                throw new Exception("Invalid costumes count");
            }
            for (int i = 0; i < costumes.size(); i += 2) {
                Element normal = costumes.get(i);
                Element dmg = costumes.get(i + 1);

                doll.costitles.add(normal.parent().parent().parent().getElementsByTag("span").first().text());
                doll.costumes.add(new URL[]{
                        new URL(normal.getElementsByAttribute("href").first().attr("href")),
                        new URL(dmg.getElementsByAttribute("href").first().attr("href"))
                });
            }
        } catch (Exception pe) { exceptions.add(new ParserException(doll, "Aux CG", pe)); }

        try {
            Elements roles = root.getElementsByAttributeValueContaining("class", "t-doll-roles");
            if (roles.size() > 0) {
                StringBuilder b = new StringBuilder();
                for (Element e : roles.first().getElementsByClass("field__item"))
                    b.append(" ").append(e.text());
                doll.role = b.toString().replaceFirst(" ", "");
            } else doll.role = "";
        } catch (Exception pe) { exceptions.add(new ParserException(doll, "Roles", pe)); doll.role = ""; }

        try {
            Element p = root.getElementsContainingOwnText("History").first().nextElementSibling();
            if (p.tagName().equals("p")) doll.description = p.text();
            else doll.description = "";
        }  catch (Exception pe) { exceptions.add(new ParserException(doll, "Description", pe)); doll.description = ""; }

        try {
            if (gftwRoot == null) throw new ParserException("gf.fws.tw page is absent");
            Elements gridColumns = gftwRoot.getElementsByAttributeValueStarting("class","effect_grid_x");
            ArrayList<Element> dl = new ArrayList<>();
            for (Element e : gridColumns)
                if (e.getElementsByAttributeValueStarting("class", "gun_grid").size() != 3)
                    dl.add(e);
            for (Element e : dl) gridColumns.remove(e);
            doll.pattern = new int[3][3];
            for (int x = 0; x < gridColumns.size(); x++) {
                Elements cells = gridColumns.get(x).getElementsByAttributeValueStarting("class","gun_grid");
                for (int y = 0; y < cells.size(); y++) {
                    if (cells.get(y).className().contains("center")) doll.pattern[x][y] = 2;
                    else if (cells.get(y).className().contains("none")) doll.pattern[x][y] = 0;
                    else doll.pattern[x][y] = 1;
                }
            }
        } catch (Exception pe) { exceptions.add(new ParserException(doll, "pattern", pe)); }

        try {
            doll.affect = root.getElementsByClass("adj-effects").first().text().replace("Affects ", "");
        } catch (Exception pe) { exceptions.add(new ParserException(doll, "Affect", pe)); doll.affect = ""; }
        try {
            StringBuilder builder = new StringBuilder();
            Element table = root.getElementsByClass("adj-bonus").first().getElementsByTag("table").first();
            for (Element effect : table.getElementsByTag("tr")) {
                if (table.getElementsByTag("tr").indexOf(effect) > 0)
                    builder.append("<br />");
                builder.append(String.format("<b>%s:</b> ", effect.getElementsByTag("th").first().text()));
                for (Element val : effect.getElementsByTag("td"))
                    builder.append(effect.getElementsByTag("td").indexOf(val) > 0
                            ? ", " : "")
                            .append(val.text());
            }
            doll.buffs = builder.toString();
        } catch (Exception pe) { exceptions.add(new ParserException(doll, "Buffs", pe)); doll.buffs = ""; }
        try {
            Element skills = root.getElementsByAttributeValue("id", "t-doll-skill").first().nextElementSibling();
            for (Element e : skills.getElementsByTag("img"))
                e.attr("src", "https://girlsfrontline.gamepress.gg"
                        + e.attr("src"));
            doll.skills = skills.toString();
        } catch (Exception pe) { exceptions.add(new ParserException(doll, "Skills", pe)); doll.skills = ""; }

        /* Макет блока парсинга
        try {

        } catch (Exception pe) { exceptions.add(new ParserException(doll, "", pe)); }
        Здесь могла быть ваша реклама */
    }

    private static void fullParseTW(TDoll doll, ArrayList<ParserException> exceptions) throws ParserException {
        Element root;
        try {
            root = Jsoup.connect(doll.fws + "").get();
        } catch (IOException e) {
            throw new ParserException("gf.fws.tw connection error", e);
        }

        try {
            doll.cgMain = doll.thumb;
            doll.cgDamage = doll.thumb;
            doll.cgMainHQ = new URL("http://gf.fws.tw"
                    + root.getElementsByClass("img-thumbnail").first().attr("src"));
            doll.cgDamageHQ = new URL("http://gf.fws.tw"
                    + root.getElementsByClass("img-thumbnail").get(1).attr("src"));
        } catch (Exception pe) { exceptions.add(new ParserException(doll, "Main CG", pe)); }

        doll.costitles = new ArrayList<>();
        doll.costumes = new ArrayList<>();
        try {
            Elements skins = root.getElementsByAttributeValue("id", "skin");
            for (Element skin : skins) {
                doll.costitles.add(skin.getElementsByTag("h3").text());
                doll.costumes.add(new URL[] {
                        new URL("https://gf.fws.tw" +
                                skin.getElementsByClass("img-thumbnail")
                                        .first().attr("src")),
                        new URL("https://gf.fws.tw" +
                                skin.getElementsByClass("img-thumbnail")
                                        .get(1).attr("src"))
                });
            }
        } catch (Exception e) { exceptions.add(new ParserException(doll, "costumes", e)); }

        try {
            doll.description = root.getElementsByTag("article").first()
                    .getElementsByTag("section").get(1)
                    .getElementsByTag("p").text();
        } catch (Exception e) { exceptions.add(new ParserException(doll, "description", e)); doll.description = ""; }

        try {
            Elements gridColumns = root.getElementsByAttributeValueStarting("class","effect_grid_x");
            ArrayList<Element> dl = new ArrayList<>();
            for (Element e : gridColumns)
                if (e.getElementsByAttributeValueStarting("class", "gun_grid").size() != 3)
                    dl.add(e);
            for (Element e : dl) gridColumns.remove(e);
            doll.pattern = new int[3][3];
            for (int x = 0; x < gridColumns.size(); x++) {
                Elements cells = gridColumns.get(x).getElementsByAttributeValueStarting("class","gun_grid");
                for (int y = 0; y < cells.size(); y++) {
                    if (cells.get(y).className().contains("center")) doll.pattern[x][y] = 2;
                    else if (cells.get(y).className().contains("none")) doll.pattern[x][y] = 0;
                    else doll.pattern[x][y] = 1;
                }
            }
        } catch (Exception pe) { exceptions.add(new ParserException(doll, "pattern", pe)); }

        try {
            Elements buffs = root.getElementsByClass("effect_desc")
                    .first().getElementsByTag("li");
            StringBuilder builder = new StringBuilder();
            for (Element buff : buffs)
                builder.append(buff.text())
                        .append(buffs.indexOf(buff) < buffs.size() - 1
                                ? ";<br />" : "");
            doll.buffs = builder.toString();
        } catch (Exception e) { exceptions.add(new ParserException(doll, "buffs", e)); doll.buffs = ""; }

        try {
            doll.skills = root.getElementsByTag("article").first()
                    .getElementsByTag("section").get(5)
                    .getElementsByTag("table").first().toString();
        } catch (Exception e) { exceptions.add(new ParserException(doll, "skills", e)); doll.skills = ""; }

        doll.role = "";
        doll.affect = "";
    }

    private static int getHighest(String key, Elements list) {
        int i;
        switch (key) {
            case "dmg": i = 5; break;
            case "acc": i = 6; break;
            case "eva": i = 7; break;
            case "rof": i = 8; break;
            case "hp": i = 9; break;
            default: return 0;
        }
        int max = 0;
        for (Element tr : list) {
            Integer entry = Integer.valueOf(
                    tr.getElementsByTag("td").get(i)
                            .getElementsByClass("hidden").text());
            if (entry > max) max = entry;
        }
        return max;
    }

    private static int getHighest(String key, ArrayList<HashMap<String, String>> list) {
        int highest = -1;
        for (HashMap<String, String> entry : list)
            if (entry.containsKey(key))
                if (Integer.valueOf(Objects.requireNonNull(entry.get(key))) > highest)
                    highest = Integer.valueOf(Objects.requireNonNull(entry.get(key)));
        return highest;
    }

    private static class ListNotPreparedException extends IOException {
        private ListNotPreparedException(String message) { super(message); }
    }

    public static class ParserException extends IOException {
        private ParserException(Throwable caused) { super(caused); }
        private ParserException(String message) { super(message); }
        private ParserException(String message, Throwable caused) { super(message, caused); }
        private ParserException(TDoll doll, String element, Throwable caused) {
            this("Exception while parsing "+doll.toString()+"'s "+element, caused);
            printStackTrace();
        }
    }

    public static TDolls getCachedList(Context context) throws ParserException {
        File file = new File(context.getCacheDir(), "cachedList");
        if (!file.exists()) throw new ParserException("List has not found in cache");

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            TDolls res = (TDolls) ois.readObject();
            ois.close();
            return res;
        } catch (IOException | ClassNotFoundException e) {
            new File(context.getCacheDir(), "lastupd").delete();
            throw new ParserException("Error on reading list", e);
        }
    }

    public static String[] getCraftbleTypes(CraftRecipe craft, int craftType, Context context) {
        TDolls list;
        try {
            list = getCachedList(context);
        } catch (ParserException e) {
            e.printStackTrace();
            return new String[0];
        }

        ArrayList<String> res = new ArrayList<>();
        for (TDoll doll : list) {
            if (res.contains(doll.type)) continue;
            if (craft.isDollCraftable(doll, craftType)) res.add(doll.type);
        }
        return res.toArray(new String[0]);
    }
}
