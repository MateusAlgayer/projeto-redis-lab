---
regra: PROM-12
status: observado
---
# Prorrogar para uma data passada é rejeitado

A promoção mantém a data e o TTL anteriores.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

```http redis:dataExpiracao
PUT /promocao/prorrogar/bf
{"novaDataExpiracao": "{{agora-1d}}"}
```

```resultado
400
promocao:bf  ttl=~60min
  dataExpiracao: "{{agora+1h}}"
```

## A promoção continua disponível

```http
GET /promocao/bf
```

```resultado
200
```
