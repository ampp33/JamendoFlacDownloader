package org.malibu.jamendo.downloader;

public class JamendoProgressToken {
	
	private ProgressListener listener;
	private String message = null;
	
	public void setProgressListener(ProgressListener listener) {
		this.listener = listener;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
		if(listener != null) listener.onMessageChange();
	}
}
