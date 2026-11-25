---
regra: PROM-13
status: observado
---
# Prorrogação exige a nova data

Sem a nova data, a requisição é rejeitada e a promoção não muda.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

## Objeto vazio

```http redis:dataExpiracao
PUT /promocao/prorrogar/bf
{}
```

```resultado
400
promocao:bf  ttl=~60min
  dataExpiracao: "{{agora+1h}}"
```
