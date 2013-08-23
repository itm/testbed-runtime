/*******************************************************************************
* Copyright (c) 2013 CEA LIST.
* Contributor:
*   ROUX Pierre
*   Kim Thuat NGUYEN
*******************************************************************************/
package eu.smartsantander.cea.utils.httprequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class HttpRequestUtil {

	private static final Logger log = LoggerFactory.getLogger(HttpRequestUtil.class);
    
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
            
        } catch (UnsupportedEncodingException e) {
	        log.error(e.getMessage(),e);
        } catch (MalformedURLException e) {
	        log.error(e.getMessage(),e);
        } catch (IOException e) {
	        log.error(e.getMessage(),e);
        }
        return response.toArray(new String[0]);
    }
}
