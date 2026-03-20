package br.com.lobo.simulador.venda.sysmos1.repository;

import br.com.lobo.simulador.venda.sysmos1.dto.CompraRequest;
import br.com.lobo.simulador.venda.sysmos1.dto.CompraResponse;
import br.com.lobo.simulador.venda.sysmos1.entity.Empresa;
import br.com.lobo.simulador.venda.sysmos1.entity.Produto;
import br.com.lobo.simulador.venda.sysmos1.entity.Transacionador;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CompraRepository {

    @Inject
    AgroalDataSource dataSource;

    public List<Empresa> listarEmpresas() {
        List<Empresa> empresas = new ArrayList<>();
        String sql = "SELECT E.COD, T.NOM FROM SPSEMP00 E JOIN TRSTRA01 T ON T.EMP = E.COD AND T.COD < 1000 ORDER BY E.COD";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                empresas.add(new Empresa(rs.getInt("COD"), rs.getString("NOM")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar empresas: " + e.getMessage(), e);
        }
        return empresas;
    }

    public List<Transacionador> listarClientes(int empresa) {
        List<Transacionador> clientes = new ArrayList<>();
        String sql = "SELECT EMP, COD, NOM FROM TRSTRA01 WHERE EMP = ? ORDER BY COD";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empresa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(new Transacionador(rs.getInt("EMP"), rs.getInt("COD"), rs.getString("NOM")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar clientes: " + e.getMessage(), e);
        }
        return clientes;
    }

    public int limparTudo() {
        int total = 0;
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM GCEITM01 WHERE MOD='E' AND OP1='S' AND NUM >= 900000")) {
                    total += ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM GCENFS01 WHERE MOD='E' AND OP1='S' AND NUM >= 900000")) {
                    total += ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Erro ao limpar: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro de conexão: " + e.getMessage(), e);
        }
        return total;
    }

    public List<Produto> listarProdutos() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT COD, DSC FROM GCEPRO02 ORDER BY COD";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                produtos.add(new Produto(rs.getInt("COD"), rs.getString("DSC")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar produtos: " + e.getMessage(), e);
        }
        return produtos;
    }

    public int proximoNumeroNota(Connection conn, int empresa) throws SQLException {
        String sql = "SELECT COALESCE(MAX(NUM), 900000) + 1 FROM GCENFS01 WHERE MOD = 'E' AND EMP = ? AND OP1 = 'S' AND SER = '1' AND NUM >= 900000";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empresa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 900001;
            }
        }
    }

    public CompraResponse inserirVenda(CompraRequest req) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int nextNum = proximoNumeroNota(conn, req.empresa);
                Date dataReq = Date.valueOf(req.data);

                // Calcula total da nota (soma de todos os itens)
                double valorTotalNota = 0;
                for (var item : req.itens) {
                    valorTotalNota += item.quantidade * item.valorUnitario;
                }

                // Insert GCENFS01 - baseado na nota 900097 real
                String sqlNfs = """
                    INSERT INTO GCENFS01 (
                        MOD, EMP, OP1, OP2, NUM, SER, CCF,
                        DTR, DTC, DTM,
                        PDV, CUP, CFL, MNT, VER,
                        COE, ORG, CVD, FPG, NMI,
                        VTM, TGD, ICB, ICV,
                        FLG, FPC, OGD, ITG, MDL, IPO,
                        FL_CONTROLARDESCONTO, CD_FINALIDADE, NR_INDICADORPRESENCA,
                        USR
                    ) VALUES (
                        'E', ?, 'S', 'V', ?, '1', ?,
                        ?, CURRENT_DATE, CURRENT_DATE,
                        0, 0, 'S', 'N', 1,
                        2110, 2, 1000, 1, ?,
                        ?, ?, ?, ?,
                        32769, 'S', 2, 1, 1, 61,
                        'S', 1, 9,
                        1
                    )
                    """;
                double icms = valorTotalNota * 0.12;
                try (PreparedStatement ps = conn.prepareStatement(sqlNfs)) {
                    ps.setInt(1, req.empresa);
                    ps.setInt(2, nextNum);
                    ps.setInt(3, req.cliente);
                    ps.setDate(4, dataReq);
                    ps.setInt(5, req.itens.size());   // NMI = numero de itens
                    ps.setDouble(6, valorTotalNota);
                    ps.setDouble(7, valorTotalNota);
                    ps.setDouble(8, valorTotalNota);   // ICB
                    ps.setDouble(9, icms);             // ICV
                    ps.executeUpdate();
                }

                // Insert GCEITM01 para cada item
                String sqlItm = """
                    INSERT INTO GCEITM01 (
                        MOD, EMP, OP1, OP2, NUM, SER, CCF,
                        DTR, DTC, DTM,
                        ITM, PRO, QNT, QND, PRU, PRD, VTL, VER,
                        FL_CONTROLEESTOQUE
                    ) VALUES (
                        'E', ?, 'S', 'V', ?, '1', ?,
                        ?, CURRENT_DATE, CURRENT_DATE,
                        ?, ?, ?, ?, ?, ?, ?, 2,
                        1
                    )
                    """;
                int itemSeq = 1;
                for (var item : req.itens) {
                    double vtl = item.quantidade * item.valorUnitario;
                    try (PreparedStatement ps = conn.prepareStatement(sqlItm)) {
                        ps.setInt(1, req.empresa);
                        ps.setInt(2, nextNum);
                        ps.setInt(3, req.cliente);
                        ps.setDate(4, dataReq);
                        ps.setInt(5, itemSeq++);
                        ps.setInt(6, item.produto);
                        ps.setDouble(7, item.quantidade);
                        ps.setDouble(8, item.quantidade);
                        ps.setDouble(9, item.valorUnitario);
                        ps.setDouble(10, item.valorUnitario);
                        ps.setDouble(11, vtl);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
                return new CompraResponse(true,
                    "Venda registrada! Nota: " + nextNum + " (" + req.itens.size() + " itens)", nextNum);
            } catch (Exception e) {
                conn.rollback();
                return new CompraResponse(false, "Erro ao inserir venda: " + e.getMessage(), 0);
            }
        } catch (SQLException e) {
            return new CompraResponse(false, "Erro de conexão: " + e.getMessage(), 0);
        }
    }

    public CompraResponse excluirVenda(int empresa, int num, int cliente, String data) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Date dtr = Date.valueOf(data);

                String sqlItm = "DELETE FROM GCEITM01 WHERE MOD='E' AND EMP=? AND OP1='S' AND NUM=? AND SER='1' AND CCF=? AND DTR=?";
                try (PreparedStatement ps = conn.prepareStatement(sqlItm)) {
                    ps.setInt(1, empresa);
                    ps.setInt(2, num);
                    ps.setInt(3, cliente);
                    ps.setDate(4, dtr);
                    ps.executeUpdate();
                }

                String sqlNfs = "DELETE FROM GCENFS01 WHERE MOD='E' AND EMP=? AND OP1='S' AND NUM=? AND SER='1' AND CCF=? AND DTR=?";
                try (PreparedStatement ps = conn.prepareStatement(sqlNfs)) {
                    ps.setInt(1, empresa);
                    ps.setInt(2, num);
                    ps.setInt(3, cliente);
                    ps.setDate(4, dtr);
                    ps.executeUpdate();
                }

                conn.commit();
                return new CompraResponse(true, "Nota " + num + " excluida com sucesso!", num);
            } catch (Exception e) {
                conn.rollback();
                return new CompraResponse(false, "Erro ao excluir: " + e.getMessage(), 0);
            }
        } catch (SQLException e) {
            return new CompraResponse(false, "Erro de conexão: " + e.getMessage(), 0);
        }
    }

    public List<Map<String, Object>> listarVendasRecentes(int limite) {
        List<Map<String, Object>> vendas = new ArrayList<>();
        String sql = """
            SELECT N.EMP, N.NUM, N.CCF, N.DTR, N.VTM, I.PRO, I.QNT, I.VTL
            FROM GCENFS01 N
            JOIN GCEITM01 I ON I.MOD=N.MOD AND I.EMP=N.EMP AND I.OP1=N.OP1
                 AND I.NUM=N.NUM AND I.SER=N.SER AND I.CCF=N.CCF AND I.DTR=N.DTR
            WHERE N.MOD='E' AND N.OP1='S' AND N.SER='1' AND N.PDV=0 AND N.NUM >= 900000
            ORDER BY N.DTR DESC, N.NUM DESC, I.ITM
            LIMIT ?
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> venda = new HashMap<>();
                    venda.put("emp", rs.getInt("EMP"));
                    venda.put("num", rs.getInt("NUM"));
                    venda.put("ccf", rs.getInt("CCF"));
                    venda.put("dtr", rs.getDate("DTR") != null ? rs.getDate("DTR").toString() : "");
                    venda.put("vtm", rs.getDouble("VTM"));
                    venda.put("pro", rs.getInt("PRO"));
                    venda.put("qnt", rs.getDouble("QNT"));
                    venda.put("vtl", rs.getDouble("VTL"));
                    vendas.add(venda);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar vendas recentes: " + e.getMessage(), e);
        }
        return vendas;
    }
}
