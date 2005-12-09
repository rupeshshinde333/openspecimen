/**
 * <p>Title: AssignPrivilegeAction Class>
 * <p>Description:	This class initializes the fields of AssignPrivilege.jsp Page</p>
 * Copyright:    Copyright (c) year
 * Company: Washington University, School of Medicine, St. Louis.
 * @author Aniruddha Phadnis
 * @version 1.00
 * Created on Sep 20, 2005
 */

package edu.wustl.catissuecore.action;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import edu.wustl.catissuecore.actionForm.AssignPrivilegesForm;
import edu.wustl.catissuecore.bizlogic.AbstractBizLogic;
import edu.wustl.catissuecore.bizlogic.BizLogicFactory;
import edu.wustl.catissuecore.util.global.Constants;
import edu.wustl.common.beans.SessionDataBean;
import edu.wustl.common.util.logger.Logger;

public class AssignPrivilegeAction extends BaseAction
{
    /**
     * Overrides the execute method of Action class.
     * Initializes the various fields in AssignPrivilege.jsp Page.
     * */
    public ActionForward executeAction(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
    	AssignPrivilegesForm privilegeForm = (AssignPrivilegesForm)form;
    	
    	try
		{
    		AbstractBizLogic bizLogic = null;
    		//DAO dao = DAOFactory.getDAO(Constants.HIBERNATE_DAO);
    		
    		String [] groupUsers = privilegeForm.getGroups();
    		String [] privileges = {privilegeForm.getPrivilege()};
    		String [] objectIds = privilegeForm.getRecordIds();
    		
    		Long [] objectIdentifiers = new Long[objectIds.length];
    		
    		for(int i=0;i<objectIds.length;i++)
    		{
    			objectIdentifiers[i] = new Long(objectIds[i]);
    		}
    		
    		SessionDataBean bean = getSessionData(request);

    		boolean assignOperation = Constants.PRIVILEGE_ASSIGN;
    		Logger.out.debug("Operation....................................."+privilegeForm.getAssignOperation());
    		if (privilegeForm.getAssignOperation().equals("Disallow"))
			{
			    assignOperation = Constants.PRIVILEGE_DEASSIGN;
			}
    		
    		for(int i=0;i<groupUsers.length;i++)
    		{
    			bizLogic = BizLogicFactory.getBizLogic(privilegeForm.getObjectType());
    			Class classObject = Class.forName(privilegeForm.getObjectType());

    			if(groupUsers[i].startsWith("Role_")) //IF THE SELECTED OPTION IS GROUP THEN
    			{
    				String groupId = groupUsers[i].substring(groupUsers[i].indexOf("_") + 1);
    				
    				Logger.out.debug("privileges[0] " + privileges[0]);
    				Logger.out.debug("classObject " + classObject);
    				Logger.out.debug("groupId " + groupId);
    				Logger.out.debug("objectIdentifiers " + objectIdentifiers);
    				Logger.out.debug("bizlogic:"+bizLogic.getClass());
    				bizLogic.setPrivilege(Constants.HIBERNATE_DAO,privileges[0],classObject,objectIdentifiers,null,bean,groupId,false,assignOperation);
    			}
    			else //IF THE SELECTED OPTION IS USER THEN
    			{
    			    Logger.out.debug("In here");
    				Long userId = new Long(groupUsers[i]);
    				bizLogic.setPrivilege(Constants.HIBERNATE_DAO,privileges[0],classObject,objectIdentifiers,userId,bean,null,true, assignOperation);
    			}
    		}

    		request.setAttribute(Constants.STATUS_MESSAGE_KEY,"38.true");
		}
    	catch(Exception e)
		{
    		Logger.out.debug(e.getMessage(),e);
    		request.setAttribute(Constants.STATUS_MESSAGE_KEY,"38.false");
		}
    	
    	return mapping.findForward((String)request.getParameter(Constants.PAGEOF));
    }
}