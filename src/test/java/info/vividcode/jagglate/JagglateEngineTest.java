package info.vividcode.jagglate;

import static org.junit.Assert.*;
import groovy.text.Template;

import org.junit.Test;

public class JagglateEngineTest {

    @Test
    public void useSimpleTemplate() {
        JagglateEngine engine = JagglateEngine.create(getClass().getClassLoader());
        Template myTemplate = engine.createTemplate("simple_template.tpl.html");
        assertEquals("Simple template can be make without parameter",
                "Simple template", myTemplate.make().toString());
    }

}
