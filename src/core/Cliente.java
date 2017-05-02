package core;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import static util.Utilidades.*;

public class Cliente extends Comunicador{
	
	private static BufferedReader in;
	private static ObjectOutputStream netOut;
	private static Socket sock;
	
	public static void main(String args[]) throws Exception {
		in = new BufferedReader (new InputStreamReader(System.in, "UTF-8"));

		
		System.out.println("Bem-vindo!\r\nPara iniciar, forneça o endereço IP do servidor...");
		String host = inputString(in);
		System.out.println("Agora a porta...");
		int port = inputInteger(in, 0, 65535);
	 
		
		try {
			sock = new Socket(host,port); // cria socket
		} catch (Exception e) {
			System.out.println("Não foi possível conectar. Erro: "+e);
			in.readLine();
			return;
		}

		try {
		    //OutputStreamWriter netOut = new OutputStreamWriter(sock.getOutputStream(), "UTF-8");
			netOut = new ObjectOutputStream(sock.getOutputStream());
		    ObjectInputStream netIn = new ObjectInputStream(sock.getInputStream());
		    //new BufferedReader(new InputStreamReader(sock.getInputStream(), "UTF-8"));
			
			
			// cria a estrutura de dados a ser enviada
			System.out.println("Digite o nome de exibição desejado:");
			String playerName = inputString(in);
			send(netOut, new Mensagem<String>(Header.M_ID, playerName));
			
			JsonObject mObj = receive(netIn);

			while(getHeader(mObj) == Header.M_NOMEINVALIDO) {
				System.out.println("O nome '"+playerName+"' já está em uso. Por favor digite outro:");
				playerName = inputString(in);
				send(netOut, new Mensagem<String>(Header.M_ID, playerName));
				mObj = receive(netIn);
			}
				
			System.out.println("Bem-vindo à central de jogos. Por favor digite o número do jogo a ser jogado:");
			ArrayList<String> mLista = getDado(mObj, new TypeToken<ArrayList<String>>() {}.getType());
			int sz = mLista.size();
			for(int i=0; i<sz; ++i) {
				System.out.println((i+1)+". "+mLista.get(i));
			}
			int gameSelect = inputInteger(in, 1, sz);
			
			System.out.println("Esta será uma partida contra outro jogador humano? (S/N)");
			boolean isMultiplayer = inputYesNo(in);
			send(netOut, new Mensagem<Integer>(Header.M_ESCOLHAJOGO, encodeEscolhaJogo(gameSelect, isMultiplayer)));
			
			JsonObject m;
			while(!sock.isClosed() && (m = receive(netIn)) != null) {
				//System.out.println(m.getDado());
				handleMessage(m);
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		if (!sock.isClosed())
			sock.close(); // fecha socket
	}
	
	public static void handleMessage(JsonObject m) throws Exception {
		   if (m==null) return;
		   
		   Header h = getHeader(m);
		   
		   switch(h) {
		   		case M_AGUARDANDO:
		   			System.out.println("Aguardando segundo jogador...");
				break;
		   		case M_OPONENTEDISP:
		   			System.out.println(getDado(m, String.class) + " entrou!");
		   			System.out.println("Preparados? O jogo vai começar.");
		   		break;
		   		case M_PROMPTRETOMAR:	   			
		   			System.out.println("Foi detectada uma partida não terminada ("+getDado(m, String.class)+").");
		   			System.out.println("Gostaria de continuá-la?");
		   			send(netOut, new Mensagem<Boolean>(Header.M_RETOMAR, inputYesNo(in)));
		   		break;
		   		case M_BATALHANAVAL_BUILDPROMPT: {
		   			System.out.println(getDado(m, String.class));
		   			JogadaBatalhaNaval data = inputBatalhaNavalBuild(in);
		   			send(netOut, new Mensagem<JogadaBatalhaNaval>(Header.M_BATALHANAVAL_BUILDACTION, data));
		   		}
		   		break;
		   		case M_BATALHANAVAL_WAIT: {
		   			System.out.println(getDado(m, String.class));
		   		}
		   		break;
		   		case M_BATALHANAVAL_BEGINTURN: {
		   			System.out.println(getDado(m, String.class));
		   			JogadaBatalhaNaval data = inputBatalhaNavalAttack(in);
		   			send(netOut, new Mensagem<JogadaBatalhaNaval>(Header.M_BATALHANAVAL_ATTACKACTION, data));
		   		}
		   		break;
		   		case M_FORCA_BEGINTURN: {
		   			System.out.println(getDado(m, String.class));
		   			JogadaForca data = inputForcaGuess(in);
		   			send(netOut, new Mensagem<JogadaForca>(Header.M_FORCA_GUESS, data));
		   		}
		   		break;
		   		case M_BATALHANAVAL_OPTURN: 
		   		case M_FORCA_OPTURN:
		   		{
		   			System.out.println(getDado(m, String.class));
		   			System.out.println("Por favor aguarde...");
		   		}
		   		break;
		   		case M_BATALHANAVAL_END: 
		   		case M_FORCA_END:
		   		{
		   			System.out.println(getDado(m, String.class));
		   			System.out.println("Obrigado por jogar!");
		   			sock.close();
		   		}
		   		break;
			   	default:
			   		System.out.println("Mensagem sem tratamento: ["+getHeader(m)+"]");
			   	break;
		   }
	}
}
