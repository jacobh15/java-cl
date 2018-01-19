package javacl;

import org.lwjgl.opencl.CL10;

public class Event extends CLObject {
	
	CommandQueue q;
	private ProfilingEvent profiler;
	private CommandType type;
	
	Event(long p, CommandQueue commandQueue){
		super(p);
		profiler = new ProfilingEvent(p);
		q = commandQueue;
		cl.addToReleaseQueue(() -> CL10.clReleaseEvent(p));
	}
	
	public CommandQueue getCommandQueue(){
		return q;
	}
	
	public CommandExecutionStatus getCommandExecutionStatus(){
		return CommandExecutionStatus.getStatus(cl.queryInfoInt(this, CL10.CL_EVENT_COMMAND_EXECUTION_STATUS));
	}
	
	public void waitForCompletion(){
		cl.errorCheck(CL10.clWaitForEvents(ptr));
	}
	
	public CommandType getCommandType(){
		if(type == null)
			type = CommandType.getCommand(cl.queryInfoInt(this, CL10.CL_EVENT_COMMAND_TYPE));
		return type;
	}
	
	public boolean canBeProfiled(){
		return q.profilingEnabled();
	}
	
	public long getTimeQueued(){
		if(q.profilingEnabled()){
			return cl.queryInfoLong(profiler, CL10.CL_PROFILING_COMMAND_QUEUED);
		}else{
			return -1;
		}
	}
	
	public long getTimeSubmitted(){
		if(q.profilingEnabled()){
			return cl.queryInfoLong(profiler, CL10.CL_PROFILING_COMMAND_SUBMIT);
		}else{
			return -1;
		}
	}
	
	public long getTimeStarted(){
		if(q.profilingEnabled()){
			return cl.queryInfoLong(profiler, CL10.CL_PROFILING_COMMAND_START);
		}else{
			return -1;
		}
	}
	
	public long getTimeFinished(){
		if(q.profilingEnabled()){
			return cl.queryInfoLong(profiler, CL10.CL_PROFILING_COMMAND_END);
		}else{
			return -1;
		}
	}
}
