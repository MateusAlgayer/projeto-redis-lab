---
regra: PROM-01
status: observado
---
# Promoção com data futura é gravada com TTL

A chave no Redis recebe um TTL calculado até a data de expiração.

```http Location ttl
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

```resultado
201
Location: /promocao/bf
promocao:bf  ttl=~60min
```
