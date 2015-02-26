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
            <form id="${n}form_create_teamtab" method="POST" action="javascript:;">
                
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
            <form id="${n}form_update_teamtab" method="POST" action="javascript:;">
                
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

            <form name="sendowner" action="//uportal.edia.nl/uPortal/p/fragment-admin.ctf3/max/action.uP?pP__eventId=selectFragment&pP_execution=e1s1" method="POST"> 
                <input type="hidden" name="impersonateUser" value="owner_team_tab_edia_uportal_group_1">
                <button style="display:none" type="submit">Submit</button>
            </form>
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
              var interval = setInterval(function(){          
                if (secondCountDown===0) {
                    clearInterval(interval);
                    $("#messagebox").html("<br/><font color=green size=+2><b>Refreshing page...</b></font>");        
                    window.location = "//" + window.location.hostname
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
        $("#${n}form_create_teamtab").on('submit', function () {
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

        $("#${n}form_update_teamtab").on('submit', function () {
            //must be entire url for some reason to make it work

            host = "//" + window.location.hostname + "/uPortal"
            var url = '<c:url value="' + host + '/p/fragment-admin.ctf3/max/action.uP?pP__eventId=selectFragment&pP_execution=e1s1"/>';
            var action = host + '/p/fragment-admin.ctf3/max/action.uP?pP__eventId=selectFragment&pP_execution=e1s1';
            //var url2 = host2 + '/uPortal/p/fragment-admin.ctf3/max/action.uP?pP__eventId=selectFragment&pP_execution=e1s1';
            var term = $("[name='ownerid']", this).val();
            var posting = $.post( url, { impersonateUser: term } );

            posting.done(function( data ) {
                $('input[name=impersonateUser]').val(term);
                $('form[name=sendowner]').attr('action', action);
                $('form[name=sendowner]').submit();

                //$.post(host + '/p/fragment-admin.ctf3/max/action.uP?pP__eventId=selectFragment&pP_execution=e1s1', {impersonateUser: term}, function () {
                //    window.location.reload();
                //});
                //$('<form action="' + host + '/p/fragment-admin.ctf3/max/action.uP?pP__eventId=selectFragment&pP_execution=e1s1" method="POST">' + 
                //'<input type="hidden" name="impersonateUser" value="' + term + '">' +
                //'<button type="submit">Click me</button></form>').submit(); 
            });


           return false;
        });
    });
</script>
