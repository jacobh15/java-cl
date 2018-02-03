package util;

import java.util.ArrayList;
import java.util.List;

public class StringStuff {
	public static String removeComments(String s) {
		List<Integer> keeps = new ArrayList<>();
		keeps.add(0);
		boolean insideSingleline = false;
		boolean insideMultiline = false;
		for(int i = 0; i < s.length() - 1; i++) {
			if(!insideSingleline && !insideMultiline) {
				if(s.charAt(i) == '/' && s.charAt(i + 1) == '/') {
					insideSingleline = true;
					keeps.add(i);
				}else if(s.charAt(i) == '/' && s.charAt(i + 1) == '*') {
					insideMultiline = true;
					keeps.add(i);
				}
			}else if(insideSingleline) {
				if(s.charAt(i) == '\n') {
					insideSingleline = false;
					keeps.add(i + 1);
				}
			}else {
				if(s.charAt(i) == '*' && s.charAt(i + 1) == '/') {
					insideMultiline = false;
					keeps.add(i + 2);
				}
			}
		}
		keeps.add(s.length());
		String ret = "";
		for(int i = 0; i < keeps.size() / 2; i++) {
			ret += s.substring(keeps.get(2 * i), keeps.get(2 * i + 1));
		}
		return ret;
	}
}
