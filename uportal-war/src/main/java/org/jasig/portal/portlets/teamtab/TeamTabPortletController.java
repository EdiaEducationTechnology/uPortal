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

package org.jasig.portal.portlets.teamtab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.EntityIdentifier;
import org.jasig.portal.groups.EntityGroupImpl;
import org.jasig.portal.groups.ICompositeGroupService;
import org.jasig.portal.groups.IEntityGroup;
import org.jasig.portal.groups.IGroupMember;
import org.jasig.portal.io.xml.IPortalDataHandlerService;
import org.jasig.portal.io.xml.IPortalDataType;
import org.jasig.portal.layout.dlm.ConfigurationLoader;
import org.jasig.portal.layout.dlm.FragmentDefinition;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.portlets.groupselector.EntityEnum;
import org.jasig.portal.security.IAuthorizationPrincipal;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.IPersonManager;
import org.jasig.portal.services.AuthorizationService;
import org.jasig.portal.services.GroupService;
import org.jasig.portal.url.IPortalRequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.ModelAndView;

/**
 * TeamTabPortletController controls the display of Team Tab creation functionality
 * 
 * @author Gatis Druva
 * @version $Revision$
 */
@Controller
@RequestMapping("VIEW")
public class TeamTabPortletController {
	
	private static final String OWNER = "UP_SYSTEM";
	private static final String EXPORT_PERMISSION = "EXPORT_ENTITY";
	private static final String DELETE_PERMISSION = "DELETE_ENTITY";

    protected final Log log = LogFactory.getLog(getClass());

    private IPersonManager personManager;
    private IPortalRequestUtils portalRequestUtils;
    private IPortalDataHandlerService portalDataHandlerService;
    private ICompositeGroupService groupService;
    private IPortletDefinitionRegistry portletDefinitionRegistry;
    private ConfigurationLoader configurationLoader;
    
	@Autowired
	public void setConfigurationLoader(ConfigurationLoader configurationLoader) {
		this.configurationLoader = configurationLoader;
	}
    
    public IPortletDefinitionRegistry getPortletDefinitionRegistry() {
		return portletDefinitionRegistry;
	}
    
    @Autowired
    public void setPortletDefinitionRegistry(
			IPortletDefinitionRegistry portletDefinitionRegistry) {
		this.portletDefinitionRegistry = portletDefinitionRegistry;
	}
	
	public ICompositeGroupService getGroupService() {
		return groupService;
	}
    @Autowired
	public void setGroupService(ICompositeGroupService groupService) {
		this.groupService = groupService;
	}

	@Autowired
    public void setPersonManager(IPersonManager personManager) {
    	this.personManager = personManager;
    }
    
    @Autowired
    public void setPortalRequestUtils(IPortalRequestUtils portalRequestUtils) {
        this.portalRequestUtils = portalRequestUtils;
    }

    @Autowired
	public void setPortalDataHandlerService(IPortalDataHandlerService portalDataHandlerService) {
        this.portalDataHandlerService = portalDataHandlerService;
    }

    /**
     * Display the entity import form view.
     * 
     * @param request
     * @return
     */
    @RequestMapping
    public ModelAndView getImportView(PortletRequest request) {
    	
    	Map<String,Object> model = new HashMap<String,Object>();
    	List<String> groupNames = findAllManagerGroupsForUser(request, false, true);
    	Map<String, String> teamTabs = findAllManagerGroupTeamTabsForUser(request);
    	model.put("groupNames", groupNames);
    	model.put("teams", teamTabs);
    	return new ModelAndView("/jsp/TeamTabPortlet/create", model);
    	//return "/jsp/TeamTabPortlet/create";
    }

    /**
     * Display the entity creation form view.
     * 
     * @param request
     * @return
     */
    @RequestMapping(params="action=create")
    public ModelAndView getCreateView(PortletRequest request) {
    	Map<String,Object> model = new HashMap<String,Object>();
    	
  
    	final Iterable<IPortalDataType> exportPortalDataTypes = this.portalDataHandlerService.getExportPortalDataTypes();

    	List<String> groupNames = findAllManagerGroupsForUser(request, false, true);
    	Map<String, String> teamTabs = findAllManagerGroupTeamTabsForUser(request);
    	model.put("groupNames", groupNames);
    	model.put("teams", teamTabs);
    	return new ModelAndView("/jsp/TeamTabPortlet/create", model);
    }    
    
    
//    /**
//     * Return a list of all permitted import/export types for the given permission
//     * and the current user.
//     * 
//     * @param request
//     * @param activityName
//     * @return
//     */
//    protected List<IPortalDataType> getAllowedTypes(PortletRequest request, String activityName, Iterable<IPortalDataType> dataTypes) {
//
//    	// get the authorization principal representing the current user
//        final HttpServletRequest httpServletRequest = this.portalRequestUtils.getPortletHttpRequest(request);
//		final IPerson person = personManager.getPerson(httpServletRequest);
//		final EntityIdentifier ei = person.getEntityIdentifier();
//	    final IAuthorizationPrincipal ap = AuthorizationService.instance().newPrincipal(ei.getKey(), ei.getType());
//
//	    // filter the list of configured import/export types by user permission
//    	final List<IPortalDataType> results = new ArrayList<IPortalDataType>();
//    	for (IPortalDataType type : dataTypes) {
//    		final String typeId = type.getTypeId();
//    	    if (ap.hasPermission(OWNER, activityName, typeId)) {
//    	    	results.add(type);
//    	    }    		
//    	}
//
//    	return results;
//    }
    protected Map<String, String> findAllManagerGroupTeamTabsForUser (PortletRequest request) {
    	final HttpServletRequest httpServletRequest = this.portalRequestUtils.getPortletHttpRequest(request);
    	Map<String, String> fragments = new HashMap<String, String>();
    	List<String> groupNames = this.findAllManagerGroupsForUser(request, true, false);
    	
    	for (String groupName : groupNames) {

	    	FragmentDefinition fragment = this.configurationLoader.getFragmentByName(generateFragmentNameForTeam(groupName));
	    	fragments.put(fragment.getOwnerId() ,fragment.getName());
    	}
    	
    	return fragments;
    }
    protected String generateFragmentNameForTeam (String teamName) {
    	String[] splittedGroup = teamName.split(":");
    	final String fragmentName = "team_tab_"+splittedGroup[splittedGroup.length-1];
    	return fragmentName;
    }
    protected List<String> findAllManagerGroupsForUser (PortletRequest request, boolean includeWithTeamTab, boolean includeWithNoTeamTab) {
        final HttpServletRequest httpServletRequest = this.portalRequestUtils.getPortletHttpRequest(request);
		final IPerson person = personManager.getPerson(httpServletRequest);
		
		ArrayList<String> groupNames = new ArrayList<String>();
		
    	EntityIdentifier[] surfteams 	= this.groupService.searchForGroups("surfteams", GroupService.IS, EntityEnum.GROUP.getClazz());    	
    	
    	if (surfteams.length > 0)  {
    		IGroupMember surfTeam = GroupService.getGroupMember(surfteams[0]);
    		Iterator<IGroupMember> surfSubGroups = surfTeam.getAllMembers();
    		while (surfSubGroups.hasNext()) {
    			IGroupMember surfSubGroup = surfSubGroups.next();
    			
    			if (surfSubGroup.isGroup()) {
    				
	        		Iterator<IGroupMember> surfSubSubGroups = surfSubGroup.getAllMembers();
	        		while (surfSubSubGroups.hasNext()) {
	        			IGroupMember surfSubSubGroup = surfSubSubGroups.next();
	        			if (surfSubSubGroup.isGroup()) {
    						EntityGroupImpl possiblyManagerGroup = (EntityGroupImpl) surfSubSubGroup; 
	        				if (possiblyManagerGroup.getName().split(":")[0].equals("managers_urn")) {	 
	        					if (person.getUserName().equals("admin")) {
	        						EntityGroupImpl surfSubGroupImpl = (EntityGroupImpl) surfSubGroup; 
	        						String fragmentName = generateFragmentNameForTeam(surfSubGroupImpl.getName());
        	        				FragmentDefinition fragmentForTeam = this.configurationLoader.getFragmentByName(fragmentName);
        	        				if (includeWithTeamTab && includeWithNoTeamTab) {
        	        					//include group without checking team tab
        	        					groupNames.add(surfSubGroupImpl.getName());
        	        				} else {  
        	        					if ((fragmentForTeam != null) && includeWithTeamTab) {
        	        						//if team tab exists for group
        	        						groupNames.add(surfSubGroupImpl.getName());
        	        					} else if ((fragmentForTeam == null) && includeWithNoTeamTab){
        	        						//if team tab does not exist for group 
        	        						groupNames.add(surfSubGroupImpl.getName());
        	        					}
        	        				}	
	        					} else {
		        	        		Iterator<IGroupMember> surfSubSubGroupUsers = surfSubSubGroup.getAllMembers();
		        	        		while (surfSubSubGroupUsers.hasNext()) {
		        	        			IGroupMember user = surfSubSubGroupUsers.next();
		        	        			if (user.isEntity() && user.getKey().equals(person.getUserName())) {
		        	        				EntityGroupImpl surfSubGroupImpl = (EntityGroupImpl) surfSubGroup; 
			        						String fragmentName = generateFragmentNameForTeam(surfSubGroupImpl.getName());
		        	        				FragmentDefinition fragmentForTeam = this.configurationLoader.getFragmentByName(fragmentName);		        	 
		        	        				if (includeWithTeamTab && includeWithNoTeamTab) {
		        	        					//include group without checking team tab
		        	        					groupNames.add(surfSubGroupImpl.getName());
		        	        				} else {  
		        	        					if ((fragmentForTeam != null) && includeWithTeamTab) {
		        	        						//if team tab exists for group
		        	        						groupNames.add(surfSubGroupImpl.getName());
		        	        					} else if ((fragmentForTeam == null) && includeWithNoTeamTab){
		        	        						//if team tab does not exist for group 
		        	        						groupNames.add(surfSubGroupImpl.getName());
		        	        					}
		        	        					
		        	        				}
		        	        			}
		        	        			
		        	        		}	
	        					}
	        				}
	        			}
	        		}
    			}
    			
    		}
    	}
    	return groupNames;
    }
    

}
