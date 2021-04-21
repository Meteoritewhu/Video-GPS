package com.example.android.camera2video;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    //GPS实时获取
    public LocationClient mLocationClient;
    private TextView positionText;
    private TextView timeView;
    //GPS实时获取结束

    //视频采集
    public static final int TAKE_PHOTO = 1;
    private VideoView picture;
    private Uri imageUri;
    //视频采集结束

    //获取当前时间
    private Timer timer;
    public static final int UPDATE_TEXT = 2;
    private Handler handler;
    //获取当前时间结束

    //数据列表
    public StringBuilder dataList;

    public String filename;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //添加活动进列表
        ActivityCollector.addActivity(this);

        //数据列表
        dataList = new StringBuilder();

        //当前文件名
        final EditText nameEdit = (EditText)findViewById(R.id.save_name_edit);

        //获取当前时间
        timer=new Timer();
        timeView = (TextView)findViewById(R.id.time_text_view);
        //获取当前时间结束

        //GPS定位开始
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        positionText = (TextView)findViewById(R.id.position_text_view);
        //GPS定位结束


        //录制视频开始
        Button takePhoto = (Button)findViewById(R.id.button_start);
        picture = (VideoView)findViewById(R.id.video_view);
        takePhoto.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                filename = nameEdit.getText().toString();

                save("--------------------记录开始--------------------"+"\n", nameEdit.getText().toString());
                //创建File对象，用于存储录制后的视频
                File outputImage = new File(getExternalCacheDir(),nameEdit.getText().toString() +".mp4" );
                try{
                    if(outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(Build.VERSION.SDK_INT>=24){
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.camera.fileprovider", outputImage);
                }
                else {
                    imageUri = Uri.fromFile(outputImage);
                }

                //启动相机程序
//                Intent intent = new Intent("android.media.action.VIDEO_CAPTURE");
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//                startActivityForResult(intent, TAKE_PHOTO);
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                intent.putExtra("file_name", nameEdit.getText().toString());
                startActivity(intent);

                //GPS实时获取
                List<String> permissionList = new ArrayList<>();
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED){
                    permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED){
                    permissionList.add(Manifest.permission.READ_PHONE_STATE);
                }
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
                    permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
                if(!permissionList.isEmpty()){
                    String[] permissions = permissionList.toArray(new String[permissionList.size()]);
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
                }
                else{
                    requestLocation();
                    //GPS实时获取结束
                }

                //获取当前时间开始 0.01秒间隔
                TimerTask task = new TimerTask() {
                    public void run() {
                        Message message = new Message();
                        message.what = UPDATE_TEXT;
                        handler.sendMessage(message);
                    }
                };
                handler = new Handler(){
                    public void handleMessage(Message msg){
                        switch (msg.what){
                            case UPDATE_TEXT:
                                //获取当前时间开始
                                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                                Date curDate =  new Date(System.currentTimeMillis());
                                String str = formatter.format(curDate);
                                timeView.setText(str);
                                StringBuilder currentTime = new StringBuilder();
                                currentTime.append("当前系统时间:").append(str);
                                //获取当前时间结束
                                //数据列表
                                dataList.append(currentTime.toString()).append("\n").append(positionText.getText()).append("\n");

                        }
                    }
                };
                timer.schedule(task, 0, 10);
                //获取当前时间结束
            }
            //录制视频结束
        });
        //结束当前系统时间的记录

        //结束视频和GPS的采集
        Button endTime = (Button)findViewById(R.id.button_end);
        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer.cancel();
                save(dataList.toString(), nameEdit.getText().toString());

            }
        });

        //记录道路方位信息
        Button saveButton = (Button)findViewById(R.id.save);
        final EditText rightOrleft = (EditText)findViewById(R.id.edit_text);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save("-------------此次数据采集在道路哪一侧-------------"+"\n", nameEdit.getText().toString());
                save(rightOrleft.getText().toString()+"\n", nameEdit.getText().toString());
                save("--------------------记录结束--------------------"+"\n", nameEdit.getText().toString());
            }
        });

        //重新调用本活动
        Button restartButton = (Button)findViewById(R.id.restart_button);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        //退出活动
        Button finishButton = (Button)findViewById(R.id.finish_all);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCollector.finishAll();
            }
        });
    }
    //GPS实时获取
    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }
    //GPS实时获取
    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        //每0.01秒种更新一下位置
        option.setScanSpan(1000);
        //强制只使用GPS定位
        //option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }
    //GPS实时获取
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    for(int result:grantResults){
                        if(result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this, "必须同意所有权限才能实用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }
                else{
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }
    //GPS实时获取
    public class MyLocationListener implements BDLocationListener{

        @Override
        public void onReceiveLocation(final BDLocation bdLocation) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder currentPosition = new StringBuilder();
                    currentPosition.append("纬度:").append(bdLocation.getLatitude()).append("\n");
                    currentPosition.append("经度:").append(bdLocation.getLongitude()).append("\n");
                    //以下信息需要网络定位
//                    currentPosition.append("国家:").append(bdLocation.getCountry()).append("\n");
//                    currentPosition.append("省:").append(bdLocation.getProvince()).append("\n");
//                    currentPosition.append("市:").append(bdLocation.getCity()).append("\n");
//                    currentPosition.append("区:").append(bdLocation.getDistrict()).append("\n");
//                    currentPosition.append("街道:").append(bdLocation.getStreet()).append("\n");
                    currentPosition.append("定位方式:");
                    if(bdLocation.getLocType() == BDLocation.TypeGpsLocation){
                        currentPosition.append("GPS");
                    }
                    else if(bdLocation.getLocType() == BDLocation.TypeNetWorkLocation){
                        currentPosition.append("网络");
                    }
                    positionText.setText(currentPosition);
                }
            });
        }
    }
    //视频录制
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch(requestCode){
            case TAKE_PHOTO:
                if(resultCode == RESULT_OK){
                    //显示拍摄的视频
                    picture.setVideoURI(imageUri);
                    MediaController mediaController=new MediaController(this);
                    picture.setMediaController(mediaController);
                    mediaController.setMediaPlayer(picture);
                }
                break;
            default:
                break;
        }
    }

    //存储系统时间和GPS和道路左右侧的信息
    public void save(String text, String name){
        //FileOutputStream out = null;
//        BufferedWriter writer = null;
        try{
            File f = new File(getExternalFilesDir(null)  + name + "_GPS.txt");
            if(f.exists()){
                FileOutputStream fos = new FileOutputStream(f,true);
                fos.write(text.getBytes());
                fos.close();
            }
            else{
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(text.getBytes());
                fos.close();
            }
//            out = openFileOutput(name +".txt", Context.MODE_APPEND);
//            writer = new BufferedWriter(new OutputStreamWriter(out));
//            writer.write(text);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //GPS实时获取
        mLocationClient.stop();
        //GPS实时获取结束

        //从活动列表删除此活动
        ActivityCollector.removeActivity(this);
    }
}
