package ru.zont.gfdb;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ru.zont.gfdb.core.Dimension;
import ru.zont.gfdb.core.TDoll;
import ru.zont.gfdb.core.TDolls;

public class TDollLibAdapter extends RecyclerView.Adapter<TDollLibAdapter.VH> {
    @SuppressWarnings("WeakerAccess")
    static final int SORT_ID = 0;
    @SuppressWarnings("WeakerAccess")
    static final int SORT_NAME = 1;
    @SuppressWarnings("WeakerAccess")
    static final int SORT_RARITY = 2;
    @SuppressWarnings("WeakerAccess")
    static final int SORT_CONSTR = 3;
    @SuppressWarnings("WeakerAccess")
    static final int SORT_TYPE = 4;
    @SuppressWarnings("WeakerAccess")
    static final int SORT_HP = 5;
    @SuppressWarnings("WeakerAccess")
    static final int SORT_DMG = 6;
    @SuppressWarnings("WeakerAccess")
    static final int SORT_ACC = 7;
    @SuppressWarnings("WeakerAccess")
    static final int SORT_EVA = 8;
    @SuppressWarnings("WeakerAccess")
    static final int SORT_ROF = 9;

    private static final ArrayList<String> TYPES_ORDER = gto();
    private static final long PAUSE_BETWEEN_FILTER_CHANGES = 300;

    private String searchQuery = "";
    private int timeMins = -1;

    private static ArrayList<String> gto() {
        ArrayList<String> list = new ArrayList<>();
        list.add("HG");
        list.add("SMG");
        list.add("AR");
        list.add("RF");
        list.add("SG");
        return list;
    }

    private static final int[] RARITY_TABLE = {-1, -1, R.color.rarity_common, R.color.rarity_rare,
                                                    R.color.rarity_epic, R.color.rarity_legend, R.color.rarity_extra};

//    private WeakReference<RecyclerView> parent;

    private View.OnClickListener listener;
    private TDolls dataset;
//    private WeakReference<ProgressBar> pb;
    private WeakReference<TextView> nores;
    private final TDolls originalDataset;
    private int sort;
    private boolean reverse = false;

    static class VH extends RecyclerView.ViewHolder {

        TextView mMeta;
        TextView mTitle;
        TextView mType;
        ImageView mThumb;
        ProgressBar mPB;

        VH(View itemView) {
            super(itemView);
            mMeta = itemView.findViewById(R.id.lib_item_meta);
            mTitle = itemView.findViewById(R.id.lib_item_title);
            mType = itemView.findViewById(R.id.lib_item_type);
            mThumb = itemView.findViewById(R.id.lib_item_thumb);
            mPB = itemView.findViewById(R.id.lib_item_pb);
        }
    }

    TDollLibAdapter(@NonNull RecyclerView parent, @NonNull ProgressBar pb, @NonNull TextView nores,
                    @NonNull View.OnClickListener listener, @NonNull TDolls list) {
        dataset = list;
        originalDataset = (TDolls) list.clone();
        sort = SORT_ID;
//        this.pb = new WeakReference<>(pb);
        this.nores = new WeakReference<>(nores);
//        this.parent = new WeakReference<>(parent);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_lib_item, parent, false);
        v.setOnClickListener(listener);
        return new VH(v);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final View itemView = holder.itemView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            holder.mMeta.setTextAppearance(android.R.style.TextAppearance_Material_Small);
        else holder.mMeta.setTextColor(itemView.getContext().getResources().getColor(android.R.color.secondary_text_dark));
        
        TDoll doll = dataset.get(position);
        String statMeta = "<font color=\"#%06X\">%s</font> <font color=\"#%06X\">%d%s</font>";
        String statStr = null;
        int statval = 0;
        int statProgr = 0;
        switch (sort) {
            default:
                StringBuilder id_builder = new StringBuilder();
                if (doll.getRarity() < 6)
                    for (int i = 0; i < doll.getRarity(); i++) id_builder.append("★");
                else id_builder.append("EXTRA");
                holder.mMeta.setText(id_builder);
                holder.mMeta.setTextColor(ResourcesCompat.getColor(itemView.getContext().getResources(), RARITY_TABLE[doll.getRarity()], null));
                break;
            case SORT_CONSTR:
                String constr_builder =
                        String.format("<font color=\"#%06X\">%s</font> %s",
                                (0xFFFFFF & ResourcesCompat.getColor(itemView.getContext().getResources(), RARITY_TABLE[doll.getRarity()], null)),
                                doll.getRarity() != 6 ? doll.getRarity()+"★" : "EX",
                                doll.getCraft());
                holder.mMeta.setText(Html.fromHtml(constr_builder));
                break;
            case SORT_HP: statStr = "HP"; statval = doll.getHp(); statProgr = doll.getHpBar(); break;
            case SORT_ACC: statStr = "ACC"; statval = doll.getAcc(); statProgr = doll.getAccBar(); break;
            case SORT_DMG: statStr = "DMG"; statval = doll.getDmg(); statProgr = doll.getDmgBar(); break;
            case SORT_EVA: statStr = "EVA"; statval = doll.getEva(); statProgr = doll.getEvaBar(); break;
            case SORT_ROF: statStr = "ROF"; statval = doll.getRof(); statProgr = doll.getRofBar(); break;
        }

        if (statStr != null) {
            int progrColor;
            if (statProgr > 60)
                progrColor = Color.rgb(/*r*/(int) (200f * (100f - statProgr) / 40f), /*g*/200, /*b*/(int) (200f * (100f - statProgr) / 40f));
            else progrColor = Color.rgb(/*r*/200, /*g*/(int) (125f * statProgr / 60f) + 75, /*b*/(int) (125f * statProgr / 60f + 75));

            holder.mMeta.setText(Html.fromHtml(String.format(statMeta,
                    (0xFFFFFF & ResourcesCompat.getColor(itemView.getContext().getResources(), RARITY_TABLE[doll.getRarity()], null)),
                    doll.getRarity() != 6 ? doll.getRarity()+"★" : "EX",
                    (0xFFFFFF & progrColor),
                    statval, statStr)));
        }

        holder.mType.setText(doll.getClss());
        holder.mTitle.setText(doll.getName());

        holder.mPB.setVisibility(View.VISIBLE);
        final ProgressBar fPB = holder.mPB;
        RequestOptions options = new RequestOptions().override(Dimension.setDp(80, 80, itemView.getContext()).pxX);
        if (doll.getThumb().getHost().contains("fws")) options = new RequestOptions()
                .override(Dimension.setDp(80, 160, itemView.getContext()).pxX, Dimension.setDp(80, 160, itemView.getContext()).pxY)
                .transform(new GFTWTransform());
        Glide.with(itemView)
                .load(doll.getThumb().toString())
                .apply(options)
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        target.onLoadFailed(itemView.getContext().getDrawable(R.drawable.conprobl));
                        fPB.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        fPB.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(holder.mThumb);
    }

    public static class GFTWTransform extends BitmapTransformation {
        private static final String ID = "ru.zont.gfdb.TDollLibAdapter.GFTWTransform";

        @Override
        protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
            return Bitmap.createBitmap(toTransform, 0, 0, toTransform.getWidth()/2, toTransform.getHeight());
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
            messageDigest.update(ID.getBytes(Charset.forName("UTF-8")));
        }

        @Override
        public int hashCode() {
            return ID.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof GFTWTransform;
        }
    }

    @Override
    public int getItemCount() {
        if (dataset==null) return 0;
        return dataset.size();
    }

    TDolls getDataset() { return dataset; }

    private void modifyDataset(TDolls newDataset) {
        int oldSize = dataset.size();
        for (TDoll doll : newDataset)
            if (!dataset.contains(doll)) dataset.add(doll);
        if (oldSize < dataset.size())
            notifyItemRangeInserted(oldSize, dataset.size() - oldSize);

        TDolls temp = (TDolls) dataset.clone();
        for (TDoll doll : temp) {
            if (!newDataset.contains(doll)) {
                int pos = temp.indexOf(doll);
                dataset.remove(doll);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, dataset.size());
            }
        }
        sort();
    }


//    private void sort() { sort(false); }
    private void sort(/*boolean silent*/) {
        Comparator<TDoll> comparator = new Comparator<TDoll>() {
            @Override
            public int compare(TDoll o1, TDoll o2) {
                switch (sort) {
                    case SORT_ID: return (reverse ? -1 : 1) * (o1.getId() - o2.getId());
                    case SORT_NAME: return  (reverse ? -1 : 1) * (o1.getName().compareTo(o2.getName()));
                    case SORT_CONSTR: return (reverse ? -1 : 1) * (o1.getCraftMins() - o2.getCraftMins());
                    case SORT_RARITY: return (reverse ? -1 : 1) * (o2.getRarity() - o1.getRarity());
                    case SORT_TYPE: return (reverse ? -1 : 1) * (TYPES_ORDER.indexOf(o1.getClss().toUpperCase()) - TYPES_ORDER.indexOf(o2.getClss().toUpperCase()));

                    case SORT_HP: return (reverse ? -1 : 1) * (o2.getHp() - o1.getHp());
                    case SORT_DMG: return (reverse ? -1 : 1) * (o2.getDmg() - o1.getDmg());
                    case SORT_ACC: return (reverse ? -1 : 1) * (o2.getAcc() - o1.getAcc());
                    case SORT_EVA: return (reverse ? -1 : 1) * (o2.getEva() - o1.getEva());
                    case SORT_ROF: return (reverse ? -1 : 1) * (o2.getRof() - o1.getRof());
                    default: return 0;
                }
            }
        };

        for (int out = dataset.size() - 1; out >= 1; out--) {  //Внешний цикл
            for (int in = 0; in < out; in++) {       //Внутренний цикл
                if(comparator.compare(dataset.get(in), dataset.get(in+1)) > 0) {
                    Collections.swap(dataset, in, in+1);
                    notifyItemMoved(in, in+1);
                }
            }
        }
    }

    private void filter() {
        TDolls newList = (TDolls) originalDataset.clone();

        if (!searchQuery.equals("")) for (TDoll doll : originalDataset)
            if (!doll.getName().toLowerCase().contains(searchQuery.toLowerCase()))
                newList.remove(doll);

        if (timeMins > -1) for (TDoll doll : originalDataset)
            if (doll.getCraftMins() != timeMins)
                newList.remove(doll);

        modifyDataset(newList);
        checkList();
    }

    void setReverse(boolean reverse) {
        if (reverse == this.reverse) return;
        this.reverse = reverse;
        sort();
    }

    boolean isReverse() { return reverse; }

    void changeSort(int sort) {
        this.sort = sort;
            notifyItemRangeChanged(0, getItemCount());
        sort();
    }

    int getSort() { return sort; }

    void resetList() {
        reverse = false;
        modifyDataset((TDolls) originalDataset.clone());
    }

    void applySearchQuery(String query) {
        searchQuery = query;
        filter();
    }

    void applyTimeFilter(int timeMins) {
        this.timeMins = timeMins;
        filter();
    }

    int getTimeFilter() { return timeMins; }

    boolean hasFilters() {
        return originalDataset.size() != dataset.size();
    }

    private void checkList() {
        nores.get().setVisibility(dataset.size() == 0 ? View.VISIBLE : View.GONE);
    }

//    void onDestroy() {
//        applier.interrupt();
//    }
}
