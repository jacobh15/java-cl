package javacl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;

public class Kernel extends CLObject implements Releaseable {
	private static final byte TRUE = 1, FALSE = 0;
	
	private Program program;
	private Context context;
	private String name;
	List<String> argumentNames;
	Map<String, MemoryType> argumentTypes;
	Map<String, Integer> argumentPositions;
	
	Kernel(long p, Program prog, String name, List<String> argNames, List<MemoryType> argTypes){
		super(p);
		program = prog;
		context = program.getContext();
		this.name = name;
		argumentTypes = new HashMap<>();
		argumentNames = new ArrayList<>(argNames);
		argumentPositions = new HashMap<>();
		for(int i = 0; i < argNames.size(); i++){
			argumentTypes.put(argNames.get(i), argTypes.get(i));
			argumentPositions.put(argNames.get(i), i);
		}
	}
	
	public void release(){
		cl.errorCheck(CL10.clReleaseKernel(ptr));
	}
	
	public String toString(){
		return "Kernel " + getName() + " in " + program.toString();
	}
	
	public int getMaxWorkGroupSize(Device d){
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer size = stack.mallocPointer(1);
			CL10.clGetKernelWorkGroupInfo(ptr, d.ptr, CL10.CL_KERNEL_WORK_GROUP_SIZE, (ByteBuffer)null, size);
			ByteBuffer data = stack.malloc((int)size.get(0));
			CL10.clGetKernelWorkGroupInfo(ptr, d.ptr, CL10.CL_KERNEL_WORK_GROUP_SIZE, data, null);
			if(size.get(0) == 4){
				return data.asIntBuffer().get(0);
			}else if(size.get(0) == 8){
				return (int)data.asLongBuffer().get(0);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	
	public int[] getCompileWorkGroupSize(Device d){
		int[] sizearr = new int[3];
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer size = stack.mallocPointer(1);
			CL10.clGetKernelWorkGroupInfo(ptr, d.ptr, CL10.CL_KERNEL_COMPILE_WORK_GROUP_SIZE, (ByteBuffer)null, size);
			ByteBuffer data = stack.malloc((int)size.get(0));
			CL10.clGetKernelWorkGroupInfo(ptr, d.ptr, CL10.CL_KERNEL_COMPILE_WORK_GROUP_SIZE, data, null);
			if(size.get(0) == 3 * 4){
				IntBuffer buf = data.asIntBuffer();
				for(int i = 0; i < 3; i++){
					sizearr[i] = buf.get(i);
				}
			}else if(size.get(0) == 3 * 8){
				LongBuffer buf = data.asLongBuffer();
				for(int i = 0; i < 3; i++){
					sizearr[i] = (int)buf.get(i);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return sizearr;
	}
	
	public int getLocalMemSize(Device d){
		try(MemoryStack stack = MemoryStack.stackPush()){
			PointerBuffer size = stack.mallocPointer(1);
			CL10.clGetKernelWorkGroupInfo(ptr, d.ptr, CL10.CL_KERNEL_LOCAL_MEM_SIZE, (ByteBuffer)null, size);
			ByteBuffer data = stack.malloc((int)size.get(0));
			CL10.clGetKernelWorkGroupInfo(ptr, d.ptr, CL10.CL_KERNEL_LOCAL_MEM_SIZE, data, null);
			if(size.get(0) == 4){
				return data.asIntBuffer().get(0);
			}else if(size.get(0) == 8){
				return (int)data.asLongBuffer().get(0);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	
	public Context getContext(){
		return context;
	}
	
	public Program getProgram(){
		return program;
	}
	
	public String getName(){
		return name;
	}
	
	public List<Device> getDevices(){
		return program.getDevices();
	}
	
	public MemoryType getArgumentType(String argName){
		return argumentTypes.get(argName);
	}
	
	public MemoryType getArgumentType(int arg){
		return getArgumentType(argumentNames.get(arg));
	}
	
	public String getArgumentName(int arg){
		return argumentNames.get(arg);
	}
	
	public int numArgs(){
		return argumentTypes.size();
	}
	
	public List<String> getArgumentNames(){
		return new ArrayList<>(argumentNames);
	}
	
	public Kernel setArgSize(String name, int size){
		cl.errorCheck(CL10.clSetKernelArg(ptr, argumentPositions.get(name), (long)size));
		return this;
	}
	
	public Kernel setArg(String name, KernelArgumentSetter setter){
		setter.setArg(ptr, argumentPositions.get(name));
		return this;
	}
	
	public Kernel setArg(String name, boolean b){
		cl.errorCheck(CL10.clSetKernelArg1b(ptr, argumentPositions.get(name), b ? TRUE: FALSE));
		return this;
	}
	
	public Kernel setArg(String name, boolean b1, boolean b2){
		cl.errorCheck(CL10.clSetKernelArg2b(ptr, argumentPositions.get(name), b1 ? TRUE: FALSE, b2 ? TRUE: FALSE));
		return this;
	}
	
	public Kernel setArg(String name, boolean b1, boolean b2, boolean b3){
		cl.errorCheck(CL10.clSetKernelArg3b(ptr, argumentPositions.get(name), b1 ? TRUE: FALSE, b2 ? TRUE: FALSE, b3 ? TRUE: FALSE));
		return this;
	}
	
	public Kernel setArg(String name, boolean b1, boolean b2, boolean b3, boolean b4){
		cl.errorCheck(CL10.clSetKernelArg4b(ptr, argumentPositions.get(name), b1 ? TRUE: FALSE, b2 ? TRUE: FALSE, b3 ? TRUE: FALSE, b4 ? TRUE: FALSE));
		return this;
	}
	
	public Kernel setArg(String name, byte b){
		cl.errorCheck(CL10.clSetKernelArg1b(ptr, argumentPositions.get(name), b));
		return this;
	}
	
	public Kernel setArg(String name, byte b1, byte b2){
		cl.errorCheck(CL10.clSetKernelArg2b(ptr, argumentPositions.get(name), b1, b2));
		return this;
	}
	
	public Kernel setArg(String name, byte b1, byte b2, byte b3){
		cl.errorCheck(CL10.clSetKernelArg3b(ptr, argumentPositions.get(name), b1, b2, b3));
		return this;
	}
	
	public Kernel setArg(String name, byte b1, byte b2, byte b3, byte b4){
		cl.errorCheck(CL10.clSetKernelArg4b(ptr, argumentPositions.get(name), b1, b2, b3, b4));
		return this;
	}
	
	public Kernel setArg(String name, short s){
		cl.errorCheck(CL10.clSetKernelArg1s(ptr, argumentPositions.get(name), s));
		return this;
	}
	
	public Kernel setArg(String name, short s1, short s2){
		cl.errorCheck(CL10.clSetKernelArg2s(ptr, argumentPositions.get(name), s1, s2));
		return this;
	}
	
	public Kernel setArg(String name, short s1, short s2, short s3){
		cl.errorCheck(CL10.clSetKernelArg3s(ptr, argumentPositions.get(name), s1, s2, s3));
		return this;
	}
	
	public Kernel setArg(String name, short s1, short s2, short s3, short s4){
		cl.errorCheck(CL10.clSetKernelArg4s(ptr, argumentPositions.get(name), s1, s2, s3, s4));
		return this;
	}
	
	public Kernel setArg(String name, int i){
		cl.errorCheck(CL10.clSetKernelArg1i(ptr, argumentPositions.get(name), i));
		return this;
	}
	
	public Kernel setArg(String name, int i1, int i2){
		cl.errorCheck(CL10.clSetKernelArg2i(ptr, argumentPositions.get(name), i1, i2));
		return this;
	}
	
	public Kernel setArg(String name, int i1, int i2, int i3){
		cl.errorCheck(CL10.clSetKernelArg3i(ptr, argumentPositions.get(name), i1, i2, i3));
		return this;
	}
	
	public Kernel setArg(String name, int i1, int i2, int i3, int i4){
		cl.errorCheck(CL10.clSetKernelArg4i(ptr, argumentPositions.get(name), i1, i2, i3, i4));
		return this;
	}
	
	public Kernel setArg(String name, long l){
		cl.errorCheck(CL10.clSetKernelArg1l(ptr, argumentPositions.get(name), l));
		return this;
	}
	
	public Kernel setArg(String name, long l1, long l2){
		cl.errorCheck(CL10.clSetKernelArg2l(ptr, argumentPositions.get(name), l1, l2));
		return this;
	}
	
	public Kernel setArg(String name, long l1, long l2, long l3){
		cl.errorCheck(CL10.clSetKernelArg3l(ptr, argumentPositions.get(name), l1, l2, l3));
		return this;
	}
	
	public Kernel setArg(String name, long l1, long l2, long l3, long l4){
		cl.errorCheck(CL10.clSetKernelArg4l(ptr, argumentPositions.get(name), l1, l2, l3, l4));
		return this;
	}
	
	public Kernel setArg(String name, float f){
		cl.errorCheck(CL10.clSetKernelArg1f(ptr, argumentPositions.get(name), f));
		return this;
	}
	
	public Kernel setArg(String name, float f1, float f2){
		cl.errorCheck(CL10.clSetKernelArg2f(ptr, argumentPositions.get(name), f1, f2));
		return this;
	}
	
	public Kernel setArg(String name, float f1, float f2, float f3){
		cl.errorCheck(CL10.clSetKernelArg3f(ptr, argumentPositions.get(name), f1, f2, f3));
		return this;
	}
	
	public Kernel setArg(String name, float f1, float f2, float f3, float f4){
		cl.errorCheck(CL10.clSetKernelArg4f(ptr, argumentPositions.get(name), f1, f2, f3, f4));
		return this;
	}
	
	public Kernel setArg(String name, double d){
		cl.errorCheck(CL10.clSetKernelArg1d(ptr, argumentPositions.get(name), d));
		return this;
	}
	
	public Kernel setArg(String name, double d1, double d2){
		cl.errorCheck(CL10.clSetKernelArg2d(ptr, argumentPositions.get(name), d1, d2));
		return this;
	}
	
	public Kernel setArg(String name, double d1, double d2, double d3){
		cl.errorCheck(CL10.clSetKernelArg3d(ptr, argumentPositions.get(name), d1, d2, d3));
		return this;
	}
	
	public Kernel setArg(String name, double d1, double d2, double d3, double d4){
		cl.errorCheck(CL10.clSetKernelArg4d(ptr, argumentPositions.get(name), d1, d2, d3, d4));
		return this;
	}
	
	public boolean hasArgument(String name){
		return argumentNames.contains(name);
	}
}
