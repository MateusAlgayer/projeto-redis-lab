---
regra: PROM-06
status: suspeito
---
# O percentual de desconto não é validado

Suspeita: o contrato OpenAPI declara o percentual como obrigatório, entre 0 e 100.

## Acima de 100

```http
POST /promocao
{"nome": "acima", "percentualDesconto": 150.0, "dataExpiracao": "{{agora+1h}}"}
```

```resultado
201
```

## Negativo

```http
POST /promocao
{"nome": "negativo", "percentualDesconto": -10.0, "dataExpiracao": "{{agora+1h}}"}
```

```resultado
201
```

## Ausente

O último passo mostra o que foi gravado nos três casos.

```http redis:percentualDesconto
POST /promocao
{"nome": "ausente", "dataExpiracao": "{{agora+1h}}"}
```

```resultado
201
promocao:acima  ttl=~60min
  percentualDesconto: 150.0
promocao:ausente  ttl=~60min
  percentualDesconto: null
promocao:negativo  ttl=~60min
  percentualDesconto: -10.0
```
