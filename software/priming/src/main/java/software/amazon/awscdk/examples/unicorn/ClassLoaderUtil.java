package software.amazon.awscdk.examples.unicorn;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awscdk.examples.unicorn.handler.ClassPriming;

public class ClassLoaderUtil {

    private static final Logger log = LoggerFactory.getLogger(ClassLoaderUtil.class);

    public static void printLoadedClasses() {
        log.info("printLoadedClasses->started");
        Path path = Paths.get("/tmp/classes-loaded.txt");

        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
            Stream<String> lines = bufferedReader.lines();
            lines.forEach(line -> {
                log.info("FILE CLASS LOADER: [{}]", line);
            });
            log.info("printLoadedClasses->finished");
        } catch (IOException exception) {
            log.error("Error getting classes loaded file", exception);
        }
    }

    public static void loadClassesFromFile() {
        log.info("loadClassesFromFile->started");
        Path path = Paths.get("classes-loaded.txt");

        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
            Stream<String> lines = bufferedReader.lines();
            lines.forEach(line -> {
                var index1 = line.indexOf("[class,load] ");
                var index2 = line.indexOf(" source: ");

                if (index1 < 0 || index2 < 0) {
                    return;
                }

                var className = line.substring(index1 + 13, index2);
                try {
                    Class.forName(className, true,
                            ClassPriming.class.getClassLoader());
                } catch (Throwable ignored) {
                }
            });

            log.info("loadClassesFromFile->finished");
        } catch (IOException exception) {
            log.error("Error on newBufferedReader", exception);
        }
    }

}
