---
regra: CART-12
status: observado
---
# A chave do cache usa o identificador pesquisado

A mesma carta buscada por outro identificador não aproveita o cache.

```stub-mtg
GET /cards/386616 -> 200
{"card": {"id": "abc123"}}
```

```stub-mtg
GET /cards/abc123 -> 200
{"card": {"id": "abc123"}}
```

```preparar
GET /carta/386616
```

## Buscar a mesma carta pelo id dela

```http X-Cache chamadas ttl
GET /carta/abc123
```

```resultado
200
X-Cache: MISS
chamadas à API: GET /cards/abc123
carta:386616  ttl=~60min
carta:abc123  ttl=~60min
```
