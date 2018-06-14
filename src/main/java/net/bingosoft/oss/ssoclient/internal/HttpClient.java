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

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.Map;


public class HttpClient {

	/**
     * 使用http get方法调用指定url并返回结果
     */
	public static String get(String url) throws HttpException {
		return get(url,null,null);
	}

    /**
     * 使用http get方法调用指定url并返回结果
     */
    public static String get(String url,Map<String, String> queryParams,
            Map<String, String> headers) throws HttpException {
        try {
        	if(queryParams!=null && queryParams.size()>0){
        		for (Map.Entry<String, String> entry : queryParams.entrySet()) {
        			url=appendQueryParams(url, entry.getKey(), entry.getValue());
				}
        	}
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setDoInput(true);
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            InputStream is = null;
            InputStreamReader isr = null;
            BufferedReader reader = null;
            try {
                int code = connection.getResponseCode();

                if (code >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    is = connection.getErrorStream();
                    isr = new InputStreamReader(is, "UTF-8");
                    reader = new BufferedReader(isr);

                    StringBuilder sb = new StringBuilder();
                    do {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        sb.append(line);
                    } while (true);

                    throw new HttpException(code,
                            "get request [" + url + "] error with response code [" + code + "] " +
                                    "\nerror message: " + sb.toString());
                }

                is = connection.getInputStream();
                isr = new InputStreamReader(is,"UTF-8");
                reader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                do {
                    String line = reader.readLine();
                    if(null == line){
                        break;
                    }
                    sb.append(line);
                } while (true);
                return sb.toString();
            } finally {
                if(reader != null){
                    reader.close();
                }
                if(isr != null){
                    isr.close();
                }
                if (is != null) {
                    is.close();
                }
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String post(String url, Map<String, String> params,
                              Map<String, String> headers) throws HttpException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
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
                if (headers != null && !headers.isEmpty()) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
                try {
                    connection.connect();
                } catch (IOException e) {
                    throw new IOException(e.getMessage() + "[" + url + "]", e);
                }
                if (params != null && !params.isEmpty()) {
                    os = connection.getOutputStream();
                    osw = new OutputStreamWriter(os, "UTF-8");
                    writer = new BufferedWriter(osw);
                    StringBuilder paramsBuilder = new StringBuilder();
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        paramsBuilder.append(entry.getKey());
                        paramsBuilder.append("=");
                        paramsBuilder.append(Urls.encode(entry.getValue()));
                        paramsBuilder.append("&");
                    }
                    if (paramsBuilder.length() > 0) {
                        paramsBuilder.deleteCharAt(paramsBuilder.length() - 1);
                    }
                    if (paramsBuilder.length() > 0) {
                        writer.write(paramsBuilder.toString());
                        writer.flush();
                    }
                }

                int code = connection.getResponseCode();
                if (code >= HttpURLConnection.HTTP_BAD_REQUEST) {

                    is = connection.getErrorStream();
                    isr = new InputStreamReader(is, "UTF-8");
                    reader = new BufferedReader(isr);

                    StringBuilder sb = new StringBuilder();
                    do {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        sb.append(line);
                    } while (true);

                    throw new HttpException(code,
                            "post request [" + url + "] error with response code [" + code + "] " +
                                    "\nerror message: " + sb.toString());
                }
                is = connection.getInputStream();
                isr = new InputStreamReader(is, "UTF-8");
                reader = new BufferedReader(isr);

                StringBuilder sb = new StringBuilder();
                do {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line);
                } while (true);

                return sb.toString();
            } finally {
                if (writer != null) {
                    writer.close();
                }
                if (osw != null) {
                    osw.close();
                }
                if (os != null) {
                    os.close();
                }

                if (reader != null) {
                    reader.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (is != null) {
                    is.close();
                }
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String appendQueryString(String url, String queryString) {
	    if(null == url) {
	        return url;
	    }

		int index = url.lastIndexOf('?');
		if(index < 0) {
			return url + "?" + queryString;
		}else{
			if(index == url.length() - 1) {
				return url + queryString;
			}else{
				return url + "&" + queryString;
			}
		}
	}

	public static String appendQueryParams(String url, String name, String value) {
	    return appendQueryString(url, name + "=" + encode(value));
	}

	public static String encode(String url) {
	    if(null == url) {
	        return url;
	    }
		try {
	        return URLEncoder.encode(url,  "UTF-8");
        } catch (UnsupportedEncodingException e) {
        	throw new RuntimeException(e);
        }
	}

    public static String encode(String url, String charset) {
        if(null == url) {
            return url;
        }
        try {
            return URLEncoder.encode(url, charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void ignoreHttpsCer(){
        // Create a trust manager that does not validate certificate chains  
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType)  {

            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {

            }
        } };

        // Install the all-trusting trust manager  
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
