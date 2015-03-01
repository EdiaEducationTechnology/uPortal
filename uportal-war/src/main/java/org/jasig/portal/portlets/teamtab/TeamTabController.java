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
import org.jasig.portal.layout.dlm.ConfigurationLoader;
import org.jasig.portal.layout.dlm.FragmentDefinition;
import org.jasig.portal.persondir.ILocalAccountDao;
import org.jasig.portal.persondir.ILocalAccountPerson;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.portlets.groupselector.EntityEnum;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.IPersonManager;
import org.jasig.portal.services.GroupService;
import org.jasig.portal.url.IPortalRequestUtils;
import org.jasig.services.persondir.IPersonAttributeDao;
import org.jasig.services.persondir.IPersonAttributes;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class TeamTabController {
    protected final Log log = LogFactory.getLog(getClass());
    protected IPersonManager personManager;
    protected IPortalRequestUtils portalRequestUtils;
    protected IPortalDataHandlerService portalDataHandlerService;
    protected ICompositeGroupService groupService;
    protected IPortletDefinitionRegistry portletDefinitionRegistry;
    protected ConfigurationLoader configurationLoader;
    protected ILocalAccountDao localAccountDao;
    protected IPersonAttributeDao personAttributeDao;
    
    
    @Autowired
    public void setPersonAttributeDao(IPersonAttributeDao personAttributeDao) {
		this.personAttributeDao = personAttributeDao;
	}

	@Autowired
	public void setLocalAccountDao(ILocalAccountDao localAccountDao) {
		this.localAccountDao = localAccountDao;
	}

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
    
    protected Map<String, String> findUsersInTeamTab(String groupKey, boolean includeManagers, boolean includeMembers) {
    	Map<String, String> users = new HashMap<String, String>();
    	
		IEntityGroup group = this.groupService.findGroup(groupKey);
		
		Iterator<IGroupMember> surfSubSubGroups = group.getAllMembers();
		while (surfSubSubGroups.hasNext()) {
			IGroupMember surfSubSubGroup = surfSubSubGroups.next();
			if (surfSubSubGroup.isGroup()) {
				EntityGroupImpl roleGroup = (EntityGroupImpl) surfSubSubGroup; 

				Iterator<IGroupMember> surfSubSubGroupUsers = surfSubSubGroup.getAllMembers();
        		while (surfSubSubGroupUsers.hasNext()) {
        			IGroupMember user = surfSubSubGroupUsers.next();
        			String fullName = "";
        			String loginTime = "";
        			ILocalAccountPerson localAccountPerson = this.localAccountDao.getPerson(user.getKey());
        			if (localAccountPerson!=null) {
	        			if (localAccountPerson.getAttributes().containsKey("fullName")) {
	        				fullName = (String) localAccountPerson.getAttributeValue("fullName");
	        			} else {
	        				fullName = (String) localAccountPerson.getName();
	        			}
	        			loginTime = (String) localAccountPerson.getAttributeValue("loginTime");
        			} else {
        				fullName = (String) user.getKey();
        				loginTime = "N/A";
        			}
        			if (roleGroup.getName().split(":")[0].equals("managers_urn")) {
						//if this is manager role group and managers must be included
						if (includeManagers) {
							users.put(fullName, loginTime);
						}
					} else if (roleGroup.getName().split(":")[0].equals("members_urn")) {
						//if this is member role group and members must be included
						if (includeMembers) {						
							users.put(fullName, loginTime);
						}
					}      			
        		}
				
			}
		}
    	
    	return users;
    }

    protected List<String> findAllManagerGroupsForUser (PortletRequest request, boolean includeWithTeamTab, boolean includeWithNoTeamTab) {
        final HttpServletRequest httpServletRequest = this.portalRequestUtils.getPortletHttpRequest(request);
		final IPerson person = personManager.getPerson(httpServletRequest);
		
		List<String> groupNames = new ArrayList<String>();
		
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
