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
        <div class="toolbar" role="toolbar">
            <ul class="btn-group">
                <li class="btn"><a class="button" href="<portlet:renderURL/>"><spring:message code="import"/></a></li>
                <li class="btn"><a class="button" href="<portlet:renderURL><portlet:param name="action" value="delete"/></portlet:renderURL>"><spring:message code="delete"/></a></li>
                <li class="btn"><a class="button" href="<portlet:renderURL><portlet:param name="action" value="export"/></portlet:renderURL>"><spring:message code="export"/></a></li>
            </ul>
        </div>
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
                            <input type="text" id="groupid" name="groupid"/>
                        </td>
                    </tr>
<!--                     <tr> -->
<!--                         <td > -->
<!--                             Owner ID -->
<!--                         </td> -->
<!--                         <td> -->
<!--                             <input type="text" id="ownerid" name="ownerid"/> -->
<!--                         </td> -->
<!--                     </tr> -->
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
        var $ = up.jQuery;
        
        $("#${n}form").submit(function () {
           var form, groupid, href;
           
           form = this;
           
           groupid = form.groupid.value;

           $.ajax({
        	   url: "<c:url value="/api/create/fragment/group/"/>" + groupid,
               type: "GET",
               statusCode: {
                   200: function() {
                       $("#messagebox").html("<br/><font color=green size=+2><b>Team tab created</b></font>");
                       setTimeout(function(){$("#messagebox").html("");},5000); 
                   }
               }              
           });
           
           return false;
        });
    });
</script>
