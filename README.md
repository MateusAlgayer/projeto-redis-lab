# 🏷️ Projeto Redis Lab — Gerenciamento de Promoções

API REST para criação, consulta e gerenciamento do ciclo de vida de promoções com **expiração automática** utilizando **Redis**, construída com **Quarkus** e acompanhada de um cliente web interativo em **Streamlit**.

Inclui também um **cache-aside** de cartas de *Magic: The Gathering*: a API consulta a [Magic: The Gathering API](https://docs.magicthegathering.io/) e grava o resultado no Redis, servindo as próximas consultas direto do cache.

---

## 📐 Arquitetura

```
                          ┌──────────────────────────────┐
                          │  api.magicthegathering.io    │
                          │  (fonte de dados das cartas) │
                          └──────────────▲───────────────┘
                                         │ 2) miss: GET /v1/cards/{id}
┌──────────────────┐        HTTP/JSON     │
│  Streamlit App   │ ◄──────────────────┐ │
│  (Python client) │   polling a cada 1s│ │
└──────────────────┘                    ▼ ▼
                       ┌────────────────────────────────┐  Redis Protocol  ┌───────────┐
                       │  Quarkus REST API              │ ◄──────────────► │   Redis   │
                       │  PromocaoResource / CartaResource │  GET/SET/EXPIRE │  (Cache) │
                       └────────────────────────────────┘  TTL/DEL/KEYS...  └───────────┘
                                         ▲
                                         │ 1) cache HIT: lê direto do Redis
```

### Estrutura do projeto

```
src/main/java/org/acme/
├── api/
│   ├── PromocaoResource.java          # Endpoints REST (JAX-RS) de promoções
│   ├── CartaResource.java             # Endpoints REST do cache-aside de cartas
│   └── dto/
│       ├── PromocaoDTO.java           # DTO de promoção
│       ├── ProrrogarPromocaoDTO.java  # DTO de prorrogação
│       └── CartaCacheDTO.java         # DTO da carta + metadados de cache
├── infra/
│   ├── api/
│   │   ├── MtgApiClient.java          # REST Client (MicroProfile) da API MTG
│   │   └── MtgApiException.java       # Erro de comunicação com a API MTG
│   └── redis/
│       ├── RedisPromocaoService.java  # Implementação Redis do repositório
│       └── RedisCartaRepository.java  # Implementação Redis do cache-aside
├── domain/
│   ├── Promocao.java                  # Entidade de domínio
│   └── Carta.java                     # Entidade de domínio (carta MTG)
├── repository/
│   ├── PromocaoRepository.java        # Interface do repositório
│   └── CartaRepository.java           # Interface do cache de cartas
└── service/
    ├── PromocaoService.java           # Regras de negócio de promoções
    └── CartaService.java              # Orquestração do cache-aside

streamlit_app.py                       # Cliente web Streamlit
requirements.txt                       # Dependências Python
```

---

## 🚀 Como Executar

### Pré-requisitos

| Ferramenta  | Versão mínima |
|-------------|---------------|
| Java (JDK)  | 25            |
| Maven       | 3.9+          |
| Docker      | 20+ (para DevServices) |
| Python      | 3.10+         |
| Streamlit   | 1.33+         |

### 1️⃣ Servidor — API Quarkus

Em modo de desenvolvimento, o Quarkus **sobe automaticamente um container Redis** via [DevServices](https://quarkus.io/guides/redis-dev-services) — basta ter o Docker rodando.

```bash
# Na raiz do projeto
./mvnw quarkus:dev
```

A API estará disponível em:

| Recurso     | URL                                  |
|-------------|--------------------------------------|
| API REST    | http://localhost:8080/promocao       |
| Swagger UI  | http://localhost:8080/q/swagger-ui   |
| OpenAPI     | http://localhost:8080/q/openapi      |

> **Produção:** configure `%prod.quarkus.redis.hosts` em `application.properties` para apontar para seu Redis. O padrão é `redis://localhost:6379`.

### 2️⃣ Cliente — Streamlit

```bash
# Instalar dependências
pip install -r requirements.txt

# Executar o cliente
streamlit run streamlit_app.py
```

O app abre em http://localhost:8501 com quatro abas:

| Aba                      | Funcionalidade                                            |
|--------------------------|-----------------------------------------------------------|
| 📋 Listar Promoções      | Lista todas as promoções ativas com **polling automático a cada 2s** |
| 🔍 Consultar / Verificar | Busca detalhes, verifica existência e consulta TTL        |
| ➕ Nova Promoção          | Formulário para criação de promoção                       |
| ⏳ Prorrogar Promoção     | Estende a validade de uma promoção existente              |

> **Dica:** A URL base da API é configurável na sidebar (padrão: `http://localhost:8080`).

---

## 🔌 Endpoints da API

| Método | Endpoint                            | Descrição                                |
|--------|-------------------------------------|------------------------------------------|
| POST   | `/promocao`                         | Cria uma nova promoção                   |
| GET    | `/promocao/{nome}`                  | Consulta promoção por nome               |
| GET    | `/promocao/existe/{nome}`           | Verifica se a promoção existe            |
| GET    | `/promocao/tempoRestante/{nome}`    | Retorna o TTL em segundos                |
| GET    | `/promocao/listar`                  | Lista todas as promoções ativas          |
| PUT    | `/promocao/prorrogar/{nome}`        | Prorroga a validade de uma promoção      |

### Endpoints — Cartas (cache-aside)

| Método | Endpoint                            | Descrição                                                     |
|--------|-------------------------------------|---------------------------------------------------------------|
| GET    | `/carta/{id}`                       | Consulta a carta aplicando o cache-aside (header `X-Cache`)   |
| GET    | `/carta/{id}/cache`                 | Verifica se a carta está no cache (200 sim / 404 não)         |
| GET    | `/carta/{id}/cache/tempoRestante`   | Retorna o TTL restante da entrada no cache                    |
| DELETE | `/carta/{id}/cache`                 | Invalida a entrada da carta no cache (evict)                  |

> `{id}` aceita o SHA1 do card ou o `multiverseid` (ex.: `386616`).

### Exemplo de payload — Criar promoção

```json
{
  "nome": "blackfriday",
  "percentualDesconto": 25.0,
  "dataExpiracao": "2026-12-01T23:59:59"
}
```

### Exemplo de payload — Prorrogar promoção

```json
{
  "novaDataExpiracao": "2027-01-31T23:59:59"
}
```

---

## 🃏 Cache-Aside de Cartas (Magic: The Gathering)

Implementação do padrão **cache-aside** (*lazy loading*) em que a aplicação — e não o cliente do cache — é responsável por ler o Redis e, em caso de *miss*, ir até a fonte de dados:

```
GET /carta/386616
      │
      ▼
 ┌──────────────┐   hit    ┌────────────────────────────┐
 │  Redis       │ ───────► │ retorna a carta            │  X-Cache: HIT   (fonte: REDIS)
 │  carta:{id}  │          └────────────────────────────┘
 └──────┬───────┘
        │ miss
        ▼
 ┌──────────────────────────┐   404   ┌──────────────┐
 │ api.magicthegathering.io │ ──────► │ HTTP 404     │
 │ GET /v1/cards/{id}       │         └──────────────┘
 └──────┬───────────────────┘
        │ 200
        ▼
 ┌────────────────────────────┐
 │ SET carta:{id} + EXPIRE    │  popula o cache com TTL (padrão 3600s)
 │ retorna a carta            │  X-Cache: MISS  (fonte: API)
 └────────────────────────────┘
```

**Fluxo ([`CartaService.buscarComCache`](src/main/java/org/acme/service/CartaService.java)):**

1. `GET carta:{id}` no Redis — se existir, devolve com `X-Cache: HIT`;
2. em caso de *miss*, chama a API Magic: The Gathering via [`MtgApiClient`](src/main/java/org/acme/infra/api/MtgApiClient.java) (REST Client do MicroProfile, `GET /v1/cards/{id}`);
3. grava a resposta no Redis com `SET` + `EXPIRE` e devolve com `X-Cache: MISS`.

> **Por que cache-aside?** A API externa tem *rate limit* de 5.000 requisições/hora; o cache reduz drasticamente as chamadas e protege a aplicação caso a fonte de dados fique indisponível (o endpoint responde `503` nesse caso).

### Exemplo de resposta

```json
{
  "carta": {
    "id": "15e45fe0-92ea-52dc-8665-7105ac30db70",
    "name": "Narset, Enlightened Master",
    "manaCost": "{3}{U}{R}{W}",
    "cmc": 6.0,
    "rarity": "Mythic Rare",
    "set": "KTK"
  },
  "cacheHit": true,
  "fonte": "REDIS",
  "tempoRestanteCache": 3594
}
```

### Configuração

| Propriedade | Padrão | Descrição |
|---|---|---|
| `mtg.api.base-url` | `https://api.magicthegathering.io/v1` | URL base da API Magic: The Gathering |
| `quarkus.rest-client.mtgApiUrl.url` | `${mtg.api.base-url}` | URL do REST Client (`configKey = mtgApiUrl`) |
| `quarkus.rest-client.mtgApiUrl.connect-timeout` | `10000` | Timeout de conexão (ms) |
| `quarkus.rest-client.mtgApiUrl.read-timeout` | `10000` | Timeout de leitura (ms) |
| `carta.cache.ttl-segundos` | `3600` | TTL da entrada no cache |

A chave no Redis segue o padrão **`carta:{id}`** (ex.: `carta:386616`).

### Comandos Redis usados pelo cache-aside

| Operação                          | Comandos Redis   |
|-----------------------------------|------------------|
| Consulta (leitura)                | `GET`            |
| Preencher o cache após o *miss*   | `SET` → `EXPIRE` |
| Verificar presença no cache       | `EXISTS`         |
| TTL restante da carta             | `TTL`            |
| Invalidar manualmente (evict)     | `DEL`            |

```bash
# Exemplos
curl -i http://localhost:8080/carta/386616            # 1ª chamada: X-Cache: MISS
curl -i http://localhost:8080/carta/386616            # 2ª chamada: X-Cache: HIT
curl -i http://localhost:8080/carta/386616/cache      # 200 se estiver no cache
curl -i -X DELETE http://localhost:8080/carta/386616/cache   # invalida a entrada
```

---

## 🗄️ Comandos Redis Utilizados

A integração com o Redis é feita na classe [`RedisPromocaoService.java`](src/main/java/org/acme/infra/redis/RedisPromocaoService.java) através da API tipada do Quarkus (`RedisDataSource`), que expõe dois grupos de comandos:

- **`ValueCommands<String, Promocao>`** — operações sobre valores (`SET`, `GET`)
- **`KeyCommands<String>`** — operações sobre chaves (`EXPIRE`, `TTL`, `EXISTS`, `KEYS`)

Todas as chaves seguem o padrão **`promocao:{nome}`** (ex: `promocao:blackfriday`).

### `SET` — Gravar promoção

```java
valueCommands.set(key, promocao);
```

Serializa o objeto `Promocao` como JSON e armazena no Redis com a chave `promocao:{nome}`.

**Usado em:** `add()` (criar) e `prorrogar()` (atualizar dados antes de redefinir o TTL).

| Complexidade | Documentação Redis |
|--------------|--------------------|
| O(1)         | [SET](https://redis.io/commands/set) |

---

### `GET` — Recuperar promoção

```java
Promocao promocao = valueCommands.get(promocaoKey(item));
```

Lê a chave e desserializa o JSON de volta para o objeto `Promocao`. Se a chave não existir (expirou ou nunca foi criada), o Quarkus lança `RedisKeyNotFoundException`.

**Usado em:** `get()` (consulta) e internamente em `prorrogar()` para recuperar os dados atuais antes de atualizar.

| Complexidade | Documentação Redis |
|--------------|--------------------|
| O(1)         | [GET](https://redis.io/commands/get) |

---

### `EXPIRE` — Definir tempo de expiração (TTL)

```java
long segundosAteExpirar = ChronoUnit.SECONDS.between(LocalDateTime.now(), promocao.getDataExpiracao());
keyCommands.expire(key, segundosAteExpirar);
```

Calcula a diferença em segundos entre agora e a data de expiração da promoção e configura esse valor como TTL da chave no Redis. Quando o TTL chega a zero, **o Redis remove a chave automaticamente** — a promoção expira sem necessidade de jobs ou cron.

**Usado em:** `add()` (define TTL na criação) e `prorrogar()` (redefine TTL com a nova data).

| Complexidade | Documentação Redis |
|--------------|--------------------|
| O(1)         | [EXPIRE](https://redis.io/commands/expire) |

---

### `EXISTS` — Verificar existência

```java
Boolean existe = keyCommands.exists(promocaoKey(item));
```

Retorna `true` se a chave existe no Redis (promoção ativa) ou `false` se não existe ou já expirou.

**Usado em:** `exists()` — endpoint `GET /promocao/existe/{nome}`.

| Complexidade | Documentação Redis |
|--------------|--------------------|
| O(1)         | [EXISTS](https://redis.io/commands/exists) |

---

### `TTL` — Consultar tempo de vida restante

```java
Long ttl = keyCommands.ttl(promocaoKey(item));
```

Retorna o número de segundos restantes até a chave expirar. Valores especiais:

| Retorno | Significado                                |
|---------|--------------------------------------------|
| `>= 0`  | Segundos restantes até a expiração         |
| `-1`    | Chave existe mas **não tem TTL** definido  |
| `-2`    | Chave **não existe** (expirou ou nunca foi criada) |

**Usado em:** `tempoRestante()` — endpoint `GET /promocao/tempoRestante/{nome}`.

| Complexidade | Documentação Redis |
|--------------|--------------------|
| O(1)         | [TTL](https://redis.io/commands/ttl) |

---

### `KEYS` — Listar chaves por padrão

```java
List<String> keys = keyCommands.keys(promocaoKey("*"));  // padrão: "promocao:*"
```

Busca todas as chaves que correspondem ao padrão glob `promocao:*`, retornando os nomes de todas as promoções ativas. Em seguida, cada chave é consultada com `GET` para montar a lista completa.

**Usado em:** `listar()` — endpoint `GET /promocao/listar`.

| Complexidade | Documentação Redis |
|--------------|--------------------|
| O(N)         | [KEYS](https://redis.io/commands/keys) |

> ⚠️ **Nota:** O comando `KEYS` faz varredura completa e pode impactar a performance em bases com muitas chaves. Para produção com grande volume, considere substituir por [`SCAN`](https://redis.io/commands/scan) que faz a iteração de forma incremental.

---

### Resumo visual dos comandos por operação

```
┌─────────────────┬────────────────────────────────────┐
│    Operação      │     Comandos Redis utilizados      │
├─────────────────┼────────────────────────────────────┤
│ Criar promoção   │  SET  →  EXPIRE                   │
│ Consultar        │  GET                               │
│ Verificar exist. │  EXISTS                            │
│ Tempo restante   │  TTL                               │
│ Listar todas     │  KEYS  →  GET (para cada chave)   │
│ Prorrogar        │  GET  →  SET  →  EXPIRE           │
└─────────────────┴────────────────────────────────────┘
```

---

## 🧪 Testes

```bash
# Executar testes unitários (DevServices sobe Redis automaticamente)
./mvnw test
```

### Teste de estresse do cache-aside

O script [`stress_test.py`](stress_test.py) executa duas fases de carga em **paralelo** (`ThreadPoolExecutor`) contra `/carta/{id}`:

1. **populate** — busca 15 cartas simultaneamente (espera-se `X-Cache: MISS` → cada miss consulta a API MTG e grava no Redis);
2. **cache** — busca as mesmas cartas de novo (espera-se `X-Cache: HIT`, só Redis).

```bash
./mvnw quarkus:dev          # API no ar
pip install -r requirements.txt

python stress_test.py                                            # padrão: 15 cartas, 8 threads
python stress_test.py --repeticoes 3 --concorrencia 12           # 3 rodadas na fase de cache
python stress_test.py --cartas 30 --concorrencia 12              # carga maior
```

O relatório imprime tempo total, latência (min/p50/média/p95/max), distribuição HIT/MISS, speedup entre as fases e sai com código `1` quando há falhas HTTP ou misses inesperados.

| Opção                | Padrão               | Descrição                                    |
|----------------------|----------------------|----------------------------------------------|
| `--base-url`         | `http://localhost:8080` | URL base da API Quarkus                    |
| `--cartas`           | `15`                 | Quantidade de cartas                         |
| `--concorrencia`     | `8`                  | Threads em paralelo                          |
| `--repeticoes`       | `1`                  | Rodadas da fase de cache                     |
| `--sem-limpar-cache` | desligado            | Não invalida o cache antes de começar        |

> Os ids das cartas são obtidos da própria API Magic: The Gathering (`/v1/cards?pageSize=N`) antes da carga.

---

## 🛠️ Tecnologias

| Camada     | Tecnologia                          |
|------------|-------------------------------------|
| Backend    | Java 25 + Quarkus 3.38             |
| Cache      | Redis (via Quarkus Redis Client)    |
| API Docs   | SmallRye OpenAPI + Swagger UI       |
| Frontend   | Python + Streamlit                  |
| Build      | Maven                               |
