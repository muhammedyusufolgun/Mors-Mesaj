package tr.olgun.mors;

import android.media.*;
import android.os.Handler;

final class AudioMorse {
    static final int RATE=16000, WINDOW=320;
    interface Listener {void message(String text); void failed();}
    private volatile boolean running;
    private AudioRecord recorder;
    private AudioTrack speaker;
    void openSpeaker() {
        short[] wave=new short[1600];
        for(int i=0;i<wave.length;i++) wave[i]=(short)(9000*Math.sin(2*Math.PI*700*i/RATE));
        speaker=new AudioTrack(AudioManager.STREAM_MUSIC,RATE,AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,wave.length*2,AudioTrack.MODE_STATIC);
        if(speaker.write(wave,0,wave.length)!=wave.length || speaker.setLoopPoints(0,wave.length,-1)!=AudioTrack.SUCCESS)
            throw new IllegalStateException();
    }
    void tone(boolean on){if(on)speaker.play();else speaker.pause();}
    void listen(Handler ui,Listener listener) {
        int min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0)throw new IllegalStateException();
        recorder=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,RATE,AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,Math.max(4096,min*2));
        recorder.startRecording();
        if(recorder.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException();
        final AudioRecord source=recorder;
        running=true;
        new Thread(()->{
            Morse.Decoder decoder=new Morse.Decoder(); short[] data=new short[WINDOW];
            long samples=0;
            try {
                while(running){
                    int offset=0;
                    while(running && offset<WINDOW){
                        int n=source.read(data,offset,WINDOW-offset);
                        if(n<=0)throw new IllegalStateException();
                        offset+=n;
                    }
                    if(!running)break;
                    int previous=decoder.message.length();
                    decoder.sample(ToneDetector.present(data,decoder.lit),samples*1000/RATE);
                    samples+=offset;
                    if(decoder.message.length()!=previous){
                        String result=decoder.message.toString();
                        ui.post(()->{if(running)listener.message(result);});
                    }
                }
            }catch(RuntimeException e){ui.post(()->{if(running)listener.failed();});}
        },"Mors mikrofon").start();
    }
    void close(){
        running=false;
        if(recorder!=null){try{recorder.stop();}catch(IllegalStateException ignored){}recorder.release();recorder=null;}
        if(speaker!=null){speaker.release();speaker=null;}
    }
}
