---
regra: CART-06
status: observado
---
# Carta inexistente não é guardada no cache

Não há cache negativo: cada consulta volta a chamar a API.

```stub-mtg
GET /cards/386616 -> 404
```

## Primeira consulta

```http chamadas ttl
GET /carta/386616
```

```resultado
404
chamadas à API: GET /cards/386616
redis: (vazio)
```

## Segunda consulta

```http chamadas ttl
GET /carta/386616
```

```resultado
404
chamadas à API: GET /cards/386616
redis: (vazio)
```
