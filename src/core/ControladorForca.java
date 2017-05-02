package core;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonObject;

public class ControladorForca implements ControladorJogo, Runnable {
	private Tratador[] players = new Tratador[2];
	private String playerNames[] = new String[2];
	private int erros[] = {0, 0};
	private int pontos[] = {0, 0};
	private int valor[] = new int[255];
	private int frequencia[] = new int[255];
	private String palavras[] = new String[3];
	private BitSet letrasCorretas;
	private ArrayList<Character> palpites = new ArrayList<Character>();
	private ArrayList<Character> proxPalpitesCPU = new ArrayList<Character>();
	private int palavraAtual = -1;
	private int proxJogador;
	private final int maxErros = 6;
	private boolean gameOver = false;
	private boolean multiplayer = false;
	private Timer turnoTimeout;
	private Timer cpuTimer;
	private TimerTask timeoutTask;
	private long cpuDelay = 3000; //3 seg
	private long timeoutTime = 300000; //5 min
	private Jogo jogo;
	private JogoPendente pendente;
	
	private final int EMPATE = 2;
	
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
			if (data.getHeader() == Header.M_FORCA_BEGINTURN) {	
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
	
	public String drawForca(int line, int erros) {
		switch(line) {
			case 0:
				return "____ ";
			case 1:
				return "|  | ";
			case 2:
				return (erros>0)?("|  o "):("|    ");
			case 3:
				switch(erros) {
					case 2: return "|  | ";
					case 3: return "| /| ";
					case 4: 
					case 5:
					case 6:
							return "| /|\\";
					default:
							return "|    ";
				}
			case 4:
				switch(erros) {
					case 5:
						return "| /  ";
					case 6:
						return "| / \\";
					default:
						return "|    ";
				}
			default: return "";
		}
	}
	
	public String getState(int player) {
		if (isGameOver()) return "";
		int opponent = (player+1)%2;
		StringBuilder sb = new StringBuilder();
		sb.append(playerNames[player]); sb.append(" x "); sb.append(playerNames[opponent]);
		sb.append("\r\n---ROUND "); sb.append(palavraAtual+1); sb.append(" de 3---\r\n\r\n");
		
		for(int i=0;i<5;++i) {
			sb.append(drawForca(i, erros[player]));
			sb.append("     ");
			sb.append(drawForca(i, erros[opponent]));
			sb.append("\r\n");
		}
		sb.append("\r\n");

		for(int i=0;i<palavras[palavraAtual].length();++i) {
			sb.append(letrasCorretas.get(i)? (palavras[palavraAtual].charAt(i)) : ('_'));
			sb.append(" ");
		}
		sb.append("\r\n\r\nTentativas: ");
		if (palpites.size()>0) {
			for(int i=0; i<palpites.size()-1;++i) {
				sb.append(palpites.get(i));
				sb.append(", ");
			}
			sb.append(palpites.get(palpites.size()-1));
		}
		
		sb.append("\r\nPlacar:\r\n");
		sb.append(playerNames[player]);
		sb.append(": "); sb.append(pontos[player]); sb.append(" pontos\r\n");
		sb.append(playerNames[opponent]);
		sb.append(": "); sb.append(pontos[opponent]); sb.append(" pontos");
		sb.append("\r\n\r\n");
		return sb.toString();
	}
	
	public JsonObject geraJogada() { //Para partidas contra o computador..
		boolean risky = new Random().nextInt(100) > 88;
		char c;
		Mensagem<JogadaForca> data;
		if (risky) {
			int offset = 1 + new Random().nextInt(8);
			c = proxPalpitesCPU.get(Math.max(0, proxPalpitesCPU.size() - offset));
		} else {
			int offset = new Random().nextInt(4);
			c = proxPalpitesCPU.get(Math.min(proxPalpitesCPU.size()-1, offset));
		}
		
		//Chance de 'saber' qual é a palavra e garantidamente escolher uma letra certa
		int remaining = letrasCorretas.cardinality()-palavras[palavraAtual].length();
		if (new Random().nextInt(100) > (remaining*remaining)*10) {
			c = palavras[palavraAtual].charAt(letrasCorretas.nextClearBit(0));
		}

		data = new Mensagem<JogadaForca>(Header.M_FORCA_GUESS, new JogadaForca(c));
		return Comunicador.getGSON().toJsonTree(data).getAsJsonObject();
	}
	
	public boolean fazJogada(JsonObject jogada, int player) {
		if (isGameOver()) return false;
		Header h = Comunicador.getHeader(jogada);
		JogadaForca data = Comunicador.getDado(jogada, JogadaForca.class);
		System.out.println("fazJogada - P"+player+" - "+h+" - "+data);
		
		switch(h) {
			case M_FORCA_GUESS: {
				//Impede o próximo jogador de jogar no turno do atual
				if (player != getProxJogador()) {
					int opponent = (player+1)%2;
					boolean valid = true;
					boolean adivinhou = false;
					char c = Character.toUpperCase(data.getC());
					
					valid = (c >= 'A' && c <= 'Z');
					
					for(Character x : palpites) {
						if (c == x) {valid = false; break;}
					}
					
					if (valid) {
						int bonus = contaOcorrencias(c, palavras[palavraAtual]) * valor[c];
						
						StringBuilder sb = new StringBuilder();
						StringBuilder osb;
						palpites.add(c);
						proxPalpitesCPU.remove((Character) c);
						sb.append(playerNames[player]);
						sb.append(" tentou a letra ");
						sb.append(data.getC());
						sb.append("!\r\n");
						
						if (bonus>0) {
							pontos[player] += bonus;
							
							for(int i = 0; i<palavras[palavraAtual].length(); ++i) {
								if (palavras[palavraAtual].charAt(i) == c) {
									letrasCorretas.set(i);
								}
							}
							
							sb.append("...e acertou! Ganhou ");
							sb.append(bonus);
							sb.append(" pontos! ");
							osb = new StringBuilder(sb);
							osb.append("Tome cuidado!\r\n\r\n");
							sb.append("Excelente!\r\n\r\n");
						} else {
							erros[player] = Math.min(maxErros, erros[player]+1);
							sb.append("...e errou! ");
							osb = new StringBuilder(sb);
							osb.append("Que sorte a sua!\r\n\r\n");
							sb.append("Oh não!\r\n\r\n");
						}
						
						//Palavra já foi concluída
						if (letrasCorretas.cardinality() >= palavras[palavraAtual].length()) {
							adivinhou = true;
							sb.append("A palavra foi descoberta, era "); sb.append(palavras[palavraAtual]); sb.append("!\r\n");
							osb.append("A palavra foi descoberta, era "); osb.append(palavras[palavraAtual]); osb.append("!\r\n");
							if (palavraAtual < 2) {
								sb.append("Próxima rodada!\r\n");
								osb.append("Próxima rodada!\r\n");
							}
						}
						
						//Jogadas esgotadas
						if (palavraAtual < 2 && erros[player] >= maxErros && erros[opponent] >= maxErros) {
							sb.append("Nenhum jogador possui mais tentativas, a palavra era "); sb.append(palavras[palavraAtual]); sb.append("!\r\n");
							osb.append("Nenhum jogador possui mais tentativas, a palavra era "); osb.append(palavras[palavraAtual]); osb.append("!\r\n");
							sb.append("Próxima rodada!\r\n");
							osb.append("Próxima rodada!\r\n");
						}
						
						if (!adivinhou && erros[player] < maxErros && erros[opponent] >= maxErros) {
							sb.append("Você não possui mais tentativas!\r\n");
							osb.append("Como "+playerNames[opponent]+" não possui mais tentativas, é sua vez novamente! Digite a letra que deseja tentar (letras mais comuns valem menos pontos): \r\n");
						} else {
							osb.append("Sua vez! Digite a letra que deseja tentar (letras mais comuns valem menos pontos): \r\n");
							sb.append("É a vez de "+playerNames[opponent]+"!\r\n");
						}
												
						nextTurn(sb.toString(), osb.toString());
						return true;
					} else {
						try {
							send(player, new Mensagem<String>(Header.M_FORCA_BEGINTURN, (getState(player)+"\r\nEssa letra não pode ser usada, tente novamente!")));
						} catch (Exception e) {
							e.printStackTrace();
						}
						return false;
					}
				}
			}
			break;
			default:
				
			break;
		}
		return false;
	}
	
	public void nextTurn(String prev, String next) {
		nextTurn(new String[] {prev, next});
	}
	
	public void nextTurn(String s[]) {
		if (isGameOver()) return;
		if (turnoTimeout != null) {
			turnoTimeout.cancel();
		}
		
		turnoTimeout = new Timer();
		
		if (erros[0] >= maxErros && erros[1] >= maxErros) {
			proxPalavra();
		}
		
		if (letrasCorretas.cardinality() >= palavras[palavraAtual].length()) {
			proxPalavra();
		}
		
		if (isGameOver()) return;
		
		if (erros[proxJogador] >= maxErros) //Pula o turno do jogador que não tiver mais tentativas
			proxJogador = (proxJogador + 1)%2;
		
		int si;
		
		try {
			Header hh;
			for(int i=0; i<2; ++i)  {
				if (proxJogador == i) 	{hh = Header.M_FORCA_BEGINTURN; si = 1;}
				else					{hh = Header.M_FORCA_OPTURN;	si = 0;}
				//si = (origProxJogador == i) ? (1) : (0); //Como podemos pular turnos, isto faz com que as strings determinadas pelo fazJogada ainda sigam para os lugares certos
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
					send(opponent, new Mensagem<String>(Header.M_FORCA_END, getState(opponent)+"-----\r\n\r\nO jogo foi encerrado por timeout."));
					send(nextPlayer, new Mensagem<String>(Header.M_FORCA_END, getState(nextPlayer)+"-----\r\n\r\n"+playerNames[opponent]+" demorou demais para responder, portanto você venceu!"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				getJogo().encerrajogo(getPendente());
				setGameOver(true);
			}
		};
		turnoTimeout.schedule(timeoutTask, timeoutTime);
	}
	
	private void proxPalavra() {
		if (palavraAtual < 2) {
			++palavraAtual;
			palpites.clear();
			letrasCorretas = new BitSet(palavras[palavraAtual].length());
			erros[0] = 0;
			erros[1] = 0;
			proxPalpitesCPU.clear();
			for(char i='A'; i<='Z'; ++i) proxPalpitesCPU.add(i);
			//Ordena os caracteres com menor valor primeiro
			Collections.sort(proxPalpitesCPU, new Comparator<Character>() {
			    @Override
			    public int compare(Character o1, Character o2) {
			        return ((Integer) valor[o1]).compareTo(valor[o2]);
			    }
			});
		} else {
			finishGame();
		}
	}
	
	private void finishGame() {
		if (pontos[0]==pontos[1]) 	winGame(EMPATE);
		else						winGame(pontos[0]>pontos[1]? 0 : 1);
	}
	
	private void winGame(int player) {
		String s;
		if (player==EMPATE) {
			if (erros[0] < maxErros || erros[1] < maxErros) 	s = "-----\r\n\r\nApós os três rounds, o jogo terminou em um empate, ambos são vencedores! Até a próxima!";
			else												s = "-----\r\n\r\nNenhum jogador possui mais tentativas, e o jogo terminou em um empate, ambos são vencedores! Até a próxima!";
			
			try {
				for(int i=0; i<2; ++i)
					send(i, new Mensagem<String>(Header.M_FORCA_END, getState(i)+s));
			} catch (Exception e) {
				e.printStackTrace();
			}
			getJogo().encerrajogo(getPendente());
		} else {
			if (erros[0] < maxErros || erros[1] < maxErros) 	s = "-----\r\n\r\nÉ o fim do último round! Agora à contagem de pontos...";
			else												s = "-----\r\n\r\nNenhuma tentativa restante! Agora à contagem de pontos...";
			int opponent = (player+1)%2;
			try {
				send(player, new Mensagem<String>(Header.M_FORCA_END, getState(player)+s+"\r\n\r\nVocê conseguiu o maior número de pontos e foi o vencedor da partida, parabéns!"));
				send(opponent, new Mensagem<String>(Header.M_FORCA_END, getState(opponent)+s+"\r\n\r\nInfelizmente você ficou com menos pontos! Boa tentativa, mas não foi dessa vez!"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			getJogo().encerrajogo(getPendente());
		}
		setGameOver(true);
	}

	public int getProxJogador() {
		return proxJogador;
	}
	
	public int contaOcorrencias(Character c, String s) {
		return s.length() - s.replace(c.toString(), "").length();
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
	
	private void freqCount(String w) {
		for(int i = 0; i<w.length(); ++i) {
			++frequencia[w.charAt(i)];
		}
	}
	
	private int randomRange(int min, int max) { //[min, max)
		return min + (new Random().nextInt(max - min));
	}

	public void run() {
		proxJogador = (new Random().nextInt(100) < 50)?(0):(1);
		
		int contPalavras, currentLine = 0, currentWord = 0;
		int indPalavras[] = new int[4];
		
		for(int i='A';i<='Z';++i) {
			frequencia[i] = 0;
		}
		
		try (BufferedReader br = new BufferedReader(new FileReader("dicionarioForca.txt"))) {
		    String line;
		    line = br.readLine();
		    //Total de palavras no dicionário (precisa ser fornecido)
		    contPalavras = Integer.parseInt(line);
		    indPalavras[3] = contPalavras+1; //Sentinela
		    //Escolhe 3 aleatoriamente
		    for(int i=0; i<3; ++i) {
		    	indPalavras[i] = randomRange(((contPalavras*i)/3), ((contPalavras*(i+1))/3));
		    }
		    //Lê todas as palavras, conta a frequência das letras para determinar as pontuações, e armazena as 3 escolhidas
		    while ((line = br.readLine()) != null) {
		    	line = line.toUpperCase();
		    	freqCount(line);
		    	if (currentLine >= indPalavras[currentWord]) {
		    		palavras[currentWord] = line;
		    		letrasCorretas = new BitSet(palavras[currentWord].length());
		    		++currentWord;
		    	}
		    	++currentLine;
		    }
		} catch (Exception e) {
			e.printStackTrace();
			terminate(); return;
		}
		
		int maxFreq = 0;
		final int maxScore = 100;
		for(int i='A';i<='Z';++i) {
			maxFreq = Math.max(maxFreq, frequencia[i]); 
		}
		
		maxFreq *= 10;
		double freqScore;
		
		for(int i='A';i<='Z';++i) {
			freqScore = (double) maxFreq / (double) frequencia[i];
			valor[i] = Math.min(maxScore, ((Double) freqScore).intValue());
		}
		
		proxPalavra();
		
		try {
			Thread.sleep(2000);
			nextTurn("O jogo começou! Você jogará logo após seu oponente.", "O jogo começou, e você dará a primeira jogada! Digite a letra que deseja tentar (letras mais comuns valem menos pontos): \r\n");
		}  catch (Exception e) {
			e.printStackTrace();
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
