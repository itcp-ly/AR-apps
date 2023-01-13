package com.vt6002.ar_apps;


import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
//Glow Button in Android help sites (https://www.geeksforgeeks.org/glow-button-in-android/)
public class OptionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);
        findViewById(R.id.opt1)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(OptionActivity.this, Model_Select_Activity.class);
                    startActivity(intent);
                });
        findViewById(R.id.opt2)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(OptionActivity.this, Model_Create_Activity.class);
                    startActivity(intent);
                });
    }
}