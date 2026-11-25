---
regra: PROM-04
status: observado
---
# Promoção com data passada é rejeitada

Uma promoção já vencida não é gravada.

```http ttl
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora-1d}}"}
```

```resultado
400
redis: (vazio)
```

## A promoção não está disponível

```http
GET /promocao/bf
```

```resultado
404
```
