---
regra: PROM-04
status: suspeito
---
# Promoção criada com data passada não expira

Suspeita: uma promoção já vencida é aceita e gravada sem TTL.

```http ttl
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora-1d}}"}
```

```resultado
201
promocao:bf  ttl=sem-expiracao
```

## A promoção vencida continua disponível

```http
GET /promocao/bf
```

```resultado
200
```
