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
        // ��ʼ���û���������
        this.username = username;
        this.password = password;
        this.handler = handler;
    }

    public String getPage(String url){
        HttpGet httpget=new HttpGet(url);
        //����HttpPost����
        HttpResponse response = null;
        try {
            response = client.execute(httpget, httpContext);
            //����GET,������һ��HttpResponse���������POST��ʡȥ�����NameValuePair����������
            if (response.getStatusLine().getStatusCode() == 200) {//���״̬��Ϊ200,������������
                String result = EntityUtils.toString(response.getEntity());
                //�õ����ص��ַ���
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
            // ���÷��ʵ�Webվ��
            String path = "http://192.168.1.103:1231/loginas.ashx";
            //����Http�������
            Map<String, String> params = new HashMap<String, String>();
            params.put("username", username);
            params.put("password", password);

            params.put("BtnLogin","��  ¼");

            String result = sendHttpClientPost(path, params, "utf-8");
            //�ѷ��صĽӿ����
            Log.i("tongtest","login html result:" + result);

            if(result.equals("true")){
                //��½�ɹ�
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

            if(!allRess[i].contains("��")){
                //�п�Ԥ��
                Log.i("tongtest", "week day : " + w + "- "+ i%3 +"�磬 �г���");

            }else{
                Log.i("tongtest", "week day : " + w + "- "+ i%3 +"�磬 �޳���");
            }
        }
    }

    /**
     * ����Http����Webվ��
     * @param path Webվ�������ַ
     * @param map Http�������
     * @param encode �����ʽ
     * @return Webվ����Ӧ���ַ���
     */
    private String sendHttpClientPost(String path,Map<String, String> map,String encode)
    {
        List<NameValuePair> list=new ArrayList<NameValuePair>();
        if(map!=null&&!map.isEmpty())
        {
            for(Map.Entry<String, String> entry:map.entrySet())
            {
                //����Map���ݵĲ�����ʹ��һ����ֵ�Զ���BasicNameValuePair���档
                list.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        try {
            //ʵ�ֽ����� �Ĳ�����װ��װ��HttpEntity�С�
            UrlEncodedFormEntity entity=new UrlEncodedFormEntity(list, encode);
            //ʹ��HttpPost����ʽ
            HttpPost httpPost=new HttpPost(path);
            //�������������Form�С�
            httpPost.setEntity(entity);
            //ʵ����һ��Ĭ�ϵ�Http�ͻ��ˣ�ʹ�õ���AndroidHttpClient
            HttpClient client=AndroidHttpClient.newInstance("");
            //ִ�����󣬲������Ӧ����
            HttpResponse httpResponse= client.execute(httpPost);
            //�ж��Ƿ�����ɹ���Ϊ200ʱ��ʾ�ɹ����������������⡣
            if(httpResponse.getStatusLine().getStatusCode()==200)
            {
                //ͨ��HttpEntity�����Ӧ��
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
     * ��Webվ�㷵�ص���Ӧ��ת��Ϊ�ַ�����ʽ
     * @param inputStream ��Ӧ��
     * @param encode �����ʽ
     * @return ת������ַ���
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
