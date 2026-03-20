import { useState, useEffect } from 'react';

interface Empresa {
  cod: number;
  nome: string;
}

interface Cliente {
  emp: number;
  cod: number;
  nome: string;
}

interface Produto {
  pro: number;
  descricao: string;
}

interface ItemVenda {
  produto: number;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
}

interface Venda {
  emp: number;
  num: number;
  ccf: number;
  dtr: string;
  vtm: number;
  pro: number;
  qnt: number;
  vtl: number;
}

function App() {
  const [empresas, setEmpresas] = useState<Empresa[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [produtos, setProdutos] = useState<Produto[]>([]);
  const [vendas, setVendas] = useState<Venda[]>([]);
  const [itens, setItens] = useState<ItemVenda[]>([]);
  const [produto, setProduto] = useState(0);
  const [quantidade, setQuantidade] = useState(100);
  const [valorUnitario, setValorUnitario] = useState(5.25);
  const [data, setData] = useState(() => new Date().toISOString().split('T')[0]);
  const [mensagem, setMensagem] = useState('');
  const [mensagemTipo, setMensagemTipo] = useState<'sucesso' | 'erro' | ''>('');
  const [enviando, setEnviando] = useState(false);
  const [confirmacao, setConfirmacao] = useState<{ msg: string; onOk: () => void } | null>(null);

  const [empresa, setEmpresa] = useState(0);
  const [cliente, setCliente] = useState(0);

  useEffect(() => {
    carregarEmpresas();
    carregarProdutos();
    carregarVendas();
  }, []);

  async function carregarEmpresas() {
    try {
      const res = await fetch('/api/empresas');
      const d = await res.json();
      setEmpresas(d);
      if (d.length > 0) {
        const empInicial = d[0].cod;
        setEmpresa(empInicial);
        await carregarClientes(empInicial, true);
      }
    } catch (e) { console.error(e); }
  }

  async function carregarClientes(emp: number, resetarCliente = false) {
    try {
      const res = await fetch(`/api/clientes/${emp}`);
      const d = await res.json();
      setClientes(d);
      if (resetarCliente) {
        const tem1005 = d.some((c: Cliente) => c.cod === 1005);
        setCliente(tem1005 ? 1005 : d.length > 0 ? d[0].cod : 0);
      }
    } catch (e) { console.error(e); }
  }

  async function carregarProdutos() {
    try {
      const res = await fetch('/api/produtos');
      const d = await res.json();
      setProdutos(d);
      const tem10 = d.some((p: Produto) => p.pro === 10);
      setProduto(tem10 ? 10 : d.length > 0 ? d[0].pro : 0);
    } catch (e) { console.error(e); }
  }

  async function carregarVendas() {
    try {
      const res = await fetch('/api/vendas-recentes?limite=30');
      setVendas(await res.json());
    } catch (e) { console.error(e); }
  }

  function adicionarItem() {
    const prod = produtos.find(p => p.pro === produto);
    if (!prod || quantidade <= 0) return;
    setItens(prev => [...prev, {
      produto: prod.pro,
      descricao: prod.descricao,
      quantidade,
      valorUnitario,
    }]);
  }

  function removerItem(index: number) {
    setItens(prev => prev.filter((_, i) => i !== index));
  }

  async function registrarVenda() {
    if (itens.length === 0) return;
    setEnviando(true);
    setMensagem('');
    try {
      const res = await fetch('/api/comprar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          empresa: empresa,
          cliente: cliente,
          data,
          itens: itens.map(i => ({ produto: i.produto, quantidade: i.quantidade, valorUnitario: i.valorUnitario })),
        }),
      });
      const resp = await res.json();
      setMensagem(resp.mensagem);
      setMensagemTipo(resp.sucesso ? 'sucesso' : 'erro');
      if (resp.sucesso) {
        setItens([]);
        carregarVendas();
      }
    } catch (e: any) {
      setMensagem('Erro: ' + e.message);
      setMensagemTipo('erro');
    } finally {
      setEnviando(false);
    }
  }

  async function excluirVenda(v: Venda) {
    try {
      const res = await fetch(`/api/vendas/${v.emp}/${v.num}/${v.ccf}/${v.dtr}`, { method: 'DELETE' });
      const resp = await res.json();
      setMensagem(resp.mensagem);
      setMensagemTipo(resp.sucesso ? 'sucesso' : 'erro');
      if (resp.sucesso) carregarVendas();
    } catch (e: any) {
      setMensagem('Erro: ' + e.message);
      setMensagemTipo('erro');
    }
  }

  const totalItens = itens.reduce((s, i) => s + i.quantidade * i.valorUnitario, 0);

  // Agrupar vendas por nota
  const grupos: Record<number, Venda[]> = {};
  vendas.forEach(v => { if (!grupos[v.num]) grupos[v.num] = []; grupos[v.num].push(v); });

  return (
    <div className="app-shell">
      <div className="app-header">
        <div className="app-title">Simulador de Venda SysmoS1</div>
      </div>

      <div className="app-main">
        {/* Cabeçalho da Venda */}
        <div className="section-card">
          <div className="section-title">Cabeçalho da Venda</div>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Empresa</label>
              <select className="form-select" value={empresa} onChange={e => { const emp = Number(e.target.value); setEmpresa(emp); carregarClientes(emp); }}>
                <option value={0} disabled>Selecione...</option>
                {empresas.map(e => <option key={e.cod} value={e.cod}>{e.cod} - {e.nome}</option>)}
              </select>
            </div>
            <div className="form-group flex-2">
              <label className="form-label">Cliente</label>
              <select className="form-select" value={cliente} onChange={e => setCliente(Number(e.target.value))}>
                <option value={0} disabled>Selecione...</option>
                {clientes.map(c => <option key={c.cod} value={c.cod}>{c.cod} - {c.nome}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Data</label>
              <input type="date" className="form-input" value={data} onChange={e => setData(e.target.value)} />
            </div>
            <div className="form-group flex-0">
              <label className="form-label">Serie</label>
              <div className="form-input readonly">1</div>
            </div>
          </div>
        </div>

        {/* Adicionar Produto */}
        <div className="section-card">
          <div className="section-title">Adicionar Produto</div>
          <div className="form-row">
            <div className="form-group flex-2">
              <label className="form-label">Produto</label>
              <select className="form-select" value={produto} onChange={e => setProduto(Number(e.target.value))}>
                <option value={0} disabled>Selecione um produto...</option>
                {produtos.map(p => <option key={p.pro} value={p.pro}>{p.pro} - {p.descricao}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Qtd</label>
              <input type="number" className="form-input" value={quantidade} onChange={e => setQuantidade(Number(e.target.value))} min={1} />
            </div>
            <div className="form-group">
              <label className="form-label">Vl. Unit.</label>
              <input type="number" step="0.01" className="form-input" value={valorUnitario} onChange={e => setValorUnitario(Number(e.target.value))} />
            </div>
            <button className="btn-add" onClick={adicionarItem}>+</button>
          </div>
        </div>

        {/* Itens da Venda */}
        {itens.length > 0 && (
          <div className="section-card">
            <div className="section-title">Itens da Venda ({itens.length})</div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Qtd</th>
                  <th>Vl.Unit</th>
                  <th>Total</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {itens.map((item, i) => (
                  <tr key={i}>
                    <td>{item.produto} - {item.descricao}</td>
                    <td className="cell-number">{item.quantidade}</td>
                    <td className="cell-number">R$ {item.valorUnitario.toFixed(2)}</td>
                    <td className="cell-number">R$ {(item.quantidade * item.valorUnitario).toFixed(2)}</td>
                    <td>
                      <button className="btn-remove" onClick={() => removerItem(i)}>X</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="card-footer">
              <div className="total-label">Total: R$ {totalItens.toFixed(2)}</div>
              <button className="btn-vender" onClick={registrarVenda} disabled={enviando}>
                {enviando ? 'Registrando...' : 'Registrar Venda'}
              </button>
            </div>
          </div>
        )}

        {mensagem && (
          <div className={mensagemTipo === 'sucesso' ? 'msg-sucesso' : 'error-message'}>{mensagem}</div>
        )}

        {/* Vendas recentes */}
        <div className="section-card">
          <div className="section-header-row">
            <div className="section-title" style={{ marginBottom: 0 }}>Vendas</div>
            <button className="btn-limpar" onClick={() => setConfirmacao({
              msg: 'Tem certeza que deseja excluir TODAS as vendas? Esta ação não pode ser desfeita.',
              onOk: async () => {
                setConfirmacao(null);
                try {
                  const res = await fetch('/api/limpar-tudo', { method: 'DELETE' });
                  const resp = await res.json();
                  setMensagem(resp.mensagem);
                  setMensagemTipo(resp.sucesso ? 'sucesso' : 'erro');
                  if (resp.sucesso) carregarVendas();
                } catch (e: any) {
                  setMensagem('Erro: ' + e.message);
                  setMensagemTipo('erro');
                }
              }
            })}>Limpar tudo</button>
          </div>
          {vendas.length === 0
            ? <div className="empty-text">Nenhuma venda</div>
            : Object.entries(grupos).map(([num, items]) => {
                const total = items.reduce((s, i) => s + i.vtl, 0);
                return (
                  <div key={num} className="venda-grupo">
                    <div className="venda-grupo-header">
                      <span className="nota">Nota {num}</span>
                      <span className="data">{items[0].dtr}</span>
                      <span className="total">
                        {total.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
                      </span>
                      <button className="btn-excluir" onClick={() => excluirVenda(items[0])}>Excluir</button>
                    </div>
                    <table className="inner-table">
                      <tbody>
                        {items.map((v, i) => (
                          <tr key={i}>
                            <td className="cell-muted">Prod. {v.pro}</td>
                            <td>Qtd: {v.qnt}</td>
                            <td className="cell-right">
                              {v.vtl.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                );
              })
          }
        </div>
      </div>

      {/* Modal de confirmação */}
      {confirmacao && (
        <div className="modal-overlay">
          <div className="modal-box">
            <div className="modal-title">Confirmação</div>
            <div className="modal-msg">{confirmacao.msg}</div>
            <div className="modal-actions">
              <button className="btn-modal-cancel" onClick={() => setConfirmacao(null)}>Cancelar</button>
              <button className="btn-modal-confirm" onClick={confirmacao.onOk}>Confirmar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
