package net.trystram.scaletest.httpReader;

import java.io.IOException;

public class Application {

    public static void main(String args[]) throws IOException {

        try(final Reader app = new Reader(Config.fromEnv())){
            app.run();
        }

    }
}
