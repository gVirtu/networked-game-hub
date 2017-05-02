package core;
import java.io.*;
import java.lang.reflect.Constructor;
import java.net.*;
import java.util.ArrayList;

import com.google.gson.JsonObject;

import static util.Utilidades.decodeEscolhaJogoIndex;
import static util.Utilidades.decodeEscolhaJogoIsMultiplayer;

public class Tratador extends Comunicador implements Runnable {
   private Socket sock;
   private String nome = null;
   private int gameIndex = -1;
   private int playerIndex = 0;
   private boolean multiplayer = false;
   private ControladorJogo game;
   public ObjectOutputStream out;

   public Tratador(Socket s) { // recebe o socket ativo no construtor
     sock = s;
   }

   public void run() {
      System.out.println("Tratador iniciado.");

	  try {
	     //BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), "UTF-8"));
	     //out = new OutputStreamWriter(sock.getOutputStream(), "UTF-8");
	     out = new ObjectOutputStream(sock.getOutputStream());
		 ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
	     JsonObject data;
	     //Recebe nome do jogador
	     while(!sock.isClosed() && (data = receive(in)) != null) {
	    	 //Primeiro recebe o nome do jogador, depois qual jogo quer jogar, depois possivelmente espera um oponente
	    	 if (gameIndex > -1 && getHeader(data).getJogo() == gameIndex) {
	    		 game.fazJogada(data, getPlayerIndex());
	    	 } else
	    		 handleMessage(data);
	     }
	     if (!sock.isClosed())
	    	 sock.close();
      }
      catch (Exception e) {
         //e.printStackTrace();
      }
	  
	  //Ao terminar o tratador, verificar se estamos na lista de espera para algum jogo, e remover caso verdade
	  if (gameIndex >= 0) {
		  synchronized(Servidor.class) {
			  Jogo j = Servidor.jogos[gameIndex];
			   if (j.getEmEspera() != null && j.getEmEspera().equals(this)) {
				   j.setEmEspera(null);
			   }
		  }
		  
		  if (game != null) {
			  game.terminate();
		  }
	  }
	  System.out.println("Conexão com "+sock.getInetAddress()+" terminada.");
	  
	  //Remover da lista de jogadores conectados
	  if (Servidor.jogadores.contains(nome)) {
		  Servidor.jogadores.remove(nome);
	  }
   }
   
   public void bindGame(ControladorJogo c) {
	   game = c;
   }
   
   private void novoJogo(Jogo j, Jogador meuJogador) throws Exception {
	   if (j.getEmEspera() == null) {
		   //Esperando segundo jogador
		   send(out, new Mensagem<Integer>(Header.M_AGUARDANDO, 0));
		   j.setEmEspera(this);
	   } else {
		   //Alguém (P2) já estava esperando, instancia controlador do jogo
		   Tratador handlerP2 = j.getEmEspera();
		   Constructor<? extends ControladorJogo> cons = j.getControlador().getConstructor();
		   game = cons.newInstance();
		   handlerP2.bindGame(game);
		   //startedGame = true;
		   send(out, new Mensagem<String>(Header.M_OPONENTEDISP, handlerP2.getNome()));
		   send(handlerP2.out, new Mensagem<String>(Header.M_OPONENTEDISP, getNome()));
		   setPlayerIndex(0);
		   handlerP2.setPlayerIndex(1);
		   game.init(new Tratador[] {this, handlerP2});
		   Thread t = new Thread((Runnable) game);
		   t.start();
		   j.setEmEspera(null);
		   Jogador jogadores[] = {	meuJogador,
				   new Jogador(handlerP2.getNome(), handlerP2.getSock().getInetAddress().getHostAddress()) };
		   JogoPendente pendente = new JogoPendente(j, jogadores, game);
		   j.getPendentes().put(jogadores[0], pendente);
		   j.getPendentes().put(jogadores[1], pendente);
		   game.setPendente(pendente);
	   }
   }
   
   public void handleMessage(JsonObject m) throws Exception {
	   if (m==null) return;
	   Header h = getHeader(m);
	   
	   switch(h) {
	   	case M_ID:
	   		nome = getDado(m, String.class); //getGSON().fromJson(m, String.class);
	   		if (Servidor.jogadores.contains(nome)) {
	   			System.out.println("Jogador "+nome+" já existe..");
	   			send(out, new Mensagem<String>(Header.M_NOMEINVALIDO, ""));
	   		} else {
		   		System.out.println("Jogador registrado como "+nome);
		   		Servidor.jogadores.add(nome);
		   		
		   		//Envia de volta a lista de jogos
		   		ArrayList<String> lista = new ArrayList<String>();
		   		for(Jogo j : Servidor.jogos) {
		   			lista.add(j.getNome());
		   		}
		   		send(out, new Mensagem<ArrayList<String>>(Header.M_LISTAJOGOS, lista));
	   		}
	   	break;
	   	case M_ESCOLHAJOGO: {
	   		int data = getDado(m, Integer.class);
	   		gameIndex = decodeEscolhaJogoIndex(data);
	   		multiplayer = decodeEscolhaJogoIsMultiplayer(data);
	   		System.out.println("Jogador "+nome+" escolheu o jogo "+Servidor.jogos[gameIndex].getNome()+" ("+((multiplayer)?("Multi-player"):("Single-player"))+")");
	   		if (multiplayer) {
	   			synchronized(Servidor.class) {
	   				Jogo j = Servidor.jogos[gameIndex];
	   				Jogador meuJogador = new Jogador(getNome(), getSock().getInetAddress().getHostAddress());
	   				System.out.println("PENDENTES = "+j.getPendentes().toString());
	   				if (j.getPendentes().containsKey(meuJogador)) {
	   					String jogadores = j.getPendentes().get(meuJogador).getJogador(0).getNome() + " x " + j.getPendentes().get(meuJogador).getJogador(1).getNome();
	   					send(out, new Mensagem<String>(Header.M_PROMPTRETOMAR, jogadores));
	   				} else {
		   				novoJogo(j, meuJogador);
	   				}
	   			}   			
	   		} else {
	   			Jogo j = Servidor.jogos[gameIndex];
	   			Constructor<? extends ControladorJogo> cons = j.getControlador().getConstructor();
				game = cons.newInstance();
				setPlayerIndex(0);
				game.init(new Tratador[] {this});
   		    	Thread t = new Thread((Runnable) game);
   		   	 	t.start();
	   		}
	   	}
	   	break;
	   	case M_RETOMAR: {
	   		boolean data = getDado(m, Boolean.class);
	   		System.out.println("Jogador "+nome+" escolheu "+((data)?("retomar"):("NÃO retomar a partida salva")));
	   		if (data) {
	   			synchronized(Servidor.class) {
	   				Jogo j = Servidor.jogos[gameIndex];
	   				Jogador meuJogador = new Jogador(getNome(), getSock().getInetAddress().getHostAddress());
	   				JogoPendente pendente = j.getPendentes().get(meuJogador);
	   				if (pendente.getEmEspera() == null) {
	   					//Esperando segundo jogador
	   					send(out, new Mensagem<Integer>(Header.M_AGUARDANDO, 0));
	   					pendente.setEmEspera(this);
	   				} else {
	   					Tratador handlerP2 = pendente.getEmEspera();
	   					game = pendente.getControlador();
	   					game.setGameOver(false);
	   					handlerP2.bindGame(game);
	   					int myPlayerInd = 1, otherPlayerInd = 0;
	   					if (getNome().equals(pendente.getJogador(0).getNome()) && handlerP2.getNome().equals(pendente.getJogador(1).getNome())) {
	   						myPlayerInd = 0; otherPlayerInd = 1;
	   					}
	   					setPlayerIndex(myPlayerInd);
		   		    	handlerP2.setPlayerIndex(otherPlayerInd);
		   		    	
		   		    	//Re-envia as duas últimas mensagens aos respectivos jogadores e atualiza o jogo com os novos tratadores
		   		    	if (myPlayerInd == 0) {
		   		    		game.init(new Tratador[] {this, handlerP2});
		   		    		send(out, pendente.getUltimaMensagem(0));
		   		    		send(handlerP2.out, pendente.getUltimaMensagem(1));
		   		    	} else {
		   		    		game.init(new Tratador[] {handlerP2, this});
		   		    		send(out, pendente.getUltimaMensagem(1));
		   		    		send(handlerP2.out, pendente.getUltimaMensagem(0));
		   		    	}
		   		    	
		   		    	pendente.setEmEspera(null);
		   		    	
	   				}
	   			}
	   		} else {
	   			//Não retomar
	   			synchronized(Servidor.class) {
	   				Jogo j = Servidor.jogos[gameIndex];
	   				Jogador meuJogador = new Jogador(getNome(), getSock().getInetAddress().getHostAddress());
	   				JogoPendente pendente = j.getPendentes().get(meuJogador);
	   				j.encerrajogo(pendente);
	   				novoJogo(j, meuJogador);
	   			}
	   		}
	   	}
	   	break;
	   	default:
	   		System.out.println("Mensagem sem tratamento: ["+h+"]");
	   	break;
	   }
   }
   
   public String getNome() {
	   return nome;
   }
   
   public Socket getSock() {
	   return sock;
   }
   
   public int getPlayerIndex() {
	   return playerIndex;
   }
   
   public void setPlayerIndex(int val) {
	   playerIndex = val;
   }
}
