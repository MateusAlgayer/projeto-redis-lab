---
regra: PROM-08
status: observado
---
# Verificação de existência

Responde apenas com o status HTTP.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

## Existente

```http corpo
GET /promocao/existe/bf
```

```resultado
200
corpo: (vazio)
```

## Inexistente

```http corpo
GET /promocao/existe/nao-existe
```

```resultado
404
corpo: (vazio)
```
