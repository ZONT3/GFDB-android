package ru.zont.gfdb;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.lang.ref.WeakReference;

import ru.zont.gfdb.core.Dimension;
import ru.zont.gfdb.core.TDoll;
import ru.zont.gfdb.core.TDolls;

public class LibraryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        Toolbar toolbar = findViewById(R.id.lib_tb);
        setSupportActionBar(toolbar);
        assert getSupportActionBar()!=null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        new AdapterLinker(this).execute();
    }

    private static class AdapterLinker extends AsyncTask<Void, Void, TDolls> {
        private WeakReference<LibraryActivity> wr;

        AdapterLinker(LibraryActivity a) { wr = new WeakReference<>(a); }

        @Override
        protected void onPostExecute(TDolls tdolls) {
            final RecyclerView recyclerView = wr.get().findViewById(R.id.lib_list);
            int spans = Dimension.toDp(wr.get().getResources().getDisplayMetrics().widthPixels, wr.get()) / 88;
            recyclerView.setLayoutManager(new GridLayoutManager(wr.get(), spans));
            recyclerView.setAdapter(new TDollLibAdapter(wr.get(), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int itemPosition = recyclerView.getChildLayoutPosition(v);
                    TDoll tDoll = ((TDollLibAdapter)recyclerView.getAdapter()).getDataset().get(itemPosition);
                    Intent intent = new Intent(wr.get(), CardActivity.class);
                    intent.putExtra("id", tDoll.getId());
                    wr.get().startActivity(intent);
                }
            }, tdolls));

            wr.get().findViewById(R.id.lib_pb).setVisibility(View.GONE);
        }

        @Override
        protected TDolls doInBackground(Void... voids) {
            return LoadActivity.getCachedList(wr.get());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onNavigateUp();
    }
}
