package tong.com.yueche;


        import java.io.ByteArrayOutputStream;
        import java.io.File;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.UnsupportedEncodingException;
        import java.net.MalformedURLException;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Calendar;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.Random;

        import org.apache.commons.lang.StringEscapeUtils;
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
        import org.apache.http.client.params.ClientPNames;
        import org.apache.http.client.protocol.ClientContext;
        import org.apache.http.entity.StringEntity;
        import org.apache.http.impl.client.BasicCookieStore;
        import org.apache.http.impl.client.DefaultHttpClient;
        import org.apache.http.message.BasicNameValuePair;
        import org.apache.http.protocol.BasicHttpContext;
        import org.apache.http.protocol.HTTP;
        import org.apache.http.protocol.HttpContext;
        import org.apache.http.util.EntityUtils;

        import android.net.http.AndroidHttpClient;
        import android.os.Environment;
        import android.os.Handler;
        import android.os.Message;
        import android.util.Log;

public class HttpUtil {

    private String username;
    private String password;
    
    public static String domain = "http://haijia.bjxueche.net/";

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
            Log.d("tongtest","getPage code:" +response.getStatusLine().getStatusCode() );
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
        client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, false);
        
        String html = getPage(domain);
        if(html == null){
            Log.w("tongtest","init fail html is null!");
            handler.sendEmptyMessage(MsgCode.CONN_SERVER_ERR);
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
            if( j==1) continue; //已经废弃
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
                handler.sendEmptyMessage(MsgCode.CONN_SERVER_SUCC);
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
            
            
            params.put("__EVENTVALIDATION", STATEVALUES[2]);
            params.put("BtnLogin","登  录");

            String result = sendHttpClientPost(domain, params, "utf-8");
            
            //Log.i("tongtest","login html result:" + result);
            if(result == null || result == ""){
                Log.i("tongtest","login html result:" + result);
                handler.sendEmptyMessage(MsgCode.LOGIN_ERR);
            }else if(result.contains("账号或密码错误")){
                Log.i("tongtest","login html result: 账号或密码错误");
                handler.sendEmptyMessage(MsgCode.PWD_ERR);
            }else if(result.equals("true") || result.contains("ych2.aspx")){
                Log.i("tong test","long reslt html:\n" + result);
                handler.sendEmptyMessage(MsgCode.LOGIN_SUCC);
               return true;
            }else{
                handler.sendEmptyMessage(MsgCode.LOGIN_ERR);
                Log.i("tongtest","login html result: " + result);
            }
        }catch (Exception e){
            e.printStackTrace();
            return  false;
        }
        return false;
    }
    
    public static boolean orderNormal = true;
    public static int failCnt = 0;
    
    public static int getWeekDay(int i){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, i);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        return w;
    }

    static String hiddenKM = "1";
    public boolean order( ) {
        orderNormal = true;
        String res = getPage(HttpUtil.domain + "/ych2.aspx");
        if(res == null || res == ""){
            MainActivity.isLogin = false;
            Log.w("tongtest","Log out, need relogin!!!");
            orderNormal = false;
            return false;
        }
        
        String hidden =  StringUtils.substringBetween(res,"id=\"hiddenKM\" value=\"","\"");
        if(hidden != null && hidden.length() == 1) hiddenKM = hidden;
        if(hidden == null){
            Log.i("tongtest","getPage ych2 result:" + res);
        }
        String allRess[] = StringUtils.substringsBetween(res,"class=\"CellCar\"","</td>");
        boolean isYue = false;
        for(int i=0; i<24; i++){
            int w = getWeekDay(i/3);
            if(allRess[i].contains("已约")){
                Message msg = handler.obtainMessage();
                msg.what = MsgCode.BOOK_SUCC;
                msg.arg1 = i;
                handler.sendMessage(msg);
                isYue = true;
                break;
            }
            else if(!allRess[i].contains("无")){
                Log.i("tongtest", "week day : " + w + "- "+ i%3 +" 有车");
                 
                if(MainActivity.boxes[i/3].isChecked()){
                    //进行约车
                   orderCar(i);
                }
            }else {
                //Log.i("tongtest", "week day : " + w + "- "+ i%3 +" 无车");
            }
        }
        return false;
    }

    static String yysds[] = {"812", "15", "58"};
    private boolean orderCar(int i) {
        
        /*if(true){
            Log.i("tongtest","start order car:" + i);
            return true;
        }*/
        String getCarJson = "{\"yyrq\":\"ORDERDATE\",\"yysd\":\""+ yysds[i%3] +"\",\"xllxID\":\"" +hiddenKM+ "\",\"pageSize\":35,\"pageNum\":1}";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, i/3);
        SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMdd");
        String orderDateStr = format1.format(c.getTime());
        getCarJson = getCarJson.replace("ORDERDATE",orderDateStr );
        
    
        String carsResult =  sendJsonPost(getCarJson, domain + "/Han/ServiceBooking.asmx/GetCars");
        
        if(carsResult == null ){
            Log.e("tongtest", "GetCars error!!!  /Han/ServiceBooking.asmx/GetCars , response null");
            orderNormal = false;
            return false;
        }
        carsResult = StringEscapeUtils.unescapeJava(carsResult);
        //Log.d("tongtest", "carsResult: " + carsResult);
        int index = -1;
        String xnsd = "";
        String cnbh = "";
        if( (index=carsResult.indexOf("\"XNSD\"")) != -1){
            carsResult = carsResult.substring(index + "\"XNSD\"".length());
            xnsd = StringUtils.substringBetween(carsResult, "\"", "\"");

            index=carsResult.indexOf("\"CNBH\"");
            carsResult = carsResult.substring(index + "\"CNBH\"".length());

            cnbh = StringUtils.substringBetween(carsResult, "\"", "\"");
            
        }else{
            orderNormal = false;
            //
            Log.w("tongtest", " GetCars error!!!  /Han/ServiceBooking.asmx/GetCars , response:" + carsResult );
            return false;
        }


        String bookCarJson = "{\"yyrq\":\""+orderDateStr+"\",\"xnsd\":\""+xnsd+"\",\"cnbh\":\""+cnbh+"\",\"imgCode\":\"\",\"KMID\":\"" +hiddenKM+"\"}";
        Log.i("tongtest","find car and start Book car!!! bookJson:" + bookCarJson);
        String bookResult =  sendJsonPost(bookCarJson, domain + "/Han/ServiceBooking.asmx/BookingCar");
        if(bookResult != null && bookResult.contains("Result\\\": true")){
            Log.i("tongtest", "boolResult:" + bookResult);
            //约车成功！
            Log.i("tongtest", "约车成功!!");
            Message msg = handler.obtainMessage();
            msg.what = 5;
            msg.arg1 = i;
            handler.sendMessage(msg);
        }else{
            orderNormal = false;
            Log.w("tongtest", " book car error!!! /Han/ServiceBooking.asmx/BookingCar , response:" + bookResult );
            return false;
        }
        orderNormal = true;
        if(orderNormal == false){
            failCnt ++;
        }else{
            failCnt = 0;
        }
        return false;
    }
    
    public boolean cancelOrder(){
        Map<String, String> params = new HashMap<String, String>();
        params.put("txtPassword", password);
        params.put("__VIEWSTATE", STATEVALUES[0]);
        
        String result = sendHttpClientPost2("http://haijia.bjxueche.net/NetBooking.aspx", params, "UTF-8");
        if(result != null && result.contains("成功取消")){
           handler.sendEmptyMessage(MsgCode.BOOK_CANEL); 
        }else{
            Log.d("tongtest","cancelOrder result:" + result);
        }
        return true;
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
    
    
    private  String sendJsonPost(String json, String path) {
        try {
            HttpPost httpPost = new HttpPost(path);
            StringEntity entity = new StringEntity(json, HTTP.UTF_8);
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
            HttpResponse response = client.execute(httpPost, httpContext);

            if(response.getStatusLine().getStatusCode()==200)
            {
                InputStream inputStream=response.getEntity().getContent();
                return changeInputStream(inputStream,"UTF-8");
            }else{
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
                String res = changeInputStream(inputStream,encode);
                httpResponse.getEntity().consumeContent();
                return res;
            }else if(httpResponse.getStatusLine().getStatusCode()==302){
                Log.i("tongtest", "Login success! : " + httpResponse.getHeaders("Location"));
                httpResponse.getEntity().consumeContent();
                return "true";
            }else{
                Log.i("tongtest","login code:" + httpResponse.getStatusLine().getStatusCode());
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    private String sendHttpClientPost2(String path,Map<String, String> map,String encode)
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
            Log.i("tongtest", "sendHttpClientPost2 getStatusCode : " + httpResponse.getStatusLine().getStatusCode());
            //if(httpResponse.getStatusLine().getStatusCode()==200)
            {
                InputStream inputStream=httpResponse.getEntity().getContent();
                String res = changeInputStream(inputStream,encode);
                httpResponse.getEntity().consumeContent();
                return res;
            }
           

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    public boolean checkisLogin() {
       String res =  getPage("http://haijia.bjxueche.net/index.aspx");
        if(res == null || res == "") return false;
        if(res.contains("ych2.aspx")) return true;
        return false;
    }
}
