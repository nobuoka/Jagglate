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

package info.vividcode.jagglate.internal;

import groovy.lang.GroovyClassLoader;
import info.vividcode.jagglate.JagglateGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class TemplateClassLoaderImpl extends GroovyClassLoader implements TemplateClassLoader {

    private final TemplateIdentifierConverter mConv = new TemplateIdentifierConverter();

    private static class TemplateIdentifierStore {
        private final HashMap<Id, String> idToClassName = new HashMap<>();
        private final HashMap<String, Id> classNameToId = new HashMap<>();
        private int nextNumber = 1;

        public Id getIdOrNull(String className) {
            return classNameToId.get(className);
        }

        public synchronized String getOrGenerateClassName(String templatePath, Class<?> parameterClass) {
            Id id = new Id(templatePath, parameterClass);
            if (!idToClassName.containsKey(id)) {
                String className = "__template__.Template" + nextNumber++;
                idToClassName.put(id, className);
                classNameToId.put(className, id);
            }
            return idToClassName.get(id);
        }

        private static final class Id {
            private final String templatePath;
            private final Class<?> parameterClass;

            private Id(String templatePath, Class<?> parameterClass) {
                this.templatePath = templatePath;
                this.parameterClass = parameterClass;
            }

            @Override
            public int hashCode() {
                return Objects.hash(templatePath, parameterClass);
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Id &&
                        templatePath.equals(((Id) obj).templatePath) &&
                        parameterClass.equals(((Id) obj).parameterClass);
            }
        }
    }
    private final TemplateIdentifierStore templateIdentifierStore = new TemplateIdentifierStore();

    private final TemplateStringLoader mTemplateStringLoader;

    private final TemplateGroovyConverter templateGroovyConverter;

    public TemplateClassLoaderImpl(ClassLoader parentClassLoader, TemplateStringLoader templateStringLoader) {
        super(parentClassLoader);
        mTemplateStringLoader = templateStringLoader;
        templateGroovyConverter = new TemplateGroovyConverter(new TemplateInstructionProcessor(this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Class<? extends JagglateGenerator<T>> loadTemplateClass(String path, Class<T> parameterClass)
            throws ClassNotFoundException {

        String className = templateIdentifierStore.getOrGenerateClassName(path, parameterClass);
        return (Class<? extends JagglateGenerator<T>>) loadClass(className);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        TemplateIdentifierStore.Id id = templateIdentifierStore.getIdOrNull(name);
        if (id != null) {
            String filePath = id.templatePath;
            String parameterClassName = id.parameterClass.getCanonicalName();

            String templateSourceStr;
            try {
                templateSourceStr = mTemplateStringLoader.load(filePath);
            } catch (IOException e) {
                throw new ClassNotFoundException("", e);
            }

            String templateGroovySrc =
                    templateGroovyConverter.convertTemplateStringToGroovyCode(name, templateSourceStr, parameterClassName);
            return parseClass(templateGroovySrc);
        }
        return super.findClass(name);
    }

}
