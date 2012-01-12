package com.gimranov.zandy.app.task;

public interface APIEvent {
	public void onComplete(APIRequest request);
	
	public void onUpdate(APIRequest request);
	
	public void onError(APIRequest request, Exception exception);
	public void onError(APIRequest request, int error);
}
