package tr.olgun.mors;
import java.util.Random;

public class ToneTest {
    public static void main(String[] args){
        Morse.Decoder decoder=new Morse.Decoder();
        long time=0;
        Random noise=new Random(23);
        java.util.List<Integer> pulses=Morse.pulses(Morse.encode("SOS IŞIK"));
        pulses.add(-7);
        for(int pulse:pulses){
            for(int ms=0;ms<Math.abs(pulse)*Morse.UNIT;ms+=20){
                short[] frame=new short[320];
                for(int i=0;i<frame.length;i++){
                    double angle=2*Math.PI*i/16000;
                    frame[i]=(short)((pulse>0?750*Math.sin(700*angle):0)+160*Math.sin(1100*angle)+noise.nextGaussian()*80);
                }
                decoder.sample(ToneDetector.present(frame,decoder.lit),time);
                time+=20;
            }
        }
        if(!decoder.message.toString().equals("SOS IŞIK"))throw new AssertionError(decoder.message.toString());
    }
}
