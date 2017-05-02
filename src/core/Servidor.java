package core;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class Servidor {
	public static Jogo[] jogos = {new Jogo("Batalha Naval", ControladorBatalhaNaval.class), new Jogo("Jogo da Forca", ControladorForca.class)};
	public static Set<String> jogadores = new HashSet<String>();
	
	public static void main(String args[]) throws IOException {
		int port = 7575;
		if (args.length>0)
			port= Integer.parseInt(args[0]);

		ServerSocket ss = new ServerSocket(port);

		try {
		    System.out.println("Servidor esperando conexões na porta "+port+"...");
		    while (true) {
		    	 Socket as = ss.accept(); // socket ativo é criado
		    	 System.out.println("Conexão estabelecida com "+as.getInetAddress().getHostAddress()+"...");
					
		    	 Tratador w = new Tratador(as);
		    	 Thread t = new Thread(w);
		    	 t.start();
	
		    	 System.out.println("Servidor aguardando nova conexão..."); 
		    }
	    } catch (Exception e) {
	    	ss.close();
	    }
    }
}
