package ru.zont.gfdb;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static String gameServer;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameServer = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("server", "EN");

        Toolbar toolbar = findViewById(R.id.main_tb);
        setSupportActionBar(toolbar);

        TextView ver = findViewById(R.id.main_ver);
        ver.setText("v." + BuildConfig.VERSION_NAME);

        if (getIntent().getBooleanExtra("upd", true)) {
            new Thread(() -> {
                try {
                    File dateFile = new File(getCacheDir(), "lastupd");
                    dateFile.delete();
                    ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(dateFile));
                    Date date = Calendar.getInstance().getTime();
                    date.setTime(System.currentTimeMillis());
                    os.writeObject(date);
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_reload:
                reloadDB();
                return true;
            case R.id.main_menu_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void reloadDB() {
        new File(getCacheDir(), "lastupd").delete();
        startActivity(new Intent(MainActivity.this, LoadActivity.class));
        finish();
    }

    public void mGoto(View v) {
        switch (v.getId()) {
            case R.id.main_bt_lib:
                startActivity(new Intent(MainActivity.this, LibraryActivity.class));
                break;
            case R.id.main_bt_craft:
                startActivity(new Intent(MainActivity.this, CraftActivity.class));
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameServer != null &&
                !Objects.equals(PreferenceManager.getDefaultSharedPreferences(this)
                        .getString("server", "EN"), gameServer)) {
            Toast.makeText(this, R.string.main_prefsChangedReloading, Toast.LENGTH_LONG).show();
            reloadDB();
        }
    }
}
