package com.tool.greeting_tool;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tool.greeting_tool.common.constant.ButtonString;
import com.tool.greeting_tool.common.constant.ErrorMessage;
import com.tool.greeting_tool.common.constant.KeySet;
import com.tool.greeting_tool.common.constant.MessageConstant;
import com.tool.greeting_tool.common.utils.FormatCheckerUtil;

import java.util.ArrayList;

public class Postcode_fill extends AppCompatActivity{
    /**
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postcode_fill);

        Intent intent = getIntent();
        String[] selectList = intent.getStringArrayExtra(KeySet.SelectedList);

        EditText postCode = findViewById(R.id.postcode);
        ImageButton submitButton = findViewById(R.id.submit_postcode_button);

        submitButton.setOnClickListener(v -> {
            String postcode = FormatCheckerUtil.processPostcode(postCode.getText().toString().trim().toUpperCase());

            if(FormatCheckerUtil.checkPostcode(postcode)){
                showPostCodeDialog(postcode, selectList);
            }else{
                Toast.makeText(Postcode_fill.this, ErrorMessage.INVALID_POSTCODE, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Temporarily use Dialog to show the postcode
     * @param postcode : the postcode that user insert
     */
    private void showPostCodeDialog(String postcode, String[] selectList) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_dialog_2, null);

        TextView messageTextView = dialogView.findViewById(R.id.dialog_message_1);
        messageTextView.setText(MessageConstant.PostCodeMessage + postcode);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(ButtonString.positiveSet, (dialogInterface, which) -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(KeySet.PostKey, postcode);
                    resultIntent.putExtra(KeySet.SelectedList, selectList);
                    resultIntent.putExtra(KeySet.IsSend, 2);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                })
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextAppearance(R.style.CustomButton_pre_se);
            positiveButton.setTextColor(Color.parseColor("#FAC307"));
        });

        dialog.show();
    }

}