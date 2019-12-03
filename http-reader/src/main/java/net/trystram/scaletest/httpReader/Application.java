package net.trystram.scaletest.httpReader;

public class Application {

    public static void main(String args[]) throws Exception {

        try(final Reader app = new Reader(Config.fromEnv())){
            app.run();
        }

    }
}
