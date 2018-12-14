package ru.zont.gfdb;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;

import ru.zont.gfdb.core.Dimension;
import ru.zont.gfdb.core.Parser;
import ru.zont.gfdb.core.TDoll;
import ru.zont.gfdb.core.TDolls;

public class LoadActivity extends AppCompatActivity {

    private String gameServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        Log.d("DisplayMetrics",
                String.format("DP\t%f\npxW\t%d\npxH\t%d\ndpW\t%d\ndpH\t%d\ninSz\t%f",
                        dm.density, dm.widthPixels, dm.heightPixels,
                        Dimension.toDp(dm.widthPixels, this), Dimension.toDp(dm.heightPixels, this),
                        Math.sqrt((dm.widthPixels*dm.widthPixels) + (dm.heightPixels*dm.heightPixels))/dm.densityDpi));

        SharedPreferences shPrefs = getSharedPreferences("ru.zont.gfdb.prefs", MODE_PRIVATE);
        gameServer = shPrefs.getString("server", "EN");

        Date date = Calendar.getInstance().getTime();
        date.setTime(System.currentTimeMillis());
        File dateFile = new File(getCacheDir(), "lastupd");
        boolean upd = false;
        if (!dateFile.exists()) upd = true;
        else {
            try {
                FileInputStream fis = new FileInputStream(dateFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                Date prevDate = (Date) ois.readObject();
                ois.close();
                if (date.getTime() - prevDate.getTime() > 24L * 60L * 60L * 1000L) upd = true;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences sysPrefs = getSharedPreferences("ru.zont.gfdb.sys", MODE_PRIVATE);
        if (sysPrefs.getInt("lastStartVer", 0) != BuildConfig.VERSION_CODE)
            upd = true;
        sysPrefs.edit().putInt("lastStartVer", BuildConfig.VERSION_CODE).apply();

        if (!upd) {
            startActivity(new Intent(this, MainActivity.class)
                    .putExtra("upd", false));
            finish();
        } else {
            dateFile.delete();
            new LoadList(this).execute();
        }
    }

    private static class LoadList extends AsyncTask<Void, Integer, Void> {
        private static final int CODE_ERROR = -1;
        private static final int CODE_DL = 0;
        private static final int CODE_PARSING = 1;
        private static final int CODE_WRITING = 2;

        WeakReference<LoadActivity> contextReference;
        boolean finished = false;

        LoadList(LoadActivity context) {
            contextReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            LoadActivity activity = contextReference.get();
            ProgressBar progressBar = activity.findViewById(R.id.load_pb);
            TextView textView = activity.findViewById(R.id.load_text);

            progressBar.setIndeterminate(true);
            textView.setText(R.string.load_preparing);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            LoadActivity activity = contextReference.get();
            if (activity == null || activity.isFinishing()) {
                cancel(true);
                return;
            }

            if (finished) activity.startActivity(new Intent(activity, MainActivity.class));
            activity.finish();
        }

        @SuppressLint("DefaultLocale")
        @Override
        protected void onProgressUpdate(Integer... values) {
            LoadActivity activity = contextReference.get();
            if (activity == null || activity.isFinishing()) {
                cancel(true);
                return;
            }
            ProgressBar progressBar = activity.findViewById(R.id.load_pb);
            TextView textView = activity.findViewById(R.id.load_text);
            TextView ex = activity.findViewById(R.id.load_ex);

            Integer code = values[0];
            Integer arg1 = values[1];
            Integer arg2 = values[2];
            Integer arg3 = values[3];

            switch (code) {
                case CODE_DL:
                    if (progressBar.isIndeterminate()) progressBar.setIndeterminate(false);
                    if (progressBar.getMax() != arg2) progressBar.setMax(arg2);
                    progressBar.setProgress(arg1);
                    textView.setText(R.string.load_dl);
                    ex.setText(new int[] {R.string.load_tdl, R.string.load_tnl, R.string.load_stat, R.string.load_aux}[arg3]);
                    break;
                case CODE_PARSING:
                    if (progressBar.isIndeterminate()) progressBar.setIndeterminate(false);
                    if (progressBar.getMax() != arg2) progressBar.setMax(arg2);
                    progressBar.setProgress(arg1);
                    textView.setText(R.string.load_parsing);
                    ex.setText(String.format("%d/%d", arg1, arg2));
                    break;
                case CODE_WRITING:
                    textView.setText(activity.getText(R.string.load_writing));
                    ex.setVisibility(View.GONE);
                    if (!progressBar.isIndeterminate()) progressBar.setIndeterminate(true);
                    break;
                case CODE_ERROR: Toast.makeText(contextReference.get(), R.string.load_error, Toast.LENGTH_LONG).show(); break;
            }
        }

        @Override
        protected void onCancelled() {
            Toast.makeText(contextReference.get(), R.string.load_fatal_err, Toast.LENGTH_LONG).show();
            contextReference.get().finish();
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        protected Void doInBackground(Void... voids) {
            Parser parser = new Parser(contextReference.get().getCacheDir(), "EN", true);
            for (int i = 0; i < Parser.FILES_COUNT; i++) {
                publishProgress(CODE_DL, i, Parser.FILES_COUNT, i);
                parser.prepare(i);
            }

            TDolls list;
            try {
                list = parser.getList();
                for (int i = 0; i < list.size(); i++) {
                    TDoll doll = list.get(i);
                    publishProgress(CODE_PARSING, i, list.size(), 0);
                    try {
                        parser.baseParse(doll);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        publishProgress(CODE_ERROR, 0, 0, 0);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                cancel(true);
                return null;
            }

            publishProgress(CODE_WRITING, 0, 0, 0);
            try {
                File file = new File(contextReference.get().getCacheDir(), "cachedList");
                if (file.exists()) file.delete();
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(list);
                oos.flush();
                oos.close();
            } catch (IOException e) { e.printStackTrace(); return null; }

            finished = true;
            return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static TDolls getCachedList(AppCompatActivity activity) {
        File file = new File(activity.getCacheDir(), "cachedList");
        if (!file.exists()) {
            activity.startActivity(new Intent(activity, LoadActivity.class));
            activity.finish();
        }

        TDolls res = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            res = (TDolls) ois.readObject();
            ois.close();
        } catch (IOException e) {
            e.printStackTrace();
            new File(activity.getCacheDir(), "lastupd").delete();
            activity.startActivity(new Intent(activity, LoadActivity.class));
            activity.finish();
        } catch (ClassNotFoundException e) { e.printStackTrace(); }
        return res;
    }
}
