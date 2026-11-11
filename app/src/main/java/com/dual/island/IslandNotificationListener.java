package com.dual.island;

import android.app.Notification;
import android.app.PendingIntent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class IslandNotificationListener extends NotificationListenerService {
    private static volatile String lastTitle;
    private static volatile String lastText;
    private static volatile PendingIntent lastIntent;

    static String lastTitle(){return lastTitle;}
    static String lastText(){return lastText;}
    static PendingIntent lastIntent(){return lastIntent;}

    @Override public void onNotificationPosted(StatusBarNotification sbn){
        if(sbn==null || sbn.getPackageName().equals(getPackageName()))return;
        Notification notification=sbn.getNotification();
        if(notification==null || notification.extras==null)return;
        CharSequence title=notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text=notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        if(title==null && text==null)return;
        lastTitle=title==null?"New notification":title.toString();
        lastText=text==null?null:text.toString();
        lastIntent=notification.contentIntent;
        IslandService.refreshFromNotification();
    }
    @Override public void onListenerConnected(){
        super.onListenerConnected();
        try {
            StatusBarNotification[] active=getActiveNotifications();
            if(active!=null)for(int i=active.length-1;i>=0;i--){
                if(!active[i].getPackageName().equals(getPackageName())){
                    onNotificationPosted(active[i]);break;
                }
            }
        }catch(Exception ignored){}
    }
    @Override public void onListenerDisconnected(){
        lastTitle=null;lastText=null;lastIntent=null;
        IslandService.refreshFromNotification();
        super.onListenerDisconnected();
    }
}
