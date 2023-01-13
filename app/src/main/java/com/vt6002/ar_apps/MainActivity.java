package com.vt6002.ar_apps;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.vt6002.ar_apps.videorecording.VideoRecorder;
import com.vt6002.ar_apps.videorecording.WritingArFragment;

import java.io.File;
import java.io.IOException;
import java.net.URI;
//Sceneform tutorials (https://developers.google.com/sceneform/develop) & (https://youtu.be/2YtlIiUKNdA)
//Sceneform code sample (https://github.com/google-ar/sceneform-android-sdk)
//Video Recording Class sample (https://github.com/google-ar/sceneform-android-sdk/tree/v1.15.0/samples/videorecording)
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private Anchor mAnchor = null;
    private TransformableNode mARObject = null;
    private AnchorNode mAnchorNode = null;
    // VideoRecorder encapsulates all the video recording functionality.
    private VideoRecorder videoRecorder;
    // The UI to record.
    private FloatingActionButton recordButton;
    private WritingArFragment arFragment;
    private URI paramUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_main);


        arFragment = (WritingArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arFragment);

        Intent intent = getIntent();

        if (intent.getStringExtra("uri") != null){
            try {
                File file = new File(new URI(intent.getStringExtra("uri")));
                buildModel(file);
            } catch (Exception ex) {
                Log.e("ModelActivity", "Error parsing activity parameters: " + ex.getMessage(), ex);
            }
        } else {
            FirebaseApp.initializeApp(this);
            FirebaseStorage storage = FirebaseStorage.getInstance();
            int position = intent.getIntExtra("position", 0);
            String[] myKeys = getResources().getStringArray(R.array.sections);
            StorageReference modelRef = storage.getReference().child(String.format("%s.glb", myKeys[position]));
            File file;
            try {
                file = File.createTempFile((String.format("%s", myKeys[position])), "glb");
                modelRef.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        buildModel(file);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
                if (renderable == null) {
                    return;
                }

                // Create the Anchor.
                mAnchor = hitResult.createAnchor();
                mAnchorNode = new AnchorNode(mAnchor);
                mAnchorNode.setParent(arFragment.getArSceneView().getScene());

                // Create the transformable object and add it to the anchor.
                mARObject = new TransformableNode(arFragment.getTransformationSystem());
                mARObject.setParent(mAnchorNode);
                mARObject.setRenderable(renderable);
                mARObject.select();

        });

        // Initialize the VideoRecorder.
        videoRecorder = new VideoRecorder();
        int orientation = getResources().getConfiguration().orientation;
        videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_2160P, orientation);
        videoRecorder.setSceneView(arFragment.getArSceneView());

        recordButton = findViewById(R.id.record);
        recordButton.setOnClickListener(this::toggleRecording);
        recordButton.setEnabled(true);
        recordButton.setImageResource(R.drawable.round_videocam);

    }

    @Override
    protected void onPause() {
        if (videoRecorder.isRecording()) {
            toggleRecording(null);
        }
        super.onPause();
    }

    /*
     * Used as a handler for onClick, so the signature must match onClickListener.
     */
    private void toggleRecording(View unusedView) {
        if (!arFragment.hasWritePermission()) {
            Log.e(TAG, "Video recording requires the WRITE_EXTERNAL_STORAGE permission");
            Toast.makeText(
                            this,
                            "Video recording requires the WRITE_EXTERNAL_STORAGE permission",
                            Toast.LENGTH_LONG)
                    .show();
            arFragment.launchPermissionSettings();
            return;
        }
        boolean recording = videoRecorder.onToggleRecord();
        if (recording) {
            recordButton.setImageResource(R.drawable.round_stop);
        } else {
            recordButton.setImageResource(R.drawable.round_videocam);
            String videoPath = videoRecorder.getVideoPath().getAbsolutePath();
            Toast.makeText(this, "Video saved: " + videoPath, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Video saved: " + videoPath);

            // Send  notification of updated content.
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.TITLE, "Sceneform Video");
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, videoPath);
            getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }


    private  ModelRenderable renderable;

    private void buildModel(File file) {
        RenderableSource renderableSource = RenderableSource
                .builder()
                .setSource(this, Uri.parse(file.getPath()), RenderableSource.SourceType.GLB)
                .setRecenterMode(RenderableSource.RecenterMode.ROOT)
                .build();
        ModelRenderable
                .builder()
                .setSource(this, renderableSource)
                .setRegistryId(file.getPath())
                .build()
                .thenAccept(modelRenderable -> {
                    Toast.makeText(this, "model built", Toast.LENGTH_SHORT).show();
                    renderable = modelRenderable;
                });
    }



}