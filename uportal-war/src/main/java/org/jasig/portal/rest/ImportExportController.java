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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portal.EntityIdentifier;
import org.jasig.portal.groups.ICompositeGroupService;
import org.jasig.portal.groups.IEntityGroup;
import org.jasig.portal.groups.IGroupMember;
import org.jasig.portal.groups.IGroupService;
import org.jasig.portal.io.xml.IPortalDataHandlerService;
import org.jasig.portal.io.xml.PortalDataKey;
import org.jasig.portal.portlets.groupselector.EntityEnum;
import org.jasig.portal.security.IAuthorizationPrincipal;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.IPersonManager;
import org.jasig.portal.services.AuthorizationService;
import org.jasig.portal.services.GroupService;
import org.jasig.portal.xml.StaxUtils;
import org.jasig.portal.xml.XmlUtilities;
import org.jasig.portal.xml.stream.BufferedXMLEventReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * ImportExportController provides AJAX/REST targets for import/export operations.
 * 
 * @author Jen Bourey, jennifer.bourey@gmail.com
 */
@Controller
public class ImportExportController {

	private static final String OWNER = "UP_SYSTEM";
	private static final String EXPORT_PERMISSION = "EXPORT_ENTITY";
	private static final String DELETE_PERMISSION = "DELETE_ENTITY";

    final Log log = LogFactory.getLog(getClass());
    
    private IPersonManager personManager;
    private IPortalDataHandlerService portalDataHandlerService;
    private ICompositeGroupService groupService;
    private XmlUtilities xmlUtilities;

    @Autowired
    public void setXmlUtilities(XmlUtilities xmlUtilities) {
        this.xmlUtilities = xmlUtilities;
    }

    @Autowired
    public void setPersonManager(IPersonManager personManager) {
    	this.personManager = personManager;
    }
    
    @Autowired
	public void setPortalDataHandlerService(IPortalDataHandlerService portalDataHandlerService) {
        this.portalDataHandlerService = portalDataHandlerService;
    }

    @Autowired
	public void setIGroupService(ICompositeGroupService groupService) {
        this.groupService = groupService;
    }
    
    @RequestMapping(value="/import", method = RequestMethod.POST)
    public void importEntity(@RequestParam("file") MultipartFile entityFile, 
    		HttpServletRequest request, HttpServletResponse response) throws IOException, XMLStreamException {
        
        //Get a StAX reader for the source to determine info about the data to import
        final BufferedXMLEventReader bufferedXmlEventReader = createSourceXmlEventReader(entityFile);
        final PortalDataKey portalDataKey = getPortalDataKey(bufferedXmlEventReader);
        

        
        final IPerson person = personManager.getPerson(request);
		final EntityIdentifier ei = person.getEntityIdentifier();
	    final IAuthorizationPrincipal ap = AuthorizationService.instance().newPrincipal(ei.getKey(), ei.getType());
	    if (!ap.hasPermission("UP_SYSTEM", "IMPORT_ENTITY", portalDataKey.getName().getLocalPart())) {
	    	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	    	return;
	    }

	    portalDataHandlerService.importData(new StAXSource(bufferedXmlEventReader));

        response.setStatus(HttpServletResponse.SC_OK);
    }

    @RequestMapping(value="/create/group", method = RequestMethod.POST)
    public void createGroupAndAddMembers(
    		@RequestParam("groupid") String groupid,
    		@RequestParam("members") String memberGroups,
    		HttpServletRequest request, 
    		HttpServletResponse response) throws IOException, XMLStreamException {
        
    	
    	response.setStatus(HttpServletResponse.SC_OK);
    }
    
    @RequestMapping(value="/create/fragment/group/{groupid}", method = RequestMethod.GET)
    public void createFragmentWithLayout(@PathVariable("groupid") String groupId, 
    		HttpServletRequest request, HttpServletResponse response) throws IOException, XMLStreamException {
    	
    	//creates the fragment definition by generating xml and importing it
    	this.createFragmentDefinition(groupId, request);
     	
    	//Step 5: import default layout for fragment
    	this.createFragmentLayout(groupId, request);
    	
    	response.setStatus(HttpServletResponse.SC_OK);
    }
    
    
    protected BufferedXMLEventReader createSourceXmlEventReader(MultipartFile multipartFile) throws IOException {
        final InputStream inputStream = multipartFile.getInputStream();
        final String name = multipartFile.getOriginalFilename();
        
        final XMLInputFactory xmlInputFactory = this.xmlUtilities.getXmlInputFactory();
        final XMLEventReader xmlEventReader;
        try {
            xmlEventReader = xmlInputFactory.createXMLEventReader(name, inputStream);
        }
        catch (XMLStreamException e) {
            throw new RuntimeException("Failed to create XML Event Reader for data Source", e);
        }
        return new BufferedXMLEventReader(xmlEventReader, -1);
    }

    protected PortalDataKey getPortalDataKey(final BufferedXMLEventReader bufferedXmlEventReader) {
        final StartElement rootElement = StaxUtils.getRootElement(bufferedXmlEventReader);
        final PortalDataKey portalDataKey = new PortalDataKey(rootElement);
        bufferedXmlEventReader.reset();
        return portalDataKey;
    }
    public void updateGroupMembership (String groupId, List<String> subgroupNames, List<String> userNames, IPerson person) {    	
    	EntityIdentifier[] results = this.groupService.searchForGroups(groupId,GroupService.IS,EntityEnum.GROUP.getClazz());
    	List<IGroupMember> existingSubgroups = new ArrayList();
    	IEntityGroup parentGroup = null;
    	if (results.length > 0)  {
    		IGroupMember parentGroupMemb = GroupService.getGroupMember(results[0]);
    		if (parentGroupMemb.isGroup()) {
    			parentGroup = (IEntityGroup) parentGroupMemb;
    			
            	for (String subgroupName : subgroupNames) {           		
            		EntityIdentifier[] subGroups = this.groupService.searchForGroups(subgroupName,GroupService.IS,EntityEnum.GROUP.getClazz());
            		IGroupMember member = GroupService.getGroupMember(subGroups[0]);
            		if (member.isGroup()) {
            			//existingSubgroups.add(member);
            			parentGroup.addMember(member);
            		}
            	}
            	for (String userName : userNames) {           		
            		EntityIdentifier[] subGroups = this.groupService.searchForEntities(userName,GroupService.IS,EntityEnum.PERSON.getClazz());
            		IGroupMember member = GroupService.getGroupMember(subGroups[0]);
            		Iterator parents = member.getAllContainingGroups();
//            		for (IGroupMember parent : parents) {
//            			
//            		}
            		parentGroup.addMember(member);
            	}
            	parentGroup.update();
    		}


    	} else {
    		Exception e = new Exception ("Group not found: "+groupId);
    		e.printStackTrace();
    	}
    	return;
    }
    public void createGroup (String groupId, List<String> subgroupNames, List<String> usernames, IPerson person, boolean shouldJoinGroup) throws IOException, XMLStreamException {
    	if (shouldJoinGroup) {
    		usernames.add(person.getUserName());
    	}
    	EntityIdentifier[] groups = this.groupService.searchForGroups(groupId,GroupService.IS,EntityEnum.GROUP.getClazz());
    	if (groups.length > 0)  {
    		this.updateGroupMembership(groupId, subgroupNames, usernames, person);    		
    		System.out.println("Warning. Group already is in database ");
    		return;
    	}
    	
//    	String userName = person.getUserName();
    	
    	String subgroupsXml = "";
    	for (String subgroup : subgroupNames ) {
    		subgroupsXml+="<group>" + subgroup + "</group>";
    	}
    	
    	String usersXml = "";
    	for (String username : usernames ) {
    		usersXml+="<literal>" + username + "</literal>";
    	}
    	
    	String groupDef = 
    			"<?xml version=\"1.0\" encoding=\"UTF-8\"?><group script=\"classpath://org/jasig/portal/io/import-group_membership_v3-2.crn\">"
    			+"  <name>" + groupId + "</name>"
    			+"  <entity-type>org.jasig.portal.security.IPerson</entity-type>"
    			+"  <creator>admin</creator>"
    			+"  <description>for anybody</description>"
    			+"  <children>"
    			+	subgroupsXml
    			+ 	usersXml
    			+"  </children>"
    			+"</group>";
    	
    	final XMLInputFactory xmlInputFactory = this.xmlUtilities.getXmlInputFactory();
    	InputStream inputStream = new ByteArrayInputStream(groupDef.getBytes("UTF-8"));
    	XMLEventReader xmlEventReader = null;   	
        try {
            xmlEventReader = xmlInputFactory.createXMLEventReader("groupdefinition.group-membership.xml", inputStream);
        }
        catch (XMLStreamException e) {
            throw new RuntimeException("Failed to create XML Event Reader for data Source", e);
        }    	    	 
    	BufferedXMLEventReader buffXmlEvnetReader = new BufferedXMLEventReader(xmlEventReader, -1);
    	final BufferedXMLEventReader bufferedXmlEventReader = buffXmlEvnetReader;  	

    	try {
	    	StAXSource fragmentSource = new StAXSource(bufferedXmlEventReader);
	    	portalDataHandlerService.importData(fragmentSource);
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new IOException(e.getMessage(), e);
    	}
    }
    
    protected void createFragmentDefinition (String groupId, HttpServletRequest request) throws IOException, XMLStreamException {
       	
//    	//Step 0: check if the group doesn't have a team tab for 
    	final IPerson person = personManager.getPerson(request);
    	String userName = person.getUserName();
    	
//    	//Step 1: get group, validate access to group
//    	
//    	//Step 2: generate XML that would normally be uploaded by a file with an XML library
    	String[] splittedGroup = groupId.split(":");
    	final String fragmentName = "team_tab_"+splittedGroup[splittedGroup.length-1]; 
    	String fragmentDef = 
    			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    	    	+"<fragment-definition xmlns:dlm=\"http://org.jasig.portal.layout.dlm.config\" script=\"classpath://org/jasig/portal/io/import-fragment-definition_v3-1.crn\">"
    	    	+    "<dlm:fragment name=\"" + fragmentName + "\" ownerID=\"" + userName + "\" precedence=\"10\">"
    	    	+        "<dlm:audience evaluatorFactory=\"org.jasig.portal.layout.dlm.providers.GroupMembershipEvaluatorFactory\">"
    	    	+            "<paren mode=\"OR\">"
    	    	+                "<attribute mode=\"deepMemberOf\" name=\"" + groupId+ "\"/>"
    	    	+            "</paren>"
    	    	+        "</dlm:audience>"
    	    	+    "</dlm:fragment>"
    	    	+"</fragment-definition>";
    	
//    	//Step 3: feed this to the BufferedXMLEventReader and continue like normal
//    	//Get a StAX reader for the source to determine info about the data to import
    	
    	final XMLInputFactory xmlInputFactory = this.xmlUtilities.getXmlInputFactory();
    	InputStream inputStream = new ByteArrayInputStream(fragmentDef.getBytes("UTF-8"));
    	XMLEventReader xmlEventReader = null;   	
        try {
            xmlEventReader = xmlInputFactory.createXMLEventReader("Fr2.fragment-definition.xml", inputStream);
        }
        catch (XMLStreamException e) {
            throw new RuntimeException("Failed to create XML Event Reader for data Source", e);
        }    	    	 
    	BufferedXMLEventReader buffXmlEvnetReader = new BufferedXMLEventReader(xmlEventReader, -1);
    	final BufferedXMLEventReader bufferedXmlEventReader = buffXmlEvnetReader; 
    	final PortalDataKey portalDataKey = getPortalDataKey(bufferedXmlEventReader);   	


    	//final EntityIdentifier ei = person.getEntityIdentifier();
    	//final IAuthorizationPrincipal ap = AuthorizationService.instance().newPrincipal(ei.getKey(), ei.getType());
    	
    	//Step 4: for now remove this permission check, we can add one back in when we know the criteria
//    	if (!ap.hasPermission("UP_SYSTEM", "IMPORT_ENTITY", portalDataKey.getName().getLocalPart())) {
//    		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//    		return;
//    	}
    	try {
	    	StAXSource fragmentSource = new StAXSource(bufferedXmlEventReader);
	    	portalDataHandlerService.importData(fragmentSource);
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new IOException(e.getMessage(), e);
    	}
    }
    
    protected void createFragmentLayout (String groupId, HttpServletRequest request) throws IOException, XMLStreamException {
    	EntityIdentifier[] groups = this.groupService.searchForGroups(groupId,GroupService.IS,EntityEnum.GROUP.getClazz());
    	if (groups.length > 0)  {

	    	final IPerson person = personManager.getPerson(request);
	    	String[] splittedGroup = groupId.split(":");
	    	final String tabName = splittedGroup[splittedGroup.length-1]; 
	    	String groupKey = groups[0].getKey();
	    	
	    	String layoutXml = 
	    			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
	    			+"<layout xmlns:dlm=\"http://www.uportal.org/layout/dlm\" script=\"classpath://org/jasig/portal/io/import-layout_v3-2.crn\""
	    			+"    username=\"" + person.getUserName() + "\" >"
	    			+"    <folder ID=\"s1\" hidden=\"false\" immutable=\"false\" name=\"Root folder\" type=\"root\" unremovable=\"true\">"
	    			+"        <!--"
	    			+"         | Hidden folders do not propagate to regular users, and fragment owner"
	    			+"         | accounts don't receive (other) fragments at all;  Fragment owners must"
	    			+"         | have their own copies of the minimal portlets required to view and manage"
	    			+"         | their own layouts."
	    			+"         +-->"
	    			+"        <folder ID=\"s2\" hidden=\"true\" immutable=\"true\" name=\"Page Top folder\" type=\"page-top\" unremovable=\"true\">"
	    			+"            <channel fname=\"dynamic-respondr-skin\" unremovable=\"false\" hidden=\"false\" immutable=\"false\" ID=\"n3\"/>"
	    			+"            <channel fname=\"fragment-admin-exit\" unremovable=\"false\" hidden=\"false\" immutable=\"false\" ID=\"n4\"/>"
	    			+"        </folder>"
	    			+"        <folder ID=\"s5\" hidden=\"true\" immutable=\"true\" name=\"Customize folder\" type=\"customize\" unremovable=\"true\">"
	    			+"            <channel fname=\"personalization-gallery\" unremovable=\"false\" hidden=\"false\" immutable=\"false\" ID=\"n6\"/>"
	    			+"        </folder>"
	    			+"        <folder ID=\"s7\" dlm:deleteAllowed=\"false\" dlm:editAllowed=\"false\" dlm:moveAllowed=\"false\" hidden=\"false\" immutable=\"false\" name=\"" +tabName+ " Tab\" type=\"regular\" unremovable=\"false\">"
	    			+"            <structure-attribute>"
	    			+"                <name>externalId</name>"
	    			+"                <value>" + groupKey + "</value>"
	    			+"            </structure-attribute>"
	    			+"            <folder ID=\"s8\" hidden=\"false\" immutable=\"false\" name=\"Column\" type=\"regular\" unremovable=\"false\">"
	    			+"                <structure-attribute>"
	    			+"                    <name>width</name>"
	    			+"                    <value>60%</value>"
	    			+"                </structure-attribute>"
	//    			+"                <channel fname=\"email-preview-demo\" unremovable=\"false\" hidden=\"false\" immutable=\"false\" ID=\"n9\" dlm:moveAllowed=\"false\" dlm:deleteAllowed=\"false\"/>"
	//    			+"                <channel fname=\"weather\" unremovable=\"false\" hidden=\"false\" immutable=\"false\" ID=\"n10\"/>"
	//    			+"                <channel fname=\"pbookmarks\" unremovable=\"false\" hidden=\"false\" immutable=\"false\" ID=\"n11\" dlm:moveAllowed=\"false\" dlm:deleteAllowed=\"false\"/>"
	    			+"            </folder>"
	    			+"            <folder ID=\"s12\" hidden=\"false\" immutable=\"false\" name=\"Column\" type=\"regular\" unremovable=\"false\">"
	    			+"                <structure-attribute>"
	    			+"                    <name>width</name>"
	    			+"                    <value>40%</value>"
	    			+"                </structure-attribute>"
	//    			+"                <channel fname=\"calendar\" unremovable=\"false\" hidden=\"false\" immutable=\"false\" ID=\"n13\"/>"
	    			+"            </folder>"
	    			+"        </folder>"
	    			+"    </folder>"
	    			+"</layout>";
	    	final XMLInputFactory xmlInputFactory = this.xmlUtilities.getXmlInputFactory();
	    	InputStream inputStream = new ByteArrayInputStream(layoutXml.getBytes("UTF-8"));
	    	XMLEventReader xmlEventReader = null;   	
	        try {
	            xmlEventReader = xmlInputFactory.createXMLEventReader("Fr2.fragment-layout.xml", inputStream);
	        }
	        catch (XMLStreamException e) {
	            throw new RuntimeException("Failed to create XML Event Reader for data Source", e);
	        }    	    	 
	    	BufferedXMLEventReader buffXmlEvnetReader = new BufferedXMLEventReader(xmlEventReader, -1);
	    	final BufferedXMLEventReader bufferedXmlEventReader = buffXmlEvnetReader; 
	
	//    	final EntityIdentifier ei = person.getEntityIdentifier();
	//    	final IAuthorizationPrincipal ap = AuthorizationService.instance().newPrincipal(ei.getKey(), ei.getType());
	//    	final PortalDataKey portalDataKey = getPortalDataKey(bufferedXmlEventReader);
	    	
	    	//Step 4: for now remove this permission check, we can add one back in when we know the criteria
	//    	if (!ap.hasPermission("UP_SYSTEM", "IMPORT_ENTITY", portalDataKey.getName().getLocalPart())) {
	//    		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	//    		return;
	//    	}
	    	try {
		    	StAXSource fragmentSource = new StAXSource(bufferedXmlEventReader);
		    	portalDataHandlerService.importData(fragmentSource);
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    		throw new IOException(e.getMessage(), e);
	    	}
    	
    	} else {
    		throw new IOException("Group not found.");
    	}
    }
    
    /**
     * Delete an uPortal database object.  This method provides a REST interface
     * for uPortal database object deletion.
     * 
     * The path for this method is /entity/type/identifier.  The identifier generally
     * a string that may be used as a unique identifier, but is dependent on the 
     * entity type.  For example, to delete the "demo" user one might use the 
     * path /entity/user/demo.
     */
    @RequestMapping(value="/entity/{entityType}/{entityId}", method = RequestMethod.DELETE)
	public void deleteEntity(@PathVariable("entityType") String entityType,
			@PathVariable("entityId") String entityId, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
    	
		final IPerson person = personManager.getPerson(request);
		final EntityIdentifier ei = person.getEntityIdentifier();
	    final IAuthorizationPrincipal ap = AuthorizationService.instance().newPrincipal(ei.getKey(), ei.getType());

	    if (!ap.hasPermission(OWNER, DELETE_PERMISSION, entityType)) {
	    	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	    	return;
	    }
               
	    // get the task associated with exporting this entity type 
	    portalDataHandlerService.deleteData(entityType, entityId);
    	    	
    	response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @RequestMapping(value="/entity/{entityType}/{entityId}", method = RequestMethod.GET)
    public void exportEntity(@PathVariable("entityId") String entityId,
    		@PathVariable("entityType") String entityType,
    		@RequestParam(value="download", required=false) boolean download,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
    	
		final IPerson person = personManager.getPerson(request);
		final EntityIdentifier ei = person.getEntityIdentifier();
	    final IAuthorizationPrincipal ap = AuthorizationService.instance().newPrincipal(ei.getKey(), ei.getType());

	    // if the current user does not have permission to delete this database
	    // object type, return a 401 error code
	    if (!ap.hasPermission(OWNER, EXPORT_PERMISSION, entityType)) {
	    	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	    	return;
	    }
	    
	    //Export the data into a string buffer
	    final StringWriter exportBuffer = new StringWriter();
	    final String fileName = portalDataHandlerService.exportData(entityType, entityId, new StreamResult(exportBuffer));
        
	    if (download) {
	    	response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "." + entityType + ".xml\"");
	    }
	    
	    final PrintWriter responseWriter = response.getWriter();
	    responseWriter.print(exportBuffer.getBuffer());
    }
    
    
}
