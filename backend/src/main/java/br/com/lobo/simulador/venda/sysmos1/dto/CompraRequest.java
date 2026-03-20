package br.com.lobo.simulador.venda.sysmos1.dto;

import java.util.List;

public class CompraRequest {
    public int empresa;
    public int cliente;
    public String data;
    public List<ItemRequest> itens;

    public static class ItemRequest {
        public int produto;
        public double quantidade;
        public double valorUnitario;
    }
}
