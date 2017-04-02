package com.example.root.anew;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button new_trek;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Animation translate = AnimationUtils.loadAnimation(this, R.anim.anim_translate);
        new_trek = (Button) findViewById(R.id.new_button);
        new_trek.startAnimation(translate);
        db = openOrCreateDatabase("Coordinate", MODE_PRIVATE, null);

        new_trek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Main2Activity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStop() {
        try
        {
            db.execSQL("delete from Coordinate");
        }
        catch (Exception e)
        {

        }
        super.onStop();
    }
}
