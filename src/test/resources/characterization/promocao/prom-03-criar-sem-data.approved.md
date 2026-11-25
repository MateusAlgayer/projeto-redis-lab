---
regra: PROM-03
status: observado
---
# Promoção exige uma data de expiração

Sem a data, a requisição é rejeitada e nada é gravado, como declara o contrato OpenAPI.

```http ttl
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": null}
```

```resultado
400
redis: (vazio)
```
