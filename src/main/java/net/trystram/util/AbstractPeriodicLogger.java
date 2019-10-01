package net.trystram.util;

public abstract class AbstractPeriodicLogger {

    private int interval = 10;
    private long timeMarker;

    long lastValue = 0;

    public void log(long value){
         if (intervalCheck()) {
             write(String.valueOf(value - lastValue));
             lastValue = value;
         }
    }

    public abstract void write(String value);

    private boolean intervalCheck(){
        long now = System.currentTimeMillis();
        if ( now - timeMarker > interval*1000){
            timeMarker = now;
            return true;
        } else {
            return false;
        }
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
