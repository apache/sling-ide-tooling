/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.eclipse.ui.views;

import java.util.Calendar;
import java.util.Date;

import org.apache.jackrabbit.util.ISO8601;

/**
 * Converts between {@link Calendar} and {@link String} according to 
 * <a href="https://developer.adobe.com/experience-manager/reference-materials/spec/jcr/2.0/3_Repository_Model.html#3.6.4.3%20From%20DATE%20To">
 * JCR Spec 2.0, Chapter 3.6.4</a>
 *
 */
public class DateTimeSupport {

    public static Date parseAsDate(String vaultDateTime) {
        return parseAsCalendar(vaultDateTime).getTime();
    }
    
    public static Calendar parseAsCalendar(String vaultDateTime) {
        final Calendar result = ISO8601.parse(vaultDateTime);
        return result;
    }
    
    public static String print(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return print(c);
    }
    
    public static String print(Calendar c) {
    	return ISO8601.format(c);
    }
}
