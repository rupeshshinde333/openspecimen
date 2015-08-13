package com.krishagni.catissueplus.core.common.errors;

public enum CommonErrorCode implements ErrorCode {	
	INVALID_REQUEST, 
	
	FILE_NOT_FOUND;

	@Override
	public String code() {
		return "COMMON_" + this.name();
	}
}
