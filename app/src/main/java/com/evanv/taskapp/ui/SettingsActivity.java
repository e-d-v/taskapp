package com.evanv.taskapp.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.text.android.InternalPlatformTextApi;
import androidx.preference.PreferenceFragmentCompat;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.ui.main.MainActivity;

import java.util.Objects;


@InternalPlatformTextApi public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        setSupportActionBar(findViewById(R.id.toolbar));

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates todayTime in SharedPreferences
     */
    @Override
    protected void onPause() {
        // Update todayTime in SharedPreferences
        SharedPreferences sp = getSharedPreferences(MainActivity.PREF_FILE, MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putLong(MainActivity.PREF_DAY, LogicSubsystem.getInstance().getStartDate().toEpochDay());
        edit.putInt(MainActivity.PREF_TIME, LogicSubsystem.getInstance().getTodayTime());
        edit.putLong(MainActivity.PREF_TIMED_TASK, LogicSubsystem.getInstance().getTimedID());
        edit.putLong(MainActivity.PREF_TIMER, LogicSubsystem.getInstance().getTimerStart());

        edit.apply();
        super.onPause();
    }
}