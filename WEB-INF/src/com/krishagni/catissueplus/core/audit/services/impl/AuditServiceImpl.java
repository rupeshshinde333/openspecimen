package com.krishagni.catissueplus.core.audit.services.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.domain.factory.AuditErrorCode;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.AuditQueryCriteria;
import com.krishagni.catissueplus.core.audit.events.RevisionDetail;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.ObjectAccessorFactory;

public class AuditServiceImpl implements AuditService {

	private DaoFactory daoFactory;

	private ObjectAccessorFactory objectAccessorFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setObjectAccessorFactory(ObjectAccessorFactory objectAccessorFactory) {
		this.objectAccessorFactory = objectAccessorFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<AuditDetail>> getAuditDetail(RequestEvent<List<AuditQueryCriteria>> req) {
		return ResponseEvent.response(getAuditDetail(req.getPayload()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<RevisionDetail>> getRevisions(RequestEvent<List<AuditQueryCriteria>> req) {
		List<AuditQueryCriteria> criteria = req.getPayload();
		ensureReadAccess(criteria);

		List<RevisionDetail> revisions = criteria.stream().map(this::getRevisions)
			.flatMap(Collection::stream)
			.collect(Collectors.toList());

		if (criteria.size() > 1) {
			Collections.sort(revisions, (r1, r2) -> r2.getChangedOn().compareTo(r1.getChangedOn()));
		}

		return ResponseEvent.response(revisions);
	}

	@Override
	@PlusTransactional
	public void insertApiCallLog(UserApiCallLog userAuditLog) {
		daoFactory.getAuditDao().saveOrUpdate(userAuditLog);
	}

	@Override
	@PlusTransactional
	public long getTimeSinceLastApiCall(Long userId, String token) {
		Date lastApiCallTime = daoFactory.getAuditDao().getLatestApiCallTime(userId, token);
		long timeSinceLastApiCallInMilli = Calendar.getInstance().getTime().getTime() - lastApiCallTime.getTime();
		return TimeUnit.MILLISECONDS.toMinutes(timeSinceLastApiCallInMilli);
	}

	private List<AuditDetail> getAuditDetail(List<AuditQueryCriteria> criteria) {
		ensureReadAccess(criteria);
		return criteria.stream().map(this::getAuditDetail).collect(Collectors.toList());
	}

	private void ensureReadAccess(List<AuditQueryCriteria> criteria) {
		for (AuditQueryCriteria crit : criteria) {
			ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
			if (accessor == null) {
				throw OpenSpecimenException.userError(AuditErrorCode.ENTITY_NOT_FOUND, crit.getObjectName());
			}

			accessor.ensureReadAllowed(crit.getObjectId());
		}
	}

	private AuditDetail getAuditDetail(AuditQueryCriteria crit) {
		ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
		return daoFactory.getAuditDao().getAuditDetail(accessor.getAuditTable(), crit.getObjectId());
	}

	private List<RevisionDetail> getRevisions(AuditQueryCriteria crit) {
		ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
		return daoFactory.getAuditDao().getRevisions(accessor.getAuditTable(), crit.getObjectId());
	}
}