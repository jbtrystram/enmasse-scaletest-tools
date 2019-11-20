package net.trystram.scaletest.httpInserter;

import io.vertx.core.Future;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(
            Application.class);

    public static void main(String args[]) {

        Optional<String> configPath = args.length > 0 ? Optional.ofNullable(args[0]) : Optional.empty();

        Creater app = new Creater(configPath.orElse("/etc/config/config.yaml"));

        Future<Void> startPromise = app.configure().compose(config -> {
                    Future<Void> runFuture = Future.future();
                    return app.run(runFuture);
                });

         startPromise.setHandler(res -> {
             if(res.succeeded()){
                     System.exit(0);
            } else {
                log.error("Failure ",res.cause());
                System.exit(1);
            }
        });
    }
}
