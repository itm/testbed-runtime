/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.debuggingguiclient;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class FieldHelper {

    public static class XMLGregorianCalendarDateChooserPanel extends DateChooserPanel {

        public XMLGregorianCalendar getValue() throws DatatypeConfigurationException {
            Date date = getDate();
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(date);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        }

    }

    public static class EnumJComboBox<T extends Enum<T>> extends JComboBox {

        private Class<T> enumClass;

        public EnumJComboBox(Class<T> enumClass) {
            super(enumClass.getEnumConstants());
            this.enumClass = enumClass;
        }

        public T getValue() {
            return (T) getSelectedItem();
        }

    }

    public static class ByteArrayJTextArea extends JTextArea {

        public byte[] getValue(int base) {
            return new BigInteger(getText(), base).toByteArray();
        }

    }

    public static class ByteJTextField extends JTextField {

        public byte getValue(int base) {
            return new BigInteger(getText(), base).toByteArray()[0];
        }

    }

    public static class StringListJTextField extends JTextField {

        public StringListJTextField() {
        }

        public StringListJTextField(String text) {
            super(text);
        }

        public StringListJTextField(int columns) {
            super(columns);
        }

        public StringListJTextField(String text, int columns) {
            super(text, columns);
        }

        public StringListJTextField(Document doc, String text, int columns) {
            super(doc, text, columns);
        }

        public List<String> getValue() {
            String text = getText();
            String[] strings = text.split(",");
            List<String> values = new ArrayList<String>(strings.length);
            for (String string : strings) {
                values.add(string.trim());
            }
            return values;
        }

    }

}
