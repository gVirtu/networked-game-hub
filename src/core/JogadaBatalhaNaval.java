package core;

public class JogadaBatalhaNaval {
	private int row, col, direction;
	
	public JogadaBatalhaNaval(int r, int c, int d) {
		setRow(r); setCol(c); setDirection(d);
	}

	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}
	
	public String toString() {
		return "("+col+","+row+") - "+direction;
	}
}
