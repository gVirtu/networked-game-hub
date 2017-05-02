package core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Jogo {
	private String nome;
	private Class<? extends ControladorJogo> controlador;
	private Tratador emEspera = null;
	private Map<Jogador, JogoPendente> pendentes;
	
	public Jogo(String n, Class<? extends ControladorJogo> c) {
		nome=n;
		controlador=c;
		pendentes = new HashMap<Jogador, JogoPendente>();
	}
	
	public String getNome() {
		return nome;
	}
	
	public Class<? extends ControladorJogo> getControlador() {
		return controlador;
	}
	
	public Tratador getEmEspera() {
		return emEspera;
	}
	
	public void setEmEspera(Tratador t) {
		emEspera = t;
	}

	public Map<Jogador, JogoPendente> getPendentes() {
		return pendentes;
	}
	
	public void encerrajogo(JogoPendente j) {
		pendentes.values().removeAll(Collections.singleton(j)); //remove retiraria apenas de um dos jogadores
	}
}
