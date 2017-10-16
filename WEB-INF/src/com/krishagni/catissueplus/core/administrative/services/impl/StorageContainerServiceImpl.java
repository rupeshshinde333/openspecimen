package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.ContainerType;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerFactory;
import com.krishagni.catissueplus.core.administrative.events.ContainerCriteria;
import com.krishagni.catissueplus.core.administrative.events.ContainerHierarchyDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerQueryCriteria;
import com.krishagni.catissueplus.core.administrative.events.ContainerReplicationDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerReplicationDetail.DestinationDetail;
import com.krishagni.catissueplus.core.administrative.events.PositionsDetail;
import com.krishagni.catissueplus.core.administrative.events.ReservePositionsOp;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerPositionDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.administrative.events.TenantDetail;
import com.krishagni.catissueplus.core.administrative.events.VacantPositionsOp;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerMapExporter;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionRule;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategy;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategyFactory;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTaskManager;
import com.krishagni.catissueplus.core.administrative.services.StorageContainerService;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenResolver;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.RollbackTransaction;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.service.ObjectStateParamsResolver;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.Filter;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.query.WideRowMode;

public class StorageContainerServiceImpl implements StorageContainerService, ObjectStateParamsResolver, InitializingBean {
	private static final Log logger = LogFactory.getLog(StorageContainerServiceImpl.class);

	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;
	
	private StorageContainerFactory containerFactory;
	
	private ContainerMapExporter mapExporter;

	private LabelGenerator nameGenerator;

	private SpecimenResolver specimenResolver;

	private ContainerSelectionStrategyFactory selectionStrategyFactory;

	private ScheduledTaskManager taskManager;

	private QueryService querySvc;

	private ExportService exportSvc;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public StorageContainerFactory getContainerFactory() {
		return containerFactory;
	}

	public void setContainerFactory(StorageContainerFactory containerFactory) {
		this.containerFactory = containerFactory;
	}
	
	public void setMapExporter(ContainerMapExporter mapExporter) {
		this.mapExporter = mapExporter;
	}

	public void setNameGenerator(LabelGenerator nameGenerator) {
		this.nameGenerator = nameGenerator;
	}

	public void setSpecimenResolver(SpecimenResolver specimenResolver) {
		this.specimenResolver = specimenResolver;
	}

	public void setSelectionStrategyFactory(ContainerSelectionStrategyFactory selectionStrategyFactory) {
		this.selectionStrategyFactory = selectionStrategyFactory;
	}

	public void setTaskManager(ScheduledTaskManager taskManager) {
		this.taskManager = taskManager;
	}

	public void setQuerySvc(QueryService querySvc) {
		this.querySvc = querySvc;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> getStorageContainers(RequestEvent<StorageContainerListCriteria> req) {
		try {			
			StorageContainerListCriteria crit = addContainerListCriteria(req.getPayload());
			List<StorageContainer> containers = daoFactory.getStorageContainerDao().getStorageContainers(crit);
			List<StorageContainerSummary> result = StorageContainerSummary.from(containers, crit.includeChildren());
			setStoredSpecimensCount(crit, result);
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getStorageContainersCount(RequestEvent<StorageContainerListCriteria> req) {
		try {
			StorageContainerListCriteria crit = addContainerListCriteria(req.getPayload());
			return ResponseEvent.response(daoFactory.getStorageContainerDao().getStorageContainersCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerDetail> getStorageContainer(RequestEvent<ContainerQueryCriteria> req) {
		try {		
			StorageContainer container = getContainer(req.getPayload());						
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

			StorageContainerDetail detail = StorageContainerDetail.from(container);
			if (req.getPayload().includeStats()) {
				detail.setSpecimensByType(daoFactory.getStorageContainerDao().getSpecimensCountByType(detail.getId()));
			}

			return ResponseEvent.response(detail);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerPositionDetail>> getOccupiedPositions(RequestEvent<Long> req) {
		try {
			StorageContainer container = getContainer(req.getPayload(), null);
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
			return ResponseEvent.response(StorageContainerPositionDetail.from(container.getOccupiedPositions()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> getSpecimens(RequestEvent<SpecimenListCriteria> req) {
		StorageContainer container = getContainer(req.getPayload().ancestorContainerId(), null);
		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
		SpecimenListCriteria crit = addSiteCpRestrictions(req.getPayload(), container);

		List<Specimen> specimens = daoFactory.getStorageContainerDao().getSpecimens(crit, !container.isDimensionless());
		return ResponseEvent.response(SpecimenInfo.from(specimens));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getSpecimensCount(RequestEvent<SpecimenListCriteria> req) {
		StorageContainer container = getContainer(req.getPayload().ancestorContainerId(), null);
		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
		SpecimenListCriteria crit = addSiteCpRestrictions(req.getPayload(), container);

		Long count = daoFactory.getStorageContainerDao().getSpecimensCount(crit);
		return ResponseEvent.response(count);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> getSpecimensReport(RequestEvent<ContainerQueryCriteria> req) {
		ContainerQueryCriteria crit = req.getPayload();
		StorageContainer container = getContainer(crit.getId(), crit.getName());
		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

		Integer queryId = ConfigUtil.getInstance().getIntSetting("common", "cont_spmns_report_query", -1);
		if (queryId == -1) {
			return ResponseEvent.userError(StorageContainerErrorCode.SPMNS_RPT_NOT_CONFIGURED);
		}

		SavedQuery query = deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
		if (query == null) {
			return ResponseEvent.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
		}

		return new ResponseEvent<>(exportResult(container, query));
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerDetail> createStorageContainer(RequestEvent<StorageContainerDetail> req) {
		try {
			return ResponseEvent.response(StorageContainerDetail.from(createStorageContainer(null, req.getPayload())));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerDetail> updateStorageContainer(RequestEvent<StorageContainerDetail> req) {
		return updateStorageContainer(req, false);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerDetail> patchStorageContainer(RequestEvent<StorageContainerDetail> req) {
		return updateStorageContainer(req, true);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> isAllowed(RequestEvent<TenantDetail> req) {
		try {
			TenantDetail detail = req.getPayload();

			StorageContainer container = getContainer(detail.getContainerId(), detail.getContainerName());
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
			
			CollectionProtocol cp = new CollectionProtocol();
			cp.setId(detail.getCpId());
			String specimenClass = detail.getSpecimenClass();
			String type = detail.getSpecimenType();
			boolean isAllowed = container.canContainSpecimen(cp, specimenClass, type);

			if (!isAllowed) {
				return ResponseEvent.userError(
						StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN, 
						container.getName(), 
						Specimen.getDesc(specimenClass, type));
			} else {
				return ResponseEvent.response(isAllowed);
			}
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ExportedFileDetail> exportMap(RequestEvent<ContainerQueryCriteria> req) {
		try {
			StorageContainer container = getContainer(req.getPayload());						
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

			if (container.isDimensionless()) {
				return ResponseEvent.userError(StorageContainerErrorCode.DIMLESS_NO_MAP, container.getName());
			}

			File file = mapExporter.exportToFile(container);
			return ResponseEvent.response(new ExportedFileDetail(container.getName(), file));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerPositionDetail>> assignPositions(RequestEvent<PositionsDetail> req) {
		try {
			PositionsDetail op = req.getPayload();
			StorageContainer container = getContainer(op.getContainerId(), op.getContainerName());
			
			List<StorageContainerPosition> positions = op.getPositions().stream()
				.map(posDetail -> createPosition(container, posDetail, op.getVacateOccupant()))
				.collect(Collectors.toList());

			container.assignPositions(positions, op.getVacateOccupant());
			daoFactory.getStorageContainerDao().saveOrUpdate(container, true);
			return ResponseEvent.response(StorageContainerPositionDetail.from(container.getOccupiedPositions()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req) {
		try {
			StorageContainer existing = getContainer(req.getPayload(), null);
			return ResponseEvent.response(existing.getDependentEntities());
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> deleteStorageContainers(RequestEvent<BulkDeleteEntityOp> req) {
		try {
			Set<Long> containerIds = req.getPayload().getIds();
			List<StorageContainer> containers = daoFactory.getStorageContainerDao().getByIds(containerIds);
			if (containerIds.size() != containers.size()) {
				containers.forEach(container -> containerIds.remove(container.getId()));
				throw OpenSpecimenException.userError(StorageContainerErrorCode.NOT_FOUND, containerIds, containerIds.size());
			}

			List<StorageContainer> ancestors = getAncestors(containers);
			ancestors.forEach(AccessCtrlMgr.getInstance()::ensureDeleteContainerRights);
			ancestors.forEach(StorageContainer::delete);

			//
			// returning summary of all containers given by user instead of only ancestor containers
			//
			return ResponseEvent.response(StorageContainerSummary.from(containers));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> replicateStorageContainer(RequestEvent<ContainerReplicationDetail> req) {
		try {
			ContainerReplicationDetail replDetail = req.getPayload();
			StorageContainer srcContainer = getContainer(
					replDetail.getSourceContainerId(), 
					replDetail.getSourceContainerName(),
					null,
					StorageContainerErrorCode.SRC_ID_OR_NAME_REQ);
			
			for (DestinationDetail dest : replDetail.getDestinations()) {
				replicateContainer(srcContainer, dest);
			}

			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> createContainerHierarchy(RequestEvent<ContainerHierarchyDetail> req) {
		ContainerHierarchyDetail input = req.getPayload();
		List<StorageContainer> containers = new ArrayList<>();

		try {
			StorageContainer container = containerFactory.createStorageContainer("dummyName", input);
			AccessCtrlMgr.getInstance().ensureCreateContainerRights(container);
			container.validateRestrictions();

			StorageContainer parentContainer = container.getParentContainer();
			if (parentContainer != null && !parentContainer.hasFreePositionsForReservation(input.getNumOfContainers())) {
				return ResponseEvent.userError(StorageContainerErrorCode.NO_FREE_SPACE, parentContainer.getName());
			}

			boolean setCapacity = true;
			for (int i = 1; i <= input.getNumOfContainers(); i++) {
				StorageContainer cloned = null;
				if (i == 1) {
					cloned = container;
				} else {
					cloned = container.copy();
					setPosition(cloned);
				}

				generateName(cloned);
				ensureUniqueConstraints(null, cloned);

				if (cloned.isStoreSpecimenEnabled() && setCapacity) {
					cloned.setFreezerCapacity();
					setCapacity = false;
				}

				createContainerHierarchy(cloned.getType().getCanHold(), cloned);
				daoFactory.getStorageContainerDao().saveOrUpdate(cloned);
				containers.add(cloned);
			}
			
			return ResponseEvent.response(StorageContainerDetail.from(containers));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> createMultipleContainers(RequestEvent<List<StorageContainerDetail>> req) {
		try {
			List<StorageContainerSummary> result = new ArrayList<>();

			for (StorageContainerDetail detail : req.getPayload()) {
				if (StringUtils.isNotBlank(detail.getTypeName()) || detail.getTypeId() != null) {
					detail.setName("dummy");
				}

				StorageContainer container = containerFactory.createStorageContainer(detail);
				AccessCtrlMgr.getInstance().ensureCreateContainerRights(container);
				if (container.getType() != null) {
					generateName(container);
				}

				ensureUniqueConstraints(null, container);
				container.validateRestrictions();
				daoFactory.getStorageContainerDao().saveOrUpdate(container);
				result.add(StorageContainerSummary.from(container));
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerPositionDetail>> blockPositions(RequestEvent<PositionsDetail> req) {
		try {
			PositionsDetail opDetail = req.getPayload();
			if (CollectionUtils.isEmpty(opDetail.getPositions())) {
				return ResponseEvent.response(Collections.emptyList());
			}

			StorageContainer container = getContainer(opDetail.getContainerId(), opDetail.getContainerName());
			AccessCtrlMgr.getInstance().ensureUpdateContainerRights(container);
			if (container.isDimensionless()) {
				return ResponseEvent.userError(StorageContainerErrorCode.DL_POS_BLK_NP, container.getName());
			}

			List<StorageContainerPosition> positions = opDetail.getPositions().stream()
				.map(detail -> container.createPosition(detail.getPosOne(), detail.getPosTwo()))
				.collect(Collectors.toList());

			List<StorageContainerPosition> blocked = container.blockPositions(positions);
			daoFactory.getStorageContainerDao().saveOrUpdate(container, true);
			return ResponseEvent.response(StorageContainerPositionDetail.from(blocked));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerPositionDetail>> unblockPositions(RequestEvent<PositionsDetail> req) {
		try {
			PositionsDetail opDetail = req.getPayload();
			if (CollectionUtils.isEmpty(opDetail.getPositions())) {
				return ResponseEvent.response(Collections.emptyList());
			}

			StorageContainer container = getContainer(opDetail.getContainerId(), opDetail.getContainerName());
			AccessCtrlMgr.getInstance().ensureUpdateContainerRights(container);
			if (container.isDimensionless()) {
				return ResponseEvent.userError(StorageContainerErrorCode.DL_POS_BLK_NP, container.getName());
			}

			List<StorageContainerPosition> positions = opDetail.getPositions().stream()
				.map(detail -> container.createPosition(detail.getPosOne(), detail.getPosTwo()))
				.collect(Collectors.toList());

			List<StorageContainerPosition> unblocked = container.unblockPositions(positions);
			daoFactory.getStorageContainerDao().saveOrUpdate(container, true);
			return ResponseEvent.response(StorageContainerPositionDetail.from(unblocked));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageLocationSummary>> reservePositions(RequestEvent<ReservePositionsOp> req) {
		long t1 = System.currentTimeMillis();
		try {
			ReservePositionsOp op = req.getPayload();
			if (StringUtils.isNotBlank(op.getReservationToCancel())) {
				cancelReservation(new RequestEvent<>(op.getReservationToCancel()));
			}

			Long cpId = op.getCpId();
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(cpId);
			if (cp == null) {
				throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, cpId);
			}

			if (StringUtils.isBlank(cp.getContainerSelectionStrategy())) {
				return ResponseEvent.response(Collections.emptyList());
			}

			ContainerSelectionStrategy strategy = selectionStrategyFactory.getStrategy(cp.getContainerSelectionStrategy());
			if (strategy == null) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.INV_CONT_SEL_STRATEGY, cp.getContainerSelectionStrategy());
			}

			Set<Pair<Long, Long>> allowedSiteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps(cpId);
			if (allowedSiteCps != null && allowedSiteCps.isEmpty()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			Set<Pair<Long, Long>> reqSiteCps = getRequiredSiteCps(allowedSiteCps, Collections.singleton(cpId));
			if (CollectionUtils.isEmpty(reqSiteCps)) {
				return ResponseEvent.response(Collections.emptyList());
			}

			String reservationId = StorageContainer.getReservationId();
			Date reservationTime = Calendar.getInstance().getTime();
			List<StorageContainerPosition> reservedPositions = new ArrayList<>();
			for (ContainerCriteria criteria : op.getCriteria()) {
				criteria.siteCps(reqSiteCps);

				if (StringUtils.isNotBlank(criteria.ruleName())) {
					ContainerSelectionRule rule = selectionStrategyFactory.getRule(criteria.ruleName());
					if (rule == null) {
						throw OpenSpecimenException.userError(StorageContainerErrorCode.INV_CONT_SEL_RULE, criteria.ruleName());
					}

					criteria.rule(rule);
				}

				boolean allAllocated = false;
				while (!allAllocated) {
					long t2 = System.currentTimeMillis();
					StorageContainer container = strategy.getContainer(criteria, cp.getAliquotsInSameContainer());
					if (container == null) {
						ResponseEvent<List<StorageLocationSummary>> resp = new ResponseEvent<>(Collections.emptyList());
						resp.setRollback(true);
						return resp;
					}

					int numPositions = criteria.minFreePositions();
					if (numPositions <= 0) {
						numPositions = 1;
					}

					List<StorageContainerPosition> positions = container.reservePositions(reservationId, reservationTime, numPositions);
					numPositions -= positions.size();
					if (numPositions == 0) {
						allAllocated = true;
					} else {
						criteria.minFreePositions(numPositions);
					}

					System.err.println("***** Allocation time: " + (System.currentTimeMillis() - t2) + " ms");
				}
			}

			return ResponseEvent.response(StorageLocationSummary.from(reservedPositions));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			System.err.println("***** Call time: " + (System.currentTimeMillis() - t1) + " ms");
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> cancelReservation(RequestEvent<String> req) {
		try {
			int vacatedPositions = daoFactory.getStorageContainerDao()
				.deleteReservedPositions(Collections.singletonList(req.getPayload()));
			return ResponseEvent.response(vacatedPositions);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<StorageContainerSummary> getAncestorsHierarchy(RequestEvent<ContainerQueryCriteria> req) {
		try {
			StorageContainer container = getContainer(req.getPayload());
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

			StorageContainerSummary summary = null;
			if (container.getParentContainer() == null) {
				summary = new StorageContainerSummary();
				summary.setId(container.getId());
				summary.setName(container.getName());
				summary.setNoOfRows(container.getNoOfRows());
				summary.setNoOfColumns(container.getNoOfColumns());
			} else {
				summary = daoFactory.getStorageContainerDao().getAncestorsHierarchy(container.getId());
			}

			return ResponseEvent.response(summary);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> getChildContainers(RequestEvent<ContainerQueryCriteria> req) {
		try {
			StorageContainer container = getContainer(req.getPayload());
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

			if (container.isDimensionless()) {
				return ResponseEvent.response(Collections.emptyList());
			} else {
				return ResponseEvent.response(daoFactory.getStorageContainerDao()
					.getChildContainers(container.getId(), container.getNoOfColumns()));
			}
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<StorageContainerSummary>> getDescendantContainers(RequestEvent<StorageContainerListCriteria> req) {
		StorageContainer container = getContainer(req.getPayload().parentContainerId(), null);
		AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
		List<StorageContainer> containers = daoFactory.getStorageContainerDao().getDescendantContainers(req.getPayload());
		return ResponseEvent.response(StorageContainerSummary.from(containers));
	}

	@Override
	@RollbackTransaction
	public ResponseEvent<List<StorageLocationSummary>> getVacantPositions(RequestEvent<VacantPositionsOp> req) {
		try {
			VacantPositionsOp detail = req.getPayload();
			StorageContainer container = getContainer(detail.getContainerId(), detail.getContainerName());
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);

			int numPositions = detail.getRequestedPositions();
			if (numPositions <= 0) {
				numPositions = 1;
			}

			List<StorageContainerPosition> vacantPositions = new ArrayList<>();
			for (int i = 0; i < numPositions; ++i) {
				StorageContainerPosition position = null;
				if (i == 0) {
					if (StringUtils.isNotBlank(detail.getStartRow()) && StringUtils.isNotBlank(detail.getStartColumn())) {
						position = container.nextAvailablePosition(detail.getStartRow(), detail.getStartColumn());
					} else if (detail.getStartPosition() > 0) {
						position = container.nextAvailablePosition(detail.getStartPosition());
					} else {
						position = container.nextAvailablePosition();
					}
				} else {
					position = container.nextAvailablePosition(true);
				}

				if (position == null) {
					throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
				}

				container.addPosition(position);
				vacantPositions.add(position);
			}

			return ResponseEvent.response(
				vacantPositions.stream().map(StorageLocationSummary::from).collect(Collectors.toList()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public StorageContainer createStorageContainer(StorageContainer base, StorageContainerDetail input) {
		StorageContainer container = containerFactory.createStorageContainer(base, input);
		AccessCtrlMgr.getInstance().ensureCreateContainerRights(container);

		ensureUniqueConstraints(null, container);
		container.validateRestrictions();

		if (container.isStoreSpecimenEnabled()) {
			container.setFreezerCapacity();
		}

		if (container.getPosition() != null) {
			container.getPosition().occupy();
		}

		daoFactory.getStorageContainerDao().saveOrUpdate(container, true);
		return container;
	}

	@Override
	public String getObjectName() {
		return "container";
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolve(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getStorageContainerDao().getContainerIds(key, value);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		taskManager.scheduleWithFixedDelay(
			new Runnable() {
				@Override
				@PlusTransactional
				public void run() {
					try {
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.MINUTE, -5);

						int count = daoFactory.getStorageContainerDao().deleteReservedPositionsOlderThan(cal.getTime());
						if (count > 0) {
							logger.info(String.format("Cleaned up %d stale container slot reservations", count));
						}
					} catch (Exception e) {
						logger.error("Error deleting older reserved container slots", e);
					}
				}
			}, 5
		);

		exportSvc.registerObjectsGenerator("storageContainer", this::getContainersGenerator);
	}

	private StorageContainerListCriteria addContainerListCriteria(StorageContainerListCriteria crit) {
		Set<Pair<Long, Long>> allowedSiteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
		if (allowedSiteCps != null && allowedSiteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (CollectionUtils.isNotEmpty(crit.cpIds())) {
			allowedSiteCps = getRequiredSiteCps(allowedSiteCps, crit.cpIds());
			if (allowedSiteCps.isEmpty()) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
		}

		return crit.siteCps(allowedSiteCps);
	}

	private Set<Pair<Long, Long>> getRequiredSiteCps(Set<Pair<Long, Long>> allowedSiteCps, Set<Long> cpIds) {
		Set<Pair<Long, Long>> reqSiteCps = daoFactory.getCollectionProtocolDao().getSiteCps(cpIds);
		if (allowedSiteCps == null) {
			allowedSiteCps = reqSiteCps;
		} else {
			allowedSiteCps = getSiteCps(allowedSiteCps, reqSiteCps);
		}

		return allowedSiteCps;
	}

	private Set<Pair<Long, Long>> getSiteCps(Set<Pair<Long, Long>> allowed, Set<Pair<Long, Long>> required) {
		Set<Pair<Long, Long>> result = new HashSet<>();
		for (Pair<Long, Long> reqSiteCp : required) {
			for (Pair<Long, Long> allowedSiteCp : allowed) {
				if (!allowedSiteCp.first().equals(reqSiteCp.first())) {
					continue;
				}

				if (allowedSiteCp.second() != null && !allowedSiteCp.second().equals(reqSiteCp.second())) {
					continue;
				}

				result.add(reqSiteCp);
			}
		}

		return result;
	}

	private void setStoredSpecimensCount(StorageContainerListCriteria crit, List<StorageContainerSummary> containers) {
		if (!crit.includeStat() || !crit.topLevelContainers()) {
			return;
		}

		List<Long> containerIds = containers.stream().map(StorageContainerSummary::getId).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(containerIds)) {
			return;
		}

		Map<Long, Integer> countMap = daoFactory.getStorageContainerDao().getRootContainerSpecimensCount(containerIds);
		containers.forEach(c -> c.setStoredSpecimens(countMap.get(c.getId())));
	}

	private StorageContainer getContainer(ContainerQueryCriteria crit) {
		return getContainer(crit.getId(), crit.getName(), crit.getBarcode());
	}

	private StorageContainer getContainer(Long id, String name) {
		return getContainer(id, name, null);
	}
	
	private StorageContainer getContainer(Long id, String name, String barcode) {
		return getContainer(id, name, barcode, StorageContainerErrorCode.ID_NAME_OR_BARCODE_REQ);
	}
	
	private StorageContainer getContainer(Long id, String name, String barcode, ErrorCode requiredErrCode) {
		StorageContainer container = null;
		Object key = null;

		if (id != null) {
			container = daoFactory.getStorageContainerDao().getById(id);
			key = id;
		} else {
			if (StringUtils.isNotBlank(name)) {
				container = daoFactory.getStorageContainerDao().getByName(name);
				key = name;
			}

			if (container == null && StringUtils.isNotBlank(barcode)) {
				container = daoFactory.getStorageContainerDao().getByBarcode(barcode);
				key = barcode;
			}
		}

		if (key == null) {
			throw OpenSpecimenException.userError(requiredErrCode);
		} else if (container == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NOT_FOUND, key, 1);
		}

		return container;
	}

	private ResponseEvent<StorageContainerDetail> updateStorageContainer(RequestEvent<StorageContainerDetail> req, boolean partial) {
		try {
			StorageContainerDetail input = req.getPayload();			
			StorageContainer existing = getContainer(input.getId(), input.getName());
			AccessCtrlMgr.getInstance().ensureUpdateContainerRights(existing);			
			
			input.setId(existing.getId());
			StorageContainer container = null;
			if (partial) {
				container = containerFactory.createStorageContainer(existing, input);
			} else {
				container = containerFactory.createStorageContainer(input); 
			}
			
			ensureUniqueConstraints(existing, container);
			existing.update(container);			
			daoFactory.getStorageContainerDao().saveOrUpdate(existing, true);
			return ResponseEvent.response(StorageContainerDetail.from(existing));			
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}
	
	private void ensureUniqueConstraints(StorageContainer existing, StorageContainer newContainer) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		if (!isUniqueName(existing, newContainer)) {
			ose.addError(StorageContainerErrorCode.DUP_NAME, newContainer.getName());
		}
		
		if (!isUniqueBarcode(existing, newContainer)) {
			ose.addError(StorageContainerErrorCode.DUP_BARCODE);
		}
		
		ose.checkAndThrow();
	}
	
	private boolean isUniqueName(StorageContainer existingContainer, StorageContainer newContainer) {
		if (existingContainer != null && existingContainer.getName().equals(newContainer.getName())) {
			return true;
		}
		
		return isUniqueName(newContainer.getName());
	}
		
	private boolean isUniqueName(String name) {
		StorageContainer container = daoFactory.getStorageContainerDao().getByName(name);
		return container == null;
	}
	
	private boolean isUniqueBarcode(StorageContainer existingContainer, StorageContainer newContainer) {
		if (StringUtils.isBlank(newContainer.getBarcode())) {
			return true;
		}
		
		if (existingContainer != null && newContainer.getBarcode().equals(existingContainer.getBarcode())) {
			return true;
		}
		
		StorageContainer container = daoFactory.getStorageContainerDao().getByBarcode(newContainer.getBarcode());
		return container == null;
	}
	
	private StorageContainerPosition createPosition(StorageContainer container, StorageContainerPositionDetail pos, boolean vacateOccupant) {
		if (StringUtils.isBlank(pos.getPosOne()) ^ StringUtils.isBlank(pos.getPosTwo())) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.INV_POS, container.getName(), pos.getPosOne(), pos.getPosTwo());
		}
		
		String entityType = pos.getOccuypingEntity();
		if (StringUtils.isBlank(entityType)) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.INVALID_ENTITY_TYPE, "none");
		}
		
		if (StringUtils.isBlank(pos.getOccupyingEntityName()) && pos.getOccupyingEntityId() == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.OCCUPYING_ENTITY_ID_OR_NAME_REQUIRED);
		}
		
		if (entityType.equalsIgnoreCase("specimen")) {
			return createSpecimenPosition(container, pos, vacateOccupant);
		} else if (entityType.equalsIgnoreCase("container")) {
			return createChildContainerPosition(container, pos);
		}
		
		throw OpenSpecimenException.userError(StorageContainerErrorCode.INVALID_ENTITY_TYPE, entityType);
	}
	
	private StorageContainerPosition createSpecimenPosition(
			StorageContainer container,
			StorageContainerPositionDetail pos,
			boolean vacateOccupant) {


		Specimen specimen = getSpecimen(pos);
		AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(specimen, false);
		
		StorageContainerPosition position = null;
		if (!container.isDimensionless() && (StringUtils.isBlank(pos.getPosOne()) || StringUtils.isBlank(pos.getPosTwo()))) {
			position = new StorageContainerPosition();
			position.setOccupyingSpecimen(specimen);
			return position;
		}

		if (!container.canContain(specimen)) {
			throw OpenSpecimenException.userError(
				StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN, container.getName(), specimen.getLabelOrDesc());
		}
		
		if (!container.canSpecimenOccupyPosition(specimen.getId(), pos.getPosOne(), pos.getPosTwo(), vacateOccupant)) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
		}
		
		position = container.createPosition(pos.getPosOne(), pos.getPosTwo());
		position.setOccupyingSpecimen(specimen);
		return position;		
	}

	private Specimen getSpecimen(StorageContainerPositionDetail pos) {
		return specimenResolver.getSpecimen(
			pos.getOccupyingEntityId(),
			pos.getCpShortTitle(),
			pos.getOccupyingEntityName()
		);
	}

	private SpecimenListCriteria addSiteCpRestrictions(SpecimenListCriteria crit, StorageContainer container) {
		Set<Pair<Long, Long>> siteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
		if (siteCps != null) {
			List<Pair<Long, Long>> contSiteCps = siteCps.stream()
				.filter(siteCp -> siteCp.first().equals(container.getSite().getId()))
				.collect(Collectors.toList());

			crit.siteCps(contSiteCps);
		}

		return crit;
	}
	
	private StorageContainerPosition createChildContainerPosition(
			StorageContainer container, 
			StorageContainerPositionDetail pos) {
		
		StorageContainer childContainer = getContainer(pos.getOccupyingEntityId(), pos.getOccupyingEntityName());
		AccessCtrlMgr.getInstance().ensureUpdateContainerRights(childContainer);
		if (!container.canContain(childContainer)) {
			throw OpenSpecimenException.userError(
					StorageContainerErrorCode.CANNOT_HOLD_CONTAINER, 
					container.getName(), 
					childContainer.getName());
		}
		
		if (!container.canContainerOccupyPosition(childContainer.getId(), pos.getPosOne(), pos.getPosTwo())) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
		}
		
		StorageContainerPosition position = container.createPosition(pos.getPosOne(), pos.getPosTwo());
		position.setOccupyingContainer(childContainer);
		return position;
	}
	
	private void replicateContainer(StorageContainer srcContainer, DestinationDetail dest) {
		StorageContainerDetail detail = new StorageContainerDetail();
		detail.setName(dest.getName());
		detail.setSiteName(dest.getSiteName());
		
		StorageLocationSummary storageLocation = new StorageLocationSummary();
		storageLocation.setId(dest.getParentContainerId());
		storageLocation.setName(dest.getParentContainerName());
		storageLocation.setPositionX(dest.getPosOne());
		storageLocation.setPositionY(dest.getPosTwo());
		storageLocation.setPosition(dest.getPosition());
		detail.setStorageLocation(storageLocation);

		createStorageContainer(getContainerCopy(srcContainer), detail);
	}

	private void createContainerHierarchy(ContainerType containerType, StorageContainer parentContainer) {
		if (containerType == null) {
			return;
		}
		
		StorageContainer container = containerFactory.createStorageContainer("dummyName", containerType, parentContainer);
		int noOfContainers = parentContainer.getNoOfRows() * parentContainer.getNoOfColumns();
		boolean setCapacity = true;
		for (int i = 1; i <= noOfContainers; i++) {
			StorageContainer cloned = null;
			if (i == 1) {
				cloned = container;
			} else {
				cloned = container.copy();
				setPosition(cloned);
			}

			generateName(cloned);
			parentContainer.addChildContainer(cloned);

			if (cloned.isStoreSpecimenEnabled() && setCapacity) {
				cloned.setFreezerCapacity();
				setCapacity = false;
			}

			createContainerHierarchy(containerType.getCanHold(), cloned);
		}
	}

	private List<StorageContainer> getAncestors(List<StorageContainer> containers) {
		Set<Long> descContIds = containers.stream()
			.flatMap(c -> c.getDescendentContainers().stream().filter(d -> !d.equals(c)))
			.map(StorageContainer::getId)
			.collect(Collectors.toSet());

		return containers.stream()
			.filter(c -> !descContIds.contains(c.getId()))
			.collect(Collectors.toList());
	}

	private void generateName(StorageContainer container) {
		ContainerType type = container.getType();
		String name = nameGenerator.generateLabel(type.getNameFormat(), container);
		if (StringUtils.isBlank(name)) {
			throw OpenSpecimenException.userError(
				StorageContainerErrorCode.INCORRECT_NAME_FMT,
				type.getNameFormat(),
				type.getName());
		}

		container.setName(name);
	}

	private void setPosition(StorageContainer container) {
		StorageContainer parentContainer = container.getParentContainer();
		if (parentContainer == null) {
			return;
		}
		
		StorageContainerPosition position = parentContainer.nextAvailablePosition(true);
		if (position == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, parentContainer.getName());
		} 
		
		position.setOccupyingContainer(container);
		container.setPosition(position);
	}

	private StorageContainer getContainerCopy(StorageContainer source) {
		StorageContainer copy = new StorageContainer();
		copy.setTemperature(source.getTemperature());
		copy.setNoOfColumns(source.getNoOfColumns());
		copy.setNoOfRows(source.getNoOfRows());
		copy.setColumnLabelingScheme(source.getColumnLabelingScheme());
		copy.setRowLabelingScheme(source.getRowLabelingScheme());
		copy.setComments(source.getComments());
		copy.setAllowedSpecimenClasses(new HashSet<String>(source.getAllowedSpecimenClasses()));
		copy.setAllowedSpecimenTypes(new HashSet<String>(source.getAllowedSpecimenTypes()));
		copy.setAllowedCps(new HashSet<CollectionProtocol>(source.getAllowedCps()));
		copy.setCompAllowedSpecimenClasses(copy.computeAllowedSpecimenClasses());
		copy.setCompAllowedSpecimenTypes(copy.computeAllowedSpecimenTypes());
		copy.setCompAllowedCps(copy.computeAllowedCps());
		copy.setStoreSpecimenEnabled(source.isStoreSpecimenEnabled());
		copy.setCreatedBy(AuthUtil.getCurrentUser());
		return copy;
	}

	private QueryDataExportResult exportResult(final StorageContainer container, SavedQuery query) {
		Filter filter = new Filter();
		filter.setField("Specimen.specimenPosition.allAncestors.ancestorId");
		filter.setOp(Filter.Op.EQ);
		filter.setValues(new String[] { container.getId().toString() });

		ExecuteQueryEventOp execReportOp = new ExecuteQueryEventOp();
		execReportOp.setDrivingForm("Participant");
		execReportOp.setAql(query.getAql(new Filter[] { filter }));
		execReportOp.setWideRowMode(WideRowMode.DEEP.name());
		execReportOp.setRunType("Export");
		return querySvc.exportQueryData(execReportOp, new QueryService.ExportProcessor() {
			@Override
			public String filename() {
				return "container_" + container.getId() + "_" + UUID.randomUUID().toString();
			}

			@Override
			public void headers(OutputStream out) {
				Map<String, String> headers = new LinkedHashMap<String, String>() {{
					String notSpecified = msg("common_not_specified");

					put(msg("container_name"), container.getName());
					put(msg("container_site"), container.getSite().getName());

					if (container.getParentContainer() != null) {
						put(msg("container_parent_container"), container.getParentContainer().getName());
					}

					if (container.getType() != null) {
						put(msg("container_type"), container.getType().getName());
					}

					put("", ""); // blank line
				}};

				Utility.writeKeyValuesToCsv(out, headers);
			}
		});
	}

	private Function<ExportJob, List<? extends Object>> getContainersGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean loadTopLevelContainers = true;

			private boolean endOfContainers;

			private int startAt;

			private StorageContainerListCriteria topLevelCrit;

			private StorageContainerListCriteria descendantsCrit;

			private List<StorageContainerDetail> topLevelContainers = new ArrayList<>();

			@Override
			public List<StorageContainerDetail> apply(ExportJob job) {
				if (endOfContainers) {
					return Collections.emptyList();
				}

				if (topLevelContainers.isEmpty()) {
					if (topLevelCrit == null) {
						topLevelCrit = new StorageContainerListCriteria().topLevelContainers(true).ids(job.getRecordIds());
						addContainerListCriteria(topLevelCrit);
					}

					if (loadTopLevelContainers) {
						topLevelContainers = getContainers(topLevelCrit.startAt(startAt));
						startAt += topLevelContainers.size();
						loadTopLevelContainers = CollectionUtils.isEmpty(job.getRecordIds());
					}
				}

				if (topLevelContainers.isEmpty()) {
					endOfContainers = true;
					return Collections.emptyList();
				}

				if (descendantsCrit == null) {
					descendantsCrit = new StorageContainerListCriteria()
						.siteCps(topLevelCrit.siteCps()).maxResults(100000);
				}

				StorageContainerDetail topLevelContainer = topLevelContainers.remove(0);
				descendantsCrit.parentContainerId(topLevelContainer.getId());
				List<StorageContainer> descendants = daoFactory.getStorageContainerDao().getDescendantContainers(descendantsCrit);

				Map<Long, List<StorageContainer>> childContainersMap = new HashMap<>();
				for (StorageContainer container : descendants) {
					Long parentId = container.getParentContainer() == null ? null : container.getParentContainer().getId();
					List<StorageContainer> childContainers = childContainersMap.get(parentId);
					if (childContainers == null) {
						childContainers = new ArrayList<>();
						childContainersMap.put(parentId, childContainers);
					}

					childContainers.add(container);
				}

				List<StorageContainerDetail> workList = new ArrayList<>();
				workList.addAll(toDetailList(childContainersMap.get(null)));

				List<StorageContainerDetail> result = new ArrayList<>();
				while (!workList.isEmpty()) {
					StorageContainerDetail containerDetail = workList.remove(0);
					result.add(containerDetail);

					List<StorageContainer> childContainers = childContainersMap.get(containerDetail.getId());
					if (childContainers != null) {
						workList.addAll(0, toDetailList(childContainers));
					}
				}

				return result;
			}

			private List<StorageContainerDetail> getContainers(StorageContainerListCriteria crit) {
				return toDetailList(daoFactory.getStorageContainerDao().getStorageContainers(crit));
			}

			private List<StorageContainerDetail> toDetailList(List<StorageContainer> containers) {
				return containers.stream()
					.sorted((c1, c2) -> {
						if (c1.getPosition() == null && c2.getPosition() == null) {
							return c1.getId().intValue() - c2.getId().intValue();
						} else if (c1.getPosition() == null) {
							return -1;
						} else if (c2.getPosition() == null) {
							return 1;
						} else {
							return c1.getPosition().getPosition() - c2.getPosition().getPosition();
						}
					})
					.map(StorageContainerDetail::from).collect(Collectors.toList());
			}
		};
	}

	private String msg(String code) {
		return MessageUtil.getInstance().getMessage(code);
	}
}
