package info.vividcode.sample.web.app.jwjagglate;

import groovy.text.Template;
import info.vividcode.jagglate.JagglateEngine;
import info.vividcode.jagglate.TemplateStringLoader;
import info.vividcode.jagglate.TemplateStringResourceLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;

@Provider
public class MyTemplateProcessor implements TemplateProcessor<Template> {

    private final JagglateEngine mJagglateEngine;

    public MyTemplateProcessor() {
        TemplateStringLoader tsLoader = new TemplateStringResourceLoader(
                StandardCharsets.UTF_8, "templates/jagglates/");
        mJagglateEngine = JagglateEngine.create(tsLoader);
    }

    @Override
    public Template resolve(String path, final MediaType mediaType) {
        return mJagglateEngine.createTemplate(path.substring(1));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(Template template, Viewable viewable,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream out) throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            template.make((Map<String, ?>) viewable.getModel()).writeTo(osw);
        }
    }

}
