
package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.repository.SiteDao;
import com.krishagni.catissueplus.core.administrative.repository.SiteListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.util.Status;

public class SiteDaoImpl extends AbstractDao<Site> implements SiteDao {
	
	@Override
	public Class<?> getType() {
		return Site.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Site> getSites(SiteListCriteria listCrit) {
		Criteria query = sessionFactory.getCurrentSession()
				.createCriteria(Site.class)
				.add(Restrictions.eq("activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()))
				.addOrder(Order.asc("name"))
				.setFirstResult(listCrit.startAt())
				.setMaxResults(listCrit.maxResults());
				
		addSearchConditions(query, listCrit);
		return query.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Site> getSitesByNames(Collection<String> siteNames) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_SITES_BY_NAMES)
				.setParameterList("siteNames", siteNames)
				.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Site> getSitesByNamesAndStatus(Collection<String> siteNames, Collection<String> status) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_SITES_BY_NAMES_AND_STATUS)
				.setParameterList("siteNames", siteNames)
				.setParameterList("status", status)
				.list();
	}
	
	@Override
	public Site getSiteByName(String siteName) {
		List<Site> result = getSitesByNames(Collections.singletonList(siteName));
		
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public Site getSiteByNameAndStatus(String siteName, Collection<String> status) {
		List<Site> result = getSitesByNamesAndStatus(Collections.singletonList(siteName), status);
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Site getSiteByCode(String siteCode) {
		List<Site> result = getSessionFactory().getCurrentSession()
				.getNamedQuery(GET_SITE_BY_CODE)
				.setString("siteCode", siteCode)
				.list();
		
		return result.isEmpty() ? null : result.get(0);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Integer> getCpCountBySite(Collection<Long> siteIds) {		
		List<Object[]> rows = getSessionFactory().getCurrentSession().getNamedQuery(GET_CP_COUNT_BY_SITES)
			.setParameterList("siteIds", siteIds)
			.list();
		
		Map<Long, Integer> countMap = new HashMap<Long, Integer>();
		for (Object[] row : rows) {
			Long siteId = (Long)row[0];
			Integer count = ((Long)row[1]).intValue();
			countMap.put(siteId, count);
		}
		
		return countMap;
	}

	
	private void addSearchConditions(Criteria query, SiteListCriteria listCrit) {
		if (StringUtils.isNotBlank(listCrit.query())) {
			MatchMode matchMode = listCrit.exactMatch() ? MatchMode.EXACT : MatchMode.ANYWHERE;
			query.add(Restrictions.ilike("name", listCrit.query(), matchMode));
		}
		
		if (StringUtils.isNotBlank(listCrit.institute())) {
			query.createAlias("institute", "i")
				.add(Restrictions.eq("i.name", listCrit.institute()));	
		}
	}
			
	private static final String FQN = Site.class.getName();

	private static final String GET_SITES_BY_NAMES = FQN + ".getSitesByNames";

	private static final String GET_SITES_BY_NAMES_AND_STATUS = FQN + ".getSitesByNamesAndStatus";

	private static final String GET_SITE_BY_CODE = FQN + ".getSiteByCode";
	
	private static final String GET_CP_COUNT_BY_SITES = FQN + ".getCpCountBySites";
}
