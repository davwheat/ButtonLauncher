package com.brouken.wear.butcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.ProgressBar;

import static com.brouken.wear.butcher.Utils.log;

public class LaunchActivity extends WearableActivity {

    private ImageView mImageView;
    private ImageView mImageView2;
    private ImageView mImageView3;
    private ProgressBar mProgressBar;

    private long countdownStart;
    private int mProgressStatus = 3000;
    private ProgressBarAsync mProgressbarAsync;

    private boolean launchedViaAssist = false;
    private boolean launchedViaCustom = false;

    private String actionHomeDefault;
    private String actionHomeButton1;
    private String actionHomeButton1Long;

    private String actionExtraDefault;
    private String actionExtraButton1;
    private String actionExtraButton1Long;

    boolean longPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch2);

        loadConfig();

        mImageView = findViewById(R.id.imageView);
        //mImageView2 = findViewById(R.id.imageView2);
        //mImageView3 = findViewById(R.id.imageView3);
        mProgressBar = findViewById(R.id.progressBar);

        // Enables Always-on
        //setAmbientEnabled();

        //Uri ref = ActivityCompat.getReferrer(LaunchActivity.this);
        // launcher: android-app://com.google.android.wearable.app
        // home long press/assist: android-app://android
        // configurable button: android-app://com.google.android.apps.wearable.settings, because intent action is null

        boolean launchedViaLauncher = false;

        String action = getIntent().getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_ASSIST))
                launchedViaAssist = true;
            else if (action.equals(Intent.ACTION_MAIN))
                launchedViaLauncher = true;
        } else
            launchedViaCustom = true;

        if (launchedViaLauncher) {
            Intent config = new Intent(this, ConfigActivity.class);
            startActivity(config);
            finish();
        } else {
            if (!launchedViaAssist)
                vibrate();

            try {
                loadIcon();

                //mImageView2.setImageDrawable(icon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mProgressbarAsync = new ProgressBarAsync();
        //mProgressbarAsync.execute();
    }

    private void loadIcon() throws PackageManager.NameNotFoundException {
        String[] app;

        if (launchedViaAssist)
            app = actionHomeDefault.split("/");
        else
            app = actionExtraDefault.split("/");

        ComponentName componentName = new ComponentName(app[0], app[1]);
        Drawable icon = getPackageManager().getActivityIcon(componentName);
        mImageView.setImageDrawable(icon);

        ////
        /*
        if (launchedViaAssist)
            app = actionHomeButton1.split("/");
        else
            app = actionExtraButton1.split("/");

        componentName = new ComponentName(app[0], app[1]);
        icon = getPackageManager().getActivityIcon(componentName);
        mImageView2.setImageDrawable(icon);

        ////

        if (launchedViaAssist)
            app = actionHomeButton1Long.split("/");
        else
            app = actionExtraButton1Long.split("/");

        componentName = new ComponentName(app[0], app[1]);
        icon = getPackageManager().getActivityIcon(componentName);
        mImageView3.setImageDrawable(icon);
        */
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (launchedViaAssist && actionHomeDefault != null) {
            if (actionHomeButton1 == null && actionHomeButton1Long == null) {
                launchApp(actionHomeDefault);
            } else
                mProgressbarAsync.execute();
        } else if (launchedViaCustom && actionExtraDefault != null) {
            if (actionExtraButton1 == null && actionExtraButton1Long == null) {
                launchApp(actionExtraDefault);
            } else
                mProgressbarAsync.execute();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mProgressbarAsync.cancel(true);
    }

    private class ProgressBarAsync extends AsyncTask<Void, Integer, Void>{

        private boolean mRunning = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            countdownStart = System.currentTimeMillis();
            mRunning = true;
        }

        @Override
        protected Void doInBackground(Void...params) {
            while(mProgressStatus>0){
                try {
                    if(!mRunning)
                        break;

                    int diff = (int) (System.currentTimeMillis() - countdownStart);

                    mProgressStatus = mProgressBar.getMax() - diff;

                    publishProgress(mProgressStatus);
                    Thread.sleep(50);
                } catch(Exception e){
                    e.printStackTrace();

                    // test
                    mProgressStatus = 0;
                    mRunning = false;
                }
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            log("onCancelled");
            mRunning = false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mProgressBar.setProgress(mProgressStatus);
            /*if(!mTglStart.isChecked()){
                this.cancel(true);
            }*/
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //mTglStart.setChecked(false);

            if (mProgressStatus <= 0) {
                if (launchedViaAssist)
                    launchApp(actionHomeDefault);
                else
                    launchApp(actionExtraDefault);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        log("onKeyDown");

        if (keyCode == KeyEvent.KEYCODE_STEM_1) {
            log("KEYCODE_STEM_1");
            event.startTracking();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {

        log("onKeyLongPress");

        if (keyCode == KeyEvent.KEYCODE_STEM_1) {
            longPressed = true;
            if (launchedViaAssist)
                launchApp(actionHomeButton1Long);
            else
                launchApp(actionExtraButton1Long);
            return true;
        }

        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        log("onKeyUp");

        if (keyCode == KeyEvent.KEYCODE_STEM_1) {
            if (!longPressed) {
                if (launchedViaAssist)
                    launchApp(actionHomeButton1);
                else
                    launchApp(actionExtraButton1);
            }
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 30};
        vibrator.vibrate(pattern, -1);
    }

    private void launchApp(String app) {
        if (app == null)
            return;

        String pkg = app.split("/")[0];
        String cls = app.split("/")[1];
        launchApp(pkg, cls);
    }

    private void launchApp(String pkg, String cls) {
        if (isFinishing())
            return;

        ComponentName componentName = new ComponentName(pkg, cls);
        Intent intent=new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(componentName);
        startActivity(intent);

        vibrate();
        finish();
    }

    private void loadConfig() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        actionHomeDefault = sharedPreferences.getString("home_default", null);
        actionHomeButton1 = sharedPreferences.getString("home_button1", null);
        actionHomeButton1Long = sharedPreferences.getString("home_button1long", null);

        actionExtraDefault = sharedPreferences.getString("extra_default", null);
        actionExtraButton1 = sharedPreferences.getString("extra_button1", null);
        actionExtraButton1Long = sharedPreferences.getString("extra_button1long", null);
    }
}
