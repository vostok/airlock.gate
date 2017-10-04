import org.junit.Assert;
import org.junit.Test;
import ru.kontur.airlock.Authorizer;
import ru.kontur.airlock.AuthorizerFactory;

import java.util.HashMap;

public class AuthorizationTest {
    private AuthorizerFactory authorizerFactory;

    public AuthorizationTest() {

        authorizerFactory = new AuthorizerFactory(new HashMap<String, String[]>() {
            {
                put("ExactPatternApiKey", new String[]{"project:env"});
                put("WildcardPatternApiKey", new String[]{"project:env:*"});
                put("SeveralPatternsApiKey", new String[]{"project:dev", "project:staging", "foo:bar"});
                put("IgnoringCasePatternsApiKey", new String[]{"Project:Env", "Foo:Bar:*"});
            }
        });
    }

    @Test
    public void UnregisteredApiKey() throws Exception {
        Authorizer authorizer = authorizerFactory.getAuthorizer("UnregisteredApiKey");
        Assert.assertEquals(false, authorizer.authorize("project:env"));
    }

    @Test
    public void ExactPattern() throws Exception {
        Authorizer authorizer = authorizerFactory.getAuthorizer("ExactPatternApiKey");
        Assert.assertEquals(true, authorizer.authorize("project:env"));
        Assert.assertEquals(false, authorizer.authorize("project:env:whatever"));
        Assert.assertEquals(false, authorizer.authorize("whatever:project:env"));
    }

    @Test
    public void WildcardPattern() throws Exception {
        Authorizer authorizer = authorizerFactory.getAuthorizer("WildcardPatternApiKey");
        Assert.assertEquals(true, authorizer.authorize("project:env:whatever"));
        Assert.assertEquals(false, authorizer.authorize("project:envwhatever"));
        Assert.assertEquals(false, authorizer.authorize("whatever:project:env"));
        Assert.assertEquals(false, authorizer.authorize("project:whatever:env"));
    }

    @Test
    public void SeveralPatterns() throws Exception {
        Authorizer authorizer = authorizerFactory.getAuthorizer("SeveralPatternsApiKey");
        Assert.assertEquals(true, authorizer.authorize("project:dev"));
        Assert.assertEquals(true, authorizer.authorize("project:staging"));
        Assert.assertEquals(false, authorizer.authorize("project:prod"));
        Assert.assertEquals(true, authorizer.authorize("foo:bar"));
    }

    @Test
    public void IgnoringCasePatterns() throws Exception {
        Authorizer authorizer = authorizerFactory.getAuthorizer("IgnoringCasePatternsApiKey");
        Assert.assertEquals(true, authorizer.authorize("project:env"));
        Assert.assertEquals(true, authorizer.authorize("project:enV"));
        Assert.assertEquals(false, authorizer.authorize("project:env2"));
        Assert.assertEquals(true, authorizer.authorize("foo:bar:dev"));
        Assert.assertEquals(true, authorizer.authorize("foO:Bar:staging"));
        Assert.assertEquals(false, authorizer.authorize("_Foo:Bar"));
    }
}
