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

import groovy.transform.CompileStatic;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TemplateGroovyConverter {

    TemplateGroovyConverter(TemplateInstructionProcessor templateInstructionProcessor) {
        mTemplateInstructionProcessor = templateInstructionProcessor;
    }

    private final TemplateInstructionProcessor mTemplateInstructionProcessor;

    /**
     * Convert specified template string to Groovy source code.
     *
     * @param name Full class name.
     * @param templateSourceStr Template string.
     * @return Groovy source code converted from a template string.
     */
    public String convertTemplateStringToGroovyCode(String name, String templateSourceStr) {
        List<Map.Entry<String, String>> templateArgTypes = new ArrayList<>();
        String templateSourceBody = parseTemplateString(templateSourceStr, templateArgTypes);
        int s = name.lastIndexOf('.');
        String packageName = name.substring(0, s);
        String className = name.substring(s + 1);
        return convertParsedTemplateBodyToGroovySource(
                templateSourceBody, packageName, className,
                templateArgTypes);
    }

    private String parseTemplateString(String templateSource, List<Map.Entry<String, String>> templateArgTypesDest) {
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

    private String convertParsedTemplateBodyToGroovySource(String templateStr, String packageName,
                                                           String className, List<Map.Entry<String, String>> argTypes) {
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
        for (Map.Entry<String, String> argInfo : argTypes) {
            sb.append("    PARAM_NAMES.add(\"" + argInfo.getKey() + "\")\n");
        }
        sb.append("  }\n");

        // void generate(Map<String, ?> args, PrintWriter out);
        //sb.append("  @").append(anoClass.getCanonicalName()).append('\n');
        sb.append("  @Override\n");
        sb.append("  void generate(Map<String, ?> args, PrintWriter out) {\n");
        for (Map.Entry<String, String> argInfo : argTypes) {
            String argName = argInfo.getKey();
            String argType = argInfo.getValue();
            sb.append("    ").append(argType).append(' ').append(argName).append(" = (").append(argType).append(") args.get('" + argName + "');\n");
        }
        sb.append("    generate(out");
        for (Map.Entry<String, String> argInfo : argTypes) {
            String argName = argInfo.getKey();
            sb.append(", ").append(argName);
        }
        sb.append(")\n");
        sb.append("  }\n");

        // void generate(PrintWriter out, ArgType1 arg1, ArgType2 arg2, ...);
        sb.append("  void generate(PrintWriter out");
        for (Map.Entry<String, String> argInfo : argTypes) {
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
                    } catch (TemplateInstructionProcessor.InvalidInstruction e) {
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

    private static void putRawText(StringBuilder sb, String rawText) {
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
