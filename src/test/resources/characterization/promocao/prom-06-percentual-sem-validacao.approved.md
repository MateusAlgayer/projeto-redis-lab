---
regra: PROM-06
status: observado
---
# O percentual de desconto deve estar entre 0 e 100

O percentual é obrigatório, como declara o contrato OpenAPI. Os limites 0 e 100 são aceitos.

## Limites aceitos

```http
POST /promocao
{"nome": "minimo", "percentualDesconto": 0.0, "dataExpiracao": "{{agora+1h}}"}
```

```resultado
201
```

```http redis:percentualDesconto
POST /promocao
{"nome": "maximo", "percentualDesconto": 100.0, "dataExpiracao": "{{agora+1h}}"}
```

```resultado
201
promocao:maximo  ttl=~60min
  percentualDesconto: 100.0
promocao:minimo  ttl=~60min
  percentualDesconto: 0.0
```

## Acima de 100

```http
POST /promocao
{"nome": "acima", "percentualDesconto": 150.0, "dataExpiracao": "{{agora+1h}}"}
```

```resultado
400
```

## Negativo

```http
POST /promocao
{"nome": "negativo", "percentualDesconto": -10.0, "dataExpiracao": "{{agora+1h}}"}
```

```resultado
400
```

## Ausente

O último passo mostra que nenhum dos três casos foi gravado.

```http redis:percentualDesconto
POST /promocao
{"nome": "ausente", "dataExpiracao": "{{agora+1h}}"}
```

```resultado
400
promocao:maximo  ttl=~60min
  percentualDesconto: 100.0
promocao:minimo  ttl=~60min
  percentualDesconto: 0.0
```
