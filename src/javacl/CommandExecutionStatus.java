package javacl;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opencl.CL10;

public enum CommandExecutionStatus {
	
	QUEUED(CL10.CL_QUEUED), SUBMITTED(CL10.CL_SUBMITTED), RUNNING(CL10.CL_RUNNING), COMPLETE(CL10.CL_COMPLETE);
	
	private static final Map<Integer, CommandExecutionStatus> TYPE_MAP = new HashMap<>();
	private static boolean init = false;
	
	private int clID;
	
	private CommandExecutionStatus(int id){
		clID = id;
	}
	
	static CommandExecutionStatus getStatus(int id){
		if(!init){
			CommandExecutionStatus[] values = CommandExecutionStatus.values();
			for(int i = 0; i < values.length; i++){
				TYPE_MAP.put(values[i].clID, values[i]);
			}
			init = true;
		}
		return TYPE_MAP.get(id);
	}
}
