package javacl;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLProgramCallbackI;
import org.lwjgl.system.MemoryStack;

import util.StringScanner;
import util.StringStuff;

public class ProgramBuilder {
	
	private Context context;
	private String source;
	private List<Device> devices;
	private String options;
	private CLProgramCallbackI callback;
	private long callbackData;
	private DeviceType targetDeviceType;
	private boolean devicesSet = false;
	
	private List<BuildLog> buildLogs = new ArrayList<>();
	
	ProgramBuilder(Context c, String src){
		source = src;
		context = c;
		devices = context.getDevices();
		options = "";
	}
	
	public Program build(){
		Program program = null;
		try(MemoryStack stack = MemoryStack.stackPush()){
			IntBuffer err = stack.mallocInt(1);
			long p = CL10.clCreateProgramWithSource(context.ptr, source, err);
			OpenCLTools.cl().errorCheck(err.get(0));
			
			if(devices.size() == 0){
				List<Device> candidates = context.getPlatform().getDevices(targetDeviceType);
				for(int i = 0; i < candidates.size(); i++){
					if(context.hasDevice(candidates.get(i))){
						devices.add(candidates.get(i));
					}
				}
			}
			if(devices.size() == 0){
				OpenCLTools.cl().errorCheck(CL10.clReleaseProgram(p));
				throw new Exception("No devices found; program creation failed.");
			}
			
			PointerBuffer deviceBuffer = stack.mallocPointer(devices.size());
			for(int i = 0; i < devices.size(); i++){
				deviceBuffer.put(i, devices.get(i).ptr);
			}
			
			int error = CL10.clBuildProgram(p, deviceBuffer, options, callback, callbackData);
			Map<Device, String> log = new HashMap<>();
			for(int i = 0; i < devices.size(); i++){
				ProgramBuild build = new ProgramBuild(p, devices.get(i));
				log.put(devices.get(i), OpenCLTools.getOpenCLTools().queryInfoString(build, CL10.CL_PROGRAM_BUILD_LOG));
			}
			buildLogs.add(new BuildLog(log, error == CL10.CL_SUCCESS));
			OpenCLTools.cl().errorCheck(error);
			
			program = new Program(p, context, devices, lastBuildLog());
		}catch(CLException e){
			throw e;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		createKernels(program);
		context.programs.add(program);
		return program;
	}
	
	public void clear(){
		options = "";
		callback = null;
		callbackData = 0;
		source = "";
	}
	
	private void createKernels(Program p){
		source = StringStuff.removeComments(source);
		StringScanner scanner = new StringScanner(source);
		while(scanner.hasNext()) {
			String token = scanner.nextToken();
			if(token.equals("kernel") || token.equals("__kernel")) {
				scanner.nextToken();
				token = scanner.nextToken();
				if(token.equals("__attribute__")) {
					scanner.advanceTill("(").advance(1).advanceTillClose("(", ")");
					token = scanner.nextToken();
				}
				String name = token;
				scanner.advanceTill("(").advance(1).mark();
				scanner.advanceTillClose("(", ")").rewind(1);
				StringScanner scanner2 = new StringScanner(scanner.markedString());
				List<String> argumentNames = new ArrayList<>();
				List<MemoryType> argumentTypes = new ArrayList<>();
				while(scanner2.hasNext()) {
					do {
						token = scanner2.nextToken();
					}while(isQualifier(token));
					String argType = token;
					int pos = scanner2.position();
					scanner2.advanceTill(StringScanner.ALL_ALPHA, "_");
					int end = scanner2.position();
					scanner2.position(pos);
					if(scanner2.contains(end - pos, '*')) {
						argType += "*";
					}
					String argName = scanner2.nextToken();
					argumentNames.add(argName);
					argumentTypes.add(MemoryType.getType(argType));
				}
				scanner.advanceTill("{").advance(1).advanceTillClose("{", "}");
				p.createKernel(name, argumentNames, argumentTypes);
			}
		}
	}
	
	private static boolean isQualifier(String s) {
		return s.equals("local") || s.equals("__local") || s.equals("global") || s.equals("__global")
				|| s.equals("private") || s.equals("__private") || s.equals("constant") || s.equals("__constant")
				|| s.equals("const") || s.equals("restrict") || s.equals("volatile") || s.equals("read_only")
				|| s.equals("__read_only") || s.equals("write_only") || s.equals("__write_only");
	}
	
	public ProgramBuilder options(String opts){
		options = opts;
		return this;
	}
	
	public ProgramBuilder addOptions(String opts){
		options += opts;
		return this;
	}
	
	public ProgramBuilder callback(CLProgramCallbackI pcb){
		callback = pcb;
		return this;
	}
	
	public ProgramBuilder callbackData(long data){
		callbackData = data;
		return this;
	}
	
	public ProgramBuilder device(Device d){
		if(!devicesSet){
			devices.clear();
			devicesSet = true;
		}
		if(!devices.contains(d) && context.hasDevice(d))
			devices.add(d);
		return this;
	}
	
	public ProgramBuilder devices(Device...devs){
		devices.clear();
		for(int i = 0; i < devs.length; i++){
			device(devs[i]);
		}
		return this;
	}
	
	public ProgramBuilder devices(List<Device >devs){
		devices.clear();
		for(int i = 0; i < devs.size(); i++){
			device(devs.get(i));
		}
		return this;
	}
	
	public ProgramBuilder addDevices(Device...devs){
		for(int i = 0; i < devs.length; i++){
			device(devs[i]);
		}
		return this;
	}
	
	public ProgramBuilder addDevices(List<Device >devs){
		for(int i = 0; i < devs.size(); i++){
			device(devs.get(i));
		}
		return this;
	}
	
	public ProgramBuilder deviceType(DeviceType type){
		targetDeviceType = type;
		devices.clear();
		devicesSet = true;
		return this;
	}
	
	public ProgramBuilder context(Context c){
		if(context != c || !devicesSet){
			context = c;
			devices.clear();
			devicesSet = true;
		}
		return this;
	}
	
	public ProgramBuilder source(String src){
		source = src;
		return this;
	}
	
	public List<BuildLog> getBuildLogs(){
		return new ArrayList<>(buildLogs);
	}
	
	public BuildLog getBuildLog(int log){
		return buildLogs.get(log);
	}
	
	public BuildLog lastBuildLog(){
		return buildLogs.get(buildLogs.size() - 1);
	}
}
