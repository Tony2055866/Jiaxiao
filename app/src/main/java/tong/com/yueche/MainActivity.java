package tong.com.yueche;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.internal.app.ToolbarActionBar;
import android.transition.Explode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {
    static String[] dates = {"上午", "早上", "晚上"};

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MsgCode.CONN_SERVER_ERR){
//                LoginActivity.this.To
                Toast.makeText(getApplicationContext(), "连接服务器错误", Toast.LENGTH_LONG).show();
                failText.setVisibility(View.VISIBLE);
            }else{
                failText.setVisibility(View.INVISIBLE);
                
                if(msg.what == MsgCode.CONN_SERVER_SUCC){
                    Toast.makeText(getApplicationContext(), "初始化成功，请输入验证码", Toast.LENGTH_LONG).show();
                    File imgFile = new  File(HttpUtil.path);
                    if(imgFile.exists()){
                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        image.setImageBitmap(myBitmap);
                        image.setVisibility(View.VISIBLE);
                    }
                }else if(msg.what == MsgCode.LOGIN_ERR){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("登录时出错");
                }else if(msg.what == MsgCode.PWD_ERR){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("用户名或密码错误");
                }else if(msg.what == MsgCode.LOGIN_SUCC){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("登录成功！");
                }else if(msg.what == MsgCode.BOOK_SUCC){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("约车成功");
                    CheckBox box = boxes[msg.arg1/3];
                    String addText = "已约 "+ dates[(msg.arg1%3)];
                    if(!box.getText().toString().contains(addText)){
                        box.setText(box.getText().toString() + " " + addText + " ");
                        box.setTextColor(Color.rgb(0, 200, 0));
                    }
                }else if(msg.what == MsgCode.BOOK_CANEL){
                    //取消约车
                    Toast.makeText(MainActivity.this, "取消约车成功！", Toast.LENGTH_LONG).show();
                    
                }else if(msg.what == MsgCode.BOOKING_STOP){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("已停止");
                }else if(msg.what == MsgCode.BOOKING){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("正在刷新中");
                }else if(msg.what == MsgCode.BOOKING_STOP_NETWORK){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("网络不可用，已停止");
                }else if(msg.what == MsgCode.BOOKING_ERROR_NETWORK){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("网络不可用");
                }else if(msg.what == MsgCode.BOOK_CANEL_CONFIRM){
                    new AlertDialog.Builder(MainActivity.this).setTitle("确认退出吗？")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d("tong test","confirm cancel book");
                                    new Thread(){
                                        @Override
                                        public void run() {
                                            httpUtils.cancelOrder();
                                        }
                                    }.start();
                                }
                            })
                            .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                }
            }
        }
    };
    
    ImageView image;
    Button loginBtn;
    TextView failText;
    EditText codeText;
    static int ids[] = {R.id.checkBox0, R.id.checkBox1,R.id.checkBox2,R.id.checkBox3,R.id.checkBox4,R.id.checkBox5,R.id.checkBox6, R.id.checkBox7};
    String weekDays[] = {"日","一","二","三","四","五","六"};
    public static CheckBox boxes[] = new CheckBox[ids.length];
    HttpUtil httpUtils = new HttpUtil("411326199004122055","19902055", handler);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        
        loginBtn = (Button) findViewById(R.id.button);
        image = (ImageView) findViewById(R.id.image);
        failText = (TextView) findViewById(R.id.failText);
        loginBtn.setOnClickListener(this);
        codeText = (EditText) findViewById(R.id.imageInput);

        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        for(int i=0; i<boxes.length; i++){
            Calendar calendar = Calendar.getInstance();
            boxes[i] = (CheckBox) findViewById(ids[i]);
            int w = HttpUtil.getWeekDay(i);
            calendar.add(Calendar.DATE, i);
            boxes[i].setText("星期" + weekDays[w] + "  " + format1.format(calendar.getTime()));
            /*if(w == 0 || w == 6){
                boxes[i].setChecked(true);
            }else {
                boxes[i].setChecked(false);
            }*/
        }
        new Thread(){
            @Override
            public void run() {
                try {
                    httpUtils.init();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
       
    }
    
    static boolean running = true;

    static boolean isLogin = false;
    static int reLoginCnt = 0;
    static final int NIGHT_SLEEP_TIME = 10000;//服务不可用时睡眠时间
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button){
            //Toast.makeText(getApplicationContext(), "登录到服务器", Toast.LENGTH_LONG).show();
            if(codeText.getText().toString().trim().length() != 4){
                Toast.makeText(getApplicationContext(), "请输入验证码", Toast.LENGTH_LONG).show();
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            httpUtils.init();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
                return;
            }
            running = true;
            new Thread(){
                @Override
                public void run() {
                    try {
                        while(running){

                            isLogin = httpUtils.checkisLogin();
                            Log.i("tongtest","is Login:" + isLogin);
                            if(!isNetworkAvailable(MainActivity.this)){
                                handler.sendEmptyMessage(MsgCode.BOOKING_ERROR_NETWORK);
                                return;
                            }
                            
                            if(!isLogin){
                                isLogin =  httpUtils.login( codeText.getText().toString() );
                                reLoginCnt++;
                                Thread.sleep(3 * 1000); //停止10秒 再登录
                                if(!isLogin && reLoginCnt >= 5 && isNetworkAvailable(MainActivity.this)) {
                                    failText.setText("登录多次失败, 程序已停止");
                                    break;
                                }
                                if(isLogin){
                                    loginBtn.setClickable(false);
                                }else{
                                    loginBtn.setClickable(true);
                                }
                                
                            }
                            if(isLogin){
                                
                                handler.sendEmptyMessage(MsgCode.BOOKING);
                                reLoginCnt = 0;
                                httpUtils.order();
                                isLogin = HttpUtil.orderNormal; //预定不正常，则任务是登录超时了
                                if(isLogin){
                                    loginBtn.setClickable(false);
                                }else{
                                    loginBtn.setClickable(true);
                                }
                                Thread.sleep(45 * 1000); //停止45秒 再刷新
                            }
                            
                        }
                        //while循环停止，就是停止刷新
                        handler.sendEmptyMessage(MsgCode.BOOKING_STOP);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }

    public void cancelClick(View v){
        new Thread(){
            @Override
            public void run() {
                String html  = httpUtils.getPage("http://haijia.bjxueche.net/NetBooking.aspx");
                if(html != null && html.contains("取消网上预约")){
                    Log.d("tongtest","has booked. cancel book");
                    handler.sendEmptyMessage(MsgCode.BOOK_CANEL_CONFIRM);
                    
                }else{
                   Log.d("tongtest","no need cancel");
                    Toast.makeText(MainActivity.this, "你还没有约车", Toast.LENGTH_LONG).show();
                }
            }
        }.start();
    }

    public void stopClick(View v){
        if(running){
            running = false;
            failText.setText("正在停止刷新");
            Button button = (Button)v;
        }
    }
    public void weekClick(View v){
        
    }

    /**
     * 检测当的网络（WLAN、3G/2G）状态
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected())
            {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED)
                {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        Intent i= new Intent(Intent.ACTION_MAIN);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }
}
