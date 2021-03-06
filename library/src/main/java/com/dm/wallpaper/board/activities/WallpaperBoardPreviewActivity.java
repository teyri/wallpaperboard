package com.dm.wallpaper.board.activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dm.wallpaper.board.R;
import com.dm.wallpaper.board.R2;
import com.dm.wallpaper.board.adapters.WallpapersAdapter;
import com.dm.wallpaper.board.fragments.dialogs.WallpaperSettingsFragment;
import com.dm.wallpaper.board.helpers.ColorHelper;
import com.dm.wallpaper.board.helpers.DrawableHelper;
import com.dm.wallpaper.board.helpers.PermissionHelper;
import com.dm.wallpaper.board.helpers.ViewHelper;
import com.dm.wallpaper.board.helpers.WallpaperHelper;
import com.dm.wallpaper.board.preferences.Preferences;
import com.dm.wallpaper.board.utils.Animator;
import com.dm.wallpaper.board.utils.Extras;
import com.dm.wallpaper.board.utils.ImageConfig;
import com.kogitune.activitytransition.ActivityTransition;
import com.kogitune.activitytransition.ExitActivityTransition;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import uk.co.senab.photoview.PhotoViewAttacher;

/*
 * Wallpaper Board
 *
 * Copyright (c) 2017 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class WallpaperBoardPreviewActivity extends AppCompatActivity implements View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    @BindView(R2.id.wallpaper)
    ImageView mWallpaper;
    @BindView(R2.id.fab)
    FloatingActionButton mFab;
    @BindView(R2.id.progress)
    ProgressBar mProgress;

    private Runnable mRunnable;
    private Handler mHandler;
    private PhotoViewAttacher mAttacher;
    private ExitActivityTransition mExitTransition;

    private String mUrl;
    private String mName;
    private String mAuthor;
    private int mColor;
    private boolean mIsEnter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.setTheme(Preferences.getPreferences(this).isDarkTheme() ?
                R.style.WallpaperThemeDark : R.style.WallpaperTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_preview);
        ButterKnife.bind(this);
        ViewHelper.setApplicationWindowColor(this);
        ViewHelper.resetNavigationBarTranslucent(this,
                getResources().getConfiguration().orientation);
        mIsEnter = true;

        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        TextView toolbarTitle = ButterKnife.findById(this, R.id.toolbar_title);
        TextView toolbarSubTitle = ButterKnife.findById(this, R.id.toolbar_subtitle);
        ColorHelper.setTransparentStatusBar(this,
                ContextCompat.getColor(this, R.color.wallpaperStatusBar));
        mColor = ColorHelper.getAttributeColor(this, R.attr.colorAccent);
        mProgress.getIndeterminateDrawable().setColorFilter(mColor, PorterDuff.Mode.SRC_IN);

        if (savedInstanceState != null) {
            mUrl = savedInstanceState.getString(Extras.EXTRA_URL);
            mName = savedInstanceState.getString(Extras.EXTRA_NAME);
            mAuthor = savedInstanceState.getString(Extras.EXTRA_AUTHOR);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mUrl = bundle.getString(Extras.EXTRA_URL);
            mName = bundle.getString(Extras.EXTRA_NAME);
            mAuthor = bundle.getString(Extras.EXTRA_AUTHOR);
        }

        toolbarTitle.setText(mName);
        toolbarSubTitle.setText(mAuthor);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_toolbar_back);
        setSupportActionBar(toolbar);

        mFab.setOnClickListener(this);

        mExitTransition = ActivityTransition.with(getIntent())
                .to(this, mWallpaper, Extras.EXTRA_IMAGE)
                .duration(300)
                .start(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && savedInstanceState == null) {
            Transition transition = getWindow().getSharedElementEnterTransition();

            if (transition != null) {
                transition.addListener(new Transition.TransitionListener() {
                    @Override
                    public void onTransitionStart(Transition transition) {

                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        if (mIsEnter) {
                            mIsEnter = false;
                            Animator.startSlideDownAnimation(WallpaperBoardPreviewActivity.this,
                                    toolbar, null);
                            loadWallpaper(mUrl);
                        }
                    }

                    @Override
                    public void onTransitionCancel(Transition transition) {

                    }

                    @Override
                    public void onTransitionPause(Transition transition) {

                    }

                    @Override
                    public void onTransitionResume(Transition transition) {

                    }
                });
                return;
            }
        }

        mRunnable = () -> {
            toolbar.setVisibility(View.VISIBLE);
            loadWallpaper(mUrl);
            mRunnable = null;
            mHandler = null;
        };
        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, 700);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewHelper.resetNavigationBarTranslucent(this, newConfig.orientation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wallpaper_preview, menu);
        MenuItem save = menu.findItem(R.id.menu_save);
        save.setVisible(getResources().getBoolean(R.bool.enable_wallpaper_download));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(Extras.EXTRA_NAME, mName);
        outState.putString(Extras.EXTRA_AUTHOR, mAuthor);
        outState.putString(Extras.EXTRA_URL, mUrl);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (mAttacher != null) mAttacher.cleanup();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        WallpapersAdapter.sIsClickable = true;
        if (mHandler != null && mRunnable != null)
            mHandler.removeCallbacks(mRunnable);
        if (mExitTransition != null) mExitTransition.exit(this);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.menu_save) {
            if (PermissionHelper.isPermissionStorageGranted(this)) {
                WallpaperHelper.downloadWallpaper(this, mColor, mUrl, mName);
                return true;
            }
            PermissionHelper.requestStoragePermission(this);
            return true;
        } else if (id == R.id.menu_wallpaper_settings) {
            WallpaperSettingsFragment.showWallpaperSettings(getSupportFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.fab) {
            WallpaperHelper.applyWallpaper(this, mAttacher.getDisplayRect(), mColor, mUrl, mName);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.PERMISSION_STORAGE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                WallpaperHelper.downloadWallpaper(this, mColor, mUrl, mName);
            } else {
                PermissionHelper.showPermissionStorageDenied(this);
            }
        }
    }

    private void loadWallpaper(String url) {
        DisplayImageOptions.Builder options = ImageConfig.getRawDefaultImageOptions();
        options.cacheInMemory(false);
        options.cacheOnDisk(true);

        ImageLoader.getInstance().handleSlowNetwork(true);
        ImageLoader.getInstance().displayImage(url, mWallpaper, options.build(), new SimpleImageLoadingListener() {

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                super.onLoadingStarted(imageUri, view);
                mProgress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                super.onLoadingFailed(imageUri, view, failReason);
                int text = ColorHelper.getTitleTextColor(mColor);
                OnWallpaperLoaded(text);

                int color = ColorHelper.getAttributeColor(
                        WallpaperBoardPreviewActivity.this, R.attr.main_background);
                FrameLayout container = (FrameLayout) findViewById(R.id.container);
                ObjectAnimator colorFade = ObjectAnimator.ofObject(
                        container, "backgroundColor", new ArgbEvaluator(),
                        Color.TRANSPARENT, color);
                colorFade.setDuration(1000);
                colorFade.start();
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
                if (!Preferences.getPreferences(WallpaperBoardPreviewActivity.this).isScrollWallpaper()) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }

                if (loadedImage != null) {
                    Palette.from(loadedImage).generate(palette -> {
                        int accent = ColorHelper.getAttributeColor(
                                WallpaperBoardPreviewActivity.this, R.attr.colorAccent);
                        int color = palette.getVibrantColor(accent);
                        mColor = color;
                        int text = ColorHelper.getTitleTextColor(color);
                        mFab.setBackgroundTintList(ColorHelper.getColorStateList(
                                android.R.attr.state_pressed,
                                color, ColorHelper.getDarkerColor(color, 0.9f)));
                        OnWallpaperLoaded(text);
                    });
                }
            }
        });
    }

    private void OnWallpaperLoaded(@ColorInt int textColor) {
        mAttacher = new PhotoViewAttacher(mWallpaper);
        mAttacher.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mProgress.setVisibility(View.GONE);
        mRunnable = null;
        mHandler = null;

        mFab.setImageDrawable(DrawableHelper.getTintedDrawable(this,
                R.drawable.ic_fab_apply, textColor));
        Animator.showFab(mFab);
    }
}
