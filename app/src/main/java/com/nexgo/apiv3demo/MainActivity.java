package com.nexgo.apiv3demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.nexgo.common.ByteUtils;
import com.nexgo.common.LogUtils;
import com.nexgo.mdbDemo.R;
import com.nexgo.mdbservice.IPaymentCallback;
import com.nexgo.mdbservice.PaymentAppInfo;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.SdkResult;
import com.nexgo.oaf.apiv3.card.ultralight.UltralightCCardHandler;
import com.nexgo.oaf.apiv3.device.beeper.Beeper;
import com.nexgo.oaf.apiv3.device.led.LEDDriver;
import com.nexgo.oaf.apiv3.device.led.LightModeEnum;
import com.nexgo.oaf.apiv3.device.mdb.led.MdbLEDDriver;
import com.nexgo.oaf.apiv3.device.mdb.led.MdbLightModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.AlgorithmModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.PinKeyboardModeEnum;
import com.nexgo.oaf.apiv3.device.pinpad.PinPad;
import com.nexgo.oaf.apiv3.device.pinpad.WorkKeyTypeEnum;
import com.nexgo.oaf.apiv3.device.scanner.OnScannerListener;
import com.nexgo.oaf.apiv3.device.scanner.Scanner;
import com.nexgo.oaf.apiv3.device.scanner.Scanner2;
import com.nexgo.oaf.apiv3.device.scanner.ScannerCfgEntity;
import com.nexgo.oaf.apiv3.device.scanner.SymbolEnum;
import com.nexgo.oaf.apiv3.emv.AidEntity;
import com.nexgo.oaf.apiv3.emv.CapkEntity;
import com.nexgo.oaf.apiv3.emv.EmvHandler2;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.nexgo.mdbclient.ICompletionCallback;
import cn.nexgo.mdbclient.MdbServiceManager;
import cn.nexgo.mdbclient.constant.DeviceStatus;
import cn.nexgo.mdbclient.constant.LogLevel;
import cn.nexgo.mdbclient.constant.TransParam;
import cn.nexgo.mdbclient.constant.TransResult;
import cn.nexgo.mdbclient.constant.VendResult;

public class MainActivity extends Activity {
    private DeviceEngine deviceEngine;
    LEDDriver ledDriver;
    Scanner scanner;
    Scanner2 scanner2;
    //UltralightCCardHandler ultralightCCardHandler = deviceEngine.getUltralightCCardHandler();
    MdbLEDDriver ledDriver2; //BUSCAR ESTE PORQUE NO SALE ES PARA PRENDER EL LED DONDE SE INSERTA LA TARJETA
    Beeper beep;
    String strTag = "pruebasx";

    public static final Logger mlog = LoggerFactory.getLogger(MainActivity.class.getSimpleName());
    private EmvHandler2 emvHandler2;
    private EmvUtils emvUtils;
    private PinPad pinpad;
    private static final byte[] main_key_data = new byte[16];
    private static final byte[] work_key_data = new byte[16];
    private final int KEY_INDEX = 10;
    private PaymentAppInfo paymentAppInfo;

    private Button btnOpenMdb;

    private void initKey() {
        Arrays.fill(main_key_data, (byte) 0xFF);
        System.arraycopy(ByteUtils.hexString2ByteArray("F616DD76F290635EF616DD76F290635E"), 0, work_key_data, 0, 16);

        int result = pinpad.writeMKey(KEY_INDEX, main_key_data, main_key_data.length);
        mlog.debug("writeMKey result:{}", result);
        mlog.debug("isKeyExist:{}", result);
        result = pinpad.writeWKey(KEY_INDEX, WorkKeyTypeEnum.MACKEY, work_key_data, work_key_data.length);
        mlog.debug("writeWKey MACKEY result:{}", result);
        result = pinpad.writeWKey(KEY_INDEX, WorkKeyTypeEnum.PINKEY, work_key_data, work_key_data.length);
        mlog.debug("writeWKey PINKEY result:{}", result);
        result = pinpad.writeWKey(KEY_INDEX, WorkKeyTypeEnum.TDKEY, work_key_data, work_key_data.length);
        mlog.debug("writeWKey TDKEY result:{}", result);
        result = pinpad.writeWKey(KEY_INDEX, WorkKeyTypeEnum.ENCRYPTIONKEY, work_key_data, work_key_data.length);
        mlog.debug("writeWKey ENCRYPTIONKEY result:{}", result);
    }

    private Handler mMainHandler;
    private Handler mInitParamHandler;
    void initData(){
        paymentAppInfo = new PaymentAppInfo();
        paymentAppInfo.setClientId("XGD");
        paymentAppInfo.setModel(deviceEngine.getDeviceInfo().getModel());
        paymentAppInfo.setCountryCode("0840");
        paymentAppInfo.setDecimalPlaces((byte) 0x02);
        paymentAppInfo.setResponseTime((byte) 0x3C);
        paymentAppInfo.setLogLevel(LogLevel.INFO.ordinal());

        //Lector de Codigos

        scanner = deviceEngine.getScanner();
        ScannerCfgEntity entity = new ScannerCfgEntity();
        entity.isAutoFocus();
        entity.setNeedPreview(true);
        entity.setUsedFrontCcd(false);


        scanner.initScanner(entity, listener);

        scanner2 = deviceEngine.getScanner2();
        scanner2.initScanner(entity, Collections.singleton(SymbolEnum.QR));
        LightModeEnum lightModeEnum;

        ledDriver = deviceEngine.getLEDDriver();
        beep = deviceEngine.getBeeper();
        ledDriver2 = deviceEngine.getMdbLEDDriver();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mlog.debug("call onCreate()");
        setContentView(R.layout.activity_main);
        btnOpenMdb = findViewById(R.id.open_mdb);
        deviceEngine = ((NexgoApplication) getApplication()).deviceEngine;
        emvHandler2 = deviceEngine.getEmvHandler2("app2");
        emvUtils = new EmvUtils(MainActivity.this);
        pinpad = deviceEngine.getPinPad();
        pinpad.setAlgorithmMode(AlgorithmModeEnum.DES);
        pinpad.setPinKeyboardMode(PinKeyboardModeEnum.FIXED);


        HandlerThread initParamThread = new HandlerThread("initParamThread", -10);
        initParamThread.start();
        mMainHandler = new Handler(Looper.getMainLooper());
        mInitParamHandler = new Handler(initParamThread.getLooper());
        initData();


    }

    OnScannerListener listener = new OnScannerListener() {
        @Override
        public void onInitResult(int i) {
            Log.d(strTag, "onInitResult > i: " + i);



        }

        @Override
        public void onScannerResult(int i, String s) {
            Log.d(strTag, "onScannerResult > i: " + i + " result: " + s);

            beep.beep(1000);

            Intent saleIntent = new Intent(MainActivity.this, EmvActivity2.class);
            saleIntent.putExtra(TransParam.REQUEST_TYPE, "");
            saleIntent.putExtra(TransParam.ORDER_AMOUNT, "2000");
            saleIntent.putExtra(TransParam.ORDER_NUMBER, "");
            saleIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(saleIntent);
        }
    };

    private void postMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mMainHandler.post(runnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("pruebasx", "entro en esta clase MainActivity");
        LogUtils.setDebugEnable(true);
        if(!GData.getInstance().isInitSuccesss()){
//            Toast.makeText(this, "init param,please wait", Toast.LENGTH_SHORT).show();
            GData.getInstance().setInitSuccesss(true);
            mInitParamHandler.post(new Runnable() {
                @Override
                public void run() {
                    initEmvAid();
                    initEmvCapk();
                    initKey();
                }
            });
        }
    }
    @Override
    protected void onDestroy() {
        mlog.debug("call onDestroy()");
        closeMdbService();
        super.onDestroy();
    }
    private final ICompletionCallback iCompletionCallback = new ICompletionCallback() {
        @Override
        public void onSuccess() {
            mlog.debug("call mdbClient success!!");
        }

        @Override
        public void onFailure(int i) {
            mlog.debug("call mdbClient failure errcode:{}",i);
        }
    };

    private final IPaymentCallback iPaymentCallback = new IPaymentCallback.Stub() {
        @Override
        public int onPay(final int tradeType, final String itemPrice, final String itemNumber) {
            mlog.debug("call onPay() tradeType:{} itemPrice:{} itemNumber:{} isTrading:{}",
                    tradeType, itemPrice, itemNumber,GData.getInstance().isTrading());

            if(GData.getInstance().isTrading()){
                return 0;
            }
            GData.getInstance().setTrading(true);
            postMainThread(new Runnable() {
                @Override
                public void run() {
                    Intent saleIntent = new Intent(MainActivity.this, EmvActivity2.class);
                    saleIntent.putExtra(TransParam.REQUEST_TYPE, tradeType);
                    saleIntent.putExtra(TransParam.ORDER_AMOUNT, itemPrice);
                    saleIntent.putExtra(TransParam.ORDER_NUMBER, itemNumber);
                    saleIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(saleIntent);
                }
            });
            return 0;
        }

        @Override
        public void notifyMdbStatus(int mdbDeviceStatus) {
            mlog.debug("isTrading:{} mdbStatus:{}",GData.getInstance().isTrading(),DeviceStatus.values()[mdbDeviceStatus]);
            if(mdbDeviceStatus==DeviceStatus.SERVICE_ERR.ordinal()){
                restartMdbService();
            }
        }


        @Override
        public void notifyVendResult(int result) {
            switch (VendResult.values()[result]){

                case VEND_SUCCESS:

                case VEND_FAILURE:
//                    update the record db
                    break;
                case VEND_CANCEL:
                    ActivityCollector.finishAllActivity();
                    break;
                case VEND_END_SESSION:
                    GData.getInstance().setTrading(false);
                    break;
            }
        }
    };


    void closeMdbService() {
        MdbServiceManager.getInstance().onClose();
    }

    void restartMdbService() {
        MdbServiceManager.getInstance().onClose();
        MdbServiceManager.getInstance().onStart(getApplicationContext(),
                paymentAppInfo,
                iPaymentCallback,
                iCompletionCallback);
    }

    private void initEmvAid() {
        emvHandler2.delAllAid();
        if (emvHandler2.getAidListNum() <= 0) {
            List<AidEntity> aidEntityList = emvUtils.getAidList();
            if (aidEntityList == null) {
                mlog.debug("initAID failed");
                return;
            }
            int i = emvHandler2.setAidParaList(aidEntityList);
            mlog.debug("setAidParaList:{} ", i);
        } else {
            mlog.debug("setAidParaList " + "already load aid");
        }
    }


    private void initEmvCapk() {
        emvHandler2.delAllCapk();
        int capk_num = emvHandler2.getCapkListNum();
        mlog.debug("capk_num:{} ", capk_num);
        if (capk_num <= 0) {
            List<CapkEntity> capkEntityList = emvUtils.getCapkList();
            if (capkEntityList == null) {
                mlog.debug("initCAPK failed");
                return;
            }
            int j = emvHandler2.setCAPKList(capkEntityList);
            mlog.debug("setCAPKList:{} ", j);
        } else {
            mlog.debug("setCAPKList already load capk");
        }

    }

    //void setLed(MdbLightModeEnum light, boolean isOn)

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.open_mdb:
                scanner2.start(listener);
                //ledDriver2.setLed(MdbLightModeEnum.MAG_RED, true);
                //mlog.debug("start to open mdb!!");
                //beep.beep(1000);
                //scanner.startScan(60, listener);
                //ledDriver.setLed(LightModeEnum.BLUE, true);

                //MdbServiceManager.getInstance().onStart(getApplicationContext(),
                //        paymentAppInfo,
                //        iPaymentCallback,
                //        iCompletionCallback);

                //btnOpenMdb.setClickable(false);
                //btnOpenMdb.setEnabled(false);
                break;
            case R.id.close_mdb:
                scanner2.stop();
                //ledDriver2.setLed(MdbLightModeEnum.MAG_RED, false);
                //scanner.stopScan();
                //ledDriver.setLed(LightModeEnum.BLUE, false);
                //mlog.debug("close mdb!!");
                //GData.getInstance().setTrading(false);
                //closeMdbService();
                //btnOpenMdb.setClickable(true);
                //btnOpenMdb.setEnabled(true);
                break;
            default:
                break;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        mlog.debug("call onStop()");
    }

    @Override
    public void onStop() {
        super.onStop();
        mlog.debug("call onStop()");
    }
}
