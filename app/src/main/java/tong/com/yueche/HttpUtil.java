package tong.com.yueche;


        import java.io.ByteArrayOutputStream;
        import java.io.File;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.UnsupportedEncodingException;
        import java.net.MalformedURLException;
        import java.util.ArrayList;
        import java.util.Calendar;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.Random;

        import org.apache.commons.lang.StringUtils;
        import org.apache.http.Header;
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
        import android.os.Environment;
        import android.os.Handler;
        import android.util.Log;

public class HttpUtil {

    private String username;
    private String password;
    
    public static String domain = "http://shenghua.bjxueche.net";

    private String __VIEWSTATE, __VIEWSTATEGENERATOR, __EVENTVALIDATION, txtIMGCode;

    HttpContext httpContext = null;
    CookieStore cookieStore = null;
    HttpClient client = null;

    Handler handler = null;
    

    public HttpUtil(String username, String password, Handler handler) {
        this.username = username;
        this.password = password;
        this.handler = handler;
    }

    public String getPage(String url){
        HttpGet httpget=new HttpGet(url);
        HttpResponse response = null;
        try {
            response = client.execute(httpget, httpContext);
            if (response.getStatusLine().getStatusCode() == 200) {
                String result = EntityUtils.toString(response.getEntity());
                return  result;
            }else{
                return "";
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    

    
    public boolean init() {

        httpContext = new BasicHttpContext();
        cookieStore = new BasicCookieStore();
        client = new DefaultHttpClient();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        HttpGet httpGet = new HttpGet();

        httpGet.setHeader("Host", domain);
        httpGet.setHeader("Referer", domain);
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.110 Safari/537.36 CoolNovo/2.0.9.19");
        httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        
        String html = getPage(domain);
        if(html == null){
            Log.w("tongtest","init fail html is null!");
            handler.sendEmptyMessage(0);
            return false;
        }else{
            Log.i("tongtest", "init ,get html success");
            return initValue(html);
            
        }
    }

    Random r = new Random();
    String STATEVALUES[] = new String[3];
    public static String path;
    public boolean initValue(String html){

        String vfinds[] = {"id=\"__VIEWSTATE\" value=\"", "id=\"__VIEWSTATEGENERATOR\" value=\"", "id=\"__EVENTVALIDATION\" value=\""};
        
        for(int j=0; j<3; j++){
            if(domain.contains("sheng") && j==1) continue;
            int i = html.indexOf(vfinds[j]);
            if(i != -1){
                Log.i("tongtest","find view value:" + vfinds[j] );

                html = html.substring(i + vfinds[j].length());
                int last = html.indexOf("\"");
                if(last != -1){
                    STATEVALUES[j] = html.substring(0, last);
                }else{
                    Log.i("tongtest","find " + vfinds[j] + "   error !" );
                    return  false;
                }
            }else{
                Log.i("tongtest","find " + vfinds[j] + "   error !" );
                return false;
            }
        }
        Log.i("tongtest","statvalue1:" + STATEVALUES[0] + "   statvalue3:" + STATEVALUES[2]);

        
        String iamgeUrl = domain +  "/tools/CreateCode.ashx?key=ImgCode&random=" + r.nextDouble();
         path= Environment.getExternalStorageDirectory()+"/yueche/imgCode.jpg" ;
        
        HttpGet get = new HttpGet(iamgeUrl);
        try {
            HttpResponse response = client.execute(get, httpContext);
            boolean downed = download(response.getEntity().getContent(), path);
            if(downed){
                handler.sendEmptyMessage(1);
            }else{
                Log.w("tongtest","download faidled");
            }
           return  downed;
        } catch (IOException e) {
            e.printStackTrace();
           
        }
        return false;
    }

    public boolean login(String txtIMGCode)  {
        try {
            Log.i("tongtest","starg login:" + txtIMGCode);
            
            Map<String, String> params = new HashMap<String, String>();
            params.put("txtUserName", username);
            params.put("txtPassword", password);
            params.put("txtIMGCode",txtIMGCode);
            params.put("__VIEWSTATE", STATEVALUES[0]);
            
            if(!domain.contains("hai"))
                params.put("__VIEWSTATEGENERATOR", STATEVALUES[1]);
            
            params.put("__EVENTVALIDATION", STATEVALUES[2]);
            params.put("BtnLogin","登  录");

            String result = sendHttpClientPost(domain, params, "utf-8");
            
            //Log.i("tongtest","login html result:" + result);
            if(result == null || result == ""){
                Log.i("tongtest","login html result:" + result);
                handler.sendEmptyMessage(2);
            }else if(result.contains("账号或密码错误")){
                Log.i("tongtest","login html result: 账号或密码错误");
                handler.sendEmptyMessage(3);
            }else if(result.equals("true")){
                handler.sendEmptyMessage(4);
               return true;
            }else{
                handler.sendEmptyMessage(2);
                Log.i("tongtest","login html result: " + result);
            }
        }catch (Exception e){
            e.printStackTrace();
            return  false;
        }
        return false;

    }

    public boolean order(String res) {
        String allRess[] = StringUtils.substringsBetween(res,"class=\"CellCar\"","</td>");
        Calendar cal = Calendar.getInstance();

        for(int i=0; i<24; i++){
            cal.add(Calendar.DATE, i);
            int w = cal.get(Calendar.DAY_OF_WEEK) - 1;

            if(!allRess[i].contains("无")){
                Log.i("tongtest", "week day : " + w + "- "+ i%3 +" 有车");

            }else{
                Log.i("tongtest", "week day : " + w + "- "+ i%3 +" 无车");
            }
        }
        return false;
        
    }

    public static boolean download(InputStream in, String path) throws IOException {
        FileOutputStream out = null;
        File f = new File(path);
        if(!f.exists())
        f.createNewFile();
            
        try
        {
            out = new FileOutputStream(path);
            byte b[] = new byte[1024];
            int j = 0;
            while ((j = in.read(b)) != -1)
            {
                out.write(b, 0, j);
            }
            out.flush();
            File file = new File(path);
            if(file.exists() && file.length() == 0)
                return false;
            return true;
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
             e.printStackTrace();
        } finally{

            if(out != null)
                try
                {
                    out.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            if(in != null)
                try
                {
                    in.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
        }
        return false;
    }


    private String sendHttpClientPost(String path,Map<String, String> map,String encode)
    {
        List<NameValuePair> list=new ArrayList<NameValuePair>();
        if(map!=null&&!map.isEmpty())
        {
            for(Map.Entry<String, String> entry:map.entrySet())
            {
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        try {
            UrlEncodedFormEntity entity=new UrlEncodedFormEntity(list, encode);
            HttpPost httpPost=new HttpPost(path);
            httpPost.setEntity(entity);
            HttpResponse httpResponse= client.execute(httpPost, httpContext);
            Log.i("tongtest", "Login getStatusCode : " + httpResponse.getStatusLine().getStatusCode());
            if(httpResponse.getStatusLine().getStatusCode()==200)
            {
                InputStream inputStream=httpResponse.getEntity().getContent();
                return changeInputStream(inputStream,encode);
            }else if(httpResponse.getStatusLine().getStatusCode()==302){
                Log.i("tongtest", "Login success! : " + httpResponse.getHeaders("Location"));
                return "true";
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

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
