package com.mycompany.verticle;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.Verticle;

/**
 * This is the main verticle which bootstraps the video transcoder. It first deploys the directory listener verticle,
 * then it deploys the ffmpeg verticle and finally publishes an event to the event bus, so the directory listener can
 * start watching the file inbox.
 *
 * @author Nils Preusker - n.preusker@gmail.com - http://www.nilspreusker.de
 */
public class MainVerticle extends Verticle {

    /**
     * The event that is published once the startup procedure is complete.
     */
    public static final String EVENT_STARTUP_COMPLETE = "com.mycompany.event.startupComplete";

    /**
     * Config property identifier for the path of the file inbox. This property is used by multiple verticles so it
     * is defined in the main verticle.
     */
    public static final String CONFIG_PROPERTY_IN_BOX_PATH = "inBoxPath";

    public void start() {
        // Make sure all required configuration properties are present
        if (!container.config().containsField(MainVerticle.CONFIG_PROPERTY_IN_BOX_PATH)) {
            throw new RuntimeException("Missing configuration property '" +
                    MainVerticle.CONFIG_PROPERTY_IN_BOX_PATH + "'!");
        }

        container.logger().info("Deploying " + DirectoryListenerVerticle.class.getSimpleName());
        container.deployWorkerVerticle(DirectoryListenerVerticle.class.getCanonicalName(), container.config(), 1,
                false, new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                if (asyncResult.succeeded()) {
                    // Deployment of directory listener succeeded, we can deploy ffmpeg verticle
                    container.logger().info("Deploying " + FFmpegVerticle.class.getSimpleName());
                    container.deployWorkerVerticle(FFmpegVerticle.class.getCanonicalName(), container.config(), 1,
                            false, new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> asyncResult) {
                            if (asyncResult.succeeded()) {
                                // Deployment of FFmpeg verticle succeeded, we can start watching the inbox for files
                                vertx.eventBus().publish(EVENT_STARTUP_COMPLETE, true);
                                container.logger().info("-------------------------------------------------");
                                container.logger().info("- Vert.x Video Transcoder successfully started! -");
                                container.logger().info("-------------------------------------------------");
                            } else {
                                asyncResult.cause().printStackTrace();
                            }
                        }
                    });
                } else {
                    asyncResult.cause().printStackTrace();
                }
            }
        });
        container.logger().info(MainVerticle.class.getSimpleName() + " started successfully.");
    }
}
