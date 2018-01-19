package javacl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum MemoryType {
	
	BOOLEAN(1, "bool"), BOOLEAN_P(1, "bool*"),
	CHARACTER(1, "char"),  CHARACTER_P(1, "char*"),
		CHARACTER2(1, "char2"), CHARACTER2_P(1, "char2*"), 
		CHARACTER3(1, "char3"), CHARACTER3_P(1, "char3*"), 
		CHARACTER4(1, "char4"), CHARACTER4_P(1, "char4*"), 
		CHARACTER8(1, "char8"), CHARACTER8_P(1, "char8*"), 
		CHARACTER16(1, "char16"), CHARACTER16_P(1, "char16*"),
	UNSIGNED_CHAR(1, "unsigned char", "uchar"), 
			UNSIGNED_CHAR_P(1, "unsigned char*", "uchar*"), 
		UNSIGNED_CHAR2(1, "uchar2"), UNSIGNED_CHAR2_P(1, "uchar2*"),
		UNSIGNED_CHAR3(1, "uchar3"), UNSIGNED_CHAR3_P(1, "uchar3*"),
		UNSIGNED_CHAR4(1, "uchar4"), UNSIGNED_CHAR4_P(1, "uchar4*"),
		UNSIGNED_CHAR8(1, "uchar8"), UNSIGNED_CHAR8_P(1, "uchar8*"),
		UNSIGNED_CHAR16(1, "uchar16"), UNSIGNED_CHAR16_P(1, "uchar16*"),
	SHORT(2, "short"), SHORT_P(2, "short*"),
		SHORT2(2, "short2"), SHORT2_P(2, "short2*"),
		SHORT3(2, "short3"), SHORT3_P(2, "short3*"),
		SHORT4(2, "short4"), SHORT4_P(2, "short4*"),
		SHORT8(2, "short8"), SHORT8_P(2, "short8*"),
		SHORT16(2, "short16"), SHORT16_P(2, "short16*"),
	UNSIGNED_SHORT(2, "unsigned short", "ushort"),
			UNSIGNED_SHORT_P(2, "unsigned short*", "ushort*"),
		UNSIGNED_SHORT2(2, "ushort2"), UNSIGNED_SHORT2_P(2, "ushort2*"),
		UNSIGNED_SHORT3(2, "ushort3"), UNSIGNED_SHORT3_P(2, "ushort3*"),
		UNSIGNED_SHORT4(2, "ushort4"), UNSIGNED_SHORT4_P(2, "ushort4*"),
		UNSIGNED_SHORT8(2, "ushort8"), UNSIGNED_SHORT8_P(2, "ushort8*"),
		UNSIGNED_SHORT16(2, "ushort16"), UNSIGNED_SHORT16_P(2, "ushort16*"),
	INTEGER(4, "int"), INTEGER_P(4, "int*"),
		INTEGER2(4, "int2"), INTEGER2_P(4, "int2*"),
		INTEGER3(4, "int3"), INTEGER3_P(4, "int3*"),
		INTEGER4(4, "int4"), INTEGER4_P(4, "int4*"),
		INTEGER8(4, "int8"), INTEGER8_P(4, "int8*"),
		INTEGER16(4, "int16"), INTEGER16_P(4, "int16*"),
	UNSIGNED_INTEGER(4, "unsigned int", "uint"), 
			UNSIGNED_INTEGER_P(4, "unsigned int*", "uint*"),
		UNSIGNED_INTEGER2(4, "uint2"), UNSIGNED_INTEGER2_P(4, "uint2*"),
		UNSIGNED_INTEGER3(4, "uint3"), UNSIGNED_INTEGER3_P(4, "uint3*"),
		UNSIGNED_INTEGER4(4, "uint4"), UNSIGNED_INTEGER4_P(4, "uint4*"),
		UNSIGNED_INTEGER8(4, "uint8"), UNSIGNED_INTEGER8_P(4, "uint8*"),
		UNSIGNED_INTEGER16(4, "uint16"), UNSIGNED_INTEGER16_P(4, "uint16*"),
	LONG(8, "long"), LONG_P(8, "long*"),
		LONG2(8, "long2"), LONG2_P(8, "long2*"),
		LONG3(8, "long3"), LONG3_P(8, "long3*"),
		LONG4(8, "long4"), LONG4_P(8, "long4*"),
		LONG8(8, "long8"), LONG8_P(8, "long8*"),
		LONG16(8, "long16"), LONG16_P(8, "long16*"),
	UNSINGED_LONG(8, "unsigned long", "ulong"), 
			UNSIGNED_LONG_P(8, "unsigned long*", "ulong*"),
		UNSIGNED_LONG2(8, "ulong2"), UNSIGNED_LONG2_P(8, "ulong2*"),
		UNSIGNED_LONG3(8, "ulong3"), UNSIGNED_LONG3_P(8, "ulong3*"),
		UNSIGNED_LONG4(8, "ulong4"), UNSIGNED_LONG4_P(8, "ulong4*"),
		UNSIGNED_LONG8(8, "ulong8"), UNSIGNED_LONG8_P(8, "ulong8*"),
		UNSIGNED_LONG16(8, "ulong16"), UNSIGNED_LONG16_P(8, "ulong16*"),
	FLOAT(4, "float"), FLOAT_P(4, "float*"),
		FLOAT2(4, "float2"), FLOAT2_P(4, "float2*"),
		FLOAT3(4, "float3"), FLOAT3_P(4, "float3*"),
		FLOAT4(4, "float4"), FLOAT4_P(4, "float4*"),
		FLOAT8(4, "float8"), FLOAT8_P(4, "float8*"),
		FLOAT16(4, "float16"), FLOAT16_P(4, "float16*"),
	DOUBLE(8, "double"), DOUBLE_P(8, "double*"),
		DOUBLE2(8, "double2"), DOUBLE2_P(8, "double2*"),
		DOUBLE3(8, "double3"), DOUBLE3_P(8, "double3*"),
		DOUBLE4(8, "double4"), DOUBLE4_P(8, "double4*"),
		DOUBLE8(8, "double8"), DOUBLE8_P(8, "double8*"),
		DOUBLE16(8, "double16"), DOUBLE16_P(8, "double16*"),
	HALF(2, "half"), HALF_P(2, "half*"),
		HALF2(2, "half2"), HALF2_P(2, "half2*"),
		HALF3(2, "half3"), HALF3_P(2, "half3*"),
		HALF4(2, "half4"), HALF4_P(2, "half4*"),
		HALF8(2, "half8"), HALF8_P(2, "half8*"),
		HALF16(2, "half16"), HALF16_P(2, "half16*"),
	IMAGE2D(1, "image2d_t"),
	IMAGE3D(1, "image3d_t");
	
	static final int LONG_SIZE = 8;
	private static final Map<String, MemoryType> TOKEN_MAP = new HashMap<>();
	private static boolean init = false;
	private static enum Type {SCALAR, VECTOR, POINTER, IMAGE}
	private static enum Type2 {BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE};
	
	private Type type;
	private Type2 type2;
	private String[] tokens;
	private int numComponents;
	private int bytesPerComponent;
	private int byteSize;
	
	private MemoryType(int compSize, String...tokens){
		this.tokens = tokens;
		bytesPerComponent = compSize;
		String testString = tokens[0];
		if(tokens[0].endsWith("*")){
			type = Type.POINTER;
			testString = testString.substring(0, testString.length() - 1);
		}
		numComponents = 1;
		try{
			numComponents = Integer.parseInt(tokens[0].substring(tokens[0].length() - 1));
			try{
				numComponents = Integer.parseInt(tokens[0].substring(tokens[0].length() - 2));
			}catch(NumberFormatException e){}
			if(type == null)
				type = Type.VECTOR;
		}catch(NumberFormatException e){
			if(type == null){
				if(testString.startsWith("image"))
					type = Type.IMAGE;
				else
					type = Type.SCALAR;
			}
		}
		byteSize = numComponents * bytesPerComponent;
		if(testString.contains("bool")){
			type2 = Type2.BOOLEAN;
		}else if(testString.contains("byte")){
			type2 = Type2.BYTE;
		}else if(testString.contains("short") || testString.contains("half")){
			type2 = Type2.SHORT;
		}else if(testString.contains("int")){
			type2 = Type2.INT;
		}else if(testString.contains("long")){
			type2 = Type2.LONG;
		}else if(testString.contains("float")){
			type2 = Type2.FLOAT;
		}else if(testString.contains("double")){
			type2 = Type2.DOUBLE;
		}
	}
	
	static MemoryType getType(String str){
		if(!init){
			MemoryType[] types = MemoryType.values();
			for(int i = 0; i < types.length; i++){
				for(int j = 0; j < types[i].tokens.length; j++){
					TOKEN_MAP.put(types[i].tokens[j], types[i]);
				}
			}
			init = true;
		}
		return TOKEN_MAP.get(str);
	}
	
	public String[] getOpenCLCTokens(){
		return Arrays.copyOf(tokens, tokens.length);
	}
	
	public String getOpenCLCTokenShort(){
		return tokens[tokens.length - 1];
	}
	
	public String getOpenCLCTokenLong(){
		return tokens[0];
	}
	
	public boolean isScalar(){
		return type == Type.SCALAR;
	}
	
	public boolean isPointer(){
		return type == Type.POINTER;
	}
	
	public boolean isVector(){
		return type == Type.VECTOR;
	}
	
	public boolean isImage(){
		return type == Type.IMAGE;
	}
	
	public boolean isBool(){
		return type2 == Type2.BOOLEAN;
	}
	
	public boolean isByte(){
		return type2 == Type2.BYTE;
	}
	
	public boolean isShort(){
		return type2 == Type2.SHORT;
	}
	
	public boolean isInt(){
		return type2 == Type2.INT;
	}
	
	public boolean isLong(){
		return type2 == Type2.LONG;
	}
	
	public boolean isFloat(){
		return type2 == Type2.FLOAT;
	}
	
	public boolean isDouble(){
		return type2 == Type2.DOUBLE;
	}
	
	public int numComps(){
		return numComponents;
	}
	
	public int bytesPerComp(){
		return bytesPerComponent;
	}
	
	public int sizeInBytes(){
		return byteSize;
	}
}
