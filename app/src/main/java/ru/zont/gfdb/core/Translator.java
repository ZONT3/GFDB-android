package ru.zont.gfdb.core;

import android.annotation.SuppressLint;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

public class Translator {
    private static final String API_KEY = "trnsl.1.1.20181218T063541Z.d45e60c6ac027dd5.6dc7c2bf18d5c211a3cd4b1fafa60e5bc71426e7";

    @SuppressLint("UseSparseArrays")
    public void translateDoll(TDoll doll, String targetLang, String sourceLang, File savedTLfile) throws YaException {
        HashMap<Integer, Translations> tl = null;
        if (savedTLfile.exists()) {
            try {
                tl = new Gson().fromJson(new FileReader(savedTLfile), new TypeToken<HashMap<Integer, Translations>>(){}.getType());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        boolean upd = false;
        Translations thisTL = null;
        if (tl != null && (thisTL = tl.get(doll.id)) != null) {
            if (thisTL.translations.get(targetLang) == null) upd = true;
            else if (!doll.role.equals(thisTL.originals[0])) upd = true;
            else if (!doll.buffs.equals(thisTL.originals[1])) upd = true;
            else if (!doll.skills.equals(thisTL.originals[2])) upd = true;
            else if (!doll.description.equals(thisTL.originals[3])) upd = true;
        } else upd = true;
        
        if (upd) {
            String[] rsp = request(doll, sourceLang, targetLang);
            if (rsp == null) {
                new NullPointerException().printStackTrace();
                return;
            }

            Translations newTl = new Translations();
            newTl.originals = new String[] {
                    doll.role, doll.buffs,
                    doll.skills, doll.description
            };
            newTl.translations.put(targetLang, rsp);

            doll.role = rsp[0];
            doll.buffs = rsp[1];
            doll.skills = rsp[2];
            doll.description = rsp[3];

            if (tl == null) tl = new HashMap<>();
            tl.put(doll.id, newTl);

            try {
                new Gson().toJson(tl, new TypeToken<HashMap<Integer, Translations>>(){}.getType(), new FileWriter(savedTLfile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //noinspection ConstantConditions
            doll.role = thisTL.translations.get(targetLang)[0];
            //noinspection ConstantConditions
            doll.buffs = thisTL.translations.get(targetLang)[1];
            //noinspection ConstantConditions
            doll.skills = thisTL.translations.get(targetLang)[2];
            //noinspection ConstantConditions
            doll.description = thisTL.translations.get(targetLang)[3];
        }
    }

    private String[] request(TDoll doll, String sourceLang, String targetLang) throws YaException {
        StringBuilder request = new StringBuilder("https://translate.yandex.net/api/v1.5/tr.json/translate");
        request.append("?key=").append(API_KEY)
                .append("&lang=").append(sourceLang).append("-").append(targetLang)
                .append("&format=html");

        request.append("&text=").append(doll.getRole());
        request.append("&text=").append(doll.getBuffs());
        request.append("&text=").append(doll.getSkills());
        request.append("&text=").append(doll.getDescription());

        StringBuilder response = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(new URL(request.toString()).openStream()));
            String input;
            while ((input = in.readLine()) != null)
                response.append(input).append("\n");
            in.close();
        } catch (IOException e) { e.printStackTrace(); }
        if (response.toString().isEmpty()) return null;

        YaResponse yresp = new Gson().fromJson(response.toString(), YaResponse.class);
        if (yresp.code != 200) throw new YaException(yresp.code);
        return yresp.text;
    }

    @SuppressWarnings("unused")
    private static class YaResponse {
        int code;
        String lang;
        String[] text;
    }
    
    private static class Translations {
        String[] originals;
        HashMap<String, String[]> translations;
    }

    private static class YaException extends Exception {
        private YaException(int code) { super("Yandex responded error " + code); }
    }
}
