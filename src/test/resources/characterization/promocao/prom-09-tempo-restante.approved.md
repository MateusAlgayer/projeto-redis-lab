---
regra: PROM-09
status: observado
---
# Tempo restante de uma promoção

A resposta é o TTL em segundos, exibido aqui em minutos arredondados para cima. Cada promoção tem o seu.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

```preparar
POST /promocao
{"nome": "longa", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+2h}}"}
```

## Com TTL

```http corpo
GET /promocao/tempoRestante/bf
```

```resultado
200
~60min
```

## Outra validade

```http corpo
GET /promocao/tempoRestante/longa
```

```resultado
200
~120min
```

## Inexistente

```http
GET /promocao/tempoRestante/nao-existe
```

```resultado
404
```
