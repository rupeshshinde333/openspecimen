<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%>

<table summary="" cellpadding="0" cellspacing="0" border="0">
    <tr>
    
      <td height="20" class="footerMenuItem" onmouseover="changeMenuStyle(this,'footerMenuItemOver'),showCursor()" onmouseout="changeMenuStyle(this,'footerMenuItem'),hideCursor()" onclick="document.location.href='ContactUs.do?requestFor=ContactUs'">
        &nbsp;&nbsp;
		<a class="footerMenuLink" href="ContactUs.do?requestFor=ContactUs">
			<bean:message key="app.contactUs" />
		</a>&nbsp;&nbsp;
      </td>
      
      <td>
      	<img src="images/ftrMenuSeparator.gif" width="1" height="16" alt="" />
      </td>
      
      <td height="20" class="footerMenuItem" onmouseover="changeMenuStyle(this,'footerMenuItemOver'),showCursor()" onmouseout="changeMenuStyle(this,'footerMenuItem'),hideCursor()" onclick="document.location.href='PrivacyNotice.do?requestFor=PrivacyNotice'">
        &nbsp;&nbsp;
        <a class="footerMenuLink" href="PrivacyNotice.do?requestFor=PrivacyNotice">
        	<bean:message key="app.privacyNotice" />
        </a>&nbsp;&nbsp;
      </td>
      
      <td>
      	<img src="images/ftrMenuSeparator.gif" width="1" height="16" alt="" />
      </td>
      
      <td height="20" class="footerMenuItem" onmouseover="changeMenuStyle(this,'footerMenuItemOver'),showCursor()" onmouseout="changeMenuStyle(this,'footerMenuItem'),hideCursor()" onclick="document.location.href='Disclaimer.do?requestFor=Disclaimer'">
        &nbsp;&nbsp;
        <a class="footerMenuLink" href="Disclaimer.do?requestFor=Disclaimer">
        	<bean:message key="app.disclaimer" />
        </a>&nbsp;&nbsp;
      </td>
      
      <td>
      	<img src="images/ftrMenuSeparator.gif" width="1" height="16" alt="" />
      </td>
      
      <td height="20" class="footerMenuItem" onmouseover="changeMenuStyle(this,'footerMenuItemOver'),showCursor()" onmouseout="changeMenuStyle(this,'footerMenuItem'),hideCursor()" onclick="document.location.href='Accessibility.do?requestFor=Accessibility'">
        &nbsp;&nbsp;
        <a class="footerMenuLink" href="Accessibility.do?requestFor=Accessibility">
        	<bean:message key="app.accessibility" />
        </a>&nbsp;&nbsp;
      </td>
      
      <td>
      	<img src="images/ftrMenuSeparator.gif" width="1" height="16" alt="" />
      </td>
      
      <td height="20" class="footerMenuItem" onmouseover="changeMenuStyle(this,'footerMenuItemOver'),showCursor()" onmouseout="changeMenuStyle(this,'footerMenuItem'),hideCursor()" onclick="document.location.href='ReportProblem.do?operation=add'">
        &nbsp;&nbsp;
        <a class="footerMenuLink" href="ReportProblem.do?operation=add">
        	<bean:message key="app.reportProblem" />
        </a>&nbsp;&nbsp;
      </td>
      
      <td>
      	<img src="images/ftrMenuSeparator.gif" width="1" height="16" alt="" />
      </td>
    </tr>
  </table>