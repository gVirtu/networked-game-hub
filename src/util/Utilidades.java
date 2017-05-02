package util;

import java.io.BufferedReader;

import core.JogadaBatalhaNaval;
import core.JogadaForca;

public class Utilidades {
	public static String inputString(BufferedReader in) {
		return inputString(in, Integer.MAX_VALUE);
	}
	
	public static String inputString(BufferedReader in, int maxSize) {
		String s;
		while(true) {
			try {
				s = in.readLine();
				if (s.length() > 0 && s.length() <= maxSize) return s;
				else { System.out.println("Deve conter entre 1 e "+maxSize+" caracteres!"); }
			} catch (Exception e) {
				System.out.println("Tente novamente...");
			}
		}
	}
	
	public static int inputInteger(BufferedReader in) {
		return inputInteger(in, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
	}
	
	public static int inputInteger(BufferedReader in, int min) {
		return inputInteger(in, min, Integer.MAX_VALUE);
	}
	
	public static int inputInteger(BufferedReader in, int min, int max) {
		int ret = min-1;
		String s;
		while(true) {
			try {
				s = in.readLine();
				ret = Integer.parseInt(s);
			} catch (Exception e) {
				System.out.println("Tente novamente...");
			}
			if (ret >= min && ret <= max) return ret;
			else { System.out.println("O valor precisa estar entre "+min+" e "+max+"."); }
		}
	}
	
	public static boolean inputYesNo(BufferedReader in) {
		String s; char c;
		while(true) {
			try {
				s = in.readLine();
				c = Character.toUpperCase(s.charAt(0));
				if (c != 'S' && c != 'N') continue;
				return (c == 'S');
			} catch (Exception e) {
				System.out.println("Tente novamente...");
			}
		}
	}
	
	public static JogadaBatalhaNaval inputBatalhaNavalBuild(BufferedReader in) {
		//Coordenada
		String s; char rc, cc, d;
		int r, c, dir; 
		while(true) {
			try {
				s = in.readLine();
				rc = Character.toUpperCase(s.charAt(0));
				if (s.length()==1 && rc=='R') return new JogadaBatalhaNaval(100, 100, 0);
				cc = Character.toUpperCase(s.charAt(1));
				if (s.length() > 2) continue;
				if (rc < 'A' || rc > 'J') continue;
				if (cc < '0' || cc > '9') continue;
				r = rc-'A';
				c = cc-'0';
				break;
			} catch (Exception e) {
				System.out.println("Tente novamente...");
			}
		}
		System.out.println("Digite a direção na qual vamos construir o navio. Use W (cima)/A (esq.)/S (baixo)/D (direita).");
		while(true) {
			try {
				s = in.readLine();
				d = Character.toUpperCase(s.charAt(0));
				if (s.length() > 1) continue;
				if (d != 'W' && d != 'A' && d != 'S' && d != 'D') continue;
				switch(d) {
					case 'W':
						dir = 90;
					break;
					case 'A':
						dir = 180;
					break;
					case 'S':
						dir = 270;
					break;
					case 'D':
						dir = 0;
					break;
					default:
						dir = 0;
					break;
				}
				break;
			} catch (Exception e) {
				System.out.println("Tente novamente...");
			}
		}
		return new JogadaBatalhaNaval(r, c, dir);
	}
	
	public static JogadaBatalhaNaval inputBatalhaNavalAttack(BufferedReader in) {
		//Coordenada
		String s; char rc, cc;
		int r, c; 
		while(true) {
			try {
				s = in.readLine();
				rc = Character.toUpperCase(s.charAt(0));
				cc = Character.toUpperCase(s.charAt(1));
				if (s.length() > 2) continue;
				if (rc < 'A' || rc > 'J') continue;
				if (cc < '0' || cc > '9') continue;
				r = rc-'A';
				c = cc-'0';
				break;
			} catch (Exception e) {
				System.out.println("Tente novamente...");
			}
		}
		return new JogadaBatalhaNaval(r, c, 0);
	}
	
	public static JogadaForca inputForcaGuess(BufferedReader in) {
		//Letra
		String s; char c;
		while(true) {
			try {
				s = in.readLine();
				c = Character.toUpperCase(s.charAt(0));
				if (s.length() > 1) continue;
				if (c < 'A' || c > 'Z') continue;
				break;
			} catch (Exception e) {
				System.out.println("Tente novamente...");
			}
		}
		return new JogadaForca(c);
	}
	
	public static int encodeEscolhaJogo(int game, boolean multiplayer) {
		return (game * 10) + ((multiplayer)?(1):(0)); 
	}
	
	public static int decodeEscolhaJogoIndex(int escolhaJogo) {
		return (escolhaJogo / 10) - 1; //indexado a partir do 0
	}
	
	public static boolean decodeEscolhaJogoIsMultiplayer(int escolhaJogo) {
		return (escolhaJogo % 10)==1;
	}
}
