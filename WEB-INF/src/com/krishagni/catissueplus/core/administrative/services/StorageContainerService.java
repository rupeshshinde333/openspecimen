package com.krishagni.catissueplus.core.administrative.services;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
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
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;

public interface StorageContainerService {
	ResponseEvent<List<StorageContainerSummary>> getStorageContainers(RequestEvent<StorageContainerListCriteria> req);
	
	ResponseEvent<Long> getStorageContainersCount(RequestEvent<StorageContainerListCriteria> req);
	
	ResponseEvent<StorageContainerDetail> getStorageContainer(RequestEvent<ContainerQueryCriteria> req);
	
	ResponseEvent<List<StorageContainerPositionDetail>> getOccupiedPositions(RequestEvent<Long> req);

	ResponseEvent<List<SpecimenInfo>> getSpecimens(RequestEvent<SpecimenListCriteria> req);

	ResponseEvent<Long> getSpecimensCount(RequestEvent<SpecimenListCriteria> req);

	ResponseEvent<QueryDataExportResult> getSpecimensReport(RequestEvent<ContainerQueryCriteria> req);
	
	ResponseEvent<StorageContainerDetail> createStorageContainer(RequestEvent<StorageContainerDetail> req);
	
	ResponseEvent<StorageContainerDetail> updateStorageContainer(RequestEvent<StorageContainerDetail> req);
	
	ResponseEvent<StorageContainerDetail> patchStorageContainer(RequestEvent<StorageContainerDetail> req);
	
	ResponseEvent<Boolean> isAllowed(RequestEvent<TenantDetail> req);
	
	ResponseEvent<ExportedFileDetail> exportMap(RequestEvent<ContainerQueryCriteria> req);
	
	ResponseEvent<List<StorageContainerPositionDetail>> assignPositions(RequestEvent<PositionsDetail> req);
		
	ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<Long> req);

	ResponseEvent<List<StorageContainerSummary>> deleteStorageContainers(RequestEvent<BulkDeleteEntityOp> req);
	
	ResponseEvent<Boolean> replicateStorageContainer(RequestEvent<ContainerReplicationDetail> req);

	ResponseEvent<List<StorageContainerSummary>> createContainerHierarchy(RequestEvent<ContainerHierarchyDetail> req);

	ResponseEvent<List<StorageContainerSummary>> createMultipleContainers(RequestEvent<List<StorageContainerDetail>> req);

	//
	// slot blocking APIs
	ResponseEvent<List<StorageContainerPositionDetail>> blockPositions(RequestEvent<PositionsDetail> req);

	ResponseEvent<List<StorageContainerPositionDetail>> unblockPositions(RequestEvent<PositionsDetail> req);
	//
	//
	// Auto allocation and reservation
	//
	ResponseEvent<List<StorageLocationSummary>> reservePositions(RequestEvent<ReservePositionsOp> req);

	ResponseEvent<Integer> cancelReservation(RequestEvent<String> req);

	//
	// Mostly present to implement UI tree for faster access
	//

	//
	// Useful for displaying UI freezer tree
	// Gets all the sibling containers, ancestors and their siblings all the way up to root container.
	// For example: [F, [R1, R2 [B1, B2*, B3], R3, ... Rn] is the
	// result when input container ID is B2
	//
	ResponseEvent<StorageContainerSummary> getAncestorsHierarchy(RequestEvent<ContainerQueryCriteria> req);

	ResponseEvent<List<StorageContainerSummary>> getChildContainers(RequestEvent<ContainerQueryCriteria> req);

	ResponseEvent<List<StorageContainerSummary>> getDescendantContainers(RequestEvent<StorageContainerListCriteria> req);

	ResponseEvent<List<StorageLocationSummary>> getVacantPositions(RequestEvent<VacantPositionsOp> req);

	//
	// Internal APIs
	//
	StorageContainer createStorageContainer(StorageContainer base, StorageContainerDetail input);
}
