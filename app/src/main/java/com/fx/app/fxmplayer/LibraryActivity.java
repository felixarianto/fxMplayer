package com.fx.app.fxmplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fx.app.wiglib.RecyclerBuilder;

import java.util.ArrayList;

public class LibraryActivity extends AppCompatActivity {

    private static final String TAG = "LibraryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        initButton();
        initList();

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.translate_left_to_right_enter, R.anim.translate_left_to_right_exit);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mLoadMoreOnScroll.cancel();
        } catch (Exception e) {
            Log.e("", "", e);
        }
    }


    private void initButton() {
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });
    }

    private ArrayList<FileHolder> DATA    = new ArrayList<>();
    private RecyclerBuilder.Adapter<FileHolder> mAdapter;
    private RecyclerBuilder.OnLoadMoreListener  mLoadMoreOnScroll;

    private void initList() {

        mAdapter = new RecyclerBuilder.Adapter<FileHolder>(R.layout.file_adapter, DATA) {

            @Override
            public void onBind(int position, View view, final FileHolder holder) {
                if (holder.isSeparator) {
                    view.findViewById(R.id.rly_separator).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.rly_content).setVisibility(View.GONE);

                    TextView txt_title = view.findViewById(R.id.txt_separator);
                    txt_title.setText(holder.name);
                    view.setOnClickListener(null);
                }
                else {
                    view.findViewById(R.id.rly_separator).setVisibility(View.GONE);
                    view.findViewById(R.id.rly_content).setVisibility(View.VISIBLE);

                    TextView txt_title = view.findViewById(R.id.txt_title);
                    TextView txt_subtitle = view.findViewById(R.id.txt_subtitle);
                    TextView txt_duration = view.findViewById(R.id.txt_duration);
                    ImageView   img_album = view.findViewById(R.id.img_album);

                    txt_title.setText(holder.name);
                    txt_subtitle.setText(holder.artist);
                    txt_duration.setText(Util.toDisplay(holder.duration));

                    Glide.with(img_album).load(holder.album_art).into(img_album);

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                Intent result = new Intent();
                                result.putExtra("path", holder.path);
                                setResult(RESULT_OK, result);
                                finish();
                            } catch (Exception e) {
                                Log.e(TAG, "", e);
                            }
                        }
                    });
                }
            }
        };

        mLoadMoreOnScroll = new RecyclerBuilder.OnLoadMoreListener(5) {
            @Override
            public void onLoadMore(CancellationSignal cancel) {
                load(LibraryActivity.this, DATA.size(), cancel);
            }
        };

        RecyclerView
        rcy_list = findViewById(R.id.rcy_list);
        rcy_list.setAdapter(mAdapter);
        rcy_list.addOnScrollListener(mLoadMoreOnScroll);
        rcy_list.requestFocus();

        GridLayoutManager
        manager = new GridLayoutManager(LibraryActivity.this, 2);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (DATA.get(position).isSeparator) {
                    return 2;
                }
                return 1;
            }
        });
        rcy_list.setLayoutManager(manager);

        mLoadMoreOnScroll.loadMore();

    }

    private class FileHolder {

        public String name;
        public String path;
        public String album_art;
        public String artist;
        public int    duration;
        public boolean isSeparator = false;

    }


    private int mLimit = 10;
    private void load(Context context, int index, CancellationSignal cancel) {
        Log.w(TAG, "LOADMORE");
        ContentResolver contentResolver = context.getContentResolver();
        Uri    uri    = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(
                uri,
                null,
                null,
                null,
                MediaStore.Audio.Media.TITLE + " ASC LIMIT " + mLimit + " OFFSET " + index,
                cancel
        );

        if (cursor == null) {

            Log.w(TAG, "LOADMORE NULL CURSOR");

        } else if (!cursor.moveToFirst()) {
            Log.w(TAG, "LOADMORE NO RECORD");

        } else {
            String sep = "";
            final int last_idx = DATA.size();
            do {
                if (cancel.isCanceled()) {
                    break;
                }
                FileHolder holder = new FileHolder();
                holder.name     = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                holder.artist   = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                holder.duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                holder.path     = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String album_id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                /*
                 * ALBUM ART
                 */
                Cursor cursor_art = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                        MediaStore.Audio.Albums._ID+ "=?",
                        new String[] {album_id},
                        null);

                if (cursor_art.moveToFirst()) {
                    holder.album_art = cursor_art.getString(cursor_art.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                }
                /*
                 * SEPARATOR
                 */
                sep = holder.name.substring(0, 1).toUpperCase();
                if (DATA.size() == 0) {
                    FileHolder
                    separator = new FileHolder();
                    separator.name        = sep;
                    separator.isSeparator = true;
                    DATA.add(separator);
                }
                else {
                    FileHolder prev = DATA.get(DATA.size() - 1);
                    if (!sep.equals(prev.name.substring(0, 1).toUpperCase())) {
                        FileHolder
                        separator = new FileHolder();
                        separator.name        = sep;
                        separator.isSeparator = true;
                        DATA.add(separator);
                    }
                }

                DATA.add(holder);
            } while (cursor.moveToNext());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int count = DATA.size() - last_idx;
                        if (count > 0) {
                            mAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "", e);
                    }
                }
            });

        }
    }

    int toDip(int px) {
        DisplayMetrics displaymetrics = getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, displaymetrics);
    }
}
