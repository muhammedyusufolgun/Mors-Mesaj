package tr.olgun.mors;

final class LightDetector {
    private double floor=-1;
    boolean isLit(byte[] data,int width,int height,boolean calibrating,boolean previouslyLit){
        // Take the brightest local patch inside the aiming box, averaging out individual noisy pixels.
        int peak=0;
        for(int y=height*42/100;y<height*58/100-3;y+=3){
            for(int x=width*42/100;x<width*58/100-3;x+=3){
                int i=y*width+x;
                int value=((data[i]&255)+(data[i+2]&255)+(data[i+2*width]&255)+(data[i+2*width+2]&255))/4;
                peak=Math.max(value,peak);
            }
        }
        if(floor<0)floor=peak;
        if(calibrating){floor+=.18*(peak-floor);return false;}
        boolean lit=peak>floor+(previouslyLit?10:20);
        if(!lit)floor+=.025*(peak-floor);
        return lit;
    }
}
