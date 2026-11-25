---
regra: PROM-11
status: observado
---
# Prorrogar para uma data futura redefine o TTL

O desconto é mantido e o TTL passa a valer até a nova data.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

```http redis:percentualDesconto,dataExpiracao
PUT /promocao/prorrogar/bf
{"novaDataExpiracao": "{{agora+2h}}"}
```

```resultado
200
promocao:bf  ttl=~120min
  percentualDesconto: 50.0
  dataExpiracao: "{{agora+2h}}"
```
