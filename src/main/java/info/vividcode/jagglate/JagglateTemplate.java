package info.vividcode.jagglate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import groovy.lang.Writable;
import groovy.text.Template;

class JagglateTemplate implements Template {

    private final JagglateGenerator mSource;

    public JagglateTemplate(JagglateGenerator source) {
        mSource = source;
    }

    @Override
    public Writable make() {
        return make(Collections.EMPTY_MAP);
    }

    @Override
    public Writable make(@SuppressWarnings("rawtypes") Map binding) {
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
                mSource.generate(binding, pw);
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
