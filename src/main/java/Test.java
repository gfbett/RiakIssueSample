import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.util.BinaryValue;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

public class Test {

    private static final Logger LOGGER = LoggerFactory.getLogger(Test.class);

    public static void main(String[] args) {

        RiakClient client = getRiakClient();
        final String bucket = "Test";
        final String key = "1";

        String json = readFile("/test.json");
        System.out.println(json);
        RiakObject object = getObject(json);
        System.out.println("Content Type to save: " + object.getContentType());

        storeValue(client, bucket, key, object);
        object = readValue(client, bucket, key);

        System.out.println("Content Type read: " + object.getContentType());


        String url = "http://localhost:8098/buckets/Test/keys/1";
        URLConnection connection = null;
        String headerField = "";
        try {
            connection = new URL(url).openConnection();
            headerField = connection.getHeaderField("Content-Type");
        } catch (IOException e) {
            LOGGER.error("Error reading by http");
        }

        System.out.println("Content Type http: " + headerField);
        client.shutdown();
    }

    private static RiakObject readValue(final RiakClient client, final String bucket, final String key) {
        try {
            Namespace ns = new Namespace(bucket);
            Location location = new Location(ns, key);
            FetchValue fetchCommand = new FetchValue.Builder(location)
                    .build();
            final FetchValue.Response response = client.execute(fetchCommand);
            return response.getValues().get(0);
        } catch (Exception ex) {
            LOGGER.error("Error reading", ex);
            throw new RuntimeException(ex);
        }
    }

    private static void storeValue(final RiakClient client, final String bucket, final String key, final RiakObject object) {
        try {
            Namespace ns = new Namespace(bucket);
            StoreValue storeCommand = new StoreValue.Builder(object)
                    .withLocation(new Location(ns, key))
                    .build();
            client.execute(storeCommand);
        } catch (Exception ex) {
            LOGGER.error("Error storing", ex);
        }
    }

    private static RiakObject getObject(final String content) {
        RiakObject object = new RiakObject();
        object.setValue(BinaryValue.createFromUtf8(content));
        object.setContentType("application/json;charset=UTF-8");
        return object;
    }

    private static RiakClient getRiakClient() {
        try {
            RiakNode.Builder builder = new RiakNode.Builder();
            List<String> addresses = new LinkedList<String>();
            addresses.add("localhost");
            List<RiakNode> nodes = RiakNode.Builder.buildNodes(builder, addresses);
            RiakCluster cluster = new RiakCluster.Builder(nodes).build();
            cluster.start();
            return new RiakClient(cluster);
        } catch (Exception ex) {
            LOGGER.error("Error connecting to riak");
            throw new RuntimeException(ex);
        }
    }

    private static String readFile(final String filename) {
        try {
            final InputStream stream = Test.class.getResourceAsStream(filename);
            return IOUtils.toString(stream, "UTF-8");
        } catch (final IOException ex) {
            LOGGER.error("Error reading file");
            throw new RuntimeException(ex);
        }
    }

}
