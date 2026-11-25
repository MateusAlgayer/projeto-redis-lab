---
regra: CART-04
status: observado
---
# Invalidar o cache força uma nova consulta à API

Depois do evict, a próxima consulta é um MISS.

```stub-mtg
GET /cards/386616 -> 200
{"card": {"name": "Narset"}}
```

```preparar
GET /carta/386616
```

## Invalidar

```http ttl
DELETE /carta/386616/cache
```

```resultado
204
redis: (vazio)
```

## Consultar novamente

```http X-Cache chamadas
GET /carta/386616
```

```resultado
200
X-Cache: MISS
chamadas à API: GET /cards/386616
```
