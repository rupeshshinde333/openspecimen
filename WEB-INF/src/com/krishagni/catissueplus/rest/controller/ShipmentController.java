package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

import com.krishagni.catissueplus.core.administrative.events.ShipmentContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentItemsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ShipmentListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ShipmentSpecimenDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.services.ShipmentService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;

@Controller
@RequestMapping("/shipments")
public class ShipmentController {

	@Autowired
	private ShipmentService shipmentSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ShipmentDetail> getShipments(
		@RequestParam(value = "name", required = false, defaultValue = "")
		String name,
			
		@RequestParam(value = "recvInstitute", required = false, defaultValue = "")
		String recvInstitute,
			
		@RequestParam(value = "recvSite", required = false, defaultValue = "")
		String recvSite,
			
		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,
				
		@RequestParam(value = "maxResults", required = false, defaultValue = "50")
		int maxResults,

		@RequestParam(value = "includeStat", required = false, defaultValue = "false")
		boolean includeStat) {
		
		ShipmentListCriteria listCrit = new ShipmentListCriteria()
			.name(name)
			.recvInstitute(recvInstitute)
			.recvSite(recvSite)
			.startAt(startAt)
			.maxResults(maxResults)
			.includeStat(includeStat);
		return response(shipmentSvc.getShipments(request(listCrit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getShipmentsCount(
		@RequestParam(value = "name", required = false, defaultValue = "")
		String name,
			
		@RequestParam(value = "recvInstitute", required = false, defaultValue = "")
		String recvInstitute,
			
		@RequestParam(value = "recvSite", required = false, defaultValue = "")
		String recvSite) {
		
		ShipmentListCriteria listCrit = new ShipmentListCriteria()
			.name(name)
			.recvInstitute(recvInstitute)
			.recvSite(recvSite);
		return Collections.singletonMap("count", response(shipmentSvc.getShipmentsCount(request(listCrit))));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ShipmentDetail getShipment(@PathVariable("id") Long id) {
		return response(shipmentSvc.getShipment(request(id)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/containers")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ShipmentContainerDetail> getShipmentContainers(
		@PathVariable("id")
		Long shipmentId,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		ShipmentItemsListCriteria crit = new ShipmentItemsListCriteria()
			.shipmentId(shipmentId)
			.startAt(startAt)
			.maxResults(maxResults);
		return response(shipmentSvc.getShipmentContainers(request(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ShipmentSpecimenDetail> getShipmentSpecimens(
		@PathVariable("id")
		Long shipmentId,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		ShipmentItemsListCriteria crit = new ShipmentItemsListCriteria()
			.shipmentId(shipmentId)
			.startAt(startAt)
			.maxResults(maxResults);
		return response(shipmentSvc.getShipmentSpecimens(request(crit)));
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ShipmentDetail createShipment(@RequestBody ShipmentDetail detail) {
		return response(shipmentSvc.createShipment(request(detail)));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ShipmentDetail updateShipment(
		@PathVariable("id")
		Long id,
			
		@RequestBody
		ShipmentDetail detail) {

		detail.setId(id);
		return response(shipmentSvc.updateShipment(request(detail)));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryDataExportResult exportReport(@PathVariable("id") Long id) {
		return response(shipmentSvc.exportReport(request(id)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/containers")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<StorageContainerSummary> getContainers(
		@RequestParam(value = "name")
		List<String> names,

		@RequestParam(value = "sendingSite", required = false, defaultValue = "")
		String sendSiteName,

		@RequestParam(value = "receivingSite", required = false, defaultValue = "")
		String recvSiteName) {
		return shipmentSvc.getContainers(names, sendSiteName, recvSiteName);
	}
	
	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
