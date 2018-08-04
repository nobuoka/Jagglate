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

import groovy.lang.Writable;
import groovy.text.Template;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

class JagglateTemplate<T> {

    private final JagglateGenerator<T> mSource;

    public JagglateTemplate(JagglateGenerator<T> source) {
        mSource = source;
    }

    public Writable make(final T parameter) {
        return new Writable() {
            /**
             * Write the template document with the set binding applied to the writer.
             *
             * @see groovy.lang.Writable#writeTo(java.io.Writer)
             */
            @SuppressWarnings("unchecked")
            @Override
            public Writer writeTo(Writer writer) {
                PrintWriter pw = new PrintWriter(writer);
                mSource.generate(parameter, pw);
                pw.flush();
                return writer;
            }

            /**
             * Convert the template and binding into a result String.
             *
             * @see java.lang.Object#toString()
             */
            @Override
            public String toString() {
                StringWriter sw = new StringWriter();
                writeTo(sw);
                return sw.toString();
            }
        };
    }

}
