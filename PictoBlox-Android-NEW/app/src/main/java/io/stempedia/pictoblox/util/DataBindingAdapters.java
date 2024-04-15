package io.stempedia.pictoblox.util;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.storage.StorageReference;

import io.stempedia.pictoblox.learn.CourseContentTypes;

public class DataBindingAdapters {

    @BindingAdapter("android:src")
    public static void setImageResource(ImageView imageView, int resource) {
        imageView.setImageResource(resource);
    }

    @BindingAdapter("android:background")
    public static void setViewBackground(View v, int resource) {
        v.setBackgroundResource(resource);
    }

    @BindingAdapter("android:layout_height")
    public static void setLayoutHeight(View view, int height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
    }

/*    @BindingAdapter("bind:imageBitmap")
    public static void loadImage(ImageView iv, Bitmap bitmap) {
        iv.setImageBitmap(bitmap);
    }*/


    @BindingAdapter("imageBitmap")
    public static void loadBitmapIntoImage(ImageView iv, Bitmap bitmap) {
        iv.setImageBitmap(bitmap);
    }

    @BindingAdapter("loadReferenceGlide")
    public static void loadImage(ImageView view, StorageReference storageReference) {
        Glide.with(view.getContext())
                .load(storageReference)
                //.transform(new RoundedCorners(32))
                //.apply(new RequestOptions().circleCrop())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(view);
    }

    @BindingAdapter("loadUrlGlide")
    public static void loadImage(ImageView view, Uri uri) {
        Glide.with(view.getContext())
                .load( uri)
                .apply(new RequestOptions().circleCrop())
                .into(view);
    }

    @BindingAdapter("localImagePath")
    public static void loadImage(ImageView view, String localImagePath) {
        Glide.with(view.getContext())
                .load( localImagePath)
                .into(view);
    }

/*    @BindingAdapter(value = { "localImagePath", "contentType" })
    public static void loadImage(ImageView view, String localImagePath, CourseContentTypes contentType) {
        Glide.with(view).load(localImagePath).asGif().into(view);

        Glide.with(view)
                .load(localImagePath)
                .into(view);

        if (contentType == CourseContentTypes.GIF) {
                rb.asGif();

        }


    }*/
}
