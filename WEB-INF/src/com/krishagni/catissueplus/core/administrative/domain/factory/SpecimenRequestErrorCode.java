package com.krishagni.catissueplus.core.administrative.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum SpecimenRequestErrorCode implements ErrorCode {
	NOT_FOUND,

	CLOSED,

	SPECIMEN_DISTRIBUTED,

	SPECIMEN_SHIPPED,

	FORM_NOT_CONFIGURED,

	FORM_NOT_FILLED,

	INVALID_CAT_ID,

	INVALID_ITEM_ID,

	REQUESTOR_EMAIL_REQ;

	@Override
	public String code() {
		return "SPMN_REQ_" + this.name();
	}
}
