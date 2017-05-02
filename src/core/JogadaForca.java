package core;

public class JogadaForca {
	private char c ;
	
	public JogadaForca(char c) {
		setC(c);
	}
	
	public String toString() {
		return "("+c+")";
	}

	public char getC() {
		return c;
	}

	public void setC(char c) {
		this.c = c;
	}
}
