/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.smartsantander.cea.utils.httprequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * 
 */
public class HttpRequestUtil {
    
    
    public static String[] sendHttpRequest(String requestUrl, String method, Map<String, String> params, String encoding) {
        List<String> response = new ArrayList<>();
        try {
            StringBuilder requestParams = new StringBuilder();
            if (params!= null && params.size() > 0) {
                Iterator<String> it = params.keySet().iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    String value = params.get(key);
                    requestParams.append(URLEncoder.encode(key, encoding));
                    requestParams.append("=");
                    requestParams.append(URLEncoder.encode(value, encoding));
                    requestParams.append("&");
                }
            }
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            
            // The request will return a response
            connection.setDoInput(true);
            
            if (method.equalsIgnoreCase("POST")) {
                connection.setDoOutput(true);
                OutputStream out = connection.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(out);
                writer.write(requestParams.toString());
                writer.flush();
                writer.close();
                out.flush();
                out.close();
            } else {
                connection.setDoOutput(false);
            }
            try (InputStreamReader ip = new InputStreamReader(connection.getInputStream())) {
                BufferedReader reader = new BufferedReader(ip);
                String line =  null;
                while ((line = reader.readLine())!=null) {
                    response.add(line);
                }
            }
            
        } catch (UnsupportedEncodingException ue) {
            ue.printStackTrace();
        } catch (MalformedURLException me) {
            me.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
        return response.toArray(new String[0]);
    }
}
