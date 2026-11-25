---
regra: PROM-05
status: suspeito
---
# Criar com nome já usado substitui a promoção

Suspeita: não há conflito. O valor e o TTL anteriores são substituídos.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

## Mesmo nome, outro desconto e sem data

```http redis:percentualDesconto
POST /promocao
{"nome": "bf", "percentualDesconto": 5.0, "dataExpiracao": null}
```

```resultado
201
promocao:bf  ttl=sem-expiracao
  percentualDesconto: 5.0
```
