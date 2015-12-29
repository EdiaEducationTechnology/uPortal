package org.jasig.portal.utils.web;

import org.jasig.portal.EntityIdentifier;
import org.jasig.portal.groups.IEntityGroup;
import org.jasig.portal.rest.ImportExportController;
import org.jasig.portal.security.IPerson;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;

@RunWith(value = MockitoJUnitRunner.class)
public class ConextSyncGroupStateFilterTest {

    @InjectMocks
    ConextSyncGroupStateFilter conextSyncGroupStateFilter;

    @Mock
    ImportExportController importExportController;

    @Mock
    IPerson iPerson;

    public String vootResponse = "[\n" +
            "    {\n" +
            "        \"id\": \"8878ae43-965a-412a-87b5-38c398a76569\",\n" +
            "        \"displayName\": \"Project on group APIs\",\n" +
            "        \"membership\": {\n" +
            "            \"basic\": \"member\"\n" +
            "        }\n" +
            "    },\n" +
            "    {\n" +
            "        \"id\": \"e01eafb1-5f1c-4992-fcd5-ab0160c7ad24\",\n" +
            "        \"displayName\": \"Course M.201 Mathematics at University of Oslo\",\n" +
            "        \"description\": \"Second year mathematics at the university\",\n" +
            "        \"active\": true,      \n" +
            "        \"notBefore\": \"2006-08-01T12:00:00Z\",\n" +
            "        \"public\": true,\n" +
            "        \"sourceID\": \"voot:sources:uninett:fs\",\n" +
            "        \"membership\": {\n" +
            "            \"basic\": \"manager\",\n" +
            "            \"affiliation\": \"student\",\n" +
            "            \"may\": {\n" +
            "                \"listMembers\": true\n" +
            "            }\n" +
            "        },\n" +
            "        \"type\": \"voot:groupTypes:edu:courses\"\n" +
            "    }\n" +
            "]";

    @Test
    public void testHandleVootGroupResponse() throws Exception{
        JSONArray response = new JSONArray(vootResponse);

        Map<String,String> groupIdToRole = new HashMap<>();
        Map<String, String> groupIdToToolPlacementId = new HashMap<>();

        EntityIdentifier[] group1 = new EntityIdentifier[1];
        group1[0] = new EntityIdentifier("group1Id", IEntityGroup.class);

        EntityIdentifier[] group2 = new EntityIdentifier[1];
        group2[0] = new EntityIdentifier("group2Id", IEntityGroup.class);

        Mockito.when(importExportController.getGroupIdentifiers(eq("8878ae43-965a-412a-87b5-38c398a76569"))).thenReturn(group1);
        Mockito.when(importExportController.getGroupIdentifiers(eq("e01eafb1-5f1c-4992-fcd5-ab0160c7ad24"))).thenReturn(group2);
        conextSyncGroupStateFilter.handleVootGroupResponse(iPerson, false, groupIdToRole, groupIdToToolPlacementId, response);

        Assert.assertEquals("This key should have role member", "member", groupIdToRole.get("group1Id"));
        Assert.assertEquals("This key should have role member", "manager", groupIdToRole.get("group2Id"));
    }

    @Test
    public void handleEmptyVootResponse() throws Exception{

        JSONArray response = new JSONArray("[]");
        Map<String,String> groupIdToRole = new HashMap<>();
        Map<String, String> groupIdToToolPlacementId = new HashMap<>();

        conextSyncGroupStateFilter.handleVootGroupResponse(iPerson, false, groupIdToRole, groupIdToToolPlacementId, response);


    }
}
