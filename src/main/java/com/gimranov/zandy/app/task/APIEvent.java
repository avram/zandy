package com.gimranov.zandy.app.task;

public interface APIEvent {
	void onComplete(APIRequest request);
	
	void onUpdate(APIRequest request);
	
	void onError(APIRequest request, Exception exception);
	void onError(APIRequest request, int error);
}
