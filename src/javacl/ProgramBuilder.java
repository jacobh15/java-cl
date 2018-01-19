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

public class ProgramBuilder {
	
	private static final String KERNEL = "kernel", __KERNEL = "__kernel";
	
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
		
		int parity = 0;
		for(int i = 0; i < source.length(); i++){
			if(parity == 0 && (i == 0 || nonIdentifier(source.charAt(i - 1))) && saysKernel(i)){
				i = createKernel(i, program);
			}
			if(source.charAt(i) == '{'){
				parity--;
			}else if(source.charAt(i) == '}'){
				parity++;
			}
		}
		clear();
		context.programs.add(program);
		return program;
	}
	
	public void clear(){
		options = "";
		callback = null;
		callbackData = 0;
		source = "";
	}
	
	private static boolean nonIdentifier(char c){
		return c == ' ' || c == '\t' || c == '\n' || c == '}' || c == ';';
	}
	
	private boolean saysKernel(int i){
		if(source.charAt(i) == 'k'){
			return source.substring(i, i + KERNEL.length()).equals(KERNEL);
		}else if(source.charAt(i) == '_'){
			return source.substring(i, i + __KERNEL.length()).equals(__KERNEL);
		}
		return false;
	}
	
	private int createKernel(int i, Program p){
		int space = i + (source.charAt(i) == 'k' ? KERNEL.length() : __KERNEL.length());
		while(nonIdentifier(source.charAt(space))){space++;}
		space = firstWhiteSpace(source, space);
		while(nonIdentifier(source.charAt(space))){space++;}
		int parenOpen = source.indexOf('(', space);
		String name = source.substring(space, parenOpen).trim();
		int parenClose = source.indexOf(')', parenOpen);
		String[] argumentsToParse = source.substring(parenOpen + 1, parenClose).split(",");
		List<String> argNames = new ArrayList<>();
		List<MemoryType> argTypes = new ArrayList<>();
		for(int a = 0; a < argumentsToParse.length; a++){
			String parse = argumentsToParse[a].trim();
			int s = parse.length() - 1;
			while(parse.charAt(s) != '*' && !nonIdentifier(parse.charAt(s))){s--;}
			String argName = parse.substring(s + 1);
			int endType = s;
			while(parse.charAt(endType) == '*' || nonIdentifier(parse.charAt(endType))){endType--;}
			int startType = endType;
			while(startType > 0 && !nonIdentifier(parse.charAt(startType))){startType--;}
			String argType = parse.substring(startType + 1, endType + 1) + 
					(parse.substring(endType + 1, s + 1).contains("*") ? "*" : "");
			argNames.add(argName);
			argTypes.add(MemoryType.getType(argType));
		}
		p.createKernel(name, argNames, argTypes);
		return parenClose;
	}
	
	private static int firstWhiteSpace(String s, int from){
		for(; from < s.length(); from++){
			if(s.charAt(from) == ' ' || s.charAt(from) == '\t' || s.charAt(from) == '\n')
				return from;
		}
		return -1;
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
