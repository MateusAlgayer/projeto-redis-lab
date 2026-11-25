---
regra: PROM-07
status: observado
---
# Consulta por nome

Uma promoção cadastrada é devolvida; um nome desconhecido resulta em 404.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

## Existente

```http corpo:nome,percentualDesconto,dataExpiracao
GET /promocao/bf
```

```resultado
200
nome: "bf"
percentualDesconto: 50.0
dataExpiracao: "{{agora+1h}}"
```

## Inexistente

```http
GET /promocao/nao-existe
```

```resultado
404
```
