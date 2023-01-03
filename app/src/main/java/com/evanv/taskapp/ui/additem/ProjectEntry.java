package com.evanv.taskapp.ui.additem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.evanv.taskapp.R;

public class ProjectEntry extends AppCompatActivity {
    // Key for the String representing the Project name.
    public static final String EXTRA_NAME = "com.evanv.taskapp.ui.additem.Project.EXTRA_NAME";
    // Key for the String representing the Project goal.
    public static final String EXTRA_GOAL = "com.evanv.taskapp.ui.additem.Project.EXTRA_GOAL";
    // Key for the int representing the color.
    public static final String EXTRA_COLOR = "com.evanv.taskapp.ui.additem.Project.EXTRA_COLOR";
    // Key for the bundle that contains information on the Project.
    public static final String EXTRA_ITEM = "com.evanv.taskapp.ui.additem.Project.EXTRA_ITEM";
    public int color; // User-selected project color

    /**
     * On creation of the project entry screen.
     *
     * @param savedInstanceState not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_entry);

        color = 11;
    }

    /**
     * Creates dialog to choose color of the project.
     *
     * @param view not used
     */
    public void handleColorPress(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.color_picker);
        dialog.setTitle("Pick Project Color");

        ImageButton[] buttons = new ImageButton[12];
        buttons[0] = dialog.findViewById(R.id.paleBlueButton);
        buttons[1] = dialog.findViewById(R.id.blueButton);
        buttons[2] = dialog.findViewById(R.id.paleGreenButton);
        buttons[3] = dialog.findViewById(R.id.greenButton);
        buttons[4] = dialog.findViewById(R.id.pinkButton);
        buttons[5] = dialog.findViewById(R.id.redButton);
        buttons[6] = dialog.findViewById(R.id.paleOrangeButton);
        buttons[7] = dialog.findViewById(R.id.orangeButton);
        buttons[8] = dialog.findViewById(R.id.lavenderButton);
        buttons[9] = dialog.findViewById(R.id.purpleButton);
        buttons[10] = dialog.findViewById(R.id.yellowButton);
        buttons[11] = dialog.findViewById(R.id.grayButton);

        for (ImageButton b : buttons) {
            b.setSelected(false);

            b.setOnClickListener(v -> {
                for (ImageButton button : buttons) {
                    button.setSelected(false);
                    button.setImageDrawable(
                            ContextCompat.getDrawable(this, R.drawable.ic_baseline_circle_24));
                }

                v.setSelected(true);
                ((ImageButton) v).setImageDrawable(
                        ContextCompat.getDrawable(this, R.drawable.ic_select_color_24));
            });
        }

        buttons[11].setSelected(true);

        dialog.findViewById(R.id.selectButton).setOnClickListener(view1 -> {
            // Set color to selected color.
            for (color = 0; color < 12; color++) {
                if (buttons[color].isSelected()) {
                    break;
                }
            }

            TextView colorLabel = findViewById(R.id.colorSelectTextView);
            String labelStart = getString(R.string.colorLabel);
            SpannableString colorText = new SpannableString(labelStart +
                    getResources().getStringArray(R.array.colors)[color]);

            int colorResource;

            switch (color) {
                case 0:
                    colorResource = getResources().getColor(R.color.pale_blue);
                    break;
                case 1:
                    colorResource = getResources().getColor(R.color.blue);
                    break;
                case 2:
                    colorResource = getResources().getColor(R.color.pale_green);
                    break;
                case 3:
                    colorResource = getResources().getColor(R.color.green);
                    break;
                case 4:
                    colorResource = getResources().getColor(R.color.pink);
                    break;
                case 5:
                    colorResource = getResources().getColor(R.color.red);
                    break;
                case 6:
                    colorResource = getResources().getColor(R.color.pale_orange);
                    break;
                case 7:
                    colorResource = getResources().getColor(R.color.orange);
                    break;
                case 8:
                    colorResource = getResources().getColor(R.color.lavender);
                    break;
                case 9:
                    colorResource = getResources().getColor(R.color.purple);
                    break;
                case 10:
                    colorResource = getResources().getColor(R.color.yellow);
                    break;
                case 11:
                    colorResource = getResources().getColor(R.color.gray);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + color);
            }

            colorText.setSpan(
                    new ForegroundColorSpan(colorResource), labelStart.length(), colorText.length(), 0);

            colorLabel.setText(colorText);

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * If no required fields are empty, pack user input into a bundle and send it.
     *
     * @param view not used
     */
    public void submit(View view) {
        String name = String.valueOf(((EditText)findViewById(R.id.editTextProjectName)).getText());
        String goal = String.valueOf(((EditText)findViewById(R.id.editTextGoal)).getText());

        if (name.equals("")) {
            Toast.makeText(this, R.string.name_reminder, Toast.LENGTH_LONG).show();
            return;
        }
        if (goal.equals("")) {
            Toast.makeText(this, R.string.goal_reminder, Toast.LENGTH_LONG).show();
            return;
        }

        Bundle bundle = new Bundle();

        bundle.putString(EXTRA_NAME, name);
        bundle.putString(EXTRA_GOAL, goal);
        bundle.putInt(EXTRA_COLOR, color);

        Intent replyIntent = new Intent();
        replyIntent.putExtra(EXTRA_ITEM, bundle);
        setResult(RESULT_OK, replyIntent);
        finish();
    }
}