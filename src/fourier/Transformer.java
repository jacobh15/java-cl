package fourier;

public class Transformer {
	
	private C[] data;
	private C[] transformed;
	private float[] transformedMags;
	private float twoPiOverN;
	private C[][] data2;
	private C[][] transformed2;
	private float[][] transformed2Mags;
	private int N;
	
	public Transformer(float[] data) {
		this.data = new C[data.length];
		for(int i = 0; i < data.length; i++)
			this.data[i] = new C(data[i]);
		twoPiOverN = (float)(Math.PI * 2 /(double)data.length);
		N = data.length;
	}
	
	public Transformer(C[] data) {
		this.data = data;
		twoPiOverN = (float)(Math.PI * 2 / (double)data.length);
		N = data.length;
	}
	
	public Transformer(float[][] data) {
		data2 = new C[data.length][data.length];
		for(int i = 0; i < data.length; i++) {
			for(int j = 0; j < data.length; j++) {
				data2[i][j] = new C(data[i][j]);
			}
		}
		twoPiOverN = (float)(Math.PI * 2 / (double)data.length);
		N = data.length;
	}
	
	public Transformer(C[][] data) {
		data2 = data;
		twoPiOverN = (float)(Math.PI * 2 / (double)data.length);
		N = data.length;
	}
	
	public Transformer DFT() {
		transformed = new C[data.length];
		C temp = new C();
		for(int n = 0; n < data.length; n++) {
			transformed[n] = new C();
			float tpoNn = twoPiOverN * (float)n;
			for(int k = 0; k < data.length; k++) {
				transformed[n].add(temp.set(-tpoNn * (float)k).expir().mul(data[k]));
			}
		}
		return this;
	}
	
	public Transformer DFTI() {
		transformed = new C[data.length];
		C temp = new C();
		for(int n = 0; n < data.length; n++) {
			transformed[n] = new C();
			float tpoNn = twoPiOverN * (float)n;
			for(int k = 0; k < data.length; k++) {
				transformed[n].add(temp.set(tpoNn * (float)k).expir().mul(data[k]));
			}
			transformed[n].mul((float)(1 / (double)N()));
		}
		return this;
	}
	
	public Transformer FFT() {
		C[] t = new C[N()];
		FFT(data, t, -1f, 1f, true);
		transformed = t;
		
		return this;
	}
	
	public Transformer FFTI() {
		C[] t = new C[N()];
		FFT(data, t, 1f, 1f / (float)N(), true);
		transformed = t;
		
		return this;
	}
	
	public Transformer FFT2D() {
		C[][] t = new C[N()][N()];
		FFT2D(data2, t, -1f, 1f, true);
		transformed2 = t;
		
		return this;
	}
	
	public Transformer FFT2DI() {
		C[][] t = new C[N()][N()];
		FFT2D(data2, t, 1f, 1f / (float)N(), true);
		transformed2 = t;
		
		return this;
	}
	
	private void calcMagnitudes() {
		transformedMags = new float[data.length];
		for(int i = 0; i < data.length; i++)
			transformedMags[i] = transformed[i].mag();
	}
	
	private void calcMagnitudes2() {
		transformed2Mags = new float[data2.length][data2.length];
		for(int i = 0; i < data2.length; i++) {
			for(int j = 0; j < data2.length; j++) {
				transformed2Mags[i][j] = transformed2[i][j].mag();
			}
		}
	}
	
	public C[] transformed() {
		return transformed;
	}
	
	public C[][] transformed2D() {
		return transformed2;
	}
	
	public float[] transformedMags() {
		if(transformedMags == null)
			calcMagnitudes();
		return transformedMags;
	}
	
	public float[][] transformedMags2D(){
		if(transformed2Mags == null)
			calcMagnitudes2();
		return transformed2Mags;
	}
	
	public C[] data() {
		return data;
	}
	
	public C[][] data2D(){
		return data2;
	}
	
	public int N() {
		return N;
	}
	
	public Transformer normalize() {
		float Ni = (float)(1 / (double)N());
		for(int i = 0; i < N(); i++)
			transformed[i].mul(Ni);
		if(transformedMags != null)
			for(int i = 0; i < N(); i++)
				transformedMags[i] *= (Ni);
		return this;
	}
	
	public Transformer normalize2D() {
		float Ni2 = (float)(1/(double)N/(double)N);
		for(int i = 0; i < N; i++) {
			for(int j = 0; j < N; j++) {
				transformed2[i][j].mul(Ni2);
			}
		}
		if(transformed2Mags != null) {
			for(int i = 0; i < N; i++) {
				for(int j = 0; j < N; j++) {
					transformed2Mags[i][j] *= Ni2;
				}
			}
		}
		return this;
	}
	
	private static void FFT(C[][] data, C[][] transformed, float twiddleMul, float scale, boolean newTransform, int row, int col) {
		if(row == -1) {
			int N = data.length;
			C[] twiddleFactors = new C[N / 2];
			for(int i = 0; i < twiddleFactors.length; i++)
				twiddleFactors[i] = new C();
			int decimations = calcDecimations(N);
			
			int firstT, otherT, firstI, otherI;
			
			if(newTransform) {
				for(int pair = 0; pair < N/2; pair++) {
					firstT = 2 * pair;
					otherT = firstT + 1;
					firstI = reversal(firstT, decimations);
					otherI = reversal(otherT, decimations);
					transformed[firstT][col] = new C(data[firstI][col]).add(data[otherI][col]);
					transformed[otherT][col] = new C(data[firstI][col]).sub(data[otherI][col]);
				}
			}else {
				for(int pair = 0; pair < N/2; pair++) {
					firstT = 2 * pair;
					otherT = firstT + 1;
					firstI = reversal(firstT, decimations);
					otherI = reversal(otherT, decimations);
					transformed[firstT][col].set(data[firstI][col]).add(data[otherI][col]);
					transformed[otherT][col].set(data[firstI][col]).sub(data[otherI][col]);
				}
			}
			
			for(int d = 1; d <= decimations; d++) {
				int twiddles = 2 << d;
				calcTwiddles(twiddleFactors, twiddles, twiddleMul);
				
				C temp = new C();
				C temp2 = new C();
				int first;
				int pairOffset = 1 << d;
				int bundles = N / pairOffset / 2;
				for(int bundle = 0; bundle < bundles; bundle++) {
					first = pairOffset * bundle * 2;
					for(int pair = 0; pair < pairOffset; pair++) {
						temp2.set(transformed[first + pairOffset][col]).mul(twiddleFactors[pair]);
						temp.set(transformed[first][col]).add(temp2);
						transformed[first + pairOffset][col].set(transformed[first][col].sub(temp2));
						transformed[first][col].set(temp);
						first++;
					}
				}
			}
			
			if(scale != 1f)
				for(int i = 0; i < N; i++) {
					transformed[i][col].mul(scale);
				}
		}else {
			FFT(data[row], transformed[row], twiddleMul, scale, newTransform);
		}
	}
	
	public static void FFT(C[] data, C[] transformed, float twiddleMul, float scale, boolean newTransform) {
		int N = data.length;
		C[] twiddleFactors = new C[N / 2];
		for(int i = 0; i < twiddleFactors.length; i++)
			twiddleFactors[i] = new C();
		int decimations = calcDecimations(N);
		
		int firstT, otherT, firstI, otherI;
		
		if(newTransform) {
			for(int pair = 0; pair < N/2; pair++) {
				firstT = 2 * pair;
				otherT = firstT + 1;
				firstI = reversal(firstT, decimations);
				otherI = reversal(otherT, decimations);
				transformed[firstT] = new C(data[firstI]).add(data[otherI]);
				transformed[otherT] = new C(data[firstI]).sub(data[otherI]);
			}
		}else {
			for(int pair = 0; pair < N/2; pair++) {
				firstT = 2 * pair;
				otherT = firstT + 1;
				firstI = reversal(firstT, decimations);
				otherI = reversal(otherT, decimations);
				transformed[firstT].set(data[firstI]).add(data[otherI]);
				transformed[otherT].set(data[firstI]).sub(data[otherI]);
			}
		}
		
		for(int d = 1; d <= decimations; d++) {
			int twiddles = 2 << d;
			calcTwiddles(twiddleFactors, twiddles, twiddleMul);
			
			C temp = new C();
			C temp2 = new C();
			int first;
			int pairOffset = 1 << d;
			int bundles = N / pairOffset / 2;
			for(int bundle = 0; bundle < bundles; bundle++) {
				first = pairOffset * bundle * 2;
				for(int pair = 0; pair < pairOffset; pair++) {
					temp2.set(transformed[first + pairOffset]).mul(twiddleFactors[pair]);
					temp.set(transformed[first]).add(temp2);
					transformed[first + pairOffset].set(transformed[first].sub(temp2));
					transformed[first].set(temp);
					first++;
				}
			}
		}
		
		if(scale != 1f)
			for(int i = 0; i < N; i++) {
				transformed[i].mul(scale);
			}
	}
	
	public static void FFT2D(C[][] data, C[][] transformed, float twiddleMul, float scale, boolean newTransform) {
		C[][] t2 = new C[data.length][data.length];
		for(int i = 0; i < data.length; i++) {
			FFT(data, t2, twiddleMul, scale, true, -1, i);
		}
		for(int i = 0; i < data.length; i++) {
			FFT(t2, transformed, twiddleMul, scale, newTransform, i, -1);
		}
	}
	
	
	private static int calcDecimations(int n) {
		int decimations = 0;
		while(n > 2) {n >>= 1; decimations++;}
		return decimations;
	}
	
	private static int reversal(int n, int decimations) {
		return Integer.reverse(n) >>> (31 - decimations);
	}
	
	private static void calcTwiddles(C[] twiddleFactors, int twiddles, float mul) {
		float twoPiOver_NOver_TwoToTheDepth__ = (float)(2 * Math.PI / (double)twiddles);
		twiddles >>= 1;
		for(int n = 0; n < twiddles; n++) {
			twiddleFactors[n].set(mul * twoPiOver_NOver_TwoToTheDepth__ * n).expir();
		}
	}
}
