

package org.jasig.portal.portlets.teamtab.administration;

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
import org.jasig.portal.portlets.teamtab.TeamTabController;
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
public class TeamTabPortletController extends TeamTabController {
    protected final Log log = LogFactory.getLog(getClass());
    protected IPersonManager personManager;
    protected IPortalRequestUtils portalRequestUtils;
    protected IPortalDataHandlerService portalDataHandlerService;
    protected ICompositeGroupService groupService;
    protected IPortletDefinitionRegistry portletDefinitionRegistry;
    protected ConfigurationLoader configurationLoader;
    

    /**
     * Display the entity import form view.
     * 
     * @param request
     * @return
     */
    @RequestMapping
    public ModelAndView getTeamTabAdminView(PortletRequest request) {
    	
    	Map<String,Object> model = new HashMap<String,Object>();
    	List<String> groupNames = findAllManagerGroupsForUser(request, false, true);
    	Map<String, String> teamTabs = findAllManagerGroupTeamTabsForUser(request);
    	model.put("groupNames", groupNames);
    	model.put("teams", teamTabs);
    	return new ModelAndView("/jsp/TeamTabPortlet/create", model);
    	//return "/jsp/TeamTabPortlet/create";
    }

//    /**
//     * Display the entity creation form view.
//     * 
//     * @param request
//     * @return
//     */
//    @RequestMapping(params="action=create")
//    public ModelAndView getTeamTabAdminCreateView(PortletRequest request) {
//    	Map<String,Object> model = new HashMap<String,Object>();
//    	
//  
//    	final Iterable<IPortalDataType> exportPortalDataTypes = this.portalDataHandlerService.getExportPortalDataTypes();
//
//    	List<String> groupNames = findAllManagerGroupsForUser(request, false, true);
//    	Map<String, String> teamTabs = findAllManagerGroupTeamTabsForUser(request);
//    	model.put("groupNames", groupNames);
//    	model.put("teams", teamTabs);
//    	return new ModelAndView("/jsp/TeamTabPortlet/create", model);
//    }    
}
