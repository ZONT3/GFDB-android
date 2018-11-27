package ru.zont.gfdb;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import ru.zont.gfdb.core.Dimension;
import ru.zont.gfdb.core.TDoll;
import ru.zont.gfdb.core.TDolls;

public class TDollLibAdapter extends RecyclerView.Adapter<TDollLibAdapter.VH> {
    static final int SORT_ID = 0;

    private static final int[] RARITY_TABLE = {-1, -1, R.color.rarity_common, R.color.rarity_rare,
                                                    R.color.rarity_epic, R.color.rarity_legend, R.color.rarity_extra};

    private WeakReference<LibraryActivity> weakReference;

    private View.OnClickListener listener;
    private TDolls dataset;
    private int sort;

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

    TDollLibAdapter(@NonNull LibraryActivity context, @NonNull View.OnClickListener listener, @Nullable TDolls list) {
        dataset = list;
        sort = SORT_ID;
        weakReference = new WeakReference<>(context);
        this.listener = listener;
        if (dataset != null) return;
        dataset = LoadActivity.getCachedList(context);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_lib_item, parent, false);
        v.setOnClickListener(listener);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TDoll doll = dataset.get(position);

        switch (sort) {
            case SORT_ID:
                StringBuilder id_builder = new StringBuilder();
                if (doll.getRarity() < 6)
                    for (int i = 0; i < doll.getRarity(); i++) id_builder.append("★");
                else id_builder.append("EXTRA");
                holder.mMeta.setText(id_builder);
                holder.mMeta.setTextColor(ResourcesCompat.getColor(weakReference.get().getResources(), RARITY_TABLE[doll.getRarity()], null));
                holder.mType.setText(doll.getClss());
                holder.mTitle.setText(doll.getName());

                holder.mPB.setVisibility(View.VISIBLE);
                final ProgressBar fPB = holder.mPB;
                RequestOptions options = new RequestOptions().override(Dimension.setDp(80, 80, weakReference.get()).pxX);
                if (doll.getThumb().getHost().contains("fws")) options = new RequestOptions()
                        .override(Dimension.setDp(80, 160, weakReference.get()).pxX, Dimension.setDp(80, 160, weakReference.get()).pxY)
                        .transform(new GFTWTransform());
                Glide.with(holder.itemView)
                        .load(doll.getThumb().toString())
                        .apply(options)
                        .addListener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                target.onLoadFailed(weakReference.get().getDrawable(R.drawable.conprobl));
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
                break;
        }
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

    public TDolls getDataset() { return dataset; }
}
