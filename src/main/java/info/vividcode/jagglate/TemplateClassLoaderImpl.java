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
import groovy.transform.CompileStatic;
import info.vividcode.jagglate.TemplateInstructionProcessor.InvalidInstruction;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TemplateClassLoaderImpl extends GroovyClassLoader implements TemplateClassLoader {

    TemplateIdentifierConverter mConv = new TemplateIdentifierConverter();

    public TemplateClassLoaderImpl(ClassLoader parentClassLoader, TemplateStringLoader templateStringLoader) {
        super(parentClassLoader);
        mTemplateStringLoader = templateStringLoader;
        mTemplateInstructionProcessor = new TemplateInstructionProcessor(this);
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
            List<Entry<String, String>> templateArgTypes = new ArrayList<>();
            String templateSourceBody;
            try {
                templateSourceBody = loadAndParseTemplateFromPath(filePath, templateArgTypes);
            } catch (IOException e) {
                throw new ClassNotFoundException("", e);
            }
            int s = name.lastIndexOf('.');
            String packageName = name.substring(0, s);
            String className = name.substring(s + 1);
            String templateGroovySrc = convertTemplateStrToGroovySource(
                    templateSourceBody, packageName, className,
                    templateArgTypes);
            return parseClass(templateGroovySrc);
        }
        return super.findClass(name);
    }

    private final TemplateStringLoader mTemplateStringLoader;
    private final TemplateInstructionProcessor mTemplateInstructionProcessor;

    public String loadAndParseTemplateFromPath(
            String templatePath, List<Entry<String, String>> templateArgTypesDest
            ) throws IOException {
        String templateSourceStr = mTemplateStringLoader.load(templatePath);
        String templateSourceBody = parseTemplateStr(templateSourceStr, templateArgTypesDest);
        return templateSourceBody;
    }

    private String parseTemplateStr(String templateSource, List<Entry<String, String>> templateArgTypesDest) {
        StringBuilder sb = new StringBuilder();
        Pattern p = Pattern.compile("%:\\s*arg\\s+([^\\s:]*):\\s*([^\\s]*)(?:\\n|\\z)", Pattern.MULTILINE);
        Matcher m = p.matcher(templateSource);
        int pos = 0;
        while (m.find()) {
            sb.append(templateSource.substring(pos, m.start()));
            pos = m.end();
            String argName = m.group(1);
            String argType = m.group(2);
            templateArgTypesDest.add(
                    new AbstractMap.SimpleImmutableEntry<String, String>(argName, argType));
        }
        sb.append(templateSource.substring(pos));
        return sb.toString();
    }

    String convertTemplateStrToGroovySource(String templateStr, String packageName,
            String className, List<Entry<String, String>> argTypes) {
        String interfaceName = JagglateGenerator.class.getCanonicalName();
        //Class<WithLogging> anoClass = WithLogging.class;
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append('\n');
        sb.append("import java.lang.Override\n");
        sb.append("import java.lang.String\n");
        sb.append("import java.io.PrintWriter\n");
        sb.append("import java.util.Map\n");
        sb.append("import java.util.ArrayList\n");
        sb.append("import ").append(CompileStatic.class.getCanonicalName()).append("\n");
        sb.append("import static ").append(TemplateStaticMethods.class.getCanonicalName()).append(".*;");
        sb.append("@").append(CompileStatic.class.getSimpleName()).append("\n");
        sb.append("class ").append(className).append(" implements ").append(interfaceName).append(" {\n");
        //sb.append("  ").append(className).append(" getInstance() {\n}\n");
        sb.append("  public static java.util.List<String> PARAM_NAMES = new ArrayList<String>()\n");
        sb.append("  static {\n");
        for (Entry<String, String> argInfo : argTypes) {
            sb.append("    PARAM_NAMES.add(\"" + argInfo.getKey() + "\")\n");
        }
        sb.append("  }\n");

        // void generate(Map<String, ?> args, PrintWriter out);
        //sb.append("  @").append(anoClass.getCanonicalName()).append('\n');
        sb.append("  @Override\n");
        sb.append("  void generate(Map<String, ?> args, PrintWriter out) {\n");
        for (Entry<String, String> argInfo : argTypes) {
            String argName = argInfo.getKey();
            String argType = argInfo.getValue();
            sb.append("    ").append(argType).append(' ').append(argName).append(" = (").append(argType).append(") args.get('" + argName + "');\n");
        }
        sb.append("    generate(out");
        for (Entry<String, String> argInfo : argTypes) {
            String argName = argInfo.getKey();
            sb.append(", ").append(argName);
        }
        sb.append(")\n");
        sb.append("  }\n");

        // void generate(PrintWriter out, ArgType1 arg1, ArgType2 arg2, ...);
        sb.append("  void generate(PrintWriter out");
        for (Entry<String, String> argInfo : argTypes) {
            sb.append(", ").append(argInfo.getValue()).append(' ').append(argInfo.getKey());
        }
        sb.append(") {\n");

        Pattern p = Pattern.compile("\\[%(.*?)%\\]|^%(.*?)(?:\\n|\\z)", Pattern.MULTILINE);
        Matcher m = p.matcher(templateStr);
        int pos = 0;
        while (m.find()) {
            putRawText(sb, templateStr.substring(pos, m.start()));
            boolean isLineCode = false;
            String groovyCode = null;
            if (m.group(1) != null) {
                groovyCode = m.group(1);
            } else {
                isLineCode = true;
                groovyCode = m.group(2);
            }
            if (groovyCode == null) {
                groovyCode = "";
            }
            groovyCode = groovyCode.trim();
            if (groovyCode != null && groovyCode.length() > 0) {
                if ('=' == groovyCode.charAt(0)) {
                    sb.append("out << ").append(groovyCode.substring(1)).append('\n');
                    if (isLineCode) sb.append("out << \"\\n\"\n");
                } else if (':' == groovyCode.charAt(0)) {
                    String templateInstruction = groovyCode.substring(1);
                    try {
                        mTemplateInstructionProcessor.process(sb, templateInstruction);
                    } catch (InvalidInstruction e) {
                        // TODO: Error handling
                        throw new RuntimeException(e);
                    }
                } else {
                    sb.append(groovyCode).append('\n');
                }
            }
            pos = m.end();
        }
        putRawText(sb, templateStr.substring(pos));

        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    static void putRawText(StringBuilder sb, String rawText) {
        String escaped = rawText;
        // `"` => `\"`, `\` => `\\`, `$` => `\$`
        escaped = escaped.replaceAll("([\"\\$\\\\])", "\\\\$1");
        // LF => `\n`
        escaped = escaped.replaceAll("\n", "\\\\n");
        // CR => `\r`
        escaped = escaped.replaceAll("\r", "\\\\r");
        sb.append("    out << \"").append(escaped).append("\"\n");
    }

}
