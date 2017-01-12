package com.example.mbriseno.audispot;

/**
 * Created by mbriseno on 1/4/2017.
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.util.concurrent.TimeUnit;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;

public class LoginActivity extends Activity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    @SuppressWarnings("SpellCheckingInspection")
    private static final String CLIENT_ID = "23da8a3cd45b4f84bf6e5e620fac3af0";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String REDIRECT_URI = "Audispot://callback";

    private static final int REQUEST_CODE = 1337;

    private static String tokenString;
    private static String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"playlist-read", "playlist-modify-public", "playlist-modify-private", "user-library-modify"})
                .build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        String token = CredentialsHandler.getToken(this);
        if (token == null) {
            setContentView(R.layout.activity_login);
        } else {
            startMainActivity(token);
        }
    }

    public void onLoginButtonClicked(View view) {
        final AuthenticationRequest request = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"playlist-read", "playlist-modify-public", "playlist-modify-private", "user-library-modify"})
                .build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    setName(response.getAccessToken());
                    logMessage("Welcome, " + name);
                    CredentialsHandler.setToken(this, response.getAccessToken(), response.getExpiresIn(), TimeUnit.SECONDS);
                    startMainActivity(response.getAccessToken());

                    break;

                // Auth flow returned an error
                case ERROR:
                    logError("Auth error: " + response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    logError("Auth result: " + response.getType());
            }
        }
    }

    private void setName(String token)    {
        tokenString = token;
        Thread t1 = new Thread(new Runnable() {
            public void run()
            {
                SpotifyApi api = new SpotifyApi();
                api.setAccessToken(tokenString);
                SpotifyService spotify = api.getService();
                name = spotify.getMe().id;
            }});
        t1.start();
    }

    private void startMainActivity(String token) {
        Intent intent = MainActivity.createIntent(this);
        intent.putExtra(MainActivity.EXTRA_TOKEN, token);
        startActivity(intent);
        finish();
    }

    private void logError(String msg) {
        Toast.makeText(this, "Error: " + msg, Toast.LENGTH_SHORT).show();
        Log.e(TAG, msg);
    }

    private void logMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.d(TAG, msg);
    }
}
