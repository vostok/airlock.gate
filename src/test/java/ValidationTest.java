import org.junit.Assert;
import org.junit.Test;
import ru.kontur.airlock.Validator;
import ru.kontur.airlock.ValidatorFactory;

import java.util.HashMap;

public class ValidationTest {
    private ValidatorFactory validatorFactory;

    public ValidationTest() {
        validatorFactory = new ValidatorFactory(new HashMap<String, String[]>() {
            {
                put("UniversalApiKey", new String[]{"*"});
                put("ExactPatternApiKey", new String[]{"project.env"});
                put("WildcardPatternApiKey", new String[]{"project.env.*"});
                put("SeveralPatternsApiKey", new String[]{"project.dev", "project.staging", "foo.bar"});
                put("IgnoringCasePatternsApiKey", new String[]{"project.Env", "Foo.Bar.*"});
            }
        });
    }

    @Test
    public void UnregisteredApiKey() throws Exception {
        Validator validator = validatorFactory.getValidator("UnregisteredApiKey");
        Assert.assertEquals(false, validator.validate("project.env"));
    }

    @Test
    public void ExactPattern() throws Exception {
        Validator validator = validatorFactory.getValidator("ExactPatternApiKey");
        Assert.assertEquals(true, validator.validate("project.env"));
        Assert.assertEquals(false, validator.validate("project.env.whatever"));
        Assert.assertEquals(false, validator.validate("whatever.project.env"));
    }

    @Test
    public void WildcardPattern() throws Exception {
        Validator validator = validatorFactory.getValidator("WildcardPatternApiKey");
        Assert.assertEquals(true, validator.validate("project.env.whatever"));
        Assert.assertEquals(false, validator.validate("project.envwhatever"));
        Assert.assertEquals(false, validator.validate("whatever.project.env"));
        Assert.assertEquals(false, validator.validate("project.whatever.env"));
    }

    @Test
    public void SeveralPatterns() throws Exception {
        Validator validator = validatorFactory.getValidator("SeveralPatternsApiKey");
        Assert.assertEquals(true, validator.validate("project.dev"));
        Assert.assertEquals(true, validator.validate("project.staging"));
        Assert.assertEquals(false, validator.validate("project.prod"));
        Assert.assertEquals(true, validator.validate("foo.bar"));
    }

    @Test
    public void IgnoringCasePatterns() throws Exception {
        Validator validator = validatorFactory.getValidator("IgnoringCasePatternsApiKey");
        Assert.assertEquals(true, validator.validate("project.env"));
        Assert.assertEquals(true, validator.validate("project.enV"));
        Assert.assertEquals(false, validator.validate("project.env2"));
        Assert.assertEquals(true, validator.validate("foo.bar.dev"));
        Assert.assertEquals(true, validator.validate("foO.Bar.staging"));
        Assert.assertEquals(false, validator.validate("_Foo.Bar"));
    }

    @Test
    public void ForbiddenCharacters() throws Exception {
        Validator validator = validatorFactory.getValidator("UniversalApiKey");
        Assert.assertEquals(true, validator.validate("project.env"));
        Assert.assertEquals(false, validator.validate("project:env"));
        Assert.assertEquals(false, validator.validate("project.env:"));
        Assert.assertEquals(false, validator.validate("@project.env"));
    }
}
