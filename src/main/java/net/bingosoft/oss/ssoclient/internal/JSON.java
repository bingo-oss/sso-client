package net.bingosoft.oss.ssoclient.internal;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * Created by kael on 2017/4/17.
 */
public class JSON {
    public static Map<String, Object> decodeToMap(String json){
        JSONObject obj = com.alibaba.fastjson.JSON.parseObject(json);
        return obj.toJavaObject(Map.class);
    }
}
