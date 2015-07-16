package tong.com.yueche;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 0){
//                LoginActivity.this.To
                Toast.makeText(getApplicationContext(), "连接服务器错误", Toast.LENGTH_LONG).show();
                failText.setVisibility(View.VISIBLE);
            }else{
                failText.setVisibility(View.INVISIBLE);
                
                if(msg.what == 1){
                    Toast.makeText(getApplicationContext(), "初始化成功，请输入验证码", Toast.LENGTH_LONG).show();
                    File imgFile = new  File(HttpUtil.path);
                    if(imgFile.exists()){
                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        image.setImageBitmap(myBitmap);
                        image.setVisibility(View.VISIBLE);
                    }
                }else if(msg.what == 2){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("登录时出错");
                }else if(msg.what == 3){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("用户名或密码错误");
                }else if(msg.what == 4){
                    failText.setVisibility(View.VISIBLE);
                    failText.setText("登录成功！");
                }
            }
        }
    };
    
    ImageView image;
    Button loginBtn;
    TextView failText;
    EditText codeText;
    
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
            
            new Thread(){
                @Override
                public void run() {
                    try {
                        while(running){
                            boolean isLogin =  httpUtils.login( codeText.getText().toString() );
                            if(isLogin){
                                String res = httpUtils.getPage(HttpUtil.domain + "/ych2.aspx");
                                boolean yue = httpUtils.order(res);
                            }else{
                                Thread.sleep(300 * 1000);
                            }
                        }
                        Thread.sleep(180 * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
