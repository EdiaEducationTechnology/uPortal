package main.java.au.edu.anu.portal.portlets.basiclti.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LtiPortletControllerClient {
    // HTTP GET request
        public HashMap getRoleAndResourcelink(String groupId, String userName, String protocol, final String USER_AGENT) throws Exception {

            String url = protocol+ "://uportal.edia.nl/uPortal/api/retrieveLtiPortletLaunchParams/group/" + groupId + "/user/" + userName;


            if(StringUtils.equals("https", protocol)) {
                disableCertificateValidationForDemo();
            }

            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            
            // add request header
            request.addHeader("User-Agent", USER_AGENT);

            HttpResponse response = client.execute(request);

            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : "
                    + response.getStatusLine().getStatusCode());

            BufferedReader rd = new BufferedReader(new InputStreamReader(response
                    .getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            System.out.println(result.toString());
            HashMap<String,Object> letiResults      = new ObjectMapper().readValue(result.toString(), HashMap.class);
            HashMap ltiData     = (HashMap) letiResults.get("ltiLauncParameters");
            return ltiData;
        }
        //TODO: Remove this and set key path, only for demo purpose
        public static void disableCertificateValidationForDemo() {
              // Create a trust manager that does not validate certificate chains
              TrustManager[] trustAllCerts = new TrustManager[] { 
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { 
                      return new X509Certificate[0]; 
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }};

                // Ignore differences between given hostname and certificate hostname
                HostnameVerifier hv = new HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
                };

                // Install the all-trusting trust manager
                try {
                  SSLContext sc = SSLContext.getInstance("SSL");
                  sc.init(null, trustAllCerts, new SecureRandom());
                  HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                  HttpsURLConnection.setDefaultHostnameVerifier(hv);
              } catch (Exception e) {}
        }
}