package core;

public class Jogador {
	private String nome;
	private String endereco;
	
	public Jogador(String nome, String endereco) {
		this.nome = nome;
		this.endereco = endereco;
	}
	
	public String getEndereco() {
		return endereco;
	}
	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	@Override
    public int hashCode() {
        return nome.hashCode()+endereco.hashCode();
    }
	
	@Override
	public boolean equals(Object obj) {
		System.out.println("Comparando dois jogadores...");
		if (obj == null) {
	        return false;
	    }
	    if (!Jogador.class.isAssignableFrom(obj.getClass())) {
	        return false;
	    }
	    final Jogador j = (Jogador) obj;
	    System.out.println("Nomes = "+this.getNome()+" , "+j.getNome());
	    System.out.println("Enderecos = "+this.getEndereco()+" , "+j.getEndereco());
	    if ((this.nome == null) ? (j.getNome() != null) : !this.nome.equals(j.getNome())) {
	    	System.out.println("false1");
	        return false;
	    }
	    if (!this.endereco.equals(j.getEndereco())) {
	    	System.out.println("false2");
	        return false;
	    }
	    System.out.println("TRUE");
	    return true;
	}
}
