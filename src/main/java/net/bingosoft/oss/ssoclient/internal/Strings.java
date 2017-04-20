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

public class Strings {
    public static String nullOrToString(Object o){
        if(o == null){
            return null;
        }
        return o.toString();
    }
    public static boolean isEmpty(String str){
        return str == null || str.trim().isEmpty();
    }
    public static boolean equals(String str1, String str2){
        if(str1 == null && str2 == null){
            return true;
        }else if(str1 == null){
            return false;
        }else {
            return str1.equals(str2);
        }
    }
}
