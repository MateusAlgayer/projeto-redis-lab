---
regra: CART-03
status: observado
---
# Consultar presença e TTL não popula o cache

Essas consultas leem apenas o Redis e nunca chamam a API.

```stub-mtg
GET /cards/386616 -> 200
{"card": {"name": "Narset"}}
```

## Antes de a carta estar no cache

```http chamadas ttl
GET /carta/386616/cache
```

```resultado
404
chamadas à API: (nenhuma)
redis: (vazio)
```

```http
GET /carta/386616/cache/tempoRestante
```

```resultado
404
```

## Depois de a carta estar no cache

```preparar
GET /carta/386616
```

```http chamadas
GET /carta/386616/cache
```

```resultado
200
chamadas à API: (nenhuma)
```

```http corpo chamadas
GET /carta/386616/cache/tempoRestante
```

```resultado
200
~60min
chamadas à API: (nenhuma)
```
