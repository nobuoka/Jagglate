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

import groovy.text.Template;
import info.vividcode.jagglate.internal.TemplateStringLoader;
import info.vividcode.jagglate.internal.TemplateStringResourceLoader;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class JagglateEngineTest {

    @Test
    public void useSimpleTemplate() {
        TemplateStringLoader tsLoader =
                new TemplateStringResourceLoader(StandardCharsets.UTF_8, "");
        JagglateEngine engine = JagglateEngine.create(tsLoader);
        Template myTemplate = engine.createTemplate("simple_template.tpl.html");

        String output = myTemplate.make(new HashMap<String, String>() {{ put("message", "hello"); }}).toString();

        assertThat("Simple template can be make without parameter",
                output, equalTo("Simple template\nok hello\n"));
    }

    @Test
    public void includeTemplate() {
        TemplateStringLoader tsLoader =
                new TemplateStringResourceLoader(StandardCharsets.UTF_8, "");
        JagglateEngine engine = JagglateEngine.create(tsLoader);
        Template myTemplate = engine.createTemplate("including_template.tpl.html");

        String output = myTemplate.make(new HashMap<String, String>() {{ put("message", "hello"); }}).toString();

        assertThat("Simple template can be make without parameter",
                output, equalTo("Including Simple template :\nSimple template\nok hello\n\n"));
    }

    @Test
    public void usePathPrefix() {
        TemplateStringLoader tsLoader =
                new TemplateStringResourceLoader(StandardCharsets.UTF_8, "test_to_path_prefix/deep/prefix/");
        JagglateEngine engine = JagglateEngine.create(tsLoader);
        Template myTemplate = engine.createTemplate("part_of_package/pp/template.tpl.html");
        assertEquals("Path prefix is enabled.",
                "Path prefix test", myTemplate.make().toString());
    }

}
