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
package org.opennms.horizon.minion.syslog;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * <p>This class can parse grok pattern strings to create a parser that can
 * assign parsed values to a syslog message object. It supports the following
 * pattern types:</p>
 * <ul>
 * <li>HOSTNAME: any valid hostname character.</li>   
 * <li>HOSTNAMEORIP: any valid hostname or IP address character.</li>  
 * <li>INT: Positive integer.</li>
 * <li>IPADDRESS: any valid IP address character.</li> 
 * <li>MONTH: 3-character English month abbreviation.</li>
 * <li>NOSPACE: String that contains no whitespace.</li>
 * <li>STRING: String. Because this matches any character, it must be followed by a delimiter in the pattern string.</li>
 * <li>WHITESPACE: String that contains only whitespace (spaces and/or tabs).</li>
 * </ul>
 * 
 * @author Seth
 */
public abstract class GrokParserStageSequenceBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(GrokParserStageSequenceBuilder.class);

	private static enum GrokState {
		TEXT,
		ESCAPE_PATTERN,
		START_PATTERN,
		PATTERN,
		SEMANTIC,
		END_PATTERN
	}

	enum GrokPattern {
		CHAR,
		HOSTNAME,
		HOSTNAMEORIP,
		INT,
		IPADDRESS,
		MONTH,
		NOSPACE,
		STRING,
		WHITESPACE
	}

	/**
	 * This enum contains all well-known syslog message fields.
	 */
	public static enum SyslogSemanticType {

		facilityPriority,

		version,


		year,


		month,


		day,


		hour,


		minute,


		second,


		secondFraction,


		timezone,

		hostname,

		processName,


		processId,


		messageId,

		message
	}

	/**
	 * The prefix for a generic parameter.
	 */
	private static final String PARAMETER_PREFIX = "parm";

	/**
	 * Used to ignore the value for the matched field.
	 */
	private static final String IGNORE_FIELD = "ignore";

	/**
	 * This function maps {@link SyslogSemanticType} values of type int to fields in the parser
	 * state.
	 * 
	 * @param semanticString
	 * @return
	 */
	private static BiConsumer<ParserState,Integer> semanticIntegerToField(String semanticString) {
		if (semanticString.startsWith(PARAMETER_PREFIX)) {
			return (s, v) -> s.message.addParameter(semanticString.substring(PARAMETER_PREFIX.length()), v.toString());
		}
		
		SyslogSemanticType semanticType = null;
		try {
			semanticType = SyslogSemanticType.valueOf(semanticString);
		} catch (IllegalArgumentException e) {
			// Leave semanticType == null
		}

		if (semanticType == null) {
			return (s,v) -> {
				s.message.setParam(semanticString, v);
			};
		} else {
			switch(semanticType) {
			case day:
				return (s,v) -> {
					s.message.setDayOfMonth(v);
				};
			case facilityPriority:
				return (s,v) -> {
					SyslogFacility facility = SyslogFacility.getFacilityForCode(v);
					SyslogSeverity priority = SyslogSeverity.getSeverityForCode(v);

					s.message.setFacility(facility);
					s.message.setSeverity(priority);
				};
			case hour:
				return (s,v) -> {
					s.message.setHourOfDay(v);
				};
			case minute:
				return (s,v) -> {
					s.message.setMinute(v);
				};
			case month:
				return (s,v) -> {
					s.message.setMonth(v);
				};
			case processId:
				// processId can be an integer or string
				return (s,v) -> {
					s.message.setProcessId(String.valueOf(v));
				};
			case second:
				return (s,v) -> {
					s.message.setSecond(v);
				};
			// TODO: This should be handled as a string... this is only
			// in here as a stopgap until we create a DIGITS pattern type.
			case secondFraction:
				return (s,v) -> {
					if (v >= 1000) {
						s.message.setMillisecond(Math.round(v / 1000f));
					} else {
						s.message.setMillisecond(v);
					}
				};
			case version:
				return (s,v) -> {
					s.message.setVersion(v);
				};
			case year:
				return (s,v) -> {
					s.message.setYear(v);
				};
			default:
				throw new IllegalArgumentException(String.format("Semantic type %s does not have an integer value", semanticString));
			}
		}

	}

	/**
	 * This function maps {@link SyslogSemanticType} values of type String to fields in the parser
	 * state.
	 * 
	 * @param semanticString
	 * @return
	 */
	private static BiConsumer<ParserState,String> semanticStringToField(String semanticString) {
		// Discard this match
		if (semanticString.equalsIgnoreCase(IGNORE_FIELD)) {
			return (s, v) -> {};
		}

		if (semanticString.startsWith(PARAMETER_PREFIX)) {
			return (s, v) -> s.message.addParameter(semanticString.substring(PARAMETER_PREFIX.length()), v);
		}
		
		SyslogSemanticType semanticType = null;
		try {
			semanticType = SyslogSemanticType.valueOf(semanticString);
		} catch (IllegalArgumentException e) {
			// Leave semanticType == null
		}

		if (semanticType == null) {
			return (s,v) -> {
				s.message.setParam(semanticString, v);
			};
		} else {
			switch(semanticType) {
			case hostname:
				return (s,v) -> {
					s.message.setHostName(v);
				};
			case message:
				return (s,v) -> {
					// Trim the message to match behavior of legacy parsers
					s.message.setMessage(v == null ? null : v.trim());
				};
			case messageId:
				// Unique to this parser
				return (s,v) -> {
					if ("-".equals(v.trim())) {
						// Ignore
					} else {
						s.message.setMessageID(v);
					}
				};
			case processId:
				// processId can be an integer or string
				return (s,v) -> {
					if ("-".equals(v.trim())) {
						// Ignore
					} else {
						s.message.setProcessId(v);
					}
				};
			case processName:
				return (s,v) -> {
					if ("-".equals(v.trim())) {
						// Ignore
					} else {
						s.message.setProcessName(v);
					}
				};
			case secondFraction:
				return (s,v) -> {
					// Convert the fraction value into a millisecond value since that
					// is the best resolution that EventBuilder can handle
					switch(v.length()) {
					case 1:
						s.message.setMillisecond(ParserStageSequenceBuilder.MatchInteger.trimAndConvert(v) * 100);
						break;
					case 2:
						s.message.setMillisecond(ParserStageSequenceBuilder.MatchInteger.trimAndConvert(v) * 10);
						break;
					case 3:
						s.message.setMillisecond(ParserStageSequenceBuilder.MatchInteger.trimAndConvert(v));
						break;
					case 4:
						s.message.setMillisecond(ParserStageSequenceBuilder.MatchInteger.trimAndConvert(v) / 10);
						break;
					case 5:
						s.message.setMillisecond(ParserStageSequenceBuilder.MatchInteger.trimAndConvert(v) / 100);
						break;
					case 6:
						s.message.setMillisecond(ParserStageSequenceBuilder.MatchInteger.trimAndConvert(v) / 1000);
						break;
					}
				};
			case month:
				return (s, v) -> {
					int month;
					try {
						month = new SimpleDateFormat("MMM", Locale.ENGLISH).parse(v).toInstant().atZone(
								ZoneId.systemDefault()).toLocalDate().getMonthValue();

					} catch (ParseException e) {
						throw new IllegalArgumentException(String.format("Could not parse month '%s'", v));
					}
					s.message.setMonth(month);
				};
			case timezone:
				return (s,v) -> {
					ZoneId zoneId;
					try {
						zoneId = ZonedDateTimeBuilder.parseZoneId(v);
					} catch (ZoneRulesException zre) {
						// Zone was not found, attempt to convert the zone to uppercase
						// i.e. if the string is 'cst', then the lookup will fail unless we query for 'CST'
						zoneId = ZonedDateTimeBuilder.parseZoneId(v.toUpperCase());
					}
					s.message.setZoneId(zoneId);
				};
			default:
				throw new IllegalArgumentException(String.format("Semantic type %s does not have a string value", semanticString));
			}
		}
	}

	public static List<ParserStage> parseGrok(String grok) {
		GrokState state = GrokState.TEXT;
		ParserStageSequenceBuilder factory = new ParserStageSequenceBuilder();

		StringBuilder pattern = new StringBuilder();
		StringBuilder semantic = new StringBuilder();

		for (char c : grok.toCharArray()) {
			switch(state) {
			case TEXT:
				switch(c) {
				case '%':
					state = GrokState.START_PATTERN;
					continue;
				case '\\':
					state = GrokState.ESCAPE_PATTERN;
					continue;
				case ' ':
					factory = factory.whitespace();
					continue;
				default:
					factory = factory.character(c);
					continue;
				}
			case ESCAPE_PATTERN:
				switch(c) {
				default:
					factory = factory.character(c);
					state = GrokState.TEXT;
					continue;
				}
			case START_PATTERN:
				switch(c) {
				case '{':
					state = GrokState.PATTERN;
					continue;
				default:
					throw new IllegalStateException("Illegal character to start pattern");
				}
			case PATTERN:
				switch(c) {
				case ':':
					state = GrokState.SEMANTIC;
					continue;
				default:
					pattern.append(c);
					continue;
				}
			case SEMANTIC:
				switch(c) {
				case '}':
					state = GrokState.END_PATTERN;
					continue;
				default:
					semantic.append(c);
					continue;
				}
			case END_PATTERN:
				final String patternString = pattern.toString();
				final String semanticString = semantic.toString();

				GrokPattern patternType = GrokPattern.valueOf(patternString);

				switch(c) {
				case '\\':
					switch(patternType) {
					case CHAR:
						throw new UnsupportedOperationException("Cannot support escape sequence directly after a CHAR pattern yet");
					case HOSTNAME:
					case HOSTNAMEORIP:
					case IPADDRESS:
						// TODO: We need to peek forward to the escaped character and then do the same as the default case
						// factory.stringUntil(String.valueOf(c), semanticStringToEventBuilder(semanticString));
						// factory.character(c);
						// break;
						throw new UnsupportedOperationException("Cannot support escape sequence directly after a " +
								patternType + " pattern yet");
					case NOSPACE:
						// TODO: We need to peek forward to the escaped character and then do the same as the default case
						// factory.stringUntil(MatchUntil.WHITESPACE + c, semanticStringToEventBuilder(semanticString));
						// factory.character(c);
						// break;
						throw new UnsupportedOperationException("Cannot support escape sequence directly after a NOSPACE pattern yet");
					case WHITESPACE:
						throw new UnsupportedOperationException("Cannot support escape sequence directly after a WHITESPACE pattern yet");
					case STRING:
						// TODO: We need to peek forward to the escaped character and then do the same as the default case
						// factory.stringUntil(String.valueOf(c), semanticStringToEventBuilder(semanticString));
						// factory.character(c);
						// break;
						throw new UnsupportedOperationException("Cannot support escape sequence directly after a STRING pattern yet");
					case INT:
						factory.integer(semanticIntegerToField(semanticString));
						break;
					case MONTH:
						factory.monthString(semanticIntegerToField(semanticString));
						break;
					}
					pattern = new StringBuilder();
					semantic = new StringBuilder();
					state = GrokState.ESCAPE_PATTERN;
					continue;
				case '%':
					switch(patternType) {
					case CHAR:
						factory.character(semanticStringToField(semanticString));
						break;
					case NOSPACE:
						// This is probably not an intended behavior
						LOG.warn("NOSPACE pattern followed immediately by another pattern will greedily consume until whitespace is encountered");
						factory.stringUntilWhitespace(semanticStringToField(semanticString));
						factory.whitespace();
						break;
					case WHITESPACE:
						factory.stringUntilNonWhitespace(semanticStringToField(semanticString));
						break;
					case STRING:
					case HOSTNAME:
					case HOSTNAMEORIP:
					case IPADDRESS:
						// TODO: Can we handle this case?
						throw new IllegalArgumentException(String.format("Invalid pattern: %s:%s does not have a trailing delimiter, cannot determine end of string", patternString, semanticString));
					case INT:
						factory.integer(semanticIntegerToField(semanticString));
						break;
					case MONTH:
						factory.monthString(semanticIntegerToField(semanticString));
						break;
					}
					pattern = new StringBuilder();
					semantic = new StringBuilder();
					state = GrokState.START_PATTERN;
					continue;
				case ' ':
					switch(patternType) {
					case CHAR:
						factory.character(semanticStringToField(semanticString));
						break;
					case NOSPACE:
					case STRING:
						factory.stringUntilWhitespace(semanticStringToField(semanticString));
						factory.whitespace();
						break;
					case WHITESPACE:
						factory.stringUntilNonWhitespace(semanticStringToField(semanticString));
					case HOSTNAME:
					case HOSTNAMEORIP:
					case IPADDRESS:
						factory.hostMatcherForPattern(patternType, semanticStringToField(semanticString));
						factory.whitespace();
						break;
					case INT:
						factory.intUntilWhitespace(semanticIntegerToField(semanticString));
						factory.whitespace();
						break;
					case MONTH:
						factory.monthString(semanticIntegerToField(semanticString));
						factory.whitespace();
						break;
					}
					break;
				default:
					switch(patternType) {
					case CHAR:
						factory.character(semanticStringToField(semanticString));
						break;

					case WHITESPACE:
						factory.stringUntilNonWhitespace(semanticStringToField(semanticString));
						factory.character(c);
						break;
					case STRING:
						factory.stringUntil(String.valueOf(c), semanticStringToField(semanticString));
						factory.character(c);
						break;
					case INT:
						factory.integer(semanticIntegerToField(semanticString));
						factory.character(c);
						break;
					case MONTH:
						factory.monthString(semanticIntegerToField(semanticString));
						factory.character(c);
						break;
					case HOSTNAME:
					case HOSTNAMEORIP:
					case IPADDRESS:
						factory.hostUntilForPattern(patternType, String.valueOf(c),
								semanticStringToField(semanticString));
						factory.character(c);
					}
				}
				pattern = new StringBuilder();
				semantic = new StringBuilder();
				state = GrokState.TEXT;
				continue;
			}
		}

		// If we are in the process of ending a pattern, then tie it up with a bow
		if (state == GrokState.END_PATTERN) {
			final String patternString = pattern.toString();
			final String semanticString = semantic.toString();

			GrokPattern patternType = GrokPattern.valueOf(patternString);

			switch(patternType) {
			case NOSPACE:
			case WHITESPACE:
			case STRING:
				factory.terminal().string(semanticStringToField(semanticString));
				break;
			case HOSTNAME:
			case HOSTNAMEORIP:
			case IPADDRESS:
				factory.terminal().hostMatcherForPattern(patternType, semanticStringToField(semanticString));
				break;
			case INT:
				factory.terminal().integer(semanticIntegerToField(semanticString));
				break;
			case MONTH:
				factory.terminal().monthString(semanticIntegerToField(semanticString));
				break;
			}
		}

		return factory.getStages();
	}
}
