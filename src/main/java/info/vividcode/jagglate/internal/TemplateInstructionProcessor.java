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

import info.vividcode.jagglate.JagglateGenerator;
import info.vividcode.jagglate.internal.TemplateInstructionProcessor.InvalidInstruction.Type;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TemplateInstructionProcessor {

    static class InvalidInstruction extends Exception {
        private static final long serialVersionUID = 1L;
        public enum Type {
            UNKNOWN_INSTRUCTION,
            COMPILE_ERROR,
        }
        public final Type type;
        InvalidInstruction(Type t, String message) {
            super(message);
            type = t;
        }
        InvalidInstruction(Type t, Throwable cause) {
            super(cause);
            type = t;
        }
    }

    private final TemplateClassLoader mTemplateClassLoader;

    TemplateInstructionProcessor(TemplateClassLoader loader) {
        mTemplateClassLoader = loader;
    }

    void process(StringBuilder sb, String templateInstruction) throws InvalidInstruction {
        List<ASTNode> nodes;
        try {
            nodes = new AstBuilder().buildFromString(
                    CompilePhase.SEMANTIC_ANALYSIS, templateInstruction);
        } catch (CompilationFailedException e) {
            throw new InvalidInstruction(Type.COMPILE_ERROR, e);
        }
        BlockStatement block = (BlockStatement) nodes.get(0);
        ExpressionStatement exStat = (ExpressionStatement) block.getStatements().get(0);
        Expression exp = exStat.getExpression();
        //System.out.println(exp.getColumnNumber() + ", " + exp.getLastColumnNumber());
        MethodCallExpression methodCall = (MethodCallExpression) exp;
        if ("include".equals(methodCall.getMethodAsString())) {
            processInclude(sb, templateInstruction, methodCall);
        } else {
            throw new InvalidInstruction(Type.UNKNOWN_INSTRUCTION, "Unknown instruction: " + methodCall.getMethodAsString());
        }
    }

    private void processInclude(StringBuilder sb, String templateInstruction, MethodCallExpression methodCall) {
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
        Class<? extends JagglateGenerator> includedTemplateClass;
        try {
            // ここで load することに意味がある。
            includedTemplateClass = mTemplateClassLoader.loadTemplateClass(templatePath, String.class);
            @SuppressWarnings("unchecked")
            List<String> tmp =
                    (List<String>) includedTemplateClass.getField("PARAM_NAMES").get(null);
            paramNames = tmp;
        } catch (ClassNotFoundException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException("テンプレート " + templatePath + " のインクルードに失敗", e);
        }
        sb.append("new ").append(includedTemplateClass.getCanonicalName()).append("().generate(out");
        for (String argName : paramNames) {
            if (!argNameToOriginalCodeMap.containsKey(argName)) {
                throw new RuntimeException("Parameter named `" + argName + "` needed");
            }
            sb.append(", ");
            sb.append(argNameToOriginalCodeMap.get(argName));
        }
        sb.append(")\n");
    }

}
