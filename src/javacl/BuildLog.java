package javacl;

import java.util.Map;

public class BuildLog {
	private Map<Device, String> logs;
	private boolean success;
	
	BuildLog(Map<Device, String> lgs, boolean success){
		logs = lgs;
		this.success = success;
	}
	
	public boolean wasSuccessful(){
		return success;
	}
	
	public String getLog(Device d){
		return logs.get(d);
	}
}
