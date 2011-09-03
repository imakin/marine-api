/* 
 * SentenceParser.java
 * Copyright (C) 2010 Kimmo Tuukkanen
 * 
 * This file is part of Java Marine API.
 * <http://sourceforge.net/projects/marineapi/>
 * 
 * Java Marine API is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * Java Marine API is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java Marine API. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.marineapi.nmea.parser;

import java.util.ArrayList;
import java.util.List;

import net.sf.marineapi.nmea.sentence.Checksum;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.SentenceValidator;
import net.sf.marineapi.nmea.sentence.TalkerId;

/**
 * <p>
 * Base sentence parser for all NMEA sentence types. Provides general services
 * such as sentence validation and data field setters and getters with
 * formatting.
 * <p>
 * NMEA data is transmitted in form of ASCII Strings that are called
 * <em>sentences</em>. Each sentence starts with a '$', a two letter
 * <em>talker ID</em>, a three letter <em>sentence ID</em>, followed by a number
 * of <em>data fields</em> separated by commas, and terminated by an
 * <em>optional checksum</em>, and a carriage return/line feed. A sentence may
 * contain up to 82 characters including the '$' and <code>CR/LF</code>. If data
 * for a field is not available, the field is simply omitted, but the commas
 * that would delimit it are still sent, with no space between them.
 * <p>
 * Sentence structure:<br>
 * <code>
 * $&lt;id&gt;,&lt;field #0&gt;,&lt;field #1&gt;,...,&lt;field #n&gt;*&lt;checksum&gt;(CR/LF)
 * </code>
 * <p>
 * For more details, see <a
 * href="http://vancouver-webpages.com/peter/nmeafaq.txt">NMEA FAQ</a> by Peter
 * Bennett or <a href="http://gpsd.berlios.de/NMEA.txt">NMEA Revealed</a> by
 * Eric S. Raymond. The Java Marine API is based mostly on these documents.
 * 
 * @author Kimmo Tuukkanen
 * @version $Revision$
 */
public class SentenceParser implements Sentence {

    // The first two characters after '$'.
    private TalkerId talkerId;

    // The next three characters after talker id.
    private final String sentenceId;

    // actual data fields (address and checksum omitted)
    private List<String> fields;

    /**
     * Creates a new instance of SentenceParser. Validates the input String and
     * resolves talker id and sentence type.
     * 
     * @param nmea A valid NMEA 0183 sentence
     * @throws IllegalArgumentException If the specified sentence is invalid or
     *             if sentence type is not supported.
     */
    public SentenceParser(String nmea) {

        if (!SentenceValidator.isValid(nmea)) {
            String msg = String.format("Invalid data [%s]", nmea);
            throw new IllegalArgumentException(msg);
        }

        talkerId = TalkerId.parse(nmea);
        sentenceId = SentenceId.parseStr(nmea);

        // remove address field
        int begin = nmea.indexOf(Sentence.FIELD_DELIMITER);
        String temp = nmea.substring(begin + 1);

        // remove checksum
        if (temp.contains(String.valueOf(CHECKSUM_DELIMITER))) {
            int end = temp.indexOf(CHECKSUM_DELIMITER);
            temp = temp.substring(0, end);
        }

        // copy data fields to list
        String[] temp2 = temp.split(String.valueOf(FIELD_DELIMITER), -1);
        fields = new ArrayList<String>(temp2.length);
        for (String s : temp2) {
            fields.add(s);
        }
    }

    /**
     * Creates a new instance of SentenceParser. Parser may be constructed only
     * if parameter <code>nmea</code> contains a valid NMEA 0183 sentence of the
     * specified <code>type</code>.
     * <p>
     * For example, GGA sentence parser should specify "GGA" as the type.
     * 
     * @param nmea NMEA 0183 sentence String
     * @param type Expected type of the sentence in <code>nmea</code> parameter
     * @throws IllegalArgumentException If the specified sentence is not a valid
     *             or is not of expected type.
     */
    protected SentenceParser(String nmea, String type) {
        this(nmea);
        if (type == null || "".equals(type)) {
            throw new IllegalArgumentException(
                    "Sentence type must be specified.");
        }
        String sid = getSentenceId();
        if (!sid.equals(type)) {
            String ptrn = "Sentence id mismatch; expected [%s], found [%s].";
            String msg = String.format(ptrn, type, sid);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Creates a new empty sentence with specified talker and sentence IDs.
     * 
     * @param talker Talker type Id, e.g. "GP" or "LC".
     * @param type Sentence type Id, e.g. "GGA or "GLL".
     * @param size Number of data fields
     */
    protected SentenceParser(TalkerId talker, String type, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Minimum number of fields is 1");
        }
        if (talker == null) {
            throw new IllegalArgumentException("Talker ID must be specified");
        }
        if (type == null || "".equals(type)) {
            throw new IllegalArgumentException("Sentence ID must be specified");
        }

        talkerId = talker;
        sentenceId = type;
        fields = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            fields.add("");
        }
    }

    /**
     * Creates a new instance of SentenceParser with specified sentence data.
     * Type of the sentence is checked against the specified expected sentence
     * type id.
     * 
     * @param nmea Sentence String
     * @param type Sentence type enum
     */
    SentenceParser(String nmea, SentenceId type) {
        this(nmea, type.toString());
    }

    /**
     * Creates a new instance of SentenceParser without any data.
     * 
     * @param tid Talker id to set in sentence
     * @param sid Sentence id to set in sentence
     * @param size Number of data fields following the sentence id field
     */
    SentenceParser(TalkerId tid, SentenceId sid, int size) {
        this(tid, sid.toString(), size);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof SentenceParser) {
            Sentence s = (Sentence) o;
            return s.toString().equals(toString());
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see net.sf.marineapi.nmea.sentence.Sentence#getFieldCount()
     */
    public final int getFieldCount() {
        return fields.size();
    }

    /*
     * (non-Javadoc)
     * @see net.sf.marineapi.nmea.sentence.Sentence#getSentenceId()
     */
    public final String getSentenceId() {
        return sentenceId;
    }

    /*
     * (non-Javadoc)
     * @see net.sf.marineapi.nmea.sentence.Sentence#getTalkerId()
     */
    public final TalkerId getTalkerId() {
        return talkerId;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /*
     * (non-Javadoc)
     * @see
     * net.sf.marineapi.nmea.sentence.Sentence#setTalkerId(net.sf.marineapi.
     * nmea.util.TalkerId)
     */
    public final void setTalkerId(TalkerId id) {
        this.talkerId = id;
    }

    /*
     * (non-Javadoc)
     * @see net.sf.marineapi.nmea.sentence.Sentence#toSentence()
     */
    public final String toSentence() {
        String sentence = toString();
        if (!SentenceValidator.isValid(sentence)) {
            String msg = String.format("Invalid result [%s]", sentence);
            throw new IllegalStateException(msg);
        }
        return sentence;
    }

    /**
     * Returns the String representation of the sentence (without line
     * terminator CR/LR). Checksum is calculated and appended at the end of the
     * sentence, but no validation is done. Use {@link #toSentence()} to also
     * validate the result.
     * 
     * @return String representation of sentence
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder(MAX_LENGTH);
        sb.append(BEGIN_CHAR);
        sb.append(talkerId.toString());
        sb.append(sentenceId);

        for (String field : fields) {
            sb.append(FIELD_DELIMITER);
            sb.append(field);
        }

        String sentence = Checksum.add(sb.toString());

        return sentence;
    }

    /**
     * Parse a single character from the specified sentence field.
     * 
     * @param index Data field index in sentence
     * @return Character contained in the field
     * @throws ParseException If field contains more than one character
     */
    protected final char getCharValue(int index) {
        String val = getStringValue(index);
        if (val.length() > 1) {
            String msg = String.format("Expected char, found String [%s]", val);
            throw new ParseException(msg);
        }
        return val.charAt(0);
    }

    /**
     * Parse double value from the specified sentence field.
     * 
     * @param index Data field index in sentence
     * @return Field as parsed by {@link java.lang.Double#parseDouble(String)}
     */
    protected final double getDoubleValue(int index) {
        double value;
        try {
            value = Double.parseDouble(getStringValue(index));
        } catch (NumberFormatException ex) {
            throw new ParseException("Field does not contain double value", ex);
        }
        return value;
    }

    /**
     * Parse integer value from the specified sentence field.
     * 
     * @param index Field index in sentence
     * @return Field parsed by {@link java.lang.Integer#parseInt(String)}
     */
    protected final int getIntValue(int index) {
        int value;
        try {
            value = Integer.parseInt(getStringValue(index));
        } catch (NumberFormatException ex) {
            throw new ParseException("Field does not contain integer value", ex);
        }
        return value;
    }

    /**
     * Get contents of a data field as a String. Field indexing is zero-based.
     * The address field (e.g. <code>$GPGGA</code>) and checksum at the end are
     * not considered as a data fields and cannot therefore be fetched with this
     * method.
     * <p>
     * Field indexing, let i = 1: <br>
     * <code>$&lt;id&gt;,&lt;i&gt;,&lt;i+1&gt;,&lt;i+2&gt;,...,&lt;i+n&gt;*&lt;checksum&gt;</code>
     * 
     * @param index Field index
     * @return Field value as String
     * @throws DataNotAvailableException If the field is empty
     */
    protected final String getStringValue(int index) {
        String value = fields.get(index);
        if (value == null || "".equals(value)) {
            throw new DataNotAvailableException("Data not available");
        }
        return value;
    }

    /**
     * Tells is if the field specified by the given index contains a value.
     * 
     * @param index Field index
     * @return True if field contains value, otherwise false.
     */
    protected final boolean hasValue(int index) {
        boolean result = true;
        try {
            getStringValue(index);
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * Set a character in specified field.
     * 
     * @param index Field index
     * @param value Value to set
     */
    protected final void setCharValue(int index, char value) {
        setStringValue(index, String.valueOf(value));
    }

    /**
     * Set double value in specified field.
     * 
     * @param index Field index
     * @param value Value to set
     */
    protected final void setDoubleValue(int index, double value) {
        // TODO add support for rounding to defined decimal precision
        setStringValue(index, String.valueOf(value));
    }

    /**
     * Set integer value in specified field.
     * 
     * @param index Field index
     * @param value Value to set
     */
    protected final void setIntValue(int index, int value) {
        setStringValue(index, String.valueOf(value));
    }

    /**
     * Set String value in specified data field.
     * 
     * @param index Field index
     * @param value String to set, <code>null</code> converts to empty String.
     */
    protected final void setStringValue(int index, String value) {
        fields.set(index, value == null ? "" : value);
    }

    /**
     * Replace multiple fields with given String array, starting at the
     * specified index. If parameter <code>first</code> is zero, all sentence
     * fields are replaced.
     * <p>
     * If the length of <code>newFields</code> does not fit in the sentence
     * field count or it contains less values, fields are removed or added
     * accordingly. As the result, total number of fields may increase or
     * decrease. Thus, if the sentence field count must not change, you may need
     * to add empty Strings to <code>newFields</code> in order to preserve the
     * original number of fields. Also, all existing values after
     * <code>first</code> are lost.
     * 
     * @param first Index of first field to set
     * @param newFields Array of Strings to set
     */
    protected final void setStringValues(int first, String[] newFields) {

        List<String> temp = new ArrayList<String>();
        for (int i = 0; i < getFieldCount(); i++) {
            if (i < first) {
                temp.add(fields.get(i));
            } else {
                break;
            }
        }

        for (String field : newFields) {
            temp.add(field == null ? "" : field);
        }

        fields.clear();
        fields = temp;
    }

    // /**
    // * Parses the sentence Id of specified NMEA sentence.
    // *
    // * @param nmea NMEA 0183 sentence String
    // * @return SentenceId Sentence ID of the specified sentence
    // */
    // static SentenceId parseSentenceId(String nmea) {
    // SentenceId id = null;
    // String sid = nmea.substring(3, 6);
    // try {
    // id = SentenceId.valueOf(sid);
    // } catch (Exception ex) {
    // String msg = String.format("Unsupported sentence Id [%s]", sid);
    // throw new IllegalArgumentException(msg, ex);
    // }
    // return id;
    // }
    //
    // /**
    // * Parses the talker Id of specified NMEA sentence.
    // *
    // * @param nmea NMEA 0183 sentence String
    // * @return TalkerId Talker ID of the specified sentence
    // */
    // static TalkerId parseTalkerId(String nmea) {
    // String tid = nmea.substring(1, 3);
    // try {
    // return TalkerId.valueOf(tid);
    // } catch (Exception ex) {
    // String msg = String.format("Unsupported talker Id [%s]", tid);
    // throw new IllegalArgumentException(msg, ex);
    // }
    // }
}