package ru.zont.gfdb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_tb);
        setSupportActionBar(toolbar);
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
                //noinspection ResultOfMethodCallIgnored
                new File(getCacheDir(), "lastupd").delete();
                startActivity(new Intent(MainActivity.this, LoadActivity.class));
                finish();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    public void gotoLib(View v) {
        startActivity(new Intent(MainActivity.this, LibraryActivity.class));
    }
}
