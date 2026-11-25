---
regra: PROM-02
status: observado
---
# Promoção exige um nome

Nome ausente ou em branco resulta em 400. Uma requisição sem corpo resulta em 415. Nada é gravado em nenhum dos casos.

## Sem nome

```http ttl
POST /promocao
{"percentualDesconto": 10.0, "dataExpiracao": "{{agora+1h}}"}
```

```resultado
400
redis: (vazio)
```

## Nome em branco

```http ttl
POST /promocao
{"nome": "   ", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

```resultado
400
redis: (vazio)
```

## Sem corpo

```http ttl
POST /promocao
```

```resultado
415
redis: (vazio)
```
