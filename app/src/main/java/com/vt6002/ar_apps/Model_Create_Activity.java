package com.vt6002.ar_apps;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.andresoviedo.util.android.AndroidUtils;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.android.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
//help sites for import file to apps (https://github.com/the3deer/android-3D-model-viewer)
public class Model_Create_Activity extends AppCompatActivity {
    private Map<String, Object> loadModelParameters = new HashMap<>();
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1000;
    private static final int REQUEST_CODE_LOAD_MODEL = 1101;
    private static final int REQUEST_READ_CONTENT_PROVIDER = 1002;
    private static final String SUPPORTED_FILE_TYPES_REGEX = "(?i).*\\.(glb)";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_create);

        Button button = findViewById(R.id.uploadBtn);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                loadModelFromSdCard();
            }
        });
    }

    private void loadModelFromSdCard() {
        // check permission starting from android API 23 - Marshmallow
        if (AndroidUtils.checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_EXTERNAL_STORAGE)) {
            FileUtils.createChooserDialog(this, "Select file", null, null, SUPPORTED_FILE_TYPES_REGEX,
                    (File file) -> {
                        if (file != null) {
                            ContentUtils.setCurrentDir(file.getParentFile());
                            launchModelRendererActivity(Uri.parse("file://" + file.getAbsolutePath()));
                        }
                    });
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ContentUtils.setThreadActivity(this);

        try {
            Uri uri = data.getData();
            if (uri == null) {
                return;
            }

            onLoadModel(uri);
        } catch (Exception ex) {

                Toast.makeText(this, "Unexpected exception: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
    }
    private Uri getUserSelectedModel() {
        return (Uri) loadModelParameters.get("model");
    }
    private void launchModelRendererActivity(Uri uri) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            URI.create(uri.toString());
            intent.putExtra("uri", uri.toString());
        } catch (Exception e) {
            // info: filesystem url may contain spaces, therefore we re-encode URI
            try {
                intent.putExtra("uri", new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), uri.getFragment()).toString());
            } catch (URISyntaxException ex) {
                Toast.makeText(this, "Error: " + uri.toString(), Toast.LENGTH_LONG).show();
                return;
            }
        }


        startActivity(intent);
    }
    private void askForRelatedFiles() throws IOException {
        launchModelRendererActivity(getUserSelectedModel());
    }
    private void onLoadModel(Uri uri) throws IOException {
        // save user selected model
        loadModelParameters.put("model", uri);
        askForRelatedFiles();
    }

}
