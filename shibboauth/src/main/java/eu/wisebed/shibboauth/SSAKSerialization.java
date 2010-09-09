package eu.wisebed.shibboauth;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: nrohwedder
 * Date: 07.09.2010
 * Time: 20:24:14
 * To change this template use File | Settings | File Templates.
 */
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
        return out;
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
}
