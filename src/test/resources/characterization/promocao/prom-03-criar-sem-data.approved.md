---
regra: PROM-03
status: observado
---
# Promoção sem data de expiração não expira

A API aceita a ausência da data e grava a chave sem TTL. O contrato OpenAPI declara o campo como obrigatório.

```http ttl
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": null}
```

```resultado
201
promocao:bf  ttl=sem-expiracao
```
