package com.mycompany.verticle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.model.NewFileEventDto;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Nils Preusker - n.preusker@gmail.com - http://www.nilspreusker.de
 */
public class FFmpegVerticle extends Verticle {

    /**
     * Config property to determine where the ffmpeg executable is located.
     */
    private static final String CONFIG_PROPERTY_FFMPEGPATH = "ffmpegPath";

    /**
     * Config property to determine which directory to place the output files in.
     */
    private static final String CONFIG_PROPERTY_OUT_BOX_PATH = "outBoxPath";

    /**
     * Jackson ObjectMapper to transform POJOs into JSON strings and vice versa.
     */
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void start() {

        // Make sure all configuration properties required by this verticle are present
        if (!container.config().containsField(CONFIG_PROPERTY_FFMPEGPATH)) {
            throw new RuntimeException("Missing configuration property '" + CONFIG_PROPERTY_FFMPEGPATH + "'!");
        }
        if (!container.config().containsField(CONFIG_PROPERTY_OUT_BOX_PATH)) {
            throw new RuntimeException("Missing configuration property '" + CONFIG_PROPERTY_OUT_BOX_PATH + "'!");
        }

        // register an event listener to be notified when a new file is present
        vertx.eventBus().registerHandler(DirectoryListenerVerticle.EVENT_FILE_RECEIVED, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> fileReceivedEvent) {

                container.logger().info("Received file-received-event: " + fileReceivedEvent.body());

                BufferedReader reader = null;
                try {
                    NewFileEventDto newFileEvent = objectMapper.readValue(fileReceivedEvent.body(), NewFileEventDto.class);
                    // TODO: here we could add validations, check if the path actually points to a file, check if the
                    // file is a video file etc.

                    String ffmpegExecutable = container.config().getString(CONFIG_PROPERTY_FFMPEGPATH);
                    String inBoxPath = container.config().getString(MainVerticle.CONFIG_PROPERTY_IN_BOX_PATH);
                    inBoxPath = inBoxPath.endsWith("/") ? inBoxPath : inBoxPath + "/";
                    String outBoxPath = container.config().getString(CONFIG_PROPERTY_OUT_BOX_PATH);
                    outBoxPath = outBoxPath.endsWith("/") ? outBoxPath : outBoxPath + "/";

                    ProcessBuilder builder = new ProcessBuilder(ffmpegExecutable, "-i",
                            inBoxPath + newFileEvent.getFileName(), "-f", "mp4",
                            "-c:v", "libx264", "-profile:v", "main", "-s", "640x480", "-aspect", "4:3", "-b:v",
                            "1250k", "-c:a", "aac", "-strict", "-2", "-b:a", "128k", "-ar", "44100", "-r", "25",
                            "-g", "50", "-preset", "faster", "-y", outBoxPath + newFileEvent.getFileName());

                    builder.redirectErrorStream(true);
                    container.logger().info("Starting FFmpeg...");
                    Process ffmpeg = builder.start();
                    reader = new BufferedReader(new InputStreamReader(ffmpeg.getInputStream()));
                    String logLine = "";
                    while ((logLine = reader.readLine()) != null) {
                        container.logger().info("FFmpeg output: '" + logLine + "'");
                    }
                    container.logger().info("FFmpeg output stream closed, waiting for FFmpeg to exit...");
                    int exitCode = ffmpeg.waitFor();
                    container.logger().info("FFmpeg finished with exit code " + exitCode);
                } catch (Exception e) {
                    container.logger().error(e.getClass().getName()
                            + " occurred while processing '" + DirectoryListenerVerticle.EVENT_FILE_RECEIVED
                            + "'. Message was '" + e.getMessage() + "'", e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        });
        container.logger().info(FFmpegVerticle.class.getSimpleName() + " started successfully.");
    }
}
