package core;

public class JogoPendente {
	private Jogo jogo;
	private ControladorJogo controlador;
	private Tratador emEspera;
	private Jogador jogador[];
	private Mensagem<?> ultimaMensagem[] = new Mensagem<?>[2];
	
	public JogoPendente(Jogo jogo, Jogador jogador[], ControladorJogo controlador) {
		this.controlador = controlador;
		this.jogador = jogador;
		this.jogo = jogo;
	}
	
	public ControladorJogo getControlador() {
		return controlador;
	}
	public void setControlador(ControladorJogo controlador) {
		this.controlador = controlador;
	}
	public Tratador getEmEspera() {
		return emEspera;
	}
	public void setEmEspera(Tratador emEspera) {
		this.emEspera = emEspera;
	}

	public Jogo getJogo() {
		return jogo;
	}
	
	public Jogador getJogador(int ind) {
		return jogador[ind];
	}
	
	public void setUltimaMensagem(int ind, Mensagem<?> m) {
		ultimaMensagem[ind] = m;
	}
	
	public Mensagem<?> getUltimaMensagem(int ind) {
		return ultimaMensagem[ind];
	}
}
