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
        // try {
        // ClassPath classPath = ClassPath.from(ClassLoader.getSystemClassLoader());
        // Set<ClassInfo> allClasses = classPath.getAllClasses();
        // for (ClassInfo classInfo : allClasses) {
        // log.info("CLASS LOADER: [{}]", classInfo.getName());
        // }
        // } catch (IOException exception) {
        // log.error("Error getting all classes", exception);
        // }

        Path path = Paths.get("/tmp/classes-loaded.txt");

        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
            Stream<String> lines = bufferedReader.lines();
            lines.forEach(line -> {
                log.info("FILE CLASS LOADER: [{}]", line);
            });
        } catch (IOException exception) {
            log.error("Error getting classes loaded file", exception);
        }
    }

    public static void loadClassesFromFile() {
        Path path = Paths.get("classes-loaded.txt");

        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
            Stream<String> lines = bufferedReader.lines();
            lines.forEach(line -> {
                processLine(line);
            });
        } catch (IOException exception) {
            log.error("Error on newBufferedReader", exception);
        }
    }

    private static void processLine(String line) {
        var index1 = line.indexOf("[class,load] ");
        var index2 = line.indexOf(" source: ");

        if (index1 < 0 || index2 < 0) {
            return;
        }

        var className = line.substring(index1 + 13, index2);
        preLoadClass(className);
    }

    public static void preLoadClass(String name) {
        try {
            Class.forName(name, true,
                    ClassPriming.class.getClassLoader());
        } catch (Throwable ignored) {
        }
    }

    /*
     * 
     * public static void writeLoadedClassesToFile(ClassLoader myCL) {
     * try {
     * while (myCL != null) {
     * System.out.println("ClassLoader: " + myCL);
     * for (Iterator iter = list(myCL); iter.hasNext();) {
     * System.out.println("\t" + iter.next());
     * }
     * myCL = myCL.getParent();
     * }
     * // Field fields[] = classLoader.getClass().getFields();
     * // System.out.println(fields.length);
     * // for (Field field : fields) {
     * // System.out.println(field.getName());
     * // }
     * // Field declaredFieldClasses =
     * ClassLoader.class.getDeclaredField("classes");
     * // declaredFieldClasses.setAccessible(true);
     * 
     * // Vector<Class<?>> classes = (Vector<Class<?>>)
     * // declaredFieldClasses.get(ClassLoader.getSystemClassLoader());
     * 
     * // for (Class<?> clazz : classes) {
     * // System.out.println(clazz.getName());
     * // }
     * } catch (NoSuchFieldException | IllegalArgumentException | SecurityException
     * | IllegalAccessException exception) {
     * exception.printStackTrace();
     * }
     * }
     * 
     * private static Iterator list(ClassLoader CL)
     * throws NoSuchFieldException, SecurityException,
     * IllegalArgumentException, IllegalAccessException {
     * Class CL_class = CL.getClass();
     * while (CL_class != java.lang.ClassLoader.class) {
     * CL_class = CL_class.getSuperclass();
     * }
     * java.lang.reflect.Field ClassLoader_classes_field = CL_class
     * .getDeclaredField("classes");
     * ClassLoader_classes_field.setAccessible(true);
     * Vector classes = (Vector) ClassLoader_classes_field.get(CL);
     * return classes.iterator();
     * }
     * 
     * public static void main(String[] args) {
     * // writeLoadedClassesToFile(Thread.currentThread().getContextClassLoader());
     * Field f;
     * try {
     * // Method getDeclaredFields0 =
     * Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
     * // getDeclaredFields0.setAccessible(true);
     * // Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
     * // System.out.println(fields.length);
     * // for (Field field : fields) {
     * // System.out.println(field.getName());
     * // }
     * // Field declaredAnnotationsField = null;
     * // for (Field field : fields) {
     * // if ("fieldAccessor".equals(field.getName())) {
     * // declaredAnnotationsField = field;
     * // declaredAnnotationsField.setAccessible(true);
     * // break;
     * // }
     * // }
     * // if (declaredAnnotationsField==null) {
     * // return;
     * // }
     * // var x = declaredAnnotationsField.get(ClassLoader.getSystemClassLoader());
     * // System.out.println("uia");
     * // System.out.println(x);
     * 
     * f = ClassLoader.class.getDeclaredField("clazz");
     * f.setAccessible(true);
     * // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
     * // Vector<Class> classes = (Vector<Class>) f.get(classLoader);
     * 
     * // for (Class cls : classes) {
     * // java.net.URL location = cls.getResource('/' + cls.getName().replace('.',
     * // '/') + ".class");
     * // System.out.println("<p>" + location + "<p/>");
     * // }
     * } catch (Exception e) {
     * 
     * e.printStackTrace();
     * }
     */

}
