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

package net.bingosoft.oss.ssoclient.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {
    /**
     * 使用http get方法调用指定url并返回结果
     * @param url
     * @return
     */
    public static String get(String url){
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(3000);
            connection.connect();
            InputStream is = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            do{
                int i = is.read();
                if(i == -1){
                    break;
                }
                sb.append((char)i);
            }while (true);
            if(sb.length() > 0){
                return sb.toString();
            }
            throw new IOException("public key get from "+url+" is empty!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
