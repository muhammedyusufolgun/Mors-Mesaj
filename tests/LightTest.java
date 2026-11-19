package tr.olgun.mors;
import java.util.Arrays;
import java.util.List;

public class LightTest {
    public static void main(String[] args){
        LightDetector detector=new LightDetector();
        Morse.Decoder decoder=new Morse.Decoder();
        byte[] pixels=new byte[160*120];
        Arrays.fill(pixels,(byte)60);
        detector.isLit(pixels,160,120,true,false);
        List<Integer> pulses=Morse.pulses(Morse.encode("IŞIK SOS"));
        pulses.add(-7);
        long now=2000;
        for(int pulse:pulses){
            for(int y=56;y<64;y++)Arrays.fill(pixels,y*160+76,y*160+84,(byte)(pulse>0?205:60));
            for(int elapsed=0;elapsed<Math.abs(pulse)*Morse.UNIT;elapsed+=30){
                decoder.sample(detector.isLit(pixels,160,120,false,decoder.lit),now);
                now+=30;
            }
        }
        if(!decoder.message.toString().equals("IŞIK SOS"))throw new AssertionError(decoder.message);
    }
}
