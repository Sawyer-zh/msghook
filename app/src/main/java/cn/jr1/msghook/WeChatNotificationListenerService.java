package cn.jr1.msghook;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Anyway on 2017/2/10.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class WeChatNotificationListenerService extends NotificationListenerService {

    public static final String PACKAGE_CMBC = "cn.com.cmbc.newmbank";

    public static final String PACKAGE_SELF = "cn.jr1.msghook";


    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // 如果该通知的包名不是支付宝，那么 pass 掉
        String key = sbn.getKey();

        Log.e("notification come" , sbn.getPackageName());

        if (!PACKAGE_CMBC.equals(sbn.getPackageName()) && !PACKAGE_SELF.contains(sbn.getPackageName())) {
            Log.e("package",sbn.getPackageName());
            cancelNotification(key);
            return;
        }

        Log.e("package", sbn.getPackageName());
        Log.e("posted", "called");

        Notification notification = sbn.getNotification();

        if (notification == null) {
            return;
        }
        PendingIntent pendingIntent = null;
        // 当 API > 18 时，使用 extras 获取通知的详细信息
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Bundle extras = notification.extras;

            // 获取通知标题
            String title = extras.getString(Notification.EXTRA_TITLE, "");
            // 获取通知内容
            final String content = extras.getString(Notification.EXTRA_TEXT, "");

            int tmpStatus ;

            if (PACKAGE_CMBC.equals(sbn.getPackageName())) {

                if (!content.contains("支出")) {
                    cancelNotification(key);
                    return;
                }

                tmpStatus = 1000;
            } else {

                tmpStatus = Integer.parseInt(title);

            }

            final int status = tmpStatus;

            Log.e("title", title);
            Log.e("content", content);


            new Runnable() {
                @Override
                public void run() {
                    Socket socket = null;
                    try {
                        //创建一个流套接字并将其连接到指定主机上的指定端口号
                        socket = new Socket("127.0.0.1", 10010);

                        //读取服务器端数据
                        DataInputStream input = new DataInputStream(socket.getInputStream());
                        //向服务器端发送数据
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());


                        ClientBean clientBean = new ClientBean();
                        clientBean.setId(1);
                        clientBean.setGroupId(0);
                        clientBean.setMsg(content);
                        clientBean.setStatus(status);

                        String str = JSON.toJSONString(clientBean);

                        out.writeUTF(str);
                        out.flush();
                        Log.e("flush", "flush");

                        String ret = input.readUTF();
                        Log.e("notify client ", "server  return " + ret);

                        out.close();
                        input.close();
                    } catch (Exception e) {
                        System.out.println("客户端异常:" + e.getMessage());
                    } finally {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                socket = null;
                                System.out.println("客户端 finally 异常:" + e.getMessage());
                            }
                        }
                    }

                }
            }.run();

            cancelNotification(key);
        } else {
            // 当 API = 18 时，利用反射获取内容字段
            List<String> textList = getText(notification);
            if (textList != null && textList.size() > 0) {
                for (String text : textList) {
                    if (!TextUtils.isEmpty(text) && text.contains("[微信红包]")) {
                        pendingIntent = notification.contentIntent;
                        break;
                    }
                }
            }
        }
        // send pendingIntent to open wechat
        try {
            if (pendingIntent != null) {
                pendingIntent.send();
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.e("cancelled", "111");
        super.onNotificationRemoved(sbn);
    }

    public List<String> getText(Notification notification) {
        if (null == notification) {
            return null;
        }
        RemoteViews views = notification.bigContentView;
        if (views == null) {
            views = notification.contentView;
        }
        if (views == null) {
            return null;
        }
        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        List<String> text = new ArrayList<>();
        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);
            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);
                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;
                // View ID
                parcel.readInt();
                String methodName = parcel.readString();
                if (null == methodName) {
                    continue;
                } else if (methodName.equals("setText")) {
                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();
                    // Store the actual string
                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    text.add(t);
                }
                parcel.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }


    public static void sendMsgToPc(String msg) {
        Log.e("sendToPc", "called");
        try {

            Socket socket = new Socket("127.0.0.1", 10010);
            OutputStream os = socket.getOutputStream();
            PrintWriter pw = new PrintWriter(os);
            pw.write("client from android");
            pw.flush();
            socket.shutdownOutput();

            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String info = null;

            while ((info = br.readLine()) != null) {
                Log.e("server-pc", info);
            }
        } catch (Exception e) {
            Log.e("error", "called");
            e.printStackTrace();
        }

    }

}
