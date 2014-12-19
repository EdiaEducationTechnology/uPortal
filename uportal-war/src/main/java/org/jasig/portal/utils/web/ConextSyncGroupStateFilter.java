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
import java.util.ArrayList;
import java.util.List;

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
import org.jasig.portal.portlet.container.services.IPortletCookieService;
import org.jasig.portal.portlet.om.IPortalCookie;
import org.jasig.portal.rest.ImportExportController;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.IPersonManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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

        HttpSession session = request.getSession(false);

        Object shouldSyncSessionObject = session.getAttribute("should_sync_conext_group_state");

        if (shouldSyncSessionObject != null) {
            boolean shouldSync = (boolean) shouldSyncSessionObject;

            if (shouldSync) {
                session.setAttribute("should_sync_conext_group_state", false);
                String conextAccessToken = (String) request.getSession(false).getAttribute("conext_access_token");

                if (StringUtils.isNotEmpty(conextAccessToken)) {
                    try {
                        handleSurfTeamStateSync(request, response);
                    } catch (JSONException | XMLStreamException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void handleSurfTeamStateSync(HttpServletRequest request, HttpServletResponse response) throws ClientProtocolException, IOException, JSONException, XMLStreamException {
        String conextAccessToken = (String) request.getSession(false).getAttribute("conext_access_token");

        if (StringUtils.isNotEmpty(conextAccessToken)) {
            IPerson person = this.personManager.getPerson(request);
            CloseableHttpClient httpclient = HttpClients.createDefault();

            HttpGet getGroups = new HttpGet("https://api.surfconext.nl/v1/social/rest/groups/@me");

            getGroups.setHeader("Authorization", "Bearer " + conextAccessToken);
            CloseableHttpResponse executeGetGroups = httpclient.execute(getGroups);
            HttpEntity getGroupsResponse = executeGetGroups.getEntity();

            JSONObject jsonOjectFromHttpEntity = getJSONOjectFromHttpEntity(getGroupsResponse);

            if (jsonOjectFromHttpEntity.has("entry")) {
                JSONArray groups = jsonOjectFromHttpEntity.getJSONArray("entry");

                for (int i = 0; i < groups.length(); i++) {
                    JSONObject group = (JSONObject) groups.get(i);
                    String groupId = group.getString("id");
                    String description = group.getString("description");
                    String vootRole = group.getString("voot_membership_role");
                    
                    // if group not found, then
                    String managerGroupId = "managers_" + groupId;
                    String memberGroupId = "members_" + groupId;
                    
                    boolean isMember = StringUtils.equals("member", vootRole);
                    boolean isManager = StringUtils.equals("manager", vootRole) || StringUtils.equals("admin", vootRole);
                    
                    importExportController.createGroup(managerGroupId, new ArrayList(), new ArrayList(), person, isManager);
                    importExportController.createGroup(memberGroupId, new ArrayList(), new ArrayList(), person, isMember);

                    importExportController.createGroup(groupId, Lists.newArrayList(managerGroupId, memberGroupId), new ArrayList(), person, false);
                }

            }
        }
    }

    private JSONObject getJSONOjectFromHttpEntity(HttpEntity entity) throws IOException, JSONException {
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
