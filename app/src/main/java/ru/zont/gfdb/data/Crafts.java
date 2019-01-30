package ru.zont.gfdb.data;

import android.content.Context;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.util.HashMap;

import ru.zont.gfdb.R;

@SuppressWarnings("unused")
public abstract class Crafts {
    public HashMap<String, String> general;
    public HashMap<String, String> heavy;
    public HashMap<Integer, ExcEntry> exceptions;

    public static class ExcEntry {
        public int craftType;
        public String value;
    }

    public static Crafts load(Context context) {
        return new Gson().fromJson(
                new InputStreamReader(
                        context.getResources()
                                .openRawResource(R.raw.crafts)),
                Crafts.class);
    }
}
