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

import groovy.lang.GroovyClassLoader;

import java.io.IOException;

class TemplateClassLoaderImpl extends GroovyClassLoader implements TemplateClassLoader {

    private final TemplateIdentifierConverter mConv = new TemplateIdentifierConverter();

    private final TemplateStringLoader mTemplateStringLoader;

    private final TemplateGroovyConverter templateGroovyConverter;

    public TemplateClassLoaderImpl(ClassLoader parentClassLoader, TemplateStringLoader templateStringLoader) {
        super(parentClassLoader);
        mTemplateStringLoader = templateStringLoader;
        templateGroovyConverter = new TemplateGroovyConverter(new TemplateInstructionProcessor(this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends JagglateGenerator> loadTemplateClass(String path) throws ClassNotFoundException {
        String className = mConv.convertPathToClassName(path);
        return (Class<? extends JagglateGenerator>) loadClass(className);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.indexOf(mConv.classNamePrefix) == 0) {
            String filePath = mConv.convertClassNameToPath(name);
            String templateSourceStr;
            try {
                templateSourceStr = mTemplateStringLoader.load(filePath);
            } catch (IOException e) {
                throw new ClassNotFoundException("", e);
            }

            String templateGroovySrc = templateGroovyConverter.convertTemplateStringToGroovyCode(name, templateSourceStr);
            return parseClass(templateGroovySrc);
        }
        return super.findClass(name);
    }

}
