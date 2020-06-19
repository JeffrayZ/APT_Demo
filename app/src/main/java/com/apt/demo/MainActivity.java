package com.apt.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.apt.annotation.ARouter;
import com.apt.annotation.BindView;

@ARouter(path = "/app/MainActivity")
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv111)
    TextView textView;

    @BindView(R.id.btn111)
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainActivityViewBinding.bind(this);
        textView.setText("打了个卡阿胶颗粒");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,textView.getText(),Toast.LENGTH_SHORT).show();
            }
        });

//        MainActivity$$ARouter.findTargetClass("/app/MainActivity");

//        Class cl
//         = MainActivity$$ARouter.findTargetClassUseJavaPoet("/app/MainActivity");
//
//        startActivity(new Intent(this,cl));
    }
}