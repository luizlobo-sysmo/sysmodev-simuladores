package br.com.lobo.simulador.venda.sysmos1.resource;

import br.com.lobo.simulador.venda.sysmos1.dto.CompraRequest;
import br.com.lobo.simulador.venda.sysmos1.dto.CompraResponse;
import br.com.lobo.simulador.venda.sysmos1.entity.Empresa;
import br.com.lobo.simulador.venda.sysmos1.entity.Produto;
import br.com.lobo.simulador.venda.sysmos1.entity.Transacionador;
import br.com.lobo.simulador.venda.sysmos1.repository.CompraRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CompraResource {

    @Inject
    CompraRepository repository;

    @GET
    @Path("/empresas")
    public List<Empresa> listarEmpresas() {
        return repository.listarEmpresas();
    }

    @GET
    @Path("/clientes/{empresa}")
    public List<Transacionador> listarClientes(@PathParam("empresa") int empresa) {
        return repository.listarClientes(empresa);
    }

    @DELETE
    @Path("/limpar-tudo")
    public CompraResponse limparTudo() {
        try {
            int total = repository.limparTudo();
            return new CompraResponse(true, "Tudo limpo! " + total + " registros removidos.", 0);
        } catch (Exception e) {
            return new CompraResponse(false, "Erro ao limpar: " + e.getMessage(), 0);
        }
    }

    @GET
    @Path("/produtos")
    public List<Produto> listarProdutos() {
        return repository.listarProdutos();
    }

    @POST
    @Path("/comprar")
    public CompraResponse comprar(CompraRequest request) {
        return repository.inserirVenda(request);
    }

    @DELETE
    @Path("/vendas/{emp}/{num}/{ccf}/{dtr}")
    public CompraResponse excluir(@PathParam("emp") int emp, @PathParam("num") int num,
                                   @PathParam("ccf") int ccf, @PathParam("dtr") String dtr) {
        return repository.excluirVenda(emp, num, ccf, dtr);
    }

    @GET
    @Path("/vendas-recentes")
    public List<Map<String, Object>> vendasRecentes(@QueryParam("limite") @DefaultValue("20") int limite) {
        return repository.listarVendasRecentes(limite);
    }
}
