package main.java.au.edu.anu.portal.portlets.basiclti.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

public class LtiPortletControllerClient {
    
    public HashMap<String, String> getRoleAndResourcelink(String groupId, HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        HashMap<String, String> result = new HashMap<String, String>();

        Object groupIdToRoleMapFromSession = session.getAttribute("groupIdToRoleMap");
        Object groupIdToToolPlacementIdFromSession = session.getAttribute("groupIdToToolPlacementId");
        
        if(groupIdToRoleMapFromSession != null && groupIdToToolPlacementIdFromSession != null) {
            @SuppressWarnings("unchecked")
            HashMap<String, String> groupIdToRoleMap = (HashMap<String, String>) groupIdToRoleMapFromSession;
            @SuppressWarnings("unchecked")
            HashMap<String, String> groupIdToToolPlacementId = (HashMap<String, String>) groupIdToToolPlacementIdFromSession;
            
            String role = groupIdToRoleMap.get(groupId);
            String toolPlacement = groupIdToToolPlacementId.get(groupId);
            
            result.put("roles", role);
            result.put("resource_link_id", toolPlacement);
        } else {
            System.err.println("Cannot find session data for LTI launch params. Not in a SURF team?");
        }
        
        return result;
    }

}