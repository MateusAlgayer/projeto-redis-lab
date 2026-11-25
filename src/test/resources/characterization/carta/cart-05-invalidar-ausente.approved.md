---
regra: CART-05
status: observado
---
# Invalidar uma carta ausente do cache resulta em 404

Nada é alterado e a API não é chamada.

```http chamadas
DELETE /carta/386616/cache
```

```resultado
404
chamadas à API: (nenhuma)
```
