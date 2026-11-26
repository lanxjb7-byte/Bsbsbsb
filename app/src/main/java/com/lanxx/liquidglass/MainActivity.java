package com.lanxx.liquidglass;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Build;
import android.graphics.RenderEffect;
import android.graphics.Shader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_APKS = 410;
    private final ArrayList<ApkItem> items = new ArrayList<>();
    private LinearLayout list;
    private TextView countText, emptyText;
    private EditText search;
    private SharedPreferences prefs;
    private int white = Color.WHITE;
    private int muted = Color.rgb(177, 190, 210);

    static class ApkItem {
        String name, pkg, size, uri;
        ApkItem(String n, String p, String s, String u) { name=n; pkg=p; size=s; uri=u; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(8,10,18));
        getWindow().setNavigationBarColor(Color.rgb(8,10,18));
        getWindow().getDecorView().setSystemUiVisibility(0);
        prefs = getSharedPreferences("glass_apks", MODE_PRIVATE);
        loadSaved();
        buildScreen();
        renderList("");
    }

    private GradientDrawable shape(int color, int stroke, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private void glass(View v, int alpha) {
        v.setBackground(shape(Color.argb(alpha, 225, 239, 255),
                Color.argb(105,255,255,255), 24));
        v.setElevation(dp(5));
    }

    private void buildScreen() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(11,17,34), Color.rgb(28,23,53), Color.rgb(8,12,23)}));

        // Soft blurred color orbs behind translucent glass panels.
        View orb1 = new View(this);
        GradientDrawable o1 = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xAA79C9FF, 0x227B5CFF});
        o1.setShape(GradientDrawable.OVAL); orb1.setBackground(o1);
        FrameLayout.LayoutParams op1 = new FrameLayout.LayoutParams(dp(250),dp(250),Gravity.TOP|Gravity.RIGHT);
        op1.setMargins(0,dp(60),dp(-90),0); root.addView(orb1,op1);
        View orb2 = new View(this);
        GradientDrawable o2 = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x886E58FF, 0x1179DFFF});
        o2.setShape(GradientDrawable.OVAL); orb2.setBackground(o2);
        FrameLayout.LayoutParams op2 = new FrameLayout.LayoutParams(dp(230),dp(230),Gravity.BOTTOM|Gravity.LEFT);
        op2.setMargins(dp(-80),0,0,dp(110)); root.addView(orb2,op2);
        if (Build.VERSION.SDK_INT >= 31) {
            orb1.setRenderEffect(RenderEffect.createBlurEffect(dp(48),dp(48), Shader.TileMode.CLAMP));
            orb2.setRenderEffect(RenderEffect.createBlurEffect(dp(52),dp(52), Shader.TileMode.CLAMP));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = vertical();
        page.setPadding(dp(20),dp(20),dp(20),dp(28));
        scroll.addView(page);
        root.addView(scroll, new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("✦",30,0xFFBDE7FF,true);
        header.addView(mark,lp(42,48));
        LinearLayout titles = vertical();
        titles.addView(text("LIQUID GLASS",20,white,true));
        titles.addView(text("APK LIBRARY  •  NATIVE ANDROID",10,muted,true));
        header.addView(titles,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView plus = text("+ ADD APK",12,Color.rgb(10,20,35),true);
        plus.setGravity(Gravity.CENTER);
        plus.setPadding(dp(15),0,dp(15),0);
        plus.setBackground(shape(0xFFE2F4FF,0,30));
        plus.setOnClickListener(v -> pickApks());
        header.addView(plus,lp(-2,42));
        page.addView(header);

        LinearLayout hero = vertical();
        hero.setPadding(dp(18),dp(18),dp(18),dp(18));
        glass(hero,42);
        LinearLayout.LayoutParams hp = lp(-1,-2); hp.topMargin=dp(22);
        page.addView(hero,hp);
        TextView eyebrow = text("YOUR GLASS COLLECTION",10,0xFFC7E7FF,true);
        hero.addView(eyebrow);
        countText = text("0 APK",32,white,true);
        hero.addView(countText);
        TextView sub = text("Simpan dan kelola aplikasi dalam satu tempat.",12,muted,false);
        hero.addView(sub);
        LinearLayout.LayoutParams bp = lp(-1,48); bp.topMargin=dp(16);
        TextView importBtn = text("＋     IMPORT FILE APK",13,Color.rgb(12,23,40),true);
        importBtn.setGravity(Gravity.CENTER);
        importBtn.setBackground(shape(0xFFDDF1FF,0,16));
        importBtn.setOnClickListener(v -> pickApks());
        hero.addView(importBtn,bp);

        LinearLayout searchCard = new LinearLayout(this);
        searchCard.setGravity(Gravity.CENTER_VERTICAL);
        searchCard.setPadding(dp(14),0,dp(14),0);
        glass(searchCard,27);
        LinearLayout.LayoutParams sp = lp(-1,50); sp.topMargin=dp(20);
        page.addView(searchCard,sp);
        TextView magnify = text("⌕",24,white,true);
        searchCard.addView(magnify,lp(30,-1));
        search = new EditText(this);
        search.setSingleLine(true); search.setTextColor(white);
        search.setHintTextColor(0xFF9AAAC0); search.setHint("Cari nama atau package APK...");
        search.setTextSize(13); search.setBackgroundColor(Color.TRANSPARENT);
        searchCard.addView(search,new LinearLayout.LayoutParams(0,dp(48),1));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){renderList(s.toString());}
            public void afterTextChanged(Editable e){}
        });

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text("APLIKASI TERSIMPAN",12,white,true);
        section.addView(heading,new LinearLayout.LayoutParams(0,dp(48),1));
        section.addView(text("MULTI SELECT",9,0xFFB9DFFF,true));
        LinearLayout.LayoutParams secp=lp(-1,-2); secp.topMargin=dp(8);
        page.addView(section,secp);

        list = vertical();
        page.addView(list,lp(-1,-2));
        emptyText = text("Belum ada APK.\nTekan ADD APK untuk mulai menambahkan.",13,muted,false);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(dp(10),dp(32),dp(10),dp(32));
        page.addView(emptyText,lp(-1,-2));

        TextView foot = text("LIQUID GLASS  •  BUILT WITH ANDROID JAVA",9,0xFF8092AB,true);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp=lp(-1,44); fp.topMargin=dp(12);
        page.addView(foot,fp);

        setContentView(root);
    }

    private void pickApks() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("application/vnd.android.package-archive");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,PICK_APKS);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=PICK_APKS || resultCode!=RESULT_OK || data==null) return;
        ArrayList<Uri> selected = new ArrayList<>();
        if(data.getClipData()!=null) {
            for(int n=0;n<data.getClipData().getItemCount();n++) selected.add(data.getClipData().getItemAt(n).getUri());
        } else if(data.getData()!=null) selected.add(data.getData());
        int added=0;
        for(Uri u:selected) {
            try {
                ApkItem item=readApk(u);
                if(item!=null && !contains(item.uri)) { items.add(item); added++; }
            } catch(Exception ignored) {}
        }
        save();
        renderList(search==null?"":search.getText().toString());
        Toast.makeText(this,added+" APK ditambahkan",Toast.LENGTH_SHORT).show();
    }

    private boolean contains(String uri) {
        for(ApkItem a:items) if(a.uri.equals(uri)) return true;
        return false;
    }

    private ApkItem readApk(Uri uri) {
        try {
            File dir = new File(getFilesDir(), "apks");
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, "apk_" + Math.abs(uri.toString().hashCode()) + ".apk");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(dest)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            }
            PackageManager pm = getPackageManager();
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= 33)
                info = pm.getPackageArchiveInfo(dest.getAbsolutePath(), PackageManager.PackageInfoFlags.of(0));
            else
                info = pm.getPackageArchiveInfo(dest.getAbsolutePath(), 0);
            if (info == null || info.applicationInfo == null) {
                dest.delete();
                Toast.makeText(this, "File tidak terbaca sebagai APK valid.", Toast.LENGTH_SHORT).show();
                return null;
            }
            info.applicationInfo.sourceDir = dest.getAbsolutePath();
            info.applicationInfo.publicSourceDir = dest.getAbsolutePath();
            String name = String.valueOf(pm.getApplicationLabel(info.applicationInfo));
            String size = android.text.format.Formatter.formatShortFileSize(this, dest.length());
            return new ApkItem(name, info.packageName, size, dest.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membaca APK: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void renderList(String query) {
        if(list==null)return;
        list.removeAllViews();
        int shown=0;
        for(int i=0;i<items.size();i++) {
            final int index=i;
            ApkItem a=items.get(i);
            if(!a.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) &&
               !a.pkg.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            LinearLayout card=new LinearLayout(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(12),dp(10),dp(10),dp(10));
            glass(card,30);
            LinearLayout.LayoutParams cp=lp(-1,76); cp.bottomMargin=dp(10);
            list.addView(card,cp);

            FrameLayout iconBox = new FrameLayout(this);
            iconBox.setBackground(shape(0x337FCBFF,0x668FD8FF,15));
            ImageView icon = new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            try {
                PackageManager pm = getPackageManager();
                PackageInfo pi;
                if (Build.VERSION.SDK_INT >= 33) pi = pm.getPackageArchiveInfo(a.uri, PackageManager.PackageInfoFlags.of(0));
                else pi = pm.getPackageArchiveInfo(a.uri, 0);
                if (pi != null && pi.applicationInfo != null) {
                    pi.applicationInfo.sourceDir = a.uri;
                    pi.applicationInfo.publicSourceDir = a.uri;
                    icon.setImageDrawable(pm.getApplicationIcon(pi.applicationInfo));
                } else icon.setImageDrawable(getDrawable(android.R.drawable.sym_def_app_icon));
            } catch (Exception ex) {
                icon.setImageDrawable(getDrawable(android.R.drawable.sym_def_app_icon));
            }
            iconBox.addView(icon, new FrameLayout.LayoutParams(dp(34),dp(34),Gravity.CENTER));
            card.addView(iconBox,lp(48,48));

            LinearLayout info=vertical();
            info.setPadding(dp(12),0,dp(4),0);
            info.addView(text(a.name,14,white,true));
            info.addView(text(a.pkg,10,muted,false));
            info.addView(text(a.size,9,0xFFB9DFFF,true));
            card.addView(info,new LinearLayout.LayoutParams(0,-2,1));

            TextView open=text("OPEN",9,Color.rgb(12,22,36),true);
            open.setGravity(Gravity.CENTER);
            open.setBackground(shape(0xFFDDF1FF,0,20));
            card.addView(open,lp(54,32));
            open.setOnClickListener(v -> openPackage(a.pkg));

            TextView del=text("×",22,0xFFFFA9B6,true);
            del.setGravity(Gravity.CENTER);
            card.addView(del,lp(34,42));
            del.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                    .setTitle("Hapus APK?")
                    .setMessage("Hapus "+a.name+" dari daftar? File asli tidak dihapus.")
                    .setNegativeButton("BATAL",null)
                    .setPositiveButton("HAPUS",(d,w)->{
                        items.remove(index); save(); renderList(search.getText().toString());
                    }).show());
            shown++;
        }
        countText.setText(items.size()+" APK");
        emptyText.setVisibility(shown==0?View.VISIBLE:View.GONE);
        if(shown>0) emptyText.setVisibility(View.GONE);
    }

    private void openPackage(String pkg) {
        try {
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch!=null) startActivity(launch);
            else Toast.makeText(this,"Aplikasi belum terinstal atau package tidak diketahui.",Toast.LENGTH_LONG).show();
        } catch(Exception e) {
            Toast.makeText(this,"Tidak dapat membuka aplikasi.",Toast.LENGTH_SHORT).show();
        }
    }

    private void save() {
        try {
            JSONArray arr=new JSONArray();
            for(ApkItem a:items) {
                JSONObject o=new JSONObject();
                o.put("name",a.name); o.put("pkg",a.pkg); o.put("size",a.size); o.put("uri",a.uri);
                arr.put(o);
            }
            prefs.edit().putString("items",arr.toString()).apply();
        } catch(Exception ignored){}
    }

    private void loadSaved() {
        try {
            JSONArray arr=new JSONArray(prefs.getString("items","[]"));
            for(int i=0;i<arr.length();i++) {
                JSONObject o=arr.getJSONObject(i);
                items.add(new ApkItem(o.optString("name"),o.optString("pkg"),o.optString("size"),o.optString("uri")));
            }
        } catch(Exception ignored){}
    }
}
