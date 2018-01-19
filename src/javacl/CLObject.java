package javacl;

public abstract class CLObject {
	long ptr;
	protected OpenCLTools cl;
	
	CLObject(long p){
		ptr = p;
		cl = OpenCLTools.getOpenCLTools();
	}
}
