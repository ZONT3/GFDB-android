package ru.zont.gfdb;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import ru.zont.gfdb.core.Dimension;
import ru.zont.gfdb.core.TDoll;
import ru.zont.gfdb.core.TDolls;

public class LibraryActivity extends AppCompatActivity {
    private static TDollLibAdapter adapter;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        toolbar = findViewById(R.id.lib_tb);
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
            adapter = new TDollLibAdapter(recyclerView, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int itemPosition = recyclerView.getChildLayoutPosition(v);
                    TDoll tDoll = ((TDollLibAdapter) recyclerView.getAdapter()).getDataset().get(itemPosition);
                    Intent intent = new Intent(wr.get(), CardActivity.class);
                    intent.putExtra("id", tDoll.getId());
                    wr.get().startActivity(intent);
                }
            }, tdolls);
            recyclerView.setAdapter(adapter);
            wr.get().toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recyclerView.scrollToPosition(0);
                }
            });

            wr.get().invalidateOptionsMenu();
            wr.get().findViewById(R.id.lib_pb).setVisibility(View.GONE);
        }

        @Override
        protected TDolls doInBackground(Void... voids) {
            return LoadActivity.getCachedList(wr.get());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.library, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.lib_menu_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            private boolean firstEditing = true;

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter == null) return false;
                adapter.applySearchQuery(newText);
                if (newText.isEmpty() && !firstEditing) invalidateOptionsMenu();
                firstEditing = false;
                return false;
            }
        });

        MenuItem sort = menu.findItem(R.id.lib_menu_sort);
        if (adapter != null && adapter.getDataset() != null && adapter.getDataset().size() > 0) {
            sort.setEnabled(true);
            SubMenu sortMenu = sort.getSubMenu();
            String[] arr = getResources().getStringArray(R.array.lib_sort_arr);
            for (int i = 0; i < arr.length; i++) {
                sortMenu.add(Menu.NONE, i, Menu.NONE, arr[i]);
                if (adapter != null && adapter.getSort() == i)
                    sortMenu.findItem(i).setEnabled(false);
            }
        } else sort.setEnabled(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.lib_menu_search:
                TransitionManager.beginDelayedTransition((ViewGroup)findViewById(R.id.lib_tb));
                item.expandActionView();
                return true;
            case R.id.lib_menu_sort_reverse:
                if (adapter == null) return true;
                item.setChecked(!item.isChecked());
                adapter.setReverse(item.isChecked());
                return true;
            default:
                if (item.getItemId() >=0 && item.getItemId() < getResources().getStringArray(R.array.lib_sort_arr).length
                        && adapter != null) {
                    closeOptionsMenu();
                    adapter.changeSort(item.getItemId());
                    invalidateOptionsMenu();
                    return true;
                } else return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onNavigateUp();
    }
}
