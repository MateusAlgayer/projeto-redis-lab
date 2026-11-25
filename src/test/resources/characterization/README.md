# Testes de caracterização (approved fixtures)

Cada arquivo `*.approved.md` descreve uma regra do sistema: as entradas e o comportamento observado
pela fronteira HTTP, pelo Redis e pelas chamadas à API Magic: The Gathering.

**Aprovado não significa correto.** O campo `status` indica a confiança na regra:

| Status | Significado |
|---|---|
| `observado` | registra o comportamento atual, ainda sem confirmação de que é o desejado |
| `suspeito` | registra o comportamento atual, que parece ser um defeito |
| `regra` | o time confirmou que o comportamento é o desejado |

## Fluxo

```bash
./mvnw test -Dtest='CaracterizacaoTest,CatalogoDeRegrasTest'   # requer Docker
```

1. Se a saída difere do aprovado, o teste falha e grava um `*.received.md` ao lado.
2. Revise a diferença: `git diff --no-index arquivo.approved.md arquivo.received.md`.
3. Se você aceitar o comportamento, aprove o arquivo: `mv arquivo.received.md arquivo.approved.md`.
4. Rode de novo. Um teste que passa remove o próprio `*.received.md`.

Um fixture ainda não aprovado tem apenas as entradas, então falha até a primeira aprovação.

## Regras de mudança

- Refatoração não altera nenhum `*.approved.md`.
- Mudança de comportamento leva código e fixture no mesmo commit.
- A aprovação é sempre manual, um arquivo por vez. A CI não aprova nada.
- Os `*.received.md` não são versionados.
- Regra nova exige um fixture e uma linha no catálogo (o `CatalogoDeRegrasTest` confere).

## Formato

Escreva somente a prosa e os blocos de entrada. Os blocos `resultado` são gerados.

| Bloco | Conteúdo |
|---|---|
| `http` | passo verificado: método e caminho na primeira linha; corpo JSON nas seguintes |
| `preparar` | requisição que só monta o cenário: exige status 2xx e não gera saída |
| `stub-mtg` | `GET /cards/{id} -> status` ou `-> desconectar`; corpo nas linhas seguintes |
| `esperar` | duração, por exemplo `4s` |

### O que cada passo valida

**Registre apenas o que a regra valida.** Um bloco `http` sempre registra o status. O restante
só aparece quando é pedido na linha de abertura, na ordem escrita, por exemplo
` ```http X-Cache chamadas ttl `.

| Opção | Registra |
|---|---|
| `X-Cache`, `Location` | o header |
| `chamadas` | as chamadas feitas à API externa durante o passo |
| `ttl` | as chaves do Redis e o TTL de cada uma, sem o valor |
| `redis:a,b.c` | o mesmo que `ttl`, mais os campos indicados do valor |
| `corpo:a,b.c` | os campos indicados da resposta; em listas, o valor de cada item |
| `corpo`, `redis` | o conteúdo inteiro: reserve para os cenários de contrato |

O que um cenário não pede, ele não detecta. O formato completo da resposta e do valor no Redis
fica em um cenário de contrato por recurso (`CART-13` e `PROM-16`).

Datas relativas usam `{{agora+1h}}` ou `{{agora-1d}}` (unidades `s`, `m`, `h`, `d`).

## Normalizações

- TTL positivo é arredondado para cima em minutos (`~60min`). Diferenças de poucos segundos não são detectadas, e um cenário não pode demorar perto de 1 minuto.
- TTL `-1` aparece como `sem-expiracao` e `-2` como `inexistente`.
- Datas informadas por placeholder voltam a aparecer como placeholder.
- Chaves JSON são ordenadas; `/promocao/listar` também é ordenada por nome.
- Do header `Location` aparece só o caminho. Somente `Location` e `X-Cache` podem ser pedidos.

## Fora do alcance destes fixtures

- Timeout da API (só a queda de conexão é simulada).
- Expiração natural de uma carta e TTL de cache configurado como zero ou negativo.
- Data de expiração a menos de 1 segundo no futuro.
- Promoção sem TTL (`tempoRestante` igual a `-1`): a API não permite mais criar uma.
- `listar()` do repositório de cartas, que não tem endpoint.

## Catálogo de regras

### Promoções

| Regra | Descrição | Status inicial |
|---|---|---|
| PROM-01 | [Promoção com data futura é gravada com TTL](promocao/prom-01-criar-com-expiracao-futura.approved.md) | `observado` |
| PROM-02 | [Promoção exige um nome](promocao/prom-02-nome-obrigatorio.approved.md) | `observado` |
| PROM-03 | [Promoção exige uma data de expiração](promocao/prom-03-criar-sem-data.approved.md) | `observado` |
| PROM-04 | [Promoção com data passada é rejeitada](promocao/prom-04-criar-com-data-passada.approved.md) | `observado` |
| PROM-05 | [Criar com nome já usado é rejeitado](promocao/prom-05-nome-repetido-substitui.approved.md) | `observado` |
| PROM-06 | [O percentual de desconto deve estar entre 0 e 100](promocao/prom-06-percentual-sem-validacao.approved.md) | `observado` |
| PROM-07 | [Consulta por nome](promocao/prom-07-consultar-por-nome.approved.md) | `observado` |
| PROM-08 | [Verificação de existência](promocao/prom-08-verificar-existencia.approved.md) | `observado` |
| PROM-09 | [Tempo restante de uma promoção](promocao/prom-09-tempo-restante.approved.md) | `observado` |
| PROM-10 | [Listagem de promoções](promocao/prom-10-listar.approved.md) | `observado` |
| PROM-11 | [Prorrogar para uma data futura redefine o TTL](promocao/prom-11-prorrogar-data-futura.approved.md) | `observado` |
| PROM-12 | [Prorrogar para uma data passada é rejeitado](promocao/prom-12-prorrogar-data-passada.approved.md) | `observado` |
| PROM-13 | [Prorrogação exige a nova data](promocao/prom-13-prorrogar-exige-nova-data.approved.md) | `observado` |
| PROM-14 | [Prorrogar uma promoção inexistente não a cria](promocao/prom-14-prorrogar-inexistente.approved.md) | `observado` |
| PROM-15 | [Promoção expira automaticamente](promocao/prom-15-expiracao-automatica.approved.md) | `observado` |
| PROM-16 | [Contrato da promoção na resposta e no Redis](promocao/prom-16-contrato-da-promocao.approved.md) | `observado` |

### Cartas

| Regra | Descrição | Status inicial |
|---|---|---|
| CART-01 | [Cache MISS consulta a API e grava a carta com TTL](carta/cart-01-miss-consulta-api-e-grava.approved.md) | `observado` |
| CART-02 | [Cache HIT não consulta a API](carta/cart-02-hit-nao-consulta-api.approved.md) | `observado` |
| CART-03 | [Consultar presença e TTL não popula o cache](carta/cart-03-presenca-e-ttl-no-cache.approved.md) | `observado` |
| CART-04 | [Invalidar o cache força uma nova consulta à API](carta/cart-04-invalidacao-forca-novo-miss.approved.md) | `observado` |
| CART-05 | [Invalidar uma carta ausente do cache resulta em 404](carta/cart-05-invalidar-ausente.approved.md) | `observado` |
| CART-06 | [Carta inexistente não é guardada no cache](carta/cart-06-carta-inexistente-sem-cache-negativo.approved.md) | `observado` |
| CART-07 | [Erro HTTP da API resulta em 503 sem gravar no cache](carta/cart-07-erro-http-da-api.approved.md) | `observado` |
| CART-08 | [Falha de comunicação com a API resulta em 503](carta/cart-08-falha-de-comunicacao.approved.md) | `observado` |
| CART-09 | [Resposta da API sem carta é tratada como inexistente](carta/cart-09-resposta-sem-carta.approved.md) | `observado` |
| CART-10 | [Carta em cache continua disponível quando a API falha](carta/cart-10-cache-atende-com-api-fora.approved.md) | `observado` |
| CART-11 | [Identificador em branco não chega à API nem ao cache](carta/cart-11-identificador-em-branco.approved.md) | `observado` |
| CART-12 | [A chave do cache usa o identificador pesquisado](carta/cart-12-chave-usa-id-da-busca.approved.md) | `observado` |
| CART-13 | [Contrato da carta na resposta e no cache](carta/cart-13-contrato-da-carta.approved.md) | `observado` |
