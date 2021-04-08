/**
 * Copyright © 2015 - 2021 SSHTOOLS Limited (support@sshtools.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.forker.wrapper;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplacementUtils {

	private static Logger logger = Logger.getLogger(ReplacementUtils.class.getSimpleName());
	
	public static String replaceSystemProperties(String value) {
		
		Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(value);

		StringBuilder builder = new StringBuilder();
		int i = 0;
		while (matcher.find()) {
			String attributeName = matcher.group(1);
			if(System.getProperty(attributeName)==null) {
				logger.log(Level.WARNING, "Replacement token " + attributeName + " not in list to replace from");
				continue;
			}
		    String replacement = System.getProperty(attributeName);
		    builder.append(value.substring(i, matcher.start()));
		    if (replacement == null) {
		        builder.append(matcher.group(0));
		    } else {
		        builder.append(replacement);
		    }
		    i = matcher.end();
		}
		
	    builder.append(value.substring(i, value.length()));
		
		return builder.toString();
	}

}
