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

public class FFT1kTest {
	private static final int PLATFORM = 0;
	private static final int DEVICE = 0;
	private static final String OUTPUT_FILE_PATH = "";
	
	public static void main(String[] args) {
		OpenCLTools cl = OpenCLTools.getOpenCLTools();
		Platform platform = cl.getPlatforms().get(PLATFORM);
		Device device = platform.getDevices().get(DEVICE);
		Context context = platform.getContextBuilder().device(device).build();
		
		float[] data = new float[1024];
		for(int i = 0; i < 1024; i++)
			data[i] = (float)i;
		
		CLBuffer realParts = (CLBuffer)context.getMemoryObjectBuilder()
				.floatData(data).memoryType(MemoryType.FLOAT_P).memCopyHost().build();
		CLBuffer imagParts = (CLBuffer)context.getMemoryObjectBuilder().components(1024).memoryType(MemoryType.FLOAT_P).build();
		
		ProgramBuilder progb = context.getProgramBuilder(Input.fileToString("fft1k.cl")).device(device);
		Program program = null;
		try {
			program = progb.build();
		}catch(CLException e) {
			e.printStackTrace();
			System.out.println(progb.lastBuildLog().getLog(device));
			System.exit(1);
		}
		
		Kernel kernel = program.getKernel("fft");
		kernel.setArg("datareal", realParts);
		kernel.setArg("dataimag", imagParts);
		kernel.setArg("scale", 1f);
		
		CommandQueue q = context.getCommandQueueBuilder().device(device).profiling(true).build();
		NDRange range = new NDRangeBuilder(1).globalX(64).localX(64).build();
		
		Event e = q.enqueue(kernel, range);
		q.waitForCompletion();
		
		try {
			realParts.enqueueRead(q, true, 0);
		}catch(CLException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		imagParts.enqueueRead(q, true, 0);
		
		C[] dataC = new C[1024];
		C[] transformed = new C[1024];
		for(int i = 0; i < 1024; i++)
			dataC[i] = new C(i);
		
		long start, time;
		start = System.nanoTime();
		Transformer.FFT(dataC, transformed, -1f, 1f, true);
		time = System.nanoTime() - start;
		
		FloatBuffer clreal = realParts.getHostMemory().asFloatBuffer();
		FloatBuffer climag = imagParts.getHostMemory().asFloatBuffer();
		
		new FileWriter(OUTPUT_FILE_PATH).write(
				(r, c) -> c == 0 ? String.format("%.02f + %.02f i", clreal.get(r), climag.get(r)) : 
					String.format("%.02f + %.02f i", transformed[r].r, transformed[r].i), 1024, 2, "\t\t\t");
		
		System.out.println("OpenCL time (ns)");
		System.out.println(e.getTimeFinished() - e.getTimeStarted());
		System.out.println("\nJava time (ns)");
		System.out.println(time);
	}
}
