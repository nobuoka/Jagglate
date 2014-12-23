package info.vividcode.sample.web.app.jwjagglate;

import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.UriBuilder;

public class App {

    public static void main(String[] args) throws InterruptedException {

        final URI baseUri = UriBuilder.fromUri("http://localhost/").port(10082).build();
        final ResourceConfig config = new ResourceConfig();
        config.register(HelloWorldResource.class);
        config.register(org.glassfish.jersey.server.mvc.MvcFeature.class);
        config.register(MyTemplateProcessor.class);
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);

        // 以下、終了処理
        final CountDownLatch mainThreadWaiter = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                System.out.println("To be shutdown");
                mainThreadWaiter.countDown();
                GrizzlyFuture<HttpServer> f = server.shutdown();
                try {
                    f.get(15, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        });

        mainThreadWaiter.await();
        System.out.println("Main thread stopped");
    }

}
