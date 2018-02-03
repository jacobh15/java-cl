package test;

import java.nio.FloatBuffer;

import fourier.C;
import fourier.Transformer;
import io.FileWriter;
import io.Input;
import javacl.CLBuffer;
import javacl.CLException;
import javacl.CommandQueue;
import javacl.Context;
import javacl.Device;
import javacl.Event;
import javacl.Kernel;
import javacl.MemoryType;
import javacl.NDRange;
import javacl.NDRangeBuilder;
import javacl.OpenCLTools;
import javacl.Platform;
import javacl.Program;
import javacl.ProgramBuilder;

public class FFT32Test {
	private static final int PLATFORM = 0;
	private static final int DEVICE = 0;
	private static final String OUTPUT_FILE_PATH = "";
	
	public static void main(String[] args) {
		OpenCLTools cl = OpenCLTools.getOpenCLTools();
		Platform platform = cl.getPlatforms().get(PLATFORM);
		Device device = platform.getDevices().get(DEVICE);
		Context context = platform.getContextBuilder().device(device).build();
		
		float[] data = new float[32];
		for(int i = 0; i < 32; i++)
			data[i] = (float)i;
		
		CLBuffer realParts = 
				(CLBuffer)context.getMemoryObjectBuilder().memoryType(MemoryType.FLOAT_P).floatData(data).memCopyHost().build();
		
		CLBuffer imagParts = (CLBuffer)context.getMemoryObjectBuilder().memoryType(MemoryType.FLOAT_P).components(32).build();
		
		ProgramBuilder progb = context.getProgramBuilder(Input.fileToString("fft.cl"));
		Program program = null;
		try {
			program = progb.build();
		}catch(CLException e) {
			e.printStackTrace();
			System.out.println(progb.lastBuildLog().getLog(device));
			System.exit(1);
		}
		
		CommandQueue q = context.getCommandQueueBuilder().profiling(true).build();
		
		Kernel kernel = program.getKernel("fft");
		kernel.setArg("scale", 1f);
		kernel.setArg("datareal", realParts);
		kernel.setArg("dataimag", imagParts);
		
		NDRange range = new NDRangeBuilder(1).globalX(16).localX(16).build();
		
		Event e = q.enqueue(kernel, range);
		q.waitForCompletion();
		
		realParts.enqueueRead(q, true, 0);
		imagParts.enqueueRead(q, true, 0);
		
		FloatBuffer real = realParts.getHostMemory().asFloatBuffer();
		FloatBuffer imag = imagParts.getHostMemory().asFloatBuffer();
		
		C[] dataC = new C[32];
		for(int i = 0; i < 32; i++)
			dataC[i] = new C(i);
		C[] transformed = new C[32];
		long time, start;
		start = System.nanoTime();
		Transformer.FFT(dataC, transformed, -1f, 1f, true);
		time = System.nanoTime() - start;
		
		new FileWriter(OUTPUT_FILE_PATH).write(
				(r, c) -> c == 0 ? new C(real.get(r), imag.get(r)).toString() : transformed[r].toString(), 32, 2, "\t\t");
		
		System.out.println("OpenCL time (ns)");
		System.out.println(e.getTimeFinished() - e.getTimeStarted());
		
		System.out.println("\nJava Time (ns)");
		System.out.println(time);
	}
}
