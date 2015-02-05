<%--

    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.

--%>

<%@ include file="/WEB-INF/jsp/include.jsp" %>

<!-- Portlet -->
<div class="fl-widget portlet imp-exp view-export" role="section">
    
    <!-- Portlet Titlebar -->
    <div class="fl-widget-titlebar titlebar portlet-titlebar" role="sectionhead">
        <h2 class="title" role="heading">Create Team Tab</h2>
    </div>
    
    <!-- Portlet Content -->
    <div class="fl-widget-content content portlet-content" role="main">
        
        <!-- Note -->
        <div class="portlet-note" role="note">
            <p>This will create the Team Tab with empty layout</p>
        </div>
        
        <div class="portlet-form">
            <form id="${n}form" method="POST" action="javascript:;">
                
                <table class="purpose-layout">
                    <tr>
                        <td >
                            Group ID
                        </td>
                        <td>
			             <select id="${n}groupid" name="groupid">
                            <option></option>
                            <c:forEach items="${groupNames}" var="name">
                                <option value="${fn:escapeXml(name)}"><spring:message code="${name}"/></option>
                            </c:forEach>
                        </select>  
                        </td>
                    </tr>
                </table>
                <br/>
                <div class="buttons">
                    <input class="button btn primary" type="submit" value="Create Team tab"/>
                </div>
            </form>
            <div id="messagebox"></div>
        </div>
        
    </div> <!-- end: portlet-content -->
</div> <!-- end: portlet -->

<script type="text/javascript">
    up.jQuery(document).ready(function () {
        var handleSuccesfulTeamTabCreation = function () {
              $("#messagebox").html("<br/><font color=green size=+2><b>Team tab created</b></font>");
              setTimeout(function(){
                $("#messagebox").html("");
              },5000); 
            },
            $ = up.jQuery;
        
        $("#${n}form").on('submit', function () {
          var groupId = $("[name='groupid']", this).val()

          // TODO: make this a POST, change controller, we are creating, not getting
          $.ajax({
              url: "<c:url value=\"/api/create/fragment/group/\"/>" + groupId,
              type: "GET",
              statusCode: {
                200: function() {
                  handleSuccesfulTeamTabCreation();
                }
              }              
           });
           return false;
        });
    });
</script>
