package core;
import java.io.IOException;

import com.google.gson.JsonObject;

public interface ControladorJogo {
	public void init(Tratador[] p);
	
	public void terminate();
	
	public String getState(int player);
	
	public void setPendente(JogoPendente jogo);
	
	public JogoPendente getPendente();
	
	public void setGameOver(boolean gameOver);
	
	public boolean isGameOver();
	
	public Jogo getJogo();
	
	public boolean isHuman(int player);
	
	public JsonObject geraJogada();
	
	public boolean fazJogada(JsonObject jogada, int player);
	
	public <T> void send(int player, Mensagem<T> data) throws IOException;
}
