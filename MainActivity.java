
package com.example.welcome.musicplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.welcome.musicplayer.SongInfo;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<SongInfo> _songs = new ArrayList<SongInfo>();
    ;
    RecyclerView recyclerView;
    SeekBar seekBar;
    SongAdapter songAdapter;
    MediaPlayer mediaPlayer;
    private Handler myHandler = new Handler();
    ;
    Runnable runnable;
    Button prevPos = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        songAdapter = new SongAdapter(this, _songs);
        recyclerView.setAdapter(songAdapter);
        mediaPlayer = new MediaPlayer();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                linearLayoutManager.getOrientation());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(dividerItemDecoration);
        songAdapter.setOnItemClickListener(new SongAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(final Button b, final View view, final SongInfo obj, int position) {

                    if(prevPos == null){
                        prevPos = b;
                    }
                    else if(b.getText().equals("Play")){
                        prevPos.setText("Play");
                        prevPos = b;
                    }
                    if (b.getText().equals("Stop")) {
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        mediaPlayer.release();
                       // mediaPlayer = null;
                        b.setText("Play");
                    }
                    else
                        {
                            Runnable runnable = new Runnable() {
                                @Override

                                public void run() {
                                    try {

                                        mediaPlayer.reset();
                                        mediaPlayer.setDataSource(obj.getSongUrl());
                                        mediaPlayer.prepareAsync();
                                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                            @Override
                                            public void onPrepared(MediaPlayer mp) {

                                                mp.start();
                                                seekBar.setProgress(0);
                                                seekBar.setMax(mediaPlayer.getDuration());

                                                Log.d("Prog", "run: " + mediaPlayer.getDuration());
                                            }
                                        });
                                        b.setText("Stop");


                                    } catch (Exception e) {
                                    }
                                }
                            };

                            myHandler.postDelayed(runnable, 100);
                            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                                int target = 0;
                                boolean rqsSeek = false;

                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress, boolean input) {
                                    if (input) {
                                        mediaPlayer.seekTo(progress);
                                    }
                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {

                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {
                                    if (rqsSeek) {
                                        if (mediaPlayer != null) {
                                            if (b.getText().equals("Play")) {
                                                //mediaPlayer.stop();
                                                b.setText("Stop");
                                                mediaPlayer.seekTo(target);
                                            }
                                        }
                                        rqsSeek = false;
                                    }

                                }
                            });


                    }
            }
        });
        checkUserPermission();

        Thread t = new runThread();
        t.start();
    }

    @Override
    protected void onDestroy() {
        if(mediaPlayer != null)
           super.onDestroy();
    }

    public class runThread extends Thread {


        @Override
        public void run() {
            while (true) {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d("Runwa", "run: " + 1);
                if (mediaPlayer != null) {
                    seekBar.post(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        }
                    });

                    Log.d("Runwa", "run: " + mediaPlayer.getCurrentPosition());
                }
            }
        }

    }

    private void checkUserPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
                return;
            }
        }
        loadSongs(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 123:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadSongs(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    checkUserPermission();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        }

    }

    private void loadSongs(Uri uri) {
        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    String url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                    SongInfo s = new SongInfo(name, artist, url);
                    _songs.add(s);

                } while (cursor.moveToNext());
            }

            cursor.close();
            songAdapter = new SongAdapter(MainActivity.this, _songs);

        }
    }


    public void playCycle() {
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
        if (mediaPlayer.isPlaying()) {
            runnable = new Runnable() {
                public void run() {
                    playCycle();
                }
            };
            myHandler.removeCallbacks(runnable);
        }

    }
}