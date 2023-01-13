package com.evanv.taskapp.ui.additem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.evanv.taskapp.R;
import com.evanv.taskapp.logic.LogicSubsystem;

/**
 * Form for creating new labels.
 *
 * @author Evan Voogd
 */
public class LabelEntry extends AppCompatActivity {
    public int color; // User-selected label color

    /**
     * On creation of the label entry screen.
     *
     * @param savedInstanceState not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_entry);

        color = 11;
    }

    /**
     * Creates dialog to choose color of the label.
     *
     * @param view not used
     */
    public void handleColorPress(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.color_picker);
        dialog.setTitle(R.string.pick_label_color);

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

            // Change the color of the label to the chosen color.
            TextView colorLabel = findViewById(R.id.colorSelectTextView);
            String labelStart = getString(R.string.colorLabel);
            SpannableString colorText = new SpannableString(labelStart +
                    getResources().getStringArray(R.array.colors)[color]);

            int[] colors = {getResources().getColor(R.color.pale_blue),
                    getResources().getColor(R.color.blue),
                    getResources().getColor(R.color.pale_green),
                    getResources().getColor(R.color.green),
                    getResources().getColor(R.color.pink),
                    getResources().getColor(R.color.red),
                    getResources().getColor(R.color.pale_orange),
                    getResources().getColor(R.color.orange),
                    getResources().getColor(R.color.lavender),
                    getResources().getColor(R.color.purple),
                    getResources().getColor(R.color.yellow),
                    getResources().getColor(R.color.gray)};

            int colorResource = colors[color];

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
        String name = String.valueOf(((EditText)findViewById(R.id.editTextLabelName)).getText());

        if (name.equals("")) {
            Toast.makeText(this, R.string.name_reminder, Toast.LENGTH_LONG).show();
            return;
        }

        LogicSubsystem.getInstance().addLabel(name, color);
        finish();
    }
}