package tr.olgun.mors;

final class ToneDetector {
    // Measure the share of energy at the transmitter's 700 Hz carrier.
    static final double COEFF=2*Math.cos(2*Math.PI*700/16000);
    static boolean present(short[] frame,boolean active){
        double a=0,b=0,energy=0;
        for(short sample:frame){
            double s=sample+COEFF*a-b;
            b=a;a=s;energy+=(double)sample*sample;
        }
        double carrier=2*(a*a+b*b-COEFF*a*b)/frame.length;
        return carrier>frame.length*(active?45*45:80*80) && carrier>energy*(active?.19:.32);
    }
}
