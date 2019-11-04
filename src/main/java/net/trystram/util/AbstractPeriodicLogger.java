package net.trystram.util;

public abstract class AbstractPeriodicLogger {

    private int interval = 10;
    long timeMarker;

    long lastValue = 0;
    long lastErrors=0;

    public void log(long value, long totalErrors){
         if (intervalCheck()) {
             write(value, totalErrors);
             lastValue = value;
             lastErrors = totalErrors;
         }
    }

    public abstract void write(long value, long errors);

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

    public int getInterval() {
       return interval;
    }
}
