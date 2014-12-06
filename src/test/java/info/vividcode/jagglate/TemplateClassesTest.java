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

import static org.junit.Assert.*;

import org.junit.Test;

public class TemplateClassesTest {

    @Test
    public void underscoreMethod() {
        assertEquals("Simple string",
                "test", TemplateClasses.underscore("Test"));
        assertEquals("Camel case to snake case",
                "good_test_example_a_a_a", TemplateClasses.underscore("GoodTestExampleAAA"));
        assertEquals("Series of underscores",
                "good___test", TemplateClasses.underscore("Good__Test"));
        assertEquals("Underscore at the head",
                "_good_test", TemplateClasses.underscore("_GoodTest"));
        assertEquals("Series of underscores at the head",
                "___good_test", TemplateClasses.underscore("___GoodTest"));
    }

    @Test
    public void camelizeMethod() {
        assertEquals("Simple string",
                "Test", TemplateClasses.camelize("test"));
        assertEquals("Snake case to camel case",
                "GoodTestExampleAAA", TemplateClasses.camelize("good_test_example_a_a_a"));
        assertEquals("Series of underscores",
                "Good__Test", TemplateClasses.camelize("good___test"));
        assertEquals("Underscore at the head",
                "_GoodTest", TemplateClasses.camelize("_good_test"));
        assertEquals("Series of underscores at the head",
                "___GoodTest", TemplateClasses.camelize("___good_test"));
    }

}
