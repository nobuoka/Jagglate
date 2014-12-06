package info.vividcode.jagglate;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;

import groovy.text.Template;

public class JagglateEngine {

    private static final Charset TEMPLATE_SOURCE_FILE_CHARSET = StandardCharsets.UTF_8;

    private final TemplateClassLoader mTemplateClassLoader;

    public static JagglateEngine create() {
        final ClassLoader parentClassLoader = JagglateEngine.class.getClassLoader();
        return create(parentClassLoader);
    }

    public static JagglateEngine create(final ClassLoader parentClassLoader) {
        JagglateFileLoader fileLoader = new JagglateFileLoader() {
            @Override
            public String load(String path) throws IOException {
                ClassLoader l = parentClassLoader;
                try (BufferedInputStream bis = new BufferedInputStream(l.getResourceAsStream(path))) {
                    byte[] bb = new byte[1024];
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int count;
                    while ((count = bis.read(bb)) != -1) {
                        out.write(bb, 0, count);
                    }
                    return new String(out.toByteArray(), TEMPLATE_SOURCE_FILE_CHARSET);
                }
            }
        };
        return new JagglateEngine(parentClassLoader, fileLoader);
    }

    private JagglateEngine(ClassLoader parentClassLoader, JagglateFileLoader fileLoader) {
        final TemplateClassLoader templateClassLoader = AccessController.doPrivileged(new PrivilegedAction<TemplateClassLoader>() {
            @Override
            public TemplateClassLoader run() {
                return new TemplateClassLoader(parentClassLoader, fileLoader);
            }
        });
        mTemplateClassLoader = templateClassLoader;
    }

    public Template createTemplate(String templateFilePath) {
        Class<? extends JagglateGenerator> clazz;
        try {
            clazz = mTemplateClassLoader.loadTemplateClass(templateFilePath);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            return new JagglateTemplate(clazz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
