---
regra: PROM-09
status: observado
---
# Tempo restante de uma promoção

A resposta é o TTL em segundos, exibido aqui em minutos arredondados para cima. Uma promoção sem TTL não é tratada como inexistente.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

```preparar
POST /promocao
{"nome": "eterna", "percentualDesconto": 50.0, "dataExpiracao": null}
```

## Com TTL

```http corpo
GET /promocao/tempoRestante/bf
```

```resultado
200
~60min
```

## Sem TTL

```http corpo
GET /promocao/tempoRestante/eterna
```

```resultado
200
sem-expiracao
```

## Inexistente

```http
GET /promocao/tempoRestante/nao-existe
```

```resultado
404
```
