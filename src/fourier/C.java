package fourier;

public class C {
	public float r, i;
	private float mag;
	private float arg;
	
	public C(float r, float i) {
		this.r = r;
		this.i = i;
		mag = Float.NaN;
		arg = Float.NaN;
	}
	
	public C(C c) {
		r = c.r;
		i = c.i;
		mag = c.mag;
		arg = c.arg;
	}
	
	public C() {
		mag = Float.NaN;
		arg = Float.NaN;
	}
	
	public C(float r) {
		this.r = r;
		mag = Float.NaN;
		arg = Float.NaN;
	}
	
	public C set(float r, float i) {
		this.r = r;
		this.i = i;
		mag = Float.NaN;
		arg = Float.NaN;
		return this;
	}
	
	public C set(float r) {
		this.r = r;
		i = 0;
		mag = r;
		arg = Float.NaN;
		return this;
	}
	
	public C set(float r, float i, float mag) {
		this.r = r;
		this.i = i;
		this.mag = mag;
		arg = Float.NaN;
		return this;
	}
	
	public C set(float r, float i, float mag, float arg) {
		this.r = r;
		this.i = i;
		this.mag = mag;
		this.arg = arg;
		return this;
	}
	
	public C set(C c) {
		r = c.r;
		i = c.i;
		mag = c.mag;
		arg = c.arg;
		return this;
	}
	
	public C conj() {
		i = -i;
		arg = Float.NaN;
		return this;
	}
	
	public C add(C z) {
		r += z.r;
		i += z.i;
		return this;
	}
	
	public C add(float r, float i) {
		this.r += r;
		this.i += i;
		return this;
	}
	
	public C sub(C z) {
		r -= z.r;
		i -= z.i;
		return this;
	}
	
	public C sub(float r, float i) {
		this.r -= r;
		this.i -= i;
		return this;
	}
	
	public C mul(C z) {
		float t = r * z.r - i * z.i;
		i = r * z.i + i * z.r;
		r = t;
		return this;
	}
	
	public C mul(float r, float i) {
		float t = this.r * r - this.i * i;
		this.i = this.r * i + this.i * r;
		this.r = t;
		return this;
	}
	
	public C mul(float r) {
		this.r *= r;
		i *= r;
		return this;
	}
	
	public float mag() {
		if(Float.isNaN(mag))
			mag = (float)Math.sqrt(i * i + r * r);
		return mag;
	}
	
	public float magSquared() {
		return r * r + i * i;
	}
	
	public float arg() {
		if(Float.isNaN(arg))
			arg = (float)Math.atan2(i, r);
		return arg;
	}
	
	public C expir() {
		i = (float)Math.sin(r);
		r = (float)Math.cos(r);
		return this;
	}
	
	public String toString() {
		return (r != 0 ? r + (i != 0 ? " + ": "" ): "") + (i != 0 ? i + " i" : "") + (r == 0 && i == 0 ? "0" : "");
	}
}
