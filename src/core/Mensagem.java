package core;

public class Mensagem<T> {
	private Header header;
	protected T dado;
	
	public Mensagem(Header h, T d) {
		header = h;
		dado = d;
	}
	
	public Header getHeader() {
		return header;
	}
	
	public T getDado() {
		return dado;
	}
}
