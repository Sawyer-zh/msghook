package cn.jr1.msghook;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;



public class App extends Application {


    @Override
    public void onCreate() {
        super.onCreate();




//        startService(new Intent(this, NotificationCollectorMonitorService.class));

//        toggleNotificationListenerService();

    }



//    private void toggleNotificationListenerService() {
//        PackageManager pm = getPackageManager();
//        pm.setComponentEnabledSetting(new ComponentName(this, cn.jr1.msghook.WeChatNotificationListenerService.class),
//                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
//
//        pm.setComponentEnabledSetting(new ComponentName(this,cn.jr1.msghook.WeChatNotificationListenerService.class),
//                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
//
//    }


}
