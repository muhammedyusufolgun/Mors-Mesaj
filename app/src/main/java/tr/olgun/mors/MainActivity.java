package tr.olgun.mors;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.hardware.camera2.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    final Handler handler = new Handler(Looper.getMainLooper());
    final int blue=0xff036da5, ink=0xff142d40;
    LinearLayout page;
    EditText input;
    TextView preview,status,received;
    Button send,listen,paste,saveReceived;
    MessageStore store;
    AudioMorse audio;
    boolean soundMode;
    String lastReceived="";
    RadioGroup channel;
    FrameLayout finder;
    TextureView texture;
    Camera camera;
    CameraManager manager;
    String torchId, code="", pending="";
    boolean sending,listening,foreground;
    List<Integer> sequence;
    int index,width,height;
    long started;
    double floor;
    Morse.Decoder decoder;
    int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    TextView label(LinearLayout box,String value,int size,int color) {
        TextView v=new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        box.addView(v); return v;
    }
    LinearLayout card() {
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(1); box.setPadding(dp(16),dp(14),dp(16),dp(14));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(-1); bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1),0xffd7e2e9); box.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(12); page.addView(box,lp); return box;
    }
    Button button(LinearLayout box,String caption) {
        Button b=new Button(this); b.setText(caption); b.setAllCaps(false);
        b.setTextColor(-1); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));
        box.addView(b,new LinearLayout.LayoutParams(-1,-2)); return b;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        manager=(CameraManager)getSystemService(CAMERA_SERVICE);
        getWindow().setStatusBarColor(ink);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        page=new LinearLayout(this); page.setOrientation(1); page.setBackgroundColor(0xfff4f7fa);
        page.setPadding(dp(16),dp(16),dp(16),dp(18)); page.setFocusableInTouchMode(true);
        scroll.addView(page); setContentView(scroll);
        label(page,"Mors Mesaj",28,ink).setTypeface(Typeface.DEFAULT,1);
        store=new MessageStore(this);
        channel=new RadioGroup(this); channel.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(channel);
        RadioButton optical=new RadioButton(this); optical.setText("Işık"); optical.setId(View.generateViewId()); channel.addView(optical);
        RadioButton sonic=new RadioButton(this); sonic.setText("Ses · 700 Hz"); sonic.setId(View.generateViewId()); channel.addView(sonic);
        channel.check(state!=null && state.getBoolean("sound")?sonic.getId():optical.getId());
        soundMode=channel.getCheckedRadioButtonId()==sonic.getId();
        channel.setOnCheckedChangeListener((group,id)->{soundMode=id==sonic.getId(); refresh();});
        LinearLayout tx=card();
        label(tx,"Gönderilecek mesaj",16,ink).setTypeface(Typeface.DEFAULT,1);
        input=new EditText(this); input.setSingleLine(false); input.setMinLines(1); input.setMaxLines(4);
        input.setHint("Mesaj yazın veya yapıştırın"); tx.addView(input);
        preview=label(tx,"",17,blue); preview.setTypeface(Typeface.MONOSPACE);
        preview.setPadding(0,dp(6),0,dp(6));
        LinearLayout edits=new LinearLayout(this); tx.addView(edits);
        paste=smallButton(edits,"Yapıştır"); paste.setOnClickListener(v->pasteText());
        smallButton(edits,"Kaydet").setOnClickListener(v->saveText(input.getText().toString()));
        send=button(tx,"Gönder"); send.setOnClickListener(v->{ if(sending) stop("Gönderim durduruldu."); else request("tx"); });
        LinearLayout rx=card();
        label(rx,"Alınan mesaj",16,ink).setTypeface(Typeface.DEFAULT,1);
        received=label(rx,"Henüz mesaj alınmadı.",20,ink); received.setTextIsSelectable(true);
        finder=new FrameLayout(this); texture=new TextureView(this);
        texture.setSurfaceTextureListener(this); finder.addView(texture);
        View target=new View(this) {
            final Paint pen=new Paint(1);
            protected void onDraw(Canvas c) {
                pen.setStyle(Paint.Style.STROKE); pen.setStrokeWidth(dp(2)); pen.setColor(0xff55ecad);
                float x=getWidth()/2f,y=getHeight()/2f;
                c.drawRect(x-getWidth()*.09f,y-getHeight()*.09f,x+getWidth()*.09f,y+getHeight()*.09f,pen);
            }
        };
        finder.addView(target); rx.addView(finder,new LinearLayout.LayoutParams(-1,dp(180))); finder.setVisibility(View.GONE);
        listen=button(rx,"Kameradan al"); listen.setOnClickListener(v->{if(listening) stop("Alım durduruldu."); else request("rx");});
        saveReceived=button(rx,"Alınan mesajı kaydet");saveReceived.setOnClickListener(v->saveText(lastReceived));
        button(page,"Kayıtlı mesajlar").setOnClickListener(v->showSaved());
        status=label(page,"",15,ink); status.setPadding(dp(2),dp(12),0,0);
        status.setText("Alıcıyı gönderimden önce açın.");
        TextView footer=label(page,"Muhammed Yusuf OLGUN",14,0xff526475);
        footer.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL); footer.setPadding(0,dp(16),0,0);
        footer.setLayoutParams(new LinearLayout.LayoutParams(-1,-2,1));
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int n,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){refresh();}
            public void afterTextChanged(Editable e){}
        });
        if(state!=null){input.setText(state.getString("draft","")); lastReceived=state.getString("last","");if(!lastReceived.isEmpty())received.setText(lastReceived);}
        refresh();
    }
    Button smallButton(LinearLayout row,String text){
        Button b=button(row,text); b.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));return b;
    }
    void pasteText(){
        ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        ClipData clip=clipboard.getPrimaryClip();
        CharSequence text=clip==null||clip.getItemCount()==0?null:clip.getItemAt(0).getText();
        if(TextUtils.isEmpty(text)){status.setText("Panoda yapıştırılacak metin yok.");return;}
        int a=input.getSelectionStart(), b=input.getSelectionEnd();
        if(a<0||b<0)a=b=input.length();
        input.getText().replace(Math.min(a,b),Math.max(a,b),text.toString());input.requestFocus();
    }
    void saveText(String text){
        if(text.trim().isEmpty()){status.setText("Kaydedilecek mesaj yok.");return;}
        store.save(text);status.setText("Mesaj kaydedildi.");
    }
    void showSaved(){
        ArrayList<MessageStore.Entry> rows=store.entries();
        if(rows.isEmpty()){new AlertDialog.Builder(this).setTitle("Kayıtlı mesajlar").setMessage("Henüz kayıt yok.").setPositiveButton("Kapat",null).show();return;}
        String[] names=new String[rows.size()];
        for(int i=0;i<rows.size();i++)names[i]=rows.get(i).text;
        new AlertDialog.Builder(this).setTitle("Kayıtlı mesajlar").setItems(names,(d,n)->{
            MessageStore.Entry entry=rows.get(n);
            AlertDialog.Builder detail=new AlertDialog.Builder(this).setTitle("Kayıtlı mesaj").setMessage(entry.text)
                .setNegativeButton("Kapat",null).setNeutralButton("Sil",(dlg,w)->confirmDelete(entry));
            if(!sending)detail.setPositiveButton("Mesajı kullan",(dlg,w)->{input.setText(entry.text);input.setSelection(input.length());});
            detail.show();
        }).setNegativeButton("Kapat",null).show();
    }
    void confirmDelete(MessageStore.Entry entry){
        new AlertDialog.Builder(this).setTitle("Mesaj silinsin mi? (1/2)").setMessage(entry.text)
            .setNegativeButton("Vazgeç",null).setPositiveButton("Devam",(d,w)->
                new AlertDialog.Builder(this).setTitle("Silmeyi onayla (2/2)").setMessage("Bu mesaj kalıcı olarak silinecek.")
                    .setNegativeButton("Vazgeç",null).setPositiveButton("Evet, sil",(dialog,which)->{
                        store.remove(entry.id);status.setText("Mesaj silindi.");showSaved();
                    }).show()).show();
    }
    void displayReceived(String text){
        lastReceived=text;received.setText(text);saveReceived.setEnabled(!text.isEmpty());
    }
    void refresh(){
        try { code=Morse.encode(input.getText().toString()); preview.setText(code.isEmpty()?"Mors karşılığı":code); }
        catch(IllegalArgumentException e){code="";preview.setText(e.getMessage());}
        send.setEnabled(!listening && (sending || !code.isEmpty())); listen.setEnabled(!sending);
        send.setText(sending?"Gönderimi durdur":"Gönder"); listen.setText(listening?"Alımı durdur":(soundMode?"Mikrofondan al":"Kameradan al"));
        paste.setEnabled(!sending);saveReceived.setEnabled(!lastReceived.isEmpty());
        for(int i=0;i<channel.getChildCount();i++)channel.getChildAt(i).setEnabled(!sending&&!listening);
        input.setEnabled(!sending);
    }
    String permissionFor(String mode){return soundMode?(mode.equals("rx")?Manifest.permission.RECORD_AUDIO:null):Manifest.permission.CAMERA;}
    void request(String mode){
        String permission=permissionFor(mode);
        if(permission!=null && checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
            pending=mode; requestPermissions(new String[]{permission},1); return;
        }
        begin(mode);
    }
    @Override public void onRequestPermissionsResult(int n,String[] p,int[] r){
        super.onRequestPermissionsResult(n,p,r);
        if(n!=1)return;
        if(r.length==0 || r[0]!=PackageManager.PERMISSION_GRANTED){pending="";status.setText((soundMode?"Mikrofon":"Kamera")+" izni gerekli. İzni uygulama ayarlarından verebilirsiniz.");return;}
        if(foreground && !pending.isEmpty()){String mode=pending;pending="";begin(mode);}
    }
    void begin(String mode){
        if(sending||listening)return;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(soundMode){
            audio=new AudioMorse();
            try{
                if(mode.equals("rx")){
                    listening=true;
                    audio.listen(handler,new AudioMorse.Listener(){
                        public void message(String s){displayReceived(s);}
                        public void failed(){stop("Mikrofondan ses okunamadı.");}
                    });
                    status.setText("Dinleniyor · 700 Hz Mors sinyali bekleniyor.");
                }else{
                    audio.openSpeaker();sequence=Morse.pulses(code);index=0;sending=true;
                    status.setText("Sesle gönderiliyor…");pulse.run();
                }
                refresh();
            }catch(RuntimeException e){stop("Ses aygıtı kullanılamıyor.");}
            return;
        }
        if(mode.equals("rx")){
            listening=true; decoder=new Morse.Decoder(); refresh();
            status.setText("Flaşı hedefin içine hizalayın. Ortam ışığı ölçülüyor…");
            finder.setVisibility(View.VISIBLE);
            if(texture.isAvailable())openCamera(); return;
        }
        try {
            for(String id:manager.getCameraIdList()){
                CameraCharacteristics c=manager.getCameraCharacteristics(id);
                if(Boolean.TRUE.equals(c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))){
                    torchId=id;
                    if(Integer.valueOf(CameraCharacteristics.LENS_FACING_BACK).equals(c.get(CameraCharacteristics.LENS_FACING)))break;
                }
            }
            if(torchId==null){stop("Bu telefonda kullanılabilir flaş bulunamadı.");return;}
            sequence=Morse.pulses(code);index=0;sending=true;refresh();
            status.setText("Flaşla gönderiliyor…");pulse.run();
        } catch(Exception e){stop("Flaş kullanılamıyor. Kamerayı kullanan diğer uygulamaları kapatın.");}
    }
    final Runnable pulse=new Runnable(){
        public void run(){
            if(!sending)return;
            try{
                if(index==sequence.size()){stop("Gönderim tamamlandı.");return;}
                int n=sequence.get(index++);if(soundMode)audio.tone(n>0);else manager.setTorchMode(torchId,n>0);
                handler.postDelayed(this,Math.abs(n)*Morse.UNIT);
            }catch(Exception e){stop("Gönderim kesildi.");}
        }
    };
    void openCamera(){
        if(!listening||soundMode||camera!=null)return;
        try{
            Camera.CameraInfo info=new Camera.CameraInfo();
            int id=0;
            for(int i=0;i<Camera.getNumberOfCameras();i++){
                Camera.getCameraInfo(i,info);if(info.facing==Camera.CameraInfo.CAMERA_FACING_BACK){id=i;break;}
            }
            Camera.getCameraInfo(id,info); camera=Camera.open(id);
            Camera.Parameters p=camera.getParameters(); Camera.Size best=null;
            for(Camera.Size s:p.getSupportedPreviewSizes()){
                if(best==null || Math.abs(s.width*s.height-640*480)<Math.abs(best.width*best.height-640*480))best=s;
            }
            width=best.width;height=best.height;p.setPreviewSize(width,height);p.setPreviewFormat(android.graphics.ImageFormat.NV21);
            camera.setParameters(p);camera.setDisplayOrientation(info.orientation);
            camera.setPreviewTexture(texture.getSurfaceTexture());started=SystemClock.elapsedRealtime();floor=-1;
            camera.setErrorCallback((err,cam)->{if(cam==camera)stop("Kamera bağlantısı kesildi.");});
            camera.setPreviewCallback((data,cam)->{if(listening && cam==camera && data!=null)frame(data);});
            camera.startPreview();
        }catch(Exception e){stop("Kamera açılamadı. Diğer kamera uygulamalarını kapatıp tekrar deneyin.");}
    }
    void frame(byte[] data){
        // Take the brightest local patch inside the aiming box, averaging out individual noisy pixels.
        int peak=0;
        for(int y=height*42/100;y<height*58/100-3;y+=3){
            for(int x=width*42/100;x<width*58/100-3;x+=3){
                int i=y*width+x;
                int value=((data[i]&255)+(data[i+2]&255)+(data[i+2*width]&255)+(data[i+2*width+2]&255))/4;
                peak=Math.max(value,peak);
            }
        }
        long now=SystemClock.elapsedRealtime();
        if(floor<0)floor=peak;
        if(now-started<1500){floor+=.18*(peak-floor);return;}
        boolean light=peak>floor+(decoder.lit?10:20);
        if(!light)floor+=.025*(peak-floor);
        int length=decoder.message.length();
        decoder.sample(light,now);
        if(length!=decoder.message.length())displayReceived(decoder.message.toString());
        String signal=decoder.symbols.toString();
        String hint=light?"Işık algılandı":(signal.isEmpty()?"Hazır · Flaşı hedefin ortasında tutun.":"Alınıyor · "+signal);
        if(!hint.contentEquals(status.getText()))status.setText(hint);
    }
    void stop(String note){
        handler.removeCallbacks(pulse);
        if(audio!=null){audio.close();audio=null;}
        if(torchId!=null){try{manager.setTorchMode(torchId,false);}catch(Exception ignored){}torchId=null;}
        sending=false;listening=false;
        if(camera!=null){Camera old=camera;camera=null;try{old.setPreviewCallback(null);old.stopPreview();}catch(RuntimeException ignored){}finally{old.release();}}
        finder.setVisibility(View.GONE);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);refresh();status.setText(note);
    }
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("draft",input.getText().toString());b.putString("last",lastReceived);b.putBoolean("sound",soundMode);}
    @Override protected void onResume(){super.onResume();foreground=true;if(!pending.isEmpty() && checkSelfPermission(permissionFor(pending))==PackageManager.PERMISSION_GRANTED){String mode=pending;pending="";begin(mode);}}
    @Override protected void onDestroy(){store.close();super.onDestroy();}
    @Override protected void onPause(){foreground=false;if(sending||listening)stop("İşlem durduruldu.");super.onPause();}
    public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){openCamera();}
    public boolean onSurfaceTextureDestroyed(SurfaceTexture s){if(listening&&!soundMode)stop("Alım durduruldu.");return true;}
    public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){}
    public void onSurfaceTextureUpdated(SurfaceTexture s){}
}
