package ru.zont.gfdb;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;

import ru.zont.gfdb.core.Dimension;
import ru.zont.gfdb.core.NetParser;
import ru.zont.gfdb.core.TDoll;

public class CardActivity extends AppCompatActivity {
    private static final String[] RARITY_TABLE = {"", "", "★★", "★★★", "★★★★", "★★★★★"};
    private static final int[] RARITY_TABLE_COLOR = {-1, -1, R.color.rarity_common, R.color.rarity_rare,
            R.color.rarity_epic, R.color.rarity_legend};
    private static final int[][] PATTERN_TABLE = {
            {R.id.card_pattern_1, R.id.card_pattern_4, R.id.card_pattern_7},
            {R.id.card_pattern_2, R.id.card_pattern_5, R.id.card_pattern_8},
            {R.id.card_pattern_3, R.id.card_pattern_6, R.id.card_pattern_9}};

    private Lvl1Parser parser1;
    private Lvl2Parser parser2;
    private ProgressBar loadingPb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        Toolbar toolbar = findViewById(R.id.card_tb);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!getResources().getBoolean(R.bool.card_tabless)) {
            ViewPager viewPager = findViewById(R.id.card_viewpager);
            viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
                private Fragment[] fragments = new Fragment[]{new MainFragment(), new CGFragment()};

                @Override
                public Fragment getItem(int position) {
                    return fragments[position];
                }

                @Override
                public int getCount() {
                    return fragments.length;
                }

                @Nullable
                @Override
                public CharSequence getPageTitle(int position) {
                    return new String[]{getString(R.string.card_info), getString(R.string.card_cgs)}[position];
                }
            });
        }

        int id = getIntent().getIntExtra("id", -1);
        if (id < 0) {
            Toast.makeText(this, "Invalid ID", Toast.LENGTH_SHORT).show();
            finish();
        }

        loadingPb = findViewById(R.id.card_pb);

        parser1 = new Lvl1Parser(this);
        parser1.execute(id);
    }

    public static class MainFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_card_main, container, false);
        }
    }

    public static class CGFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_card_cgs, container, false);
        }
    }

    private void checkAsyncTasks() {
        boolean p1;
        boolean p2;

        if (parser1==null) p1 = true;
        else p1 = parser1.done;
        if (parser2==null) p2 = true;
        else p2 = parser2.done;

        if (p1 && p2) loadingPb.setVisibility(View.GONE);
    }

    /*
        ImageView cg = wr.get().findViewById(R.id.card_maincg);
        TextView roles = wr.get().findViewById(R.id.card_role);
        TextView build = wr.get().findViewById(R.id.card_build);
        TextView hp = wr.get().findViewById(R.id.card_hp);
        ProgressBar pbHP = wr.get().findViewById(R.id.card_pb_hp);
        TextView dmg = wr.get().findViewById(R.id.card_dmg);
        ProgressBar pbDMG = wr.get().findViewById(R.id.card_pb_dmg);
        TextView acc = wr.get().findViewById(R.id.card_acc);
        ProgressBar pbACC = wr.get().findViewById(R.id.card_pb_acc);
        TextView eva = wr.get().findViewById(R.id.card_eva);
        ProgressBar pbEVA = wr.get().findViewById(R.id.card_pb_eva);
        TextView rof = wr.get().findViewById(R.id.card_rof);
        ProgressBar pbROF = wr.get().findViewById(R.id.card_pb_rof);
        ProgressBar pb = wr.get().findViewById(R.id.card_pb);
    */

    private static class Lvl1Parser extends AsyncTask<Integer, Void, TDoll> {
        WeakReference<CardActivity> wr;
        boolean done = false;

        Lvl1Parser(CardActivity activity) {
            wr = new WeakReference<>(activity);
        }

        @Override
        protected TDoll doInBackground(Integer... args) {
            return LoadActivity.getCachedList(wr.get()).getById(args[0]);
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(TDoll tDoll) {
            CardActivity activity = wr.get();
            if (activity == null || activity.isFinishing()) return;
            if (tDoll==null) {
                Toast.makeText(wr.get(), "Invalid ID", Toast.LENGTH_SHORT).show();
                wr.get().finish();
                return;
            }

            TextView roles = wr.get().findViewById(R.id.card_role);
            TextView build = wr.get().findViewById(R.id.card_build);
            TextView hp = wr.get().findViewById(R.id.card_hp);
            ProgressBar pbHP = wr.get().findViewById(R.id.card_pb_hp);
            TextView dmg = wr.get().findViewById(R.id.card_dmg);
            ProgressBar pbDMG = wr.get().findViewById(R.id.card_pb_dmg);
            TextView acc = wr.get().findViewById(R.id.card_acc);
            ProgressBar pbACC = wr.get().findViewById(R.id.card_pb_acc);
            TextView eva = wr.get().findViewById(R.id.card_eva);
            ProgressBar pbEVA = wr.get().findViewById(R.id.card_pb_eva);
            TextView rof = wr.get().findViewById(R.id.card_rof);
            ProgressBar pbROF = wr.get().findViewById(R.id.card_pb_rof);
            ActionBar toolbar = wr.get().getSupportActionBar();
            assert toolbar != null;

            if (wr.get().getResources().getBoolean(R.bool.card_monotitle)) {
                toolbar.setTitle(Html.fromHtml(String.format("[%s] %s <font color=\"#%06X\">%s</font>",
                        tDoll.getClss(), tDoll.getName(),
                        (0xFFFFFF & ResourcesCompat.getColor(wr.get().getResources(), RARITY_TABLE_COLOR[tDoll.getRarity()], null)),
                        RARITY_TABLE[tDoll.getRarity()])));
                toolbar.setSubtitle("");
            } else {
                toolbar.setTitle(tDoll.getName());
                toolbar.setSubtitle(Html.fromHtml(String.format("%s <font color=\"#%06X\">%s</font>",
                        tDoll.getClss(),
                        (0xFFFFFF & ResourcesCompat.getColor(wr.get().getResources(), RARITY_TABLE_COLOR[tDoll.getRarity()], null)),
                        RARITY_TABLE[tDoll.getRarity()])));
            }

            roles.setText("");
            build.setText(tDoll.getCraft());
            hp.setText(tDoll.getHp()+"");
            dmg.setText(tDoll.getDmg()+"");
            acc.setText(tDoll.getAcc()+"");
            eva.setText(tDoll.getEva()+"");
            rof.setText(tDoll.getRof()+"");
            pbHP.setMax(1); pbHP.setProgress(0);
            pbDMG.setMax(1); pbDMG.setProgress(0);
            pbACC.setMax(1); pbACC.setProgress(0);
            pbEVA.setMax(1); pbEVA.setProgress(0);
            pbROF.setMax(1); pbROF.setProgress(0);

            done = true;
            wr.get().parser2 = new Lvl2Parser(wr);
            wr.get().parser2.execute(tDoll);
        }
    }

    private static class Lvl2Parser extends AsyncTask<TDoll, Void, TDoll> {
        WeakReference<CardActivity> wr;
        boolean done = false;
        ArrayList<NetParser.ParserException> exceptions;

        Lvl2Parser(WeakReference<CardActivity> wr) { this.wr = wr; }

        @Override
        protected TDoll doInBackground(TDoll... tDolls) {
            try {
                NetParser parser = new NetParser(wr.get(), -1);
                exceptions = parser.parseDoll(tDolls[0], 2);
                if (exceptions == null) return null;
            } catch (IOException e) { e.printStackTrace(); return null; }
            return tDolls[0];
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(TDoll tDoll) {
            CardActivity activity = wr.get();
            if (activity == null || activity.isFinishing()) return;
            if (tDoll == null) {
                Toast.makeText(wr.get(), "Error while parsing", Toast.LENGTH_SHORT).show();
                wr.get().finish();
                return;
            }

            ImageView cg = wr.get().findViewById(R.id.card_maincg);
            TextView roles = wr.get().findViewById(R.id.card_role);
            ProgressBar pbHP = wr.get().findViewById(R.id.card_pb_hp);
            ProgressBar pbDMG = wr.get().findViewById(R.id.card_pb_dmg);
            ProgressBar pbACC = wr.get().findViewById(R.id.card_pb_acc);
            ProgressBar pbEVA = wr.get().findViewById(R.id.card_pb_eva);
            ProgressBar pbROF = wr.get().findViewById(R.id.card_pb_rof);
            TextView affects = wr.get().findViewById(R.id.card_affects);
            TextView buffs = wr.get().findViewById(R.id.card_buffs);
            TextView skills = wr.get().findViewById(R.id.card_skills);
            TextView description = wr.get().findViewById(R.id.card_desc);
            final ProgressBar cgPb = wr.get().findViewById(R.id.card_cgpb);

            if (cg!=null && tDoll.getCgMain() != null && tDoll.getCgDamage() != null)
                Glide.with(wr.get())
                        .load(tDoll.getCgMain().toString())
                        .apply(new RequestOptions()
                                .override(Dimension.toPx(300, wr.get()))
                                .error(R.drawable.conprobl))
                        .addListener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                cgPb.setVisibility(View.GONE);
                                return false;
                            }
                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                cgPb.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(cg);

            roles.setText(tDoll.getRole());
            pbHP.setMax(100); pbHP.setProgress(tDoll.getHpBar());
            pbDMG.setMax(100); pbDMG.setProgress(tDoll.getDmgBar());
            pbACC.setMax(100); pbACC.setProgress(tDoll.getAccBar());
            pbEVA.setMax(100); pbEVA.setProgress(tDoll.getEvaBar());
            pbROF.setMax(100); pbROF.setProgress(tDoll.getRofBar());
            affects.setText("Affects "+tDoll.getAffect().toUpperCase());
            buffs.setText(Html.fromHtml(tDoll.getBuffs()));
            skills.setText(Html.fromHtml(tDoll.getSkills()));
            description.setText(tDoll.getDescription());

            RecyclerView rw = wr.get().findViewById(R.id.card_recycler);
            ArrayList<URL[]> urls = new ArrayList<>();
            urls.add(new URL[] { tDoll.getCgMain(), tDoll.getCgDamage() });
            urls.addAll(tDoll.getCostumes());
            rw.setLayoutManager(new LinearLayoutManager(wr.get(), LinearLayoutManager.VERTICAL, false));
            rw.setAdapter(new CgAdapter(urls));

            if (tDoll.getPattern() != null) {
                for (int x = 0; x < tDoll.getPattern().length; x++) {
                    for (int y = 0; y < tDoll.getPattern()[x].length; y++) {
                        ImageView iw = wr.get().findViewById(PATTERN_TABLE[x][y]);
                        switch (tDoll.getPattern()[x][y]) {
                            case 1: iw.setImageResource(android.R.color.holo_blue_bright); break;
                            case 2: iw.setImageResource(android.R.color.white); break;
                            default: iw.setImageResource(android.R.color.darker_gray); break;
                        }
                    }
                }
            }

            done = true;
            wr.get().checkAsyncTasks();
        }

        private static class CgAdapter extends RecyclerView.Adapter<CgAdapter.VH> {
            private ArrayList<URL[]> dataset;

            static class VH extends RecyclerView.ViewHolder {
                ImageView iw;
                ProgressBar pb;

                VH(View itemView) {
                    super(itemView);
                    iw = itemView.findViewById(R.id.card_cg_iw);
                    pb = itemView.findViewById(R.id.card_cg_pb);
                }
            }

            CgAdapter(ArrayList<URL[]> urls) {
                dataset = urls;
            }

            @NonNull
            @Override
            public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_card_cg, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull VH holder, int position) {
                String url = dataset.get(position/2)[position%2].toString();

                final ProgressBar fPB = holder.pb;
                fPB.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView)
                        .load(url)
                        .apply(new RequestOptions().error(R.drawable.conprobl))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                fPB.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                fPB.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(holder.iw);
            }

            @Override
            public int getItemCount() {
                return dataset.size()*2;
            }

        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.card, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.card_menu_link:

                return true;
            case R.id.card_menu_mod3:

                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }
}
