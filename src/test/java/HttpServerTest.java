import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.Assert;
import org.junit.Test;

public class HttpServerTest extends HttpServerTestBase {

    @Test
    public void emptyBody() throws Exception {
        testSend(null, HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void validKey() throws Exception {
        testSend(AirlockMessageGenerator.generateAirlockMessage("project.env.a"), HttpStatus.SC_OK);
    }

    @Test
    public void invalidKey() throws Exception {
        testSend(AirlockMessageGenerator.generateAirlockMessage("project.env2.a"),
                HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void invalidMethod() throws Exception {
        Request request = Request.Get("http://localhost:" + port + "/send")
                .connectTimeout(10000)
                .socketTimeout(5000);
        HttpResponse response = request.addHeader("x-apikey", "WildcardPatternApiKey").execute()
                .returnResponse();
        Assert.assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED,
                response.getStatusLine().getStatusCode());
    }

    @Test
    public void notFound() throws Exception {
        Request request = Request.Get("http://localhost:" + port + "/helloServer")
                .connectTimeout(10000)
                .socketTimeout(5000);
        HttpResponse response = request.addHeader("x-apikey", "WildcardPatternApiKey").execute()
                .returnResponse();
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }

    @Test
    public void wrongApiKey() throws Exception {
        testSend(AirlockMessageGenerator.generateAirlockMessage(), HttpStatus.SC_UNAUTHORIZED,
                "HackerApiKey");
    }

    @Test
    public void emptyApiKey() throws Exception {
        testSend(AirlockMessageGenerator.generateAirlockMessage(), HttpStatus.SC_UNAUTHORIZED,
                null);
    }

}
