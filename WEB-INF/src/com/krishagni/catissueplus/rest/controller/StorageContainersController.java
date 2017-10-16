package com.krishagni.catissueplus.rest.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.administrative.events.PositionsDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerHierarchyDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerQueryCriteria;
import com.krishagni.catissueplus.core.administrative.events.ContainerReplicationDetail;
import com.krishagni.catissueplus.core.administrative.events.ReservePositionsOp;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerPositionDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.administrative.events.TenantDetail;
import com.krishagni.catissueplus.core.administrative.events.VacantPositionsOp;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionStrategyFactory;
import com.krishagni.catissueplus.core.administrative.services.StorageContainerService;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;

import edu.common.dynamicextensions.nutility.IoUtil;

@Controller
@RequestMapping("/storage-containers")
public class StorageContainersController {
	
	@Autowired
	private StorageContainerService storageContainerSvc;
	
	@Autowired
	private HttpServletRequest httpReq;

	@Autowired
	private ContainerSelectionStrategyFactory containerSelectionStrategyFactory;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerSummary> getStorageContainers(
		@RequestParam(value = "name", required = false)
		String name,
			
		@RequestParam(value = "site", required = false)
		String siteName,

		@RequestParam(value = "canHold", required = false)
		String canHold,
			
		@RequestParam(value = "onlyFreeContainers", required = false, defaultValue = "false")
		boolean onlyFreeContainers,
			
		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,
			
		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults,
			
		@RequestParam(value = "parentContainerId", required = false)
		Long parentContainerId,
			
		@RequestParam(value = "includeChildren", required = false, defaultValue = "false")
		boolean includeChildren,
			
		@RequestParam(value = "topLevelContainers", required = false, defaultValue = "false")
		boolean topLevelContainers,
			
		@RequestParam(value = "specimenClass", required = false)
		String specimenClass,
			
		@RequestParam(value = "specimenType", required = false)
		String specimenType,
			
		@RequestParam(value = "cpId", required = false)
		Long[] cpIds,
			
		@RequestParam(value = "cpShortTitle", required = false)
		String[] cpShortTitles,

		@RequestParam(value = "storeSpecimensEnabled", required = false)
		Boolean storeSpecimensEnabled,
			
		@RequestParam(value = "hierarchical", required = false, defaultValue = "false")
		boolean hierarchical,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats) {
		
		StorageContainerListCriteria crit = new StorageContainerListCriteria()
			.query(name)
			.siteName(siteName)
			.canHold(canHold)
			.onlyFreeContainers(onlyFreeContainers)
			.startAt(startAt)
			.maxResults(maxResults)
			.parentContainerId(parentContainerId)
			.includeChildren(includeChildren)
			.topLevelContainers(topLevelContainers)
			.specimenClass(specimenClass)
			.specimenType(specimenType)
			.cpIds(cpIds)
			.cpShortTitles(cpShortTitles)
			.storeSpecimensEnabled(storeSpecimensEnabled)
			.hierarchical(hierarchical)
			.includeStat(includeStats);
					
		RequestEvent<StorageContainerListCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<List<StorageContainerSummary>> resp = storageContainerSvc.getStorageContainers(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getStorageContainersCount (
		@RequestParam(value = "name", required = false)
		String name,
			
		@RequestParam(value = "site", required = false)
		String siteName,

		@RequestParam(value = "canHold", required = false)
		String canHold,
			
		@RequestParam(value = "onlyFreeContainers", required = false, defaultValue = "false")
		boolean onlyFreeContainers,
			
		@RequestParam(value = "parentContainerId", required = false)
		Long parentContainerId,
			
		@RequestParam(value = "includeChildren", required = false, defaultValue = "false")
		boolean includeChildren,
			
		@RequestParam(value = "topLevelContainers", required = false, defaultValue = "false")
		boolean topLevelContainers,
			
		@RequestParam(value = "specimenClass", required = false)
		String specimenClass,
			
		@RequestParam(value = "specimenType", required = false)
		String specimenType,
			
		@RequestParam(value = "cpId", required = false)
		Long[] cpIds,
			
		@RequestParam(value = "cpShortTitle", required = false)
		String[] cpShortTitles,

		@RequestParam(value = "storeSpecimensEnabled", required = false)
		Boolean storeSpecimensEnabled,
			
		@RequestParam(value = "hierarchical", required = false, defaultValue = "false")
		boolean hierarchical) {
		
		StorageContainerListCriteria crit = new StorageContainerListCriteria()
			.query(name)
			.siteName(siteName)
			.canHold(canHold)
			.onlyFreeContainers(onlyFreeContainers)
			.parentContainerId(parentContainerId)
			.includeChildren(includeChildren)
			.topLevelContainers(topLevelContainers)
			.specimenClass(specimenClass)
			.specimenType(specimenType)
			.cpIds(cpIds)
			.cpShortTitles(cpShortTitles)
			.storeSpecimensEnabled(storeSpecimensEnabled)
			.hierarchical(hierarchical);

		RequestEvent<StorageContainerListCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<Long> resp = storageContainerSvc.getStorageContainersCount(req);
		resp.throwErrorIfUnsuccessful();

		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.HEAD, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public Boolean isTenantAllowed(
		@PathVariable("id")
		Long containerId,
			
		@RequestParam(value = "cpId", required = true)
		Long cpId,
			
		@RequestParam(value = "specimenType", required = true)
		String specimenType,
			
		@RequestParam(value = "specimenClass", required = true)
		String specimenClass) {
		
		TenantDetail detail = new TenantDetail();
		detail.setContainerId(containerId);
		detail.setCpId(cpId);
		detail.setSpecimenClass(specimenClass);
		detail.setSpecimenType(specimenType);
		
		RequestEvent<TenantDetail> req = new RequestEvent<TenantDetail>(detail);
		ResponseEvent<Boolean> resp = storageContainerSvc.isAllowed(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
			
	@RequestMapping(method = RequestMethod.GET, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public StorageContainerDetail getStorageContainer(
		@PathVariable("id")
		Long containerId,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats) {

		return getContainer(new ContainerQueryCriteria(containerId).includeStats(includeStats));
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/byname")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public StorageContainerDetail getStorageContainer(
		@RequestParam(value = "name", required = false)
		String name,

		@RequestParam(value = "barcode", required = false)
		String barcode,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats) {

		return getContainer(new ContainerQueryCriteria(name, barcode).includeStats(includeStats));
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public StorageContainerDetail createStorageContainer(@RequestBody StorageContainerDetail detail) {
		RequestEvent<StorageContainerDetail> req = new RequestEvent<>(detail);
		ResponseEvent<StorageContainerDetail> resp = storageContainerSvc.createStorageContainer(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public StorageContainerDetail updateStorageContainer(
		@PathVariable("id")
		Long containerId,
			
		@RequestBody
		StorageContainerDetail detail) {
		
		detail.setId(containerId);
		
		RequestEvent<StorageContainerDetail> req = new RequestEvent<>(detail);
		ResponseEvent<StorageContainerDetail> resp = storageContainerSvc.updateStorageContainer(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PATCH, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public StorageContainerDetail patchStorageContainer(
		@PathVariable("id")
		Long containerId,
			
		@RequestBody
		StorageContainerDetail detail) {
		
		detail.setId(containerId);
		
		RequestEvent<StorageContainerDetail> req = new RequestEvent<>(detail);
		ResponseEvent<StorageContainerDetail> resp = storageContainerSvc.patchStorageContainer(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value="{id}/occupied-positions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerPositionDetail> getContainerOccupiedPositions(@PathVariable("id") Long containerId) {
		RequestEvent<Long> req = new RequestEvent<Long>(containerId);
		ResponseEvent<List<StorageContainerPositionDetail>> resp = storageContainerSvc.getOccupiedPositions(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "{id}/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> getContainerSpecimens(
		@PathVariable("id")
		Long id,

		@RequestParam(value = "container", required = false)
		String container,

		@RequestParam(value = "specimenType", required = false)
		String specimenType,

		@RequestParam(value = "anatomicSite", required = false)
		String anatomicSite,

		@RequestParam(value = "ppid", required = false)
		String ppid,

		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "cpShortTitle", required = false)
		String cpShortTitle,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {
		SpecimenListCriteria crit = new SpecimenListCriteria()
			.ancestorContainerId(id)
			.container(container)
			.type(specimenType)
			.anatomicSite(anatomicSite)
			.ppid(ppid)
			.cpId(cpId)
			.cpShortTitle(cpShortTitle)
			.startAt(startAt)
			.maxResults(maxResults);

		ResponseEvent<List<SpecimenInfo>> resp = storageContainerSvc.getSpecimens(new RequestEvent<>(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "{id}/specimens-count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getContainerSpecimensCount(
		@PathVariable("id")
		Long id,

		@RequestParam(value = "container", required = false)
		String container,

		@RequestParam(value = "specimenType", required = false)
		String specimenType,

		@RequestParam(value = "anatomicSite", required = false)
		String anatomicSite,

		@RequestParam(value = "ppid", required = false)
		String ppid,

		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "cpShortTitle", required = false)
		String cpShortTitle) {
		SpecimenListCriteria crit = new SpecimenListCriteria()
			.ancestorContainerId(id)
			.container(container)
			.type(specimenType)
			.anatomicSite(anatomicSite)
			.ppid(ppid)
			.cpId(cpId)
			.cpShortTitle(cpShortTitle);

		ResponseEvent<Long> resp = storageContainerSvc.getSpecimensCount(new RequestEvent<>(crit));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.GET, value = "{id}/report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryDataExportResult getContainerSpecimensReport(@PathVariable("id") Long id) {
		ContainerQueryCriteria crit = new ContainerQueryCriteria(id);
		ResponseEvent<QueryDataExportResult> resp = storageContainerSvc.getSpecimensReport(new RequestEvent<>(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}/export-map")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public void exportContainerMap(@PathVariable("id") Long id, HttpServletResponse response) {
		ContainerQueryCriteria crit = new ContainerQueryCriteria(id);
		RequestEvent<ContainerQueryCriteria> req = new RequestEvent<ContainerQueryCriteria>(crit);
		ResponseEvent<ExportedFileDetail> resp = storageContainerSvc.exportMap(req);
		resp.throwErrorIfUnsuccessful();
		
		
		ExportedFileDetail detail = resp.getPayload();
		response.setContentType("application/csv");
		response.setHeader("Content-Disposition", "attachment;filename=" + detail.getName() + ".csv");
			
		InputStream in = null;
		try {
			in = new FileInputStream(detail.getFile());
			IoUtil.copy(in, response.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IoUtil.close(in);
			detail.getFile().delete();
		}				
	}
	
	@RequestMapping(method = RequestMethod.POST, value="{id}/occupied-positions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerPositionDetail> assignPositions(
		@PathVariable("id")
		Long containerId,
			
		@RequestBody
		PositionsDetail detail) {
		
		detail.setContainerId(containerId);
		
		RequestEvent<PositionsDetail> req = new RequestEvent<>(detail);
		ResponseEvent<List<StorageContainerPositionDetail>> resp = storageContainerSvc.assignPositions(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();		
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{id}/dependent-entities")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DependentEntityDetail> getDependentEntities(@PathVariable Long id) {
		RequestEvent<Long> req = new RequestEvent<Long>(id);
		ResponseEvent<List<DependentEntityDetail>> resp = storageContainerSvc.getDependentEntities(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public StorageContainerSummary deleteStorageContainer(@PathVariable Long id) {
		return deleteStorageContainers(new Long[] { id }).get(0);
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerSummary> deleteStorageContainers(@RequestParam(value = "id") Long[] ids) {
		BulkDeleteEntityOp op = new BulkDeleteEntityOp();
		op.setIds(new HashSet<>(Arrays.asList(ids)));

		RequestEvent<BulkDeleteEntityOp> req = new RequestEvent<>(op);
		ResponseEvent<List<StorageContainerSummary>> resp = storageContainerSvc.deleteStorageContainers(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value="/{id}/replica")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> replicateStorageContainer(@PathVariable Long id, @RequestBody ContainerReplicationDetail repl) {
		repl.setSourceContainerId(id);

		RequestEvent<ContainerReplicationDetail> req = new RequestEvent<>(repl);
		ResponseEvent<Boolean> resp = storageContainerSvc.replicateStorageContainer(req);
		resp.throwErrorIfUnsuccessful();

		return Collections.singletonMap("status", true);
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/create-hierarchy")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerSummary> createContainerHierarchy(@RequestBody ContainerHierarchyDetail detail) {
		RequestEvent<ContainerHierarchyDetail> req = new RequestEvent<ContainerHierarchyDetail>(detail);
		ResponseEvent<List<StorageContainerSummary>> resp = storageContainerSvc.createContainerHierarchy(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value="/multiple")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerSummary> createMultipleContainers(@RequestBody List<StorageContainerDetail> containers) {
		RequestEvent<List<StorageContainerDetail>> req = new RequestEvent<>(containers);
		ResponseEvent<List<StorageContainerSummary>> resp = storageContainerSvc.createMultipleContainers(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	//
	// Block slots in container
	//
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/block-positions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerPositionDetail> blockPositions(
		@PathVariable("id")
		Long id,

		@RequestBody
		List<StorageContainerPositionDetail> positions) {

		PositionsDetail detail = new PositionsDetail();
		detail.setContainerId(id);
		detail.setPositions(positions);

		RequestEvent<PositionsDetail> req = new RequestEvent<>(detail);
		ResponseEvent<List<StorageContainerPositionDetail>> resp = storageContainerSvc.blockPositions(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/unblock-positions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerPositionDetail> unblockPositions(
		@PathVariable("id")
		Long id,

		@RequestBody
		List<StorageContainerPositionDetail> positions) {

		PositionsDetail detail = new PositionsDetail();
		detail.setContainerId(id);
		detail.setPositions(positions);

		RequestEvent<PositionsDetail> req = new RequestEvent<>(detail);
		ResponseEvent<List<StorageContainerPositionDetail>> resp = storageContainerSvc.unblockPositions(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	//
	// Reserve slots in container
	//
	@RequestMapping(method = RequestMethod.POST, value = "/reserve-positions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageLocationSummary> reservePositions(@RequestBody ReservePositionsOp op) {
		RequestEvent<ReservePositionsOp> req = new RequestEvent<>(op);
		ResponseEvent<List<StorageLocationSummary>> resp = storageContainerSvc.reservePositions(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/reserve-positions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> vacatePositions(@RequestParam(value = "reservationId") String reservationId) {
		ResponseEvent<Integer> resp = storageContainerSvc.cancelReservation(new RequestEvent<>(reservationId));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("vacatedPositions", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/auto-allocation-strategies")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<Map<String, String>> getAllocationStrategies() {
		String msgPrefix = "container_alloc_";
		return containerSelectionStrategyFactory.getStrategyNames().stream()
			.map(name -> {
				Map<String, String> strategy = new HashMap<>();
				strategy.put("name", name);
				strategy.put("caption", MessageUtil.getInstance().getMessage(msgPrefix + name.replaceAll("-", "_")));
				return strategy;
			})
			.collect(Collectors.toList());
	}

	//
	// APIs created mostly for ease of implementing UI views
	//
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/ancestors-hierarchy")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public StorageContainerSummary getAncestorsHierarchy(@PathVariable("id") Long containerId) {
		RequestEvent<ContainerQueryCriteria> req = new RequestEvent<>(new ContainerQueryCriteria(containerId));
		ResponseEvent<StorageContainerSummary> resp = storageContainerSvc.getAncestorsHierarchy(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/child-containers")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerSummary> getChildContainers(@PathVariable("id") Long containerId) {
		RequestEvent<ContainerQueryCriteria> req = new RequestEvent<>(new ContainerQueryCriteria(containerId));
		ResponseEvent<List<StorageContainerSummary>> resp = storageContainerSvc.getChildContainers(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "{id}/descendant-containers")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerSummary> getDescendentContainers(
		@PathVariable("id")
		Long containerId,

		@RequestParam(value = "name", required = false, defaultValue = "")
		String name,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {
		StorageContainerListCriteria crit = new StorageContainerListCriteria()
			.parentContainerId(containerId)
			.query(name)
			.startAt(startAt)
			.maxResults(maxResults);

		ResponseEvent<List<StorageContainerSummary>> resp = storageContainerSvc.getDescendantContainers(new RequestEvent<>(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/vacant-positions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageLocationSummary> getVacantPositions(
		@PathVariable("id")
		Long containerId,

		@RequestParam(value = "startRow", required =  false)
		String startRow,

		@RequestParam(value = "startColumn", required = false)
		String startColumn,

		@RequestParam(value = "startPosition", required = false, defaultValue = "0")
		int startPosition,

		@RequestParam(value = "numPositions", required = false, defaultValue = "1")
		int numPositions) {

		VacantPositionsOp op = new VacantPositionsOp();
		op.setContainerId(containerId);
		op.setStartRow(startRow);
		op.setStartColumn(startColumn);
		op.setStartPosition(startPosition);
		op.setRequestedPositions(numPositions);

		RequestEvent<VacantPositionsOp> req = new RequestEvent<>(op);
		ResponseEvent<List<StorageLocationSummary>> resp = storageContainerSvc.getVacantPositions(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/vacant-positions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageLocationSummary> getVacantPositions(
		@RequestParam(value = "name")
		String containerName,

		@RequestParam(value = "startRow", required =  false)
		String startRow,

		@RequestParam(value = "startColumn", required = false)
		String startColumn,

		@RequestParam(value = "startPosition", required = false, defaultValue = "0")
		int startPosition,

		@RequestParam(value = "numPositions", required = false, defaultValue = "1")
		int numPositions) {

		VacantPositionsOp op = new VacantPositionsOp();
		op.setContainerName(containerName);
		op.setStartRow(startRow);
		op.setStartColumn(startColumn);
		op.setStartPosition(startPosition);
		op.setRequestedPositions(numPositions);

		RequestEvent<VacantPositionsOp> req = new RequestEvent<>(op);
		ResponseEvent<List<StorageLocationSummary>> resp = storageContainerSvc.getVacantPositions(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private StorageContainerDetail getContainer(ContainerQueryCriteria crit) {
		RequestEvent<ContainerQueryCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<StorageContainerDetail> resp = storageContainerSvc.getStorageContainer(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();		
	}	
}
