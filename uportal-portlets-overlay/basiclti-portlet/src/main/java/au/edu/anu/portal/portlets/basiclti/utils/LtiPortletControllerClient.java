package main.java.au.edu.anu.portal.portlets.basiclti.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LtiPortletControllerClient {
	// HTTP GET request
		public HashMap sendHttpGet(String url, final String USER_AGENT) throws Exception {

			// String url = "http://www.google.com/search?q=developer";

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
			HashMap<String,Object> letiResults 		= new ObjectMapper().readValue(result.toString(), HashMap.class);
			HashMap ltiData		= (HashMap) letiResults.get("ltiLauncParameters");
			return ltiData;
		}
		
		public static void main(String[] args) {
			LtiPortletControllerClient client = new LtiPortletControllerClient();
			try {
				HashMap map = client.sendHttpGet("http://uportal.edia.nl/uPortal/api/retrieveLtiPortletLaunchParams/group/local_62/user/owner1", "");
				System.out.println(map.get("roles"));
			} catch (Exception e) {
				System.out.println(e.getStackTrace());
			}
		}
}
