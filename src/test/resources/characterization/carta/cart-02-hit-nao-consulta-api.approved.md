---
regra: CART-02
status: observado
---
# Cache HIT não consulta a API

A segunda consulta é atendida pelo Redis.

```stub-mtg
GET /cards/386616 -> 200
{"card": {"name": "Narset"}}
```

## Primeira consulta

```preparar
GET /carta/386616
```

## Segunda consulta

```http X-Cache chamadas corpo:fonte
GET /carta/386616
```

```resultado
200
X-Cache: HIT
chamadas à API: (nenhuma)
fonte: "REDIS"
```
