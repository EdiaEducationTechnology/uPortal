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

package org.jasig.portal.utils.web;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jasig.portal.EntityIdentifier;
import org.jasig.portal.UserPreferencesManager;
import org.jasig.portal.layout.IUserLayoutManager;
import org.jasig.portal.layout.UserLayoutManagerFactory;
import org.jasig.portal.persondir.ILocalAccountDao;
import org.jasig.portal.persondir.ILocalAccountPerson;
import org.jasig.portal.portlet.om.IPortalCookie;
import org.jasig.portal.rest.ImportExportController;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.IPersonManager;
import org.jasig.portal.security.IdentitySwapperManager;
import org.jasig.portal.user.IUserInstance;
import org.jasig.portal.user.IUserInstanceManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.collect.Lists;

/**
 * Servlet filter to trigger {@link IPortalCookie} creation.
 * 
 * @author Nicholas Blair
 * @version $Id$
 */
public class ConextSyncGroupStateFilter extends OncePerRequestFilter {
	
    private IPersonManager personManager;

    @Autowired
    private ImportExportController importExportController;
    
    @Autowired
    private  UserLayoutManagerFactory userLayoutManagerFactory;
    
    @Autowired
    private IUserInstanceManager userInstanceManager;
    
    @Autowired
    protected ILocalAccountDao localAccountDao;
    
    @Autowired
    @Qualifier("identitySwapperManager")
    private IdentitySwapperManager identitySwapper;


    @Autowired
	public void setLocalAccountDao(ILocalAccountDao localAccountDao) {
		this.localAccountDao = localAccountDao;
	}


	@Autowired
    public void setPersonManager(IPersonManager personManager) {
        this.personManager = personManager;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.web.filter.OncePerRequestFilter#doFilterInternal(
     * javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, javax.servlet.FilterChain)
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    	
    	//if(!identitySwapper.isImpersonating(request)) {
    		HttpSession session = request.getSession(false);
    		
    		Object shouldSyncSessionObject = session.getAttribute("should_sync_conext_group_state");
    		
    		if (shouldSyncSessionObject != null) {
    			boolean shouldSync = (boolean) shouldSyncSessionObject;
    			
    			if (shouldSync) {
    				session.setAttribute("should_sync_conext_group_state", false);
    				String conextAccessToken = (String) request.getSession(false).getAttribute("conext_access_token");
    				
    				if (StringUtils.isNotEmpty(conextAccessToken)) {
    					IPerson person = this.personManager.getPerson(request);
 
    					this.updateLocalAccount(person);    					

    					try {
    						handleSurfTeamStateSync(person, request, response);
    					} catch (JSONException | XMLStreamException e) {
    						e.printStackTrace();
    					} finally {
    						IUserInstance ui = userInstanceManager.getUserInstance(request);
    						UserPreferencesManager upm = (UserPreferencesManager) ui.getPreferencesManager();
    						
    						IUserLayoutManager userLayoutManager = userLayoutManagerFactory.getUserLayoutManager(person, upm.getUserProfile());
    						userLayoutManager.loadUserLayout(true);
    					}
    				}
    			}
    		}
    	//}

        filterChain.doFilter(request, response);
    }
    
    protected boolean isValidString(String value) {
    	if (value == null) {
    		return false;
    	} else if (value.trim().isEmpty()) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    protected boolean hasValidAttribute(IPerson person, String attribute) {
    	if (person.getAttributeMap().containsKey(attribute)) {
    		String value = (String) person.getAttribute(attribute);
    		if (isValidString(value)) {
    			return true;
    		} else {
    			return false;
    		}
    	} else {
    		return false;
    	}	
    }
    protected boolean hasValidAttribute(ILocalAccountPerson person, String key) {
    	if (person.getAttributes().containsKey(key)) {
    		String value = (String) person.getAttributeValue(key);
    		if (isValidString(value)) {
    			return true;
    		} else {
    			return false;
    		}
    	} else {
    		return false;
    	}	
    }
    protected void updateLocalAccount(IPerson person) {			
			String fullName = "";
			String sn = "";
			String givenName = "";
			String loginTime = "";
			String displayName = "";
			
			ILocalAccountPerson localAccount = this.localAccountDao.getPerson(person.getUserName());
			if (localAccount == null) {
				localAccount = this.localAccountDao.createPerson(person.getUserName());
			}
			
			if (isValidString(person.getFullName()) ) {
				fullName = person.getFullName();				
			}
			if (hasValidAttribute(person, "sn") ) {
				sn = (String) person.getAttribute("sn");
			}
			if (hasValidAttribute(person, "givenName") ) {
				givenName = (String) person.getAttribute("givenName");
			}
			if (hasValidAttribute(person, "displayName") ) { 
				displayName = (String) person.getAttribute("displayName");
			}
			
			if (isValidString(sn)) {
				if (!hasValidAttribute(localAccount, "sn")) {					
					localAccount.setAttribute("sn", sn);					
				}
			}
			if (isValidString(givenName)) {
				if (!hasValidAttribute(localAccount, "givenName")) {					
					localAccount.setAttribute("givenName", givenName);					
				}
			}
			if (isValidString(fullName)) {
				if (!hasValidAttribute(localAccount, "fullName")) {					
					localAccount.setAttribute("fullName", fullName);					
				}
			} else {
				if (!hasValidAttribute(localAccount, "fullName")) {	
					if (isValidString(givenName) && isValidString(sn)) {
						localAccount.setAttribute("fullName", givenName + " " + sn);	
					}
				}
			}
			if (isValidString(displayName)) {
				if (!hasValidAttribute(localAccount, "displayName")) {					
					localAccount.setAttribute("displayName", displayName);					
				}
			} else {
				if (!hasValidAttribute(localAccount, "displayName")) {	
					if (isValidString(givenName) && isValidString(sn)) {
						localAccount.setAttribute("displayName", givenName + " " + sn);	
					} else {
						if (isValidString(person.getUserName())) {
							localAccount.setAttribute("displayName", person.getUserName());
						} else if (isValidString(person.getName())) {
							localAccount.setAttribute("displayName", person.getName());
						}
					}
				}
			}
			
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			loginTime = dateFormat.format(date);
			localAccount.setAttribute("loginTime", loginTime);
			try {
				this.localAccountDao.updateAccount(localAccount);
			}catch (NullPointerException e) {
				e.printStackTrace();
			}
    }

    private void handleSurfTeamStateSync(IPerson person, HttpServletRequest request, HttpServletResponse response) throws ClientProtocolException, IOException, JSONException, XMLStreamException {
        String conextAccessToken = (String) request.getSession(false).getAttribute("conext_access_token");

		if (StringUtils.isNotEmpty(conextAccessToken)) {
            Map<String, String> groupIdToRoleMap = new HashMap<String, String>();
            Map<String, String> groupIdToToolPlacementId = new HashMap<String, String>();

            CloseableHttpClient httpclient = HttpClients.createDefault();

            HttpGet getGroups = new HttpGet("https://voot.surfconext.nl/v1/social/rest/groups/@me");

            getGroups.setHeader("Authorization", "Bearer " + conextAccessToken);
            CloseableHttpResponse executeGetGroups = httpclient.execute(getGroups);
            HttpEntity getGroupsResponse = executeGetGroups.getEntity();

            JSONArray jsonOjectFromHttpEntity = getJSONArrayFromHttpEntity(getGroupsResponse);
			boolean isImpersonating = identitySwapper.isImpersonating(request);

			handleVootGroupResponse(person, isImpersonating, groupIdToRoleMap, groupIdToToolPlacementId, jsonOjectFromHttpEntity);


			HttpSession session = request.getSession();
            session.setAttribute("groupIdToRoleMap", groupIdToRoleMap);
            session.setAttribute("groupIdToToolPlacementId", groupIdToToolPlacementId);
        }
    }

	protected void handleVootGroupResponse(IPerson person, boolean isImpersonating, Map<String, String> groupIdToRoleMap, Map<String, String> groupIdToToolPlacementId, JSONArray groups) throws JSONException, IOException, XMLStreamException {
		//remove "entry" level
		if (groups.length() != 0) {
            //JSONArray groups = jsonOjectFromHttpEntity.getJSONArray("entry");

            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = (JSONObject) groups.get(i);
                String groupId = group.getString("id");
                //change reading role
                String vootRole = group.getJSONObject("membership").getString("basic");

                // if group not found, then
                String managerGroupId = "managers_" + groupId;
                String memberGroupId = "members_" + groupId;
                String ownerGroupId = "owners_" + groupId;

                //if current user not impersonating fragment owner
                if(!isImpersonating) {

                    boolean isMember = StringUtils.equals("member", vootRole);
                    boolean isManager = StringUtils.equals("manager", vootRole) || StringUtils.equals("admin", vootRole);

                    importExportController.createOwnerIfNotFound(managerGroupId);
//                    String ownerName = importExportController.generateFragmentOwnerName(managerGroupId);
                    ArrayList userList = new ArrayList();
//                    userList.add(ownerName);

                    importExportController.createGroup(managerGroupId, new ArrayList(), userList, person, isManager);
                    importExportController.createGroup(memberGroupId, new ArrayList(), new ArrayList(), person, isMember);
                    importExportController.createGroup(ownerGroupId, new ArrayList(), new ArrayList(), person, false);

                    importExportController.createGroup(groupId, Lists.newArrayList(managerGroupId, memberGroupId, ownerGroupId), new ArrayList(), person, false);
                    importExportController.createGroup("surfteams", Arrays.asList(groupId), new ArrayList(), person, false);

                    importExportController.updateGroupMembership("Everyone", Arrays.asList("surfteams"), new ArrayList(), person);
                    importExportController.updateGroupMembership("Subscribable Fragments", Arrays.asList(memberGroupId, ownerGroupId), new ArrayList(), person);

                    EntityIdentifier[] rootGroup = importExportController.getGroupIdentifiers(groupId);
                    if(rootGroup.length != 1) {
                        logger.error("Expected one group but found multiple while querying for: " + groupId);
                    } else {
                        EntityIdentifier actualId = rootGroup[0];
                        String actualIdKey = actualId.getKey();
                        groupIdToRoleMap.put(actualIdKey, vootRole);
                        groupIdToToolPlacementId.put(actualIdKey, groupId);
                    }
                } else { //if current user is impersonating fragment owner or any other user
                    //adds fragment owner to the fragment owners group when it gets first time impersonated
                    importExportController.createOwnerIfNotFound(groupId);
                    String ownerUserName = importExportController.generateFragmentOwnerName(groupId);
                    importExportController.updateGroupMembership(ownerGroupId, new ArrayList(), Arrays.asList(ownerUserName), person);
                }
            }

        }
	}

	private JSONArray getJSONArrayFromHttpEntity(HttpEntity entity) throws IOException, JSONException {
        InputStream content = entity.getContent();

        StringBuilder b = new StringBuilder();
        List<String> readLines = IOUtils.readLines(content);
        for (String line : readLines) {
            b.append(line);
        }
        return new JSONArray(b.toString());
    }
}
