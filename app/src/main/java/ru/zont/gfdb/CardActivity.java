package ru.zont.gfdb;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
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

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;

import ru.zont.gfdb.core.Dimension;
import ru.zont.gfdb.core.DollCaching;
import ru.zont.gfdb.core.Parser;
import ru.zont.gfdb.core.TDoll;
import ru.zont.gfdb.core.TDolls;

public class CardActivity extends AppCompatActivity {
    private static final String[] RARITY_TABLE = {"", "", "★★", "★★★", "★★★★", "★★★★★", "EXTRA"};
    private static final int[] RARITY_TABLE_COLOR = {-1, -1, R.color.rarity_common, R.color.rarity_rare,
            R.color.rarity_epic, R.color.rarity_legend, R.color.rarity_extra};
    private static final int[][] PATTERN_TABLE = {
            {R.id.card_pattern_1, R.id.card_pattern_2, R.id.card_pattern_3},
            {R.id.card_pattern_4, R.id.card_pattern_5, R.id.card_pattern_6},
            {R.id.card_pattern_7, R.id.card_pattern_8, R.id.card_pattern_9}};

    static final String TN_CONTENT = "transition:card:content";
//    private static final String LOG = "CardActivity";

    private Lvl1Parser parser1;
    private Lvl2Parser parser2;
    private ViewGroup content;
    private ViewGroup loadView;
    private ViewPager viewPager;
    private View toTransit;

    private TDoll doll;

    private boolean mfReady = false;
    private boolean cgfReady = false;
    private boolean isTabless;
    private ArrayList<WebView> webViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        Toolbar toolbar = findViewById(R.id.card_tb);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        assert supportActionBar != null;
        supportActionBar.setDisplayShowHomeEnabled(true);
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        setup();
        init();
    }

    private void init() {
        int id = getIntent().getIntExtra("id", -1);
        if (id < 0) {
            Toast.makeText(this, "Invalid ID", Toast.LENGTH_SHORT).show();
            finishAfterTransition();
        }

        new Thread(() -> {
            if (!isTabless) {
                try {
                    while (!(mfReady && cgfReady)) Thread.sleep(100);
                } catch (InterruptedException ignored) { }
            }

            runOnUiThread(() -> {
                parser1 = new Lvl1Parser(this);
                parser1.execute(id);
            });
        }).start();
    }

    private void setup() {
        toTransit = null;
        if (!(isTabless = getResources().getBoolean(R.bool.card_tabless))) {
            viewPager = findViewById(R.id.card_viewpager);
            viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
                private Fragment[] fragments = new Fragment[]{
                        new MainFragment().setWR(CardActivity.this),
                        new CGFragment().setWR(CardActivity.this)
                };

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
            toTransit = viewPager;
        } else onCreateMainFragment(null);
        if (toTransit == null) toTransit = findViewById(R.id.card_toTransit);
        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("anim", false))
            toTransit.setTransitionName(TN_CONTENT);
    }

    public static class MainFragment extends Fragment {
        private WeakReference<CardActivity> wr;
        private MainFragment setWR(CardActivity activity) {
            wr = new WeakReference<>(activity);
            return this;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_card_main, container, false);
            wr.get().onCreateMainFragment(view);
            wr.get().mfReady = true;
            return view;
        }
    }

    public static class CGFragment extends Fragment {
        private WeakReference<CardActivity> wr;
        private CGFragment setWR(CardActivity activity) {
            wr = new WeakReference<>(activity);
            return this;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            wr.get().cgfReady = true;
            return inflater.inflate(R.layout.fragment_card_cgs, container, false);
        }
    }

    private void onCreateMainFragment(@Nullable View parent) {
        if (parent == null) {
            content = findViewById(R.id.card_content);
            loadView = findViewById(R.id.card_load);
        } else {
            content = parent.findViewById(R.id.card_content);
            loadView = parent.findViewById(R.id.card_load);
        }

        content.setVisibility(View.INVISIBLE);
        loadView.setVisibility(View.VISIBLE);
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
        private WeakReference<CardActivity> wr;
        private Parser.ParserException error;

        Lvl1Parser(CardActivity activity) {
            wr = new WeakReference<>(activity);
        }

        @Override
        protected TDoll doInBackground(Integer... args) {
            Integer id = args[0];
            TDoll doll = PreferenceManager.getDefaultSharedPreferences(wr.get())
                    .getBoolean("doll_cache", true)
                            ? DollCaching.getDoll(id) : null;
            if (doll == null) {
                try {
                    TDolls list = Parser.getCachedList(wr.get());
                    doll = list.getById(id);
                } catch (Parser.ParserException e) {
                    e.printStackTrace();
                    error = e;
                    cancel(true);
                }
            }
            return doll;
        }

        @Override
        protected void onCancelled() {
            if (error!=null && error.getMessage().equals("List has not found in cache")) {
                Toast.makeText(wr.get(), R.string.cacheloadfail, Toast.LENGTH_LONG).show();
                wr.get().startActivity(new Intent(wr.get(), LoadActivity.class));
                wr.get().finish();
            } else if (error!=null) {
                Toast.makeText(wr.get(), R.string.dberr, Toast.LENGTH_LONG).show();
                wr.get().finish();
            }
        }

        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        protected void onPostExecute(TDoll tDoll) {
            CardActivity activity = wr.get();
            if (activity == null || activity.isFinishing()) return;
            if (tDoll==null) {
                Toast.makeText(wr.get(), "Invalid ID", Toast.LENGTH_SHORT).show();
                wr.get().finishAfterTransition();
                return;
            }

            wr.get().doll = tDoll;

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
                toolbar.setTitle(Html.fromHtml(String.format("<font color=\"#%06X\">No.%d</font> %s <font color=\"#%06X\">%s</font> <font color=\"#%06X\">%s</font>",
                        (0xFFFFFF & ResourcesCompat.getColor(wr.get().getResources(), android.R.color.darker_gray, null)),
                        tDoll.getId(), tDoll.getName(),
                        (0xFFFFFF & ResourcesCompat.getColor(wr.get().getResources(), android.R.color.darker_gray, null)),
                        tDoll.getType(),
                        (0xFFFFFF & ResourcesCompat.getColor(wr.get().getResources(), RARITY_TABLE_COLOR[tDoll.getRarity()], null)),
                        RARITY_TABLE[tDoll.getRarity()])));
                toolbar.setSubtitle("");
            } else {
                toolbar.setTitle(Html.fromHtml(String.format("<font color=\"#%06X\">No.%d</font> %s",
                        (0xFFFFFF & ResourcesCompat.getColor(wr.get().getResources(), android.R.color.darker_gray, null)),
                        tDoll.getId(), tDoll.getName())));
                toolbar.setSubtitle(Html.fromHtml(String.format("%s <font color=\"#%06X\">%s</font>",
                        tDoll.getType(),
                        (0xFFFFFF & ResourcesCompat.getColor(wr.get().getResources(), RARITY_TABLE_COLOR[tDoll.getRarity()], null)),
                        RARITY_TABLE[tDoll.getRarity()])));
            }

            roles.setText("");
            build.setText(tDoll.getCraftTime());
            hp.setText(tDoll.getHp()+"");
            dmg.setText(tDoll.getDmg()+"");
            acc.setText(tDoll.getAcc()+"");
            eva.setText(tDoll.getEva()+"");
            rof.setText(tDoll.getRof()+"");
            pbHP.setMax(100); pbHP.setProgress(tDoll.getHpBar());
            pbDMG.setMax(100); pbDMG.setProgress(tDoll.getDmgBar());
            pbACC.setMax(100); pbACC.setProgress(tDoll.getAccBar());
            pbEVA.setMax(100); pbEVA.setProgress(tDoll.getEvaBar());
            pbROF.setMax(100); pbROF.setProgress(tDoll.getRofBar());

            wr.get().parser2 = new Lvl2Parser(wr);
            wr.get().parser2.execute(tDoll);
            wr.get().invalidateOptionsMenu();
        }
    }

    private static class Lvl2Parser extends AsyncTask<TDoll, Void, TDoll> {
        WeakReference<CardActivity> wr;
        ArrayList<Parser.ParserException> exceptions;

        Lvl2Parser(WeakReference<CardActivity> wr) { this.wr = wr; }

        @Override
        protected TDoll doInBackground(TDoll... tDolls) {
            if (tDolls[0].getParsingLevel() >= 2) return tDolls[0];

            String server = PreferenceManager.getDefaultSharedPreferences(wr.get())
                    .getString("server", "EN");
            Parser parser = new Parser(wr.get().getCacheDir(), server);
            try {
                exceptions = parser.fullParse(tDolls[0]);
            } catch (Parser.ParserException e) {
                e.printStackTrace();
                return null;
            }

            if (PreferenceManager.getDefaultSharedPreferences(wr.get())
                    .getBoolean("doll_cache", true))
                DollCaching.cacheDoll(tDolls[0]);
            return tDolls[0];
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(TDoll tDoll) {
            CardActivity activity = wr.get();
            if (activity == null || activity.isFinishing()) return;
            if (tDoll == null) {
                Toast.makeText(wr.get(), "Error while parsing", Toast.LENGTH_SHORT).show();
                wr.get().finishAfterTransition();
                return;
            }

            if (exceptions != null && exceptions.size() > 0) {
                Snackbar.make(wr.get().findViewById(R.id.card_root), wr.get().getString(R.string.card_parserr), Snackbar.LENGTH_LONG)
                        .setAction(R.string.card_parserr_details, v -> {
                            StringBuilder sb1 = new StringBuilder();
                            for (Parser.ParserException e : exceptions) {
                                sb1.append(e.getMessage())
                                        .append(", Caused by: ")
                                        .append(e.getCause().getLocalizedMessage())
                                        .append(
                                        exceptions.indexOf(e) != exceptions.size()-1
                                                ? ";\n\n"
                                                : "");
                            }
                            new AlertDialog.Builder(wr.get())
                                    .setTitle(R.string.card_parserr_details_title)
                                    .setMessage(sb1.toString())
                                    .create().show();
                        }).show();
            }

            ImageView cg = wr.get().findViewById(R.id.card_maincg);
            TextView roles = wr.get().findViewById(R.id.card_role);
            TextView affects = wr.get().findViewById(R.id.card_affects);
            TextView buffs = wr.get().findViewById(R.id.card_buffs);
            WebView skills = wr.get().findViewById(R.id.card_skills);
            TextView description = wr.get().findViewById(R.id.card_desc);
            final ProgressBar cgPb = wr.get().findViewById(R.id.card_cgpb);

            if (cg != null && tDoll.getCgMain() != null && tDoll.getCgDamage() != null)
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

            //TODO Rewrite this code

            roles.setText(tDoll.getRole());
            affects.setText(activity.getString(R.string.card_affects)+" "+tDoll.getAffect().toUpperCase());
            buffs.setText(Html.fromHtml(tDoll.getBuffs()));
            skills.loadDataWithBaseURL("", tDoll.getSkills(), "text/html", "UTF-8", "");
            description.setText(Html.fromHtml(tDoll.getDescription()));

            roles.setVisibility(tDoll.getRole().isEmpty() ? View.GONE : View.VISIBLE);
            affects.setVisibility(tDoll.getAffect().isEmpty() ? View.GONE : View.VISIBLE);
            skills.setVisibility(tDoll.getSkills().isEmpty() ? View.GONE : View.VISIBLE);
            description.setVisibility(tDoll.getDescription().isEmpty() ? View.GONE : View.VISIBLE);

            RecyclerView rw = wr.get().findViewById(R.id.card_recycler);
            ArrayList<URL[]> urls = new ArrayList<>();
            ArrayList<String> titles = new ArrayList<>();
            urls.add(new URL[] {
                    tDoll.getCgMainHQ() != null ? tDoll.getCgMainHQ() : tDoll.getCgMain(),
                    tDoll.getCgDamageHQ() != null ? tDoll.getCgDamageHQ() : tDoll.getCgDamage()
            });
            titles.add(wr.get().getString(R.string.card_cg_main));
            urls.addAll(tDoll.getCostumes());
            titles.addAll(tDoll.getCostitles());
            rw.setLayoutManager(new LinearLayoutManager(wr.get(), LinearLayoutManager.VERTICAL, false));
            rw.setAdapter(new CgAdapter(urls, titles, cg == null));

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

            CardActivity cardActivity = wr.get();
            cardActivity.content.startAnimation(AnimationUtils.loadAnimation(cardActivity, R.anim.fadein));
            cardActivity.loadView.startAnimation(AnimationUtils.loadAnimation(cardActivity, R.anim.fadeout));
            cardActivity.content.setVisibility(View.VISIBLE);
            cardActivity.loadView.postOnAnimation(() -> {
                cardActivity.loadView.setVisibility(View.GONE);
                cardActivity.loadView.setAlpha(1);
            });
            cardActivity.webViews.add(skills);
            if (cardActivity.toTransit != null) cardActivity.toTransit.setTransitionName(null);
            if (cg != null) cg.setTransitionName(TN_CONTENT);
        }

        private static class CgAdapter extends RecyclerView.Adapter<CgAdapter.VH> {
            private ArrayList<String> titles;
            private ArrayList<URL[]> dataset;
            private boolean cMark;

            static class VH extends RecyclerView.ViewHolder {
                ProgressBar pb;
                TextView title;
                ImageView iw;
                ImageView iwdmg;

                VH(View itemView) {
                    super(itemView);
                    title = itemView.findViewById(R.id.card_cg_title);
                    iw = itemView.findViewById(R.id.card_cg_iw);
                    iwdmg = itemView.findViewById(R.id.card_cg_iw_dam);
                    pb = itemView.findViewById(R.id.card_cg_pb);
                }
            }

            CgAdapter(ArrayList<URL[]> urls, ArrayList<String> titles, boolean mark) {
                dataset = urls;
                this.titles = titles;
                cMark = mark;
            }

            @NonNull
            @Override
            public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_card_cg, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull VH holder, int position) {
                holder.title.setText(titles.get(position));

                final ProgressBar fPB = holder.pb;
                fPB.setVisibility(View.VISIBLE);

                RequestListener<Drawable> listener = new RequestListener<Drawable>() {
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
                };

                Glide.with(holder.itemView)
                        .load(dataset.get(position)[0].toString())
                        .apply(new RequestOptions().error(R.drawable.conprobl))
                        .listener(listener)
                        .into(holder.iw);
                Glide.with(holder.itemView)
                        .load(dataset.get(position)[1].toString())
                        .apply(new RequestOptions().error(R.drawable.conprobl))
                        .into(holder.iwdmg);

                final String url = dataset.get(position)[0].toString();
                final String urldm = dataset.get(position)[1].toString();
                holder.iw.setOnClickListener(v -> v.getContext().startActivity(
                        new Intent(Intent.ACTION_VIEW)
                                .setDataAndType(Uri.parse(url), "image/*")));
                holder.iwdmg.setOnClickListener(v -> v.getContext().startActivity(
                        new Intent(Intent.ACTION_VIEW)
                                .setDataAndType(Uri.parse(urldm), "image/*")));

                if (position == 0 && cMark) holder.iw.setTransitionName(TN_CONTENT);
            }

            @Override
            public int getItemCount() {
                return dataset.size();
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
        menu.findItem(R.id.card_menu_craft)
                .setVisible(doll != null && doll.getCraftTimeMins() < Integer.MAX_VALUE);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (viewPager != null && viewPager.getCurrentItem() != 0) {
            viewPager.setCurrentItem(0, true);
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(80);
                    runOnUiThread(this::finishAfterTransition);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        } else finishAfterTransition();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.card_menu_link:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.card_menu_link)
                        .setItems(R.array.card_links_en, (dialog, which) -> {
                            switch (which) {
                                case 0: startActivity(new Intent(Intent.ACTION_VIEW)
                                        .setData(Uri.parse(doll.getGamepress().toString()))); break;
                                case 1: startActivity(new Intent(Intent.ACTION_VIEW)
                                        .setData(Uri.parse(doll.getFws().toString()))); break;
                                case 2:
                                    if (doll.getWiki() != null)
                                        startActivity(new Intent(Intent.ACTION_VIEW)
                                                .setData(Uri.parse(doll.getWiki().toString())));
                                    else Toast.makeText(this, R.string.nowiki, Toast.LENGTH_LONG).show();
                                    break;
                            }
                        }).create().show();
                return true;
            case R.id.card_menu_craft:
                Intent intent = new Intent(this, CraftActivity.class)
                        .putExtra("id", doll.getId());
                startActivity(intent);

//                new AlertDialog.Builder(this)
//                        .setTitle(R.string.lib_tocraft_title)
//                        .setItems(R.array.lib_tocraft_options, (dialog, which) -> {
//                            Intent intent = new Intent(this, CraftActivity.class)
//                                    .putExtra("id", doll.getId())
//                                    .putExtra("option", which);
//                            startActivity(intent);
//                        })
//                        .setNegativeButton(android.R.string.cancel, null)
//                        .create().show();
                return true;
//            case R.id.card_menu_mod3:
//
//                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void finishAfterTransition() {
        for (WebView wv : webViews)
            wv.setVisibility(View.INVISIBLE);
        super.finishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        if (parser1 != null) parser1.cancel(true);
        if (parser2 != null) parser2.cancel(true);
        super.onDestroy();
    }
}
