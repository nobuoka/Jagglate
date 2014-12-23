package info.vividcode.sample.web.app.jwjagglate;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.glassfish.jersey.server.mvc.Template;

@Path("/")
public class HelloWorldResource {
    @GET
    @Produces("text/plain; charset=utf-8")
    @Template(name="/test.tpl.html")
    public Map<String, Object> getResource() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("message", "Good luck");
        return m;
    }
}
