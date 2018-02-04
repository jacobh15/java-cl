package test;

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

public class TransposeTest {
	
	private static final int PLATFORM = 0;
	private static final int DEVICE = 0;
	private static final String OUTPUT_FILE_START = "C:/Users/hauckjp/Documents/transpose";
	private static final int SIZE = 1024;
	private static final int SWITCHES_PER_WORK_ITEM = 1;
	
	public static void main(String[] args) {
		OpenCLTools cl = OpenCLTools.getOpenCLTools();
		
		Platform platform = cl.getPlatforms().get(PLATFORM);
		Device device = platform.getDevices().get(DEVICE);
		System.out.println(device.getName());
		
		Context context = platform.getContextBuilder().device(device).build();
		
		CommandQueue q = context.getCommandQueueBuilder().profiling(true).build();
		Program program = null;
		ProgramBuilder progb = context.getProgramBuilder(Input.fileToString("transpose.cl"));
		try {
			program = progb.options("-D SIZE=" + SIZE + " -D NUM_SWITCHES=" + SWITCHES_PER_WORK_ITEM).build();
		}catch(CLException e) {
			System.out.println(progb.lastBuildLog().getLog(device));
			e.printStackTrace();
			OpenCLTools.closeCL();
			System.exit(1);
		}
		
		float[] data = new float[SIZE * SIZE];
		for(int i = 0; i < SIZE * SIZE; i++)
			data[i] = (float)(Math.random() * 10);
		CLBuffer toTranspose = (CLBuffer)context.getMemoryObjectBuilder()
				.memoryType(MemoryType.FLOAT_P).floatData(data).memCopyHost().build();
		
		Kernel kernel = program.getKernel("transpose");
		kernel.setArg("matrix", toTranspose);
		NDRange range = new NDRangeBuilder(1).globalX(SIZE * (SIZE - 1) / 2 / SWITCHES_PER_WORK_ITEM).localX(32).build();
		
		Event e = q.enqueue(kernel, range);
		q.waitForCompletion();
		System.out.println((e.getTimeFinished() - e.getTimeStarted()) + " ns in OpenCL");
		
		toTranspose.enqueueRead(q, true, 0);
		
		new FileWriter(OUTPUT_FILE_START + "0.txt").write((r, c) -> String.format("%.02f", data[r * SIZE + c]), SIZE, SIZE, "\t");
		
		long start, time;
		start = System.nanoTime();
		for(int r = 0; r < SIZE - 1; r++) {
			for(int c = r; c < SIZE; c++) {
				float tmp = data[r * SIZE + c];
				data[r * SIZE + c] = data[c * SIZE + r];
				data[c * SIZE + r] = tmp;
			}
		}
		time = System.nanoTime() - start;
		System.out.println(time + " ns in Java");
		
		new FileWriter(OUTPUT_FILE_START + "1.txt").write((r, c) -> String.format("%.02f", data[r * SIZE + c]), SIZE, SIZE, "\t");
		new FileWriter(OUTPUT_FILE_START + "2.txt").write(
				(r, c) -> String.format("%.02f", toTranspose.getHostMemory().asFloatBuffer().get(r * SIZE + c)), SIZE, SIZE, "\t");
		
		OpenCLTools.closeCL();
		
		int errors = 0;
		for(int r = 0; r < SIZE; r++) {
			for(int c = 0; c < SIZE; c++) {
				if(Math.abs(data[r * SIZE + c] - toTranspose.getHostMemory().asFloatBuffer().get(r * SIZE + c)) > .01f) {
					errors += 1;
				}
			}
		}
		System.out.println(errors + " errors");
	}
}
