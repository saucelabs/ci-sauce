package com.saucelabs.ci.sauceconnect;

import com.saucelabs.sauceconnect.SauceConnect;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Ross Rowe
 * @deprecated Will be removed when Sauce Connect 3 support is dropped
 */
public final class SauceConnectUtils {
    public static final String SAUCE_CONNECT_JAR = "sauce-connect-3.+jar";
    public static final String META_INF_SAUCE_CONNECT_JAR = "(META|WEB)-INF/lib/" + SAUCE_CONNECT_JAR;

    private SauceConnectUtils() {
    }

    public static File extractSauceConnectJarFile() throws URISyntaxException, IOException {
        Class clazz = SauceConnect.class;
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        File jarFile = null;

        URL location = codeSource.getLocation();
        if (location == null) {
            location = SauceConnectUtils.class.getClassLoader().getResource("com/saucelabs/sauceconnect/SauceConnect.class");
            String name = location.toString();
            name = name.substring(0, name.indexOf("!"));
            name = name.substring(name.lastIndexOf(':')+1);
            jarFile = new File(name);
        } else {
            jarFile = new File(location.toURI());
        }


        return extractSauceConnectJar(jarFile);
    }

    private static File extractSauceConnectJar(File jarFile) throws IOException {
        if (jarFile.getName().matches(SAUCE_CONNECT_JAR)) {
            return jarFile;
        } else {
            JarFile jar = new JarFile(jarFile);
            java.util.Enumeration entries = jar.entries();
            final File destDir = jarFile.getParentFile();
            while (entries.hasMoreElements()) {
                JarEntry file = (JarEntry) entries.nextElement();

                if (file.getName().matches(META_INF_SAUCE_CONNECT_JAR)) {
                    File f = new File(destDir, file.getName());

                    if (f.exists()) {
                        f.delete();
                    }
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                    f.deleteOnExit();
                    InputStream is = jar.getInputStream(file); // get the input stream
                    FileOutputStream fos = new java.io.FileOutputStream(f);
                    IOUtils.copy(is, fos);
                    return f;
                }
            }
        }
        return null;
    }
}
