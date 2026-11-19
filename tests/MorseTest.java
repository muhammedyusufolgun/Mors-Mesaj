package tr.olgun.mors;
public class MorseTest {
    public static void main(String[] args) {
        if (!Morse.encode("sos").equals("... --- ...")) throw new AssertionError();
        for (String expected : new String[]{"SOS","ÇĞİÖŞÜ IŞIK","A B 123","MERHABA!","E E","T", "DENEME?"}) {
            String code=Morse.encode(expected);
            Morse.Decoder d=new Morse.Decoder();
            long now=2000;
            for(int p:Morse.pulses(code)) {
                long end=now+Math.abs(p)*Morse.UNIT;
                for(long t=now; t<end; t+=33) d.sample(p>0,t);
                now=end;
            }
            d.sample(false,now); d.sample(false,now+Morse.UNIT*3);
            if (!d.message.toString().equals(expected)) throw new AssertionError(expected+" -> "+d.message);
        }
    }
}
