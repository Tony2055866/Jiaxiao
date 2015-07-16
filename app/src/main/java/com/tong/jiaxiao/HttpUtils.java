package com.tong.jiaxiao;

/**
 * Created by gaotong1 on 2015/7/14.
 */

        import java.io.ByteArrayOutputStream;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.UnsupportedEncodingException;
        import java.util.ArrayList;
        import java.util.Calendar;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;

        import org.apache.commons.lang.StringUtils;
        import org.apache.http.HttpResponse;
        import org.apache.http.NameValuePair;
        import org.apache.http.client.ClientProtocolException;
        import org.apache.http.client.CookieStore;
        import org.apache.http.client.HttpClient;
        import org.apache.http.client.entity.UrlEncodedFormEntity;
        import org.apache.http.client.methods.HttpGet;
        import org.apache.http.client.methods.HttpPost;
        import org.apache.http.client.protocol.ClientContext;
        import org.apache.http.impl.client.BasicCookieStore;
        import org.apache.http.impl.client.DefaultHttpClient;
        import org.apache.http.message.BasicNameValuePair;
        import org.apache.http.protocol.BasicHttpContext;
        import org.apache.http.protocol.HttpContext;
        import org.apache.http.util.EntityUtils;

        import android.net.http.AndroidHttpClient;
        import android.os.Handler;
        import android.util.Log;

public class HttpUtils {

    private String username;
    private String password;

    private String __VIEWSTATE, __VIEWSTATEGENERATOR, __EVENTVALIDATION, txtIMGCode;

    HttpContext httpContext = null;
    CookieStore cookieStore = null;
    HttpClient client = null;

    Handler handler = null;

    public HttpUtils(String username, String password, Handler handler) {
        // 初始化用户名和密码
        this.username = username;
        this.password = password;
        this.handler = handler;
    }

    public String getPage(String url){
        HttpGet httpget=new HttpGet(url);
        //建立HttpPost对象
        HttpResponse response = null;
        try {
            response = client.execute(httpget, httpContext);
            //发送GET,并返回一个HttpResponse对象，相对于POST，省去了添加NameValuePair数组作参数
            if (response.getStatusLine().getStatusCode() == 200) {//如果状态码为200,就是正常返回
                String result = EntityUtils.toString(response.getEntity());
                //得到返回的字符串
               return  result;
            }else{
                return "";
            }
        }catch (Exception e){
            return null;
        }
    }

    public void init() {

        httpContext = new BasicHttpContext();
        cookieStore = new BasicCookieStore();
        client = new DefaultHttpClient();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        String url="http://haijia.bjxueche.net/";
        String html = getPage(url);
        if(html == null){
            Log.w("tongtest","init fail!");
            handler.sendEmptyMessage(0);
        }else{
            initValue(html);
        }
    }

    String STATEVALUES[] = new String[3];
    public void initValue(String html){

        String vfinds[] = {"id=\"__VIEWSTATE\" value=\"", "id=\"__VIEWSTATEGENERATOR\" value=\"", "id=\"__EVENTVALIDATION\" value=\""};
        for(int j=0; j<3; j++){
            int i = html.indexOf(vfinds[j]);
            if(i != -1){
                html = html.substring(i + vfinds[j].length());
                int last = html.indexOf("\"");
                if(last != -1){
                    STATEVALUES[j] = html.substring(0, last);
                }
            }
        }
        Log.i("tongtest","statvalue1:" + STATEVALUES[0] + "   statvalue3:" + STATEVALUES[2]);
    }

    public void login()  {
        try {
            // 设置访问的Web站点
            String path = "http://192.168.1.103:1231/loginas.ashx";
            //设置Http请求参数
            Map<String, String> params = new HashMap<String, String>();
            params.put("username", username);
            params.put("password", password);

            params.put("BtnLogin","登  录");

            String result = sendHttpClientPost(path, params, "utf-8");
            //把返回的接口输出
            Log.i("tongtest","login html result:" + result);

            if(result.equals("true")){
                //登陆成功
                String res = getPage("http://haijia.bjxueche.net/ych2.aspx");
                if(res != null){
                    order(res);
                }else{
                    init();
                    login();
                }
                Thread.sleep(180 * 1000);
            }else{
                Thread.sleep(100*1000);
                init();
                login();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void order(String res) {
        String allRess[] = StringUtils.substringsBetween(res,"class=\"CellCar\"","</td>");
        Calendar cal = Calendar.getInstance();

        for(int i=0; i<24; i++){
            cal.add(Calendar.DATE, i);
            int w = cal.get(Calendar.DAY_OF_WEEK) - 1;

            if(!allRess[i].contains("无")){
                //有可预订
                Log.i("tongtest", "week day : " + w + "- "+ i%3 +"午， 有车！");

            }else{
                Log.i("tongtest", "week day : " + w + "- "+ i%3 +"午， 无车！");
            }
        }
    }

    /**
     * 发送Http请求到Web站点
     * @param path Web站点请求地址
     * @param map Http请求参数
     * @param encode 编码格式
     * @return Web站点响应的字符串
     */
    private String sendHttpClientPost(String path,Map<String, String> map,String encode)
    {
        List<NameValuePair> list=new ArrayList<NameValuePair>();
        if(map!=null&&!map.isEmpty())
        {
            for(Map.Entry<String, String> entry:map.entrySet())
            {
                //解析Map传递的参数，使用一个键值对对象BasicNameValuePair保存。
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        try {
            //实现将请求 的参数封装封装到HttpEntity中。
            UrlEncodedFormEntity entity=new UrlEncodedFormEntity(list, encode);
            //使用HttpPost请求方式
            HttpPost httpPost=new HttpPost(path);
            //设置请求参数到Form中。
            httpPost.setEntity(entity);
            //实例化一个默认的Http客户端，使用的是AndroidHttpClient
            HttpClient client=AndroidHttpClient.newInstance("");
            //执行请求，并获得响应数据
            HttpResponse httpResponse= client.execute(httpPost);
            //判断是否请求成功，为200时表示成功，其他均问有问题。
            if(httpResponse.getStatusLine().getStatusCode()==200)
            {
                //通过HttpEntity获得响应流
                InputStream inputStream=httpResponse.getEntity().getContent();
                return changeInputStream(inputStream,encode);
            }else if(httpResponse.getStatusLine().getStatusCode()==302){
                Log.i("tongtest", "Login success! : " + httpResponse.getHeaders("Location"));
                return "true";
            }

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return "";
    }
    /**
     * 把Web站点返回的响应流转换为字符串格式
     * @param inputStream 响应流
     * @param encode 编码格式
     * @return 转换后的字符串
     */
    private  String changeInputStream(InputStream inputStream,
                                      String encode) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        String result="";
        if (inputStream != null) {
            try {
                while ((len = inputStream.read(data)) != -1) {
                    outputStream.write(data,0,len);
                }
                result=new String(outputStream.toByteArray(),encode);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
