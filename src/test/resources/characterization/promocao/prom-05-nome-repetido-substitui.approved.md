---
regra: PROM-05
status: observado
---
# Criar com nome já usado é rejeitado

Resulta em conflito. O valor e o TTL da promoção existente são mantidos.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

## Mesmo nome, outro desconto e outra data

```http redis:percentualDesconto
POST /promocao
{"nome": "bf", "percentualDesconto": 5.0, "dataExpiracao": "{{agora+2h}}"}
```

```resultado
409
promocao:bf  ttl=~60min
  percentualDesconto: 50.0
```
