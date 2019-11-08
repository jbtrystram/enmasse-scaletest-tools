package net.trystram.util;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import java.util.Date;

/**
 * A simple CSV writer that write a new line with the current timestamp as the first column.
 */
public class CsvLogger extends AbstractPeriodicLogger {

    private Buffer buffer;
    private char delimiter = ',';
    private String filename;
    private Vertx vertx;

//todo : multiples files at the same time, autodetect and adjust the filename.
    public CsvLogger(Vertx vertx, String filename) {

        this.filename = filename;
        this.vertx = vertx;
        this.buffer = Buffer.buffer();
        this.timeMarker = System.currentTimeMillis();

        verifyFileAvailability();
        buffer.appendString(String.join(String.valueOf(delimiter),
                "time", "insertions", "insertions/s", "errors", "errors/s"))
                .appendString("\n");
    }

    @Override
    public void write(long value, long errors){
        String time = new Date(System.currentTimeMillis()).toString();
        buffer.appendString(String.join(String.valueOf(delimiter),
                time,
                String.valueOf(value),
                String.valueOf((value-lastValue)/(double)getInterval()),
                String.valueOf(errors),
                String.valueOf((errors-lastErrors)/(double)getInterval())))
                .appendString("\n");
    }

    public void saveFile() {
        vertx.fileSystem().writeFileBlocking(filename, buffer);
    }

    public CsvLogger setDelimiter(char delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    private void verifyFileAvailability(){
        int count = 1;
        String newfilename = filename;
        String barefilename = getFilenameWithoutExtension();
        while(vertx.fileSystem().existsBlocking(newfilename)){
            newfilename = barefilename.concat(String.valueOf(count)).concat(".csv");
            count++;
        }

        filename = newfilename;
        System.out.println("Data will be saved in "+newfilename);
    }

    private String getFilenameWithoutExtension(){
        int ext = filename.lastIndexOf(".csv");
        return filename.substring(0, ext);
    }
}