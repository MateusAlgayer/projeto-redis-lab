---
regra: PROM-16
status: observado
---
# Contrato da promoção na resposta e no Redis

Este é o único cenário de promoção que registra o JSON inteiro. O valor gravado tem os mesmos campos da resposta.

```preparar
POST /promocao
{"nome": "bf", "percentualDesconto": 50.0, "dataExpiracao": "{{agora+1h}}"}
```

```http corpo redis
GET /promocao/bf
```

```resultado
200
{
  "dataExpiracao" : "{{agora+1h}}",
  "nome" : "bf",
  "percentualDesconto" : 50.0
}
promocao:bf  ttl=~60min
  {"dataExpiracao":"{{agora+1h}}","nome":"bf","percentualDesconto":50.0}
```
