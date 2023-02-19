package com.evanv.taskapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.text.android.InternalPlatformTextApi;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.evanv.taskapp.R;
import com.evanv.taskapp.databinding.ActivityLabelsBinding;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.ui.additem.LabelEntry;
import com.evanv.taskapp.ui.main.MainActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
@InternalPlatformTextApi public class LabelsActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    private ChipGroup mChipGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLabelsBinding binding = ActivityLabelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mChipGroup = findViewById(R.id.chipGroup);

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        List<String> labelNames = LogicSubsystem.getInstance().getLabelNames();
        List<Integer> labelColors = LogicSubsystem.getInstance().getLabelColors();
        for (int i = 0; i < labelNames.size(); i++) {
            addChip(labelNames.get(i), labelColors.get(i), LogicSubsystem.getInstance().getLabelID(i));
        }
    }

    @SuppressWarnings("unused")
    private void clickChip (@SuppressWarnings("unused") long id, @SuppressWarnings("unused") View view) {
        AlertDialog.Builder diag = new AlertDialog.Builder(this);
        diag.setItems(R.array.label_options, (dialogInterface, i) -> {
            switch (i) {
                case 0:
                    Intent intent = new Intent(this, TaskListActivity.class);
                    intent.putExtra(TaskListActivity.EXTRA_LABELS, new long[]{id});
                    startActivity(intent);
                    break;
                case 1:
                    LabelEntry labelEntry = new LabelEntry();
                    labelEntry.setID(id);
                    labelEntry.setOnSubmit(view1 -> {
                        mChipGroup.removeView(view);
                        addChip(LogicSubsystem.getInstance().getLabelName(id),
                                LogicSubsystem.getInstance().getLabelColor(id), id);
                    });
                    labelEntry.show(getSupportFragmentManager(), "LABEL ENTRY");
                    break;
                case 2:
                    LogicSubsystem.getInstance().deleteLabel(id);
                    mChipGroup.removeView(view);
                    break;
            }
        });
        diag.show();
    }

    @SuppressWarnings("unused")
    private void addChip(String name, @SuppressWarnings("unused") int color, long id) {
        // Set label color
        int[] colors = {R.color.pale_blue,
                R.color.blue,
                R.color.pale_green,
                R.color.green,
                R.color.pink,
                R.color.red,
                R.color.pale_orange,
                R.color.orange,
                R.color.lavender,
                R.color.purple,
                R.color.yellow,
                R.color.gray};

        // What color is most readable on the background
        int[] textColors = { Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK,
                Color.BLACK,
                Color.WHITE,
                Color.BLACK,
                Color.BLACK};

        Chip toAdd = new Chip(this);
        toAdd.setText(name);
        toAdd.setChipMinHeightResource(R.dimen.text_height);
        toAdd.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        toAdd.setChipBackgroundColorResource(colors[color]);
        toAdd.setTextColor(textColors[color]);
        toAdd.setEnsureMinTouchTargetSize(false);
        toAdd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        toAdd.setOnClickListener(v -> clickChip(id, v));
        mChipGroup.addView(toAdd);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_button_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.labels_url)));
            startActivity(browserIntent);
        }
        else if (item.getItemId() == android.R.id.home) {
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