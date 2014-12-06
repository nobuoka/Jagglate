/*
Copyright 2014 NOBUOKA Yu

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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

    private JagglateEngine(final ClassLoader parentClassLoader, final JagglateFileLoader fileLoader) {
        final TemplateClassLoader templateClassLoader = AccessController.doPrivileged(new PrivilegedAction<TemplateClassLoader>() {
            @Override
            public TemplateClassLoader run() {
                return new TemplateClassLoaderImpl(parentClassLoader, fileLoader);
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
