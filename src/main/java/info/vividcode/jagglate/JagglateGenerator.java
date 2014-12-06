package info.vividcode.jagglate;

import java.io.PrintWriter;
import java.util.Map;

public interface JagglateGenerator {

    void generate(Map<String, ?> args, PrintWriter out);

}
