package com.evanv.taskapp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.text.android.InternalPlatformTextApi;
import androidx.core.app.ActivityOptionsCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.evanv.taskapp.R;
import com.evanv.taskapp.db.TaskAppRoomDatabase;
import com.evanv.taskapp.ui.main.MainActivity;

import java.io.File;
import java.net.URI;

import de.raphaelebner.roomdatabasebackup.core.RoomBackup;

@InternalPlatformTextApi public class SettingsActivity extends AppCompatActivity {
    private RoomBackup mBackup;

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
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Create a backup system
        mBackup = new RoomBackup(this);
        mBackup.database(TaskAppRoomDatabase.getDatabase(this));

        // Handle backup completion
        mBackup.onCompleteListener((success, message, exitCode) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (success) {
                builder.setTitle(R.string.restore_title_success);
                builder.setCancelable(false);
                builder.setMessage(R.string.restore_message_success);
                builder.setPositiveButton(R.string.ok, (d, i) -> System.exit(0));
            }
            else {
                builder.setTitle(R.string.restore_title_failure);
                builder.setMessage(R.string.restore_message_failure);
                builder.setPositiveButton(R.string.ok, null);
            }
            builder.show();
        });

        // Backup the database
        findViewById(R.id.exportButton).setOnClickListener(v -> {
            mBackup.backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG);
            mBackup.backup();
        });

        // Restore the backup
        findViewById(R.id.importButton).setOnClickListener(v -> {
            mBackup.backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG);
            mBackup.restore();
        });
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
}