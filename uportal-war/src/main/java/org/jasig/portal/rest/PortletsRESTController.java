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

package org.jasig.portal.rest;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.jasig.portal.EntityIdentifier;
import org.jasig.portal.groups.EntityGroupImpl;
import org.jasig.portal.groups.ICompositeGroupService;
import org.jasig.portal.groups.IEntityGroup;
import org.jasig.portal.groups.IGroupMember;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.PortletCategory;
import org.jasig.portal.portlet.registry.IPortletCategoryRegistry;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.security.IAuthorizationPrincipal;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.IPersonManager;
import org.jasig.portal.services.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * This controller logic is derived from {@link org.jasig.portal.layout.dlm.remoting.ChannelListController}
 * 
 * @since 4.1
 * @author Shawn Connolly, sconnolly@unicon.net
 */

@Controller
public class PortletsRESTController {

    private IPortletDefinitionRegistry portletDefinitionRegistry;
    private IPortletCategoryRegistry portletCategoryRegistry;
    private IPersonManager personManager;
    private ICompositeGroupService groupService;
    
    
    @Autowired
    public void setGroupService(ICompositeGroupService groupService) {
		this.groupService = groupService;
	}

	@Autowired
    public void setPortletDefinitionRegistry(IPortletDefinitionRegistry portletDefinitionRegistry) {
        this.portletDefinitionRegistry = portletDefinitionRegistry;
    }

    @Autowired
    public void setPersonManager(IPersonManager personManager) {
        this.personManager = personManager;
    }

    @Autowired
    public void setPortletCategoryRegistry(IPortletCategoryRegistry portletCategoryRegistry) {
        this.portletCategoryRegistry = portletCategoryRegistry;
    }

    @RequestMapping(value="/retrieveLtiPortletLaunchParams/group/{groupid}/user/{username}", method = RequestMethod.GET)
    public ModelAndView retrieveLtiPortletLaunchParams(
    		@PathVariable("groupid") String groupId,
    		@PathVariable("username") String userName,
    		HttpServletRequest request, HttpServletResponse response) throws IOException, XMLStreamException {
    	String groupKey = groupId.replace("_", ".");
    	String role = null;
    	//IEntity
    	
		IEntityGroup group = this.groupService.findGroup(groupKey);
		
		Iterator<IGroupMember> surfSubSubGroups = group.getAllMembers();
		while (surfSubSubGroups.hasNext()) {
			IGroupMember surfSubSubGroup = surfSubSubGroups.next();
			if (surfSubSubGroup.isGroup()) {
				EntityGroupImpl roleGroup = (EntityGroupImpl) surfSubSubGroup; 

				Iterator<IGroupMember> surfSubSubGroupUsers = surfSubSubGroup.getAllMembers();
        		while (surfSubSubGroupUsers.hasNext()) {
        			IGroupMember user = surfSubSubGroupUsers.next();
        			//when user exists in this managers/members group, then depending on the group, the LTI portlet role is set
        			if (user.isEntity() && user.getKey().equals(userName)) {
        				if (roleGroup.getName().split(":")[0].equals("managers_urn")) {	 
        					role = "Instructor";
        				} else if (roleGroup.getName().split(":")[0].equals("members_urn")) {
        					role = "Student";
        				}
        			}
        			
        		}
				
			}
		}
		
		String groupName = group.getName();
    	
    	Map<String,String> rslt = new HashMap();
    	rslt.put("roles", role);
    	rslt.put("resource_link_id", group.getName());
        return new ModelAndView("json", "ltiLauncParameters", rslt);
    }
    
    @RequestMapping(value="/portlets.json", method = RequestMethod.GET)
    public ModelAndView getPortlets(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get a list of all channels
        List<IPortletDefinition> allPortlets = portletDefinitionRegistry.getAllPortletDefinitions();
        IPerson user = personManager.getPerson(request);
        EntityIdentifier ei = user.getEntityIdentifier();
        IAuthorizationPrincipal ap = AuthorizationService.instance().newPrincipal(ei.getKey(), ei.getType());

        List<PortletTuple> rslt = new ArrayList<PortletTuple>();
        for (IPortletDefinition pdef : allPortlets) {
            if (ap.canManage(pdef.getPortletDefinitionId().getStringId())) {
                rslt.add(new PortletTuple(pdef));
            }
        }

        return new ModelAndView("json", "portlets", rslt);

    }

    private Set<String> getPortletCategories(IPortletDefinition pdef) {
        Set<PortletCategory> categories = portletCategoryRegistry.getParentCategories(pdef);
        Set<String> rslt = new HashSet<String>();
        for (PortletCategory category : categories) {
            rslt.add(StringUtils.capitalize(category.getName().toLowerCase()));
        }
        return rslt;
    }

    /*
     * Nested Types
     */

    @SuppressWarnings("unused")
    private /* non-static */ final class PortletTuple implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String id;
        private final String name;
        private final String fname;
        private final String description;
        private final String type;
        private final String lifecycleState;
        private final Set<String> categories;

        public PortletTuple(IPortletDefinition pdef) {
            this.id = pdef.getPortletDefinitionId().getStringId();
            this.name = pdef.getName();
            this.fname = pdef.getFName();
            this.description = pdef.getDescription();
            this.type = pdef.getType().getName();
            this.lifecycleState = pdef.getLifecycleState().toString();
            this.categories = getPortletCategories(pdef);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getFname() {
            return fname;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getLifecycleState() {
            return lifecycleState;
        }

        public Set<String> getCategories() {
            return categories;
        }

    }

}
