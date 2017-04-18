/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.bingosoft.oss.ssoclient;

import javax.servlet.http.HttpServletRequest;

/**
 * 工具类。
 */
public class SSOUtils {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER               = "Bearer";
    
    /**
     * todo : doc.
     */
    public static String extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if(header == null || header.trim().isEmpty()){
            return null;
        }
        header = header.trim();
        if(header.startsWith(BEARER)){
            header = header.substring(BEARER.length());
            return header.trim();
        }else {
            return header;
        }
    }

    protected SSOUtils() {

    }

}