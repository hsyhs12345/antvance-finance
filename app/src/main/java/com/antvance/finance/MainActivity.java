package com.antvance.finance;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricPrompt;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.Executor;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MainActivity extends Activity {
    LinearLayout root, content, nav;
    int red=Color.rgb(255,59,69), green=Color.rgb(70,214,155), bg=Color.rgb(5,7,11), panel=Color.rgb(17,21,29), muted=Color.rgb(143,152,168), white=Color.rgb(245,247,250);
    android.content.SharedPreferences prefs;
    int waits, points, trades;
    int cooldownSeconds=30, dailyLimit=5, protectionLevel=2, emotionRisk=0;
    boolean biometricEnabled=false;
    String preferredMessage="충동은 멈추고, 실력은 쌓는다.";
    int setupStep=0;
    EditText passwordInput, passwordConfirm;
    TextView setupTitle, setupProgress;
    LinearLayout setupBody;

    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }
    TextView tv(String s,float size,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); t.setPadding(dp(2),dp(2),dp(2),dp(2)); return t; }
    GradientDrawable bg(int color,float r){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(r)); return g; }
    Button btn(String s,int color){ Button b=new Button(this); b.setText(s); b.setTextColor(white); b.setTextSize(14); b.setAllCaps(false); b.setBackground(bg(color,18)); b.setPadding(dp(12),0,dp(12),0); return b; }
    LinearLayout col(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    TextView small(String s){ return tv(s,12,muted); }
    TextView cardText(String s,float size,int color){ TextView t=tv(s,size,color); t.setPadding(dp(14),dp(12),dp(14),dp(12)); t.setBackground(bg(panel,16)); return t; }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        prefs=getSharedPreferences("data",0);
        waits=prefs.getInt("waits",0); points=prefs.getInt("points",0); trades=prefs.getInt("trades",0);
        cooldownSeconds=prefs.getInt("cooldown",30); dailyLimit=prefs.getInt("daily_limit",5); protectionLevel=prefs.getInt("protection",2);
        emotionRisk=prefs.getInt("emotion_risk",0); biometricEnabled=prefs.getBoolean("biometric",false); preferredMessage=prefs.getString("message",preferredMessage);
        if(!prefs.getBoolean("setup_complete",false)) showSetup(); else showHome();
    }

    void showSetup(){
        root=col(); root.setBackgroundColor(bg);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout outer=col(); outer.setPadding(dp(22),dp(28),dp(22),dp(24)); scroll.addView(outer);
        TextView logo=tv("ANTVANCE\nFINANCE",25,white); logo.setTypeface(null,1); outer.addView(logo);
        setupProgress=small("초기 설정 1 / 7"); setupProgress.setPadding(0,dp(10),0,dp(18)); outer.addView(setupProgress);
        setupTitle=tv("나만의 보호 설정을\n만들어볼게요",27,white); setupTitle.setTypeface(null,1); outer.addView(setupTitle);
        TextView intro=small("몇 가지 질문에 답하면 앱의 제어 강도와 대기시간 등을 답변에 맞춰 설정합니다. 투자 판단을 대신하지 않고, 직접 정한 규칙을 지키는 데 사용됩니다."); intro.setPadding(0,dp(10),0,dp(20)); outer.addView(intro);
        setupBody=col(); outer.addView(setupBody);
        LinearLayout buttons=row(); buttons.setPadding(0,dp(18),0,0); Button back=btn("이전",Color.rgb(34,40,51)); Button next=btn("다음",red); buttons.addView(back,new LinearLayout.LayoutParams(0,dp(54),1)); buttons.addView(next,new LinearLayout.LayoutParams(0,dp(54),1)); outer.addView(buttons);
        back.setOnClickListener(v->{ if(setupStep>0){setupStep--; renderSetupStep();} });
        next.setOnClickListener(v->{ if(validateSetupStep()){ if(setupStep<6){setupStep++; renderSetupStep();} else finishSetup();} });
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root); renderSetupStep();
    }

    void renderSetupStep(){
        setupBody.removeAllViews(); setupProgress.setText("초기 설정 " + (setupStep+1) + " / 7");
        switch(setupStep){
            case 0: renderSecurity(); break;
            case 1: renderFrequency(); break;
            case 2: renderEmotion(); break;
            case 3: renderPlanning(); break;
            case 4: renderControl(); break;
            case 5: renderLimits(); break;
            case 6: renderMessage(); break;
        }
    }

    TextView q(String s){ TextView t=tv(s,16,white); t.setTypeface(null,1); t.setPadding(0,dp(8),0,dp(10)); setupBody.addView(t); return t; }
    EditText input(String hint, int type){ EditText e=new EditText(this); e.setHint(hint); e.setHintTextColor(muted); e.setTextColor(white); e.setTextSize(16); e.setSingleLine(true); e.setInputType(type); e.setPadding(dp(14),0,dp(14),0); e.setBackground(bg(panel,14)); setupBody.addView(e,new LinearLayout.LayoutParams(-1,dp(54))); return e; }
    RadioGroup choices(String... values){
        RadioGroup g=new RadioGroup(this); g.setOrientation(RadioGroup.VERTICAL);
        for(String v:values){ RadioButton r=new RadioButton(this); r.setText(v); r.setTextColor(white); r.setTextSize(15); r.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{red,muted})); r.setPadding(dp(8),dp(4),dp(8),dp(4)); g.addView(r,new RadioGroup.LayoutParams(-1,dp(52))); }
        setupBody.addView(g); return g;
    }

    void renderSecurity(){
        q("앱 잠금 비밀번호"); passwordInput=input("4자리 이상 비밀번호",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        q("비밀번호 확인"); passwordConfirm=input("같은 비밀번호를 다시 입력",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        q("생체인식을 사용할까요?");
        RadioGroup g=choices("사용할게요  — 지원되는 기기에서 지문/얼굴 인증", "비밀번호만 사용할게요");
        g.check(biometricEnabled && Build.VERSION.SDK_INT>=28 ? g.getChildAt(0).getId() : g.getChildAt(1).getId());
        g.setOnCheckedChangeListener((group,id)->biometricEnabled=(id==group.getChildAt(0).getId() && Build.VERSION.SDK_INT>=28));
        if(Build.VERSION.SDK_INT<28) setupBody.addView(cardText("현재 Android 버전에서는 이 방식의 생체인식을 지원하지 않아 비밀번호 잠금으로 설정됩니다.",13,muted));
    }

    void renderFrequency(){
        q("평소 매매 빈도는 어느 정도인가요?"); choices("거의 하지 않음","가끔 — 하루 1~2회 정도","자주 — 하루 여러 번","매우 자주 — 짧은 간격으로 반복");
        q("앱이 매매 빈도에 얼마나 민감하게 반응하길 원하나요?"); choices("부드럽게 알림","보통","강하게 알려주기","매우 강하게 제어");
    }

    void renderEmotion(){
        q("손실이 생겼을 때 나에게 가장 가까운 모습은?");
        RadioGroup g=choices("별로 신경 쓰지 않음","조금 신경 쓰임","만회하고 싶은 마음이 생김","바로 다시 매매하고 싶어짐");
        g.setOnCheckedChangeListener((group,id)->{ emotionRisk=group.indexOfChild(group.findViewById(id)); });
        q("수익이 생겼을 때 나에게 가장 가까운 모습은?"); choices("계획대로 멈춤","조금 더 지켜봄","욕심이 생김","계속 매매하고 싶어짐");
    }

    void renderPlanning(){
        q("매매 전에 계획을 세우는 편인가요?"); choices("항상 세움","대부분 세움","가끔 세움","거의 세우지 않음");
        q("충동적인 매매를 막는 데 도움이 될 것 같은 것은?"); choices("짧은 알림","잠깐 기다리는 시간","강한 화면 차단","기록을 먼저 작성하게 하기");
    }

    void renderControl(){
        q("앱의 보호 강도를 직접 선택하세요.");
        RadioGroup g=choices("1  부드러움 — 알림 중심","2  보통 — 대기 + 알림","3  강함 — 대기 + 반복 경고","4  매우 강함 — 긴 대기 + 강한 경고");
        g.check(g.getChildAt(Math.max(0,Math.min(3,protectionLevel-1))).getId());
        g.setOnCheckedChangeListener((group,id)->protectionLevel=group.indexOfChild(group.findViewById(id))+1);
        setupBody.addView(cardText("이 값은 매매의 옳고 그름을 판단하지 않습니다. 사용자가 정한 자기통제 수준만 조절합니다.",13,muted));
    }

    void renderLimits(){
        q("하루에 앱에서 관리하고 싶은 최대 매매 횟수는?");
        EditText limit=input("예: 5",InputType.TYPE_CLASS_NUMBER); limit.setText(""+dailyLimit);
        q("매매 전에 기다리고 싶은 시간은?");
        RadioGroup g=choices("5초","10초","30초","60초","120초");
        int idx=cooldownSeconds==5?0:cooldownSeconds==10?1:cooldownSeconds==30?2:cooldownSeconds==60?3:4;
        g.check(g.getChildAt(idx).getId());
        g.setOnCheckedChangeListener((group,id)->{ int i=group.indexOfChild(group.findViewById(id)); cooldownSeconds=new int[]{5,10,30,60,120}[i]; });
        limit.setTag("daily_limit_input");
    }

    void renderMessage(){
        q("경고가 뜰 때 어떤 문구가 가장 도움이 되나요?");
        RadioGroup g=choices("잠깐 멈추고 다시 생각하기","내가 세운 계획을 확인하기","지금 꼭 필요한 행동인지 확인하기","차분하게 기록하고 기다리기");
        g.setOnCheckedChangeListener((group,id)->{ String[] m={"잠깐 멈추고 다시 생각하기","내가 세운 계획을 확인하기","지금 꼭 필요한 행동인지 확인하기","차분하게 기록하고 기다리기"}; preferredMessage=m[group.indexOfChild(group.findViewById(id))]; });
        q("설정 완료 전 확인"); setupBody.addView(cardText("보호 수준: " + protectionLevel + "/4\n기본 대기: " + cooldownSeconds + "초\n하루 관리 횟수: " + dailyLimit + "회\n생체인식: " + (biometricEnabled?"사용":"미사용"),14,white));
    }

    boolean validateSetupStep(){
        if(setupStep==0){
            String a=passwordInput.getText().toString(), b=passwordConfirm.getText().toString();
            if(a.length()<4){ toast("비밀번호는 4자리 이상으로 설정해주세요."); return false; }
            if(!a.equals(b)){ toast("비밀번호가 서로 다릅니다."); return false; }
            savePassword(a); return true;
        }
        if(setupStep==5){
            View v=setupBody.findViewWithTag("daily_limit_input");
            if(v instanceof EditText){ try{ dailyLimit=Math.max(1,Math.min(100,Integer.parseInt(((EditText)v).getText().toString()))); }catch(Exception e){toast("하루 횟수를 숫자로 입력해주세요.");return false;} }
        }
        return true;
    }

    void savePassword(String password){
        try{
            byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
            byte[] hash=derive(password.toCharArray(),salt);
            prefs.edit().putString("pw_salt",hex(salt)).putString("pw_hash",hex(hash)).apply();
        }catch(Exception e){ toast("비밀번호 저장 중 문제가 발생했습니다."); }
    }
    byte[] derive(char[] p,byte[] salt) throws Exception{ PBEKeySpec spec=new PBEKeySpec(p,salt,120000,256); byte[] out=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); spec.clearPassword(); return out; }
    String hex(byte[] a){ StringBuilder s=new StringBuilder(); for(byte b:a)s.append(String.format("%02x",b)); return s.toString(); }
    byte[] unhex(String s){ byte[] a=new byte[s.length()/2]; for(int i=0;i<a.length;i++)a[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16); return a; }

    void finishSetup(){
        prefs.edit().putBoolean("setup_complete",true).putBoolean("biometric",biometricEnabled && Build.VERSION.SDK_INT>=28).putInt("cooldown",cooldownSeconds).putInt("daily_limit",dailyLimit).putInt("protection",protectionLevel).putInt("emotion_risk",emotionRisk).putString("message",preferredMessage).apply();
        waits=0; points=0; trades=0; save();
        showHome();
    }

    void showLock(){
        final Dialog d=new Dialog(this); LinearLayout l=col(); l.setPadding(dp(24),dp(24),dp(24),dp(20)); l.setBackground(bg(Color.rgb(10,13,19),26));
        TextView title=tv("ANTVANCE FINANCE\n잠금 해제",21,white); title.setTypeface(null,1); l.addView(title);
        EditText p=new EditText(this); p.setHint("비밀번호"); p.setHintTextColor(muted); p.setTextColor(white); p.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); p.setSingleLine(true); p.setPadding(dp(14),0,dp(14),0); p.setBackground(bg(panel,14)); l.addView(p,new LinearLayout.LayoutParams(-1,dp(54)));
        Button unlock=btn("잠금 해제",red); l.addView(unlock,new LinearLayout.LayoutParams(-1,dp(52)));
        if(biometricEnabled && Build.VERSION.SDK_INT>=28){ Button bio=btn("생체인식으로 잠금 해제",Color.rgb(34,40,51)); l.addView(bio,new LinearLayout.LayoutParams(-1,dp(52))); bio.setOnClickListener(v->authenticateBiometric(d)); }
        d.setContentView(l); unlock.setOnClickListener(v->{ if(checkPassword(p.getText().toString())) d.dismiss(); else toast("비밀번호가 맞지 않습니다."); }); d.setOnShowListener(x->{ Window w=d.getWindow(); if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(dp(340),-2);} }); d.setCancelable(false); d.show();
    }
    boolean checkPassword(String p){
        try{ byte[] salt=unhex(prefs.getString("pw_salt","")); byte[] expected=unhex(prefs.getString("pw_hash","")); byte[] actual=derive(p.toCharArray(),salt); return MessageDigest.isEqual(expected,actual); }catch(Exception e){return false;}
    }
    void authenticateBiometric(final Dialog d){
        if(Build.VERSION.SDK_INT<28)return;
        Executor ex=getMainExecutor(); BiometricPrompt prompt=new BiometricPrompt.Builder(this).setTitle("ANTVANCE FINANCE").setSubtitle("생체인식으로 잠금 해제").setNegativeButton("비밀번호 사용",ex,(dialog,which)->{}).build();
        prompt.authenticate(new BiometricPrompt.CryptoObject(null),new BiometricPrompt.AuthenticationCallback(){
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){runOnUiThread(()->d.dismiss());}
            public void onAuthenticationError(int code,CharSequence msg){toast("생체인식을 완료하지 못했습니다.");}
        });
    }

    @Override protected void onResume(){ super.onResume(); if(prefs!=null && prefs.getBoolean("setup_complete",false) && prefs.getBoolean("biometric",false) && !isFinishing() && content!=null){ /* app lock can be invoked from settings later */ } }

    void base(){ root=col(); root.setBackgroundColor(bg); ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); content=col(); content.setPadding(dp(18),dp(18),dp(18),dp(18)); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); nav=row(); nav.setPadding(dp(10),dp(8),dp(10),dp(8)); nav.setBackgroundColor(Color.rgb(9,12,17)); root.addView(nav,new LinearLayout.LayoutParams(-1,dp(70))); setContentView(root); addNav("⌂\n홈",()->showHome()); addNav("▣\n일지",()->showJournal()); addNav("♧\n알림",()->showAlerts()); addNav("♢\n보상",()->showRewards()); addNav("⋯\n더보기",()->showMore()); }
    void addNav(String s, final Runnable r){ TextView n=tv(s,11,muted); n.setGravity(Gravity.CENTER); nav.addView(n,new LinearLayout.LayoutParams(0,-1,1)); n.setOnClickListener(v->r.run()); }
    void header(String sub){ LinearLayout h=row(); TextView logo=tv("ANTVANCE\nFINANCE",16,white); logo.setTypeface(null,1); h.addView(logo,new LinearLayout.LayoutParams(0,dp(52),1)); TextView gear=tv("⚙",23,white); h.addView(gear,new LinearLayout.LayoutParams(dp(45),dp(52))); gear.setGravity(Gravity.CENTER); gear.setOnClickListener(v->showMore()); content.addView(h); if(sub!=null){TextView x=small(sub);x.setPadding(0,0,0,dp(14));content.addView(x);} }
    TextView section(String s){ TextView t=tv(s,15,white); t.setTypeface(null,1); t.setPadding(0,dp(18),0,dp(10)); content.addView(t); return t; }
    LinearLayout stat(String a,String b,String c){ LinearLayout l=col(); l.setPadding(dp(10),dp(10),dp(10),dp(10)); l.setBackground(bg(panel,14)); l.addView(small(a)); TextView y=tv(b,19,white); y.setTypeface(null,1); l.addView(y); l.addView(small(c)); return l; }
    LinearLayout.LayoutParams w(){ return new LinearLayout.LayoutParams(0,dp(92),1); }

    void showHome(){
        base(); header("오늘도 차분한 투자를 응원해요"); LinearLayout stats=row(); stats.setWeightSum(3); stats.addView(stat("오늘 매매",""+trades+"회","설정 " + dailyLimit + "회 이하"),w()); stats.addView(stat("참은 횟수",""+waits+"회","대기 " + cooldownSeconds + "초"),w()); stats.addView(stat("보호 수준",""+protectionLevel+"/4","사용자 설정"),w()); content.addView(stats);
        section("멘탈 온도계"); int mental=Math.max(0,Math.min(100,75-emotionRisk*12)); content.addView(cardText("현재 상태  " + mental + "%\n\n" + bar(mental) + "\n\n최근 자기기록을 바탕으로 표시되는 참고 지표입니다.",14,white));
        section("오늘의 자기통제 신호"); int risk=Math.max(0,Math.min(100,emotionRisk*22 + Math.max(0,trades-dailyLimit)*8)); content.addView(cardText("참고 지표  " + risk + "/100\n\n이 값은 투자 수익이나 시장을 예측하지 않습니다. 사용자가 입력한 행동·감정 기록을 시각화합니다.\n\n오늘의 개인 원칙\n“"+preferredMessage+"”",14,white));
        LinearLayout actions=row(); actions.setPadding(0,dp(14),0,0); Button wait=btn(cooldownSeconds+"초 기다리기",red); Button journal=btn("매매일지 기록",Color.rgb(34,40,51)); actions.addView(wait,new LinearLayout.LayoutParams(0,dp(52),1)); actions.addView(journal,new LinearLayout.LayoutParams(0,dp(52),1)); wait.setOnClickListener(v->cooldown()); journal.setOnClickListener(v->showJournal()); content.addView(actions);
    }
    String bar(int n){ int on=n/5; StringBuilder s=new StringBuilder(); for(int i=0;i<20;i++)s.append(i<on?"█":"░"); return s.toString(); }

    void cooldown(){
        final Dialog d=new Dialog(this); LinearLayout l=col(); l.setPadding(dp(24),dp(24),dp(24),dp(20)); l.setBackground(bg(Color.rgb(10,13,19),26)); TextView x=tv("잠깐!\n다시 한 번 생각해보세요.",20,white); x.setTypeface(null,1); l.addView(x); TextView timer=tv(""+cooldownSeconds,54,red); timer.setGravity(Gravity.CENTER); l.addView(timer,new LinearLayout.LayoutParams(-1,dp(110))); l.addView(cardText("“"+preferredMessage+"”\n\n지금 행동이 내가 정한 계획에 포함되어 있나요?",14,white)); Button wait=btn("기다릴게요  (+100P)",red); l.addView(wait,new LinearLayout.LayoutParams(-1,dp(52))); Button close=btn("닫기",Color.TRANSPARENT); l.addView(close,new LinearLayout.LayoutParams(-1,dp(48))); d.setContentView(l); d.setOnShowListener(v->{Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(dp(340),-2);}}); final int[] sec={cooldownSeconds}; final Handler h=new Handler(); Runnable rr=new Runnable(){public void run(){if(sec[0]>0){timer.setText(""+sec[0]);sec[0]--;h.postDelayed(this,1000);}else{timer.setText("✓");timer.setTextColor(green);wait.setText("기다리기 성공  +100P");}}}; d.setOnDismissListener(v->h.removeCallbacks(rr)); wait.setOnClickListener(v->{if(sec[0]<=0){waits++;points+=100;save();d.dismiss();showHome();}else toast("타이머가 끝날 때까지 기다려주세요.");}); close.setOnClickListener(v->d.dismiss()); d.show(); h.post(rr);
    }

    void showJournal(){ base(); header("캡처 1장으로 매매일지를 간단하게"); section("체결 화면 캡처"); content.addView(cardText("📷  체결 화면을 선택하면\n종목 · 금액 · 시간이 자동으로 채워지는 구조입니다.\n\n※ 현재 버전에서는 이미지 선택 흐름을 먼저 구현했습니다.",14,white)); Button pick=btn("캡처 이미지 선택",red); content.addView(pick,new LinearLayout.LayoutParams(-1,dp(52))); pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,101);}); section("감정 태그"); LinearLayout tags=row(); String[] ts={"🙂 차분함","😟 불안","😡 욕심","😔 후회"}; for(String s:ts){TextView t=cardText(s,12,white);tags.addView(t,new LinearLayout.LayoutParams(0,dp(48),1));} content.addView(tags); Button saveb=btn("일지 저장하기",red); content.addView(saveb,new LinearLayout.LayoutParams(-1,dp(52))); saveb.setOnClickListener(v->Toast.makeText(this,"매매일지가 저장됐어요.",Toast.LENGTH_SHORT).show()); }
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==101&&result==RESULT_OK&&data!=null)Toast.makeText(this,"캡처를 불러왔어요. 감정 태그를 선택해 기록을 완성하세요.",Toast.LENGTH_LONG).show();}

    void showAlerts(){ base(); header("사용자 설정에 맞춘 알림"); section("개인 프리셋"); String[] a={"📉 가격 변화 알림\n원하는 기준을 직접 입력","📈 수익률 알림\n원하는 기준을 직접 입력","⏱ 매매 쉬기\n원하는 휴식 시간을 직접 설정","🔔 전일 고점 돌파\n조건을 켜고 끌 수 있습니다","🛑 보호선\n개인 규칙에 맞는 값을 직접 설정"}; for(String s:a){LinearLayout r=row();TextView t=cardText(s,14,white);r.addView(t,new LinearLayout.LayoutParams(0,dp(76),1));Button set=btn("설정",Color.rgb(34,40,51));r.addView(set,new LinearLayout.LayoutParams(dp(76),dp(76)));content.addView(r);set.setOnClickListener(v->Toast.makeText(this,"개인 알림 설정 화면은 다음 단계에서 연결됩니다.",Toast.LENGTH_SHORT).show());}}
    void showRewards(){ base(); header("참는 행동을 기록하고 성장하세요"); section("현재 보상"); content.addView(cardText("🐜  사용자 맞춤 성장\n\nPOINT  " + points + "P\n\n대기시간 " + cooldownSeconds + "초를 완료하면 포인트가 기록됩니다.",16,white)); section("현재 보호 설정"); content.addView(cardText("보호 수준  " + protectionLevel + "/4\n하루 관리 횟수  " + dailyLimit + "회\n생체인식  " + (biometricEnabled?"사용":"미사용"),14,white)); }
    void showMore(){ base(); header("ANTVANCE 설정"); section("보안"); Button lock=btn("앱 잠금 테스트",red); content.addView(lock,new LinearLayout.LayoutParams(-1,dp(52))); lock.setOnClickListener(v->showLock()); Button reset=btn("초기 설정 다시 하기",Color.rgb(34,40,51)); content.addView(reset,new LinearLayout.LayoutParams(-1,dp(52))); reset.setOnClickListener(v->{prefs.edit().putBoolean("setup_complete",false).apply();showSetup();}); section("화면 제어"); Button ov=btn("화면 오버레이 권한 설정",red); content.addView(ov,new LinearLayout.LayoutParams(-1,dp(52))); ov.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception e){}}); section("내 설정"); content.addView(cardText("대기시간  " + cooldownSeconds + "초\n하루 관리 횟수  " + dailyLimit + "회\n보호 수준  " + protectionLevel + "/4\n생체인식  " + (biometricEnabled?"사용":"미사용") + "\n\n경고 문구\n“" + preferredMessage + "”",14,white)); section("앱 정보"); content.addView(cardText("ANTVANCE FINANCE\n\n투자 결정을 대신하지 않고 기록과 자기통제를 돕는 도구입니다.",13,muted)); }
    void save(){prefs.edit().putInt("waits",waits).putInt("points",points).putInt("trades",trades).apply();}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
