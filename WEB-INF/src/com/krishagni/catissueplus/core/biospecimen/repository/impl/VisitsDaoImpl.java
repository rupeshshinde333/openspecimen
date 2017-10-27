
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent.EventPointIntervalUnit;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsDao;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.util.Utility;

public class VisitsDaoImpl extends AbstractDao<Visit> implements VisitsDao {
	
	@Override
	public Class<Visit> getType() {
		return Visit.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<VisitSummary> getVisits(VisitsListCriteria crit) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_VISITS_SUMMARY_BY_CPR_ID)
			.setLong("cprId", crit.cprId())
			.list();
		
		List<VisitSummary> visits = new ArrayList<>();
		Map<Long, VisitSummary> createdVisits = new HashMap<>();      // visitId: key
		Map<Long, VisitSummary> anticipatedVisits = new HashMap<>();  // eventId: key

		Date regDate = null;
		int minEventPoint = 0, minEventPointInDays = 0;
		EventPointIntervalUnit minEventPointIntervalUnit = EventPointIntervalUnit.DAYS;
		for (Object[] row : rows) {
			Long visitId = (Long)row[0];
			String eventStatus = (String)row[3];
			if (visitId == null && StringUtils.isNotBlank(eventStatus) && eventStatus.equals("Disabled")) {
				continue;
			}
			
			VisitSummary visit = new VisitSummary();
			visit.setId(visitId);
			visit.setEventId((Long)row[1]);
			visit.setName((String)row[2]);
			visit.setEventLabel((String)row[4]);
			visit.setEventPoint((Integer)row[5]);
			visit.setIntervalUnit(StringUtils.isNotBlank((String) row[6]) ?
				EventPointIntervalUnit.valueOf((String) row[6]) : null);
			visit.setStatus((String)row[7]);
			visit.setVisitDate((Date)row[8]);
			regDate = (Date)row[9];
			visit.setMissedReason((String)row[10]);
			visit.setCpId((Long)row[11]);
			visits.add(visit);

			if (crit.includeStat()) {
				if (visit.getId() != null) {
					createdVisits.put(visitId, visit);
				} else {
					anticipatedVisits.put(visit.getEventId(), visit);
				}
			}

			if (visit.getEventPoint() != null) {
				Integer eventPointNoOfDays = Utility.getNoOfDays(visit.getEventPoint(), visit.getIntervalUnit());
				if (eventPointNoOfDays < minEventPointInDays) {
					minEventPoint = visit.getEventPoint();
					minEventPointInDays = eventPointNoOfDays;
					minEventPointIntervalUnit = visit.getIntervalUnit();
				}
			}
		}

		Calendar cal = Calendar.getInstance();
		for (VisitSummary visit : visits) {
			if (visit.getEventPoint() == null) {
				continue;
			}

			cal.setTime(regDate);
			setAnticipatedVisitDateInCalender(visit.getIntervalUnit(), visit.getEventPoint(), cal);
			if (minEventPoint < 0) {
				setAnticipatedVisitDateInCalender(minEventPointIntervalUnit, -minEventPoint, cal);
			}
			visit.setAnticipatedVisitDate(cal.getTime());
		}

		if (crit.includeStat()) {
			if (!createdVisits.isEmpty()) {
				loadCreatedVisitStats(createdVisits);
			}

			if (!anticipatedVisits.isEmpty()) {
				loadAnticipatedVisitStats(anticipatedVisits);
			}
		}

		Collections.sort(visits);
		return visits;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getVisitsList(VisitsListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(Visit.class, "visit");

		String startAlias = "cpr";
		if (crit.cpId() != null) {
			startAlias = "cpSite";
			query.createAlias("visit.registration", "cpr")
				.createAlias("cpr.collectionProtocol", "cp")
				.add(Restrictions.eq("cp.id", crit.cpId()));
		}

		boolean limitItems = true;
		if (CollectionUtils.isNotEmpty(crit.names())) {
			query.add(Restrictions.in("name", crit.names()));
			limitItems = false;
		}

		if (CollectionUtils.isNotEmpty(crit.siteCps())) {
			BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit.siteCps(), crit.useMrnSites(), startAlias);
		}

		if (limitItems) {
			query.setFirstResult(crit.startAt()).setMaxResults(crit.maxResults());
		}

		return query.addOrder(Order.asc("id")).list();
	}

	@Override
	public Visit getByName(String name) {
		List<Visit> visits = getByName(Collections.singleton(name));
		return !visits.isEmpty() ? visits.iterator().next() : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getByName(Collection<String> names) {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_VISIT_BY_NAME)
			.setParameterList("names", names)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getByIds(Collection<Long> ids) {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_VISITS_BY_IDS)
			.setParameterList("ids", ids)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getBySpr(String sprNumber) {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_VISIT_BY_SPR)
			.setString("sprNumber", sprNumber)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getCprVisitIds(String key, Object value) {
		List<Object[]> rows = getCurrentSession().createCriteria(Visit.class)
			.createAlias("registration", "cpr")
			.createAlias("cpr.collectionProtocol", "cp")
			.setProjection(
				Projections.projectionList()
					.add(Projections.property("id"))
					.add(Projections.property("cpr.id"))
					.add(Projections.property("cp.id")))
			.add(Restrictions.eq(key, value))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return Collections.emptyMap();
		}

		Object[] row = rows.iterator().next();
		Map<String, Object> ids = new HashMap<>();
		ids.put("visitId", row[0]);
		ids.put("cprId", row[1]);
		ids.put("cpId", row[2]);
		return ids;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Visit getLatestVisit(Long cprId) {
		List<Visit> visits = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_LATEST_VISIT_BY_CPR_ID)
			.setLong("cprId", cprId)
			.setMaxResults(1)
			.list();

		return visits.isEmpty() ? null :  visits.get(0);
	}

	@SuppressWarnings("unchecked")
	private void loadCreatedVisitStats(Map<Long, VisitSummary> visitsMap) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_VISIT_STATS)
			.setParameterList("visitIds", visitsMap.keySet())
			.list();

		for (Object[] row : rows) {
			int idx = 0;
			Long visitId = (Long) row[idx++];
			VisitSummary visit = visitsMap.get(visitId);
			visit.setTotalPendingSpmns((Integer) row[idx++]);
			visit.setPendingPrimarySpmns((Integer) row[idx++]);
			visit.setPlannedPrimarySpmnsColl((Integer) row[idx++]);
			visit.setUnplannedPrimarySpmnsColl((Integer) row[idx++]);
			visit.setUncollectedPrimarySpmns((Integer) row[idx++]);
			visit.setStoredSpecimens((Integer) row[idx++]);
			visit.setNotStoredSpecimens((Integer) row[idx++]);
			visit.setDistributedSpecimens((Integer) row[idx++]);
			visit.setClosedSpecimens((Integer) row[idx++]);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadAnticipatedVisitStats(Map<Long, VisitSummary> visitsMap) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_ANTICIPATED_VISIT_STATS)
			.setParameterList("eventIds", visitsMap.keySet())
			.list();

		for (Object[] row : rows) {
			int idx = 0;
			Long eventId = (Long) row[idx++];
			VisitSummary visit = visitsMap.get(eventId);
			visit.setTotalPendingSpmns((Integer) row[idx++]);
			visit.setPendingPrimarySpmns((Integer) row[idx++]);
		}
	}

	private void setAnticipatedVisitDateInCalender(EventPointIntervalUnit intervalUnit, Integer eventPoint, Calendar cal) {
		switch (intervalUnit) {
			case DAYS:
				cal.add(Calendar.DAY_OF_YEAR, eventPoint);
				break;
			case WEEKS:
				cal.add(Calendar.WEEK_OF_YEAR, eventPoint);
				break;
			case MONTHS:
				cal.add(Calendar.MONTH, eventPoint);
				break;
			case YEARS:
				cal.add(Calendar.YEAR, eventPoint);
				break;
		}
	}

	private static final String FQN = Visit.class.getName();
	
	private static final String GET_VISITS_SUMMARY_BY_CPR_ID = FQN + ".getVisitsSummaryByCprId";

	private static final String GET_VISIT_STATS = FQN + ".getVisitStats";

	private static final String GET_ANTICIPATED_VISIT_STATS = FQN + ".getAnticipatedVisitStats";

	private static final String GET_VISITS_BY_IDS = FQN + ".getVisitsByIds";

	private static final String GET_VISIT_BY_NAME = FQN + ".getVisitByName";

	private static final String GET_VISIT_BY_SPR = FQN + ".getVisitBySpr";

	private static final String GET_LATEST_VISIT_BY_CPR_ID = FQN + ".getLatestVisitByCprId";
}

