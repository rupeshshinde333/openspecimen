
package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;

import org.apache.commons.lang3.ObjectUtils;

import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

public class VisitSummary implements Comparable<VisitSummary> {
	private Long id;

	private Long cpId;
	
	private Long eventId;

	private String name;
	
	private String eventLabel;
	
	private Integer eventPoint;

	private IntervalUnit eventPointUnit;
	
	private String status;
	
	private Date visitDate;
	
	private Date anticipatedVisitDate;

	private int totalPendingSpmns;

	private int pendingPrimarySpmns;
	
	private int plannedPrimarySpmnsColl;
	
	private int uncollectedPrimarySpmns;

	private int unplannedPrimarySpmnsColl;

	private int storedSpecimens;

	private int notStoredSpecimens;

	private int distributedSpecimens;

	private int closedSpecimens;

	private String missedReason;

	private UserSummary missedBy;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
	}

	public Integer getEventPoint() {
		return eventPoint;
	}

	public void setEventPoint(Integer eventPoint) {
		this.eventPoint = eventPoint;
	}

	public IntervalUnit getEventPointUnit() {
		return eventPointUnit;
	}

	public void setEventPointUnit(IntervalUnit eventPointUnit) {
		this.eventPointUnit = eventPointUnit;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getVisitDate() {
		return visitDate;
	}

	public void setVisitDate(Date visitDate) {
		this.visitDate = visitDate;
	}

	public Date getAnticipatedVisitDate() {
		return anticipatedVisitDate;
	}

	public void setAnticipatedVisitDate(Date anticipatedVisitDate) {
		this.anticipatedVisitDate = anticipatedVisitDate;
	}

	public int getTotalPendingSpmns() {
		return totalPendingSpmns;
	}

	public void setTotalPendingSpmns(int totalPendingSpmns) {
		this.totalPendingSpmns = totalPendingSpmns;
	}

	public int getPendingPrimarySpmns() {
		return pendingPrimarySpmns;
	}

	public void setPendingPrimarySpmns(int pendingPrimarySpmns) {
		this.pendingPrimarySpmns = pendingPrimarySpmns;
	}

	public int getPlannedPrimarySpmnsColl() {
		return plannedPrimarySpmnsColl;
	}

	public void setPlannedPrimarySpmnsColl(int plannedPrimarySpmnsColl) {
		this.plannedPrimarySpmnsColl = plannedPrimarySpmnsColl;
	}

	public int getUncollectedPrimarySpmns() {
		return uncollectedPrimarySpmns;
	}

	public void setUncollectedPrimarySpmns(int uncollectedPrimarySpmns) {
		this.uncollectedPrimarySpmns = uncollectedPrimarySpmns;
	}

	public int getUnplannedPrimarySpmnsColl() {
		return unplannedPrimarySpmnsColl;
	}

	public void setUnplannedPrimarySpmnsColl(int unplannedPrimarySpmnsColl) {
		this.unplannedPrimarySpmnsColl = unplannedPrimarySpmnsColl;
	}

	public int getStoredSpecimens() {
		return storedSpecimens;
	}

	public void setStoredSpecimens(int storedSpecimens) {
		this.storedSpecimens = storedSpecimens;
	}

	public int getNotStoredSpecimens() {
		return notStoredSpecimens;
	}

	public void setNotStoredSpecimens(int notStoredSpecimens) {
		this.notStoredSpecimens = notStoredSpecimens;
	}

	public int getDistributedSpecimens() {
		return distributedSpecimens;
	}

	public void setDistributedSpecimens(int distributedSpecimens) {
		this.distributedSpecimens = distributedSpecimens;
	}

	public int getClosedSpecimens() {
		return closedSpecimens;
	}

	public void setClosedSpecimens(int closedSpecimens) {
		this.closedSpecimens = closedSpecimens;
	}

	public String getMissedReason() {
		return missedReason;
	}

	public void setMissedReason(String missedReason) {
		this.missedReason = missedReason;
	}

	public UserSummary getMissedBy() {
		return missedBy;
	}

	public void setMissedBy(UserSummary missedBy) {
		this.missedBy = missedBy;
	}

	@Override
	public int compareTo(VisitSummary other) {
		Integer thisEventPoint = Utility.getNoOfDays(eventPoint, eventPointUnit);
		Integer otherEventPoint = Utility.getNoOfDays(other.eventPoint, other.eventPointUnit);
		int result = ObjectUtils.compare(thisEventPoint, otherEventPoint, true);
		if (result != 0) {
			return result;
		}

		result = ObjectUtils.compare(this.eventId, other.eventId, true);
		if (result != 0) {
			return result;
		}

		Date thisVisitDate = visitDate != null ? visitDate : anticipatedVisitDate;
		Date otherVisitDate = other.visitDate != null ? other.visitDate : other.anticipatedVisitDate;
		return ObjectUtils.compare(thisVisitDate, otherVisitDate, true);
	}
}
