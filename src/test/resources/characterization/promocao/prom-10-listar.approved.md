---
regra: PROM-10
status: observado
---
# Listagem de promoções

Inclui promoções com e sem TTL. O runner ordena por nome porque a ordem do `KEYS` não é garantida.

## Sem promoções

```http corpo
GET /promocao/listar
```

```resultado
200
[ ]
```

## Com promoções

```preparar
POST /promocao
{"nome": "b-natal", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

```preparar
POST /promocao
{"nome": "a-carnaval", "percentualDesconto": 50.0, "dataExpiracao": null}
```

```http corpo:nome
GET /promocao/listar
```

```resultado
200
nome: ["a-carnaval", "b-natal"]
```
