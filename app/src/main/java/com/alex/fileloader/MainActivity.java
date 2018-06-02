package com.alex.fileloader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private String videoUrl = "http://clips.vorwaerts-gmbh.de/VfE_html5.mp4";
    private FileLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loader = new FileLoader(this, videoUrl) {

            @Override
            public void onStartLoading() {
                Log.w(this.getClass().getSimpleName(), "Start loading.");
                Log.w(this.getClass().getSimpleName(), "File URI: " + String.valueOf(this.getFileUri()));
            }

            @Override
            public void onSuccess(File file) {
                Log.w(this.getClass().getSimpleName(), "Success: " + file.getAbsolutePath());
                Log.w(this.getClass().getSimpleName(), "File URI: " + String.valueOf(this.getFileUri()));
            }

            @Override
            public void onError(Throwable error) {
                Log.w(this.getClass().getSimpleName(), "Error: " + error.getLocalizedMessage());
                Log.w(this.getClass().getSimpleName(), "File URI: " + String.valueOf(this.getFileUri()));
            }

            @Override
            public void onCancel() {
                Log.w(this.getClass().getSimpleName(), "Loading canceled.");
                Log.w(this.getClass().getSimpleName(), "File URI: " + String.valueOf(this.getFileUri()));
            }
        };

        loader.loadFile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        loader.cancel();
    }
}
