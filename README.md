# Simuladores

Simulador de venda do Sysmo S1: grava notas de saída de teste direto na base local do
S1, para exercitar telas e rotinas que dependem de venda sem passar pelo PDV.

```
backend/    Quarkus 3.8 + Java 17 + PostgreSQL (Agroal)   API  em 127.0.0.1:5001
frontend/   React 18 + TypeScript + Vite                  site em localhost:8001
```

## Rodar

Pelo Painel do SysmoDev, bloco **Desenvolvimento**, projeto **Simuladores**. Ou à mão:

```
cd backend  && mvnw.cmd quarkus:dev
cd frontend && npm run dev
```

O Vite repassa `/api` para a 5001, então o site não precisa saber onde a API está.

`npm install` tem de ser executado **no Windows**: feito pelo WSL, o `node_modules` sai
sem os atalhos `.cmd` e com o `esbuild` de Linux, e o `npm run dev` falha.

## Qual base ele usa

A mesma do S1 instalado na máquina. `DbxConfigSource` lê `C:\SysmoVs\dbxconnections.ini`,
seção `[Delphi]`, chave `Database=host:porta/base`, e monta a URL JDBC a partir dela.
Trocar a base do S1 troca a do simulador, sem configurar nada aqui.

> Sem o `dbxconnections.ini` o datasource fica sem URL e nenhuma rota consegue
> conectar. O motivo aparece no log como `[DbxConfigSource] Arquivo nao encontrado`.

Usuário e senha são os padrões de desenvolvimento, fixos em `DbxConfigSource`. Esse
simulador serve só para base local: apontado para uma base de cliente, ele gravaria nota.

## O que ele grava

Nota de saída em `GCENFS01` e itens em `GCEITM01`, com `MOD='E'`, `OP1='S'`, `SER='1'`.

A numeração começa em **900001** (`proximoNumeroNota`): a faixa alta separa as notas
simuladas das reais, e é por ela que as exclusões se orientam.

| Rota | O que faz |
|---|---|
| `GET /api/empresas` | empresas (`SPSEMP00` + `TRSTRA01`) |
| `GET /api/clientes/{empresa}` | transacionadores da empresa |
| `GET /api/produtos` | produtos (`GCEPRO02`) |
| `POST /api/comprar` | grava a nota e os itens numa transação |
| `GET /api/vendas-recentes` | últimas notas simuladas (`NUM >= 900000`, `PDV=0`) |
| `DELETE /api/vendas/{emp}/{num}/{ccf}/{dtr}` | exclui uma nota simulada |
| `DELETE /api/limpar-tudo` | apaga todas as notas com `NUM >= 900000` |

> `limpar-tudo` apaga pela faixa de número (`MOD='E'`, `OP1='S'`, `NUM >= 900000`), e não
> por marca de origem. Nota real de saída numerada nessa faixa seria apagada junto.
