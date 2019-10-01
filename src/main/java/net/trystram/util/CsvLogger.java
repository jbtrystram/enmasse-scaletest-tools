package net.trystram.util;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

/**
 * A simple CSV writer that write a new line with the current timestamp as the first column.
 */
public class CsvLogger extends AbstractPeriodicLogger {

    private Buffer buffer;
    private char separator = ',';
    private String filename;
    private long beginTime;
    private Vertx vertx;

//todo : multiples files at the same time, autodetect and adjust the filename.
    public CsvLogger(Vertx vertx, String filename) {

        this.filename = filename;
        this.vertx = vertx;
        this.beginTime = System.currentTimeMillis();

        buffer = Buffer.buffer();
    }

    @Override
    public void write(String value){
        long time = System.currentTimeMillis() - beginTime;
        System.out.println(String.format("%d%c%s\n", time, separator, value));
        buffer.appendString(String.format("%d%c%s\n", time, separator, value));
    }

    public void saveFile() {
        vertx.fileSystem().writeFileBlocking(filename, buffer);
    }

    public CsvLogger setSeparator(char separator) {
        this.separator = separator;
        return this;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }
}