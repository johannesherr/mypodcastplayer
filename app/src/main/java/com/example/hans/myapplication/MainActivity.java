package com.example.hans.myapplication;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    public static final String CURRENT = "current";
    private MediaPlayer mediaPlayer;
    private Thread thread;
    private boolean destroy;
    private File podcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (fixPermissions()) {
            load();
        }
    }

    private void load() {
        setContentView(R.layout.activity_main);

        final Handler handler = new Handler();

        final SharedPreferences persist = getPreferences(Context.MODE_PRIVATE);

        final Button back30 = (Button) findViewById(R.id.back30);
        final Button playpause = (Button) findViewById(R.id.playpause);
        final Button forward30 = (Button) findViewById(R.id.forward30);
        back30.setEnabled(false);
        forward30.setEnabled(false);

        final TextView text = (TextView) findViewById(R.id.thetext);
        final SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        final int max = 100_000;
        seekBar.setMax(max);


        final List<File> podcasts = new LinkedList<>();
        File root = Environment.getExternalStorageDirectory();
        List<File> q = new LinkedList<>();
        q.add(root);
        while (!q.isEmpty()) {
            File n = q.remove(0);
            String name = n.getName();
            if (name.endsWith(".mp3") && n.length() > 10_000_000) {
                podcasts.add(n);
            }
            File[] children = n.listFiles();
            if (children != null) {
                q.addAll(Arrays.asList(children));
            }
        }

        final Map<File, Integer> positions = new HashMap<>();
        for (File podcast : podcasts) {
            positions.put(podcast, persist.getInt(podcast.getName(), 0));
        }

        // currently playing is always on top
        final String lastPlaying = persist.getString(CURRENT, "");
        Collections.sort(podcasts, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return Long.signum(num(rhs) - num(lhs));
            }

            private long num(File lhs) {
                if (lastPlaying.equals(lhs.getName())) return Long.MIN_VALUE;
                return lhs.lastModified();
            }
        });

        List<String> names = new LinkedList<>();
        for (File podcast : podcasts) names.add(String.format("%s (%sm)",
                podcast.getName().replaceAll(".mp3$", ""),
                positions.get(podcast) / 1000 / 60));

        this.podcast = podcasts.get(0);
        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(getPodcast()));

        thread = new Thread() {
            @Override
            public void run() {
                while (!destroy) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            int duration = mediaPlayer.getDuration();
                            int currentPosition = mediaPlayer.getCurrentPosition();
                            seekBar.setProgress((int) ((currentPosition / ((double) duration)) * max));

                            String text1 = text.getText().toString();
                            int idx = text1.lastIndexOf(" (");
                            int secs = currentPosition / 1000;
                            if (idx != -1) {
                                text1 = text1.substring(0, idx);
                            }
                            text.setText(String.format("%s (%02d:%02d)", text1, secs / 60, secs % 60));
                        }
                    });

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        thread.start();

        final File[] playing = {null};
        ListView listView = (ListView) findViewById(R.id.pods);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int listViewPosition, long id) {
                podcast = podcasts.get((int) id);

                persist.edit().putString(CURRENT, getPodcast().getName()).apply();

                try {
                    boolean changeSong = getPodcast() != playing[0];

                    if (changeSong) {
                        if (mediaPlayer.isPlaying()) {
                            storePosition(playing[0], positions, persist, mediaPlayer.getCurrentPosition());
                        }
                        playNew(getPodcast(), positions.get(getPodcast()), text, mediaPlayer, seekBar);
                        playing[0] = getPodcast();
                        back30.setEnabled(true);
                        forward30.setEnabled(true);

                    } else if (mediaPlayer.isPlaying()) {
                        doPause(getPodcast(), text, positions, persist, back30, forward30);

                    } else if (!mediaPlayer.isPlaying()) {
                        doPlay(getPodcast(), text, back30, forward30);

                    } else {
                        throw new AssertionError();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar11, int progress, boolean fromUser) {
                if (fromUser) {
                    int duration = mediaPlayer.getDuration();
                    double relPos = progress / ((double) max);
                    mediaPlayer.seekTo((int) (duration * relPos));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar11) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar11) {
            }
        });

        back30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.seekTo(Math.max(0, mediaPlayer.getCurrentPosition() - 30_000));
            }
        });
        playpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    doPause(getPodcast(), text, positions, persist, back30, forward30);
                } else {
                    doPlay(getPodcast(), text, back30, forward30);
                }
            }
        });
        forward30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.seekTo(Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 30_000));
            }
        });
    }

    private File getPodcast() {
        return podcast;
    }

    private void doPlay(File podcast, TextView text, Button back30, Button forward30) {
        text.setText("play: " + podcast.getName());
        mediaPlayer.start();
        back30.setEnabled(true);
        forward30.setEnabled(true);
    }

    private void doPause(File podcast, TextView text, Map<File, Integer> positions, SharedPreferences persist, Button back30, Button forward30) {
        text.setText("pause: " + podcast.getName());
        mediaPlayer.pause();
        storePosition(podcast, positions, persist, mediaPlayer.getCurrentPosition());
        back30.setEnabled(false);
        forward30.setEnabled(false);
    }

    private boolean fixPermissions() {
        // Here, thisActivity is the current activity
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this, new String[]{permission}, 42);
            return false;
        } else {
            return true;
        }
        
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            load();
        }
    }

    @Override
    protected void onDestroy() {
        destroy = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mediaPlayer.release();
        mediaPlayer = null;
        super.onDestroy();
    }

    private void storePosition(File podcast, Map<File, Integer> positions, SharedPreferences persist, int currentPosition) {
        positions.put(podcast, currentPosition);
        persist.edit().putInt(podcast.getName(), currentPosition).apply();
    }

    private void playNew(File podcast, int startPos, TextView text, MediaPlayer mediaPlayer, SeekBar seekBar) throws IOException {
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.setDataSource(MainActivity.this, Uri.fromFile(podcast));
        mediaPlayer.prepare();
        text.setText("playing: " + podcast.getName());
        mediaPlayer.seekTo(startPos);
        mediaPlayer.start();

        seekBar.setProgress(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
