---
regra: CART-09
status: observado
---
# Resposta da API sem carta é tratada como inexistente

A API responde 200, mas sem o conteúdo da carta.

```stub-mtg
GET /cards/386616 -> 200
{"card": null}
```

```http ttl
GET /carta/386616
```

```resultado
404
redis: (vazio)
```
