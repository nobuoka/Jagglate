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

import static org.junit.Assert.assertEquals;

public class JagglateEngineTest {

    @Test
    public void useSimpleTemplate() {
        TemplateStringLoader tsLoader =
                new TemplateStringResourceLoader(StandardCharsets.UTF_8, "");
        JagglateEngine engine = JagglateEngine.create(tsLoader);
        Template myTemplate = engine.createTemplate("simple_template.tpl.html");
        assertEquals("Simple template can be make without parameter",
                "Simple template", myTemplate.make().toString());
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
