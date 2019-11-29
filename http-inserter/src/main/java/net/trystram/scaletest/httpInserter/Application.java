package net.trystram.scaletest.httpInserter;

import java.io.IOException;

public class Application {

    public static void main(String args[]) throws IOException {

        try (final Creater app = new Creater(Config.fromEnv())) {
            app.run();
        }

    }
}
