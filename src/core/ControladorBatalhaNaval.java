package core;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonObject;

public class ControladorBatalhaNaval implements ControladorJogo, Runnable {
	public static enum Celula { 
		AGUA(-1), PORTAAVIOES(0), NAVIOGUERRA(1), CRUZADOR(2), SUBMARINO(3), DESTRUIDOR(4), ATINGIDO(-100), ERRADO(-99);
	
		private int valor;
		private Celula(int v) {
			valor=v;
		}
		
		public int getValor() {
			return valor;
		}
	};
	
	private class Coordenada {
		public int x, y;
		
		public Coordenada(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public int getX() { return x; }
		public int getY() { return y; }
		
		//Métodos específicos para determinar o ponto de ataque de partidas contra o computador..
		public int getAdjRank() {
			int tot = 0;
			int val = getCampo(0, x, y);
			int valAdj;
			int colInc; 
			int rowInc; 
			if (val < Celula.AGUA.getValor()) return -1; //Nunca atirar aqui
			for(int dir = 0; dir < 360; dir += 90) {
				for(int dist = 1; dist < 3; ++dist) {
					colInc = dist * (int) Math.round(Math.cos((dir * Math.PI) / (180.0)));
					rowInc = dist * (int) -Math.round(Math.sin((dir * Math.PI) / (180.0)));
					valAdj = getCampo(0, x+colInc, y+rowInc);
					if (valAdj <= Celula.ATINGIDO.getValor()) { //Algum tipo de navio atingido se encontra aqui
						if (frota[0][getNavioAtingido(valAdj)] > 0) { //Não foi totalmente destruído
							tot += 1000; //Mais prioridade
						}
					} else if (valAdj == Celula.ERRADO.getValor()) { break; } //Não vale a pena tentar essa direção
				}
			}
			if (tot==0) {
				//Segunda passagem caso não tenha nenhum semi-destruído por perto
				for(int dir = 0; dir < 360; dir += 90) {
					for(int dist = 1; dist < 3; ++dist) {
						colInc = dist * (int) Math.round(Math.cos((dir * Math.PI) / (180.0)));
						rowInc = dist * (int) -Math.round(Math.sin((dir * Math.PI) / (180.0)));
						valAdj = getCampo(0, x+colInc, y+rowInc);
						if (valAdj != Celula.ERRADO.getValor()) { //Algum tipo de navio atingido se encontra aqui
							tot += 50; //Mais prioridade aos lugares longe de tentativas frustradas
						}
					}
				}
			}
			
			if (tot==0)
				tot += new Random().nextInt(100); //Desempate aleatório
			else
				tot += new Random().nextInt(25); //Desempate aleatório
			return tot;
		}		
	}
	
	private final int width=10, height=10;
	private final int tamanhos[] = {5, 4, 3, 3, 2};
	private int frota[][] = new int[2][5];
	private int campo[][][] = new int[2][height][width];
	private Tratador[] players = new Tratador[2];
	private String shipNames[] = {"Porta-aviões", "Navio de Guerra", "Cruzador", "Submarino", "Destruidor"};
	private String playerNames[] = new String[2];
	private List<Map<Integer, Character>> celulaDisplay = new ArrayList<Map<Integer, Character>>();
	private int proxJogador;
	private boolean gameOver = false;
	private boolean multiplayer = false;
	private Timer turnoTimeout;
	private Timer cpuTimer;
	private TimerTask timeoutTask;
	private long timeoutTime = 300000; //5 min
	private boolean buildPhase = true;
	private int builtCount[] = {0, 0};
	private long cpuDelay = 3000; //3 seg
	private JogoPendente pendente;
	private Jogo jogo;
	private final int RANDOMIZE = 100;
	
	public void init(Tratador[] p) {
		multiplayer = p.length>1;
		for(int i=0;i<p.length;++i) {
			players[i] = p[i];
			playerNames[i] = p[i].getNome();
		}
		
		if (!multiplayer) playerNames[1] = "Computador";
	}
	
	public <T> void send(int player, Mensagem<T> data) throws IOException {
		if (isHuman(player)) {
			Comunicador.send(players[player].out, data);
			if (multiplayer)
				getPendente().setUltimaMensagem(player, data);
		} else {
			//Intercepta mensagens a serem enviadas a um jogador não-humano e responde adequadamente
			if (data.getHeader() == Header.M_BATALHANAVAL_BEGINTURN || data.getHeader() == Header.M_BATALHANAVAL_BUILDPROMPT) {	
				cpuTimer = new Timer();
				cpuTimer.schedule(new TimerTask() {
											@Override
											public void run() {
												fazJogada(geraJogada(), player);
											}
										}, cpuDelay);
			}
		}
	}
	
	public String getState(int player) {
		if (isGameOver()) return "";
		int opponent = (player+1)%2;
		StringBuilder sb = new StringBuilder();
		sb.append(playerNames[player]);
		sb.append(" x ");
		sb.append(playerNames[opponent]);
		sb.append("\r\n\r\n");
		sb.append("  0123456789     0123456789\r\n");
		for(int i=0;i<10;++i) {
			sb.append((char) ('A' + i));
			sb.append(" ");
			for(int j=0;j<10;++j) {
				sb.append(celulaDisplay.get(0).get(campo[player][i][j]));
			}
			sb.append("   ");
			sb.append((char) ('A' + i));
			sb.append(" ");
			for(int j=0;j<10;++j) {
				sb.append(celulaDisplay.get(1).get(campo[1-player][i][j]));
			}
			sb.append("\r\n");
		}
		sb.append("\r\n");
		if (builtCount[player] < tamanhos.length) {
			sb.append("Digite a coordenada (e.g.: A1, D5, F0) onde deseja começar a construir o seu ");
			sb.append(shipNames[builtCount[player]]);
			sb.append(" ("); sb.append(tamanhos[builtCount[player]]);
			sb.append(" espaços) ou digite R para posicionar todos os navios restantes randomicamente: \r\n");
		} else if (buildPhase) {
			sb.append("Tudo pronto. Aguardando "+playerNames[opponent]+" ficar pronto(a)...");
		}
		return sb.toString();
	}
	
	public boolean isValidBuild(int player, int x, int y, int direction) {
		int colInc = (int) Math.round(Math.cos((direction * Math.PI) / (180.0)));
		int rowInc = (int) -Math.round(Math.sin((direction * Math.PI) / (180.0)));
		int sz = tamanhos[builtCount[player]];
		for(int i=0; i<sz; ++i) {
			if (x < 0 || y < 0 || x >= width || y >= height || campo[player][y][x] != Celula.AGUA.getValor()) return false;
			x += colInc; y += rowInc;
		}
		return true;
	}
	
	public JsonObject geraJogada() { //Para partidas contra o computador..
		boolean risky = new Random().nextInt(100) > 94;
		int x=0, y=0, d=0;
		Mensagem<JogadaBatalhaNaval> data;
		Header hh;
		
		if (buildPhase) {
			hh = Header.M_BATALHANAVAL_BUILDACTION;
			//Gera configurações aleatórias até sair uma válida
			do {
				x = new Random().nextInt(width);
				y = new Random().nextInt(height);
				d = new Random().nextInt(4);
				d *= 90;
			} while(!isValidBuild(1,x,y,d));
		} else {
			hh = Header.M_BATALHANAVAL_ATTACKACTION;
			if (risky) {
				System.out.println("Risky play..");
				do {
					x = new Random().nextInt(width);
					y = new Random().nextInt(height);
				} while(getCampo(0, x, y) != Celula.AGUA.getValor());
			} else {
				Coordenada best = new Coordenada(0, 0);
				int bestRank = -1, attemptRank = -1;
				System.out.println("Analytic play..");
				for(int i=0; i<width; ++i) {
					for(int j=0; j<height; ++j) {
						if (getCampo(0, i, j) >= Celula.AGUA.getValor()) {
							Coordenada attempt = new Coordenada(i, j);
							attemptRank = attempt.getAdjRank();
							//System.out.println("("+i+","+j+") = "+attemptRank); 
							if (attemptRank > bestRank) {
								best = attempt;
								bestRank = attemptRank;
							}
						}
					}
				}
				x = best.getX();
				y = best.getY();
			}
			
			//System.out.println("Chose ("+x+","+y+")");
		}

		data = new Mensagem<JogadaBatalhaNaval>(hh, new JogadaBatalhaNaval(y,x,d));
		return Comunicador.getGSON().toJsonTree(data).getAsJsonObject();
	}
	
	public void buildShip(int player, int xx, int yy, int dd) {
		int sz = tamanhos[builtCount[player]];
		int colInc = (int) Math.round(Math.cos((dd * Math.PI) / (180.0)));
		int rowInc = (int) -Math.round(Math.sin((dd * Math.PI) / (180.0)));
		for(int i=0; i<sz; ++i) {
			campo[player][yy][xx] = builtCount[player];
			xx += colInc; yy += rowInc;
		}
		builtCount[player]++;
	}
	
	public boolean fazJogada(JsonObject jogada, int player) {
		if (isGameOver()) return false;
		Header h = Comunicador.getHeader(jogada);
		JogadaBatalhaNaval data = Comunicador.getDado(jogada, JogadaBatalhaNaval.class);
		System.out.println("fazJogada - P"+player+" - "+h+" - "+data);
		
		switch(h) {
			case M_BATALHANAVAL_BUILDACTION: {
				if (buildPhase) { //Não permite construir quando o jogo já começou!
					boolean valid = true;
					int xx = data.getCol();
					int yy = data.getRow();
					Header hh = Header.M_BATALHANAVAL_BUILDPROMPT;
					if (xx == RANDOMIZE && yy == RANDOMIZE) {
						//Constroi todos aleatorios
						int dd;
						while(builtCount[player] < tamanhos.length) {
							do {
								xx = new Random().nextInt(width);
								yy = new Random().nextInt(height);
								dd = new Random().nextInt(4);
								dd *= 90;
							} while(!isValidBuild(player,xx,yy,dd));
							buildShip(player, xx, yy, dd);
						}
					} else {
						//Testar todas as casas que serão ocupadas pelo navio
						int dd = data.getDirection();
						valid = (dd%90 == 0 && isValidBuild(player, xx, yy, dd));
						if (valid) buildShip(player, xx, yy, dd);
					}
					
					if (valid) {			
						
						if (builtCount[0] >= tamanhos.length && builtCount[1] >= tamanhos.length) {
							//Ambos os jogadores prontos
							buildPhase = false;
							nextTurn("O jogo começou! É a vez de "+playerNames[getProxJogador()]+".", "O jogo começou! É sua vez! Digite a coordenada (e.g.: A1, D5, F0) onde deseja atirar no campo do oponente: \r\n");
							return true;
						}
						
						if (builtCount[player] >= tamanhos.length)
							hh = Header.M_BATALHANAVAL_WAIT;
					}
					
					try {
						send(player, new Mensagem<String>(hh, 
										(valid)?(getState(player)):(getState(player)+"\r\nNão foi possível usar essas coordenadas.")));
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					return valid;
				}
			}
			break;
			
			case M_BATALHANAVAL_ATTACKACTION: {
				//Impede o próximo jogador de jogar no turno do atual
				if (player != getProxJogador()) {
					int opponent = (player+1)%2;
					int xx = data.getCol(); int yy = data.getRow();
					int cel = campo[opponent][yy][xx];
					
					if (cel <= Celula.ERRADO.getValor()) { //Já tentei aqui
						try {
							send(player, new Mensagem<String>(Header.M_BATALHANAVAL_BEGINTURN, (getState(player)+"\r\nVocê já atirou nessas coordenadas.")));
						} catch (Exception e) {
							e.printStackTrace();
						}
						return false;
					} else {
						StringBuilder sb = new StringBuilder();
						
						sb.append(playerNames[player]);
						sb.append(" atirou em ");
						sb.append((char) ('A'+data.getRow()));
						sb.append((char) ('0'+data.getCol()));
						sb.append("!\r\n");
						
						StringBuilder osb = new StringBuilder(sb);
						
						if (cel<0 || cel>=tamanhos.length) {
							//Não acertou
							if (cel<0 && cel>Celula.ATINGIDO.getValor())
								campo[opponent][yy][xx] = Celula.ERRADO.getValor();
							
							sb.append("SPLASH! Nada foi acertado...\r\n");
							osb.append("SPLASH! Por sorte, foi direto ao mar...\r\n");	
						} else {
							//Acertou
							campo[opponent][yy][xx] = Celula.ATINGIDO.getValor()-campo[opponent][yy][xx];
							--frota[opponent][cel];
							if (frota[opponent][cel] <= 0) {
								sb.append("BOOOM! Parabéns, você destruiu um ");
								sb.append(shipNames[cel]);
								sb.append("!\r\n");
								osb.append("BOOOM! Ah não! O seu ");
								osb.append(shipNames[cel]);
								osb.append(" está completamente destruído!\r\n");
							} else {
								sb.append("BOOM! Você acertou alguma coisa!\r\n");
								osb.append("BOOM! O seu ");
								osb.append(shipNames[cel]);
								osb.append(" foi atingido e está danificado!\r\n");
							}
						}
						
						osb.append("Sua vez! Digite a coordenada (e.g.: A1, D5, F0) onde deseja atirar no campo do oponente: \r\n");
						sb.append("É a vez de "+playerNames[opponent]+"!\r\n");
						nextTurn(sb.toString(), osb.toString());
						return true;
					}
				}
			}
			break;
			default:
				
			break;
		}
		return false;
	}
	
	public boolean noMoreShips(int player) {
		for(int i = 0; i<frota[player].length; ++i) {
			if (frota[player][i]>0) return false;
		}
		return true;
	}
	
	private int getCampo(int p, int x, int y) {
		if (x<0 || x>=width || y<0 || y>=height) 
			return Celula.AGUA.getValor();
		return campo[p][y][x];
	}
	
	private int getNavioAtingido(int celula) {
		return -(celula+100);
	}
	
	public void nextTurn(String prev, String next) {
		nextTurn(new String[] {prev, next});
	}
	
	public void nextTurn(String s[]) {
		if (isGameOver()) return;
		int nextPlayer = getProxJogador();
		if (turnoTimeout != null) {
			turnoTimeout.cancel();
		}
		
		turnoTimeout = new Timer();
		
		if (noMoreShips(nextPlayer)) {
			winGame((nextPlayer+1)%2);
			return;
		}
		
		int si;
		
		try {
			Header hh;
			for(int i=0; i<2; ++i)  {
				if (nextPlayer == i) 	{hh = Header.M_BATALHANAVAL_BEGINTURN; 	si = 1;}
				else					{hh = Header.M_BATALHANAVAL_OPTURN;		si = 0;}
				send(i, new Mensagem<String>(hh, getState(i)+s[si]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		proxJogador = (proxJogador + 1)%2;
		
		timeoutTask = new TimerTask() {
			@Override
			public void run() {
				int nextPlayer = getProxJogador();
				int opponent = (nextPlayer+1)%2;
				try {
					send(opponent, new Mensagem<String>(Header.M_BATALHANAVAL_END, getState(opponent)+"-----\r\n\r\nO jogo foi encerrado por timeout."));
					send(nextPlayer, new Mensagem<String>(Header.M_BATALHANAVAL_END, getState(nextPlayer)+"-----\r\n\r\n"+playerNames[opponent]+" demorou demais para responder, portanto você venceu!"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				getJogo().encerrajogo(getPendente());
				setGameOver(true);
			}
		};
		turnoTimeout.schedule(timeoutTask, timeoutTime);
	}
	
	private void winGame(int player) {
		int opponent = (player+1)%2;
		try {
			send(player, new Mensagem<String>(Header.M_BATALHANAVAL_END, getState(player)+"-----\r\n\r\nVOCÊ VENCEU! Parabéns pela vitória!"));
			send(opponent, new Mensagem<String>(Header.M_BATALHANAVAL_END, getState(opponent)+"-----\r\n\r\nLá se foi o último de seus navios... mais sorte da próxima vez!"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		getJogo().encerrajogo(getPendente());
		setGameOver(true);
	}

	public int getProxJogador() {
		return proxJogador;
	}
	
	public boolean isHuman(int player) {
		return player==0 || multiplayer;
	}
	
	public void terminate() {
		if (isGameOver()) return;
		for(int i=0;i<2;++i) {
			try {
				if (!players[i].getSock().isClosed())
					players[i].getSock().close();
			} catch (Exception e) {};
		}
		setGameOver(true);
	}

	public void run() {
		proxJogador = (new Random().nextInt(100) < 50)?(0):(1);
		for(int i=0; i<2; ++i) {
			for(int j=0; j<10; ++j) {
				Arrays.fill(campo[i][j], Celula.AGUA.getValor());
			}
		}
		celulaDisplay.add(new HashMap<Integer, Character>());
		celulaDisplay.get(0).put(Celula.AGUA.getValor(), '~');
		celulaDisplay.get(0).put(Celula.PORTAAVIOES.getValor(), 'P');
		celulaDisplay.get(0).put(Celula.NAVIOGUERRA.getValor(), 'G');
		celulaDisplay.get(0).put(Celula.CRUZADOR.getValor(), 'C');
		celulaDisplay.get(0).put(Celula.SUBMARINO.getValor(), 'S');
		celulaDisplay.get(0).put(Celula.DESTRUIDOR.getValor(), 'D');
		for(int i=0; i<tamanhos.length; ++i)
			celulaDisplay.get(0).put(Celula.ATINGIDO.getValor() - i, '*');
		celulaDisplay.get(0).put(Celula.ERRADO.getValor(), 'w');
		celulaDisplay.add(new HashMap<Integer, Character>(celulaDisplay.get(0)));
		celulaDisplay.get(1).replace(Celula.PORTAAVIOES.getValor(), '~');
		celulaDisplay.get(1).replace(Celula.NAVIOGUERRA.getValor(), '~');
		celulaDisplay.get(1).replace(Celula.CRUZADOR.getValor(), '~');
		celulaDisplay.get(1).replace(Celula.SUBMARINO.getValor(), '~');
		celulaDisplay.get(1).replace(Celula.DESTRUIDOR.getValor(), '~');
		
		for(int i=0; i<2; ++i) {
			for(int j=0; j<tamanhos.length; ++j) {
				frota[i][j] = tamanhos[j];
			}
		}
		
		try {
			Thread.sleep(2000);
			for(int i=0;i<2;++i)
				send(i, new Mensagem<String>(Header.M_BATALHANAVAL_BUILDPROMPT, getState(i)));
		}  catch (Exception e) {
			terminate(); return;
		}
	}
	
	public void setPendente(JogoPendente jogo) {
		pendente = jogo;
		this.jogo = pendente.getJogo();
	}

	public JogoPendente getPendente() {
		return pendente;
	}

	public Jogo getJogo() {
		return jogo;
	}
	
	public boolean isGameOver() {
		return gameOver;
	}

	public void setGameOver(boolean gameOver) {
		this.gameOver = gameOver;
	}
}
