package cn.jr1.msghook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Semaphore;


public class MainActivity extends AppCompatActivity {

    ArrayList<ClientBean> msgList = new ArrayList<>();

    public static Semaphore semaCommand = new Semaphore(1);

    public static Semaphore semaNotify = new Semaphore(0);

    public static Semaphore semaScreenRead = new Semaphore(0);

    public static Semaphore semaScreenCap = new Semaphore(1);

    public static final String LOCATION = "/sdcard/tmp.dump";

    public static final String BUTTON_COLOR = "23 94 E5 FF";

    public static final String ALIPAY_SCHEME = "alipays://platformapi/startapp?saId=10000007&qrcode=";

    public static boolean ready = false;

    public static final short TYPE_NOTIFY = 0;
    public static final short TYPE_COMMAND = 1;
    public static final int BRIDGE_PORT = 10010;

    public static final int AXIS_X = 240;
    //    public static final int AXIS_X = 270;
//    public static final int AXIS_Y = 600;
    public static final int AXIS_Y = 800;

    public int width;
    public int height;


    private static final String TAG = "ServerThread";

    ServerThread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        Log.e("width-display :", String.valueOf(dm.widthPixels));
        Log.e("heigth-display :", String.valueOf(dm.heightPixels));

//        int position = width * AXIS_Y + height + 3;
//        String ret = execShellCmd("dd if=\"" + LOCATION + "\" bs=4 count=1 skip=" + position + " 2>/dev/null ",true);

//        writeMethod1(ret);

//        String ret = execShellCmd("");
//        Log.e("read ret:", ret + "this is ret");


        serverThread = new ServerThread();
        serverThread.start();
    }

    public void clickScreen(int x, int y) {
        execShellCmd("input tap " + x + " " + y, false);
        Log.e("adb tap", x + "," + y);
    }


    class ServerThread extends Thread {
        boolean isLoop = true;

        public void setIsLoop(boolean isLoop) {
            this.isLoop = isLoop;
        }

        @Override
        public void run() {
            Log.d(TAG, "running");
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(BRIDGE_PORT);
                while (isLoop) {

                    Log.e("loop start", "loop start");
                    Socket socket = serverSocket.accept();

                    Thread thread = new TaskThread(socket);
                    thread.start();

                }
                Log.e("finished", "finished");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Log.d(TAG, "destory");
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class TaskThread extends Thread {
        protected Socket socket;

        public TaskThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                while (true) {

                    Log.e(TAG, "accept");
                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    String msg = inputStream.readUTF();
                    Log.e("read-socket", msg);

                    ClientBean client = JSON.parseObject(msg, ClientBean.class);

                    Log.e("msg", client.getMsg());
                    Log.e("groupid", String.valueOf(client.getGroupId()));
                    Log.e("id", String.valueOf(client.getId()));
                    // local client
                    if (client.getGroupId() == TYPE_NOTIFY) {

                        msgList.add(client);

                        semaNotify.release();

                        outputStream.writeUTF("ok");

                        socket.close();

                    } else {

                        semaCommand.acquire();

                        execPay(client.getMsg());

                        semaNotify.acquire();

                        ClientBean notifyMsg = msgList.remove(0);

                        NotifyBean notifyBean = new NotifyBean();

                        notifyBean.setMsg(notifyMsg.getMsg());

                        String str = JSON.toJSONString(notifyBean);

                        outputStream.writeUTF(str);

                        semaCommand.release();

                        socket.close();

                    }

                }

            } catch (Exception e) {


                Log.e("socket-error-occurred", "exit");
                e.printStackTrace();
            }


        }
    }

    public void execPay(String url) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(Uri.parse( ALIPAY_SCHEME + url));
        startActivity(intent);

        tellIfReady();

        clickScreen(AXIS_X, AXIS_Y);
    }


    private void tellIfReady() {
        // reader

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Log.e("read loop", "entered");
                    try {
                        semaScreenRead.acquire();
                        int position = width * AXIS_Y + AXIS_X + 3;
                        String ret = execShellCmd("dd if='" + LOCATION + "' bs=4 count=1 skip=" + position + " 2>/dev/null ", true);
                        Log.e("read ret:", ret + "this is ret");
                        if (ret.contains(BUTTON_COLOR)) {
                            ready = true;
                            semaScreenCap.release();
                            break;
                        }
                        semaScreenCap.release();


                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("read error", "occurred!");
                    }
                }
            }
        }).start();


        while (true) {
            Log.e("write loop", "entered");
            try {
                semaScreenCap.acquire();

                execShellCmd("screencap " + LOCATION, false);

                semaScreenRead.release();

                Thread.sleep(100);

                if (ready) {

                    break;
                }

                Log.e("in cap ready status", String.valueOf(ready));

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("write error", "occurred");
            }

        }

        ready = false;
    }

    private String execShellCmd(String cmd, boolean returnHex) {

        String result = "";
        DataOutputStream dos = null;
//        DataInputStream dis = null;
        InputStream response = null;
        try {
            Process p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            dos = new DataOutputStream(p.getOutputStream());
//            dis = new DataInputStream(p.getInputStream());
            response = p.getInputStream();

            Log.i(TAG, cmd);
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();

//            BufferedReader mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
//            StringBuffer mRespBuff = new StringBuffer();
//            char[] buff = new char[1024];
//            int ch = 0;
//            while((ch = mReader.read(buff)) != -1){
//                mRespBuff.append(buff, 0, ch);
//            }
//            mReader.close();


//            String line = null;
//            while ((line = dis.readLine()) != null) {
//                Log.d("result", line);
//                result += line;
//            }
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!returnHex) {

                result = readFully(response);
            } else {
                result = readHexString(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;


//        try
//
//    {
//        // 申请获取root权限，这一步很重要，不然会没有作用
//        Process process = Runtime.getRuntime().exec("su");
//        // 获取输出流
//        OutputStream outputStream = process.getOutputStream();
//        DataOutputStream dataOutputStream = new DataOutputStream(
//                outputStream);
//        dataOutputStream.writeBytes(cmd);
//        dataOutputStream.flush();
//        dataOutputStream.close();
//        outputStream.close();
//    } catch(
//    Throwable t)
//
//    {
//        t.printStackTrace();
//    }
    }


    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            Log.e("111", String.valueOf(length));
            baos.write(buffer, 0, length);
        }

//        return Hexdump.dumpHexString(baos.toByteArray());
        return baos.toString();
    }

    public static String readHexString(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            Log.e("111", String.valueOf(length));
            baos.write(buffer, 0, length);
        }

        return Hexdump.dumpHexString(baos.toByteArray());
//        return baos.toString();
    }


}

