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

import java.security.AccessController;
import java.security.PrivilegedAction;

import groovy.text.Template;

public class JagglateEngine {

    private final TemplateClassLoader mTemplateClassLoader;

    public static JagglateEngine create(TemplateStringLoader tsLoader) {
        return JagglateEngine.create(JagglateEngine.class.getClassLoader(), tsLoader);
    }

    public static JagglateEngine create(ClassLoader parentClassLoader, TemplateStringLoader tsLoader) {
        return new JagglateEngine(parentClassLoader, tsLoader);
    }

    private JagglateEngine(final ClassLoader parentClassLoader, final TemplateStringLoader templateStringLoader) {
        final TemplateClassLoader templateClassLoader = AccessController.doPrivileged(new PrivilegedAction<TemplateClassLoader>() {
            @Override
            public TemplateClassLoader run() {
                return new TemplateClassLoaderImpl(parentClassLoader, templateStringLoader);
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
