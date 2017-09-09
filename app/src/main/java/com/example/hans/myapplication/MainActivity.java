package com.example.hans.myapplication;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


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

        Collections.sort(podcasts);

        List<String> names = new LinkedList<>();
        for (File podcast : podcasts) names.add(podcast.getName());

        final MediaPlayer mediaPlayer = MediaPlayer.create(this, Uri.fromFile(podcasts.get(0)));

        ListView listView = (ListView) findViewById(R.id.pods);
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("id = " + id);
                File podcast = podcasts.get((int) id);
                System.out.println("podcasts = " + podcast.getName());

                TextView text = (TextView) findViewById(R.id.thetext);

                try {
                    if (mediaPlayer.isPlaying()) {
                        text.setText("pause");
                        mediaPlayer.pause();

                    } else {
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(MainActivity.this, Uri.fromFile(podcast));
                        mediaPlayer.prepare();
                        text.setText("playing");
                        mediaPlayer.start();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
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
