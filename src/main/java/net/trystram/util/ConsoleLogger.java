package net.trystram.util;

/**
 * A simple console logger.
 */
public class ConsoleLogger extends AbstractPeriodicLogger {

    private long beginTime;

    public ConsoleLogger() {

        this.beginTime = System.currentTimeMillis();
    }

    @Override
    public void write(long value, long errors){

        long elapsedSecs = Math.floorDiv(System.currentTimeMillis() - beginTime, 1000);
        long secs = elapsedSecs % 60;
        long min = elapsedSecs / 60;
        System.out.println(String.format("Running for %dm%ds, Total insertions: %d, %s in the last 10s. %d errors.",
                min, secs, value, value-lastValue, errors));
        }
}