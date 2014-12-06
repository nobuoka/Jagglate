package info.vividcode.jagglate;

import groovy.lang.GroovyClassLoader;
import groovy.transform.CompileStatic;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.CompilePhase;

class TemplateClassLoader extends GroovyClassLoader {

    private static final Map<String, String> CLASS_NAME_TO_TEMPLATE_FILE_PATH_MAP = new HashMap<>();
    static {
        CLASS_NAME_TO_TEMPLATE_FILE_PATH_MAP.put("templates.Included1$Tpl$Html", "included1.tpl.html");
        CLASS_NAME_TO_TEMPLATE_FILE_PATH_MAP.put("templates.Test$Tpl$Html", "test.tpl.html");
    }

    static String classNamePrefix = "templates.";
    static String pathPrefix = "";

    public static String camelize(String str) {
        StringBuilder sb = new StringBuilder();
        Pattern topUnderScore = Pattern.compile("\\A_+");
        Matcher m = topUnderScore.matcher(str);
        if (m.find()) {
            sb.append(str.substring(0, m.end()));
            str = str.substring(m.end());
        }

        Pattern firstLetterPattern = Pattern.compile("(?:\\A|_)([a-z])");
        m = firstLetterPattern.matcher(str);
        int pos = 0;
        while (m.find()) {
            sb.append(str.substring(pos, m.start()));
            sb.append(m.group(1).toUpperCase(Locale.US));
            pos = m.end();
        }
        sb.append(str.substring(pos));
        return sb.toString();
    }

    public static String underscore(String str) {
        StringBuilder sb = new StringBuilder();
        Pattern topUnderScore = Pattern.compile("\\A_+");
        Matcher m = topUnderScore.matcher(str);
        if (m.find()) {
            sb.append(str.substring(0, m.end()));
            str = str.substring(m.end());
        }

        Pattern capitalLetterPattern = Pattern.compile("[A-Z]");
        m = capitalLetterPattern.matcher(str);
        int pos = 0;
        while (m.find()) {
            sb.append(str.substring(pos, m.start()));
            sb.append(m.group().toLowerCase(Locale.US));
            pos = m.end();
        }
        sb.append(str.substring(pos));
        return sb.toString();
    }

    public static String convertPathToClassName(String path) {
        String prefix = path.substring(0, pathPrefix.length());
        if (!prefix.equals(pathPrefix)) {
            throw new RuntimeException("invalid path");
        }
        path = path.substring(pathPrefix.length());

        StringBuilder sb = new StringBuilder();
        String[] pp = path.split("/");
        String fileName = pp[pp.length - 1];
        for (int i = 0; i < pp.length - 1; i++) {
            sb.append(pp[i]).append('.');
        }
        String[] ff = fileName.split("\\.");
        boolean isFirst = true;
        for (String f : ff) {
            if (!isFirst) sb.append('$');
            sb.append(camelize(f));
            isFirst = false;
        }
        return classNamePrefix + sb.toString();
    }

    public static String convertClassNameToPath(String className) {
        String prefix = className.substring(0, classNamePrefix.length());
        if (!prefix.equals(classNamePrefix)) {
            throw new RuntimeException("invalid className");
        }
        className = className.substring(classNamePrefix.length());

        StringBuilder sb = new StringBuilder();
        String[] cc = className.split("\\.");
        String localClassName = cc[cc.length - 1];
        for (int i = 0; i < cc.length - 1; i++) {
            sb.append(cc[i]).append('/');
        }
        String[] ff = localClassName.split("\\$");
        boolean isFirst = true;
        for (String f : ff) {
            if (!isFirst) sb.append('.');
            sb.append(underscore(f));
            isFirst = false;
        }
        return pathPrefix + sb.toString();
    }

    private Charset mTemplateSourceFileCharset = StandardCharsets.UTF_8;

    public TemplateClassLoader(ClassLoader parentClassLoader) {
        super(parentClassLoader);
    }

    @SuppressWarnings("unchecked")
    public Class<? extends JagglateGenerator> loadTemplateClass(String path) throws ClassNotFoundException {
        String className = convertPathToClassName(path);
        return (Class<? extends JagglateGenerator>) loadClass(className);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.indexOf(classNamePrefix) == 0) {
            String filePath = convertClassNameToPath(name);
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

    private final JagglateFileLoader mTemplateFileLoader = new JagglateFileLoader() {
        @Override
        public String load(String path) throws IOException {
            try (BufferedInputStream bis = new BufferedInputStream(getResourceAsStream(path))) {
                byte[] bb = new byte[1024];
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int count;
                while ((count = bis.read(bb)) != -1) {
                    out.write(bb, 0, count);
                }
                return new String(out.toByteArray(), mTemplateSourceFileCharset);
            }
            //byte[] bytes = Files.readAllBytes(Paths.get(path));
            //return new String(bytes, mTemplateSourceFileCharset);
        }
    };

    public String loadAndParseTemplateFromPath(
            String templatePath, List<Entry<String, String>> templateArgTypesDest
            ) throws IOException {
        System.out.println("loadAndParseTemplateFromPath : " + templatePath);
        String templateSourceStr = mTemplateFileLoader.load(templatePath);
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
            putRawTest(sb, templateStr.substring(pos, m.start()));
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
                    System.out.println(templateInstruction);
                    List<ASTNode> nodes = new AstBuilder().buildFromString(
                            CompilePhase.SEMANTIC_ANALYSIS, templateInstruction);
                    System.out.println(nodes);
                    BlockStatement block = (BlockStatement) nodes.get(0);
                    ExpressionStatement exStat = (ExpressionStatement) block.getStatements().get(0);
                    Expression exp = exStat.getExpression();
                    //System.out.println(exp.getColumnNumber() + ", " + exp.getLastColumnNumber());
                    MethodCallExpression methodCall = (MethodCallExpression) exp;
                    if ("include".equals(methodCall.getMethodAsString())) {
                        //System.out.println("include!!!!");
                        ArgumentListExpression ee = (ArgumentListExpression) methodCall.getArguments();
                        //System.out.println(ee.getExpression(0));
                        List<Expression> argExps = ee.getExpressions();
                        Expression templatePathExp = argExps.get(argExps.size() - 1);
                        String templatePath = (String) ((ConstantExpression) templatePathExp).getValue();
                        //System.out.println(ee.getExpression(0));
                        Map<String, String> argNameToOriginalCodeMap = new HashMap<>();
                        // 引数があれば処理。
                        if (argExps.size() > 1) {
                            String[] lines = templateInstruction.split("\n");
                            for (MapEntryExpression e : ((MapExpression) ee.getExpression(0)).getMapEntryExpressions()) {
                                String paramName = (String) ((ConstantExpression) e.getKeyExpression()).getValue();
                                Expression valExp = e.getValueExpression();
                                StringBuilder psb = new StringBuilder();
                                if (valExp.getLineNumber() == valExp.getLastLineNumber()) {
                                    psb.append(lines[valExp.getLineNumber() - 1].substring(valExp.getColumnNumber() - 1, valExp.getLastColumnNumber() - 1));
                                } else {
                                    // First line.
                                    psb.append(lines[valExp.getLineNumber() - 1].substring(valExp.getColumnNumber() - 1));
                                    psb.append('\n');
                                    // Middle lines.
                                    for (int lineNum = valExp.getLineNumber() + 1; lineNum < valExp.getLastLineNumber(); lineNum++) {
                                        psb.append(lines[lineNum - 1]).append('\n');
                                    }
                                    // Last line.
                                    psb.append(lines[valExp.getLastLineNumber() - 1].substring(0, valExp.getLastColumnNumber() - 1));
                                }
                                argNameToOriginalCodeMap.put(paramName, psb.toString());
                            };
                        }
                        //System.out.println(templatePath);
                        List<String> paramNames;
                        String c = convertPathToClassName(templatePath);
                        try {
                            // ここで load することに意味がある。
                            Class<?> includedTemplateClass = loadClass(c);
                            @SuppressWarnings("unchecked")
                            List<String> tmp =
                                    (List<String>) includedTemplateClass.getField("PARAM_NAMES").get(null);
                            paramNames = tmp;
                            // TODO: パラメータチェック。
                        } catch (ClassNotFoundException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                            // TODO Auto-generated catch block
                            throw new RuntimeException("テンプレート " + templatePath + " のインクルードに失敗", e);
                        }
                        sb.append("new ").append(c).append("().generate(out");
                        for (String argName : paramNames) {
                            if (!argNameToOriginalCodeMap.containsKey(argName)) {
                                throw new RuntimeException("Parameter named `" + argName + "` needed");
                            }
                            sb.append(", ");
                            sb.append(argNameToOriginalCodeMap.get(argName));
                        }
                        sb.append(")\n");
                    }
                } else {
                    sb.append(groovyCode).append('\n');
                }
            }
            pos = m.end();
        }
        putRawTest(sb, templateStr.substring(pos));

        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    static void putRawTest(StringBuilder sb, String rowString) {
        String escaped = rowString;
        // ["\$\\] => \$1
        escaped = escaped.replaceAll("([\"\\$\\\\])", "\\\\$1");
        // CR and LF
        escaped = escaped.replaceAll("\n", "\\\\n");
        escaped = escaped.replaceAll("\r", "\\\\r");
        sb.append("    out << \"").append(escaped).append("\"\n");
    }

}
