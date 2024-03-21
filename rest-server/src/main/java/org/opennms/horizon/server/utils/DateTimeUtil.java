/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.server.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class DateTimeUtil {

    public static final String D_MM_YYYY_HH_MM_SS_SSS = "M/dd/yyyy HH:mm:ss.SSS";

    public static String convertAndFormatLongDate(Long longDate, String format) {
        if (longDate == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        String dateFormat = format == null ? D_MM_YYYY_HH_MM_SS_SSS : format;
        DateFormat dateFormatter = new SimpleDateFormat(dateFormat);
        Date dateTime = new Date(longDate.longValue());
        return dateFormatter.format(dateTime);
    }
}