package info.vividcode.jagglate;

import java.security.AccessController;
import java.security.PrivilegedAction;

import groovy.text.Template;

public class JagglateEngine {

    private final TemplateClassLoader mTemplateClassLoader;

    public JagglateEngine() {
        final ClassLoader parentClassLoader = getClass().getClassLoader();
        mTemplateClassLoader = AccessController.doPrivileged(new PrivilegedAction<TemplateClassLoader>() {
            @Override
            public TemplateClassLoader run() {
                return new TemplateClassLoader(parentClassLoader);
            }
        });
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
