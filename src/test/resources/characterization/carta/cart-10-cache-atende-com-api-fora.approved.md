---
regra: CART-10
status: observado
---
# Carta em cache continua disponível quando a API falha

Enquanto a entrada existir, a indisponibilidade da API não afeta a consulta.

```stub-mtg
GET /cards/386616 -> 200
{"card": {"name": "Narset"}}
```

```preparar
GET /carta/386616
```

## A API passa a falhar

```stub-mtg
GET /cards/386616 -> 500
```

```http X-Cache chamadas
GET /carta/386616
```

```resultado
200
X-Cache: HIT
chamadas à API: (nenhuma)
```
