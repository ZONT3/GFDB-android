package ru.zont.gfdb.core;

import java.io.Serializable;
import java.util.ArrayList;

public class TDolls extends ArrayList<TDoll> implements Serializable {
    public TDoll getById(int id) {
        for (int i = 0; i < size(); i++) if (get(i).id == id) return get(i);
        return null;
    }
}
