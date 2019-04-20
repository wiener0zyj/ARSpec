package com.magiccube.arspec;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.support.v4.app.Fragment;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class ARSpecActivity extends AppCompatActivity {
    private static final String TAG = ARSpecActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private Button btn_Camera;
    private TextView tv_Desp;
    private ArFragment arFragment;
    private ModelRenderable andyRenderable;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_arspec);


        btn_Camera = (Button) findViewById(R.id.btn_circle_camera);
        tv_Desp = (TextView)findViewById(R.id.tv_desp);

    }

    public void createFragmentAndOpenCamera(){
        if(arFragment == null){
            arFragment = new ArFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fl_main, arFragment).commit();


            // When you build a Renderable, Sceneform loads its resources in the background while returning
            // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
            ModelRenderable.builder()
                    .setSource(this, R.raw.andy)
                    .build()
                    .thenAccept(renderable -> andyRenderable = renderable)
                    .exceptionally(
                            throwable -> {
                                Toast toast =
                                        Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                return null;
                            });

            arFragment.setOnTapArPlaneListener(
                    (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                        if (andyRenderable == null) {
                            return;
                        }

                        // Create the Anchor.
                        Anchor anchor = hitResult.createAnchor();
                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());

                        // Create the transformable andy and add it to the anchor.
                        TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                        andy.setParent(anchorNode);
                        andy.setRenderable(andyRenderable);
                        andy.select();
                    });
        }

        btn_Camera.setVisibility(View.GONE);
        tv_Desp.setVisibility(View.GONE);

    }

    public void openCamera(View view){
        createFragmentAndOpenCamera();
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
}