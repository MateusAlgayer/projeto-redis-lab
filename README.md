# 🏷️ Projeto Redis Lab — Gerenciamento de Promoções

API REST para criação, consulta e gerenciamento do ciclo de vida de promoções com **expiração automática** utilizando **Redis**, construída com **Quarkus** e acompanhada de um cliente web interativo em **Streamlit**.

---

## 📐 Arquitetura

```
┌──────────────────┐        HTTP/JSON        ┌──────────────────────────┐       Redis Protocol       ┌───────────┐
│  Streamlit App   │ ◄────────────────────►  │  Quarkus REST API        │ ◄────────────────────────► │   Redis   │
│  (Python client) │   polling a cada 1s     │  (PromocaoResource.java) │   SET/GET/EXPIRE/TTL/...   │  (Cache)  │
└──────────────────┘                         └──────────────────────────┘                            └───────────┘
```

### Estrutura do projeto

```
src/main/java/org/acme/
├── api/
│   ├── PromocaoResource.java          # Endpoints REST (JAX-RS)
│   └── dto/
│       ├── PromocaoDTO.java           # DTO de promoção
│       └── ProrrogarPromocaoDTO.java  # DTO de prorrogação
├── domain/
│   └── Promocao.java                  # Entidade de domínio
├── repository/
│   └── PromocaoRepository.java        # Interface do repositório
├── service/
│   └── PromocaoService.java           # Regras de negócio
└── infra/redis/
    └── RedisPromocaoService.java      # Implementação Redis do repositório

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

---

## 🛠️ Tecnologias

| Camada     | Tecnologia                          |
|------------|-------------------------------------|
| Backend    | Java 25 + Quarkus 3.38             |
| Cache      | Redis (via Quarkus Redis Client)    |
| API Docs   | SmallRye OpenAPI + Swagger UI       |
| Frontend   | Python + Streamlit                  |
| Build      | Maven                               |
