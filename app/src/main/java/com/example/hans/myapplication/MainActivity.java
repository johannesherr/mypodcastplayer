package com.example.hans.myapplication;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

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
            if (name.endsWith(".mp3") && name.toLowerCase().contains("jocko")) {
                podcasts.add(n);
            }
            File[] children = n.listFiles();
            if (children != null) {
                q.addAll(Arrays.asList(children));
            }
        }

        Collections.sort(podcasts, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return num(rhs) - num(lhs);
            }

            private int num(File lhs) {
                Pattern nbr = Pattern.compile("\\d+");
                Matcher matcher = nbr.matcher(lhs.getName());
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(0));
                } else {
                    return 0;
                }
            }
        });

        List<String> names = new LinkedList<>();
        for (File podcast : podcasts) names.add(podcast.getName());

        final MediaPlayer mediaPlayer = MediaPlayer.create(this, Uri.fromFile(podcasts.get(0)));

        final File[] playing = {null};
        ListView listView = (ListView) findViewById(R.id.pods);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File podcast = podcasts.get((int) id);

                TextView text = (TextView) findViewById(R.id.thetext);

                try {
                    boolean changeSong = podcast != playing[0];
                    playing[0] = podcast;

                    if (changeSong) {
                        playNew(podcast, text, mediaPlayer, seekBar);

                    } else if (mediaPlayer.isPlaying()) {
                        text.setText("pause: " + podcast.getName());
                        mediaPlayer.pause();

                    } else if (!mediaPlayer.isPlaying()) {
                        text.setText("play: " + podcast.getName());
                        mediaPlayer.start();

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
    }

    private void playNew(File podcast, TextView text, MediaPlayer mediaPlayer, SeekBar seekBar) throws IOException {
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.setDataSource(MainActivity.this, Uri.fromFile(podcast));
        mediaPlayer.prepare();
        text.setText("playing: " + podcast.getName());
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
