package at.chaosfield.updaterUpdater;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class JarRunner {

    private static final Class<?>[] parameters = new Class[]{URL.class};

    public static void addFile(File file) throws IOException {
        URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL",parameters);
            method.setAccessible(true);
            method.invoke(sysloader,new Object[]{ file.toURI().toURL() });
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add File to system classloader");
        }
    }
}