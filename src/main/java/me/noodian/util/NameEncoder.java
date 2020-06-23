package me.noodian.util;

public class NameEncoder {

	// Add a hex value to the end of the string
	public static String encode(int value) {
		StringBuilder out = new StringBuilder();
		String hex = Integer.toHexString(value);
		for (int i = 0; i < hex.length(); i++) {
			out.append("§").append(hex.charAt(i));
		}

		return out.toString();
	}

	// Read the hex value at the end of the string
	public static int decode(String string) {
		StringBuilder hex = new StringBuilder();
		while (string.charAt(string.length()-2) == '§') {
			hex.append(string.charAt(string.length()-1));
			string = string.substring(0, string.length()-2);
		}

		if (hex.length() == 0) return -1;
		return Integer.parseInt(hex.toString(), 16);
	}

	// Remove the hex value at the end of the string
	public static String clear(String string) {
		while (string.charAt(string.length()-2) == '§') {
			string = string.substring(0, string.length()-2);
		}
		return string;
	}
}
