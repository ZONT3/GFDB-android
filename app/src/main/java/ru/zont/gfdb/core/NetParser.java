package ru.zont.gfdb.core;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class NetParser {
    public static final int TDLIST = 0;
    public static final int TNLIST = 1;
    public static final int STATLIST = 2;
    public static final int AUX = 3;

    private File jsonFile;
    private File statFile;
    private File gftwFile;

    private ArrayList<HashMap<String, String>> list;
    private ArrayList<HashMap<String, String>> stats;
    private Element gftw;

    public NetParser(Context context, int obj) throws IOException {
        if (obj < 0) {
            jsonFile = new File(context.getCacheDir(), "doc.json");
            statFile = new File(context.getCacheDir(), "stats.json");
            gftwFile = new File(context.getCacheDir(), "gftw.html");
            if (!jsonFile.exists()) jsonFile = null;
            if (!gftwFile.exists()) gftwFile = null;
            if (!statFile.exists()) statFile = null;
            return;
        }
        prepare(context, obj);
    }

    public void prepare(Context context, int obj) throws IOException {
        switch (obj) {
            case TDLIST:
                jsonFile = getJson("https://gamepress.gg/sites/default/files/aggregatedjson/t-dolls-list.json", "doc.json", context);
                break;
            case TNLIST:
                File gftw = new File(context.getCacheDir(), "gftw.html");
                if (gftw.exists()) gftw.delete();
                new BufferedWriter(new FileWriter(gftw)).write(Jsoup.connect("https://gf.fws.tw/db/guns/alist").get().toString());
                gftwFile = gftw;
                break;
            case STATLIST:
                statFile = getJson("https://gamepress.gg/sites/default/files/aggregatedjson/GFLStatRankings.json", "stats.json", context);
                break;
            case AUX: // debuggable document
                new BufferedWriter(new FileWriter(new File(context.getCacheDir(), "416.html"))).write(Jsoup.connect("https://girlsfrontline.gamepress.gg/t-doll/hk416").get().toString());
                break;
        }
    }

    private File getJson(String sUrl, String fileName, Context context) throws IOException {
        URL url = new URL(sUrl);
        URLConnection ucon = url.openConnection();
        ucon.setReadTimeout(5000);
        ucon.setConnectTimeout(10000);

        InputStream is = ucon.getInputStream();
        BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

        File file = new File(context.getCacheDir(), fileName);
        if (file.exists()) file.delete();
        file.createNewFile();

        FileOutputStream outStream = new FileOutputStream(file);
        byte[] buff = new byte[5 * 1024];
        int len;
        while ((len = inStream.read(buff)) != -1) outStream.write(buff, 0, len);
        outStream.flush();
        outStream.close();
        inStream.close();
        return file;
    }

    public TDolls loadTDL() {
        TDolls list = new TDolls();
        ArrayList<HashMap<String, String>> raw;
        if (jsonFile==null) return list;
        try {
            Type type = new TypeToken<ArrayList<HashMap<String, String>>>() {}.getType();
            raw = new Gson().fromJson(new JsonReader(new FileReader(jsonFile)), type);
        } catch (Exception e) { e.printStackTrace(); return list; }

        for (HashMap<String, String> doll : raw) list.add(new TDoll(Integer.valueOf(doll.get("id"))));

        return list;
    }

    public ArrayList<ParserException> parseDoll(TDoll doll, int level) {
        ArrayList<ParserException> exceptions = new ArrayList<>();
        if (list == null || gftw == null || stats == null) {
            if (jsonFile == null || gftwFile == null || statFile == null) return null;
            try {
                Type type = new TypeToken<ArrayList<HashMap<String, String>>>() {}.getType();
                list = new Gson().fromJson(new JsonReader(new FileReader(jsonFile)), type);
                stats = new Gson().fromJson(new JsonReader(new FileReader(statFile)), type);
                gftw = Jsoup.parse(gftwFile, "UTF-8").body();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        for (HashMap<String, String> entry : list) {
            if (!entry.get("id").equals(doll.id+"")) continue;
            Element gftwentry = null;
            for (Element e : gftw.getElementsByClass("gun_card_list")) {
                if (e.getElementsByTag("span")
                        .first().text().replaceAll("No\\.", "")
                        .equals(doll.id+"")) {
                    gftwentry = e;
                    break;
                }
            }
            try {
                if (doll.parsingLevel <= 0) {
                    if (gftwentry != null) doll.thumb = new URL("http://gf.fws.tw"
                            + gftwentry.getElementsByAttributeValueContaining("style", "background-image: url(")
                            .first().attr("style").replaceAll("background-image: url\\(", "").replaceAll("\\);", ""));
                    else doll.thumb = new URL("https://gamepress.gg" + entry.get("icon").replaceAll("\\\\", ""));
                    doll.name = entry.get("title");
                    doll.lvl2 = new URL("https://girlsfrontline.gamepress.gg" + entry.get("path").replaceAll("\\\\", ""));
                    doll.clss = entry.get("tdoll_class");
                    doll.rarity = Integer.valueOf(entry.get("rarity").replace("rarity-", ""));
                    doll.craft = entry.get("field_t_doll_craft_time");
                    doll.acc = Integer.parseInt(entry.get("acc"));
                    doll.dmg = Integer.parseInt(entry.get("dmg"));
                    doll.eva = Integer.parseInt(entry.get("eva"));
                    doll.hp = Integer.parseInt(entry.get("hp"));
                    doll.rof = Integer.parseInt(entry.get("rof"));
                    doll.parsingLevel = 1;
                }
                if (level > 1 && doll.parsingLevel <= 1) {
                    Element root = Jsoup.connect(doll.lvl2.toString()).get().body();

                    try {
                        doll.cgMain = new URL("http://gamepress.gg"
                                + root.getElementsByAttributeValue("id", "tab-1-img").first()
                                .getElementsByTag("img").first().attr("src"));
                        doll.cgDamage = new URL("http://gamepress.gg"
                                + root.getElementsByAttributeValue("id", "tab-2-img").first()
                                .getElementsByTag("img").first().attr("src"));
                    } catch (Exception pe) { exceptions.add(new ParserException(doll, "Main CG", pe)); }

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
                        }
                    } catch (Exception pe) { exceptions.add(new ParserException(doll, "Roles", pe)); doll.role = ""; }

                    try {
                        Element p = root.getElementsContainingOwnText("History").first().nextElementSibling();
                        if (p.tagName().equals("p")) doll.description = p.text();
                    }  catch (Exception pe) { exceptions.add(new ParserException(doll, "Description", pe)); doll.role = ""; }

                    try {
                        Elements adjTrs = root.getElementsByAttributeValue("id", "adj-tile").first().getElementsByTag("tr");
                        Elements selfTrs = root.getElementsByAttributeValue("id", "self-pos-tile").first().getElementsByTag("tr");
                        doll.pattern = new int[3][3];
                        for (int i = 0; i < doll.pattern.length; i++) {
                            Elements adjTds = adjTrs.get(i).getElementsByTag("td");
                            Elements selfTds = selfTrs.get(i).getElementsByTag("td");
                            for (int j = 0; j < doll.pattern[i].length; j++) {
                                if (adjTds.get(j).hasAttr("class")) doll.pattern[i][j] = 1;
                                else if (selfTds.get(j).hasAttr("class")) doll.pattern[i][j] = 2;
                                else doll.pattern[i][j] = 0;
                            }
                        }
                    } catch (Exception pe) { exceptions.add(new ParserException(doll, "pattern", pe)); }

                    try {
                        doll.affect = root.getElementsByClass("adj-effects").first().text().replace("Affects ", "");
                    } catch (Exception pe) { exceptions.add(new ParserException(doll, "Affect", pe)); doll.affect = ""; }
                    try {
                        doll.buffs = root.getElementsByClass("adj-bonus").first().getElementsByTag("table").first().toString();
                    } catch (Exception pe) { exceptions.add(new ParserException(doll, "Buffs", pe)); doll.buffs = ""; }
                    try {
                        doll.skills = root.getElementsByAttributeValue("id", "t-doll-skill").first().nextElementSibling().toString();
                    } catch (Exception pe) { exceptions.add(new ParserException(doll, "Skills", pe)); doll.skills = ""; }

                    try {
                        doll.hpBar = (int) ((float) doll.hp / (float) getHighest("hp", stats) * 100);
                        doll.dmgBar = (int) ((float) doll.dmg / (float) getHighest("dmg", stats) * 100);
                        doll.accBar = (int) ((float) doll.acc / (float) getHighest("acc", stats) * 100);
                        doll.evaBar = (int) ((float) doll.eva / (float) getHighest("eva", stats) * 100);
                        doll.rofBar = (int) ((float) doll.rof / (float) getHighest("rof", stats) * 100);
                    } catch (Exception pe) { exceptions.add(new ParserException(doll, "Bars", pe));
                        doll.hpBar = 0;
                        doll.dmgBar = 0;
                        doll.accBar = 0;
                        doll.evaBar = 0;
                        doll.rofBar = 0;
                    }

//                    doll.hpBar = (int) Float.parseFloat(root.getElementsByAttributeValue("id", "hp-bar").first().attr("style")
//                            .replaceAll("[^0-9.]", ""));
//                    doll.dmgBar = (int) Float.parseFloat(root.getElementsByAttributeValue("id", "attack-bar").first().attr("style")
//                            .replaceAll("[^0-9.]", ""));
//                    doll.accBar = (int) Float.parseFloat(root.getElementsByAttributeValue("id", "speed-bar").first().attr("style")
//                            .replaceAll("[^0-9.]", ""));
//                    doll.evaBar = (int) Float.parseFloat(root.getElementsByAttributeValue("id", "defense-bar").first().attr("style")
//                            .replaceAll("[^0-9.]", ""));
//                    doll.rofBar = (int) Float.parseFloat(root.getElementsByAttributeValue("id", "res-bar").first().attr("style")
//                            .replaceAll("[^0-9.]", ""));
                }
            } catch (Exception e) { Log.e("PARSER_ERROR", "T-Doll ID"+doll.id); e.printStackTrace(); return null; }
            break;
        }
        return exceptions;
    }

    private static int getHighest(String key, ArrayList<HashMap<String, String>> list) {
        int highest = -1;
        for (HashMap<String, String> entry : list)
            if (entry.containsKey(key))
                if (Integer.valueOf(entry.get(key)) > highest) highest = Integer.valueOf(entry.get(key));
        return highest;
    }

    public class ParserException extends IOException {
        private ParserException(TDoll doll, String elementName, Exception e) {
            super(String.format("Cannot parse %s's %s: %s", doll.getName(), elementName, e.getMessage()));
        }
    }
}
