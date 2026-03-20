package br.com.lobo.simulador.venda.sysmos1.dto;

public class CompraResponse {
    public boolean sucesso;
    public String mensagem;
    public int numeroNota;

    public CompraResponse() {}

    public CompraResponse(boolean sucesso, String mensagem, int numeroNota) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
        this.numeroNota = numeroNota;
    }
}
