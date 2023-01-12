package com.vt6002.ar_apps;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;

import java.io.File;
import java.io.IOException;

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