---
regra: PROM-12
status: suspeito
---
# Prorrogar para uma data passada torna a promoção permanente

Suspeita: a operação é aceita e a chave perde o TTL.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

```http ttl
PUT /promocao/prorrogar/bf
{"novaDataExpiracao": "{{agora-1d}}"}
```

```resultado
200
promocao:bf  ttl=sem-expiracao
```

## A promoção continua disponível

```http
GET /promocao/bf
```

```resultado
200
```
