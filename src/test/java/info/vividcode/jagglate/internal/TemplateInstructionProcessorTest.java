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
import info.vividcode.jagglate.internal.TemplateInstructionProcessor.InvalidInstruction;
import info.vividcode.jagglate.internal.TemplateInstructionProcessor.InvalidInstruction.Type;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TemplateInstructionProcessorTest {

    private static class MockTemplate implements JagglateGenerator {
        @SuppressWarnings("unused")
        public static List<String> PARAM_NAMES;
        @Override
        public void generate(Map<String, ?> args, PrintWriter out) {}
    }

    private static class MockClassLoader implements TemplateClassLoader {
        @Override
        public Class<? extends JagglateGenerator> loadTemplateClass(String path)
                throws ClassNotFoundException {
            if ("mock_template".equals(path)) {
                return MockTemplate.class;
            } else {
                throw new ClassNotFoundException();
            }
        }
    }

    @Test
    public void process_include() throws InvalidInstruction {
        final TemplateInstructionProcessor proc =
                new TemplateInstructionProcessor(new MockClassLoader());
        class TestProc {
            void proc(List<String> paramNames, String instructionParams, String expectedParams) throws InvalidInstruction {
                MockTemplate.PARAM_NAMES = paramNames;
                StringBuilder sb = new StringBuilder();
                proc.process(sb, "include \"mock_template\"" + instructionParams);
                assertEquals("The `generate` method call is output.",
                        "new " + MockTemplate.class.getCanonicalName() + "().generate(out" + expectedParams + ")\n",
                        sb.toString());
            }
        };
        TestProc testProc = new TestProc();
        // With one parameter.
        testProc.proc(Arrays.asList("goodArg"), ", goodArg: \"name\"", ", \"name\"");
        // With no parameter.
        testProc.proc(Collections.<String>emptyList(), "", "");
        // With multiple parameters.
        testProc.proc(Arrays.asList("goodArg", "numTests", "pp"),
                ", numTests: 3, goodArg: 'yeah', pp: null",
                ", 'yeah', 3, null");
    }

    @Test
    public void process_invalidInstruction() throws InvalidInstruction {
        final TemplateInstructionProcessor proc =
                new TemplateInstructionProcessor(new MockClassLoader());
        {
            StringBuilder sb = new StringBuilder();
            try {
                proc.process(sb, "unknownInstruction \"mock_template\"");
                fail("Exception not occurred");
            } catch (InvalidInstruction e) {
                assertEquals(Type.UNKNOWN_INSTRUCTION, e.type);
            }
        }
        {
            StringBuilder sb = new StringBuilder();
            try {
                proc.process(sb, "include \"mock_template");
                fail("Exception not occurred");
            } catch (InvalidInstruction e) {
                assertEquals(Type.COMPILE_ERROR, e.type);
            }
        }
        {
            StringBuilder sb = new StringBuilder();
            try {
                proc.process(sb, "include }");
                fail("Exception not occurred");
            } catch (InvalidInstruction e) {
                assertEquals(Type.COMPILE_ERROR, e.type);
            }
        }
    }

}
