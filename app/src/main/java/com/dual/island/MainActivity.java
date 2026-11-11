package com.dual.island;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BG = 0xFF0B1020, PANEL = 0xFF161F34, BORDER = 0xFF2B3751;
    private static final int WHITE = 0xFFF0F5FF, MUTED = 0xFF9FAFC8, MINT = 0xFF45F0C8;
    private LinearLayout content;
    private TextView permissionLabel, notificationLabel, usageLabel, runningLabel;
    private final LinearLayout[] islandCards = new LinearLayout[2];
    private boolean resumeReady;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildScreen();
        resumeReady = true;
    }

    private int dp(float n) { return Math.round(getResources().getDisplayMetrics().density*n); }
    private GradientDrawable bg(int fill, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    private LinearLayout.LayoutParams lp(int width, int height, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.topMargin=dp(top); return p;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private Button button(String title, boolean filled) {
        Button b = new Button(this);
        b.setText(title); b.setTextSize(12); b.setAllCaps(false);
        b.setTextColor(filled ? BG : WHITE);
        b.setBackground(bg(filled ? MINT : 0xFF263149, 13, filled ? 0 : BORDER));
        b.setMinimumHeight(0); b.setMinHeight(0); b.setPadding(dp(12),dp(11),dp(12),dp(11));
        return b;
    }
    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        content = column();
        content.setPadding(dp(18),dp(22),dp(18),dp(38));
        scroll.addView(content);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            content.setPadding(dp(18),dp(18)+top,dp(18),dp(32)+bottom);
            return insets;
        });
        setContentView(scroll);

        TextView kicker = text("YOUR MULTITASKING SPACE",11,MINT,true);
        kicker.setLetterSpacing(.12f); content.addView(kicker,lp(-1,dp(24),0));
        content.addView(text("Dual Island",34,WHITE,true),lp(-1,dp(49),0));
        content.addView(text("Two islands. Your rules.",16,MUTED,false),lp(-1,dp(26),0));

        FrameLayout demo = new FrameLayout(this); demo.setBackground(bg(PANEL,22,BORDER));
        content.addView(demo,lp(-1,dp(180),21));
        TextView demoHint = text("LIVE LAYOUT PREVIEW",10,MUTED,true);
        FrameLayout.LayoutParams dh = new FrameLayout.LayoutParams(-2,dp(22),Gravity.TOP|Gravity.START);
        dh.leftMargin=dp(18);dh.topMargin=dp(12);demo.addView(demoHint,dh);
        TextView camera = text("●",17,0xFF5D687D,true); camera.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cc = new FrameLayout.LayoutParams(dp(26),dp(26),Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        cc.topMargin=dp(7); demo.addView(camera,cc);
        TextView first = demoPill("♪  Playing now     ▸",IslandConfig.COLORS[IslandConfig.color(this,0)]);
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(195),dp(42),Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        fp.topMargin=dp(44);demo.addView(first,fp);
        TextView second = demoPill("▦  Quick tools     ▸",IslandConfig.COLORS[IslandConfig.color(this,1)]);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(dp(166),dp(38),Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        sp.topMargin=dp(98);demo.addView(second,sp);
        TextView foot=text("Drag each island anywhere on screen",11,MUTED,false);
        foot.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams fh = new FrameLayout.LayoutParams(-1,dp(28),Gravity.BOTTOM);
        fh.bottomMargin=dp(4); demo.addView(foot,fh);

        LinearLayout status = column(); status.setPadding(dp(16),dp(15),dp(16),dp(15));
        status.setBackground(bg(PANEL,18,BORDER));content.addView(status,lp(-1,-2,16));
        LinearLayout statusRow = row();
        runningLabel = text("● Not running",14,0xFFFFBB81,true);
        statusRow.addView(runningLabel,new LinearLayout.LayoutParams(0,dp(28),1));
        Button permissions = button("Permissions",false);
        permissions.setOnClickListener(v -> openOverlaySettings());
        statusRow.addView(permissions);status.addView(statusRow);
        permissionLabel=text("Display over other apps: checking",12,MUTED,false);
        status.addView(permissionLabel,lp(-1,dp(23),5));
        notificationLabel=text("Notifications: optional",12,MUTED,false);
        status.addView(notificationLabel,lp(-1,dp(22),0));
        usageLabel=text("Recent apps: optional",12,MUTED,false);
        status.addView(usageLabel,lp(-1,dp(22),0));

        LinearLayout command = row(); content.addView(command,lp(-1,dp(51),15));
        Button start=button("▶  Start islands",true);
        command.addView(start,new LinearLayout.LayoutParams(0,dp(51),1));
        Button stop=button("■  Stop",false);
        LinearLayout.LayoutParams stopLp=new LinearLayout.LayoutParams(0,dp(51),.64f);stopLp.leftMargin=dp(9);
        command.addView(stop,stopLp);
        start.setOnClickListener(v -> startIslands());
        stop.setOnClickListener(v -> stopIslands());

        TextView section = text("CUSTOMISE EACH ISLAND",12,MUTED,true);
        section.setLetterSpacing(.10f); content.addView(section,lp(-1,dp(26),26));
        for (int i=0;i<2;i++) {
            islandCards[i]=column();
            islandCards[i].setBackground(bg(PANEL,20,BORDER));
            content.addView(islandCards[i],lp(-1,-2,11));
            populateIslandCard(i);
        }

        LinearLayout extras = column();extras.setPadding(dp(16),dp(17),dp(16),dp(18));
        extras.setBackground(bg(PANEL,18,BORDER));content.addView(extras,lp(-1,-2,17));
        extras.addView(text("EXTRA ACCESS (OPTIONAL)",13,WHITE,true));
        extras.addView(text("You can enable these later. Neither is needed for the two islands to appear.",12,MUTED,false),lp(-1,-2,9));
        Button notifButton=button("Allow notification access",false);
        notifButton.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
            catch (Exception e) { Toast.makeText(this,"Open notification access in Android settings",Toast.LENGTH_LONG).show(); }
        });
        extras.addView(notifButton,lp(-1,dp(44),13));
        Switch privacy = new Switch(this);
        privacy.setText("Show message text on island");privacy.setTextColor(WHITE);privacy.setTextSize(12);
        privacy.setThumbTintList(ColorStateList.valueOf(MINT));
        privacy.setChecked(IslandConfig.previewNotifications(this));
        privacy.setOnCheckedChangeListener((b,checked)->{
            IslandConfig.prefs(this).edit().putBoolean("show_notification_text",checked).apply();applyLive();
        });
        extras.addView(privacy,lp(-1,dp(45),7));
        Button usageButton=button("Allow recent-app access",false);
        usageButton.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
            catch(Exception e) { Toast.makeText(this,"Open Usage Access in Android settings",Toast.LENGTH_LONG).show(); }
        });
        extras.addView(usageButton,lp(-1,dp(44),8));
        Button batteryButton=button("Battery optimisation settings",false);
        batteryButton.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
            catch(Exception e) { Toast.makeText(this,"Open battery settings on your phone",Toast.LENGTH_LONG).show(); }
        });
        extras.addView(batteryButton,lp(-1,dp(44),8));
        content.addView(text("Tap an island to open its controls. Drag its compact pill to move it. Your settings are saved on this phone.",12,MUTED,false),lp(-1,-2,18));
        updateStatus();
    }
    private TextView demoPill(String label,int accent) {
        TextView v=text(label,13,WHITE,true);
        v.setGravity(Gravity.CENTER);v.setBackground(bg(0xFF05070F,99,accent));
        return v;
    }
    private void populateIslandCard(int i) {
        LinearLayout card=islandCards[i];card.removeAllViews();card.setPadding(dp(16),dp(13),dp(16),dp(16));
        LinearLayout top=row();card.addView(top);
        TextView name=text(i==0 ? "ISLAND A  •  PRIMARY" : "ISLAND B  •  SECONDARY",14,WHITE,true);
        top.addView(name,new LinearLayout.LayoutParams(0,dp(42),1));
        Switch enabled=new Switch(this);enabled.setShowText(false);
        enabled.setThumbTintList(ColorStateList.valueOf(IslandConfig.COLORS[IslandConfig.color(this,i)]));
        enabled.setChecked(IslandConfig.enabled(this,i));
        top.addView(enabled);
        enabled.setOnCheckedChangeListener((b,checked)-> {IslandConfig.putBool(this,i,"enabled",checked);applyLive();});

        card.addView(text("What this island controls",12,MUTED,false),lp(-1,dp(22),6));
        Spinner mode=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,IslandConfig.MODES);
        mode.setAdapter(adapter);mode.setSelection(IslandConfig.mode(this,i));
        mode.setBackground(bg(0xFF263149,12,0));
        card.addView(mode,lp(-1,dp(47),5));
        mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id) {
                if (IslandConfig.mode(MainActivity.this,i)!=position) {IslandConfig.putInt(MainActivity.this,i,"mode",position);applyLive();}
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        card.addView(text("Accent colour",12,MUTED,false),lp(-1,dp(22),12));
        LinearLayout colours=row();card.addView(colours,lp(-1,dp(42),4));
        for (int c=0;c<IslandConfig.COLORS.length;c++) {
            final int choice=c;
            TextView circle=text("●",29,IslandConfig.COLORS[c],true);
            circle.setGravity(Gravity.CENTER);
            circle.setContentDescription(IslandConfig.COLOR_NAMES[c]);
            circle.setBackground(bg(0xFF263149,14,c==IslandConfig.color(this,i)?WHITE:0));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(42),1);
            p.leftMargin=dp(4);p.rightMargin=dp(4);colours.addView(circle,p);
            circle.setOnClickListener(v->{IslandConfig.putInt(this,i,"color",choice);populateIslandCard(i);applyLive();});
        }

        TextView sizeLabel=text("Pill width  •  "+IslandConfig.width(this,i)+" dp",12,MUTED,false);
        card.addView(sizeLabel,lp(-1,dp(22),14));
        SeekBar size=new SeekBar(this);size.setMax(163);
        size.setProgress(IslandConfig.width(this,i)-112);
        size.setProgressTintList(ColorStateList.valueOf(IslandConfig.COLORS[IslandConfig.color(this,i)]));
        card.addView(size,lp(-1,dp(40),2));
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b,int progress,boolean user) {
                if(!user) return;int w=112+progress;sizeLabel.setText("Pill width  •  "+w+" dp");
                IslandConfig.putInt(MainActivity.this,i,"width",w);applyLive();
            }
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){}
        });
        TextView opacityLabel=text("Opacity  •  "+IslandConfig.opacity(this,i)+"%",12,MUTED,false);
        card.addView(opacityLabel,lp(-1,dp(22),5));
        SeekBar opacity=new SeekBar(this);opacity.setMax(45);
        opacity.setProgress(IslandConfig.opacity(this,i)-55);
        opacity.setProgressTintList(ColorStateList.valueOf(IslandConfig.COLORS[IslandConfig.color(this,i)]));
        card.addView(opacity,lp(-1,dp(40),2));
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b,int progress,boolean user) {
                if(!user)return;int value=55+progress;
                opacityLabel.setText("Opacity  •  "+value+"%");
                IslandConfig.putInt(MainActivity.this,i,"opacity",value);applyLive();
            }
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){}
        });
        Button reset=button("↺ Reset this island's position",false);
        reset.setOnClickListener(v->{
            IslandConfig.prefs(this).edit().remove(IslandConfig.k(i,"x"))
                .remove(IslandConfig.k(i,"y")).apply();applyLive();
            Toast.makeText(this,"Island position reset",Toast.LENGTH_SHORT).show();
        });
        card.addView(reset,lp(-1,dp(43),11));
    }
    private boolean hasNotificationAccess() {
        String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        return enabled!=null && enabled.contains(new ComponentName(this,IslandNotificationListener.class).flattenToString());
    }
    private boolean hasUsageAccess() {
        AppOpsManager ops=(AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
        if(ops==null)return false;
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_ALLOWED;
    }
    private void updateStatus() {
        if (permissionLabel==null)return;
        boolean overlay=Settings.canDrawOverlays(this);
        permissionLabel.setText("Display over apps: "+(overlay?"✓ Allowed":"✕ Permission needed"));
        notificationLabel.setText("Notifications: "+(hasNotificationAccess()?"✓ Allowed":"Optional, not connected"));
        usageLabel.setText("Recent apps: "+(hasUsageAccess()?"✓ Allowed":"Optional, not connected"));
        runningLabel.setText(IslandService.active()?"● Islands running":"● Islands stopped");
        runningLabel.setTextColor(IslandService.active()?MINT:0xFFFFBB81);
    }
    private void openOverlaySettings() {
        Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));
        try {startActivity(intent);}catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        }
    }
    private void startIslands() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this,"First, allow Display over other apps",Toast.LENGTH_LONG).show();
            openOverlaySettings();return;
        }
        if(!IslandConfig.enabled(this,0)&&!IslandConfig.enabled(this,1)) {
            Toast.makeText(this,"Enable at least one island",Toast.LENGTH_LONG).show();return;
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},77);
        }
        try {
            Intent intent=new Intent(this,IslandService.class).setAction(IslandService.ACTION_START);
            if(Build.VERSION.SDK_INT>=26)startForegroundService(intent); else startService(intent);
            runningLabel.setText("● Starting islands…");runningLabel.setTextColor(MINT);
        } catch(Exception e) {Toast.makeText(this,"Could not start: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void stopIslands() {
        try {stopService(new Intent(this,IslandService.class));}
        catch(Exception ignored){}
        runningLabel.setText("● Islands stopped");runningLabel.setTextColor(0xFFFFBB81);
    }
    private void applyLive() {
        if(IslandService.active()) {
            Intent intent=new Intent(this,IslandService.class).setAction(IslandService.ACTION_APPLY);
            startService(intent);
        }
        updateStatus();
    }
    @Override protected void onResume() {super.onResume();if(resumeReady)updateStatus();}
}
