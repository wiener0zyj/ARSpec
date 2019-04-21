package com.magiccube.arspec;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;


public class ARSpecActivity extends AppCompatActivity {

    private Button btn_Camera;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_arspec);


        btn_Camera = (Button) findViewById(R.id.btn_circle_camera);

        btn_Camera.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                gotoCameraPage();
            }
        });
    }


    public void gotoCameraPage(){
        Intent intent = new Intent(ARSpecActivity.this, CameraActivity.class);
        startActivity(intent);
    }


}