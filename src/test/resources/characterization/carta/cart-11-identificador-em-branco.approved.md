---
regra: CART-11
status: observado
---
# Identificador em branco não chega à API nem ao cache

O identificador enviado é um espaço.

## Consultar

```http chamadas ttl
GET /carta/%20
```

```resultado
404
chamadas à API: (nenhuma)
redis: (vazio)
```

## Verificar presença

```http chamadas
GET /carta/%20/cache
```

```resultado
404
chamadas à API: (nenhuma)
```

## Invalidar

```http chamadas
DELETE /carta/%20/cache
```

```resultado
404
chamadas à API: (nenhuma)
```
