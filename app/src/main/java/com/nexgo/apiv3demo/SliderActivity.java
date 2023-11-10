package com.nexgo.apiv3demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.nexgo.mdbDemo.R;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.device.beeper.Beeper;
import com.nexgo.oaf.apiv3.device.scanner.OnScannerListener;
import com.nexgo.oaf.apiv3.device.scanner.Scanner;
import com.nexgo.oaf.apiv3.device.scanner.Scanner2;
import com.nexgo.oaf.apiv3.device.scanner.ScannerCfgEntity;
import com.nexgo.oaf.apiv3.device.scanner.SymbolEnum;

import java.util.Collections;

import cn.nexgo.mdbclient.constant.TransParam;

public class SliderActivity extends Activity {
    int request_code = 1;
    String strTag = "pruebasx", strCodigo = "";
    Beeper beep;
    Scanner scanner;
    Scanner2 scanner2;
    private DeviceEngine deviceEngine;
    ViewPager viewPager;
    Slider_Adapter adapter;
    private int page = 0;
    private Handler handler;
    private int delay = 5000; //milliseconds
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slider);
        deviceEngine = ((NexgoApplication) getApplication()).deviceEngine;


        ScannerCfgEntity entity = new ScannerCfgEntity();
        entity.isAutoFocus();
        entity.setNeedPreview(true);
        entity.setUsedFrontCcd(false);

        beep = deviceEngine.getBeeper();
        scanner = deviceEngine.getScanner();
        scanner.initScanner(entity, listener);
        scanner2 = deviceEngine.getScanner2();
        scanner2.initScanner(entity, Collections.singleton(SymbolEnum.QR));


        //getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        viewPager = findViewById(R.id.viewpager);
        adapter = new Slider_Adapter(this);
        viewPager.setAdapter(adapter);
        handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                if (adapter.getCount() == page) {
                    page = 0;
                } else {
                    page++;
                }
                viewPager.setCurrentItem(page, true);
                handler.postDelayed(this, delay);
            }
        };

        runnable.run();

        scanner2.start(listener);
    }



    OnScannerListener listener = new OnScannerListener() {
        @Override
        public void onInitResult(int i) {
            Log.d(strTag, "onInitResult > i: " + i);



        }

        @Override
        public void onScannerResult(int i, String s) {
            //Log.d(strTag, "onScannerResult > i: " + i + " result: " + s);
            if (strCodigo.length() == 0){
                strCodigo = s;
                beep.beep(1000);

                Intent saleIntent = new Intent(SliderActivity.this, EmvActivity2.class);
                saleIntent.putExtra(TransParam.REQUEST_TYPE, "");
                saleIntent.putExtra(TransParam.ORDER_AMOUNT, "2000");
                saleIntent.putExtra(TransParam.ORDER_NUMBER, "");
                saleIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //startActivityForResult(saleIntent, request_code);
                startActivityForResult(saleIntent, 2);// Activity is started with requestCode 2
            }

        }
    };



    // Call Back method  to get the Message form other Activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if(requestCode==2)
        {
            Log.i(strTag, "requestCode:" + requestCode);
            //String message=data.getStringExtra("MESSAGE");
            //textView1.setText(message);
        }
    }

}
