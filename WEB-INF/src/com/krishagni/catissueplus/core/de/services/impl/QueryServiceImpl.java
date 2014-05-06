package com.krishagni.catissueplus.core.de.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.repository.UserDao;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.ObjectCreationException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.domain.AqlBuilder;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.domain.factory.QueryFolderFactory;
import com.krishagni.catissueplus.core.de.events.CreateQueryFolderEvent;
import com.krishagni.catissueplus.core.de.events.DeleteQueryFolderEvent;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEvent;
import com.krishagni.catissueplus.core.de.events.FolderQueriesEvent;
import com.krishagni.catissueplus.core.de.events.FolderQueriesUpdatedEvent;
import com.krishagni.catissueplus.core.de.events.QueryExecutedEvent;
import com.krishagni.catissueplus.core.de.events.QueryFolderCreatedEvent;
import com.krishagni.catissueplus.core.de.events.QueryFolderDeletedEvent;
import com.krishagni.catissueplus.core.de.events.QueryFolderDetailEvent;
import com.krishagni.catissueplus.core.de.events.QueryFolderDetails;
import com.krishagni.catissueplus.core.de.events.QueryFolderSharedEvent;
import com.krishagni.catissueplus.core.de.events.QueryFolderSummary;
import com.krishagni.catissueplus.core.de.events.QueryFolderUpdatedEvent;
import com.krishagni.catissueplus.core.de.events.QueryFoldersEvent;
import com.krishagni.catissueplus.core.de.events.QuerySavedEvent;
import com.krishagni.catissueplus.core.de.events.QueryUpdatedEvent;
import com.krishagni.catissueplus.core.de.events.ReqFolderQueriesEvent;
import com.krishagni.catissueplus.core.de.events.ReqQueryFolderDetailEvent;
import com.krishagni.catissueplus.core.de.events.ReqQueryFoldersEvent;
import com.krishagni.catissueplus.core.de.events.ReqSavedQueriesSummaryEvent;
import com.krishagni.catissueplus.core.de.events.ReqSavedQueryDetailEvent;
import com.krishagni.catissueplus.core.de.events.SaveQueryEvent;
import com.krishagni.catissueplus.core.de.events.SavedQueriesSummaryEvent;
import com.krishagni.catissueplus.core.de.events.SavedQueryDetail;
import com.krishagni.catissueplus.core.de.events.SavedQueryDetailEvent;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;
import com.krishagni.catissueplus.core.de.events.ShareQueryFolderEvent;
import com.krishagni.catissueplus.core.de.events.UpdateFolderQueriesEvent;
import com.krishagni.catissueplus.core.de.events.UpdateQueryEvent;
import com.krishagni.catissueplus.core.de.events.UpdateQueryFolderEvent;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;

import edu.common.dynamicextensions.query.Query;
import edu.common.dynamicextensions.query.QueryParserException;
import edu.common.dynamicextensions.query.QueryResultData;
import edu.wustl.common.beans.SessionDataBean;

public class QueryServiceImpl implements QueryService {
	private static final String cpForm = "CollectionProtocol";

	private static final String cprForm = "CollectionProtocolRegistration";

	private static final String dateFormat = "MM/dd/yyyy";

	private DaoFactory daoFactory;

	private UserDao userDao;
	
	private QueryFolderFactory queryFolderFactory;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public UserDao getUserDao() {
		return userDao;
	}

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}
	

	public QueryFolderFactory getQueryFolderFactory() {
		return queryFolderFactory;
	}

	public void setQueryFolderFactory(QueryFolderFactory queryFolderFactory) {
		this.queryFolderFactory = queryFolderFactory;
	}

	@Override
	@PlusTransactional
	public SavedQueriesSummaryEvent getSavedQueries(ReqSavedQueriesSummaryEvent req) {
		try {
			if (req.getStartAt() < 0 || req.getMaxRecords() <= 0) {
				String msg = SavedQueryErrorCode.INVALID_PAGINATION_FILTER.message();
				return SavedQueriesSummaryEvent.badRequest(msg, null);
			}

			Long userId = req.getSessionDataBean().getUserId();
			List<SavedQuery> queries = daoFactory.getSavedQueryDao().getQueries(userId, req.getStartAt(), req.getMaxRecords());
			return SavedQueriesSummaryEvent.ok(toQuerySummaryList(queries));
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return SavedQueriesSummaryEvent.serverError(message, e);
		}
	}

	@Override
	@PlusTransactional
	public SavedQueryDetailEvent getSavedQuery(ReqSavedQueryDetailEvent req) {
		try {
			SavedQuery savedQuery = daoFactory.getSavedQueryDao().getQuery(req.getQueryId());
			return SavedQueryDetailEvent.ok(SavedQueryDetail.fromSavedQuery(savedQuery));
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return SavedQueryDetailEvent.serverError(message, e);
		}
	}

	@Override
	@PlusTransactional
	public QuerySavedEvent saveQuery(SaveQueryEvent req) {
		try {
			SavedQueryDetail queryDetail = req.getSavedQueryDetail();
			if (queryDetail.getId() != null) {
				return QuerySavedEvent.badRequest(SavedQueryErrorCode.QUERY_ID_FOUND.message(), null);
			}

			Query query = Query.createQuery();
			query.wideRows(true).ic(true).dateFormat(dateFormat).compile(cprForm, getAql(queryDetail));

			SavedQuery savedQuery = getSavedQuery(req.getSessionDataBean(), queryDetail);	
			daoFactory.getSavedQueryDao().saveOrUpdate(savedQuery);
			return QuerySavedEvent.ok(SavedQueryDetail.fromSavedQuery(savedQuery));
		} catch (QueryParserException qpe) {
			return QuerySavedEvent.badRequest(qpe.getMessage(), qpe);
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return QuerySavedEvent.serverError(message, e);
		}	
	}

	@Override
	@PlusTransactional
	public QueryUpdatedEvent updateQuery(UpdateQueryEvent req) {
		try {
			SavedQueryDetail queryDetail = req.getSavedQueryDetail();

			Query query = Query.createQuery();
			query.wideRows(true).ic(true).dateFormat(dateFormat).compile(cprForm, getAql(queryDetail));

			SavedQuery savedQuery = getSavedQuery(req.getSessionDataBean(), queryDetail);
			SavedQuery existing = daoFactory.getSavedQueryDao().getQuery(queryDetail.getId());
			existing.update(savedQuery);

			daoFactory.getSavedQueryDao().saveOrUpdate(existing);	
			return QueryUpdatedEvent.ok(SavedQueryDetail.fromSavedQuery(existing));
		} catch (QueryParserException qpe) {
			return QueryUpdatedEvent.badRequest(qpe.getMessage(), qpe);
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return QueryUpdatedEvent.serverError(message, e);
		}
	}

	@Override
	@PlusTransactional
	public QueryExecutedEvent executeQuery(ExecuteQueryEvent req) {
		try {
			Query query = Query.createQuery();
			query.wideRows(req.isWideRows()).ic(true).dateFormat(dateFormat).compile(cprForm, req.getAql());
			QueryResultData queryResult = query.getData();
			return QueryExecutedEvent.ok(queryResult.getColumnLabels(),queryResult.getRows());
		} catch (QueryParserException qpe) {
			return QueryExecutedEvent.badRequest(qpe.getMessage(), qpe);
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return QueryExecutedEvent.serverError(message, e);
		}
	}
	
	@Override
	@PlusTransactional
	public QueryFoldersEvent getUserFolders(ReqQueryFoldersEvent req) {
		try {
			Long userId = req.getSessionDataBean().getUserId();			
			List<QueryFolder> queryFolders = daoFactory.getQueryFolderDao().getUserFolders(userId);

			List<QueryFolderSummary> result = new ArrayList<QueryFolderSummary>();
			for (QueryFolder folder : queryFolders) {
				result.add(QueryFolderSummary.fromQueryFolder(folder));
			}
			
			return QueryFoldersEvent.ok(result);
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return QueryFoldersEvent.serverError(message, e);
		}
	}
	
	@Override
	@PlusTransactional
	public QueryFolderDetailEvent getFolder(ReqQueryFolderDetailEvent req) {
		try {
			Long folderId = req.getFolderId();
			QueryFolder folder = daoFactory.getQueryFolderDao().getQueryFolder(folderId);
			if (folder == null) {
				return QueryFolderDetailEvent.notFound(folderId);
			}
			
			Long userId = req.getSessionDataBean().getUserId();
			if (!folder.canUserAccess(userId)) {
				return QueryFolderDetailEvent.notAuthorized(folderId);
			}
			
			return QueryFolderDetailEvent.ok(QueryFolderDetails.fromQueryFolder(folder));			
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return QueryFolderDetailEvent.serverError(message, e);			
		}
	}	
	
	
	@Override
	@PlusTransactional
	public QueryFolderCreatedEvent createFolder(CreateQueryFolderEvent req) {
		try {
			QueryFolderDetails folderDetails = req.getFolderDetails();
			
			UserSummary owner = new UserSummary();
			owner.setId(req.getSessionDataBean().getUserId());
			folderDetails.setOwner(owner);
			
			QueryFolder queryFolder = queryFolderFactory.createQueryFolder(folderDetails);	
			
			daoFactory.getQueryFolderDao().saveOrUpdate(queryFolder);
			return QueryFolderCreatedEvent.ok(QueryFolderDetails.fromQueryFolder(queryFolder));
		} catch (ObjectCreationException oce) {
			return QueryFolderCreatedEvent.badRequest(oce);
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return QueryFolderCreatedEvent.serverError(message, e);
		}
	}

	@Override
	@PlusTransactional
	public QueryFolderUpdatedEvent updateFolder(UpdateQueryFolderEvent req) {
		try {
			QueryFolderDetails folderDetails = req.getFolderDetails();
			Long folderId = folderDetails.getId();
			if (folderId == null) {
				return QueryFolderUpdatedEvent.badRequest(SavedQueryErrorCode.FOLDER_ID_REQUIRED);
			}
						
			QueryFolder existing = daoFactory.getQueryFolderDao().getQueryFolder(folderId);
			if (existing == null) {
				return QueryFolderUpdatedEvent.badRequest(SavedQueryErrorCode.FOLDER_DOESNT_EXISTS);
			}
			
			Long userId = req.getSessionDataBean().getUserId();
			if (!existing.getOwner().getId().equals(userId)) {
				return QueryFolderUpdatedEvent.badRequest(SavedQueryErrorCode.USER_NOT_AUTHORISED);
			}
			
			UserSummary owner = new UserSummary();
			owner.setId(userId);
			folderDetails.setOwner(owner);
			
			QueryFolder queryFolder = queryFolderFactory.createQueryFolder(folderDetails);
			existing.update(queryFolder);
			
			daoFactory.getQueryFolderDao().saveOrUpdate(existing);
			return QueryFolderUpdatedEvent.ok(QueryFolderDetails.fromQueryFolder(existing));			
		} catch (ObjectCreationException oce) {
			return QueryFolderUpdatedEvent.badRequest(oce);
		} catch (Exception e) {		
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return QueryFolderUpdatedEvent.serverError(message, e);
		}
	}

	@Override
	@PlusTransactional
	public QueryFolderDeletedEvent deleteFolder(DeleteQueryFolderEvent req) {
		try {
			Long folderId = req.getFolderId();
			if (folderId == null) {
				return QueryFolderDeletedEvent.notFound(folderId);
			}

			QueryFolder existing = daoFactory.getQueryFolderDao().getQueryFolder(folderId);
			if (existing == null) {
				return QueryFolderDeletedEvent.notFound(folderId);								
			}
			
			Long userId = req.getSessionDataBean().getUserId();
			if (!existing.getOwner().getId().equals(userId)) {
				return QueryFolderDeletedEvent.notAuthorized(folderId);
			}
			
			daoFactory.getQueryFolderDao().deleteFolder(existing);
			return QueryFolderDeletedEvent.ok(folderId);
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return QueryFolderDeletedEvent.serverError(message, e);
		}
	}
	
	@Override
	@PlusTransactional
	public FolderQueriesEvent getFolderQueries(ReqFolderQueriesEvent req) {
		try {
			Long folderId = req.getFolderId();
			QueryFolder folder = daoFactory.getQueryFolderDao().getQueryFolder(folderId);			
			if (folder == null) {
				return FolderQueriesEvent.notFound(folderId);
			}
			
			Long userId = req.getSessionDataBean().getUserId();
			if (!folder.canUserAccess(userId)) {
				return FolderQueriesEvent.notAuthorized(folderId);
			}
			
			return FolderQueriesEvent.ok(daoFactory.getSavedQueryDao().getQueriesByFolderId(folderId));
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return FolderQueriesEvent.serverError(message, e);
		}
	}
	
	@Override
	@PlusTransactional
	public FolderQueriesUpdatedEvent updateFolderQueries(UpdateFolderQueriesEvent req) {
		try {
			Long folderId = req.getFolderId();
			QueryFolder queryFolder = daoFactory.getQueryFolderDao().getQueryFolder(folderId);			
			if (queryFolder == null) {
				return FolderQueriesUpdatedEvent.notFound(folderId);
			}
			
			Long userId = req.getSessionDataBean().getUserId();
			if (!queryFolder.getOwner().getId().equals(userId)) {
				return FolderQueriesUpdatedEvent.notAuthorized(folderId);
			}
			
			List<SavedQuery> savedQueries = null;
			List<Long> queryIds = req.getQueries();
			
			if (queryIds == null || queryIds.isEmpty()) {
				savedQueries = new ArrayList<SavedQuery>();
			} else {
				savedQueries = daoFactory.getSavedQueryDao().getQueriesByIds(queryIds);
			}
			
			switch (req.getOp()) {
				case ADD:
					queryFolder.addQueries(savedQueries);
					break;
				
				case UPDATE:
					queryFolder.updateQueries(savedQueries);
					break;
				
				case REMOVE:
					queryFolder.removeQueries(savedQueries);
					break;				
			}
			
			daoFactory.getQueryFolderDao().saveOrUpdate(queryFolder);			
			List<SavedQuerySummary> result = new ArrayList<SavedQuerySummary>();
			for (SavedQuery query : savedQueries) {
				result.add(SavedQuerySummary.fromSavedQuery(query));
			}
			
			return FolderQueriesUpdatedEvent.ok(folderId, result);
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return FolderQueriesUpdatedEvent.serverError(message, e);
		}
	}
	
	@Override
	@PlusTransactional
	public QueryFolderSharedEvent shareFolder(ShareQueryFolderEvent req) {
		try {
			Long folderId = req.getFolderId();
			QueryFolder queryFolder = daoFactory.getQueryFolderDao().getQueryFolder(folderId);
			if (queryFolder == null) {
				return QueryFolderSharedEvent.notFound(folderId);
			}
			
			Long userId = req.getSessionDataBean().getUserId();
			if (!queryFolder.getOwner().getId().equals(userId)) {
				return QueryFolderSharedEvent.notAuthorized(folderId);
			}
			
			List<User> users = null;
			List<Long> userIds = req.getUserIds();
			if (userIds == null || userIds.isEmpty()) {
				users = new ArrayList<User>();
			} else {
				users = userDao.getUsersById(userIds);
			}
			
			switch (req.getOp()) {
				case ADD:
					queryFolder.addSharedUsers(users);
					break;
					
				case UPDATE:
					queryFolder.updateSharedUsers(users);
					break;
					
				case REMOVE:
					queryFolder.removeSharedUsers(users);
					break;					
			}
						
			daoFactory.getQueryFolderDao().saveOrUpdate(queryFolder);			
			List<UserSummary> result = new ArrayList<UserSummary>();
			for (User user : queryFolder.getSharedWith()) {
				result.add(UserSummary.fromUser(user));
			}
			
			return QueryFolderSharedEvent.ok(folderId, result);
		} catch (Exception e) {
			String message = e.getMessage();
			if (message == null) {
				message = "Internal Server Error";
			}
			return QueryFolderSharedEvent.serverError(message, e);
		}
	}
	
	private List<SavedQuerySummary> toQuerySummaryList(List<SavedQuery> queries) {
		List<SavedQuerySummary> querySummaries = new ArrayList<SavedQuerySummary>();
		
		for (SavedQuery savedQuery : queries) {
			querySummaries.add(SavedQuerySummary.fromSavedQuery(savedQuery));
		}
		
		return querySummaries;
	}	
	
	private SavedQuery getSavedQuery(SessionDataBean sdb, SavedQueryDetail detail) {
		User user = new User();
		user.setId(sdb.getUserId());
		
		SavedQuery savedQuery = new SavedQuery();
		savedQuery.setTitle(detail.getTitle());
		savedQuery.setSelectList(detail.getSelectList());
		savedQuery.setFilters(detail.getFilters());
		savedQuery.setQueryExpression(detail.getQueryExpression());
		if (detail.getId() == null) {
			savedQuery.setCreatedBy(user);
		}
		
		savedQuery.setLastUpdatedBy(user);
		savedQuery.setLastUpdated(Calendar.getInstance().getTime());
		return savedQuery;
	}


	private String getAql(SavedQueryDetail queryDetail) {
		return AqlBuilder.getInstance().getQuery(
				queryDetail.getSelectList(),
				queryDetail.getFilters(),
				queryDetail.getQueryExpression());
	}
}