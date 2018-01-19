package javacl;

import java.nio.ByteBuffer;

import org.lwjgl.opencl.CL10;

public abstract class Scalar extends MemoryObject {
	
	Scalar(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
			boolean useHostMem, boolean copiedHost) {
		super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
	}
	
	public boolean hostHasDataBuffer(){
		return false;
	}
	
	public static class Boolean extends Scalar {
		byte val;
		boolean valb;
		Boolean(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			val = buffer.get(0);
			valb = (val == 1);
		}

		public void setArg(long kernel, int arg) {
			cl.errorCheck(CL10.clSetKernelArg1b(kernel, arg, val));
		}
	}
	
	public static class Byte extends Scalar {
		byte val;
		Byte(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			val = buffer.get(0);
		}

		public void setArg(long kernel, int arg) {
			cl.errorCheck(CL10.clSetKernelArg1b(kernel, arg, val));
		}
	}
	
	public static class Short extends Scalar {
		short val;
		Short(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			val = buffer.asShortBuffer().get(0);
		}

		public void setArg(long kernel, int arg) {
			cl.errorCheck(CL10.clSetKernelArg1s(kernel, arg, val));
		}
	}
	
	public static class Int extends Scalar {
		int val;
		Int(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			val = buffer.asIntBuffer().get(0);
		}

		public void setArg(long kernel, int arg) {
			cl.errorCheck(CL10.clSetKernelArg1i(kernel, arg, val));
		}
	}
	
	public static class Long extends Scalar {
		long val;
		Long(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			val = buffer.asLongBuffer().get(0);
		}
		
		public void setArg(long kernel, int arg){
			cl.errorCheck(CL10.clSetKernelArg1l(kernel, arg, val));
		}
	}
	
	public static class Float extends Scalar {
		float val;
		Float(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			val = buffer.asFloatBuffer().get(0);
		}
		
		public void setArg(long kernel, int arg){
			cl.errorCheck(CL10.clSetKernelArg1f(kernel, arg, val));
		}
	}
	
	public static class Double extends Scalar {
		double val;
		Double(long p, Context ctx, MemoryType type, ByteBuffer buffer, boolean read, boolean write, boolean useHostPtr,
				boolean useHostMem, boolean copiedHost) {
			super(p, ctx, type, buffer, read, write, useHostPtr, useHostMem, copiedHost);
			val = buffer.asDoubleBuffer().get(0);
		}
		
		public void setArg(long kernel, int arg){
			cl.errorCheck(CL10.clSetKernelArg1d(kernel, arg, val));
		}
	}
}
