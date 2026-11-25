---
regra: PROM-14
status: observado
---
# Prorrogar uma promoção inexistente não a cria

Resulta em 404 e nada é gravado.

```http ttl
PUT /promocao/prorrogar/nao-existe
{"novaDataExpiracao": "{{agora+1h}}"}
```

```resultado
404
redis: (vazio)
```
