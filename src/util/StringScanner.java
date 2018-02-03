package util;

public class StringScanner {
	public static final int NUMBERS = 1, UPPER_CASE = 2, LOWER_CASE = 4, ALL_ALPHA = 8, WHITESPACE = 16;
	
	private int position;
	private int marker;
	private String value;
	
	public StringScanner(String s) {
		value = s;
		position = 0;
		marker = 0;
	}
	
	public String nextToken() {
		return nextToken(ALL_ALPHA, "_", ALL_ALPHA | NUMBERS, "_");
	}
	
	public String nextToken(int charsetStart, String otherStartChars) {
		return nextToken(charsetStart, otherStartChars, ALL_ALPHA | NUMBERS, "_");
	}
	
	public String nextToken(int charsetStart, String otherStartChars, int charsetContain, String otherContainChars) {
		if(!hasNext())
			return "";
		
		while(!inCharset(charsetStart, value.charAt(position)) && !inCharset(otherStartChars, value.charAt(position))) {
			position++;
			if(!hasNext())
				return "";
		}
		int start = position;
		while(inCharset(charsetContain, value.charAt(position)) || inCharset(otherContainChars, value.charAt(position))) {
			position++;
			if(!hasNext())
				break;
		}
		return value.substring(start, position);
	}
	
	public StringScanner advanceWhile(int charset, String chars) {
		if(!hasNext())
			return this;
		
		while(inCharset(charset, value.charAt(position)) || inCharset(chars, value.charAt(position))) {
			position++;
			if(!hasNext())
				break;
		}
		return this;
	}
	
	public StringScanner advanceWhile(String chars) {
		return advanceWhile(0, chars);
	}
	
	public StringScanner advanceWhile(int charset) {
		return advanceWhile(0, "");
	}
	
	public StringScanner advanceTill(int charset, String chars) {
		if(!hasNext())
			return this;
		
		while(!inCharset(charset, value.charAt(position)) && !inCharset(chars, value.charAt(position))){
			position++;
			if(!hasNext())
				break;
		}
		return this;
	}
	
	public StringScanner advanceTill(int charset) {
		return advanceTill(charset, "");
	}
	
	public StringScanner advanceTill(String chars) {
		return advanceTill(0, chars);
	}
	
	public StringScanner advanceTillClose(String open, String close) {
		if(!hasNext())
			return this;
		int parity = 1;
		while(parity > 0) {
			if(value.charAt(position) == open.charAt(0)) {
				parity++;
			}
			if(value.charAt(position) == close.charAt(0)) {
				parity--;
			}
			position++;
			if(!hasNext())
				return this;
		}
		return this;
	}
	
	public boolean contains(int chars, char c) {
		for(int i = position; i < Math.min(position + chars, value.length()); i++) {
			if(value.charAt(i) == c)
				return true;
		}
		return false;
	}
	
	public int markerPositionRelative() {
		return (int)Math.signum(marker - position);
	}
	
	public int position() {
		return position;
	}
	
	public StringScanner position(int p) {
		position = p;
		return this;
	}
	
	public StringScanner reset() {
		position = 0;
		return this;
	}
	
	public StringScanner mark() {
		marker = position;
		return this;
	}
	
	public StringScanner markAndSetToMarker() {
		int mark = position;
		position = marker;
		marker = mark;
		return this;
	}
	
	public StringScanner setToMarker() {
		position = marker;
		return this;
	}
	
	public StringScanner rewind(int chars) {
		position -= chars;
		return this;
	}
	
	public StringScanner advance(int chars) {
		position += chars;
		return this;
	}
	
	public char nextChar() {
		position++;
		return value.charAt(position - 1);
	}
	
	public boolean hasNext() {
		return position < value.length();
	}
	
	public String markedString() {
		return value.substring(marker, position);
	}
	
	public static boolean inCharset(int charset, char c) {
		return ((charset & NUMBERS) > 0 && (c == '0' || (c >= '1' && c <= '9')))
				|| ((charset & UPPER_CASE) > 0 && c >= 'A' && c <= 'Z')
				|| ((charset & LOWER_CASE) > 0 && c >= 'a' && c <= 'z')
				|| ((charset & ALL_ALPHA) > 0 && ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')))
				|| ((charset & WHITESPACE) > 0 && (c == ' ' || c == '\t' || c == '\n'));
	}
	
	public static boolean inCharset(String chars, char c) {
		for(int i = 0; i < chars.length(); i++) {
			if(chars.charAt(i) == c)
				return true;
		}
		return false;
	}
}
