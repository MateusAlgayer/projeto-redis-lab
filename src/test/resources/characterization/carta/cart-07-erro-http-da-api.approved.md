---
regra: CART-07
status: observado
---
# Erro HTTP da API resulta em 503 sem gravar no cache

Vale para erro interno e para limite de requisições.

## Erro interno

```stub-mtg
GET /cards/386616 -> 500
```

```http ttl
GET /carta/386616
```

```resultado
503
redis: (vazio)
```

## Limite de requisições

```stub-mtg
GET /cards/386616 -> 429
```

```http ttl
GET /carta/386616
```

```resultado
503
redis: (vazio)
```
