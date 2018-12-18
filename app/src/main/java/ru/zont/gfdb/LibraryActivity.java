package ru.zont.gfdb;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.transition.TransitionManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

import ru.zont.gfdb.core.Dimension;
import ru.zont.gfdb.core.TDoll;
import ru.zont.gfdb.core.TDolls;

public class LibraryActivity extends AppCompatActivity {
    private static TDollLibAdapter adapter;
    private Toolbar toolbar;
    private ProgressBar pb;
    private boolean inited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        toolbar = findViewById(R.id.lib_tb);
        setSupportActionBar(toolbar);
        assert getSupportActionBar()!=null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        pb = findViewById(R.id.lib_pb);

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
            adapter = new TDollLibAdapter(recyclerView, wr.get().pb,
                    wr.get().findViewById(R.id.lib_nores), v -> {
                        int itemPosition = recyclerView.getChildLayoutPosition(v);
                        TDollLibAdapter adapter = (TDollLibAdapter) recyclerView.getAdapter();
                        if (adapter == null) return;
                        TDoll tDoll = adapter.getDataset().get(itemPosition);
                        Intent intent = new Intent(wr.get(), CardActivity.class);
                        intent.putExtra("id", tDoll.getId());
                        wr.get().startActivity(intent);
                    }, tdolls);
            recyclerView.setAdapter(adapter);

            LibraryActivity libraryActivity = wr.get();
            libraryActivity.toolbar.setOnClickListener(v -> recyclerView.scrollToPosition(0));
            libraryActivity.findViewById(R.id.lib_pb).setVisibility(View.GONE);
            libraryActivity.inited = true;
            libraryActivity.invalidateOptionsMenu();
        }

        @Override
        protected TDolls doInBackground(Void... voids) {
            return LoadActivity.getCachedList(wr.get());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!inited) return super.onCreateOptionsMenu(menu);
        
        getMenuInflater().inflate(R.menu.library, menu);
        MenuItem searchItem = menu.findItem(R.id.lib_menu_search);
        final Menu finalMenu = menu;
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                finalMenu.setGroupVisible(R.id.lib_menu_group_hos, false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                finalMenu.setGroupVisible(R.id.lib_menu_group_hos, true);
                invalidateOptionsMenu();
                return true;
            }
        });
        ((SearchView) searchItem.getActionView()).setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
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

        menu.findItem(R.id.lib_menu_clear).setVisible(adapter == null || adapter.hasFilters());

        MenuItem sort = menu.findItem(R.id.lib_menu_sort);
        if (adapter != null) {
            menu.findItem(R.id.lib_menu_sort_reverse).setChecked(adapter.isReverse());
            if (adapter.getDataset() != null && adapter.getDataset().size() > 0) {
                sort.setEnabled(true);
                SubMenu sortMenu = sort.getSubMenu();
                String[] arr = getResources().getStringArray(R.array.lib_sort_arr);
                for (int i = 0; i < arr.length; i++) {
                    MenuItem item = sortMenu.add(R.id.lib_menu_group_sort, i, Menu.NONE, arr[i]);
                    item.setCheckable(true);
                    if (adapter != null && adapter.getSort() == i)
                        item.setChecked(true);
                }
                sortMenu.setGroupCheckable(R.id.lib_menu_group_sort, true, true);
            } else sort.setEnabled(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.lib_menu_search:
                TransitionManager.beginDelayedTransition(findViewById(R.id.lib_tb));
                item.expandActionView();
                return true;
            case R.id.lib_menu_sort_reverse:
                if (adapter == null) return true;
                item.setChecked(!item.isChecked());
                adapter.setReverse(item.isChecked());
                return true;
            case R.id.lib_menu_fbt:
                @SuppressLint("InflateParams") View picker = getLayoutInflater()
                        .inflate(R.layout.dialog_time, null, false);
                final NumberPicker hrs = picker.findViewById(R.id.time_hrs);
                final NumberPicker mins = picker.findViewById(R.id.time_mins);
                NumberPicker secs = picker.findViewById(R.id.time_secs);
                hrs.setMinValue(0); hrs.setMaxValue(8);
                hrs.setValue(adapter.getTimeFilter() > 0 ? adapter.getTimeFilter()/60 : 1);
                mins.setMinValue(0); mins.setMaxValue(59);
                mins.setValue(adapter.getTimeFilter() > 0 ? adapter.getTimeFilter()%60 : 10);
                secs.setMinValue(0); secs.setMaxValue(59); secs.setValue(0); secs.setEnabled(false);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.time_title)
                        .setView(picker)
                        .setPositiveButton(R.string.time_set, (dialog, which) -> {
                            if (adapter == null) return;
                            adapter.applyTimeFilter(hrs.getValue()*60 + mins.getValue());
                            if (adapter.getSort() != TDollLibAdapter.SORT_CONSTR)
                                adapter.changeSort(TDollLibAdapter.SORT_CONSTR);
                            invalidateOptionsMenu();
                        }).create().show();
                return true;
            case R.id.lib_menu_clear:
                adapter.resetList();
                invalidateOptionsMenu();
                return true;
            default:
                if (item.getGroupId() == R.id.lib_menu_group_sort && adapter != null) {
                    if (!item.isChecked()) {
                        item.setChecked(true);
                        adapter.changeSort(item.getItemId());
//                        invalidateOptionsMenu();
                    }
                    return true;
                } else return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onNavigateUp();
    }

    @Override
    protected void onDestroy() {
        //adapter.onDestroy();
        super.onDestroy();
    }
}
