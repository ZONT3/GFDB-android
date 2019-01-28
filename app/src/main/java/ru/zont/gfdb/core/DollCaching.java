package ru.zont.gfdb.core;

public class DollCaching {
    private static final int MAX_SIZE = 10;
    private static TDolls cache = new TDolls();

    public static TDoll getDoll(int id) {
        return cache.getById(id);
    }

    public static void cacheDoll(TDoll doll) {
        TDoll exist = cache.getById(doll.id);
        if (exist != null) cache.remove(exist);

        cache.add(doll);
        if (cache.size() > MAX_SIZE)
            cache.remove(0);
    }
}
