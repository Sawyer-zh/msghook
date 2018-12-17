package cn.jr1.msghook;

import android.util.Log;


import com.alibaba.fastjson.JSON;
import com.baidu.aip.ocr.AipOcr;


import org.json.JSONObject;

import java.util.HashMap;

import static cn.jr1.msghook.MainActivity.SCREENSHOT;

public class Ocr {
    //设置APPID/AK/SK
    public static final String APP_ID = "15167836";
    public static final String API_KEY = "uTeP0ZbNkO88Tw5LbPrb7sVG";
    public static final String SECRET_KEY = "quug2GpzmU6pWYPAHu8DURvGQXhgvaKd";

    public static String getAmount(HashMap<String , String> hashMap){
        // 初始化一个AipOcr
        AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
//        client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
//        client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理

        // 可选：设置log4j日志输出格式，若不设置，则使用默认配置
        // 也可以直接通过jvm启动参数设置此环境变量
//        System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");

        // 调用接口
        String path = SCREENSHOT;
        JSONObject res = client.basicGeneral(path, hashMap);
        try{

            OcrBean orc = JSON.parseObject(res.toString() ,OcrBean.class );

            String result = orc.getWords_result().get(0).getWords();


            System.out.println(res.toString(2));

            Log.e("word_ret" , result);

            return result;
        }catch (Exception e){
            return "";
        }

    }
}



