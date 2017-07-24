/*
 *
 *  * Copyright 2017 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  
 */

package tests;

import net.bingosoft.oss.ssoclient.SSOConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kael on 2017/4/18.
 */
public class SSOConfigTest {
    @Test
    public void testSettersAndGetters(){
        String ssoBaseUrl = "http://localhost:9999";
        SSOConfig config = new SSOConfig();
        config.setClientId("clientId");
        config.setClientSecret("clientSecret");
        config.autoConfigureUrls(ssoBaseUrl);
        Assert.assertEquals("clientId",config.getClientId());
        Assert.assertEquals("clientSecret",config.getClientSecret());
        Assert.assertEquals(ssoBaseUrl+"/oauth2/publickey",config.getPublicKeyEndpointUrl());
        Assert.assertEquals(ssoBaseUrl+"/oauth2/token",config.getTokenEndpointUrl());
        Assert.assertEquals(ssoBaseUrl+"/oauth2/authorize",config.getAuthorizationEndpointUrl());
        Assert.assertEquals(ssoBaseUrl+"/oauth2/logout",config.getOauthLogoutEndpoint());
    }
}
