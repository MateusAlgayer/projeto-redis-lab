---
regra: CART-01
status: observado
---
# Cache MISS consulta a API e grava a carta com TTL

O TTL padrão é de 3600 segundos.

```stub-mtg
GET /cards/386616 -> 200
{"card": {"name": "Narset"}}
```

```http X-Cache chamadas ttl
GET /carta/386616
```

```resultado
200
X-Cache: MISS
chamadas à API: GET /cards/386616
carta:386616  ttl=~60min
```
