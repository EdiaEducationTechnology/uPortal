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
            <p style="font-size: 16px;">Select a team for creating Team Tab</p>
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
        <br/><br/>
        <!-- Note -->
        <div class="portlet-note" role="note">
            <p style="font-size: 16px;">Select a Team Tab for editing</p>
        </div>
        
        <div class="portlet-form">
            <form id="${n}form2" method="POST" action="javascript:;">
                
                <table class="purpose-layout">
                    <tr>
                        <td>
                         <select id="${n}ownerid" name="ownerid" max-width="90%">
                            <option></option>
                            <c:forEach items="${teams}" var="teams">
                                <option value="${teams.key}"><spring:message code="${teams.value}"/></option>
                            </c:forEach>
                        </select>  
                        </td>
                    </tr>
                </table>
                <br/>
                <div class="buttons">
                    <input class="button btn primary update-team-tab" type="submit" value="Update Team tab" disabled="disabled"/>
                </div>
            </form>
            <div id="messagebox"></div>
        </div>
<!--
        <form method="post" name="fragmentAdminForm" action="/uPortal/p/fragment-admin.ctf3/max/action.uP?pP__eventId=selectFragment&pP_execution=e1s1">
                <select id="fragmentOwner" name="impersonateUser" title="Choose">
                    <option value="NONE"> -- <spring:message code="fragments"/> -- </option>
                    <option value="edia1">edia1</option>
                </select>
                <input class="button btn" type="submit" value="<spring:message code="go"/>" />
        </form> -->
    </div> <!-- end: portlet-content -->
</div> <!-- end: portlet -->

<script type="text/javascript">
     up.jQuery(document).ready(function () {
        
        var handleSuccesfulTeamTabCreation = function () {
              var secondCountDown = 5;
              setInterval(function(){          
                if (secondCountDown===0) {
                    $("#messagebox").html("<br/><font color=green size=+2><b>Refreshing page...</b></font>");
                    window.location = "/uPortal";
                } else {
                    $("#messagebox").html("<br/><font color=green size=+2><b>Team tab created. Refreshing after " + secondCountDown + " seconds</b></font>");
                    secondCountDown--;                  
                }
              },1000); 
            },

            $ = up.jQuery;

        $("[name='groupid']").on('change', function () {
            if ($(this).val()) {
                $('.create-team-tab').removeAttr('disabled');
            } else {
                $('.create-team-tab').attr('disabled', 'disabled');
            }
        });
        $("[name='ownerid']").on('change', function () {
            if ($(this).val()) {
                $('.update-team-tab').removeAttr('disabled');
            } else {
                $('.update-team-tab').attr('disabled', 'disabled');
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

        $("#${n}form2").on('submit', function () {
            //must be entire url for some reason to make it work
            var url = '<c:url value="/p/fragment-admin.ctf3/max/action.uP?pP__eventId=selectFragment&pP_execution=e1s1"/>';
            var term = $("[name='ownerid']", this).val();
            var posting = $.post( url, { impersonateUser: term } );

            posting.done(function( data ) {
                $('<form action="/uPortal/p/fragment-admin.ctf3/max/action.uP?pP__eventId=selectFragment&pP_execution=e1s1" method="POST">' + 
                '<input type="hidden" name="impersonateUser" value="' + term + '">' +
                '</form>').submit();
            });


           return false;
        });
    });
</script>
