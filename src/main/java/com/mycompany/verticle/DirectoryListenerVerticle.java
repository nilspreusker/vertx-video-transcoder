package com.mycompany.verticle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.model.NewFileEventDto;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

/**
 * This verticle registers an event handler which will be notified when the startup process is complete. The verticle
 * then starts to watch the file inbox for new files. When a file arrives, an event with the file name is sent to the
 * event bus.
 *
 * @author Nils Preusker - n.preusker@gmail.com - http://www.nilspreusker.de
 */
public class DirectoryListenerVerticle extends Verticle {

    /**
     * The event that this verticle publishes to the event bus whenever a new file is detected in the
     * watched directory.
     */
    public static final String EVENT_FILE_RECEIVED = "com.mycompany.event.fileReceived";

    /**
     * Jackson ObjectMapper to convert POJOs into JSON strings and vice versa.
     */
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void start() {
        vertx.eventBus().registerHandler(MainVerticle.EVENT_STARTUP_COMPLETE, new Handler<Message>() {
            @Override
            public void handle(Message message) {
                try {
                    WatchService watcher = FileSystems.getDefault().newWatchService();
                    Path dir = Paths.get(container.config().getString(MainVerticle.CONFIG_PROPERTY_IN_BOX_PATH));
                    WatchKey key = dir.register(watcher, ENTRY_CREATE);
                    while (true) {
                        key = watcher.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            // Context for directory entry event is the file name of entry
                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            Path name = ev.context();
                            Path child = dir.resolve(name);
                            if (event.kind().equals(ENTRY_CREATE)) {
                                NewFileEventDto newFileEvent = new NewFileEventDto();
                                newFileEvent.setFileName(child.getFileName().toString());
                                // Publish the file received event to the event bus
                                vertx.eventBus().send(EVENT_FILE_RECEIVED, objectMapper
                                        .writeValueAsString(newFileEvent));
                            }
                        }
                        key.reset();
                    }
                } catch (Exception e) {
                    container.logger().error(e.getClass().getName() + " occurred during execution of "
                            + DirectoryListenerVerticle.class.getSimpleName() + ". Message was '" + e.getMessage(), e);
                }
            }
        });
        container.logger().info(DirectoryListenerVerticle.class.getSimpleName() + " started successfully.");
    }
}
