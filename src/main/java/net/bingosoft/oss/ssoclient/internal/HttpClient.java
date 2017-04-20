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

import net.bingosoft.oss.ssoclient.exception.HttpException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpClient {
    /**
     * 使用http get方法调用指定url并返回结果
     * @param url
     * @return
     */
    public static String get(String url) throws HttpException {
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(3000);
            InputStream is = null;
            try {
                connection.connect();
                is = connection.getInputStream();
                StringBuilder sb = new StringBuilder();
                do{
                    int i = is.read();
                    if(i == -1){
                        break;
                    }
                    sb.append((char)i);
                }while (true);
                return sb.toString();
            } finally {
                if(is != null){
                    is.close();
                }
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String post(String url, Map<String, String> params, Map<String, String> headers) throws HttpException{
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(3000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            OutputStream os = null;
            OutputStreamWriter osw = null;
            BufferedWriter writer = null;
            
            InputStream is = null;
            InputStreamReader isr = null;
            BufferedReader reader = null;
            try {
                if(headers != null && !headers.isEmpty()){
                    for (Map.Entry<String,String> entry : headers.entrySet()){
                        connection.setRequestProperty(entry.getKey(),entry.getValue());
                    }
                }
                connection.connect();
                if(params != null && !params.isEmpty()){
                    os = connection.getOutputStream();
                    osw = new OutputStreamWriter(os,"UTF-8");
                    writer = new BufferedWriter(osw);
                    StringBuilder paramsBuilder = new StringBuilder();
                    for (Map.Entry<String, String> entry : params.entrySet()){
                        paramsBuilder.append(entry.getKey());
                        paramsBuilder.append("=");
                        paramsBuilder.append(entry.getValue());
                        paramsBuilder.append("&");
                    }
                    if(paramsBuilder.length() > 0){
                        paramsBuilder.deleteCharAt(paramsBuilder.length()-1);
                    }
                    if(paramsBuilder.length() > 0){
                        writer.write(paramsBuilder.toString());
                        writer.flush();
                    }
                }
                
                int code = connection.getResponseCode();
                if(code >= HttpURLConnection.HTTP_BAD_REQUEST){
                    
                    is = connection.getErrorStream();
                    isr = new InputStreamReader(is,"UTF-8");
                    reader = new BufferedReader(isr);
                    
                    StringBuilder sb = new StringBuilder();
                    do{
                        String line = reader.readLine();
                        if(line == null){
                            break;
                        }
                        sb.append(line);
                    }while (true);
                    
                    throw new HttpException(code, "post request [" + url + "] error with response code ["+code+"] " +
                            "\nerror message: " + sb.toString());
                }
                is = connection.getInputStream();
                isr = new InputStreamReader(is,"UTF-8");
                reader = new BufferedReader(isr);
                
                StringBuilder sb = new StringBuilder();
                do{
                    String line = reader.readLine();
                    if(line == null){
                        break;
                    }
                    sb.append(line);
                }while (true);
                
                return sb.toString();
            }finally {
                if(writer != null){
                    writer.close();
                }
                if(osw != null){
                    osw.close();
                }
                if(os != null){
                    os.close();
                }
                
                if(reader != null){
                    reader.close();
                }
                if(isr != null){
                    isr.close();
                }
                if(is != null){
                    is.close();
                }
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
