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

package eu.wisebed.shibboauth;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import java.io.*;

public class SSAKSerialization {
    static private BASE64Encoder encode = new BASE64Encoder();
    static private BASE64Decoder decode = new BASE64Decoder();

    static public String serialize(ShibbolethSecretAuthenticationKey shibbolethSecretAuthenticationKey) {
        String out = null;
        if (shibbolethSecretAuthenticationKey != null){
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeObject(shibbolethSecretAuthenticationKey);
                out = encode.encode(baos.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return out.replaceAll("\\n","");
    }

    static public ShibbolethSecretAuthenticationKey deserialize(String s) {
        ShibbolethSecretAuthenticationKey ssak = null;
        if (s != null) {
            try {
                ByteArrayInputStream bios = new ByteArrayInputStream(decode.decodeBuffer(s));
                ObjectInputStream ois = new ObjectInputStream(bios);
                ssak = (ShibbolethSecretAuthenticationKey) ois.readObject();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        return ssak;
    }

    public static void main(String[] args){
        ShibbolethSecretAuthenticationKey key = SSAKSerialization.deserialize("rO0ABXNyADdldS53aXNlYmVkLnNoaWJib2F1dGguU2hpYmJvbGV0aFNlY3JldEF1dGhlbnRpY2F0\n" +
                "aW9uS2V5JqA7D7lBAz0CAAJMAApjb29raWVNYXBzdAAQTGphdmEvdXRpbC9MaXN0O0wAF3NlY3Jl\n" +
                "dEF1dGhlbnRpY2F0aW9uS2V5dAASTGphdmEvbGFuZy9TdHJpbmc7eHBzcgAUamF2YS51dGlsLkxp\n" +
                "bmtlZExpc3QMKVNdSmCIIgMAAHhwdwQAAAAHc3IAF2phdmEudXRpbC5MaW5rZWRIYXNoTWFwNMBO\n" +
                "XBBswPsCAAFaAAthY2Nlc3NPcmRlcnhyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAK\n" +
                "bG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAHdAAEbmFtZXQAE19zaGli\n" +
                "c3RhdGVfMjM2YmI5NzN0AAV2YWx1ZXQAP2h0dHBzJTNBJTJGJTJGZ3JpZGxhYjIzLnVuaWJlLmNo\n" +
                "JTJGcG9ydGFsJTJGU05BJTJGc2VjcmV0VXNlcktleXQAB3ZlcnNpb25zcgARamF2YS5sYW5nLklu\n" +
                "dGVnZXIS4qCk94GHOAIAAUkABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhw\n" +
                "AAAAAHQABmRvbWFpbnQAEmdyaWRsYWIyMy51bmliZS5jaHQABHBhdGh0AAEvdAAKZXhwaXJ5RGF0\n" +
                "ZXB0AAhpc1NlY3VyZXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAB4\n" +
                "AHNxAH4ABj9AAAAAAAAMdwgAAAAQAAAAB3EAfgAJdAAQd2lzZWJlZF9zYW1sX2lkcHEAfgALdABG\n" +
                "YUhSMGNITTZMeTkzYVhObFltVmtNUzVwZEcwdWRXNXBMV3gxWldKbFkyc3VaR1V2YVdSd0wzTm9h\n" +
                "V0ppYjJ4bGRHZyUzRHEAfgANcQB+ABBxAH4AEXQACHVuaWJlLmNocQB+ABN0AAEvcQB+ABVzcgAO\n" +
                "amF2YS51dGlsLkRhdGVoaoEBS1l0GQMAAHhwdwgAAAFATzQQ+HhxAH4AFnEAfgAYeABzcQB+AAY/\n" +
                "QAAAAAAADHcIAAAAEAAAAAdxAH4ACXQACkpTRVNTSU9OSURxAH4AC3QAIENDNEIzNDkwNTNGMzZD\n" +
                "OEU3RkVGQzAyNUExQ0IzNjZFcQB+AA1xAH4AEHEAfgARdAAbd2lzZWJlZDEuaXRtLnVuaS1sdWVi\n" +
                "ZWNrLmRlcQB+ABN0AAQvaWRwcQB+ABVwcQB+ABZzcQB+ABcBeABzcQB+AAY/QAAAAAAADHcIAAAA\n" +
                "EAAAAAdxAH4ACXQACkpTRVNTSU9OSURxAH4AC3QAIEEzNzVFOEZBNDU1OUM3RDlDOENBMkJERUMw\n" +
                "RUZGN0NGcQB+AA1xAH4AEHEAfgARdAAbd2lzZWJlZDEuaXRtLnVuaS1sdWViZWNrLmRlcQB+ABN0\n" +
                "AAQvY2FzcQB+ABVwcQB+ABZxAH4AJXgAc3EAfgAGP0AAAAAAAAx3CAAAABAAAAAHcQB+AAl0AAZD\n" +
                "QVNUR0NxAH4AC3QAPlRHVC00OTYtb2Q0RHg2U2dRWEFCVHZkcmRQSTlYSTFhWWZDZmFJdE5QakFI\n" +
                "TUhnc0RkVUNXcXQxM0stY2FzcQB+AA1xAH4AEHEAfgARdAAbd2lzZWJlZDEuaXRtLnVuaS1sdWVi\n" +
                "ZWNrLmRlcQB+ABN0AAQvY2FzcQB+ABVwcQB+ABZxAH4AJXgAc3EAfgAGP0AAAAAAAAx3CAAAABAA\n" +
                "AAAHcQB+AAl0AAxfaWRwX3Nlc3Npb25xAH4AC3QAQGY1Njc1Y2RlM2QxNmJlMTg0ZDA3NmI0ZmU3\n" +
                "YjQxYTk3Y2IzZjNhYmFkYzU5NDE4M2FiZjViOTc3OTMzMzkyYmFxAH4ADXEAfgAQcQB+ABF0ABt3\n" +
                "aXNlYmVkMS5pdG0udW5pLWx1ZWJlY2suZGVxAH4AE3QABC9pZHBxAH4AFXBxAH4AFnEAfgAYeABz\n" +
                "cQB+AAY/QAAAAAAADHcIAAAAEAAAAAdxAH4ACXQAZV9zaGlic2Vzc2lvbl82NDY1NjY2MTc1NmM3\n" +
                "NDY4NzQ3NDcwNzMzYTJmMmY2NzcyNjk2NDZjNjE2MjMyMzMyZTc1NmU2OTYyNjUyZTYzNjgyZjcz\n" +
                "Njg2OTYyNjI2ZjZjNjU3NDY4cQB+AAt0ACFfYzk3MzkxZTVmYTE1M2EyNDlkOWRlNGM5OTg4MjUz\n" +
                "YmVxAH4ADXEAfgAQcQB+ABF0ABJncmlkbGFiMjMudW5pYmUuY2hxAH4AE3QAAS9xAH4AFXBxAH4A\n" +
                "FnEAfgAYeAB4dAAXQVRGLTAwNzYxMjAzNjEyMDM2MTAyMzY=");
        System.out.println(key.getSecretAuthenticationKey());
    }
}
