package com.magiccube.arspec;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import android.media.Image;
import android.graphics.ImageFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.core.Frame;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.FrameTime;

import com.google.zxing.Result;
import com.google.zxing.oned.EAN13Reader;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = ARSpecActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        setContentView(R.layout.activity_camera);

        arFragment = (ArFragment)getSupportFragmentManager().findFragmentById(R.id.fr_main);
        //getSupportFragmentManager().beginTransaction().add(R.id.fl_main, arFragment).commit();


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

        ArSceneView sceneView = arFragment.getArSceneView();
        // This is important to make sure that the camera stream renders first so that
        // the face mesh occlusion works correctly.

        sceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);

        Scene arScene = sceneView.getScene();
        arScene.addOnUpdateListener(
                (FrameTime frameTime) -> {
                    Frame currentFrame = null;
                    Image currentImage = null;
                    try{
                        currentFrame = sceneView.getArFrame();
                        currentImage = currentFrame.acquireCameraImage();
                        int imageFormat = currentImage.getFormat();
                        if (imageFormat == ImageFormat.YUV_420_888) {
                            Log.d("ImageFormat", "Image format is YUV_420_888");
                        }


                        //////////////////////////////////
                        //decode the bar code
                        //////////////////////////////////
                        if(currentImage != null){
                            EAN13Reader reader_en13 = new EAN13Reader();
                            GenericMultipleBarcodeReader mbarReader = new GenericMultipleBarcodeReader(reader_en13);

                            //convert bitmap to binarybitmap
                            ByteBuffer buffer = currentImage.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                            if(bitmapImage != null){
                                int[] intArray = new int[bitmapImage.getWidth()*bitmapImage.getHeight()];
                                bitmapImage.getPixels(intArray, 0, bitmapImage.getWidth(), 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight());
                                LuminanceSource source = new RGBLuminanceSource(bitmapImage.getWidth(), bitmapImage.getHeight(),intArray);
                                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                                Result[] results = mbarReader.decodeMultiple(bitmap);
                                for(int i = 0; i < results.length; i++){
                                    String info = results[i].getText();
                                }
                            }
                        }


                    }catch(Exception ex){
                        // do nothing
                        Log.e(TAG, "Sceneform Exception");
                    }finally {
                        if(currentImage != null){
                            currentImage.close();
                        }
                    }

                }
        );
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
