package com.dual.island;

import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IslandService extends Service {
    public static final String ACTION_START = "com.dual.island.START";
    public static final String ACTION_STOP = "com.dual.island.STOP";
    public static final String ACTION_APPLY = "com.dual.island.APPLY";
    private static final String CHANNEL_ID = "dual_island_running";
    private static final int NOTIFICATION_ID = 101;
    private static volatile IslandService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final IslandWindow[] windows = new IslandWindow[2];
    private WindowManager manager;
    private long timerEnd;
    private final Runnable timerTicker = new Runnable() {
        @Override public void run() {
            for (IslandWindow window:windows) if(window != null)window.refreshSmallText();
            handler.postDelayed(this,1000L);
        }
    };

    public static boolean active() { return instance != null; }
    public static void refreshFromNotification() {
        IslandService service = instance;
        if (service != null) service.handler.post(() -> {
            for (IslandWindow window : service.windows) {
                if (window != null && IslandConfig.mode(service,window.id)==1)window.render();
                else if (window != null)window.refreshSmallText();
            }
        });
    }
    @Override public void onCreate() {
        super.onCreate();
        manager=(WindowManager)getSystemService(WINDOW_SERVICE);
        ensureChannel();
        instance=this;
        handler.postDelayed(timerTicker,1000L);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && ACTION_STOP.equals(intent.getAction())){
            stopSelf();return START_NOT_STICKY;
        }
        if(!Settings.canDrawOverlays(this)){
            stopSelf();return START_NOT_STICKY;
        }
        try {
            startForeground(NOTIFICATION_ID,foregroundNotification());
            applySettings();
        }catch(Exception e) {
            android.util.Log.e("DualIsland","Unable to start overlays",e);
            Toast.makeText(this,"Dual Island could not show overlays",Toast.LENGTH_LONG).show();
            stopSelf();
        }
        return START_NOT_STICKY;
    }
    private void ensureChannel() {
        NotificationManager notificationManager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(notificationManager==null)return;
        NotificationChannel channel=new NotificationChannel(CHANNEL_ID,"Dual Island active",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps your floating islands available while you use other apps");
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }
    private Notification foregroundNotification() {
        Intent open=new Intent(this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPi=PendingIntent.getActivity(this,2,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop=new Intent(this,IslandService.class).setAction(ACTION_STOP);
        PendingIntent stopPi=PendingIntent.getService(this,3,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL_ID)
            .setSmallIcon(com.dualisland.manager.R.drawable.ic_notification)
            .setContentTitle("Dual Island is active")
            .setContentText("Tap to edit · Two independent floating islands")
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"Stop islands",stopPi).build())
            .build();
    }
    private void applySettings() {
        int count=0;
        for(int i=0;i<2;i++) {
            if(IslandConfig.enabled(this,i)) {
                count++;
                if(windows[i]==null)windows[i]=new IslandWindow(i);
                else windows[i].applyConfig();
            } else {
                if(windows[i]!=null){windows[i].remove();windows[i]=null;}
            }
        }
        if(count==0)stopSelf();
    }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        for(int i=0;i<2;i++){
            if(windows[i]!=null){windows[i].remove();windows[i]=null;}
        }
        instance=null;
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) {return null;}
    private int dp(float v) {return Math.round(getResources().getDisplayMetrics().density*v);}
    private GradientDrawable rounded(int fill,int outline,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));
        if(outline!=0)d.setStroke(dp(1),outline);return d;
    }
    private TextView txt(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        t.setSingleLine(false);t.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private void launch(Intent intent) {
        try {intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);}
        catch(Exception e){Toast.makeText(this,"No app available for this action",Toast.LENGTH_SHORT).show();}
    }
    private void openSettings() {launch(new Intent(this,MainActivity.class));}
    private boolean usageAllowed() {
        AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);
        return ops!=null && ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_ALLOWED;
    }
    private MediaController currentMedia() {
        try {
            MediaSessionManager sessions=(MediaSessionManager)getSystemService(MEDIA_SESSION_SERVICE);
            if(sessions==null)return null;
            List<MediaController> controllers=sessions.getActiveSessions(new ComponentName(this,IslandNotificationListener.class));
            if(controllers==null || controllers.isEmpty())return null;
            for(MediaController c:controllers){
                PlaybackState state=c.getPlaybackState();
                if(state!=null && state.getState()==PlaybackState.STATE_PLAYING)return c;
            }
            return controllers.get(0);
        } catch (SecurityException | IllegalArgumentException e) {return null;}
    }
    private String musicTitle() {
        MediaController c=currentMedia();if(c==null)return "Music controls";
        MediaMetadata metadata=c.getMetadata();if(metadata==null)return "Music controls";
        String title=metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        return (title==null || title.isEmpty())?"Music controls":title;
    }
    private void mediaButton(int keycode) {
        MediaController c=currentMedia();
        if(c!=null){
            try {
                switch(keycode){
                    case KeyEvent.KEYCODE_MEDIA_NEXT:c.getTransportControls().skipToNext();return;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:c.getTransportControls().skipToPrevious();return;
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        PlaybackState state=c.getPlaybackState();
                        if(state!=null && state.getState()==PlaybackState.STATE_PLAYING)c.getTransportControls().pause();
                        else c.getTransportControls().play();
                        handler.postDelayed(()->{
                            for(IslandWindow window:windows)if(window!=null)window.refreshSmallText();
                        },250);return;
                }
            }catch(Exception ignored){}
        }
        AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        if(audio!=null){
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,keycode));
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,keycode));
        }
    }
    private List<String> recentApps() {
        ArrayList<String> apps=new ArrayList<>();
        if(!usageAllowed())return apps;
        UsageStatsManager manager=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
        if(manager==null)return apps;
        UsageEvents events=manager.queryEvents(System.currentTimeMillis()-24L*60*60*1000,System.currentTimeMillis());
        if(events==null)return apps;
        LinkedHashMap<String,Long> seen=new LinkedHashMap<>();
        UsageEvents.Event event=new UsageEvents.Event();
        while(events.hasNextEvent()) {
            events.getNextEvent(event);
            if(event.getEventType()==UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT>=29 && event.getEventType()==UsageEvents.Event.ACTIVITY_RESUMED)) {
                String pkg=event.getPackageName();
                if(pkg!=null && !pkg.equals(getPackageName()))seen.put(pkg,event.getTimeStamp());
            }
        }
        ArrayList<Map.Entry<String,Long>> ordered=new ArrayList<>(seen.entrySet());
        ordered.sort((a,b)->Long.compare(b.getValue(),a.getValue()));
        PackageManager pm=getPackageManager();
        for(Map.Entry<String,Long> entry:ordered){
            if(pm.getLaunchIntentForPackage(entry.getKey())!=null){apps.add(entry.getKey());if(apps.size()==3)break;}
        }
        return apps;
    }
    private String appLabel(String pkg) {
        try {
            ApplicationInfo info=getPackageManager().getApplicationInfo(pkg,0);
            String label=getPackageManager().getApplicationLabel(info).toString();
            return label.length()>13?label.substring(0,12)+"…":label;
        }catch(PackageManager.NameNotFoundException e){return pkg;}
    }
    private void startTimer() {timerEnd=SystemClock.elapsedRealtime()+5*60_000L;refreshAll();}
    private void addMinute() {timerEnd=Math.max(timerEnd,SystemClock.elapsedRealtime())+60_000L;refreshAll();}
    private void cancelTimer() {timerEnd=0;refreshAll();}
    private String timeLeft() {
        long s=Math.max(0,(timerEnd-SystemClock.elapsedRealtime()+999L)/1000L);
        if(s==0)return "Timer ready";
        return String.format(java.util.Locale.US,"%02d:%02d",s/60,s%60);
    }
    private void refreshAll() {for(IslandWindow window:windows)if(window!=null)window.render();}

    private final class IslandWindow {
        final int id;
        final LinearLayout root;
        WindowManager.LayoutParams params;
        boolean expanded;
        TextView smallLabel;
        TextView timerLabel;

        IslandWindow(int index) {
            id=index;
            root=new LinearLayout(IslandService.this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setClipChildren(false);
            params=new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
            params.gravity=Gravity.TOP|Gravity.START;
            resetCoordsFromPrefs();
            render();
            manager.addView(root,params);
        }
        private void resetCoordsFromPrefs() {
            int x=IslandConfig.x(IslandService.this,id);
            int screen=getResources().getDisplayMetrics().widthPixels;
            params.x=x<0?Math.max(0,(screen-dp(IslandConfig.width(IslandService.this,id)))/2):dp(x);
            params.y=dp(IslandConfig.y(IslandService.this,id));
            clamp();
        }
        private void clamp(){
            int screenW=getResources().getDisplayMetrics().widthPixels;
            int screenH=getResources().getDisplayMetrics().heightPixels;
            params.x=Math.max(0,Math.min(params.x,Math.max(0,screenW-dp(65))));
            params.y=Math.max(dp(4),Math.min(params.y,Math.max(dp(8),screenH-dp(95))));
        }
        private void avoidOtherIsland(){
            IslandWindow other=windows[1-id];
            if(other==null)return;
            int horizontalGap=Math.abs(params.x-other.params.x);
            int verticalGap=Math.abs(params.y-other.params.y);
            int limit=dp(Math.min(IslandConfig.width(IslandService.this,id),
                IslandConfig.width(IslandService.this,other.id))*0.65f);
            if(horizontalGap<limit && verticalGap<dp(43)){
                int above=other.params.y-dp(64);
                params.y=above>=dp(4)?above:other.params.y+dp(64);
                clamp();safeLayout();
            }
        }
        void applyConfig() {
            resetCoordsFromPrefs();render();safeLayout();
        }
        private void safeLayout() {
            try {manager.updateViewLayout(root,params);}catch(IllegalArgumentException ignored){}
        }
        void remove() {
            try {manager.removeView(root);}catch(IllegalArgumentException ignored){}
        }
        private LinearLayout row(){
            LinearLayout l=new LinearLayout(IslandService.this);
            l.setGravity(Gravity.CENTER_VERTICAL);l.setOrientation(LinearLayout.HORIZONTAL);return l;
        }
        private LinearLayout.LayoutParams layout(int w,int h) {
            return new LinearLayout.LayoutParams(w,h);
        }
        private String shortTitle(){
            switch(IslandConfig.mode(IslandService.this,id)){
                case 0: return musicTitle();
                case 1: return IslandNotificationListener.lastTitle()==null?"Notifications":
                    IslandConfig.previewNotifications(IslandService.this)?IslandNotificationListener.lastTitle():"New notification";
                case 2: return "Quick launch";
                case 3: return timeLeft();
                default:return "Island";
            }
        }
        void refreshSmallText() {
            if(smallLabel!=null)smallLabel.setText(shortTitle());
            if(timerLabel!=null)timerLabel.setText(timeLeft());
        }
        void render() {
            int accent=IslandConfig.COLORS[IslandConfig.color(IslandService.this,id)];
            int width=IslandConfig.width(IslandService.this,id);
            root.removeAllViews();
            root.setPadding(dp(5),dp(5),dp(5),dp(5));
            root.setAlpha(IslandConfig.opacity(IslandService.this,id)/100f);
            root.setBackground(rounded(0xFF070C17,accent,expanded?20:100));
            root.setMinimumWidth(dp(width));
            root.setVisibility(View.VISIBLE);
            if(!expanded){
                LinearLayout compact=row();
                compact.setMinimumWidth(dp(width)-dp(10));
                compact.setPadding(dp(11),dp(1),dp(11),dp(1));
                TextView dot=txt("●",17,accent,true);
                compact.addView(dot,layout(dp(22),dp(36)));
                smallLabel=txt(shortTitle(),12,Color.WHITE,true);
                smallLabel.setSingleLine(true);smallLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
                compact.addView(smallLabel,new LinearLayout.LayoutParams(0,dp(36),1));
                TextView more=txt("⌄",18,0xFF9FAFC8,true);more.setGravity(Gravity.CENTER);
                compact.addView(more,layout(dp(18),dp(36)));
                root.addView(compact,layout(-1,dp(36)));
                attachDrag(compact);
                timerLabel=null;
            }else{
                LinearLayout header=row();header.setPadding(dp(12),dp(5),dp(8),dp(3));
                TextView title=txt(id==0?"ISLAND A":"ISLAND B",12,accent,true);
                header.addView(title,new LinearLayout.LayoutParams(0,dp(34),1));
                TextView close=txt("✕",15,Color.WHITE,true);close.setGravity(Gravity.CENTER);
                header.addView(close,layout(dp(32),dp(34)));
                close.setOnClickListener(v->{expanded=false;render();safeLayout();});
                root.addView(header,layout(-1,dp(42)));
                attachDrag(title);
                View line=new View(IslandService.this);line.setBackgroundColor(0xFF2A3951);
                root.addView(line,layout(-1,dp(1)));
                LinearLayout body=new LinearLayout(IslandService.this);
                body.setPadding(dp(10),dp(6),dp(10),dp(12));
                body.setOrientation(LinearLayout.VERTICAL);
                root.addView(body,layout(-1,-2));
                smallLabel=null;timerLabel=null;
                switch(IslandConfig.mode(IslandService.this,id)){
                    case 0:drawMedia(body,accent);break;
                    case 1:drawNotifications(body,accent);break;
                    case 2:drawShortcuts(body,accent);break;
                    case 3:drawTimer(body,accent);break;
                }
            }
            safeLayout();
        }
        private void attachDrag(View target) {
            target.setOnTouchListener(new View.OnTouchListener(){
                float sx,sy;int x,y;boolean dragging;
                @Override public boolean onTouch(View view,MotionEvent event){
                    switch(event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            sx=event.getRawX();sy=event.getRawY();x=params.x;y=params.y;
                            dragging=false;return true;
                        case MotionEvent.ACTION_MOVE:
                            float dx=event.getRawX()-sx,dy=event.getRawY()-sy;
                            if(Math.abs(dx)>dp(7)||Math.abs(dy)>dp(7))dragging=true;
                            if(dragging){params.x=x+Math.round(dx);params.y=y+Math.round(dy);clamp();safeLayout();}
                            return true;
                        case MotionEvent.ACTION_UP:
                            if(dragging){
                                avoidOtherIsland();
                                IslandConfig.putInt(IslandService.this,id,"x",Math.round(params.x/getResources().getDisplayMetrics().density));
                                IslandConfig.putInt(IslandService.this,id,"y",Math.round(params.y/getResources().getDisplayMetrics().density));
                            }else{expanded=!expanded;render();safeLayout();}
                            return true;
                        case MotionEvent.ACTION_CANCEL:return true;
                    }return false;
                }
            });
        }
        private TextView action(String caption,int accent,Runnable action) {
            TextView b=txt(caption,12,Color.WHITE,true);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(8),dp(10),dp(8),dp(10));
            b.setBackground(rounded(0xFF233047,0,11));
            b.setOnClickListener(v->{action.run();refreshSmallText();});
            return b;
        }
        private void threeActions(LinearLayout parent,String[] captions,Runnable[] callbacks,int accent){
            LinearLayout actions=row();
            LinearLayout.LayoutParams base=new LinearLayout.LayoutParams(-1,dp(45));base.topMargin=dp(7);
            parent.addView(actions,base);
            for(int i=0;i<captions.length;i++){
                TextView v=action(captions[i],accent,callbacks[i]);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
                if(i>0)p.leftMargin=dp(5);
                actions.addView(v,p);
            }
        }
        private void drawMedia(LinearLayout body,int accent){
            TextView song=txt(musicTitle(),13,Color.WHITE,true);
            song.setMaxLines(2);body.addView(song,layout(-1,-2));
            threeActions(body,new String[]{"◀◀","▶ / Ⅱ","▶▶"},new Runnable[]{
                ()->mediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS),
                ()->mediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
                ()->mediaButton(KeyEvent.KEYCODE_MEDIA_NEXT)},accent);
            TextView open=action("Open music app",accent,()->{
                Intent music=Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,Intent.CATEGORY_APP_MUSIC);
                launch(music);
            });
            LinearLayout.LayoutParams p=layout(-1,dp(42));p.topMargin=dp(8);body.addView(open,p);
            body.addView(txt("Media titles need notification access.",10,0xFF9FAFC8,false),layout(-1,dp(27)));
        }
        private void drawNotifications(LinearLayout body,int accent){
            boolean preview=IslandConfig.previewNotifications(IslandService.this);
            String title=IslandNotificationListener.lastTitle();
            String bodyText=IslandNotificationListener.lastText();
            body.addView(txt(title==null?"No recent notification":preview?title:"Latest notification",13,Color.WHITE,true),layout(-1,-2));
            if(preview && bodyText!=null){
                TextView detail=txt(bodyText,11,0xFFD0D8E7,false);detail.setMaxLines(3);
                LinearLayout.LayoutParams d=layout(-1,-2);d.topMargin=dp(5);body.addView(detail,d);
            }
            TextView open=action("Open latest notification",accent,()->{
                PendingIntent pi=IslandNotificationListener.lastIntent();
                if(pi!=null){try{pi.send();}catch(PendingIntent.CanceledException e){openSettings();}}
                else openSettings();
            });
            LinearLayout.LayoutParams p=layout(-1,dp(42));p.topMargin=dp(9);body.addView(open,p);
            body.addView(txt("Enable notification access in the app.",10,0xFF9FAFC8,false),layout(-1,dp(28)));
        }
        private void drawShortcuts(LinearLayout body,int accent){
            body.addView(txt("FAST ACTIONS",11,0xFF9FAFC8,true),layout(-1,dp(25)));
            threeActions(body,new String[]{"⚙ Settings","◎ Camera","◉ Web"},new Runnable[]{
                ()->launch(new Intent(Settings.ACTION_SETTINGS)),
                ()->launch(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
                ()->launch(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")))},accent);
            List<String> recent=recentApps();
            TextView heading=txt("RECENT APPS",11,0xFF9FAFC8,true);
            LinearLayout.LayoutParams hp=layout(-1,dp(30));hp.topMargin=dp(7);body.addView(heading,hp);
            if(recent.isEmpty()){
                TextView setup=action(usageAllowed()?"No recent launchable apps":"Allow recent-app access",accent,()->{
                    launch(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                });
                body.addView(setup,layout(-1,dp(42)));
            }else {
                for(String pkg:recent){
                    TextView item=action("↗  "+appLabel(pkg),accent,()->{
                        Intent target=getPackageManager().getLaunchIntentForPackage(pkg);
                        if(target!=null)launch(target);
                    });
                    LinearLayout.LayoutParams p=layout(-1,dp(39));p.topMargin=dp(5);body.addView(item,p);
                }
            }
        }
        private void drawTimer(LinearLayout body,int accent){
            timerLabel=txt(timeLeft(),27,Color.WHITE,true);
            timerLabel.setGravity(Gravity.CENTER);
            body.addView(timerLabel,layout(-1,dp(52)));
            threeActions(body,new String[]{"Start 5m","+ 1 min","Cancel"},new Runnable[]{
                ()->startTimer(),()->addMinute(),()->cancelTimer()},accent);
        }
    }
}
