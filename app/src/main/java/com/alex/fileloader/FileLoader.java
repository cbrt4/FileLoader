package com.alex.fileloader;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public abstract class FileLoader {

    private HandlerThread downloadThread;
    private Handler downloadHandler;
    private Runnable downloadRunnable;

    private String loadUrl;
    private File localFile;
    private URI fileUri;
    private int bufferSize = 4 * 1024;
    private int readTimeout = 60 * 1000;

    private String TAG = this.getClass().getSimpleName();

    public FileLoader(@NonNull Context context, @NonNull String loadUrl) {
        this.loadUrl = loadUrl;
        localFile = new File(context.getFilesDir().getAbsolutePath(), loadUrl.substring(loadUrl.lastIndexOf(File.separator) + 1));
        fileUri = URI.create(localFile.getAbsolutePath());
    }

    public URI getFileUri() {
        return fileUri;
    }

    public void loadFile() {
        onStartLoading();

        if (fileDownloaded(localFile)) {
            fileUri = URI.create(localFile.getAbsolutePath());
        } else {
            runDownloadThread();
        }
    }

    public void cancel() {
        onCancel();
        stopDownloadThread();
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    private void runDownloadThread() {
        downloadThread = new HandlerThread("DownloadThread");
        downloadThread.start();
        downloadHandler = new Handler(downloadThread.getLooper());

        downloadRunnable = () -> {
            try {
                download();
            } catch (Throwable error) {
                removeFile(localFile);
                onError(error);
                Log.e(TAG, error.getLocalizedMessage());
            } finally {
                stopDownloadThread();
            }
        };

        downloadHandler.post(downloadRunnable);

        Log.i(TAG, "Download thread started.");
    }

    private void download() throws IOException {

        URLConnection connection = new URL(loadUrl).openConnection();
        connection.setReadTimeout(readTimeout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(localFile, false)) {
                byte[] buffer = new byte[bufferSize];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }

                fileUri = URI.create(localFile.getAbsolutePath());
                onSuccess(localFile);
            } catch (Throwable error) {
                removeFile(localFile);
                onError(error);
                Log.e(TAG, error.getLocalizedMessage());
            }
        } else {
            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(localFile, false);
            try {
                byte[] buffer = new byte[bufferSize];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }

                fileUri = URI.create(localFile.getAbsolutePath());
                onSuccess(localFile);
            } catch (Throwable error) {
                removeFile(localFile);
                onError(error);
                Log.e(TAG, error.getLocalizedMessage());
            } finally {
                inputStream.close();
                outputStream.flush();
                outputStream.close();
            }
        }
    }

    private boolean fileDownloaded(File inputFile) {
        return inputFile.exists();
    }

    private void removeFile(File inputFile) {
        if (inputFile.exists()) {
            Log.i(TAG, "Local file exists.");

            if (inputFile.delete()) {
                Log.i(TAG, "Local file removed.");
            }
        }
    }

    private void stopDownloadThread() {

        if (downloadHandler != null) {
            downloadHandler.removeCallbacks(downloadRunnable);
            downloadRunnable = null;
            downloadHandler = null;
        }

        if (downloadThread != null) {
            downloadThread.quit();
        }

        Log.i(TAG, "Download thread stopped.");
    }

    public abstract void onStartLoading();

    public abstract void onSuccess(File file);

    public abstract void onError(Throwable error);

    public abstract void onCancel();
}
