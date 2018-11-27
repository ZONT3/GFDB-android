package ru.zont.gfdb;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import ru.zont.gfdb.core.NetParser;
import ru.zont.gfdb.core.TDoll;
import ru.zont.gfdb.core.TDolls;

public class LoadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        Log.d("DisplayMetrics",
                String.format("DP\t%f\npxW\t%d\npxH\t%d\ndpW\t%d\ndpH\t%d\ninSz\t%f",
                        dm.density, dm.widthPixels, dm.heightPixels,
                        Dimension.toDp(dm.widthPixels, this), Dimension.toDp(dm.heightPixels, this),
                        Math.sqrt((dm.widthPixels*dm.widthPixels) + (dm.heightPixels*dm.heightPixels))/dm.densityDpi));

        Date date = Calendar.getInstance().getTime();
        File dateFile = new File(getCacheDir(), "lastupd");
        boolean upd = false;
        if (!dateFile.exists()) {
            new LoadList(this).execute();
            upd = true;
        } else {
            try {
                FileInputStream fis = new FileInputStream(dateFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                Date prevDate = (Date) ois.readObject();
                ois.close();
                if (date.getTime() - prevDate.getTime() > 24L * 60L * 60L * 1000L) {
                    new LoadList(this).execute();
                    upd = true;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (upd) {
            try {
                FileOutputStream fos = new FileOutputStream(dateFile);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(date);
                oos.flush();
                oos.close();
            } catch (IOException e) { e.printStackTrace(); }
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
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
            if (activity == null || activity.isFinishing()) return;

            if (finished) activity.startActivity(new Intent(activity, MainActivity.class));
            activity.finish();
        }

        @SuppressLint("DefaultLocale")
        @Override
        protected void onProgressUpdate(Integer... values) {
            LoadActivity activity = contextReference.get();
            if (activity == null || activity.isFinishing()) return;
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
                    ex.setText(new int[] {R.string.load_tdl, R.string.load_tnl, R.string.load_stat}[arg3]);
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

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Override
        protected Void doInBackground(Void... voids) {
            NetParser parser;
            try {
                publishProgress(CODE_DL, 0, 3, NetParser.TDLIST);
                parser = new NetParser(contextReference.get(), NetParser.TDLIST);
                publishProgress(CODE_DL, 1, 3, NetParser.TNLIST);
                parser.prepare(contextReference.get(), NetParser.TNLIST);;
                publishProgress(CODE_DL, 2, 3, NetParser.STATLIST);
                parser.prepare(contextReference.get(), NetParser.STATLIST);
//                publishProgress(CODE_DL, 3, 4, NetParser.AUX);
//                parser.prepare(contextReference.get(), NetParser.AUX);
            }
            catch (IOException e) { e.printStackTrace(); return null; }

            TDolls list = parser.loadTDL();
            for (int i = 0; i < list.size(); i++) {
                TDoll doll = list.get(i);
                publishProgress(CODE_PARSING, i, list.size(), 0);
                if (!parser.parseDoll(doll, 1)) publishProgress(CODE_ERROR,0,0,0);
                //try { Thread.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
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

    static TDolls getCachedList(AppCompatActivity context) {
        File file = new File(context.getCacheDir(), "cachedList");
        if (!file.exists()) {
            context.startActivity(new Intent(context, LoadActivity.class));
            context.finish();
        }

        TDolls res = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            res = (TDolls) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
        return res;
    }
}
