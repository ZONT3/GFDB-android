package ru.zont.gfdb.core;

import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SmartParser {
    public enum FILES {
        LIST_GP, LIST_FWS, STATS_GP,
        STATS_FWS, LIST_WIKI, CRAFTLIST_WIKI
    }

    private static File gpListFile;
    private static File gpStatFile;
    private static File fwsListFile;
    private static File fwsStatFile;
    private static File wikiListFile;
    private static File wikiCraftlistFile;
    private static File[] filesList = {
            gpListFile, gpStatFile, fwsListFile,
            fwsStatFile, wikiListFile, wikiCraftlistFile
    };
    private static String[] fileNames = {
            "list_gamepress.json",
            "stats_gamepress.json",
            "list_gffws.html",
            "stats_gffws.html",
            "list_wiki.html",
            "caftlist_wiki.html"
    };

    private static boolean isUpdated() {
        return gpListFile != null
                && gpStatFile != null
                && fwsListFile != null
                && fwsStatFile != null
                && wikiListFile != null
                && wikiCraftlistFile != null;
    }

    @WeakExecution
    public static void prepare(File cacheDir, @Nullable PrepareOnProgress onProgress) {
        try {
            for (int i = 0; i < FILES.values().length; i++) {
                checkInterrupted();
                if (onProgress != null)
                    onProgress.onProgressUpdate(i, filesList.length, i, null);

                filesList[i] = new File(cacheDir, fileNames[i]);
                download(i, filesList[i]);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            gpListFile = null;
            gpStatFile = null;
            fwsListFile = null;
            fwsStatFile = null;
            wikiListFile = null;
            wikiCraftlistFile = null;
        } catch (Exception e) {
            if (onProgress != null)
                onProgress.onProgressUpdate(-1, -1, -1, e);
        }
    }

    private static void download(int file, File f) {
        URL url;
        try {
            switch (FILES.values()[file]) {
                case LIST_GP:
                    url = new URL("https://gamepress.gg/sites/default/files/aggregatedjson/t-dolls-list.json");
                    break;
                case LIST_FWS:
                    url = new URL("https://gf.fws.tw/db/guns/alist");
                    break;
                case STATS_GP:
                    url = new URL("https://gamepress.gg/sites/default/files/aggregatedjson/GFLStatRankings.json");
                    break;
                case STATS_FWS:
                    url = new URL("https://gf.fws.tw/db/guns/table_list");
                    break;
                case LIST_WIKI:
                    url = new URL("https://en.gfwiki.com/wiki/T-Doll_Index");
                    break;
                case CRAFTLIST_WIKI:
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
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                checkInterrupted();
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException | InterruptedException e) { e.printStackTrace(); }
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException("Prepare thread has been interrupted");
    }

    public interface PrepareOnProgress {
        void onProgressUpdate(int loaded, int total, int loading, Exception e);
    }
}
