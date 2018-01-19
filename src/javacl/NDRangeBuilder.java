package javacl;

public class NDRangeBuilder {
	int globalX, globalY, globalZ;
	int localX, localY, localZ;
	int dims;
	
	public NDRangeBuilder(int dimensions){
		dims = dimensions;
		clear();
	}
	
	public NDRange build(){
		if(!allDivide()){
			System.err.println("Invalid NDRange; not all local dimensions divide their corresponding global dimensions.");
			clear();
			return null;
		}
		
		NDRange range = new NDRange(globalX, globalY, globalZ, localX, localY, localZ, dims);
		clear();
		return range;
	}
	
	private boolean allDivide(){
		return !( 	(localX == 0 || localY == 0 || localZ == 0) 			||
					(localX > 0 && localX * (globalX / localX) != globalX) 	||
					(localY > 0 && localY * (globalY / localY) != globalY)	||
					(localZ > 0 && localZ * (globalZ / localZ) != globalZ)	);
	}
	
	public void clear(){
		globalX = 0;
		globalY = 0;
		globalZ = 0;
		localX = -1;
		localY = -1;
		localZ = -1;
	}
	
	public NDRangeBuilder dimensions(int d){
		dims = Math.max(1, Math.min(3, d));
		if(dims < 3){
			globalZ = 0;
			localZ = -1;
			if(dims < 2){
				globalY = 0;
				localY = -1;
			}
		}
		return this;
	}
	
	public NDRangeBuilder globalX(int gx){
		globalX = gx;
		return this;
	}
	
	public NDRangeBuilder globalY(int gy){
		globalY = gy;
		dims = Math.max(dims, 2);
		return this;
	}
	
	public NDRangeBuilder globalZ(int gz){
		globalZ = gz;
		dims = 3;
		return this;
	}
	
	public NDRangeBuilder localX(int lx){
		localX = lx;
		return this;
	}
	
	public NDRangeBuilder localY(int ly){
		localY = ly;
		dims = Math.max(dims, 2);
		return this;
	}
	
	public NDRangeBuilder localZ(int lz){
		localZ = lz;
		dims = 3;
		return this;
	}
}
