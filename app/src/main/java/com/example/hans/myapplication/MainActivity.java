package com.example.hans.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    public static final String CURRENT = "current";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Handler handler = new Handler();

        final SharedPreferences persist = getPreferences(Context.MODE_PRIVATE);

        final Button back30 = (Button) findViewById(R.id.back30);
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

        final MediaPlayer mediaPlayer = MediaPlayer.create(this, Uri.fromFile(podcasts.get(0)));

        Thread thread = new Thread() {
            @Override
            public void run() {
                while (true) {
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
                File podcast = podcasts.get((int) id);

                persist.edit().putString(CURRENT, podcast.getName()).apply();

                try {
                    boolean changeSong = podcast != playing[0];

                    if (changeSong) {
                        if (mediaPlayer.isPlaying()) {
                            storePosition(playing[0], positions, persist, mediaPlayer.getCurrentPosition());
                        }
                        playNew(podcast, positions.get(podcast), text, mediaPlayer, seekBar);
                        playing[0] = podcast;
                        back30.setEnabled(true);
                        forward30.setEnabled(true);

                    } else if (mediaPlayer.isPlaying()) {
                        text.setText("pause: " + podcast.getName());
                        mediaPlayer.pause();
                        storePosition(podcast, positions, persist, mediaPlayer.getCurrentPosition());
                        back30.setEnabled(false);
                        forward30.setEnabled(false);

                    } else if (!mediaPlayer.isPlaying()) {
                        text.setText("play: " + podcast.getName());
                        mediaPlayer.start();
                        back30.setEnabled(true);
                        forward30.setEnabled(true);

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
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int duration = mediaPlayer.getDuration();
                    double relPos = progress / ((double) max);
                    mediaPlayer.seekTo((int) (duration * relPos));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        back30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.seekTo(Math.max(0, mediaPlayer.getCurrentPosition() - 30_000));
            }
        });
        forward30.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.seekTo(Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 30_000));
            }
        });
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
