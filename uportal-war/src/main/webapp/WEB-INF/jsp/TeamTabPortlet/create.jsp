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
    
    <!-- Portlet Content -->
    <div class="fl-widget-content content portlet-content" role="main">
        
        <!-- Note -->
        <div class="portlet-note" role="note">
            <p style="font-size: 16px;">Select a team</p>
        </div>
        
        <div class="portlet-form">
            <form id="${n}form" method="POST" action="javascript:;">
                
                <table class="purpose-layout">
                    <tr>
                        <td>
                         <select id="${n}groupid" name="groupid" max-width="90%">
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
                    <input class="button btn primary create-team-tab" type="submit" value="Create Team tab" disabled="disabled"/>
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
                window.location = "//" + window.location.host;
              },5000); 
            },
            $ = up.jQuery;

        $("[name='groupid']").on('change', function () {
            if ($(this).val()) {
                $('.create-team-tab').removeAttr('disabled');
            } else {
                $('.create-team-tab').attr('disabled', 'disabled');
            }
        });

        $("#${n}form").on('submit', function () {
          var groupId = $("[name='groupid']", this).val()

          // TODO: make this a POST, change controller, we are creating, not getting
          $.ajax({
              url: '<c:url value="/api/create/fragment/group/"/>' + groupId,
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
