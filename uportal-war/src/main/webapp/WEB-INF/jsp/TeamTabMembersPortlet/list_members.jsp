<%--
@author Gatis Druva

--%>

<%@ include file="/WEB-INF/jsp/include.jsp" %>

<!-- Portlet -->
<div class="fl-widget portlet imp-exp view-export" role="section">
    
    <!-- Portlet Content -->
    <div class="fl-widget-content content portlet-content" role="main">
        
        <!-- Note -->
        <div class="portlet-note" role="note">
            <p style="font-size: 16px;">Team managers/admins</p>
        </div>
        
        <div class="portlet-form">
            <form id="${n}form_list_members" method="POST" action="javascript:;">
                
                <table class="portlet-table table table-hover">
                    <thead>
                     <tr>
                        <th style="width: 80%">                            
                                User
                        </th>
                        <th>
                                Online
                        </th>
                    </tr>                       
                    </thead>
                    <tbody>
                	<c:forEach items="${managers}" var="managers">
                    <tr>
                        <td>                            
                                ${fn:escapeXml(managers.key)} 
                        </td>
                        <td>
                        		${managers.value}
                        </td>
                    </tr>
                    </c:forEach>
                    <tbody>
                </table>
                <br/><br/>
                <div class="portlet-note" role="note">
            		<p style="font-size: 16px;">Team members</p>
        		</div>
                <table class="portlet-table table table-hover">
                    <thead>
                     <tr>
                        <th style="width: 80%">                          
                                User
                        </th>
                        <th>
                                Online
                        </th>
                    </tr>                       
                    </thead>
                    <tbody>
                    <c:forEach items="${members}" var="members">
                    <tr>
                        <td>                            
                                ${fn:escapeXml(members.key)} 
                        </td>
                        <td>
                                ${members.value}
                        </td>
                    </tr>
                    </c:forEach>
                    <tbody>
                </table>
                <br/>
            </form>           
        </div>
        <br/><br/>


<script type="text/javascript">
     up.jQuery(document).ready(function () {
  
    });
</script>
