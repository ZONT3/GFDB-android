package ru.zont.gfdb;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.Charset;
import java.security.MessageDigest;

class GFTWTransform extends BitmapTransformation {
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
