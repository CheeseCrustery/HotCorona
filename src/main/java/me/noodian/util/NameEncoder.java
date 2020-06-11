package me.noodian.util;

public class NameEncoder {

	public static String Encode(int value) {
		String out = "";
		String hex = Integer.toHexString(value);
		for (int i = 0; i < hex.length(); i++) {
			out += "§" + hex.charAt(i);
		}

		return out;
	}

	public static int Decode(String string) {
		String hex = "";
		while (string.charAt(string.length()-2) == '§') {
			hex += string.charAt(string.length()-1);
			string = string.substring(0, string.length()-2);
		}

		if (hex == "") return -1;
		return Integer.parseInt(hex, 16);
	}

	public static String Clear(String string) {
		while (string.charAt(string.length()-2) == '§') {
			string = string.substring(0, string.length()-2);
		}
		return string;
	}
}
