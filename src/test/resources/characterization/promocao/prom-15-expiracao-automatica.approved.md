---
regra: PROM-15
status: observado
---
# Promoção expira automaticamente

Quando o TTL termina, o Redis remove a chave. Este cenário espera de verdade.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+3s}}"}
```

```esperar
4s
```

## Após o vencimento

```http ttl
GET /promocao/bf
```

```resultado
404
redis: (vazio)
```

```http corpo
GET /promocao/listar
```

```resultado
200
[ ]
```
