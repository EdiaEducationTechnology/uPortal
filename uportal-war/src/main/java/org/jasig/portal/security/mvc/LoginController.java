/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package  org.jasig.portal.security.mvc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.jasig.portal.properties.PropertiesManager;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.IPersonManager;
import org.jasig.portal.url.IPortalUrlBuilder;
import org.jasig.portal.url.IPortalUrlProvider;
import org.jasig.portal.url.UrlType;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Receives the username and password and tries to authenticate the user.
 * The form presented by org.jasig.portal.channels.CLogin is typically used
 * to generate the post to this servlet.
 * @author Bernie Durfee (bdurfee@interactivebusiness.com)
 * @version $Revision$
 * @author Don Fracapane (df7@columbia.edu)
 * Added properties in the security properties file that hold the tokens used to
 * represent the principal and credential for each security context.
 */
@Controller
public class LoginController {
    public static final String REFERER_URL_PARAM = "refUrl";
    
    public static final String AUTH_ATTEMPTED_KEY = "up_authenticationAttempted";
    public static final String AUTH_ERROR_KEY = "up_authenticationError";
    public static final String ATTEMPTED_USERNAME_KEY = "up_attemptedUserName";
    public static final String REQUESTED_PROFILE_KEY = "profile";

    
    protected final Log log = LogFactory.getLog(getClass());
    protected final Log swapperLog = LogFactory.getLog("org.jasig.portal.portlets.swapper");
    
    private IPortalUrlProvider portalUrlProvider;
    private IPersonManager personManager;
    
    private static final String CONEXT_CLIENT_ID = PropertiesManager.getProperty("org.jasig.portal.surfconext.client.id");

    private static final String CONEXT_OAUTH_KEY = PropertiesManager.getProperty("org.jasig.portal.surfconext.oauth.key");

    @Autowired
    public void setPersonManager(IPersonManager personManager) {
        this.personManager = personManager;
    }

    @Autowired
    public void setPortalUrlProvider(IPortalUrlProvider portalUrlProvider) {
        this.portalUrlProvider = portalUrlProvider;
    }


    /**
     * Process the incoming HttpServletRequest
     * @param request
     * @param response
     * @exception ServletException
     * @exception IOException
     * @throws OAuthSystemException 
     */
    @RequestMapping("/Login")
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, OAuthSystemException {
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        // create the redirect URL, adding fname and args parameters if necessary
        String redirectTarget = null;

        final String refUrl = request.getParameter(REFERER_URL_PARAM);
        if (refUrl != null) {
            if (refUrl.startsWith("/")) {
                redirectTarget = refUrl;
            }
            else {
                log.warn("Refernce URL passed in does not start with a / and will be ignored: " + refUrl);
            }
        }

        if (redirectTarget == null) {
            /* Grab the target functional name, if any, off the login request.
             * Also any arguments for the target
             * We will pass them  along after authentication.
             */
            String targetFname = request.getParameter("uP_fname");

            if (targetFname == null) {
                final IPortalUrlBuilder defaultUrl = this.portalUrlProvider.getDefaultUrl(request);
                redirectTarget = defaultUrl.getUrlString();
            }
            else {
                try {
                    final IPortalUrlBuilder urlBuilder = this.portalUrlProvider.getPortalUrlBuilderByPortletFName(request, targetFname, UrlType.RENDER);
                    
                    @SuppressWarnings("unchecked")
                    Enumeration<String> e = request.getParameterNames();
                    while (e.hasMoreElements()) {
                        String paramName = e.nextElement();
                        if (!paramName.equals("uP_fname")) {
                            urlBuilder.addParameter(paramName, request.getParameterValues(paramName));
                        }
                    }
                    
                    redirectTarget = urlBuilder.getUrlString();
                }
                catch (IllegalArgumentException e) {
                    final IPortalUrlBuilder defaultUrl = this.portalUrlProvider.getDefaultUrl(request);
                    redirectTarget = defaultUrl.getUrlString();
                }
            }
        }
        
        IPerson person = null;
        
        final Object authError = request.getSession(false).getAttribute(LoginController.AUTH_ERROR_KEY);
        if (authError == null || !((Boolean)authError)) {
            person = this.personManager.getPerson(request);
        }

        boolean isPostRequest = request.getMethod().equals("POST");
        if (person == null || !person.getSecurityContext().isAuthenticated()) {
			if (isPostRequest)
                request.getSession(false).setAttribute(AUTH_ATTEMPTED_KEY, "true");
            // Preserve the attempted username so it can be redisplayed to the user by CLogin
            String attemptedUserName = request.getParameter("userName");
            if (attemptedUserName != null)
                request.getSession(false).setAttribute(ATTEMPTED_USERNAME_KEY, request.getParameter("userName"));
        }

        String conextAccessToken = (String) request.getSession(false).getAttribute("conext_access_token");
        if (person != null && !person.isGuest() && conextAccessToken == null) {
			//Force the three-legged oauth request
			//  http://uportal.edia.nl/edia-conext-clientwebapp/start.html
			OAuthClientRequest authReq = OAuthClientRequest
			   .authorizationLocation("https://api.surfconext.nl/v1/oauth2/authorize")
			   .setClientId("https://uportal.edia.nl")
			   .setResponseType("code")
			   .setScope("read")
			   .setRedirectURI("http://uportal.edia.nl/uPortal/Token")
			   .buildQueryMessage();
	
			String locationUri = authReq.getLocationUri();
			response.sendRedirect(locationUri);
		} else {
			final String encodedRedirectURL = response.encodeRedirectURL(redirectTarget);
			response.sendRedirect(encodedRedirectURL);
		}

    }

    @RequestMapping("Token")
	public void code(HttpServletRequest req, HttpServletResponse response) throws Exception {
		OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(req);
		String code = oar.getCode();
		
		
		OAuthClientRequest request = OAuthClientRequest
                .tokenLocation("https://api.surfconext.nl/v1/oauth2/token")
                .setGrantType(GrantType.AUTHORIZATION_CODE)
                .setClientId(CONEXT_CLIENT_ID)
                .setClientSecret(CONEXT_OAUTH_KEY)
                .setRedirectURI("http://uportal.edia.nl/uPortal/Token")
                .setCode(code)
                .buildQueryMessage();
		
		String locationUri = request.getLocationUri();
		

		HttpGet httpget = new HttpGet(
				locationUri);
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
		CloseableHttpResponse execute = httpclient.execute(httpget);
		
		HttpEntity entity = execute.getEntity();
		
		JSONObject obj = getJSONOjectFromHttpEntity(entity);
		
		String access_token = obj.getString("access_token");
		if (StringUtils.isNotEmpty(access_token)) {
			HttpSession session = req.getSession();
			session.setAttribute("conext_access_token", access_token);
			
			
			HttpGet getGroups = new HttpGet("https://api.surfconext.nl/v1/social/rest/groups/@me");
			getGroups.setHeader("Authorization", "Bearer " + access_token);
			
			
			CloseableHttpResponse executeGetGroups = httpclient.execute(getGroups);
			
			HttpEntity getGroupsResponse = executeGetGroups.getEntity();
			JSONObject jsonOjectFromHttpEntity = getJSONOjectFromHttpEntity(getGroupsResponse);
			System.out.println(jsonOjectFromHttpEntity);
		}
		
		//Hard coded for now as the request contains a different host
		response.sendRedirect("http://localhost:8080/uPortal");
	}

	private JSONObject getJSONOjectFromHttpEntity(HttpEntity entity)
			throws IOException, JSONException {
		InputStream content = entity.getContent();
		
		StringBuilder b = new StringBuilder();
		List<String> readLines = IOUtils.readLines(content);
		for (String line : readLines) {
			b.append(line);
		}
		JSONObject obj = new JSONObject(b.toString());
		return obj;
	}

}
