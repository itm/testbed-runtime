/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

package eu.wisebed.testbed.api.snaa.authorization;

import eu.wisebed.testbed.api.snaa.helpers.Helper;
import eu.wisebed.testbed.api.snaa.v1.Action;
import eu.wisebed.testbed.api.snaa.v1.SNAAExceptionException;

import java.util.List;
import java.util.Map;

public class AttributeBasedAuthorization implements IUserAuthorization {

    private Map<String, String> attributes;

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean isAuthorized(Action action, UserDetails details) throws SNAAExceptionException {
        try {
            //check on action
            List<Object> actionValues = details.getUserDetails().get(action.getAction());
            if (actionValues == null) return false;

            for (Object actionValue : actionValues){
                if (!Boolean.valueOf((String) actionValue)) return false;
            }

            //check on authorization-attributes
            for (Object key : details.getUserDetails().keySet()) {
                String regex = getRegex(key);
                if (regex != null) {
                    List<Object> cmpValues = details.getUserDetails().get(key);
                    if (!compareValues(regex, cmpValues)) return false;
                }
            }

        }
        catch (Exception e) {
            throw Helper.createSNAAException(e.getMessage());
        }
        return true;
    }

    private String getRegex(Object key) {
        for (Object keyRegex : attributes.keySet()) {
            String keyRegexString = (String) keyRegex;
            if (((String) key).matches(keyRegexString)) {
                return keyRegexString;
            }
        }
        return null;
    }

    private boolean compareValues(String regex, List<Object> cmpValues) {
        for (Object value : cmpValues) {
            if (!compareValue(regex, value)) return false;
        }
        return true;
    }

    private boolean compareValue(String regex, Object value) {
        return (((String) value).matches(attributes.get(regex)));
    }
}
