
package com.krishagni.catissueplus.core.administrative.repository.impl;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.administrative.repository.ContainerRestrictionsCriteria;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerDao;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSite;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.util.Status;

public class StorageContainerDaoImpl extends AbstractDao<StorageContainer> implements StorageContainerDao {

	@Override
	public Class<?> getType() {
		return StorageContainer.class;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<StorageContainer> getStorageContainers(StorageContainerListCriteria listCrit) {
		return new ListQueryBuilder(listCrit).query()
				.setFirstResult(listCrit.startAt())
				.setMaxResults(listCrit.maxResults())
				.list();
	}
	
	@Override
	public Long getStorageContainersCount(StorageContainerListCriteria listCrit) {
		return ((Number) new ListQueryBuilder(listCrit, true).query().uniqueResult()).longValue();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public StorageContainer getByName(String name) {
		List<StorageContainer> result = sessionFactory.getCurrentSession()
				.createCriteria(StorageContainer.class)
				.add(Restrictions.eq("name", name))
				.add(Restrictions.eq("activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()))
				.list();

		return result.isEmpty() ? null : result.iterator().next();
	}
	
	@SuppressWarnings("unchecked")
	public StorageContainer getByBarcode(String barcode) {		
		List<StorageContainer> result = sessionFactory.getCurrentSession()
				.createCriteria(StorageContainer.class)
				.add(Restrictions.eq("barcode", barcode))
				.add(Restrictions.eq("activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()))
				.list();

		return result.isEmpty() ? null : result.iterator().next();		
	}

	@Override
	public void delete(StorageContainerPosition position) {
		sessionFactory.getCurrentSession().delete(position);		
	}

	@Override
	public Map<String, Object> getContainerIds(String key, Object value) {
		return getObjectIds("containerId", key, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getNonCompliantContainers(ContainerRestrictionsCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(StorageContainer.class)
			.createAlias("descendentContainers", "d")
			.add(Restrictions.eq("id", crit.containerId()))
			.setProjection(Projections.distinct(Projections.property("d.name")));

		Disjunction restriction = Restrictions.disjunction();
		if (isNotEmpty(crit.specimenClasses())) {
			query.createAlias("d.compAllowedSpecimenClasses", "spmnClass", JoinType.LEFT_OUTER_JOIN);
			restriction.add(
				Restrictions.conjunction()
					.add(Restrictions.isNotNull("spmnClass.elements"))
					.add(Restrictions.not(Restrictions.in("spmnClass.elements", crit.specimenClasses())))
			);
		}

		if (isNotEmpty(crit.specimenTypes())) {
			query.createAlias("d.compAllowedSpecimenTypes", "spmnType", JoinType.LEFT_OUTER_JOIN);
			restriction.add(
				Restrictions.conjunction()
					.add(Restrictions.isNotNull("spmnType.elements"))
					.add(Restrictions.not(Restrictions.in("spmnType.elements", crit.specimenTypes())))
			);
		}

		if (isNotEmpty(crit.collectionProtocols())) {
			query.createAlias("d.compAllowedCps", "cp", JoinType.LEFT_OUTER_JOIN);
			restriction.add(
				Restrictions.conjunction()
					.add(Restrictions.isNotNull("cp.id"))
					.add(Restrictions.not(Restrictions.in("cp.id", crit.collectionProtocolIds())))
			);
		} else {
			DetachedCriteria siteCrit = DetachedCriteria.forClass(CollectionProtocolSite.class)
				.createAlias("collectionProtocol", "cp1")
				.createAlias("site", "site1")
				.add(Restrictions.eqProperty("cp1.id", "cp.id"))
				.setProjection(Property.forName("site1.id"));

			query.createAlias("d.compAllowedCps", "cp", JoinType.LEFT_OUTER_JOIN);
			restriction.add(
				Restrictions.conjunction()
					.add(Restrictions.isNotNull("cp.id"))
					.add(Subqueries.notIn(crit.siteId(), siteCrit))
			);
		}

		flush();
		return query.add(restriction).list();
	}

	@SuppressWarnings(value = "unchecked")
	@Override
	public List<String> getNonCompliantSpecimens(ContainerRestrictionsCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(StorageContainer.class)
			.createAlias("descendentContainers", "d")
			.createAlias("d.occupiedPositions", "pos")
			.createAlias("pos.occupyingSpecimen", "spmn")
			.createAlias("spmn.visit", "visit")
			.createAlias("visit.registration", "cpr")
			.createAlias("cpr.collectionProtocol", "cp")
			.add(Restrictions.eq("id", crit.containerId()))
			.setProjection(Projections.distinct(Projections.property("spmn.label")));

		Disjunction restriction = Restrictions.disjunction();
		if (isNotEmpty(crit.specimenClasses())) {
			restriction.add(Restrictions.not(Restrictions.in("spmn.specimenClass", crit.specimenClasses())));
		}

		if (isNotEmpty(crit.specimenTypes())) {
			restriction.add(Restrictions.not(Restrictions.in("spmn.specimenType", crit.specimenTypes())));
		}

		if (isNotEmpty(crit.collectionProtocols())) {
			restriction.add(Restrictions.not(Restrictions.in("cp.id", crit.collectionProtocolIds())));
		} else {
			DetachedCriteria siteCrit = DetachedCriteria.forClass(CollectionProtocolSite.class)
				.createAlias("collectionProtocol", "cp1")
				.createAlias("site", "site1")
				.add(Restrictions.eqProperty("cp1.id", "cp.id"))
				.setProjection(Property.forName("site1.id"));

			query.createAlias("cp.sites", "cpSite").createAlias("cpSite.site", "site");
			restriction.add(Subqueries.propertyNotIn("site.id", siteCrit));
		}

		return query.add(restriction).list();
	}

	@Override
	public int getSpecimensCount(Long containerId) {
		Map<Long, Integer> counts = getSpecimensCount(Collections.singletonList(containerId));
		return counts.containsKey(containerId) ? counts.get(containerId) : 0;
	}

	@SuppressWarnings(value = "unchecked")
	@Override
	public Map<Long, Integer> getSpecimensCount(Collection<Long> containerIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_SPECIMENS_COUNT)
			.setParameterList("containerIds", containerIds)
			.list();

		return rows.stream().collect(Collectors.toMap(r -> ((Number)r[0]).longValue(), r -> ((Number)r[1]).intValue()));
	}

	@Override
	public List<Specimen> getSpecimens(SpecimenListCriteria crit, boolean orderByLocation) {
		Criteria query = getCurrentSession().createCriteria(Specimen.class, "specimen")
			.createAlias("specimen.position", "pos")
			.add(Subqueries.propertyIn("specimen.id", getContainerSpecimensListQuery(crit)))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults());

		if (orderByLocation) {
			query.createAlias("pos.container", "container")
				.addOrder(Order.asc("container.name"))
				.addOrder(Order.asc("pos.posTwoOrdinal"))
				.addOrder(Order.asc("pos.posOneOrdinal"));
		} else {
			query.addOrder(Order.asc("pos.id"));
		}

		return query.list();
	}

	@Override
	public Long getSpecimensCount(SpecimenListCriteria crit) {
		Number count = (Number) getContainerSpecimensListQuery(crit)
			.setProjection(Projections.rowCount())
			.getExecutableCriteria(getCurrentSession())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public Map<Long, Integer> getRootContainerSpecimensCount(Collection<Long> containerIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_ROOT_CONT_SPMNS_COUNT)
			.setParameterList("containerIds", containerIds)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (Long)row[0], row -> (Integer)row[1]));
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public Map<String, Integer> getSpecimensCountByType(Long containerId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_SPMNS_CNT_BY_TYPE)
			.setLong("containerId", containerId)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (String)row[0], row -> (Integer)row[1]));
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public StorageContainerSummary getAncestorsHierarchy(Long containerId) {
		Set<Long> ancestorIds = new HashSet<>();
		Long rootId = null;

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_ANCESTORS)
			.setLong("containerId", containerId)
			.list();
		for (Object[] row : rows) {
			ancestorIds.add((Long)row[0]);
			if (row[1] == null) {
				rootId = (Long)row[0];
			}
		}

		rows = getCurrentSession().getNamedQuery(GET_ROOT_AND_CHILD_CONTAINERS)
			.setLong("rootId", rootId)
			.setParameterList("parentIds", ancestorIds)
			.list();

		Map<Long, StorageContainerSummary> containersMap = new HashMap<>();
		for (Object[] row : rows) {
			StorageContainerSummary container = createContainer(row, 10000);
			containersMap.put(container.getId(), container);
		}

		linkParentChildContainers(containersMap);
		return sortChildContainers(containersMap.get(rootId));
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public List<StorageContainerSummary> getChildContainers(Long containerId, Integer noOfColumns) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_CHILD_CONTAINERS)
			.setLong("parentId", containerId)
			.list();

		return rows.stream().map(row -> createContainer(row, noOfColumns))
			.sorted(this::comparePositions).collect(Collectors.toList());
	}

	@Override
	@SuppressWarnings(value = "unchecked")
	public List<StorageContainer> getDescendantContainers(StorageContainerListCriteria crit) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StorageContainer.class, "cont")
			.setProjection(Projections.distinct(Projections.property("cont.id")));

		Criteria innerQuery = detachedCriteria.getExecutableCriteria(getCurrentSession())
			.createAlias("cont.ancestorContainers", "ancestors")
			.add(Restrictions.eq("ancestors.id", crit.parentContainerId()));

		if (crit.siteCps() != null && !crit.siteCps().isEmpty()) {
			innerQuery.createAlias("cont.site", "site")
				.createAlias("cont.allowedCps", "cp", JoinType.LEFT_OUTER_JOIN);

			Disjunction siteCpsCond = Restrictions.disjunction();
			for (Pair<Long, Long> siteCp : crit.siteCps()) {
				Junction siteCpCond = Restrictions.conjunction().add(Restrictions.eq("site.id", siteCp.first()));
				if (siteCp.second() != null) {
					siteCpCond.add(Restrictions.or(
						Restrictions.isNull("cp.id"),
						Restrictions.eq("cp.id", siteCp.second())
					));
				}

				siteCpsCond.add(siteCpCond);
			}

			innerQuery.add(siteCpsCond);
		}

		if (StringUtils.isNotBlank(crit.query())) {
			innerQuery.add(Restrictions.ilike("cont.name", crit.query(), crit.matchMode()));
		}

		return getCurrentSession().createCriteria(StorageContainer.class, "cont")
			.add(Subqueries.propertyIn("cont.id", detachedCriteria))
			.addOrder(Order.asc("cont.id"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	@Override
	public int deleteReservedPositions(List<String> reservationIds) {
		return getCurrentSession().getNamedQuery(DEL_POS_BY_RSV_ID)
			.setParameterList("reservationIds", reservationIds)
			.executeUpdate();
	}

	@Override
	public int deleteReservedPositionsOlderThan(Date expireTime) {
		return getCurrentSession().getNamedQuery(DEL_EXPIRED_RSV_POS)
			.setTimestamp("expireTime", expireTime)
			.executeUpdate();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getLeafContainerIds(Long containerId, int startAt, int maxContainers) {
		return getCurrentSession().getNamedQuery(GET_LEAF_CONTAINERS)
			.setParameter("containerId", containerId)
			.setFirstResult(startAt)
			.setMaxResults(maxContainers)
			.list();
	}

	//
	// [ { containerName : [ specimen labels ] } ]
	//
	@Override
	public Map<String, List<String>> getInaccessibleSpecimens(
		List<Long> containerIds, List<Pair<Long, Long>> siteCps,
		boolean useMrnSites, int firstN) {

		DetachedCriteria validSpmnsQuery = DetachedCriteria.forClass(Specimen.class, "specimen")
			.setProjection(Projections.distinct(Projections.property("specimen.id")));

		Criteria query = validSpmnsQuery.getExecutableCriteria(getCurrentSession())
			.createAlias("specimen.position", "pos")
			.createAlias("pos.container", "container")
			.createAlias("container.ancestorContainers", "aContainer")
			.add(Restrictions.in("aContainer.id", containerIds));
		BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, siteCps, useMrnSites, "visit");
		return getInvalidSpecimens(containerIds, validSpmnsQuery, firstN);
	}

	@Override
	public Map<String, List<String>> getInvalidSpecimensForSite(List<Long> containerIds, Long siteId, int firstN) {
		DetachedCriteria validSpmnsQuery = DetachedCriteria.forClass(Specimen.class, "specimen")
			.setProjection(Projections.distinct(Projections.property("specimen.id")));
		validSpmnsQuery.getExecutableCriteria(getCurrentSession())
			.createAlias("specimen.position", "pos")
			.createAlias("pos.container", "container")
			.createAlias("container.ancestorContainers", "aContainer")
			.createAlias("specimen.collectionProtocol", "cp")
			.createAlias("cp.sites", "cpSite")
			.createAlias("cpSite.site", "site")
			.add(Restrictions.in("aContainer.id", containerIds))
			.add(Restrictions.eq("site.id", siteId));
		return getInvalidSpecimens(containerIds, validSpmnsQuery, firstN);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, List<Long>> getDescendantContainerIds(Collection<Long> containerIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_DESCENDANT_CONTAINER_IDS)
			.setParameterList("containerIds", containerIds)
			.list();

		Map<Long, List<Long>> result = new HashMap<>();
		for (Object[] row : rows) {
			Long ancestorId = (Long) row[0];
			List<Long> descendantIds = result.get(ancestorId);
			if (descendantIds == null) {
				descendantIds = new ArrayList<>();
				result.put(ancestorId, descendantIds);
			}
			descendantIds.add((Long) row[1]);
		}

		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<StorageContainer> getShippedContainers(Collection<Long> containerIds) {
		return getCurrentSession().getNamedQuery(GET_SHIPPED_CONTAINERS)
			.setParameterList("containerIds", containerIds)
			.list();
	}

	@SuppressWarnings("unchecked")
	private Map<String, List<String>> getInvalidSpecimens(List<Long> containerIds, DetachedCriteria validSpmnsQuery, int firstN) {
		List<Object[]> rows = getCurrentSession().createCriteria(Specimen.class, "specimen")
			.createAlias("specimen.position", "pos")
			.createAlias("pos.container", "container")
			.createAlias("container.ancestorContainers", "aContainer")
			.setProjection(Projections.projectionList()
				.add(Projections.property("specimen.label"))
				.add(Projections.property("container.name")))
			.add(Subqueries.propertyNotIn("specimen.id", validSpmnsQuery))
			.add(Restrictions.in("aContainer.id", containerIds))
			.addOrder(Order.asc("container.name"))
			.setMaxResults(firstN > 0 ? firstN : 5)
			.list();

		Map<String, List<String>> result = new HashMap<>();
		for (Object[] row : rows) {
			List<String> spmnLabels = result.get((String) row[1]);
			if (spmnLabels == null) {
				spmnLabels = new ArrayList<>();
				result.put((String) row[1], spmnLabels);
			}

			spmnLabels.add((String) row[0]);
		}

		return result;
	}

	private StorageContainerSummary createContainer(Object[] row, Integer noOfColumns) {
		int idx = 0;

		StorageContainerSummary container = new StorageContainerSummary();
		container.setId((Long)row[idx++]);
		container.setName((String)row[idx++]);
		container.setNoOfRows((Integer)row[idx++]);
		container.setNoOfColumns((Integer)row[idx++]);

		if (row[idx] != null) {
			StorageLocationSummary location = new StorageLocationSummary();
			location.setId((Long)row[idx++]);

			if (row[idx] != null) {
				//
				// if not stored in dimensionless container
				//
				int rowNo = (Integer)row[idx++];
				int colNo = (Integer)row[idx++];
				location.setPosition((rowNo - 1) * noOfColumns + colNo);
			}

			container.setStorageLocation(location);
		}

		return container;
	}

	private void linkParentChildContainers(Map<Long, StorageContainerSummary> containersMap) {
		for (StorageContainerSummary container : containersMap.values()) {
			StorageLocationSummary location = container.getStorageLocation();
			if (location == null) {
				// root container
				continue;
			}

			StorageContainerSummary parent = containersMap.get(location.getId());

			//
			// Get back actual position value based on parent container dimension
			//
			if (location.getPosition() != null) {
				int rowNo = (location.getPosition() - 1) / 10000 + 1, colNo = (location.getPosition() - 1) % 10000 + 1;
				location.setPosition((rowNo - 1) * parent.getNoOfColumns() + colNo);
			}

			if (parent.getChildContainers() == null) {
				parent.setChildContainers(new ArrayList<>());
			}

			parent.getChildContainers().add(container);
		}
	}

	private StorageContainerSummary sortChildContainers(StorageContainerSummary container) {
		if (CollectionUtils.isEmpty(container.getChildContainers())) {
			return container;
		}

		Collections.sort(container.getChildContainers(), this::comparePositions);
		container.getChildContainers().forEach(this::sortChildContainers);
		return container;
	}

	private int comparePositions(StorageContainerSummary s1, StorageContainerSummary s2) {
		return ObjectUtils.compare(s1.getStorageLocation().getPosition(), s2.getStorageLocation().getPosition());
	}

	private DetachedCriteria getContainerSpecimensListQuery(SpecimenListCriteria crit) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Specimen.class, "specimen")
			.setProjection(Projections.distinct(Projections.property("specimen.id")));
		Criteria query = detachedCriteria.getExecutableCriteria(getCurrentSession());


		query.createAlias("specimen.position", "pos")
			.createAlias("pos.container", "container")
			.createAlias("container.ancestorContainers", "anscestor")
			.add(Restrictions.eq("anscestor.id", crit.ancestorContainerId()));

		if (crit.containerId() != null) {
			query.add(Restrictions.eq("container.id", crit.containerId()));
		} else if (StringUtils.isNotBlank(crit.container())) {
			query.add(Restrictions.eq("container.name", crit.container()));
		}

		if (StringUtils.isNotBlank(crit.type())) {
			query.add(Restrictions.eq("specimen.specimenType", crit.type()));
		}

		if (StringUtils.isNotBlank(crit.anatomicSite())) {
			query.add(Restrictions.eq("specimen.tissueSite", crit.anatomicSite()));
		}

		String startAlias = "visit";
		if (StringUtils.isNotBlank(crit.ppid()) || crit.cpId() != null || StringUtils.isNotBlank(crit.cpShortTitle())) {
			query.createAlias("specimen.visit", "visit")
				.createAlias("visit.registration", "cpr");

			startAlias = "cp";

			if (StringUtils.isNotBlank(crit.ppid())) {
				query.add(Restrictions.ilike("cpr.ppid", crit.ppid(), crit.matchMode()));
			}

			if (crit.cpId() != null || StringUtils.isNotBlank(crit.cpShortTitle())) {
				query.createAlias("cpr.collectionProtocol", "cp");

				if (crit.cpId() != null) {
					query.add(Restrictions.eq("cp.id", crit.cpId()));
				} else {
					query.add(Restrictions.eq("cp.shortTitle", crit.cpShortTitle()));
				}

				startAlias = "cpSite";
			}
		}

		BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit.siteCps(), crit.useMrnSites(), startAlias);
		return detachedCriteria;
	}

	private class ListQueryBuilder {
		private StorageContainerListCriteria crit;
		
		private StringBuilder select = new StringBuilder();
		
		private StringBuilder from = new StringBuilder();
		
		private StringBuilder where = new StringBuilder();
		
		private Map<String, Object> params = new HashMap<String, Object>();
		
		public ListQueryBuilder(StorageContainerListCriteria crit) {
			prepareQuery(crit, false);
		}
		
		public ListQueryBuilder(StorageContainerListCriteria crit, boolean countReq) {
			prepareQuery(crit, countReq);
		}

		public Query query() {
			addIdsRestriction();
			addNameRestriction();		
			addSiteRestriction();
					
			addFreeContainersRestriction();
			addSpecimenRestriction();
			addCpRestriction();
			addStoreSpecimenRestriction();
			
			addParentRestriction();
			addCanHoldRestriction();

			// permissions check
			addSiteCpRestriction();
			
			String hql = new StringBuilder(select)
				.append(" ").append(from)
				.append(" ").append(where)
				.append(" order by c.id asc")
				.toString();
			
			Query query = getCurrentSession().createQuery(hql);
			for (Map.Entry<String, Object> param : params.entrySet()) {
				if (param.getValue() instanceof Collection<?>) {
					query.setParameterList(param.getKey(), (Collection<?>)param.getValue());
				} else {
					query.setParameter(param.getKey(), param.getValue());
				}				
			}
			
			return query;
		}
		
		private void prepareQuery(StorageContainerListCriteria crit, boolean countReq) {
			this.crit = crit;

			select = new StringBuilder(countReq ? "select count(distinct c.id)" : "select distinct c");
			if (crit.hierarchical()) {
				from = new StringBuilder("from ").append(getType().getName()).append(" c join c.descendentContainers dc");
				where = new StringBuilder("where dc.activityStatus = :activityStatus");
			} else {
				from = new StringBuilder("from ").append(getType().getName()).append(" c");
				where = new StringBuilder("where c.activityStatus = :activityStatus");						
			}

			from.append(countReq ? " left join c.position pos " : " left join fetch c.position pos ");
			params.put("activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		}
		
		private void addAnd() {
			if (where.length() != 0) {
				where.append(" and ");
			}
		}

		private void addIdsRestriction() {
			if (CollectionUtils.isEmpty(crit.ids())) {
				return;
			}

			addAnd();
			where.append("c.id in :ids");
			params.put("ids", crit.ids());
		}

		private void addNameRestriction() {
			if (StringUtils.isNotBlank(crit.query())) {
				addAnd();
				where.append("upper(c.name) like :name");
				params.put("name", "%" + crit.query().toUpperCase() + "%");
			}

			if (CollectionUtils.isNotEmpty(crit.names())) {
				addAnd();
				where.append("c.name in (:names)");
				params.put("names", crit.names());
			}
		}

		private void addFreeContainersRestriction() {			
			if (!crit.onlyFreeContainers()) {
				return;
			}

			addAnd();
			if (crit.hierarchical()) {
				where.append("((dc.noOfRows is null and dc.noOfColumns is null)")
					.append("or")
					.append("(size(dc.occupiedPositions) - dc.noOfRows * dc.noOfColumns < 0))");
			} else {
				where.append("((c.noOfRows is null and c.noOfColumns is null)")
					.append("or")
					.append("(size(c.occupiedPositions) - c.noOfRows * c.noOfColumns < 0))");
			}
		}

		private void addSiteAlias() {
			from.append(" join c.site site");
		}

		private void addSiteRestriction() {
			if (StringUtils.isBlank(crit.siteName())) {
				return;
			}
			
			addSiteAlias();

			addAnd();
			where.append("site.name = :siteName");
			params.put("siteName", crit.siteName());
		}

		private void addParentRestriction() {
			if (!crit.topLevelContainers() && crit.parentContainerId() == null) {
				return;
			}
			
			if (crit.topLevelContainers()) {
				from.append(" left join c.parentContainer pc");			
			} else if (crit.parentContainerId() != null) {
				from.append(" join c.parentContainer pc");
			} 

			addAnd();
			Long parentId = crit.parentContainerId();
			if (parentId == null) {
				where.append("pc is null");
			} else {
				where.append("pc.id = :parentId");
				params.put("parentId", parentId);						
			}
		}

		private void addCanHoldRestriction() {
			if (StringUtils.isBlank(crit.canHold())) {
				return;
			}

			from.append(" left join c.type type");
			from.append(" left join type.canHold canHold");

			addAnd();
			where.append("(type is null or canHold.name = :canHold)");
			params.put("canHold", crit.canHold());
		}

		private void addSpecimenRestriction() {			
			String specimenClass = crit.specimenClass(), specimenType = crit.specimenType();			
			if (StringUtils.isBlank(specimenClass) && StringUtils.isBlank(specimenType)) {
				return;
			}
			
			addAnd();
			if (crit.hierarchical()) {
				where.append("(:specimenClass in elements(dc.compAllowedSpecimenClasses)")
				.append(" or ")
				.append(":specimenType in elements(dc.compAllowedSpecimenTypes)")
				.append(")");				
			} else {
				where.append("(:specimenClass in elements(c.compAllowedSpecimenClasses)")
				.append(" or ")
				.append(":specimenType in elements(c.compAllowedSpecimenTypes)")
				.append(")");				
			}
			
			params.put("specimenClass", specimenClass);
			params.put("specimenType", specimenType);
		}

		private void addCpAlias() {
			if (crit.hierarchical()) {
				from.append(" left join dc.compAllowedCps cp");
			} else {
				from.append(" left join c.compAllowedCps cp");
			}
		}

		private void addCpRestriction() {
			if (CollectionUtils.isEmpty(crit.cpIds()) && CollectionUtils.isEmpty(crit.cpShortTitles())) {
				return;
			}

			addCpAlias();

			addAnd();

			if (CollectionUtils.isNotEmpty(crit.cpIds())) {
				where.append("(cp is null or cp.id in (:cpIds))");
				params.put("cpIds", crit.cpIds());
			} else {
				where.append("(cp is null or cp.shortTitle in (:cpShortTitles))");
				params.put("cpShortTitles", crit.cpShortTitles());
			}
		}	
		
		private void addStoreSpecimenRestriction() {			
			if (crit.storeSpecimensEnabled() == null) {
				return;
			}

			addAnd();
			if (crit.hierarchical()) {
				where.append("dc.storeSpecimenEnabled = :storeSpecimenEnabled");
			} else {
				where.append("c.storeSpecimenEnabled = :storeSpecimenEnabled");
			}
			
			params.put("storeSpecimenEnabled", crit.storeSpecimensEnabled());
		}

		private void addSiteCpRestriction() {
			if (CollectionUtils.isEmpty(crit.siteCps())) {
				return;
			}

			if (StringUtils.isBlank(crit.siteName())) {
				addSiteAlias();
			}

			if (CollectionUtils.isEmpty(crit.cpIds()) && CollectionUtils.isEmpty(crit.cpShortTitles())) {
				addCpAlias();
			}

			List<String> disjunctions = new ArrayList<>();
			for (Pair<Long, Long> siteCp : crit.siteCps()) {
				StringBuilder restriction = new StringBuilder("(site.id = ").append(siteCp.first());
				if (siteCp.second() != null) {
					restriction.append(" and (")
						.append("cp.id is null or ")
						.append("cp.id = ").append(siteCp.second())
						.append(")");
				}

				restriction.append(")");
				disjunctions.add(restriction.toString());
			}

			addAnd();
			where.append("(").append(StringUtils.join(disjunctions, " or ")).append(")");
		}
	}


	private static final String FQN = StorageContainer.class.getName();

	private static final String GET_SPECIMENS_COUNT = FQN + ".getSpecimensCount";

	private static final String GET_ANCESTORS = FQN + ".getAncestors";

	private static final String GET_ROOT_AND_CHILD_CONTAINERS = FQN + ".getRootAndChildContainers";

	private static final String GET_CHILD_CONTAINERS = FQN + ".getChildContainers";

	private static final String DEL_POS_BY_RSV_ID = FQN + ".deletePositionsByReservationIds";

	private static final String DEL_EXPIRED_RSV_POS = FQN + ".deleteReservationsOlderThanTime";

	private static final String GET_ROOT_CONT_SPMNS_COUNT = FQN + ".getRootContainerSpecimensCount";

	private static final String GET_SPMNS_CNT_BY_TYPE = FQN + ".getSpecimenCountsByType";

	private static final String GET_LEAF_CONTAINERS = FQN + ".getLeafContainerIds";

	private static final String GET_DESCENDANT_CONTAINER_IDS = FQN + ".getDescendantContainerIds";

	private static final String GET_SHIPPED_CONTAINERS = FQN + ".getShippedContainers";
}
