package com.example.mbriseno.audispot;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import java.util.*;

import android.view.View;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Connectivity;
import com.wnafee.vector.MorphButton;


import java.util.List;
import java.util.concurrent.TimeUnit;
//import java.util.logging.Handler;


import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;


public class MainActivity extends AppCompatActivity implements
        SpotifyPlayer.NotificationCallback,ConnectionStateCallback,Search.View
{

    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "23da8a3cd45b4f84bf6e5e620fac3af0";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "Audispot://callback";
    static final String EXTRA_TOKEN = "EXTRA_TOKEN";
    private static final String KEY_CURRENT_QUERY = "CURRENT_QUERY";

    private Search.ActionListener mActionListener;

    private LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
    private ScrollListener mScrollListener = new ScrollListener(mLayoutManager);
    private SearchResultsAdapter mAdapter;
    private SpotifyPlayer  mPlayer;
    private Track curTrack;
    private Button fwdButton,pauseButton,rwdButton;
    private int forwardTime = 5000;
    private int backwardTime = 5000;
    private SeekBar seekbar;
    public TextView tx1,tx2,tx3;
    private long startTime = 0;
    private long finalTime = 0;
    public static int oneTimeOnly = 0;
    private Handler myHandler = new Handler();;

    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
           // logStatus("OK!");
        }

        @Override
        public void onError(Error error) {
            //logStatus("ERROR:" + error);
        }
    };

    private Connectivity getNetworkConnectivity(Context context) {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String token = intent.getStringExtra(EXTRA_TOKEN);

        mActionListener = new SearchPresenter(this, this);
        mActionListener.init(token);

        //Button pauseButton = (Button)findViewById(R.id.button2);
        //Button stopButton = (Button)findViewById(R.id.button3);

        fwdButton = (Button) findViewById(R.id.button);
        pauseButton = (Button) findViewById(R.id.button2);
        rwdButton = (Button)findViewById(R.id.button4);

        tx1 = (TextView)findViewById(R.id.textView2);
        tx2 = (TextView)findViewById(R.id.textView3);
        tx3 = (TextView)findViewById(R.id.textView4);
        seekbar = (SeekBar)findViewById(R.id.seekBar);


        seekbar.setClickable(false);
        fwdButton.setEnabled(true);
        rwdButton.setEnabled(true);
        pauseButton.setEnabled(true);

        // Setup search field
        final SearchView searchView = (SearchView) findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mActionListener.search(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


        // Setup search results list
        //A Track has been selected
        mAdapter = new SearchResultsAdapter(this, new SearchResultsAdapter.ItemSelectedListener() {
            @Override
            public void onItemSelected(View itemView, Track item) {
                mActionListener.selectTrack(item);
                mPlayer.playUri(null, item.uri, 0, 0);
                //mPlayer.
                //playButton.setText("Pause");
                fwdButton.setEnabled(true);
                rwdButton.setEnabled(true);
                pauseButton.setEnabled(true);
                curTrack = item;
                finalTime = curTrack.duration_ms;
                tx3.setText(curTrack.name);
                tx3.setTextSize(25);
                tx3.setWidth(100);
                tx3.setHeight(150);
                tx3.setLines(1);
                tx3.setHorizontallyScrolling(true);
                tx3.isFocusableInTouchMode();
                tx3.isFocusable();
                tx3.setSelected(true);
                tx3.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                tx3.setMarqueeRepeatLimit(-1);

                System.out.println(curTrack.name);
                //startTime = curTrack.
                seekbar.setMax((int) finalTime);
                tx2.setText(String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                        TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                        finalTime)))
                );

                tx1.setText(String.format("%d min, %d sec",
                        TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                        TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                        startTime)))
                );
                seekbar.setMax((int) finalTime);
               // seekbar.setProgress((int)startTime);
                myHandler.postDelayed(UpdateSongTime,100);

            }
        });

        RecyclerView resultsList = (RecyclerView) findViewById(R.id.search_results);
        resultsList.setHasFixedSize(true);
        resultsList.setLayoutManager(mLayoutManager);
        resultsList.setAdapter(mAdapter);
        resultsList.addOnScrollListener(mScrollListener);

        Config playerConfig = new Config(getApplicationContext(), intent.getStringExtra(EXTRA_TOKEN), CLIENT_ID);
        // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
        // the second argument in order to refcount it properly. Note that the method
        // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
        // one passed in here. If you pass different instances to Spotify.getPlayer() and
        // Spotify.destroyPlayer(), that will definitely result in resource leaks.
        mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer player) {
                //logStatus("-- Player initialized --");
                mPlayer.setConnectivityStatus(mOperationCallback, getNetworkConnectivity(MainActivity.this));
                mPlayer.addNotificationCallback(MainActivity.this);
                mPlayer.addConnectionStateCallback(MainActivity.this);
                // Trigger UI refresh
            }

            @Override
            public void onError(Throwable error) {
               // logStatus("Error in initialization: " + error.getMessage());
            }
        });

        FloatingActionButton myFab = (FloatingActionButton) findViewById(R.id.myFAB);
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //if (curTrack != null) {
                    Thread t1 = new Thread(new Runnable() {
                        public void run()
                        {
                            Looper.prepare();
                            if (addToLibrary())
                                Toast.makeText(getApplicationContext(), curTrack.name + " was added to your library!", Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(getApplicationContext(), "No track selected", Toast.LENGTH_LONG).show();
                            System.out.print("pushed FAB");
                        }});
                    t1.start();
                //}
            }
        });




            // If Activity was recreated wit active search restore it
        if (savedInstanceState != null) {
            String currentQuery = savedInstanceState.getString(KEY_CURRENT_QUERY);
            mActionListener.search(currentQuery);
        }
    }

    public boolean addToLibrary()
    {
        SpotifyApi api = new SpotifyApi();
        Intent intent = getIntent();
        String token = intent.getStringExtra(EXTRA_TOKEN);
        api.setAccessToken(token);

        SpotifyService spotify = api.getService();
        //Looper.prepare();
        if (curTrack != null) {
            spotify.addToMySavedTracks(curTrack.id);

            return true;
        }
        else
            return false;

    }

    public void playTrack(View v)
    {
        Button b = (Button)v;

        if(mPlayer.getPlaybackState().isPlaying && b != null){
            mPlayer.pause(null);
        }
        else {
            mPlayer.resume(null);
        }

        if (b == findViewById(R.id.button2)) {

        }


    }
    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            startTime = mPlayer.getPlaybackState().positionMs;
            tx1.setText(String.format("%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                    toMinutes((long) startTime)))
            );
            seekbar.setProgress((int)startTime);
            myHandler.postDelayed(this, 100);
        }
    };

    public void pauseTrack(View v)
    {

    }

    public void stopTrack(View v)
    {
        mPlayer.shutdown();
    }

    @Override
    public void reset() {
        mScrollListener.reset();
        mAdapter.clearData();
    }


    @Override
    public void addData(List<Track> items) {
        mAdapter.addData(items);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActionListener.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActionListener.resume();
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        mActionListener.destroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActionListener.getCurrentQuery() != null) {
            outState.putString(KEY_CURRENT_QUERY, mActionListener.getCurrentQuery());
        }
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        //Toast.makeText(MainActivity.this, "User logged in successfully", Toast.LENGTH_LONG);
        //mPlayer.playUri(null, "spotify:track:4mqxoq5MzQUnPg9AyFlVEt", 0, 0);


    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
        Toast.makeText(MainActivity.this, "User logged out successfully", Toast.LENGTH_SHORT);
    }


    @Override
    public void onLoginFailed(Error i) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    private class ScrollListener extends ResultListScrollListener {
        public ScrollListener(LinearLayoutManager layoutManager) {
            super(layoutManager);
        }

        @Override
        public void onLoadMore() {
            mActionListener.loadMoreResults();
        }
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }


}