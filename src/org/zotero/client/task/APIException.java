package org.zotero.client.task;

public class APIException extends Exception {

	/**
	 * Don't know what this is for.
	 */
	private static final long serialVersionUID = 1L;

	public APIRequest request;
	
	public APIException(APIRequest request) {
		this.request = request;
	}
	
}
