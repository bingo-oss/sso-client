package tests;

import bingoee.sso.client.web.Client;
import bingoee.sso.client.web.WebAppConfig;
import bingoee.sso.client.web.verify.TokenManager;
import bingoee.sso.client.web.verify.impl.TokenManagerFactory;
import org.junit.Test;

/**
 * Created by kael on 2017/4/13.
 */
public class TokenManagerTest {
    
    private TokenManager manager = TokenManagerFactory.generateManager(new WebAppConfig() {
        @Override
        public String getSSOEndpoint() {
            return null;
        }

        @Override
        public String getAuthorizationEndpoint() {
            return null;
        }

        @Override
        public Client getClient() {
            return new Client() {
                @Override
                public String getId() {
                    return "clientId";
                }

                @Override
                public String getSecret() {
                    return "clientSecret";
                }
            };
        }

        @Override
        public String getSSOInnerEndpoint() {
            return null;
        }

        @Override
        public String getSSOPublicKeyEndpoint() {
            return null;
        }

        @Override
        public String getSSOTokenEndpoint() {
            return null;
        }

        @Override
        public String getSSOInnerTokenEndpoint() {
            return null;
        }

        @Override
        public String getSSOInnerPublicKeyEndpoint() {
            return null;
        }
    });
    @Test
    public void testVerifyIdToken(){
        // TODO add test code
    }
    
    public void testFetchAccessToken(){
        // TODO add test code
    }
    
}
