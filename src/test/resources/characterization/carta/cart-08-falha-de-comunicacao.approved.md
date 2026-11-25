---
regra: CART-08
status: observado
---
# Falha de comunicação com a API resulta em 503

A conexão é encerrada sem resposta. O timeout não é simulado separadamente.

```stub-mtg
GET /cards/386616 -> desconectar
```

```http ttl
GET /carta/386616
```

```resultado
503
redis: (vazio)
```
