/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.commons.core.edm.primitivetype;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

/**
 * Implementation of the EDM primitive type DateTimeOffset.
 */
public final class EdmDateTimeOffset extends SingletonPrimitiveType {

    private static final ZoneOffset ZULU = ZoneOffset.UTC;

    private static final Pattern PATTERN = Pattern.compile("(-?\\p{Digit}{4,})-(\\p{Digit}{2})-(\\p{Digit}{2})"
            + "T(\\p{Digit}{2}):(\\p{Digit}{2})(?::(\\p{Digit}{2})(\\.(\\p{Digit}{0,12}?)0*)?)?"
            + "(Z|([-+]\\p{Digit}{2}:\\p{Digit}{2}))?");

    private static final EdmDateTimeOffset INSTANCE = new EdmDateTimeOffset();
    private static final DateTimeFormatter isoLocalTimeCommon = new DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).toFormatter();

    public static EdmDateTimeOffset getInstance() {
        return INSTANCE;
    }

    private static OffsetDateTime parseZonedDateTime(String value) throws DateTimeParseException {
        Matcher matcher = PATTERN.matcher(value); // Harmonize to ISO-8601 conform pattern

        return OffsetDateTime.parse(value + ((matcher.matches() && matcher.group(9) == null) ? "Z" : ""));
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertOffsetDateTime(OffsetDateTime odt, Class<T> returnType) {
        if (returnType == OffsetDateTime.class) {
            return (T) odt;
        } else if (returnType == Instant.class) {
            return (T) odt.toInstant();
        } else if (returnType.isAssignableFrom(Timestamp.class)) {
            return (T) Timestamp.from(odt.toInstant());
        } else if (returnType.isAssignableFrom(java.util.Date.class)) {
            return (T) java.util.Date.from(odt.toInstant());
        } else if (returnType.isAssignableFrom(java.sql.Time.class)) {
            return (T) new java.sql.Time(odt.toInstant().truncatedTo(ChronoUnit.SECONDS).toEpochMilli());
        } else if (returnType.isAssignableFrom(java.sql.Date.class)) {
            return (T) new java.sql.Date(odt.toInstant().truncatedTo(ChronoUnit.SECONDS).toEpochMilli());
        } else if (returnType.isAssignableFrom(Long.class)) {
            return (T) Long.valueOf(odt.toInstant().toEpochMilli());
        } else if (returnType.isAssignableFrom(Calendar.class)) {
            return (T) GregorianCalendar.from(odt.toZonedDateTime());
        } else if (returnType.isAssignableFrom(ZonedDateTime.class)) return (T) ZonedDateTime.from(odt);
        else throw new ClassCastException("Unsupported return type " + returnType.getSimpleName());

    }

/*  private static <T> ZonedDateTime createZonedDateTime(final T value) throws EdmPrimitiveTypeException {
        if (value instanceof ZonedDateTime) {
            return (ZonedDateTime) value;
        }

        if (value instanceof Instant) {
            return ((Instant) value).atZone(ZULU);
        }

        if (value instanceof GregorianCalendar) {
            GregorianCalendar calendar = (GregorianCalendar) value;
            ZonedDateTime zdt = calendar.toZonedDateTime();
            ZoneId normalizedZoneId = calendar.getTimeZone().toZoneId().normalized();
            return zdt.withZoneSameInstant(normalizedZoneId);
        }


        return convertToInstant(value).atZone(ZULU);
    }
     * Creates an {@link Instant} from the given value.
     *
     * @param value the value as {@link Instant}, {@link java.util.Date},
     *              {@link java.sql.Timestamp}, {@link Long} or
     *              {@link GregorianCalendar}
     * @return the value as {@link Instant}
     * @throws EdmPrimitiveTypeException if the type of the value is not supported
     *
     private static <T> Instant convertToInstant(final T value) throws EdmPrimitiveTypeException {
        if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toInstant();}
        else if (value instanceof Timestamp) {
            return ((Timestamp) value).toInstant();
        } else if (value instanceof java.util.Date) {
            return Instant.ofEpochMilli(((java.util.Date) value).getTime());
        } else if (value instanceof Long) {
            return Instant.ofEpochMilli((Long) value);
        } else {
            throw new EdmPrimitiveTypeException("The value type " + value.getClass() + " is not expected.");
        }
    }*/

    protected <T> String internalValueToString(T value,
                                               Boolean isNullable,
                                               Integer maxLength,
                                               Integer precision,
                                               Integer scale,
                                               Boolean isUnicode) throws EdmPrimitiveTypeException {
        OffsetDateTime odt = createOffsetDateTime(value);

        return format(odt, precision);
    }

    private static <T> OffsetDateTime createOffsetDateTime(T value) throws EdmPrimitiveTypeException {
        if (value instanceof OffsetDateTime) {
            return (OffsetDateTime) value;
        }
        if (value instanceof Instant) {
            return OffsetDateTime.ofInstant((Instant) value, ZULU);
        }

        /*if (value instanceof GregorianCalendar) {
            GregorianCalendar calendar = (GregorianCalendar) value;
            OffsetDateTime odt = calendar.toOffsetDateTime();
            ZoneId normalizedZoneId = calendar.getTimeZone().toZoneId().normalized();
            return odt.withZoneSameInstant(normalizedZoneId);
        }*/

        if (!((value instanceof Time) || (value instanceof java.sql.Date)) && (value instanceof java.util.Date)) {
            OffsetDateTime odt = Instant.ofEpochMilli(((java.util.Date) value).getTime()).atOffset(ZULU);

            return (value instanceof Timestamp) ? odt.withNano(((Timestamp) value).getNanos()) : odt;
        }
        if (value instanceof Long) return Instant.ofEpochMilli((Long) value).atOffset(ZULU);
        else throw new EdmPrimitiveTypeException("The value type " + value.getClass() + " is not supported.");
    }

    private static String format(OffsetDateTime dateTime, Integer _precision) {
        int precision = _precision == null ? 0 : _precision;

        DateTimeFormatter isoLocalTime = (precision == 0)
                ? new DateTimeFormatterBuilder().append(isoLocalTimeCommon).optionalStart().toFormatter()
                : new DateTimeFormatterBuilder().append(isoLocalTimeCommon).optionalStart()
                .appendFraction(NANO_OF_SECOND, 0, precision, true).toFormatter();

        DateTimeFormatter isoOffsetDateTime = new DateTimeFormatterBuilder()
                .append(ISO_LOCAL_DATE).appendLiteral('T').append(isoLocalTime)
                .parseLenient().appendOffsetId().parseStrict().toFormatter();

        return dateTime.format(isoOffsetDateTime);
    }

    @Override
    public Class<?> getDefaultType() { return OffsetDateTime.class; }

    protected <T> T internalValueOfString(String value, Boolean isNullable, Integer maxLength,
                                          Integer precision, Integer scale, Boolean isUnicode, Class<T> returnType)
            throws EdmPrimitiveTypeException {
        try {
            OffsetDateTime odt = parseZonedDateTime(value);

            return convertOffsetDateTime(odt, returnType);
        } catch (DateTimeParseException ex) {
            throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.", ex);
        } catch (ClassCastException e) {
            throw new EdmPrimitiveTypeException("The value type " + returnType + " is not supported.", e);
        }
    }

}