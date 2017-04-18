package tests;

import net.bingosoft.oss.ssoclient.internal.Base64;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * Created by kael on 2017/4/17.
 */
public class Base64Test {
    @Test
    public void testMimeDecode() throws UnsupportedEncodingException {
        String pk = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDASOjIWexLpnXiJNJF2pL6NzP\n" +
                "fBoF0tKEr2ttAkJ/7f3uUHhj2NIhQ01Wu9OjHfXjCvQSXMWqqc1+O9G1UwB2Xslb\n" +
                "WNwEZFMwmQdP5VleGbJLR3wOl3IzdggkxBJ1Q9rXUlVtslK/CsMtkwkQEg0eZDH1\n" +
                "VeJXqKBlEhsNckYIGQIDAQAB";
        byte[] bytesJdk6 = Base64.urlDecode(pk);
        //byte[] bytesJdk8 = java.util.Base64.getMimeDecoder().decode(pk);
        byte[] bytesJdk8 = new byte[]{48,-127,-97,48,13,6,9,42,-122,72,-122,-9,13,
                1,1,1,5,0,3,-127,-115,0,48,-127,-119,2,-127,-127,0,-61,1,35,-93,
                33,103,-79,46,-103,-41,-120,-109,73,23,106,75,-24,-36,-49,124,26,
                5,-46,-46,-124,-81,107,109,2,66,127,-19,-3,-18,80,120,99,-40,-46,
                33,67,77,86,-69,-45,-93,29,-11,-29,10,-12,18,92,-59,-86,-87,-51,
                126,59,-47,-75,83,0,118,94,-55,91,88,-36,4,100,83,48,-103,7,79,
                -27,89,94,25,-78,75,71,124,14,-105,114,51,118,8,36,-60,18,117,67,
                -38,-41,82,85,109,-78,82,-65,10,-61,45,-109,9,16,18,13,30,100,49,
                -11,85,-30,87,-88,-96,101,18,27,13,114,70,8,25,2,3,1,0,1};
        Assert.assertArrayEquals(bytesJdk6,bytesJdk8);
        
        Assert.assertNull(Base64.urlDecode(null));
    }
    @Test
    public void testUrlDecode() throws UnsupportedEncodingException {
        String str = "rig2Y67pkpxxfJxZD9gyKCCwQK5K9bS5w6FcDhnkJWc8FEXZEn3kICByb2W9PivouRc5l2_9N4dVXyEH1s2k17Jp9aAWU7AFEWwtjdRQe7UIjCxock--FOUzuUKZhrI1tgeVHP4p-NNnkh-at43NxEI63HLOKvCo67R3QgK3wrg";
        byte[] bytesJdk6 = Base64.urlDecode(str);
        //byte[] bytesJdk8 = java.util.Base64.getUrlDecoder().decode(str.getBytes(CharsetName.UTF8));
        byte[] bytesJdk8 = new byte[]{-82,40,54,99,-82,-23,-110,-100,113,124,-100,89,15,-40,50,40,32,
                -80,64,-82,74,-11,-76,-71,-61,-95,92,14,25,-28,37,103,60,20,69,-39,18,125,-28,32,32,
                114,111,101,-67,62,43,-24,-71,23,57,-105,111,-3,55,-121,85,95,33,7,-42,-51,-92,-41,
                -78,105,-11,-96,22,83,-80,5,17,108,45,-115,-44,80,123,-75,8,-116,44,104,114,79,-66,
                20,-27,51,-71,66,-103,-122,-78,53,-74,7,-107,28,-2,41,-8,-45,103,-110,31,-102,-73,
                -115,-51,-60,66,58,-36,114,-50,42,-16,-88,-21,-76,119,66,2,-73,-62,-72};
        Assert.assertArrayEquals(bytesJdk6,bytesJdk8);
    }
    
}
